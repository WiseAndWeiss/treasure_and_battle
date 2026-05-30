package com.example.treasure_and_battle.affix.impl.equip.trigger;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;

public class EquipTriggerSkillCastRecoverMpAffix extends BaseEquipAffix {

    public EquipTriggerSkillCastRecoverMpAffix(int affixId, String affixName, String description, Rarity rarity,
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

        AttributeSet attr = owner.getFinalAttributes();
        int recoverAmount = (int) (attr.maxMp * affixValue);
        if (recoverAmount <= 0) {
            return;
        }

        int beforeMp = owner.getCurrentMp();
        owner.healMp(recoverAmount);
        int actualRecover = owner.getCurrentMp() - beforeMp;

        if (actualRecover > 0) {
            context.addLogWithMeta(
                    LogType.AFFIX,
                    this,
                    "【词缀触发】[%s] 的 [%s] 触发，恢复了 %d 点魔力值。",
                    owner.getName(),
                    getAffixName(),
                    actualRecover
            );
        }
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100);
    }
}
