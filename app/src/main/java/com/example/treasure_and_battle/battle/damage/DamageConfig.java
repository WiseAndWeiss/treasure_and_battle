package com.example.treasure_and_battle.battle.damage;

public class DamageConfig {

    public final DamageType damageType;
    public final DamageSource damageSource;

    public final boolean canDodge;
    public final boolean canCrit;
    public final boolean useDefense;
    public final boolean useDamageReduction;
    public final boolean useShield;
    public final boolean triggerOnHitPostEvents;

    private DamageConfig(DamageType damageType, DamageSource damageSource,
                         boolean canDodge, boolean canCrit, boolean useDefense,
                         boolean useDamageReduction, boolean useShield,
                         boolean triggerOnHitPostEvents) {
        this.damageType = damageType;
        this.damageSource = damageSource;
        this.canDodge = canDodge;
        this.canCrit = canCrit;
        this.useDefense = useDefense;
        this.useDamageReduction = useDamageReduction;
        this.useShield = useShield;
        this.triggerOnHitPostEvents = triggerOnHitPostEvents;
    }

    // ====================== 攻击/技能 物理伤害 ======================

    public static DamageConfig normalAttack() {
        return new DamageConfig(DamageType.PHYSICAL, DamageSource.NORMAL_ATTACK,
                true, true, true, true, true, true);
    }

    public static DamageConfig skillPhysical() {
        return new DamageConfig(DamageType.PHYSICAL, DamageSource.ACTIVE_SKILL,
                true, true, true, true, true, true);
    }

    public static DamageConfig skillMagical() {
        return new DamageConfig(DamageType.MAGICAL, DamageSource.ACTIVE_SKILL,
                true, true, true, true, true, true);
    }

    // ====================== Buff 伤害（不可闪避不可暴击，但走减伤和护盾） ======================

    public static DamageConfig buffPhysical() {
        return new DamageConfig(DamageType.PHYSICAL, DamageSource.BUFF,
                false, false, true, true, true, false);
    }

    public static DamageConfig buffMagical() {
        return new DamageConfig(DamageType.MAGICAL, DamageSource.BUFF,
                false, false, true, true, true, false);
    }

    public static DamageConfig buffTrue() {
        return new DamageConfig(DamageType.TRUE, DamageSource.BUFF,
                false, false, false, false, false, false);
    }

    // ====================== 真伤（可闪避，其他全跳过） ======================

    public static DamageConfig trueDamage(DamageSource source) {
        return new DamageConfig(DamageType.TRUE, source,
                true, false, false, false, false, false);
    }

    // ====================== 穿甲伤害（不可闪避不可暴击，无视防御无视减伤，只走护盾） ======================

    public static DamageConfig piercing(DamageSource source) {
        return new DamageConfig(DamageType.PIERCING, source,
                false, false, false, false, true, false);
    }

    // ====================== 反击伤害 ======================

    public static DamageConfig counterAttack() {
        return new DamageConfig(DamageType.PHYSICAL, DamageSource.COUNTER_ATTACK,
                true, true, true, true, true, true);
    }

    // ====================== 被动技能伤害 ======================

    public static DamageConfig passiveSkill(DamageType type) {
        return new DamageConfig(type, DamageSource.PASSIVE_SKILL,
                type == DamageType.TRUE ? false : true,
                false,
                type != DamageType.TRUE,
                type != DamageType.TRUE,
                type != DamageType.TRUE,
                false);
    }

    // ====================== 道具伤害 ======================

    public static DamageConfig itemDamage() {
        return new DamageConfig(DamageType.TRUE, DamageSource.ITEM,
                false, false, false, false, false, false);
    }
}
