package com.example.treasure_and_battle.affix.impl.equip;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.EquipCategory;

/**
 * 装备词缀：增加百分比物理攻击力
 */
public class EquipPercentAtkAffix extends BaseEquipAffix {

    public EquipPercentAtkAffix(int affixId, String affixName, String description, Rarity rarity,
                                AffixTriggerType triggerType, EquipCategory[] allowCategories, float value) {
        super(affixId, affixName, description, rarity, triggerType, allowCategories, value);
    }

    @Override
    public void onTrigger(BattleContext context) {
        // 常驻属性词缀不需要在战斗流中特定时机触发逻辑
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        attributeSet.percentPhysicalAtk += this.affixValue;
    }

    @Override
    public String getDescription() {
        // 动态转为百分比展示，比如 0.1 代表 10%
        return String.format(description, affixValue * 100);
    }
}
