package com.example.treasure_and_battle.model.buff;

// Buff触发时机枚举，和词缀触发时机对齐
public enum BuffTriggerType {
    PERMANENT,          // 常驻生效（属性加成类）
    ON_ROUND_START,     // 回合开始时
    ON_ROUND_END,       // 回合结束时
    ON_ATTACK,          // 攻击发起时
    ON_ATTACK_MISS,     // 攻击未命中时
    ON_HIT,             // 攻击命中时
    ON_CRIT,            // 造成暴击时
    ON_BEING_CRIT,      // 受到暴击时
    ON_DODGE,          // 闪避时
    ON_BEFORE_DAMAGE_TAKEN,   // 受到伤害前
    ON_AFTER_DAMAGE_TAKEN,    // 受到伤害时
    ON_KILL,            // 击杀目标时
    ON_BATTLE_START,    // 战斗开始时
    ON_BATTLE_END       // 战斗结束时
}