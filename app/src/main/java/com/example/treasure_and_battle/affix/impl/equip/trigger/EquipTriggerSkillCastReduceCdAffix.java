package com.example.treasure_and_battle.affix.impl.equip.trigger;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.utils.RandomUtils;

import java.util.List;

public class EquipTriggerSkillCastReduceCdAffix extends BaseEquipAffix {

    public EquipTriggerSkillCastReduceCdAffix(int affixId, String affixName, String description, Rarity rarity,
                                               TriggerType triggerType, EquipCategory[] allowCategories,
                                               float affixValue) {
        super(affixId, affixName, description, rarity, triggerType, allowCategories, affixValue);
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        if (owner == null || context == null || owner.getContext() == null) {
            return;
        }

        if (!RandomUtils.checkProbability(affixValue)) {
            return;
        }

        if (owner.getActiveSkillList() == null || owner.getActiveSkillList().isEmpty()) {
            return;
        }

        List<ActiveSkill> skills = owner.getActiveSkillList();
        int reduced = 0;
        for (ActiveSkill skill : skills) {
            if (skill.getCurrentCooldown() > 0) {
                skill.decreaseCooldown();
                reduced++;
            }
        }

        if (reduced > 0) {
            context.addLogWithMeta(
                    LogType.AFFIX,
                    this,
                    "【词缀触发】[%s] 的 [%s] 触发，%d 个技能的冷却时间各减少 1 回合。",
                    owner.getName(),
                    getAffixName(),
                    reduced
            );
        }
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100);
    }
}
