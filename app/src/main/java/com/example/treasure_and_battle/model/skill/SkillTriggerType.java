package com.example.treasure_and_battle.model.skill;

public enum SkillTriggerType {
    ACTIVE_SKILL_USED,  // 使用主动技能时
    ON_BATTLE_START,   // 战斗开始时
    ON_BATTLE_END,     // 战斗结束时
    ON_ROUND_START,    // 回合开始时
    ON_ROUND_END,      // 回合结束时
    ON_ATTACK,         // 进行攻击时
    ON_ATTACKED,       // 被攻击时
    ON_BEFORE_DAMAGE_DEALT,    // 造成伤害前
    ON_AFTER_DAMAGE_DEALT,     // 造成伤害后
    ON_BEFORE_DAMAGE_RECEIVED, // 受到伤害前
    ON_AFTER_DAMAGE_RECEIVED,  // 受到伤害后
    ON_USE_SKILL,      // 使用技能时
    ON_USE_ITEM,       // 使用道具时
    ON_KILL,           // 造成击杀时
    ON_DEATH,          // 死亡时
}
