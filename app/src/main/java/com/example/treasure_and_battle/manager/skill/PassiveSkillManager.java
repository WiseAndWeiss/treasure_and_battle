package com.example.treasure_and_battle.manager.skill;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 被动技能统一管理类
 * 单例模式，通过统一的 trigger() 方法处理所有触发时机。
 * 与 BuffManager/AffixManager 对齐：一个方法 + TriggerType 枚举完成派发。
 */
public class PassiveSkillManager {
    private static PassiveSkillManager instance;

    private PassiveSkillManager() {
    }

    public static synchronized PassiveSkillManager getInstance() {
        if (instance == null) {
            instance = new PassiveSkillManager();
        }
        return instance;
    }

    public static synchronized void releaseInstance() {
        instance = null;
    }

    // ====================== 统一触发入口 ======================

    /**
     * 简单触发（单实体，无额外参数）
     * BATTLE_START / BATTLE_END / ROUND_START / ROUND_END / DEATH / ON_SKILL_CAST
     */
    public void trigger(BattleEntity owner, BattleContext context, TriggerType type) {
        if (owner.getPassiveSkillList() == null || owner.getPassiveSkillList().isEmpty()) return;

        for (PassiveSkill skill : owner.getPassiveSkillList()) {
            if (!skill.hasTriggerType(type)) continue;
            try {
                switch (type) {
                    case ON_BATTLE_START:  skill.onBattleStart(owner, context); break;
                    case ON_BATTLE_END:    skill.onBattleEnd(owner, context); break;
                    case ON_ROUND_START:   skill.onRoundStart(owner, context); break;
                    case ON_ROUND_END:     skill.onRoundEnd(owner, context); break;
                    case ON_DEATH:         skill.onDeath(owner, context); break;
                    case ON_SKILL_CAST:    skill.onUseSkill(owner, context); break;
                    default: break;
                }
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                        skill.getSkillName(), e.getMessage());
            }
        }
    }

    /**
     * 双实体触发
     * ON_ATTACK / ON_ATTACKED / ON_KILL
     */
    public void trigger(BattleEntity owner, BattleEntity other, BattleContext context, TriggerType type) {
        if (owner.getPassiveSkillList() == null || owner.getPassiveSkillList().isEmpty()) return;

        for (PassiveSkill skill : owner.getPassiveSkillList()) {
            if (!skill.hasTriggerType(type)) continue;
            try {
                switch (type) {
                    case ON_ATTACK:   skill.onAttack(owner, other, context); break;
                    case ON_ATTACKED: skill.onAttacked(owner, other, context); break;
                    case ON_KILL:     skill.onKill(owner, other, context); break;
                    default: break;
                }
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                        skill.getSkillName(), e.getMessage());
            }
        }
    }

    /**
     * 伤害前触发，返回修正后的伤害值
     * ON_BEFORE_DAMAGE_DEALT / ON_BEFORE_DAMAGE_TAKEN
     */
    public int triggerBeforeDamage(BattleEntity owner, BattleEntity other,
                                    int damage, BattleContext context, TriggerType type) {
        if (owner.getPassiveSkillList() == null || owner.getPassiveSkillList().isEmpty()) return damage;

        int modified = damage;
        for (PassiveSkill skill : owner.getPassiveSkillList()) {
            if (!skill.hasTriggerType(type)) continue;
            try {
                if (type == TriggerType.ON_BEFORE_DAMAGE_DEALT) {
                    modified = skill.onBeforeDamageDealt(owner, other, modified, context);
                } else if (type == TriggerType.ON_BEFORE_DAMAGE_TAKEN) {
                    modified = skill.onBeforeDamageReceived(owner, other, modified, context);
                }
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                        skill.getSkillName(), e.getMessage());
            }
        }
        return modified;
    }

    /**
     * 伤害后触发
     * ON_AFTER_DAMAGE_DEALT / ON_AFTER_DAMAGE_TAKEN
     */
    public void triggerAfterDamage(BattleEntity owner, BattleEntity other,
                                    int damage, BattleContext context, TriggerType type) {
        if (owner.getPassiveSkillList() == null || owner.getPassiveSkillList().isEmpty()) return;

        for (PassiveSkill skill : owner.getPassiveSkillList()) {
            if (!skill.hasTriggerType(type)) continue;
            try {
                if (type == TriggerType.ON_AFTER_DAMAGE_DEALT) {
                    skill.onAfterDamageDealt(owner, other, damage, context);
                } else if (type == TriggerType.ON_AFTER_DAMAGE_TAKEN) {
                    skill.onAfterDamageReceived(owner, other, damage, context);
                }
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                        skill.getSkillName(), e.getMessage());
            }
        }
    }

    // ====================== 护盾破碎事件 ======================

    public void triggerShieldBreak(BattleEntity owner, BattleEntity attacker,
                                    BattleContext context, BattleManager battleManager) {
        if (owner.getPassiveSkillList() == null) return;

        for (PassiveSkill skill : owner.getPassiveSkillList()) {
            try {
                skill.onShieldBreak(owner, attacker, context, battleManager);
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] onShieldBreak 触发失败: %s",
                        skill.getSkillName(), e.getMessage());
            }
        }
    }
}
