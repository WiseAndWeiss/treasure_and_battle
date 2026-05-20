package com.example.treasure_and_battle.affix.impl.equip.trigger;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;
import com.example.treasure_and_battle.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;

public class EquipTriggerPurifyAffix extends BaseEquipAffix {

    public EquipTriggerPurifyAffix(int affixId, String affixName, String description, Rarity rarity,
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

        List<BaseBuff> debuffs = new ArrayList<>();
        for (BaseBuff buff : owner.getActiveBuffList()) {
            if (buff.getBuffType() == BuffType.DEBUFF && buff.isDispellable() && buff.getStackCount() > 0) {
                debuffs.add(buff);
            }
        }

        if (debuffs.isEmpty()) {
            return;
        }

        int randomIndex = (int) (Math.random() * debuffs.size());
        BaseBuff targetBuff = debuffs.get(randomIndex);
        int oldStacks = targetBuff.getStackCount();
        targetBuff.setStack(oldStacks - 1);

        context.addLogWithMeta(
                LogType.AFFIX,
                this,
                "【词缀触发】[%s] 的 [%s] 触发，净化了 1 层 [%s]（剩余 %d 层）。",
                owner.getName(),
                getAffixName(),
                targetBuff.getBuffName(),
                targetBuff.getStackCount()
        );
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100);
    }
}
