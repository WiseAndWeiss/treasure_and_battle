package com.example.treasure_and_battle.model.affix;

public enum AffixTriggerType {
    // 常驻生效：加属性、增伤倍率等永久生效的词缀（最常用）
    PERMANENT,
    // 攻击发起时触发：宣告攻击动作后立即触发，不要求命中
    ON_ATTACK,
    // 攻击未命中时触发：当攻击动作未命中目标时触发
    ON_ATTACK_MISS,
    // 命中后触发：造成本次命中伤害后触发，可读取最终伤害
    ON_HIT,
    // 造成暴击时触发：伤害计算完成后，如果是暴击则触发
    ON_CRIT,
    // 受到暴击时触发：当角色受到暴击伤害时触发
    ON_BEING_CRIT,
    // 闪避时触发：当角色成功闪避攻击时触发
    ON_DODGE,
    // 受到伤害前触发
    ON_BEFORE_DAMAGE_TAKEN,
    // 受到伤害后触发
    ON_AFTER_DAMAGE_TAKEN,
    // 击杀怪物时触发
    ON_KILL,
    // 战斗开始时触发
    ON_BATTLE_START,
    // 战斗结束时触发
    ON_BATTLE_END,
    // 回合开始时触发
    ON_ROUND_START,
    // 回合结束时触发
    ON_ROUND_END,
    // 释放技能时触发
    ON_SKILL_CAST,
    // 血量低于X%时触发
    ON_LOW_HEALTH,
    // 后续可根据需要添加更多触发时机，如：暴击时、闪避时、被治疗时等

    // 怪物专用触发类型
    ON_MONSTER_DEATH,    // 怪物死亡时
    ON_MONSTER_SPAWN,    // 怪物生成时
    ON_MONSTER_ENRAGE    // 怪物狂暴时（血量低于50%）
}
