package com.example.treasure_and_battle.skill.passive.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

import java.util.List;

/**
 * 碎盾冲击 - 战士被动技能
 * 效果：当护盾被敌人攻击打破时，对全场所有敌人造成一次x%自身防御力的伤害
 */
public class PassiveSkill_ShieldBurst extends PassiveSkill {

    public PassiveSkill_ShieldBurst(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onShieldBreak(BattleEntity owner, BattleEntity attacker, BattleContext context, BattleManager battleManager) {
        int defensePercent = getEffectParams().x; // 防御力百分比

        if (defensePercent <= 0) {
            return;
        }

        // 计算AOE伤害
        int defense = owner.getFinalAttributes().physicalDef;
        int aoeDamage = (int) (defense * defensePercent / 100.0f);

        if (aoeDamage <= 0) {
            return;
        }

        // 获取所有敌人
        List<BattleEntity> enemies = battleManager.getAllEnemies(owner, context);

        if (enemies.isEmpty()) {
            return;
        }

        int totalDamage = 0;
        int totalHits = 0;

        // 对每个存活的敌人造成伤害
        for (BattleEntity enemy : enemies) {
            if (enemy.isDead()) {
                continue;
            }

            // 造成防御反击伤害（无视防御，真实伤害）
            enemy.takeDamage(aoeDamage, com.example.treasure_and_battle.battle.DamageType.TRUE);
            totalDamage += aoeDamage;
            totalHits++;
        }

        // 记录日志
        context.addLog(LogType.ACTION,
            "【碎盾冲击】[%s] 的护盾被击碎，对 %d 个敌人发动反击！总共造成 %d 点防御反击伤害（基于防御力的%d%%）",
            owner.getName(), totalHits, totalDamage, defensePercent);
    }
}
