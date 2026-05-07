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

        // 1.3 触发被动技能（战斗开始）
        triggerPassiveSkills(player, ctx);
        for (Monster m : monsters) {
            triggerPassiveSkills(m, ctx);
        }

        // 1.3 判定先手（功能清单第5点）
        determineTurnOrder(ctx);
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

        // 3.3 触发回合开始Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(ctx.currentActor, ctx, BuffTriggerType.ON_ROUND_START);
        AffixManager.getInstance(context).triggerAffixes(ctx.currentActor, ctx, AffixTriggerType.ON_ROUND_START);

        // 3.4 触发回合开始被动技能
        triggerPassiveSkills(ctx.currentActor, ctx);
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
                playerActionPhase(ctx);
            } else if (actor instanceof Monster) {
                monsterActionPhaseFor(ctx, (Monster) actor);
            }
        }
    }

    // ====================== 被动技能系统支持 ======================

    /**
     * 触发实体的所有被动技能
     * @param owner 被动技能的持有者
     * @param context 战斗上下文
     */
    /**
     * 触发战斗开始时的被动技能
     */
    public void triggerBattleStartPassiveSkills(BattleEntity owner, BattleContext context) {
        if (owner.getPassiveSkillList() == null || owner.getPassiveSkillList().isEmpty()) {
            return;
        }

        for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : owner.getPassiveSkillList()) {
            try {
                // 检查技能是否在战斗开始时触发
                if (passiveSkill.hasTriggerType(com.example.treasure_and_battle.model.skill.SkillTriggerType.ON_BATTLE_START)) {
                    passiveSkill.onBattleStart(owner, context);
                }
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] onBattleStart 触发失败: %s",
                    passiveSkill.getSkillName(), e.getMessage());
            }
        }
    }

    /**
     * 触发回合结束时的被动技能
     */
    public void triggerRoundEndPassiveSkills(BattleEntity owner, BattleContext context) {
        if (owner.getPassiveSkillList() == null || owner.getPassiveSkillList().isEmpty()) {
            return;
        }

        for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : owner.getPassiveSkillList()) {
            try {
                // 检查技能是否在回合结束时触发
                if (passiveSkill.hasTriggerType(com.example.treasure_and_battle.model.skill.SkillTriggerType.ON_ROUND_END)) {
                    passiveSkill.onRoundEnd(owner, context);
                }
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] onRoundEnd 触发失败: %s",
                    passiveSkill.getSkillName(), e.getMessage());
            }
        }
    }

    /**
     * 触发回合开始时的被动技能
     */
    public void triggerRoundStartPassiveSkills(BattleEntity owner, BattleContext context) {
        if (owner.getPassiveSkillList() == null || owner.getPassiveSkillList().isEmpty()) {
            return;
        }

        for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : owner.getPassiveSkillList()) {
            try {
                // 检查技能是否在回合开始时触发
                if (passiveSkill.hasTriggerType(com.example.treasure_and_battle.model.skill.SkillTriggerType.ON_ROUND_START)) {
                    passiveSkill.onRoundStart(owner, context);
                }
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] onRoundStart 触发失败: %s",
                    passiveSkill.getSkillName(), e.getMessage());
            }
        }
    }

    public void triggerPassiveSkills(BattleEntity owner, BattleContext context) {
        // 保留原方法用于兼容，调用战斗开始触发
        triggerBattleStartPassiveSkills(owner, context);
    }

    /**
     * 触发攻击相关的被动技能
     * @param attacker 攻击者
     * @param target 目标
     * @param context 战斗上下文
     */
    public void triggerAttackPassiveSkills(BattleEntity attacker, BattleEntity target, BattleContext context) {
        // 触发攻击者的被动技能
        if (attacker.getPassiveSkillList() != null) {
            for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : attacker.getPassiveSkillList()) {
                if (passiveSkill.hasTriggerType(com.example.treasure_and_battle.model.skill.SkillTriggerType.ON_ATTACK)) {
                    try {
                        passiveSkill.onAttack(attacker, target, context);
                    } catch (Exception e) {
                        context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                            passiveSkill.getSkillName(), e.getMessage());
                    }
                }
            }
        }

        // 触发目标的被动技能（被攻击）
        if (target.getPassiveSkillList() != null) {
            for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : target.getPassiveSkillList()) {
                if (passiveSkill.hasTriggerType(com.example.treasure_and_battle.model.skill.SkillTriggerType.ON_ATTACKED)) {
                    try {
                        passiveSkill.onAttacked(target, attacker, context);
                    } catch (Exception e) {
                        context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                            passiveSkill.getSkillName(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 触发造成伤害相关的被动技能（可能修改伤害值）
     * @param attacker 攻击者
     * @param target 目标
     * @param damage 原始伤害
     * @param context 战斗上下文
     * @return 修改后的伤害值
     */
    private int triggerBeforeDamageDealtPassiveSkills(BattleEntity attacker, BattleEntity target, int damage, BattleContext context) {
        int modifiedDamage = damage;

        if (attacker.getPassiveSkillList() != null) {
            for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : attacker.getPassiveSkillList()) {
                if (passiveSkill.hasTriggerType(com.example.treasure_and_battle.model.skill.SkillTriggerType.ON_BEFORE_DAMAGE_DEALT)) {
                    try {
                        modifiedDamage = passiveSkill.onBeforeDamageDealt(attacker, target, modifiedDamage, context);
                    } catch (Exception e) {
                        context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                            passiveSkill.getSkillName(), e.getMessage());
                    }
                }
            }
        }

        return modifiedDamage;
    }

    /**
     * 触发造成伤害后的被动技能
     * @param attacker 攻击者
     * @param target 目标
     * @param damage 实际造成的伤害
     * @param context 战斗上下文
     */
    public void triggerAfterDamageDealtPassiveSkills(BattleEntity attacker, BattleEntity target, int damage, BattleContext context) {
        if (attacker.getPassiveSkillList() != null) {
            for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : attacker.getPassiveSkillList()) {
                if (passiveSkill.hasTriggerType(com.example.treasure_and_battle.model.skill.SkillTriggerType.ON_AFTER_DAMAGE_DEALT)) {
                    try {
                        passiveSkill.onAfterDamageDealt(attacker, target, damage, context);
                    } catch (Exception e) {
                        context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                            passiveSkill.getSkillName(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 触发受到伤害相关的被动技能（可能修改伤害值）
     * @param target 目标
     * @param attacker 攻击者
     * @param damage 原始伤害
     * @param context 战斗上下文
     * @return 修改后的伤害值
     */
    public int triggerBeforeDamageReceivedPassiveSkills(BattleEntity target, BattleEntity attacker, int damage, BattleContext context) {
        int modifiedDamage = damage;

        if (target.getPassiveSkillList() != null) {
            for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : target.getPassiveSkillList()) {
                if (passiveSkill.hasTriggerType(com.example.treasure_and_battle.model.skill.SkillTriggerType.ON_BEFORE_DAMAGE_RECEIVED)) {
                    try {
                        modifiedDamage = passiveSkill.onBeforeDamageReceived(target, attacker, modifiedDamage, context);
                    } catch (Exception e) {
                        context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                            passiveSkill.getSkillName(), e.getMessage());
                    }
                }
            }
        }

        return modifiedDamage;
    }

    /**
     * 触发受到伤害后的被动技能
     * @param target 目标
     * @param attacker 攻击者
     * @param damage 实际受到的伤害
     * @param context 战斗上下文
     */
    public void triggerAfterDamageReceivedPassiveSkills(BattleEntity target, BattleEntity attacker, int damage, BattleContext context) {
        if (target.getPassiveSkillList() != null) {
            for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : target.getPassiveSkillList()) {
                if (passiveSkill.hasTriggerType(com.example.treasure_and_battle.model.skill.SkillTriggerType.ON_AFTER_DAMAGE_RECEIVED)) {
                    try {
                        passiveSkill.onAfterDamageReceived(target, attacker, damage, context);
                    } catch (Exception e) {
                        context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                            passiveSkill.getSkillName(), e.getMessage());
                    }
                }
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
                            java.util.List<BattleEntity> targets, BattleContext context) {
        // 设置当前战斗上下文
        this.currentBattleContext = context;

        // 1. 消耗资源
        skill.applyCastCost(caster);

        // 2. 触发技能效果
        skill.onCast(caster, targets, this);

        // 3. 设置冷却
        skill.resetCooldown();

        // 4. 记录日志
        context.addLog(LogType.ACTION, "【%s】[%s] 对目标施放了 [%s]",
            caster.getClass().getSimpleName(), caster.getName(), skill.getSkillName());

        // 清除当前战斗上下文
        this.currentBattleContext = null;
    }

    /**
     * 造成物理伤害（用于技能）
     * @param attacker 攻击者
     * @param target 目标
     * @param baseDamage 基础伤害
     * @param context 战斗上下文
     * @return 实际造成的伤害
     */
    public int dealPhysicalDamage(BattleEntity attacker, BattleEntity target,
                                   int baseDamage, BattleContext context) {
        context.resetDamageData();
        context.currentActor = attacker;
        context.currentTarget = target;
        context.damageType = DamageType.PHYSICAL.name();

        AttributeSet attackerAttr = attacker.getFinalAttributes();
        AttributeSet targetAttr = target.getFinalAttributes();

        // 计算基础伤害
        context.rawDamage = baseDamage;

        // 计算命中（技能通常100%命中，除非有特殊机制）
        float hitChance = calculateHitChance(attackerAttr, targetAttr);
        context.isHit = RandomUtils.checkProbability(hitChance);

        if (!context.isHit) {
            context.rawDamage = 0;
            context.finalDamage = 0;
            context.isCriticalHit = false;
            return 0;
        }

        // 计算暴击
        float critChance = clampProbability(attackerAttr.physicalCritRate);
        context.isCriticalHit = RandomUtils.checkProbability(critChance);

        if (context.isCriticalHit) {
            context.rawDamage *= attackerAttr.physicalCritDmg;
        }

        // 计算最终伤害（扣除防御）
        context.finalDamage = Math.max(1, context.rawDamage - targetAttr.physicalDef);

        // 触发被动技能：造成伤害前
        context.finalDamage = triggerBeforeDamageDealtPassiveSkills(attacker, target, context.finalDamage, context);

        // 触发buff事件：受到伤害前（可修改伤害）
        context.finalDamage = BuffManager.getInstance(this.context).triggerBeforeDamageReceivedEvent(target, attacker, context.finalDamage, context);

        // 应用防御机制
        BuffManager.getInstance(this.context).triggerBuffs(target, context, BuffTriggerType.ON_BEFORE_DAMAGE_TAKEN);
        context.finalDamage = applyCountBasedDamageReduction(context, target, context.finalDamage);
        context.finalDamage = applyShieldAbsorption(context, target, context.finalDamage);

        // 造成伤害
        target.takeDamage(context.finalDamage);

        // 触发buff事件：被攻击
        BuffManager.getInstance(this.context).triggerAttackedEvent(target, attacker, context);

        // 触发buff事件：受到伤害后（HP扣除之后）
        BuffManager.getInstance(this.context).triggerAfterDamageReceivedEvent(target, attacker, context.finalDamage, context);

        // 触发被动技能：造成伤害后
        triggerAfterDamageDealtPassiveSkills(attacker, target, context.finalDamage, context);

        // 触发buff事件：造成伤害后
        BuffManager.getInstance(this.context).triggerAfterDamageDealtEvent(attacker, target, context.finalDamage, context);

        // 触发命中后事件
        BuffManager.getInstance(this.context).triggerBuffs(attacker, context, BuffTriggerType.ON_HIT);

        return context.finalDamage;
    }

    /**
     * 造成穿甲伤害（无视防御与护盾）
     * @param attacker 攻击者
     * @param target 目标
     * @param piercingDamage 破甲伤害值
     * @param context 战斗上下文
     * @return 实际造成的伤害
     */
    public int dealPiercingDamage(BattleEntity attacker, BattleEntity target,
                                   int piercingDamage, BattleContext context) {
        // TODO：穿甲伤害无视防御与护盾，直接造成伤害，现在暂且作为真伤处理
        target.takeDamage(piercingDamage);

        context.addLog(LogType.DAMAGE,
            "  破甲伤害：%d（无视防御与护盾）", piercingDamage);

        return piercingDamage;
    }

    /**
     * 造成真实伤害（无视防御、护盾、减伤等一切防御机制）
     * @param target 目标
     * @param trueDamage 真实伤害值
     * @param context 战斗上下文
     */
    public void dealTrueDamage(BattleEntity target, int trueDamage, BattleContext context) {
        target.takeDamage(trueDamage);

        context.addLog(LogType.DAMAGE,
            "  真实伤害：%d（无视一切防御）", trueDamage);
    }

    /**
     * 造成法术伤害（用于技能）
     */
    public int dealMagicalDamage(BattleEntity attacker, BattleEntity target,
                                  int baseDamage, BattleContext context) {
        context.resetDamageData();
        context.currentActor = attacker;
        context.currentTarget = target;
        context.damageType = DamageType.MAGICAL.name();

        AttributeSet attackerAttr = attacker.getFinalAttributes();
        AttributeSet targetAttr = target.getFinalAttributes();

        // 计算基础伤害
        context.rawDamage = baseDamage;

        // 计算命中
        float hitChance = calculateHitChance(attackerAttr, targetAttr);
        context.isHit = RandomUtils.checkProbability(hitChance);

        if (!context.isHit) {
            context.rawDamage = 0;
            context.finalDamage = 0;
            context.isCriticalHit = false;
            return 0;
        }

        // 计算暴击
        float critChance = clampProbability(attackerAttr.magicalCritRate);
        context.isCriticalHit = RandomUtils.checkProbability(critChance);

        if (context.isCriticalHit) {
            context.rawDamage *= attackerAttr.magicalCritDmg;
        }

        // 计算最终伤害
        context.finalDamage = Math.max(1, context.rawDamage - targetAttr.magicalDef);

        // 应用防御机制
        BuffManager.getInstance(this.context).triggerBuffs(target, context, BuffTriggerType.ON_BEFORE_DAMAGE_TAKEN);
        context.finalDamage = applyCountBasedDamageReduction(context, target, context.finalDamage);
        context.finalDamage = applyShieldAbsorption(context, target, context.finalDamage);

        // 造成伤害
        target.takeDamage(context.finalDamage);

        // 触发命中后事件
        BuffManager.getInstance(this.context).triggerBuffs(attacker, context, BuffTriggerType.ON_HIT);

        return context.finalDamage;
    }

    /**
     * 施加buff到目标
     */
    public void applyBuff(BattleEntity target, com.example.treasure_and_battle.buff.BaseBuff buff) {
        BuffManager.getInstance(this.context).addBuff(target, buff);
        target.markAttributeCacheDirty();
    }

    // ====================== 【核心规则实现】功能清单具体逻辑 ======================

    // 5. 先手规则判定
    private void determineTurnOrder(BattleContext ctx) {
        if (ctx.surpriseAttacker != SurpriseDirection.NONE) {
            ctx.addLog(LogType.INIT, "【偷袭】一方发起突袭，获得先手行动权。");
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
            triggerRoundEndPassiveSkills(e, ctx);
            BuffManager.getInstance(context).triggerBuffs(e, ctx, BuffTriggerType.ON_ROUND_END);
            AffixManager.getInstance(context).triggerAffixes(e, ctx, AffixTriggerType.ON_ROUND_END);
            BuffManager.getInstance(context).onRoundEnd(e, ctx);
            BuffManager.getInstance(context).tickBuffs(e);
        }
        for (Monster m : ctx.getAliveMonsters()) {
            if (m == null) continue;
            triggerRoundEndPassiveSkills(m, ctx);
            BuffManager.getInstance(context).onRoundEnd(m, ctx);
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
        if (incomingDamage <= 0) {
            return 0;
        }

        // 检测护盾破碎：记录吸收前的护盾状态
        boolean hadShieldBefore = hasShield(target);

        int remainingDamage = incomingDamage;
        for (BaseBuff buff : target.getActiveBuffList()) {
            if (!(buff instanceof ShieldBuff)) continue;
            if (remainingDamage <= 0) break;
            remainingDamage = ((ShieldBuff) buff).absorbDamage(remainingDamage, target, ctx);
        }

        // 检测护盾是否刚刚破碎
        boolean hasShieldAfter = hasShield(target);
        if (hadShieldBefore && !hasShieldAfter && remainingDamage < incomingDamage) {
            // 护盾刚刚破碎！触发被动技能
            triggerShieldBreakPassiveSkills(target, ctx.currentActor, ctx);
        }

        return remainingDamage;
    }

    /**
     * 检查实体是否有护盾
     */
    private boolean hasShield(BattleEntity entity) {
        for (BaseBuff buff : entity.getActiveBuffList()) {
            if (buff instanceof ShieldBuff) {
                ShieldBuff shieldBuff = (ShieldBuff) buff;
                return shieldBuff.getStackCount() > 0;
            }
        }
        return false;
    }

    /**
     * 触发护盾破碎被动技能
     */
    private void triggerShieldBreakPassiveSkills(BattleEntity owner, BattleEntity attacker, BattleContext context) {
        for (com.example.treasure_and_battle.skill.passive.PassiveSkill passiveSkill : owner.getPassiveSkillList()) {
            try {
                passiveSkill.onShieldBreak(owner, attacker, context, this);
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM,
                    "被动技能 [%s] onShieldBreak 触发失败: %s",
                    passiveSkill.getSkillName(), e.getMessage());
            }
        }
    }

    /**
     * 获取所有敌人（根据当前行动者判断）
     * @param owner 护盾所有者
     * @param context 战斗上下文
     * @return 敌人列表
     */
    public java.util.List<BattleEntity> getAllEnemies(BattleEntity owner, BattleContext context) {
        java.util.List<BattleEntity> enemies = new java.util.ArrayList<>();

        if (owner instanceof com.example.treasure_and_battle.model.entity.Player) {
            // 玩家敌人是怪物
            if (context.monster != null && !context.monster.isDead()) {
                enemies.add(context.monster);
            }
        } else if (owner instanceof com.example.treasure_and_battle.model.entity.Monster) {
            // 怪物敌人是玩家
            if (context.player != null && !context.player.isDead()) {
                enemies.add(context.player);
            }
        }

        return enemies;
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
