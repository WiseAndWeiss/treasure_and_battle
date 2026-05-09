package com.example.treasure_and_battle.skill.passive;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import com.example.treasure_and_battle.model.skill.SkillType;
import com.example.treasure_and_battle.skill.Skill;

/**
 * 被动技能基类
 * 提供针对不同触发类型的回调方法，子类重写需要的方法即可
 */
public abstract class PassiveSkill extends Skill {
    public PassiveSkill(SkillTemplate skillTemplate) {
        super(skillTemplate);
        assert skillTemplate.getSkillType() == SkillType.PASSIVE;
    }

    // ====================== 战斗流程事件 ======================

    /**
     * 战斗开始时触发
     */
    public void onBattleStart(BattleEntity owner, BattleContext context) {
        // 子类重写
    }

    /**
     * 战斗结束时触发
     */
    public void onBattleEnd(BattleEntity owner, BattleContext context) {
        // 子类重写
    }

    /**
     * 回合开始时触发
     */
    public void onRoundStart(BattleEntity owner, BattleContext context) {
        // 子类重写
    }

    /**
     * 回合结束时触发
     */
    public void onRoundEnd(BattleEntity owner, BattleContext context) {
        // 子类重写
    }

    // ====================== 攻击相关事件 ======================

    /**
     * 攻击时触发（在命中判定后）
     */
    public void onAttack(BattleEntity owner, BattleEntity target, BattleContext context) {
        // 子类重写
    }

    /**
     * 被攻击时触发
     */
    public void onAttacked(BattleEntity owner, BattleEntity attacker, BattleContext context) {
        // 子类重写
    }

    // ====================== 伤害相关事件 ======================

    /**
     * 造成伤害前触发
     * @param damage 将造成的伤害（可修改）
     * @return 修改后的伤害值
     */
    public int onBeforeDamageDealt(BattleEntity owner, BattleEntity target, int damage, BattleContext context) {
        return damage; // 子类可重写以修改伤害
    }

    /**
     * 造成伤害后触发
     */
    public void onAfterDamageDealt(BattleEntity owner, BattleEntity target, int damage, BattleContext context) {
        // 子类重写
    }

    /**
     * 受到伤害前触发
     * @param damage 将受到的伤害（可修改）
     * @return 修改后的伤害值
     */
    public int onBeforeDamageReceived(BattleEntity owner, BattleEntity attacker, int damage, BattleContext context) {
        return damage; // 子类可重写以修改伤害
    }

    /**
     * 受到伤害后触发
     */
    public void onAfterDamageReceived(BattleEntity owner, BattleEntity attacker, int damage, BattleContext context) {
        // 子类重写
    }

    /**
     * 护盾破碎时触发
     * 当实体身上的护盾被击碎（从有值变为0）时触发
     * @param owner 护盾所有者
     * @param attacker 击碎护盾的攻击者
     * @param context 战斗上下文
     * @param battleManager 战斗管理器（用于触发AOE伤害等效果）
     */
    public void onShieldBreak(BattleEntity owner, BattleEntity attacker, BattleContext context, com.example.treasure_and_battle.manager.BattleManager battleManager) {
        // 子类重写
    }

    // ====================== 击杀/死亡事件 ======================

    /**
     * 击杀敌人时触发
     */
    public void onKill(BattleEntity owner, BattleEntity target, BattleContext context) {
        // 子类重写
    }

    /**
     * 死亡时触发
     */
    public void onDeath(BattleEntity owner, BattleContext context) {
        // 子类重写
    }

    // ====================== 使用技能/道具事件 ======================

    /**
     * 使用技能时触发
     */
    public void onUseSkill(BattleEntity owner, BattleContext context) {
        // 子类重写
    }

    /**
     * 使用道具时触发
     */
    public void onUseItem(BattleEntity owner, BattleContext context) {
        // 子类重写
    }

    // ====================== 属性加成 ======================

    /**
     * 应用属性加成（子类可重写以提供永久属性加成）
     * @param modifiers 属性修饰器，被动技能的加成将累加到这里
     */
    public void applyAttributeBonus(com.example.treasure_and_battle.model.attribute.AttributeSet modifiers) {
        // 子类重写以提供属性加成
    }

}
