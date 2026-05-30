package com.example.treasure_and_battle.affix.impl.equip.trigger;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;
import com.example.treasure_and_battle.utils.RandomUtils;

public class EquipTriggerCritReduceApAffix extends BaseEquipAffix {

    public EquipTriggerCritReduceApAffix(int affixId, String affixName, String description, Rarity rarity,
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

        BattleEntity target = context.currentTarget;
        if (target == null || target.isDead()) {
            return;
        }

        if (!RandomUtils.checkProbability(affixValue)) {
            return;
        }

        int currentAp = target.getCurrentActionPoints();
        if (currentAp <= 0) {
            return;
        }

        target.setCurrentActionPoints(Math.max(0, currentAp - 1));

        context.addLogWithMeta(
                LogType.AFFIX,
                this,
                "【词缀触发】[%s] 的 [%s] 触发，[%s] 失去 1 点行动点（剩余 %d 点）。",
                owner.getName(),
                getAffixName(),
                target.getName(),
                target.getCurrentActionPoints()
        );
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100);
    }
}
