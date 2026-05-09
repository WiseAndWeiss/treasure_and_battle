package com.example.treasure_and_battle.skill.passive.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

import java.util.List;

/**
 * 净化 - 通用被动技能
 * 效果：回合开始时，有{x}%几率移除自身某一种debuff的所有层数
 */
public class PassiveSkill_Purify extends PassiveSkill {
    public PassiveSkill_Purify(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onRoundStart(BattleEntity owner, BattleContext context) {
        int purifyChance = getEffectParams().x; // 净化概率

        // 检查是否触发净化
        if (!com.example.treasure_and_battle.utils.RandomUtils.checkProbability(purifyChance / 100.0f)) {
            return; // 未触发
        }

        // 查找身上的debuff
        List<BaseBuff> buffs = owner.getActiveBuffList();
        BaseBuff debuffToRemove = null;

        for (BaseBuff buff : buffs) {
            if (buff.getBuffType() == BuffType.DEBUFF) {
                debuffToRemove = buff;
                break; // 找到一个即可
            }
        }

        // 移除找到的debuff
        if (debuffToRemove != null) {
            String buffName = debuffToRemove.getBuffName();
            owner.getActiveBuffList().remove(debuffToRemove);
            owner.markAttributeCacheDirty();

            context.addLog(LogType.BUFF,
                "【净化】[%s] 移除了 [%s] 的效果",
                owner.getName(), buffName);
        }
    }
}
