package com.example.treasure_and_battle.manager.battle;

import android.content.Context;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.damage.DamageConfig;
import com.example.treasure_and_battle.battle.damage.DamageSource;
import com.example.treasure_and_battle.battle.damage.DamageType;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.affix.impl.monster.defensive.MonsterDamageCapAffix;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.defensive.DamageReductionBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;

import com.example.treasure_and_battle.manager.skill.PassiveSkillManager;
import com.example.treasure_and_battle.manager.affix.AffixManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.TriggerType;
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

        // ===== 阶段③：伤害前拦截（被动技能 + Buff 可修改伤害） =====
        ctx.finalDamage = ctx.rawDamage;
        if (attacker != null) {
            ctx.finalDamage = PassiveSkillManager.getInstance()
                    .triggerBeforeDamage(attacker, target, ctx.finalDamage, ctx, TriggerType.ON_BEFORE_DAMAGE_DEALT);
        }
        ctx.finalDamage = PassiveSkillManager.getInstance()
                .triggerBeforeDamage(target, attacker, ctx.finalDamage, ctx, TriggerType.ON_BEFORE_DAMAGE_TAKEN);
        ctx.finalDamage = BuffManager.getInstance(context)
                .triggerBeforeDamageReceivedEvent(target, attacker, ctx.finalDamage, ctx);

        // ===== 阶段④：防御力减免 =====
        if (config.useDefense && targetAttr != null) {
            int def = config.damageType == DamageType.PHYSICAL ? targetAttr.physicalDef : targetAttr.magicalDef;
            ctx.finalDamage = Math.max(1, ctx.finalDamage - def);
        }

        // ===== 阶段④b：硬化皮肤（单次伤害上限钳制） =====
        ctx.finalDamage = applyDamageCap(target, ctx.finalDamage);

        // ===== 阶段⑤：减伤Buff =====
        if (config.useDamageReduction) {
            BuffManager.getInstance(context).triggerBuffs(target, ctx, TriggerType.ON_BEFORE_DAMAGE_TAKEN);
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

        if (config.triggerOnHitPostEvents && ctx.isHit && attacker != null) {
            PassiveSkillManager.getInstance().trigger(attacker, ctx, TriggerType.ON_HIT);
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, TriggerType.ON_HIT);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, TriggerType.ON_HIT);
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
        PassiveSkillManager.getInstance().trigger(attacker, ctx, TriggerType.ON_ATTACK_MISS);
        BuffManager.getInstance(context).triggerBuffs(attacker, ctx, TriggerType.ON_ATTACK_MISS);
        AffixManager.getInstance(context).triggerAffixes(attacker, ctx, TriggerType.ON_ATTACK_MISS);
        PassiveSkillManager.getInstance().trigger(target, attacker, ctx, TriggerType.ON_DODGE);
        BuffManager.getInstance(context).triggerBuffs(target, ctx, TriggerType.ON_DODGE);
        AffixManager.getInstance(context).triggerAffixes(target, ctx, TriggerType.ON_DODGE);
    }

    // ====================== 暴击触发 ======================

    private void triggerOnCrit(DamageConfig config, BattleEntity attacker, BattleEntity target, BattleContext ctx) {
        PassiveSkillManager.getInstance().trigger(attacker, ctx, TriggerType.ON_CRIT);
        BuffManager.getInstance(context).triggerBuffs(attacker, ctx, TriggerType.ON_CRIT);
        AffixManager.getInstance(context).triggerAffixes(attacker, ctx, TriggerType.ON_CRIT);
        PassiveSkillManager.getInstance().trigger(target, attacker, ctx, TriggerType.ON_BEING_CRIT);
        BuffManager.getInstance(context).triggerBuffs(target, ctx, TriggerType.ON_BEING_CRIT);
        AffixManager.getInstance(context).triggerAffixes(target, ctx, TriggerType.ON_BEING_CRIT);
    }

    // ====================== 伤害后置触发 ======================

    private void triggerAfterDamage(DamageConfig config, BattleEntity attacker, BattleEntity target, BattleContext ctx) {
        if (ctx.finalDamage <= 0) return;

        if (attacker != null) {
            BuffManager.getInstance(context).triggerAttackedEvent(target, attacker, ctx);
        }

        BuffManager.getInstance(context).triggerAfterDamageReceivedEvent(target, attacker, ctx.finalDamage, ctx);
        PassiveSkillManager.getInstance().triggerAfterDamage(target, attacker, ctx.finalDamage, ctx, TriggerType.ON_AFTER_DAMAGE_TAKEN);

        if (attacker != null) {
            PassiveSkillManager.getInstance().triggerAfterDamage(attacker, target, ctx.finalDamage, ctx, TriggerType.ON_AFTER_DAMAGE_DEALT);
            BuffManager.getInstance(context).triggerAfterDamageDealtEvent(attacker, target, ctx.finalDamage, ctx);
        }

        BuffManager.getInstance(context).triggerBuffs(target, ctx, TriggerType.ON_AFTER_DAMAGE_TAKEN);
        AffixManager.getInstance(context).triggerAffixes(target, ctx, TriggerType.ON_AFTER_DAMAGE_TAKEN);

        if (target.isDead() && attacker != null) {
            PassiveSkillManager.getInstance().trigger(attacker, target, ctx, TriggerType.ON_KILL);
            PassiveSkillManager.getInstance().trigger(target, ctx, TriggerType.ON_DEATH);
            BuffManager.getInstance(context).triggerBuffs(attacker, ctx, TriggerType.ON_KILL);
            AffixManager.getInstance(context).triggerAffixes(attacker, ctx, TriggerType.ON_KILL);
            BuffManager.getInstance(context).triggerBuffs(target, ctx, TriggerType.ON_DEATH);
            AffixManager.getInstance(context).triggerAffixes(target, ctx, TriggerType.ON_DEATH);
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

        int beforeShield = incomingDamage;
        int remainingDamage = incomingDamage;
        for (BaseBuff buff : target.getActiveBuffList()) {
            if (!(buff instanceof ShieldBuff)) continue;
            if (remainingDamage <= 0) break;
            remainingDamage = ((ShieldBuff) buff).absorbDamage(remainingDamage, target, ctx);
        }

        ctx.shieldAbsorbed = beforeShield - remainingDamage;

        if (ctx.shieldAbsorbed > 0) {
            BattleManager.getInstance(context).notifyShieldAbsorbed(target, ctx.shieldAbsorbed);
        }

        boolean hasShieldAfter = ShieldBuff.hasShield(target);
        if (hadShieldBefore && !hasShieldAfter && remainingDamage < incomingDamage) {
            PassiveSkillManager.getInstance().triggerShieldBreak(target, ctx.currentActor, ctx,
                    BattleManager.getInstance(context));
        }

        return remainingDamage;
    }

    private int applyDamageCap(BattleEntity target, int incomingDamage) {
        if (incomingDamage <= 0 || target == null) return incomingDamage;
        for (BaseAffix affix : target.getEntityAffixList()) {
            if (affix instanceof MonsterDamageCapAffix) {
                int maxHp = target.getFinalAttributes().maxHp;
                return ((MonsterDamageCapAffix) affix).capDamage(incomingDamage, maxHp);
            }
        }
        return incomingDamage;
    }

    // ====================== 工具方法 ======================

    private float clampHitChance(AttributeSet attackerAttr, AttributeSet targetAttr) {
        return clampProbability(attackerAttr.hitRate - targetAttr.dodgeRate);
    }

    private float clampProbability(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
