package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.DamageConfig;
import com.example.treasure_and_battle.battle.DamageSource;
import com.example.treasure_and_battle.battle.DamageType;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.defensive.DamageReductionBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.utils.RandomUtils;

/**
 * 伤害管理器 (DamageManager)
 * 职责：所有伤害的唯一入口，封装完整的伤害管线
 *
 * 管线流程：
 *   ① 闪避判定（可配置）
 *   ② 暴击判定（可配置）
 *   ③ 防御力减免（可配置）
 *   ④ 伤害前拦截：被动技能 + Buff 减伤事件
 *   ⑤ 减伤Buff（可配置）
 *   ⑥ 护盾吸收（可配置）
 *   ⑦ 扣除HP
 *   ⑧ 后置触发：被动技能 + Buff/Affix 事件
 */
public class DamageManager {
    private static DamageManager instance;
    private Context context;

    private DamageManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized DamageManager getInstance(Context context) {
        if (instance == null) {
            instance = new DamageManager(context);
        }
        return instance;
    }

    public static synchronized void releaseInstance() {
        instance = null;
    }

    // ====================== 核心入口 ======================

    /**
     * 所有伤害的统一入口
     *
     * @param config    伤害配置（决定走哪条管线）
     * @param attacker  伤害来源者（Buff真伤时可为 null）
     * @param target    受伤目标
     * @param baseDamage 基础伤害值
     * @param ctx       战斗上下文
     * @return 实际造成的伤害
     */
    public int dealDamage(DamageConfig config, BattleEntity attacker, BattleEntity target,
                           int baseDamage, BattleContext ctx) {
        ctx.resetDamageData();
        ctx.currentActor = attacker;
        ctx.currentTarget = target;
        ctx.damageType = config.damageType.name();
        ctx.damageSource = config.damageSource;

        AttributeSet attackerAttr = attacker != null ? attacker.getFinalAttributes() : null;
        AttributeSet targetAttr = target.getFinalAttributes();

        // ===== 阶段①：闪避判定 =====
        ctx.rawDamage = baseDamage;
        if (config.canDodge && attacker != null) {
            float hitChance = clampHitChance(attackerAttr, targetAttr);
            ctx.isHit = RandomUtils.checkProbability(hitChance);
            ctx.isDodged = !ctx.isHit;
            if (!ctx.isHit) {
                ctx.rawDamage = 0;
                ctx.finalDamage = 0;
                ctx.isCriticalHit = false;
                triggerOnMiss(attacker, target, ctx);
                return 0;
            }
        } else {
            ctx.isHit = true;
            ctx.isDodged = false;
        }

        // ===== 阶段②：暴击判定 =====
        ctx.rawDamage = baseDamage;
        ctx.isCriticalHit = false;
        if (config.canCrit && attacker != null) {
            boolean isPhy = config.damageType == DamageType.PHYSICAL;
            float critRate = clampProbability(isPhy ? attackerAttr.physicalCritRate : attackerAttr.magicalCritRate);
            ctx.isCriticalHit = RandomUtils.checkProbability(critRate);
            if (ctx.isCriticalHit) {
                float critDmg = isPhy ? attackerAttr.physicalCritDmg : attackerAttr.magicalCritDmg;
                ctx.rawDamage = (int)(ctx.rawDamage * critDmg);
                triggerOnCrit(config, attacker, target, ctx);
            }
        }

        // ===== 阶段③：防御力减免 =====
        ctx.finalDamage = ctx.rawDamage;
        if (config.useDefense && targetAttr != null) {
            int def = config.damageType == DamageType.PHYSICAL ? targetAttr.physicalDef : targetAttr.magicalDef;
            ctx.finalDamage = Math.max(1, ctx.finalDamage - def);
        }

        // ===== 阶段④：伤害前拦截（被动技能 + Buff 可修改伤害） =====
        ctx.finalDamage = PassiveSkillManager.getInstance()
                .triggerBeforeDamageDealtPassiveSkills(attacker, target, ctx.finalDamage, ctx);
        ctx.finalDamage = BuffManager.getInstance(context)
                .triggerBeforeDamageReceivedEvent(target, attacker, ctx.finalDamage, ctx);

        // ===== 阶段⑤：减伤Buff =====
        if (config.useDamageReduction) {
            BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_BEFORE_DAMAGE_TAKEN);
            ctx.finalDamage = applyCountBasedDamageReduction(ctx, target, ctx.finalDamage);
        }

        // ===== 阶段⑥：护盾吸收 =====
        if (config.useShield) {
            ctx.finalDamage = applyShieldAbsorption(ctx, target, ctx.finalDamage);
        }

        // ===== 阶段⑦：扣除HP =====
        if (ctx.finalDamage > 0) {
            target.takeDamage(ctx.finalDamage);
        }

        // ===== 阶段⑧：后置触发 =====
        triggerAfterDamage(config, attacker, target, ctx);

        if (config.triggerOnHitPostEvents && ctx.isHit) {
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_HIT);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_HIT);
        }

        return ctx.finalDamage;
    }

    // ====================== 便捷方法（保持 BattleManager 的 API 兼容） ======================

    public int dealPhysicalDamage(BattleEntity attacker, BattleEntity target,
                                   int baseDamage, BattleContext ctx) {
        return dealDamage(DamageConfig.skillPhysical(), attacker, target, baseDamage, ctx);
    }

    public int dealMagicalDamage(BattleEntity attacker, BattleEntity target,
                                  int baseDamage, BattleContext ctx) {
        return dealDamage(DamageConfig.skillMagical(), attacker, target, baseDamage, ctx);
    }

    public void dealTrueDamage(BattleEntity target, int trueDamage, BattleContext ctx) {
        dealDamage(DamageConfig.trueDamage(DamageSource.ACTIVE_SKILL), ctx.currentActor, target, trueDamage, ctx);
    }

    public int dealPiercingDamage(BattleEntity attacker, BattleEntity target,
                                   int piercingDamage, BattleContext ctx) {
        return dealDamage(DamageConfig.piercing(DamageSource.PIERCING), attacker, target, piercingDamage, ctx);
    }

    // ====================== 未命中触发 ======================

    private void triggerOnMiss(BattleEntity attacker, BattleEntity target, BattleContext ctx) {
        BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_ATTACK_MISS);
        AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_ATTACK_MISS);
        BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_DODGE);
        AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_DODGE);
    }

    // ====================== 暴击触发 ======================

    private void triggerOnCrit(DamageConfig config, BattleEntity attacker, BattleEntity target, BattleContext ctx) {
        BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_CRIT);
        AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_CRIT);
        BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_BEING_CRIT);
        AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_BEING_CRIT);
    }

    // ====================== 伤害后置触发 ======================

    private void triggerAfterDamage(DamageConfig config, BattleEntity attacker, BattleEntity target, BattleContext ctx) {
        if (ctx.finalDamage <= 0) return;

        // 被攻击事件
        if (attacker != null) {
            BuffManager.getInstance(context).triggerAttackedEvent(target, attacker, ctx);
        }

        // 受到伤害后
        BuffManager.getInstance(context).triggerAfterDamageReceivedEvent(target, attacker, ctx.finalDamage, ctx);
        PassiveSkillManager.getInstance().triggerAfterDamageReceivedPassiveSkills(target, attacker, ctx.finalDamage, ctx);

        // 造成伤害后
        if (attacker != null) {
            PassiveSkillManager.getInstance().triggerAfterDamageDealtPassiveSkills(attacker, target, ctx.finalDamage, ctx);
            BuffManager.getInstance(context).triggerAfterDamageDealtEvent(attacker, target, ctx.finalDamage, ctx);
        }

        // 目标受击后
        BuffManager.getInstance(context).triggerBuffs(target, ctx, BuffTriggerType.ON_AFTER_DAMAGE_TAKEN);
        AffixManager.getInstance(context).triggerAffixes(target, ctx, AffixTriggerType.ON_AFTER_DAMAGE_TAKEN);

        // 击杀事件
        if (target.isDead() && attacker != null) {
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, BuffTriggerType.ON_KILL);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, AffixTriggerType.ON_KILL);
        }
    }

    // ====================== 防御结算 ======================

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

        boolean hadShieldBefore = ShieldBuff.hasShield(target);

        int remainingDamage = incomingDamage;
        for (BaseBuff buff : target.getActiveBuffList()) {
            if (!(buff instanceof ShieldBuff)) continue;
            if (remainingDamage <= 0) break;
            remainingDamage = ((ShieldBuff) buff).absorbDamage(remainingDamage, target, ctx);
        }

        boolean hasShieldAfter = ShieldBuff.hasShield(target);
        if (hadShieldBefore && !hasShieldAfter && remainingDamage < incomingDamage) {
            PassiveSkillManager.getInstance().triggerShieldBreakPassiveSkills(target, ctx.currentActor, ctx,
                    BattleManager.getInstance(context));
        }

        return remainingDamage;
    }

    // ====================== 工具方法 ======================

    private float clampHitChance(AttributeSet attackerAttr, AttributeSet targetAttr) {
        return clampProbability(attackerAttr.hitRate - targetAttr.dodgeRate);
    }

    private float clampProbability(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
