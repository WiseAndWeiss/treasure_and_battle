package com.example.treasure_and_battle.buff.impl.attribute;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.AttributeModifierType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;

/**
 * 力量属性Buff：单一职责，只负责力量属性的加成
 */
public class StrengthBuff extends BaseBuff {
    private final AttributeModifierType modifierType;

    public StrengthBuff(String buffId, String buffName, String descriptionFormat,
                        BuffType buffType, boolean isDispellable, int maxDuration,
                        int maxStackCount, boolean refreshOnApply, float buffValue,
                        AttributeModifierType modifierType) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.PERMANENT,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.modifierType = modifierType;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        float finalValue = this.buffValue * this.stackCount;
        // 1. 固定值力量加成
        if (modifierType == AttributeModifierType.FLAT) {
            attributeSet.strength += (int) finalValue;
        }
        // 2. 百分比力量加成
        else if (modifierType == AttributeModifierType.PERCENTAGE) {
            attributeSet.percentStrength += finalValue;
        }
    }

    @Override
    public void onTrigger(BattleContext context, BuffTriggerType triggerType) {
        // 常驻属性Buff无触发逻辑
    }
}