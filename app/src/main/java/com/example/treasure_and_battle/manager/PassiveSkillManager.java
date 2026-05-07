package com.example.treasure_and_battle.manager;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.skill.SkillTriggerType;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

import java.util.List;

/**
 * 被动技能统一管理类
 * 单例模式，负责被动技能的触发调度，将9个重复的trigger方法统一为一个dispatch模式
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

    // ====================== 战斗流程事件 ======================

    public void triggerBattleStartPassiveSkills(BattleEntity owner, BattleContext context) {
        if (owner.getPassiveSkillList() == null || owner.getPassiveSkillList().isEmpty()) return;

        for (PassiveSkill skill : owner.getPassiveSkillList()) {
            if (!skill.hasTriggerType(SkillTriggerType.ON_BATTLE_START)) continue;
            try {
                skill.onBattleStart(owner, context);
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] onBattleStart 触发失败: %s",
                        skill.getSkillName(), e.getMessage());
            }
        }
    }

    public void triggerRoundEndPassiveSkills(BattleEntity owner, BattleContext context) {
        if (owner.getPassiveSkillList() == null || owner.getPassiveSkillList().isEmpty()) return;

        for (PassiveSkill skill : owner.getPassiveSkillList()) {
            if (!skill.hasTriggerType(SkillTriggerType.ON_ROUND_END)) continue;
            try {
                skill.onRoundEnd(owner, context);
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] onRoundEnd 触发失败: %s",
                        skill.getSkillName(), e.getMessage());
            }
        }
    }

    public void triggerRoundStartPassiveSkills(BattleEntity owner, BattleContext context) {
        if (owner.getPassiveSkillList() == null || owner.getPassiveSkillList().isEmpty()) return;

        for (PassiveSkill skill : owner.getPassiveSkillList()) {
            if (!skill.hasTriggerType(SkillTriggerType.ON_ROUND_START)) continue;
            try {
                skill.onRoundStart(owner, context);
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] onRoundStart 触发失败: %s",
                        skill.getSkillName(), e.getMessage());
            }
        }
    }

    // ====================== 攻击相关事件 ======================

    public void triggerAttackPassiveSkills(BattleEntity attacker, BattleEntity target, BattleContext context) {
        if (attacker.getPassiveSkillList() != null) {
            for (PassiveSkill skill : attacker.getPassiveSkillList()) {
                if (!skill.hasTriggerType(SkillTriggerType.ON_ATTACK)) continue;
                try {
                    skill.onAttack(attacker, target, context);
                } catch (Exception e) {
                    context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                            skill.getSkillName(), e.getMessage());
                }
            }
        }

        if (target.getPassiveSkillList() != null) {
            for (PassiveSkill skill : target.getPassiveSkillList()) {
                if (!skill.hasTriggerType(SkillTriggerType.ON_ATTACKED)) continue;
                try {
                    skill.onAttacked(target, attacker, context);
                } catch (Exception e) {
                    context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                            skill.getSkillName(), e.getMessage());
                }
            }
        }
    }

    // ====================== 伤害相关事件 ======================

    public int triggerBeforeDamageDealtPassiveSkills(BattleEntity attacker, BattleEntity target, int damage, BattleContext context) {
        int modifiedDamage = damage;

        if (attacker.getPassiveSkillList() != null) {
            for (PassiveSkill skill : attacker.getPassiveSkillList()) {
                if (!skill.hasTriggerType(SkillTriggerType.ON_BEFORE_DAMAGE_DEALT)) continue;
                try {
                    modifiedDamage = skill.onBeforeDamageDealt(attacker, target, modifiedDamage, context);
                } catch (Exception e) {
                    context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                            skill.getSkillName(), e.getMessage());
                }
            }
        }

        return modifiedDamage;
    }

    public void triggerAfterDamageDealtPassiveSkills(BattleEntity attacker, BattleEntity target, int damage, BattleContext context) {
        if (attacker.getPassiveSkillList() == null) return;

        for (PassiveSkill skill : attacker.getPassiveSkillList()) {
            if (!skill.hasTriggerType(SkillTriggerType.ON_AFTER_DAMAGE_DEALT)) continue;
            try {
                skill.onAfterDamageDealt(attacker, target, damage, context);
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                        skill.getSkillName(), e.getMessage());
            }
        }
    }

    public int triggerBeforeDamageReceivedPassiveSkills(BattleEntity target, BattleEntity attacker, int damage, BattleContext context) {
        int modifiedDamage = damage;

        if (target.getPassiveSkillList() != null) {
            for (PassiveSkill skill : target.getPassiveSkillList()) {
                if (!skill.hasTriggerType(SkillTriggerType.ON_BEFORE_DAMAGE_RECEIVED)) continue;
                try {
                    modifiedDamage = skill.onBeforeDamageReceived(target, attacker, modifiedDamage, context);
                } catch (Exception e) {
                    context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                            skill.getSkillName(), e.getMessage());
                }
            }
        }

        return modifiedDamage;
    }

    public void triggerAfterDamageReceivedPassiveSkills(BattleEntity target, BattleEntity attacker, int damage, BattleContext context) {
        if (target.getPassiveSkillList() == null) return;

        for (PassiveSkill skill : target.getPassiveSkillList()) {
            if (!skill.hasTriggerType(SkillTriggerType.ON_AFTER_DAMAGE_RECEIVED)) continue;
            try {
                skill.onAfterDamageReceived(target, attacker, damage, context);
            } catch (Exception e) {
                context.addLog(LogType.SYSTEM, "被动技能 [%s] 触发失败: %s",
                        skill.getSkillName(), e.getMessage());
            }
        }
    }

    // ====================== 护盾破碎事件 ======================

    public void triggerShieldBreakPassiveSkills(BattleEntity owner, BattleEntity attacker, BattleContext context, BattleManager battleManager) {
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

    // ====================== 兼容性：单实体触发 ======================

    /**
     * 触发战斗开始时的被动技能（保留原方法名用于兼容）
     */
    public void triggerPassiveSkills(BattleEntity owner, BattleContext context) {
        triggerBattleStartPassiveSkills(owner, context);
    }

    // ====================== 批量触发辅助方法（减少BattleManager显式循环） ======================

    public void triggerBattleStartForAll(BattleContext ctx) {
        if (ctx == null) return;
        triggerBattleStartPassiveSkills(ctx.player, ctx);
        for (Monster m : ctx.monsters) {
            if (m == null) continue;
            triggerBattleStartPassiveSkills(m, ctx);
        }
    }

    public void triggerRoundStartForAll(BattleContext ctx) {
        if (ctx == null) return;
        triggerRoundStartPassiveSkills(ctx.currentActor, ctx);
        for (BattleEntity e : ctx.playerParty) {
            if (e == null || e.isDead()) continue;
            triggerRoundStartPassiveSkills(e, ctx);
        }
        for (Monster m : ctx.getAliveMonsters()) {
            if (m == null) continue;
            triggerRoundStartPassiveSkills(m, ctx);
        }
    }

    public void triggerRoundEndForAll(BattleContext ctx) {
        if (ctx == null) return;
        for (BattleEntity e : ctx.playerParty) {
            if (e == null || e.isDead()) continue;
            triggerRoundEndPassiveSkills(e, ctx);
        }
        for (Monster m : ctx.getAliveMonsters()) {
            if (m == null) continue;
            triggerRoundEndPassiveSkills(m, ctx);
        }
    }
}
