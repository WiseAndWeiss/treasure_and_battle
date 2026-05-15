package com.example.treasure_and_battle.affix.impl.monster.attribute;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.battle.BattleContext;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.utils.AttributeUtils;

public class MonsterAttributeAffix extends BaseMonsterAffix {
    private final AttributeType attributeType;
    private final ValueType valueType;

    public MonsterAttributeAffix(int affixId, String affixName, String description, Rarity rarity,
                                 TriggerType triggerType, float value, AttributeType attributeType, ValueType valueType) {
        super(affixId, affixName, description, rarity, triggerType, value);
        this.attributeType = attributeType;
        this.valueType = valueType;
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        // 常驻属性词缀不会在战斗中按特定时机触发行为，仅在属性计算时生效
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        AttributeUtils.applyAttributeTypeBonus(attributeSet, attributeType, valueType, affixValue);
    }

     @Override
     public String getDescription() {
        if (valueType == ValueType.PERCENTAGE) {
            return String.format(description, affixValue * 100);
        }
         return String.format(description, affixValue);
     }
}
