package com.example.treasure_and_battle.affix.impl.equip;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.EquipCategory;

/**
 * 装备词缀：增加固定物理攻击力
 */
public class EquipFlatAtkAffix extends BaseEquipAffix {

    public EquipFlatAtkAffix(int affixId, String affixName, String description, Rarity rarity,
                             AffixTriggerType triggerType, EquipCategory[] allowCategories, float value) {
        super(affixId, affixName, description, rarity, triggerType, allowCategories, value);
    }

    @Override
    public void onTrigger(BattleContext context) {
        // 常驻属性词缀不需要在战斗流中特定时机触发逻辑
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        attributeSet.physicalAtk += (int) this.affixValue;
    }
}
