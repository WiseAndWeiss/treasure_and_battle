package com.example.treasure_and_battle.skill.passive.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 毒愈共生 - 法师被动技能
 * 效果：对带有中毒效果的敌人造成伤害时，恢复本次伤害x%的生命值
 */
public class PassiveSkill_PoisonSymbiosis extends PassiveSkill {

    public PassiveSkill_PoisonSymbiosis(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onAfterDamageDealt(BattleEntity owner, BattleEntity target, int damage, BattleContext context) {
        int healPercent = getEffectParams().x; // 回血百分比

        // 检查目标是否有中毒debuff
        boolean hasPoison = false;
        for (BaseBuff buff : target.getActiveBuffList()) {
            if (buff instanceof PoisoningDebuff) {
                hasPoison = true;
                break;
            }
        }

        if (!hasPoison || damage <= 0) {
            return; // 目标没有中毒或没有伤害，不触发
        }

        // 计算回血量
        int healAmount = (int) (damage * healPercent / 100.0f);

        // 检查HP是否已满
        int maxHp = owner.getFinalAttributes().maxHp;
        int currentHp = owner.getCurrentHp();
        int newHp = Math.min(currentHp + healAmount, maxHp);
        int actualHeal = newHp - currentHp;

        if (actualHeal > 0) {
            owner.setCurrentHp(newHp);

            context.addLog(LogType.HEAL,
                    "【毒愈共生】[%s] 从 [%s] 的中毒反馈中汲取了%d点生命值",
                    owner.getName(), target.getName(), actualHeal);
        }
    }
}
