package com.example.treasure_and_battle.skill.passive.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 嗜血 - 通用被动技能
 * 效果：你造成伤害的{x}%会转化为生命值治疗自己
 */
public class PassiveSkill_Bloodthirsty extends PassiveSkill {
    public PassiveSkill_Bloodthirsty(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onAfterDamageDealt(BattleEntity owner, BattleEntity target, int damage, BattleContext context) {
        int lifestealPercent = getEffectParams().x; // 吸血百分比

        if (damage <= 0 || lifestealPercent <= 0) {
            return;
        }

        // 计算吸血量
        int healAmount = (int) (damage * lifestealPercent / 100.0f);

        if (healAmount > 0 && !owner.isDead()) {
            int hpBefore = owner.getCurrentHp();
            owner.healHp(healAmount);
            int actualHealed = owner.getCurrentHp() - hpBefore;

            if (actualHealed > 0) {
                context.addLog(LogType.HEAL,
                    "【嗜血】[%s] 从伤害中吸回了 %d 点生命值（%d%%）",
                    owner.getName(), actualHealed, lifestealPercent);
            }
        }
    }
}
