package com.example.treasure_and_battle.model.attribute;

/**
 * 属性类型枚举：列出所有可被Buff修改的属性
 * 后续加新属性，只需要在这里加一个枚举值即可
 */
public enum AttributeType {
    // ====================== 六维属性 ======================
    STRENGTH,
    AGILITY,
    INTELLIGENCE,
    SPIRIT,
    PHYSIQUE,
    LUCK,

    // ====================== 核心战斗属性 ======================
    PHYSICAL_ATK,
    MAGICAL_ATK,
    PHYSICAL_DEF,
    MAGICAL_DEF,
    SPEED,
    MAX_HP,
    MAX_MP,
    MAX_ACTION_POINTS,

    // ====================== 附加战斗属性 ======================
    PHYSICAL_CRIT_RATE,
    MAGICAL_CRIT_RATE,
    PHYSICAL_CRIT_DMG,
    MAGICAL_CRIT_DMG,
    HIT_RATE,
    DODGE_RATE,
    DEBUFF_RESIST,
    DAMAGE_REDUCTION_RATE
}
