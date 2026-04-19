package com.example.treasure_and_battle.affix.impl.equip;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.EquipCategory;

/**
 * 装备词缀：增加百分比力量
 */
public class EquipPercentStrAffix extends BaseEquipAffix {

    public EquipPercentStrAffix(int affixId, String affixName, String description, Rarity rarity,
                                AffixTriggerType triggerType, EquipCategory[] allowCategories, float value) {
        super(affixId, affixName, description, rarity, triggerType, allowCategories, value);
    }

    @Override
    public void onTrigger(BattleContext context) {
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        attributeSet.percentStrength += this.affixValue;
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100);
    }
}
