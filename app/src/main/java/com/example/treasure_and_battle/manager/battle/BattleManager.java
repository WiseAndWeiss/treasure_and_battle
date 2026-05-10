package com.example.treasure_and_battle.manager.battle;

import android.content.Context;

import com.example.treasure_and_battle.battle.RewardCalculator;
import com.example.treasure_and_battle.battle.action.BattleAction;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.BattleContext.RevealedIntent;
import com.example.treasure_and_battle.battle.BattleContext.SurpriseDirection;
import com.example.treasure_and_battle.battle.damage.DamageConfig;
import com.example.treasure_and_battle.battle.EscapeCalculator;
import com.example.treasure_and_battle.battle.SkillTargetResolver;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.TriggerDispatcher;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.battle.action.ActionIntent;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.item.Item;

import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.utils.RandomUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 战斗管理器 (BattleManager)
 * 职责：战斗流程的唯一驱动者，封装所有战斗规则
 * 设计模式：状态机 + 全局速度优先队列
 */
public class BattleManager {
    private static BattleManager instance;
    private Context context;
    private BattleContext currentBattleContext;

    private BattleManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public BattleContext getContext() {
        return currentBattleContext;
    }

    public static synchronized BattleManager getInstance(Context context) {
        if (instance == null) {
            instance = new BattleManager(context);
        }
        return instance;
    }

    public static synchronized void releaseInstance() {
        instance = null;
    }

    // ====================== 【入口】1. 初始化战斗 ======================
    public BattleContext startBattle(Player player, List<Monster> monsters, SurpriseDirection surpriseAttacker) {
        BattleContext ctx = createAndInitBattle(player, monsters, surpriseAttacker);
        battleLoop(ctx);
        return ctx;
    }

    /**
     * UI 驱动战斗：完成开局与第一回合开始，按速度条执行到「玩家」行动前暂停；之后由界面在玩家耗光行动点后调用
     * {@link #onPlayerTurnFullySpent(BattleContext)} 继续。
     */
    public BattleContext bootstrapBattleForUi(Player player, List<Monster> monsters, SurpriseDirection surpriseAttacker) {
        BattleContext ctx = createAndInitBattle(player, monsters, surpriseAttacker);
        if (!beginRoundForUi(ctx)) {
            return ctx;
        }
        runMonsterTurnsUntilPlayerTurn(ctx);
        return ctx;
    }

    private BattleContext createAndInitBattle(Player player, List<Monster> monsters, SurpriseDirection surpriseAttacker) {
        BattleContext ctx = new BattleContext(player, monsters, surpriseAttacker);

        player.setDead(false);
        player.resetActionPoints();
        for (Monster m : ctx.monsters) {
            if (m == null) continue;
            m.setDead(false);
            m.resetActionPoints();
        }

        if (ctx.surpriseAttacker != SurpriseDirection.NONE) {
            ctx.addLog(LogType.INIT, "【偷袭】一方发起突袭，获得先手行动权。");
        }
        TriggerDispatcher.dispatch(ctx, TriggerType.ON_BATTLE_START, context);

        ctx.addLog(LogType.INIT, "战斗开始：[%s] VS [%d个怪物]",
                player.getName(), ctx.getAliveMonsters().size());
        return ctx;
    }

    /** 新回合：递增回合数、日志、回合开始阶段（意图/速度条）。达到回合上限则结束战斗。 */
    private boolean beginRoundForUi(BattleContext ctx) {
        ctx.currentRound++;
        ctx.resetDamageData();

        if (ctx.currentRound >= 100) {
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.DEFEAT;
            ctx.addLog(LogType.SYSTEM, "【系统】达到回合数上限(100)，战斗强制判定为失败。");
            return false;
        }

        ctx.addLog(LogType.ROUND_INFO, "======== 第 %d 回合开始 ========", ctx.currentRound);

        onRoundStart(ctx);
        return !ctx.isBattleEnded;
    }

    /**
     * 从当前 {@link BattleContext#actionOrderIndex} 起执行怪物行动，直到轮到玩家或战斗结束或需进入下一回合。
     */
    public void runMonsterTurnsUntilPlayerTurn(BattleContext ctx) {
        if (ctx == null) {
            return;
        }
        if (ctx.roundActionOrder == null) {
            ctx.roundActionOrder = new ArrayList<>();
        }
        while (!ctx.isBattleEnded) {
            if (ctx.actionOrderIndex >= ctx.roundActionOrder.size()) {
                onRoundEnd(ctx);
                if (ctx.isBattleEnded) {
                    return;
                }
                if (!beginRoundForUi(ctx)) {
                    return;
                }
                continue;
            }

            BattleEntity actor = ctx.roundActionOrder.get(ctx.actionOrderIndex);
            if (actor.isDead()) {
                ctx.actionOrderIndex++;
                continue;
            }

            ctx.currentActor = actor;
            ctx.currentTarget = ctx.getPrimaryMonsterTarget();
            if (ctx.currentTarget == null) {
                ctx.currentTarget = ctx.player;
            }

            ctx.addLog(LogType.ROUND_INFO, "轮到 [%s] 行动", actor.getName());

            if (actor instanceof Player) {
                ctx.currentActionPoints = ctx.player.getCurrentActionPoints();
                ctx.addLog(LogType.ROUND_INFO, "玩家回合，行动点: %d", ctx.currentActionPoints);
                return;
            }

            monsterActionPhaseFor(ctx, (Monster) actor);
            ctx.actionOrderIndex++;
            checkDeath(ctx);
        }
    }

    /** 玩家本回合行动点已用尽时调用：越过玩家序号并继续执行速度条上后续单位。 */
    public void onPlayerTurnFullySpent(BattleContext ctx) {
        if (ctx == null || ctx.isBattleEnded) {
            return;
        }
        if (ctx.player != null && ctx.player.getCurrentActionPoints() > 0) {
            return;
        }
        ctx.actionOrderIndex++;
        runMonsterTurnsUntilPlayerTurn(ctx);
    }

    // ====================== 2. 战斗主循环（全自动，含玩家自动普攻） ======================
    private void battleLoop(BattleContext ctx) {
        while (!ctx.isBattleEnded) {
            if (!beginRoundForUi(ctx)) {
                break;
            }

            executeRoundActionPhase(ctx);
            if (ctx.isBattleEnded) {
                break;
            }

            onRoundEnd(ctx);
            if (ctx.isBattleEnded) {
                break;
            }
        }

        settleBattleResult(ctx);
    }

    // ====================== 3. 回合开始阶段 ======================
    private void onRoundStart(BattleContext ctx) {
        // 3.1 所有实体重置行动点
        for (BattleEntity e : ctx.playerParty) {
            if (e != null && !e.isDead()) e.resetActionPoints();
        }
        for (Monster m : ctx.getAliveMonsters()) {
            m.resetActionPoints();
        }

        // 3.2 所有怪物统一下达本轮意图 + 看破判定
        ctx.monsterRevealedIntents.clear();
        for (Monster m : ctx.getAliveMonsters()) {
            List<ActionIntent> intents = m.decideNextTurnIntents();
            if (intents == null) intents = new ArrayList<>();

            double seeThroughChance = calculateSeeThroughChance(ctx.player, m);
            List<RevealedIntent> revealed = new ArrayList<>();
            for (ActionIntent intent : intents) {
                boolean seen = RandomUtils.checkProbability((float) seeThroughChance);
                revealed.add(new RevealedIntent(intent, seen));
            }
            ctx.monsterRevealedIntents.put(m.getEntityId(), revealed);

            ctx.addLog(LogType.DODGE_CRIT,
                    "【意图看破判定】vs[%s] 看破率:%.1f%% 意图数:%d",
                    m.getName(), seeThroughChance * 100, intents.size());
            for (RevealedIntent ri : revealed) {
                String label = ri.seenThrough ? ri.intent.getType().name() : "?";
                ctx.addLog(LogType.ACTION, "  [%s] 意图: %s", m.getName(), label);
            }
        }

        // 构建全局速度优先队列
        buildSpeedQueue(ctx);

        // 触发回合开始 Buff/词缀/被动（全部实体）
        TriggerDispatcher.dispatch(ctx, TriggerType.ON_ROUND_START, context);
    }
    // ====================== 4. 构建速度优先队列 ======================
    public void buildSpeedQueue(BattleContext ctx) {
        List<BattleEntity> actors = new ArrayList<>();
        for (BattleEntity e : ctx.playerParty) {
            if (e != null && !e.isDead()) actors.add(e);
        }
        actors.addAll(ctx.getAliveMonsters());

        actors.sort(Comparator.comparingInt(e -> -e.getFinalAttributes().speed));

        ctx.roundActionOrder = applySurpriseToQueue(ctx, actors);
        ctx.actionOrderIndex = 0;

        StringBuilder orderDesc = new StringBuilder("行动顺序: ");
        for (BattleEntity e : ctx.roundActionOrder) {
            orderDesc.append("[").append(e.getName()).append("(速").append(e.getFinalAttributes().speed).append(")] ");
        }
        ctx.addLog(LogType.ROUND_INFO, orderDesc.toString().trim());
    }

    // ====================== 5. 偷袭阵营偏移 ======================
    public List<BattleEntity> applySurpriseToQueue(BattleContext ctx, List<BattleEntity> actors) {
        if (ctx.surpriseAttacker == SurpriseDirection.NONE) return new ArrayList<>(actors);

        List<BattleEntity> playerSide = new ArrayList<>();
        List<BattleEntity> monsterSide = new ArrayList<>();

        for (BattleEntity e : actors) {
            if (e instanceof Player || ctx.playerParty.contains(e)) {
                playerSide.add(e);
            } else {
                monsterSide.add(e);
            }
        }

        List<BattleEntity> ordered = new ArrayList<>();
        if (ctx.surpriseAttacker == SurpriseDirection.PLAYER_SURPRISE) {
            ordered.addAll(playerSide);
            ordered.addAll(monsterSide);
            ctx.addLog(LogType.INIT, "【偷袭】玩家方发起突袭，全阵营先行动。");
        } else {
            ordered.addAll(monsterSide);
            ordered.addAll(playerSide);
            ctx.addLog(LogType.INIT, "【伏击】怪物方发起伏击，全阵营先行动。");
        }
        return ordered;
    }

    // ====================== 6. 统一轮流行动阶段 ======================
    private void executeRoundActionPhase(BattleContext ctx) {
        for (; ctx.actionOrderIndex < ctx.roundActionOrder.size(); ctx.actionOrderIndex++) {
            if (ctx.isBattleEnded) break;

            BattleEntity actor = ctx.roundActionOrder.get(ctx.actionOrderIndex);
            if (actor.isDead()) continue;

            ctx.currentActor = actor;
            ctx.currentTarget = ctx.getPrimaryMonsterTarget();
            if (ctx.currentTarget == null) ctx.currentTarget = ctx.player;

            ctx.addLog(LogType.ROUND_INFO, "轮到 [%s] 行动", actor.getName());

            if (actor instanceof Player) {
                playerActionPhase(ctx);
            } else if (actor instanceof Monster) {
                monsterActionPhaseFor(ctx, (Monster) actor);
            }
        }
    }

    // ====================== 技能系统支持 ======================

    /**
     * 技能施放的核心方法
     * 负责处理技能消耗、目标选择、效果触发等
     * @param caster 施法者
     * @param skill 技能
     * @param targets 目标列表
     * @param context 战斗上下文
     */
    public void executeSkill(BattleEntity caster, com.example.treasure_and_battle.skill.active.ActiveSkill skill,
                            List<BattleEntity> targets, BattleContext context) {
        this.currentBattleContext = context;
        try {
            skill.applyCastCost(caster);
            skill.onCast(caster, targets, this);
            context.addLog(LogType.ACTION, "【%s】[%s] 对目标施放了 [%s]",
                caster.getClass().getSimpleName(), caster.getName(), skill.getSkillName());
        } finally {
            this.currentBattleContext = null;
        }
    }

    /**
     * 造成物理伤害（用于技能）- 委托给 DamageManager
     */
    public int dealPhysicalDamage(BattleEntity attacker, BattleEntity target,
                                   int baseDamage, BattleContext context) {
        return DamageManager.getInstance(this.context).dealPhysicalDamage(attacker, target, baseDamage, context);
    }

    /**
     * 造成穿甲伤害（无视防御与减伤，受护盾吸收）
     */
    public int dealPiercingDamage(BattleEntity attacker, BattleEntity target,
                                   int piercingDamage, BattleContext context) {
        return DamageManager.getInstance(this.context).dealPiercingDamage(attacker, target, piercingDamage, context);
    }

    /**
     * 造成真实伤害（无视防御、护盾、减伤等一切防御机制）
     */
    public void dealTrueDamage(BattleEntity target, int trueDamage, BattleContext context) {
        DamageManager.getInstance(this.context).dealTrueDamage(target, trueDamage, context);
    }

    /**
     * 造成法术伤害（用于技能）- 委托给 DamageManager
     */
    public int dealMagicalDamage(BattleEntity attacker, BattleEntity target,
                                  int baseDamage, BattleContext context) {
        return DamageManager.getInstance(this.context).dealMagicalDamage(attacker, target, baseDamage, context);
    }

    /**
     * 施加buff到目标
     */
    public void applyBuff(BattleEntity target, com.example.treasure_and_battle.buff.BaseBuff buff) {
        BuffManager.getInstance(this.context).addBuff(target, buff);
        target.markAttributeCacheDirty();
    }

    // ====================== 【核心规则实现】功能清单具体逻辑 ======================

    // ====================== 7. 怪物行动阶段 ======================
    private void monsterActionPhaseFor(BattleContext ctx, Monster m) {
        List<RevealedIntent> revealed = ctx.monsterRevealedIntents.get(m.getEntityId());
        if (revealed == null || revealed.isEmpty()) {
            BattleAction fallback = BattleAction.normalAttack(m, ctx.player);
            submitBattleAction(ctx, fallback);
            return;
        }

        for (RevealedIntent ri : revealed) {
            if (ctx.isBattleEnded) break;
            ri.executed = true;

            BattleAction action = toBattleAction(ctx, m, ri.intent);
            if (action == null) continue;

            submitBattleAction(ctx, action);
        }
    }

    // ====================== 8. 玩家行动阶段 ======================
    private void playerActionPhase(BattleContext ctx) {
        ctx.currentActionPoints = ctx.player.getCurrentActionPoints();
        ctx.addLog(LogType.ROUND_INFO, "玩家回合，行动点: %d", ctx.currentActionPoints);

        if (ctx.currentActionPoints > 0 && !ctx.isBattleEnded) {
            Monster target = ctx.getPrimaryMonsterTarget();
            if (target != null) {
                submitBattleAction(ctx, BattleAction.normalAttack(ctx.player, target));
            }
            ctx.currentActionPoints = 0;
            ctx.player.setCurrentActionPoints(0);
        }
    }

    // ====================== 9. 回合结束阶段 ======================
    private void onRoundEnd(BattleContext ctx) {
        for (BattleEntity e : ctx.playerParty) {
            if (e == null || e.isDead()) continue;
            BuffManager.getInstance(context).onRoundEnd(e, ctx);
            BuffManager.getInstance(context).tickBuffs(e);
        }
        for (Monster m : ctx.getAliveMonsters()) {
            if (m == null) continue;
            BuffManager.getInstance(context).onRoundEnd(m, ctx);
            m.tickSkillCooldowns();
        }
        TriggerDispatcher.dispatch(ctx, TriggerType.ON_ROUND_END, context);
        BuffManager.getInstance(context).tickBuffsForAllMonsters(ctx);

        checkDeath(ctx);
    }

    // ====================== 10. 看破概率计算 ======================
    public double calculateSeeThroughChance(Player player, Monster monster) {
        AttributeSet playerAttr = player.getFinalAttributes();
        AttributeSet monsterAttr = monster.getFinalAttributes();

        int playerSpirit = Math.max(1, playerAttr.spirit);
        int monsterSpirit = Math.max(1, monsterAttr.spirit);

        double chance = 0.5 * ((double) playerSpirit / monsterSpirit);
        return Math.max(0.1, Math.min(0.9, chance));
    }

    // ====================== 11. 玩家逃跑 ======================
    public boolean executePlayerEscape(BattleContext ctx) {
        ctx.addLog(LogType.ACTION, "玩家尝试逃跑...");

        if (!ctx.player.consumeActionPoints(1)) {
            ctx.addLog(LogType.SYSTEM, "行动点不足，逃跑失败。");
            return false;
        }
        ctx.currentActionPoints = Math.max(0, ctx.currentActionPoints - 1);

        AttributeSet playerAttr = ctx.player.getFinalAttributes();
        Monster fastestMonster = pickFastestAliveMonster(ctx);
        if (fastestMonster == null) {
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.VICTORY;
            return true;
        }
        AttributeSet monsterAttr = fastestMonster.getFinalAttributes();

        double escapeChance = EscapeCalculator.calculateEscapeChance(playerAttr.speed, monsterAttr.speed);

        ctx.addLog(LogType.DODGE_CRIT, "【逃跑判定】玩家速度(%.0f) vs 怪物速度(%.0f) => 成功率: %.1f%%",
                playerAttr.speed, monsterAttr.speed, escapeChance * 100);

        if (RandomUtils.checkProbability((float) escapeChance)) {
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.ESCAPED;
            ctx.addLog(LogType.ACTION, "逃跑成功。");
            return true;
        } else {
            ctx.addLog(LogType.ACTION, "逃跑失败，遭到怪物追击。");
            executeNormalAttack(ctx, fastestMonster, ctx.player);
            return false;
        }
    }

    // ====================== 12. 普通攻击 ======================
    public void executeNormalAttack(BattleContext ctx, BattleEntity attacker, BattleEntity target) {
        AttributeSet attackerAttr = attacker.getFinalAttributes();
        executeNormalAttack(ctx, attacker, target, attackerAttr.physicalAtk);
    }

    public void executeNormalAttack(BattleContext ctx, BattleEntity attacker, BattleEntity target, int baseDamage) {
        String actorName = attacker.getName();
        String targetName = target.getName();
        ctx.addLog(LogType.ACTION, "[%s] 发动普通攻击。", actorName);

        TriggerDispatcher.dispatch(attacker, ctx, TriggerType.ON_ATTACK, context);

        AttributeSet targetAttr = target.getFinalAttributes();
        ctx.addLog(LogType.DAMAGE, "  基础物理伤害：%d", baseDamage);

        DamageManager.getInstance(this.context).dealDamage(
                DamageConfig.normalAttack(), attacker, target, baseDamage, ctx);

        if (!ctx.isHit) {
            ctx.addLog(LogType.DODGE_CRIT, "  攻击未命中");
        } else {
            ctx.addLog(LogType.DAMAGE, "  %s受到 %d 点伤害。剩余HP：(%d/%d)",
                    targetName, ctx.finalDamage, target.getCurrentHp(), targetAttr.maxHp);
        }

        checkDeath(ctx);
    }

    // ====================== 13. 意图→动作转换 ======================
    private BattleAction toBattleAction(BattleContext ctx, Monster actor, ActionIntent intent) {
        switch (intent.getType()) {
            case ATTACK:
                return new BattleAction(BattleAction.ActionType.ATTACK, actor, ctx.player,
                        intent.getApCost(), intent.getMpCost(), 0,
                        intent.getPowerMultiplier(), null, intent.getName());
            case SKILL:
                return BattleAction.skillTodo(actor, ctx.player,
                        intent.getActionRefId(), intent.getApCost(), intent.getMpCost(),
                        intent.getPowerMultiplier(), intent.getName());
            case ESCAPE:
                return new BattleAction(BattleAction.ActionType.ESCAPE, actor, ctx.player,
                        intent.getApCost(), intent.getMpCost(), 0,
                        1.0, null, intent.getName());
            default:
                return null;
        }
    }

    // ====================== 14. 执行战斗动作 ======================
    public boolean submitBattleAction(BattleContext ctx, BattleAction action) {
        if (action == null || action.getActor() == null) return false;

        BattleEntity actor = action.getActor();
        BattleEntity target = action.getTarget();

        ctx.currentActor = actor;
        ctx.currentTarget = target;

        if (action.getType() == BattleAction.ActionType.ESCAPE && actor == ctx.player) {
            executePlayerEscape(ctx);
            return true;
        }

        if (actor.getCurrentActionPoints() < action.getApCost()) {
            ctx.addLog(LogType.SYSTEM, "[%s] 行动点不足，无法执行 [%s]。", actor.getName(), action.getDisplayName());
            return false;
        }
        if (actor.getCurrentMp() < action.getMpCost()) {
            ctx.addLog(LogType.SYSTEM, "[%s] 魔力不足，无法执行 [%s]。", actor.getName(), action.getDisplayName());
            return false;
        }

        if (action.getType() != BattleAction.ActionType.SKILL) {
            actor.consumeActionPoints(action.getApCost());
            actor.setCurrentMp(Math.max(0, actor.getCurrentMp() - action.getMpCost()));
        }

        AttributeSet attackerAttr = actor.getFinalAttributes();

        switch (action.getType()) {
            case ATTACK: {
                int baseDamage = (int) Math.round(attackerAttr.physicalAtk * action.getPowerMultiplier());
                executeNormalAttack(ctx, actor, target, baseDamage);
                return true;
            }
            case ESCAPE:
                if (actor instanceof Monster) executeMonsterEscape(ctx);
                return true;
            case SKILL:
                if (actor instanceof Monster) {
                    Monster m = (Monster) actor;
                    ActiveSkill skill = m.getMonsterSkill(action.getActionRefId());
                    if (skill != null && skill.isCooldownReady()) {
                        List<BattleEntity> targets =
                                SkillTargetResolver.resolve(skill.getSkillRangeType(), actor, ctx);
                        try {
                            this.currentBattleContext = ctx;
                            skill.applyCastCost(actor);
                            skill.onCast(actor, targets, this);
                            ctx.addLog(LogType.ACTION, "[%s] 释放了 [%s]",
                                    actor.getName(), skill.getSkillName());
                        } catch (Exception e) {
                            ctx.addLog(LogType.SYSTEM, "[%s] 释放技能 [%s] 失败: %s",
                                    actor.getName(), skill.getSkillName(), e.getMessage());
                            e.printStackTrace();
                        } finally {
                            this.currentBattleContext = null;
                        }
                    } else {
                        ctx.addLog(LogType.ACTION, "[%s] 尝试释放技能 [%s]（技能未就绪或不存在）",
                                actor.getName(), action.getDisplayName());
                    }
                }
                return true;
            case ITEM:
                ctx.addLog(LogType.ACTION, "[%s] 尝试使用道具 [%s]（TODO：道具体系未接入）",
                        actor.getName(), action.getDisplayName());
                return true;
            default:
                return false;
        }
    }

    // ====================== 15. 怪物逃跑 ======================
    public boolean executeMonsterEscape(BattleContext ctx) {
        if (!(ctx.currentActor instanceof Monster)) return false;
        Monster actingMonster = (Monster) ctx.currentActor;
        ctx.addLog(LogType.ACTION, "怪物[%s]尝试逃跑...", actingMonster.getName());

        AttributeSet monsterAttr = actingMonster.getFinalAttributes();
        AttributeSet playerAttr = ctx.player.getFinalAttributes();

        double escapeChance = EscapeCalculator.calculateEscapeChance(monsterAttr.speed, playerAttr.speed);

        ctx.addLog(LogType.DODGE_CRIT,
                "【怪物逃跑判定】怪物速度(%.0f) vs 玩家速度(%.0f) => 成功率: %.1f%%",
                monsterAttr.speed, playerAttr.speed, escapeChance * 100);

        if (RandomUtils.checkProbability((float) escapeChance)) {
            actingMonster.setDead(true);
            ctx.addLog(LogType.ACTION, "怪物[%s]逃跑成功。", actingMonster.getName());
            if (ctx.getAliveMonsters().isEmpty()) {
                ctx.isBattleEnded = true;
                ctx.battleResult = BattleContext.BattleResult.MONSTER_ESCAPED;
            }
            return true;
        }
        ctx.addLog(LogType.ACTION, "怪物[%s]逃跑失败。", actingMonster.getName());
        return false;
    }

    // ====================== 16. 死亡检查 ======================
    public void checkDeath(BattleContext ctx) {
        boolean playerPartyAllDead = ctx.getAlivePlayerParty().isEmpty();
        if (playerPartyAllDead) {
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.DEFEAT;
            ctx.addLog(LogType.DEATH, "玩家方全灭。");
        } else if (ctx.getAliveMonsters().isEmpty()) {
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.VICTORY;
            ctx.addLog(LogType.DEATH, "全部怪物已失去战斗能力，战斗胜利。");
        }
    }

    // ====================== 17. 战斗结算 ======================
    public void settleBattleResult(BattleContext ctx) {
        ctx.addLog(LogType.ROUND_INFO, "======== 战斗结算 ========");

        TriggerDispatcher.dispatch(ctx, TriggerType.ON_BATTLE_END, context);

        if (ctx.battleResult == BattleContext.BattleResult.VICTORY) {
            int finalExp = RewardCalculator.calculateExp(ctx.player, ctx.monsters);
            int finalGold = RewardCalculator.calculateGold(ctx.player, ctx.monsters);
            if (ctx.player.owner != null) {
                ctx.player.owner.gainExp(finalExp);
                ctx.player.owner.addGold(finalGold);
            }
            ctx.addLog(LogType.RESULT, "获得战利品：\n  - 金币：+%d\n  - 经验：+%d", finalGold, finalExp);

            // 掉落物生成 → 存入待领取列表（玩家可选择拿取/全部拿取）
            List<Item> drops =
                DropManager.getInstance(context).generateDrops(ctx);
            ctx.pendingLoot = drops;
            ctx.addLog(LogType.RESULT, "战斗掉落：共 %d 件物品待领取", drops.size());
        } else if (ctx.battleResult == BattleContext.BattleResult.DEFEAT) {
            ctx.player.setCurrentHp(1);
            ctx.player.setDead(false);
            ctx.addLog(LogType.RESULT, "战斗失败，已扣除部分金币，保留1点生命值。");
        } else if (ctx.battleResult == BattleContext.BattleResult.ESCAPED) {
            ctx.addLog(LogType.RESULT, "战斗结束：玩家成功逃跑。\n");
        } else if (ctx.battleResult == BattleContext.BattleResult.MONSTER_ESCAPED) {
            ctx.addLog(LogType.RESULT, "战斗结束：怪物逃跑。\n");
        } else {
            ctx.addLog(LogType.SYSTEM, "战斗结算时未识别战斗结果，跳过奖励与惩罚。\n");
        }
    }

    // ====================== 18. 工具方法 ======================

    private Monster pickFastestAliveMonster(BattleContext ctx) {
        Monster fastest = null;
        for (Monster m : ctx.getAliveMonsters()) {
            if (fastest == null || m.getFinalAttributes().speed > fastest.getFinalAttributes().speed) {
                fastest = m;
            }
        }
        return fastest;
    }

    public Monster pickActingMonster(BattleContext ctx) {
        return pickFastestAliveMonster(ctx);
    }
}
