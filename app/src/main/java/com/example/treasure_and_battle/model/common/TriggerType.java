package com.example.treasure_and_battle.model.common;

/**
 * 统一触发时机枚举
 * 合并了原有的 BuffTriggerType、AffixTriggerType、SkillTriggerType 三套孤立体系统一管理。
 *
 * <h3>触发顺序约定</h3>
 * <ol>
 *   <li>玩家方 → 怪物方</li>
 *   <li>同一实体上：被动技能 → Buff → 词缀</li>
 * </ol>
 */
public enum TriggerType {

    // ========== 常驻 ==========

    PERMANENT,

    // ========== 战斗生命周期 ==========

    /** 战斗开始时触发 */
    ON_BATTLE_START,

    /** 战斗结束时触发 */
    ON_BATTLE_END,

    /** 回合开始时触发 */
    ON_ROUND_START,

    /** 回合结束时触发 */
    ON_ROUND_END,

    // ========== 攻击流程 ==========

    /** 发起普通攻击时触发（不要求命中） */
    ON_ATTACK,

    /** 被普通攻击时触发（不要求命中） */
    ON_ATTACKED,

    /** 攻击未命中时触发 */
    ON_ATTACK_MISS,

    /** 攻击命中后触发 */
    ON_HIT,

    /** 造成暴击伤害时触发 */
    ON_CRIT,

    /** 受到暴击时触发 */
    ON_BEING_CRIT,

    /** 闪避攻击时触发 */
    ON_DODGE,

    // ========== 伤害流程（攻击方视角） ==========

    /** 攻击方造成伤害前触发，可修改伤害值 */
    ON_BEFORE_DAMAGE_DEALT,

    /** 攻击方造成伤害后触发 */
    ON_AFTER_DAMAGE_DEALT,

    // ========== 伤害流程（受击方视角） ==========

    /** 受击方受到伤害前触发，可修改伤害值 */
    ON_BEFORE_DAMAGE_TAKEN,

    /** 受击方受到伤害后触发 */
    ON_AFTER_DAMAGE_TAKEN,

    // ========== 结果 ==========

    /** 击杀目标时触发 */
    ON_KILL,

    /** 自身死亡时触发 */
    ON_DEATH,

    // ========== 技能 ==========

    /** 释放技能时触发 */
    ON_SKILL_CAST,

    // ========== 特殊（不参与统一分发） ==========

    /** 血量低于阈值时触发（轮询检查，非事件分发） */
    ON_LOW_HEALTH
}
