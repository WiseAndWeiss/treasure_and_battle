package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.battle.action.BattleAction;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.BattleContext.RevealedIntent;
import com.example.treasure_and_battle.battle.BattleContext.SurpriseDirection;
import com.example.treasure_and_battle.battle.DamageType;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.defensive.DamageReductionBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.ActionIntent;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
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

    private BattleManager(Context context) {
        this.context = context.getApplicationContext();
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
    public BattleContext startBattle(Player player, Monster monster, boolean isSurpriseAttack) {
        List<Monster> monsters = new ArrayList<>();
        if (monster != null) {
            monsters.add(monster);
        }
        return startBattle(player, monsters, isSurpriseAttack);
    }

    public BattleContext startBattle(Player player, List<Monster> monsters, boolean isSurpriseAttack) {
        return startBattle(player, monsters,
                isSurpriseAttack ? SurpriseDirection.PLAYER_SURPRISE : SurpriseDirection.NONE);
    }

    public BattleContext startBattle(Player player, List<Monster> monsters, SurpriseDirection surpriseAttacker) {
        BattleContext ctx = new BattleContext(player, monsters, surpriseAttacker);

        player.setDead(false);
        player.resetActionPoints();
        for (Monster m : ctx.monsters) {
            if (m == null) continue;
            m.setDead(false);
            m.resetActionPoints();
        }

        BuffManager.getInstance(context).triggerBuffs(player, ctx, BuffTriggerType.ON_BATTLE_START);
        AffixManager.getInstance(context).triggerAffixes(player, ctx, AffixTriggerType.ON_BATTLE_START);
        BuffManager.getInstance(context).triggerBuffsForAllMonsters(ctx, BuffTriggerType.ON_BATTLE_START);
        AffixManager.getInstance(context).triggerAffixesForAllMonsters(ctx, AffixTriggerType.ON_BATTLE_START);

        ctx.addLog(LogType.INIT, "战斗开始：[%s] VS [%d个怪物]",
                player.getName(), ctx.getAliveMonsters().size());

        battleLoop(ctx);

        return ctx;
    }

    // ====================== 2. 战斗主循环 ======================
    private void battleLoop(BattleContext ctx) {
        while (!ctx.isBattleEnded) {
            ctx.currentRound++;
            ctx.resetDamageData();

            if (ctx.currentRound >= 100) {
                ctx.isBattleEnded = true;
                ctx.battleResult = BattleContext.BattleResult.DEFEAT;
                ctx.addLog(LogType.SYSTEM, "【系统】达到回合数上限(100)，战斗强制判定为失败。");
                break;
            }

            ctx.addLog(LogType.ROUND_INFO, "======== 第 %d 回合开始 ========", ctx.currentRound);

            onRoundStart(ctx);
            if (ctx.isBattleEnded) break;

            executeRoundActionPhase(ctx);
            if (ctx.isBattleEnded) break;

            onRoundEnd(ctx);
            if (ctx.isBattleEnded) break;
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

        // 3.3 构建全局速度优先队列
        buildSpeedQueue(ctx);

        // 3.4 触发回合开始 Buff/词缀（全部实体）
        for (BattleEntity e : ctx.playerParty) {
            if (e == null || e.isDead()) continue;
            BuffManager.getInstance(context).triggerBuffs(e, ctx, BuffTriggerType.ON_ROUND_START);
            AffixManager.getInstance(context).triggerAffixes(e, ctx, AffixTriggerType.ON_ROUND_START);
        }
        BuffManager.getInstance(context).triggerBuffsForAllMonsters(ctx, BuffTriggerType.ON_ROUND_START);
        AffixManager.getInstance(context).triggerAffixesForAllMonsters(ctx, AffixTriggerType.ON_ROUND_START);
    }

    // ====================== 4. 构建速度优先队列 ======================
    public void buildSpeedQueue(BattleContext ctx) {
        List<BattleEntity> actors = new ArrayList<>();
        for (BattleEntity e : ctx.playerParty) {
            if (e != null && !e.isDead()) actors.add(e);
        }
        actors.addAll(ctx.getAliveMonsters());

        actors.sort(Comparator.comparingInt(e -> -e.getFinalAttributes().speed));

        applySurpriseToQueue(ctx, actors);

        ctx.roundActionOrder = actors;
        ctx.actionOrderIndex = 0;

        StringBuilder orderDesc = new StringBuilder("行动顺序: ");
        for (BattleEntity e : actors) {
            orderDesc.append("[").append(e.getName()).append("(速").append(e.getFinalAttributes().speed).append(")] ");
        }
        ctx.addLog(LogType.ROUND_INFO, orderDesc.toString().trim());
    }

    // ====================== 5. 偷袭阵营偏移 ======================
    public void applySurpriseToQueue(BattleContext ctx, List<BattleEntity> actors) {
        if (ctx.surpriseAttacker == SurpriseDirection.NONE) return;

        List<BattleEntity> playerSide = new ArrayList<>();
        List<BattleEntity> monsterSide = new ArrayList<>();

        for (BattleEntity e : actors) {
            if (e instanceof Player || ctx.playerParty.contains(e)) {
                playerSide.add(e);
            } else {
                monsterSide.add(e);
            }
        }

        actors.clear();
        if (ctx.surpriseAttacker == SurpriseDirection.PLAYER_SURPRISE) {
            actors.addAll(playerSide);
            actors.addAll(monsterSide);
            ctx.addLog(LogType.INIT, "【偷袭】玩家方发起突袭，全阵营先行动。");
        } else {
            actors.addAll(monsterSide);
            actors.addAll(playerSide);
            ctx.addLog(LogType.INIT, "【伏击】怪物方发起伏击，全阵营先行动。");
        }
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
                ctx.currentTarget = ctx.getPrimaryMonsterTarget();
                playerActionPhase(ctx);
            } else if (actor instanceof Monster) {
                Monster m = (Monster) actor;
                ctx.currentTarget = ctx.player;
                ctx.monster = m;
                monsterActionPhaseFor(ctx, m);
            }

            checkDeath(ctx);
        }
    }

    // ====================== 7. 怪物行动阶段 ======================
    private void monsterActionPhaseFor(BattleContext ctx, Monster m) {
        List<RevealedIntent> revealed = ctx.monsterRevealedIntents.get(m.getEntityId());
        if (revealed == null || revealed.isEmpty()) {
            BattleAction fallback = BattleAction.normalAttack(m, ctx.player);
            executeBattleAction(ctx, fallback);
            return;
        }

        for (RevealedIntent ri : revealed) {
            if (ctx.isBattleEnded) break;
            ri.executed = true;

            BattleAction action = toBattleAction(ctx, m, ri.intent);
            if (action == null) continue;

            executeBattleAction(ctx, action);
        }
    }

    // ====================== 8. 玩家行动阶段 ======================
    private void playerActionPhase(BattleContext ctx) {
        ctx.currentActionPoints = ctx.player.getCurrentActionPoints();
        ctx.addLog(LogType.ROUND_INFO, "玩家回合，行动点: %d", ctx.currentActionPoints);

        if (ctx.currentActionPoints > 0 && !ctx.isBattleEnded) {
            Monster target = ctx.getPrimaryMonsterTarget();
            if (target != null) {
                executeBattleAction(ctx, BattleAction.normalAttack(ctx.player, target));
            }
            ctx.currentActionPoints = 0;
            ctx.player.setCurrentActionPoints(0);
        }
    }

    // ====================== 9. 回合结束阶段 ======================
    private void onRoundEnd(BattleContext ctx) {
        for (BattleEntity e : ctx.playerParty) {
            if (e == null || e.isDead()) continue;
            BuffManager.getInstance(context).triggerBuffs(e, ctx, BuffTriggerType.ON_ROUND_END);
            AffixManager.getInstance(context).triggerAffixes(e, ctx, AffixTriggerType.ON_ROUND_END);
            BuffManager.getInstance(context).tickBuffs(e);
        }
        BuffManager.getInstance(context).triggerBuffsForAllMonsters(ctx, BuffTriggerType.ON_ROUND_END);
        AffixManager.getInstance(context).triggerAffixesForAllMonsters(ctx, AffixTriggerType.ON_ROUND_END);
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

        double escapeChance = 0.2 + ((double) playerAttr.speed / Math.max(1, monsterAttr.speed) - 1) * 0.5;
        escapeChance = Math.max(0.1, Math.min(0.9, escapeChance));

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
        ctx.resetDamageData();
        ctx.currentActor = attacker;
        ctx.currentTarget = target;
        ctx.damageType = DamageType.PHYSICAL.name();

        String actorName = attacker.getName();
        String targetName = target.getName();
        ctx.addLog(LogType.ACTION, "[%s] 发动普通攻击。", actorName);

        AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_ATTACK);
        BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_ATTACK);

        AttributeSet attackerAttr = attacker.getFinalAttributes();
        AttributeSet targetAttr = target.getFinalAttributes();
        ctx.rawDamage = attackerAttr.physicalAtk;
        ctx.addLog(LogType.DAMAGE, "  基础物理伤害：%d", ctx.rawDamage);

        float hitChance = calculateHitChance(attackerAttr, targetAttr);
        ctx.isHit = RandomUtils.checkProbability(hitChance);
        ctx.isDodged = !ctx.isHit;
        ctx.addLog(LogType.DODGE_CRIT, "  命中判定：命中率=%.1f%% (命中%.1f%% - 闪避%.1f%%)",
                hitChance * 100f, attackerAttr.hitRate * 100f, targetAttr.dodgeRate * 100f);

        if (!ctx.isHit) {
            ctx.rawDamage = 0;
            ctx.finalDamage = 0;
            ctx.isCriticalHit = false;
            ctx.addLog(LogType.DODGE_CRIT, "  攻击未命中");
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_ATTACK_MISS);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_ATTACK_MISS);
            BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_DODGE);
            AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_DODGE);
            return;
        }

        float critChance = clampProbability(attackerAttr.physicalCritRate);
        ctx.isCriticalHit = RandomUtils.checkProbability(critChance);
        ctx.addLog(LogType.DODGE_CRIT, "  暴击判定：暴击率=%.1f%%", critChance * 100f);

        if (ctx.isCriticalHit) {
            ctx.rawDamage *= attackerAttr.physicalCritDmg;
            ctx.addLog(LogType.DODGE_CRIT, "  触发暴击！伤害提升至 %d", ctx.rawDamage);
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_CRIT);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_CRIT);
            BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_BEING_CRIT);
            AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_BEING_CRIT);
        }

        ctx.finalDamage = Math.max(1, ctx.rawDamage - targetAttr.physicalDef);
        ctx.addLog(LogType.DAMAGE, "  扣除物理防御(%d)，结算伤害：%d", targetAttr.physicalDef, ctx.finalDamage);

        BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_BEFORE_DAMAGE_TAKEN);
        AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_BEFORE_DAMAGE_TAKEN);

        ctx.finalDamage = applyCountBasedDamageReduction(ctx, target, ctx.finalDamage);
        ctx.finalDamage = applyShieldAbsorption(ctx, target, ctx.finalDamage);

        target.takeDamage(ctx.finalDamage);
        ctx.addLog(LogType.DAMAGE, "  %s受到 %d 点伤害。剩余HP：(%d/%d)",
                targetName, ctx.finalDamage, target.getCurrentHp(), targetAttr.maxHp);

        if (ctx.isHit) {
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_HIT);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_HIT);
        }

        BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_AFTER_DAMAGE_TAKEN);
        AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_AFTER_DAMAGE_TAKEN);

        if (target.isDead()) {
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_KILL);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_KILL);
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
    private boolean executeBattleAction(BattleContext ctx, BattleAction action) {
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

        actor.consumeActionPoints(action.getApCost());
        actor.setCurrentMp(Math.max(0, actor.getCurrentMp() - action.getMpCost()));

        switch (action.getType()) {
            case ATTACK:
                executeAttackAction(ctx, action, actor, target);
                return true;
            case ESCAPE:
                if (actor instanceof Monster) executeMonsterEscape(ctx);
                return true;
            case SKILL:
                SkillManager.getInstance(context).executeSkill(action.getActionRefId(), actor, target, ctx);
                ctx.addLog(LogType.ACTION, "[%s] 尝试释放技能 [%s]（TODO：技能系统接入中）",
                        actor.getName(), action.getDisplayName());
                return true;
            case ITEM:
                ctx.addLog(LogType.ACTION, "[%s] 尝试使用道具 [%s]（TODO：道具体系未接入）",
                        actor.getName(), action.getDisplayName());
                return true;
            default:
                return false;
        }
    }

    private void executeAttackAction(BattleContext ctx, BattleAction action, BattleEntity actor, BattleEntity target) {
        double multiplier = Math.max(0, action.getPowerMultiplier());
        executeNormalAttack(ctx, actor, target);

        if (!ctx.isHit || ctx.finalDamage <= 0 || Math.abs(multiplier - 1.0) < 0.0001) return;

        int adjustedDamage = Math.max(1, (int) Math.round(ctx.finalDamage * multiplier));
        int extraDamage = adjustedDamage - ctx.finalDamage;
        if (extraDamage <= 0) return;

        target.takeDamage(extraDamage);
        ctx.addLog(LogType.DAMAGE, "  动作倍率生效(%.2fx)，追加伤害：%d。", multiplier, extraDamage);
        ctx.finalDamage = adjustedDamage;
        checkDeath(ctx);
    }

    // ====================== 15. 怪物逃跑 ======================
    public boolean executeMonsterEscape(BattleContext ctx) {
        if (!(ctx.currentActor instanceof Monster)) return false;
        Monster actingMonster = (Monster) ctx.currentActor;
        ctx.addLog(LogType.ACTION, "怪物[%s]尝试逃跑...", actingMonster.getName());

        AttributeSet monsterAttr = actingMonster.getFinalAttributes();
        AttributeSet playerAttr = ctx.player.getFinalAttributes();

        double escapeChance = 0.2 + ((double) monsterAttr.speed / Math.max(1, playerAttr.speed) - 1) * 0.5;
        escapeChance = Math.max(0.1, Math.min(0.9, escapeChance));

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

    // ====================== 16. 防御结算 ======================
    private int applyCountBasedDamageReduction(BattleContext ctx, BattleEntity target, int incomingDamage) {
        if (incomingDamage <= 0) return 0;
        int remainingDamage = incomingDamage;
        for (BaseBuff buff : target.getActiveBuffList()) {
            if (!(buff instanceof DamageReductionBuff)) continue;
            remainingDamage = ((DamageReductionBuff) buff).reduceDamageForOneHit(remainingDamage, target, ctx);
            break;
        }
        return remainingDamage;
    }

    private int applyShieldAbsorption(BattleContext ctx, BattleEntity target, int incomingDamage) {
        if (incomingDamage <= 0) return 0;
        int remainingDamage = incomingDamage;
        for (BaseBuff buff : target.getActiveBuffList()) {
            if (!(buff instanceof ShieldBuff)) continue;
            if (remainingDamage <= 0) break;
            remainingDamage = ((ShieldBuff) buff).absorbDamage(remainingDamage, target, ctx);
        }
        return remainingDamage;
    }

    // ====================== 17. 死亡检查 ======================
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

    // ====================== 18. 战斗结算 ======================
    public void settleBattleResult(BattleContext ctx) {
        ctx.addLog(LogType.ROUND_INFO, "======== 战斗结算 ========");

        if (ctx.player != null) {
            BuffManager.getInstance(context).triggerBuffs(ctx.player, ctx, BuffTriggerType.ON_BATTLE_END);
            AffixManager.getInstance(context).triggerAffixes(ctx.player, ctx, AffixTriggerType.ON_BATTLE_END);
        }
        BuffManager.getInstance(context).triggerBuffsForAllMonsters(ctx, BuffTriggerType.ON_BATTLE_END);
        AffixManager.getInstance(context).triggerAffixesForAllMonsters(ctx, AffixTriggerType.ON_BATTLE_END);

        if (ctx.battleResult == BattleContext.BattleResult.VICTORY) {
            int baseExp = 0;
            int baseGold = 0;
            int monsterLevel = ctx.player.getLevel();
            for (Monster m : ctx.monsters) {
                if (m == null) continue;
                baseExp += m.getExpReward();
                baseGold += m.getGoldReward();
                monsterLevel = Math.max(monsterLevel, m.getLevel());
            }
            int playerLevel = ctx.player.getLevel();
            double expBonus = 1.0;
            if (playerLevel < monsterLevel) expBonus += 0.1 * (monsterLevel - playerLevel);
            int finalExp = (int) (baseExp * expBonus * ctx.player.getFinalAttributes().expBonus);
            ctx.player.gainExp(finalExp);

            int finalGold = (int) (baseGold * ctx.player.getFinalAttributes().goldBonus);
            ctx.addLog(LogType.RESULT, "获得战利品：\n  - 金币：+%d\n  - 经验：+%d", finalGold, finalExp);

            // 掉落物生成 → 存入待领取列表（玩家可选择拿取/全部拿取）
            java.util.List<com.example.treasure_and_battle.model.item.Item> drops =
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

    // ====================== 19. 工具方法 ======================
    private float calculateHitChance(AttributeSet attackerAttr, AttributeSet targetAttr) {
        return clampProbability(attackerAttr.hitRate - targetAttr.dodgeRate);
    }

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

    private float clampProbability(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
