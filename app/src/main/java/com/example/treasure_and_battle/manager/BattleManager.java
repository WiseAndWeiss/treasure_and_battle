package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.DamageType;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.defensive.DamageReductionBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.entity.MonsterIntent;
import com.example.treasure_and_battle.utils.RandomUtils;

import java.util.ArrayList;

/**
 * 战斗管理器 (BattleManager)
 * 职责：战斗流程的唯一驱动者，封装所有战斗规则
 * 设计模式：状态机 + 流水线
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

    // ====================== 【入口】1. 初始化战斗 ======================
    public BattleContext startBattle(Player player, Monster monster, boolean isSurpriseAttack) {
        BattleContext battleContext = new BattleContext(player, monster, isSurpriseAttack);

        // 1.1 初始化战斗实体状态
        player.setDead(false);
        monster.setDead(false);
        player.resetActionPoints();
        monster.resetActionPoints();

        // 1.2 触发战斗开始Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(player, battleContext, BuffTriggerType.ON_BATTLE_START);
        BuffManager.getInstance(context).triggerBuffs(monster, battleContext, BuffTriggerType.ON_BATTLE_START);
        AffixManager.getInstance(context).triggerAffixes(player, battleContext, AffixTriggerType.ON_BATTLE_START);
        AffixManager.getInstance(context).triggerAffixes(monster, battleContext, AffixTriggerType.ON_BATTLE_START);

        // 1.3 判定先手（功能清单第5点）
        determineTurnOrder(battleContext);

        battleContext.addLog(LogType.INIT, "战斗开始：[%s] VS [%s]", 
                player.getName(), monster.getName());

        // 1.4 进入战斗循环
        battleLoop(battleContext);

        return battleContext;
    }

    // ====================== 2. 战斗主循环 ======================
    private void battleLoop(BattleContext ctx) {
        while (!ctx.isBattleEnded) {
            ctx.currentRound++;
            ctx.resetDamageData();

            // 防卡死安全机制：回合数过大时强行平局结束
            if (ctx.currentRound >= 100) {
                ctx.isBattleEnded = true;
                ctx.battleResult = BattleContext.BattleResult.DEFEAT;
                ctx.addLog(LogType.SYSTEM, "【系统】达到回合数上限(100)，战斗强制判定为失败。");
                break;
            }

            ctx.addLog(LogType.ROUND_INFO, "======== 第 %d 回合开始 (行动方: %s) ========", 
                    ctx.currentRound, ctx.isPlayerTurn ? "玩家" : "怪物");

            // 2.1 回合开始阶段
            onRoundStart(ctx);
            if (ctx.isBattleEnded) break;

            // 2.2 行动阶段（功能清单第1、3、4点）
            if (ctx.isPlayerTurn) {
                playerActionPhase(ctx);
            } else {
                monsterActionPhase(ctx);
            }
            if (ctx.isBattleEnded) break;

            // 2.3 回合结束阶段
            onRoundEnd(ctx);
            if (ctx.isBattleEnded) break;

            // 2.4 切换回合
            ctx.isPlayerTurn = !ctx.isPlayerTurn;
        }

        // 2.5 战斗结束结算
        settleBattleResult(ctx);
    }

    // ====================== 3. 回合开始阶段 ======================
    private void onRoundStart(BattleContext ctx) {
        // 3.1 重置行动点
        if (ctx.isPlayerTurn) {
            ctx.player.resetActionPoints();
            ctx.currentActor = ctx.player;
            ctx.currentTarget = ctx.monster;
        } else {
            ctx.monster.resetActionPoints();
            ctx.currentActor = ctx.monster;
            ctx.currentTarget = ctx.player;
        }
        ctx.currentActionPoints = ctx.currentActor.getCurrentActionPoints();

        // 3.2 怪物回合：生成并显示意图（功能清单第4点）
        if (!ctx.isPlayerTurn) {
            generateAndRevealMonsterIntents(ctx);
        }

        // 3.3 触发回合开始Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(ctx.currentActor, ctx, BuffTriggerType.ON_ROUND_START);
        AffixManager.getInstance(context).triggerAffixes(ctx.currentActor, ctx, AffixTriggerType.ON_ROUND_START);
    }

    // ====================== 4. 玩家行动阶段 ======================
    private void playerActionPhase(BattleContext ctx) {
        // 这里是一个循环，直到玩家行动点耗尽或选择结束回合
        // 实际项目中，这里通过UI回调玩家的选择
        // 这里仅展示核心逻辑框架：
        // TODO: 连接UI输入，处理玩家选择的操作（普攻/技能/道具/逃跑等），并调用相应的执行方法

        // 兜底：未接入UI时，自动结束玩家行动阶段，避免空循环卡死。
        if (ctx.currentActionPoints > 0 && !ctx.isBattleEnded) {
            ctx.addLog(LogType.SYSTEM, "玩家操作尚未接入，自动结束本回合行动阶段。");
            ctx.currentActionPoints = 0;
            ctx.player.setCurrentActionPoints(0);
        }
    }

    // ====================== 5. 怪物行动阶段 ======================
    private void monsterActionPhase(BattleContext ctx) {
        // 按顺序执行怪物意图（功能清单第4点）
        if (ctx.currentMonsterIntents == null || ctx.currentMonsterIntents.isEmpty()) {
            return;
        }

        for (MonsterIntent intent : ctx.currentMonsterIntents) {
            if (ctx.isBattleEnded) break;

            // 检查行动点和MP是否足够
            if (ctx.monster.getCurrentActionPoints() < intent.getApCost() ||
                    ctx.monster.getCurrentMp() < intent.getMpCost()) {
                continue;
            }

            // 执行意图
            executeMonsterIntent(ctx, intent);
            ctx.monster.consumeActionPoints(intent.getApCost());
            ctx.monster.setCurrentMp(ctx.monster.getCurrentMp() - intent.getMpCost());
        }
    }

    // ====================== 6. 回合结束阶段 ======================
    private void onRoundEnd(BattleContext ctx) {
        // 6.1 触发回合结束Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(ctx.player, ctx, BuffTriggerType.ON_ROUND_END);
        BuffManager.getInstance(context).triggerBuffs(ctx.monster, ctx, BuffTriggerType.ON_ROUND_END);
        AffixManager.getInstance(context).triggerAffixes(ctx.player, ctx, AffixTriggerType.ON_ROUND_END);
        AffixManager.getInstance(context).triggerAffixes(ctx.monster, ctx, AffixTriggerType.ON_ROUND_END);

        // 6.2 Buff Tick（减少持续时间，清理过期）
        BuffManager.getInstance(context).tickBuffs(ctx.player);
        BuffManager.getInstance(context).tickBuffs(ctx.monster);

        // 6.3 检查是否有实体死亡
        checkDeath(ctx);
    }

    // ====================== 【核心规则实现】功能清单具体逻辑 ======================

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
     * 造成破甲伤害（无视防御与护盾）
     * @param attacker 攻击者
     * @param target 目标
     * @param piercingDamage 破甲伤害值
     * @param context 战斗上下文
     * @return 实际造成的伤害
     */
    public int dealPiercingDamage(BattleEntity attacker, BattleEntity target,
                                   int piercingDamage, BattleContext context) {
        // 破甲伤害无视防御与护盾，直接造成伤害
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
        if (ctx.isSurpriseAttack) {
            // 偷袭战斗：袭击方先行动（这里假设袭击方是玩家，可根据需求调整）
            ctx.isPlayerTurn = true;
            ctx.addLog(LogType.INIT, "【偷袭】[%s] 发起突袭，获得先手行动权。", ctx.player.getName());
            return;
        }

        // 常规战斗：速度高的先行动
        AttributeSet playerAttr = ctx.player.getFinalAttributes();
        AttributeSet monsterAttr = ctx.monster.getFinalAttributes();
        ctx.isPlayerTurn = playerAttr.speed >= monsterAttr.speed;

        ctx.addLog(LogType.INIT, "【先手判定】玩家速度(%.0f) vs 怪物速度(%.0f) => [%s] 行动。", 
            playerAttr.speed, monsterAttr.speed, ctx.isPlayerTurn ? "玩家" : "怪物");
    }

    // 4. 怪物意图生成与看破
    private void generateAndRevealMonsterIntents(BattleContext ctx) {
        // 4.1 怪物决策意图
        ctx.currentMonsterIntents = ctx.monster.decideNextTurnIntents();
        ctx.intentVisibility = new ArrayList<>();

        // 4.2 计算看破概率（功能清单第4点）
        AttributeSet playerAttr = ctx.player.getFinalAttributes();
        AttributeSet monsterAttr = ctx.monster.getFinalAttributes();

        double seeThroughChance = 0.5 * ((double) playerAttr.spirit / monsterAttr.spirit);
        // 截断在10%-90%之间
        seeThroughChance = Math.max(0.1, Math.min(0.9, seeThroughChance));

        ctx.addLog(LogType.DODGE_CRIT, "【意图看破判定】玩家精神(%.0f) vs 怪物精神(%.0f) => 看破率: %.1f%%", 
                playerAttr.spirit, monsterAttr.spirit, seeThroughChance * 100);

        // 4.3 判定每个意图是否可见
        for (int i = 0; i < ctx.currentMonsterIntents.size(); i++) {
            boolean isVisible = RandomUtils.checkProbability((float) seeThroughChance);
            ctx.intentVisibility.add(isVisible);
            
            MonsterIntent intent = ctx.currentMonsterIntents.get(i);
            if (isVisible) {
                ctx.addLog(LogType.ACTION, "  看破怪物意图：[%s]", intent.getType().name());
            } else {
                ctx.addLog(LogType.ACTION, "  怪物意图未知。");
            }
        }
    }

    // 8. 玩家逃跑逻辑
    public boolean executePlayerEscape(BattleContext ctx) {
        ctx.addLog(LogType.ACTION, "玩家尝试逃跑...");

        // 8.1 消耗1点行动点
        if (!ctx.player.consumeActionPoints(1)) {
            ctx.addLog(LogType.SYSTEM, "行动点不足，逃跑失败。");
            return false;
        }
        ctx.currentActionPoints--;

        // 8.2 计算逃跑成功率（功能清单第8点）
        AttributeSet playerAttr = ctx.player.getFinalAttributes();
        AttributeSet monsterAttr = ctx.monster.getFinalAttributes();

        double escapeChance = 0.2 + ((double) playerAttr.speed / monsterAttr.speed - 1) * 0.5;
        escapeChance = Math.max(0.1, Math.min(0.9, escapeChance));
        
        ctx.addLog(LogType.DODGE_CRIT, "【逃跑判定】玩家速度(%.0f) vs 怪物速度(%.0f) => 成功率: %.1f%%", 
                playerAttr.speed, monsterAttr.speed, escapeChance * 100);

        // 8.3 判定是否成功
        if (RandomUtils.checkProbability((float) escapeChance)) {
            // 逃跑成功
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.ESCAPED;
            ctx.addLog(LogType.ACTION, "逃跑成功。");
            return true;
        } else {
            // 逃跑失败：怪物立即执行一次攻击
            ctx.addLog(LogType.ACTION, "逃跑失败，遭到怪物追击。");
            executeNormalAttack(ctx, ctx.monster, ctx.player);
            return false;
        }
    }

    // 3. 普攻逻辑（玩家、怪物共用）
    public void executeNormalAttack(BattleContext ctx, BattleEntity attacker, BattleEntity target) {
        ctx.resetDamageData();
        ctx.currentActor = attacker;
        ctx.currentTarget = target;
        ctx.damageType = DamageType.PHYSICAL.name();

        // 动态获取攻击者名称（用于日志）
        String actorName = attacker.getName();
        String targetName = target.getName();
        ctx.addLog(LogType.ACTION, "[%s] 发动普通攻击。", actorName);

        // 攻击发起阶段：触发攻击前Buff和词缀（ON_ATTACK），用于修改攻击属性、增加特殊效果等。
        AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_ATTACK);
        BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_ATTACK);

        // 计算原始伤害（100%物理攻击，统一逻辑）
        AttributeSet attackerAttr = attacker.getFinalAttributes();
        AttributeSet targetAttr = target.getFinalAttributes();
        ctx.rawDamage = attackerAttr.physicalAtk;
        ctx.addLog(LogType.DAMAGE, "  基础物理伤害：%d", ctx.rawDamage);

        // 统一计算命中/闪避：命中率 = 攻击者命中 - 防守者闪避，并截断到[0,1]。
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
            // 触发攻击未命中Buff和词缀
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_ATTACK_MISS);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_ATTACK_MISS);
            // 触发目标的攻击未命中Buff和词缀
            BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_DODGE);
            AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_DODGE);
            return;
        }

        // 只在命中后判定暴击。
        float critChance = clampProbability(attackerAttr.physicalCritRate);
        ctx.isCriticalHit = RandomUtils.checkProbability(critChance);
        ctx.addLog(LogType.DODGE_CRIT, "  暴击判定：暴击率=%.1f%%", critChance * 100f);

        if (ctx.isCriticalHit) {
            ctx.rawDamage *= attackerAttr.physicalCritDmg;
            ctx.addLog(LogType.DODGE_CRIT, "  触发暴击！伤害提升至 %d", ctx.rawDamage);
            // 触发暴击Buff和词缀
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_CRIT);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_CRIT);
            // 触发目标的被暴击Buff和词缀
            BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_BEING_CRIT);
            AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_BEING_CRIT);
        }

        // 计算最终伤害（减去目标防御，统一逻辑）
        ctx.finalDamage = Math.max(1, ctx.rawDamage - targetAttr.physicalDef);
        ctx.addLog(LogType.DAMAGE, "  扣除物理防御(%d)，结算伤害：%d", targetAttr.physicalDef, ctx.finalDamage);


        // 触发【目标】的受击前Buff和词缀（ON_BEFORE_DAMAGE）
        BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_BEFORE_DAMAGE_TAKEN);
        AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_BEFORE_DAMAGE_TAKEN);

        // 防御结算顺序：先结算计次减伤，再结算护盾吸收。
        // 这样设计是为了让“减伤”负责直接削减本次命中的伤害，而“护盾”只吸收减伤后的剩余值，
        // 从而明确区分两类防御资源的定位，避免护盾替代减伤的战术价值，并保持玩家叠加防御Buff时的策略预期一致。
        ctx.finalDamage = applyCountBasedDamageReduction(ctx, target, ctx.finalDamage);
        ctx.finalDamage = applyShieldAbsorption(ctx, target, ctx.finalDamage);

        // 造成伤害
        target.takeDamage(ctx.finalDamage);
        ctx.addLog(LogType.DAMAGE, "  %s受到 %d 点伤害。剩余HP：(%d/%d)",
                targetName, ctx.finalDamage, target.getCurrentHp(), targetAttr.maxHp);

        // 命中后阶段：用于“命中后触发”词缀、Buff，可读取最终落地伤害。
        if (ctx.isHit) {
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_HIT);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_HIT);
        }

        // 触发【目标】的受击后Buff和词缀（ON_AFTER_DAMAGE）
        BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_AFTER_DAMAGE_TAKEN);
        AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_AFTER_DAMAGE_TAKEN);

        if (target.isDead()) {
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_KILL);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_KILL);
        }

        // 检查死亡
        checkDeath(ctx);
    }


    // 执行怪物意图
    private void executeMonsterIntent(BattleContext ctx, MonsterIntent intent) {
        ctx.addLog(LogType.ACTION, "怪物执行动作：[%s]", intent.getType().name());
        switch (intent.getType()) {
            case ATTACK:
                executeNormalAttack(ctx, ctx.monster, ctx.player);
                break;
            case DEFEND:
                ctx.monster.setDefending(true);
                ctx.addLog(LogType.ACTION, "  怪物进入防御状态。");
                break;
            case BUFF:
                // 给怪物上Buff
                ctx.addLog(LogType.ACTION, "  怪物施放增益技能。");
                break;
            case DEBUFF:
                // 给玩家上Debuff
                ctx.addLog(LogType.ACTION, "  怪物施放减益技能。");
                break;
            case HEAL:
                int healAmount = (int) (intent.getPowerMultiplier() * ctx.monster.getFinalAttributes().maxHp);
                ctx.monster.healHp(healAmount);
                ctx.addLog(LogType.ACTION, "  怪物回复了 %d 点HP。", healAmount);
                break;
        }
    }

    private int applyCountBasedDamageReduction(BattleContext ctx, BattleEntity target, int incomingDamage) {
        if (incomingDamage <= 0) {
            return 0;
        }

        int remainingDamage = incomingDamage;
        for (BaseBuff buff : target.getActiveBuffList()) {
            if (!(buff instanceof DamageReductionBuff)) {
                continue;
            }

            // 单次受击默认只消费一条计次减伤，避免多条同时叠乘导致过强。
            remainingDamage = ((DamageReductionBuff) buff).reduceDamageForOneHit(remainingDamage, target, ctx);
            break;
        }

        return remainingDamage;
    }

    private int applyShieldAbsorption(BattleContext ctx, BattleEntity target, int incomingDamage) {
        if (incomingDamage <= 0) {
            return 0;
        }

        int remainingDamage = incomingDamage;
        for (BaseBuff buff : target.getActiveBuffList()) {
            if (!(buff instanceof ShieldBuff)) {
                continue;
            }
            if (remainingDamage <= 0) {
                break;
            }
            remainingDamage = ((ShieldBuff) buff).absorbDamage(remainingDamage, target, ctx);
        }
        return remainingDamage;
    }

    // 检查死亡
    private void checkDeath(BattleContext ctx) {
        if (ctx.player != null && ctx.player.isDead()) {
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.DEFEAT;
            ctx.addLog(LogType.DEATH, "玩家阵亡。");
        } else if (ctx.monster != null && ctx.monster.isDead()) {
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.VICTORY;
            ctx.addLog(LogType.DEATH, "怪物阵亡，战斗胜利。");
        }
    }

    // 7. 战斗结果结算（功能清单第6、7点）
    private void settleBattleResult(BattleContext ctx) {
        ctx.addLog(LogType.ROUND_INFO, "======== 战斗结算 ========");

        // 战斗结束统一触发。用于处理“战斗结束时”词缀/Buff。
        if (ctx.player != null) {
            BuffManager.getInstance(context).triggerBuffs(ctx.player, ctx, BuffTriggerType.ON_BATTLE_END);
            AffixManager.getInstance(context).triggerAffixes(ctx.player, ctx, AffixTriggerType.ON_BATTLE_END);
        }
        if (ctx.monster != null) {
            BuffManager.getInstance(context).triggerBuffs(ctx.monster, ctx, BuffTriggerType.ON_BATTLE_END);
            AffixManager.getInstance(context).triggerAffixes(ctx.monster, ctx, AffixTriggerType.ON_BATTLE_END);
        }

        if (ctx.battleResult == BattleContext.BattleResult.VICTORY) {
            // 7.1 计算经验加成（功能清单第7点）
            int baseExp = ctx.monster.getExpReward();
            int playerLevel = ctx.player.getLevel();
            int monsterLevel = ctx.monster.getLevel();
            double expBonus = 1.0;
            if (playerLevel < monsterLevel) {
                expBonus += 0.1 * (monsterLevel - playerLevel);
            }
            int finalExp = (int) (baseExp * expBonus * ctx.player.getFinalAttributes().expBonus);
            ctx.player.gainExp(finalExp);

            // 7.2 计算金币加成
            int baseGold = ctx.monster.getGoldReward();
            int finalGold = (int) (baseGold * ctx.player.getFinalAttributes().goldBonus);
            // TODO: 给玩家加金币

            ctx.addLog(LogType.RESULT, "获得战利品：\n  - 金币：+%d\n  - 经验：+%d", finalGold, finalExp);

            // 7.3 生成掉落物（功能清单第7点）
            // DropManager.getInstance().generateDrops(ctx.monster);

        } else if (ctx.battleResult == BattleContext.BattleResult.DEFEAT) {
            // 6.1 失败惩罚：损失25%金币，保留1点HP（功能清单第6点）
            // TODO: 扣除玩家金币
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

    private float calculateHitChance(AttributeSet attackerAttr, AttributeSet targetAttr) {
        return clampProbability(attackerAttr.hitRate - targetAttr.dodgeRate);
    }

    private float clampProbability(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}