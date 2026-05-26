package com.example.treasure_and_battle.affix.impl.equip.attribute;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;

import com.example.treasure_and_battle.model.affix.EquipAffixScope;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;
import com.example.treasure_and_battle.utils.AttributeUtils;

/**
 * 通用装备属性词缀：通过 AttributeType + ValueType 统一承载固定值/百分比属性加成。
 */
public class EquipAttributeAffix extends BaseEquipAffix {

    private final AttributeType attributeType;
    private final ValueType valueType;
    private final EquipAffixScope affixScope;

    public EquipAttributeAffix(int affixId, String affixName, String description, Rarity rarity,
                               TriggerType triggerType, EquipCategory[] allowCategories, float value,
                               AttributeType attributeType, ValueType valueType, EquipAffixScope affixScope) {
        super(affixId, affixName, description, rarity, triggerType, allowCategories, value);
        this.attributeType = attributeType;
        this.valueType = valueType;
        this.affixScope = affixScope == null ? EquipAffixScope.GLOBAL : affixScope;
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        // 常驻属性词缀不需要在战斗流中特定时机触发逻辑
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        if (affixScope != EquipAffixScope.GLOBAL) {
            // EQUIPMENT_ONLY 的应用位置应在装备属性计算链路，这里先避免误加到全局属性池。
            return;
        }

        applyByType(attributeSet);
    }

    /**
     * 装备生成阶段使用：按词缀定义写入属性，不做作用域过滤。
     */
    public void applyToEquipmentAttributeBonus(AttributeSet attributeSet) {
        applyByType(attributeSet);
    }

    public EquipAffixScope getAffixScope() {
        return affixScope;
    }

    private void applyByType(AttributeSet attributeSet) {
        AttributeUtils.applyAttributeTypeBonus(attributeSet, attributeType, valueType, affixValue);
    }

    @Override
    public String getDescription() {
        if (description == null) {
            return "";
        }
        try {
            if (valueType == ValueType.PERCENTAGE) {
                return String.format(description, affixValue * 100);
            }
            return String.format(description, affixValue);
        } catch (RuntimeException e) {
            return description;
        }
    }
}
