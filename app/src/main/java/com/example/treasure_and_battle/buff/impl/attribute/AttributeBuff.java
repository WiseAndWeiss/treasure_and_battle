package com.example.treasure_and_battle.buff.impl.attribute;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.utils.AttributeUtils;

/**
 * 【通用】属性修改Buff：一个类覆盖所有属性的修改
 * 核心设计：通过 AttributeType 枚举区分要修改的属性，通过 ValueType 区分固定值/百分比
 * 后续加新属性，只需要在 AttributeType 枚举里加一个，在 switch 里加一个 case，无需新建类
 */
public class AttributeBuff extends BaseBuff {
    private final AttributeType attributeType; // 【核心】要修改的属性类型
    private final ValueType valueType;         // 你已有的：固定值/百分比

    public AttributeBuff(String buffId, String buffName, String descriptionFormat,
                         BuffType buffType, boolean isDispellable, int maxDuration,
                         int maxStackCount, boolean refreshOnApply, float buffValue,
                         AttributeType attributeType, ValueType valueType) {
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.PERMANENT,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.attributeType = attributeType;
        this.valueType = valueType;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        float finalValue = this.buffValue * this.stackCount;
        AttributeUtils.applyAttributeTypeBonus(attributeSet, attributeType, valueType, finalValue);
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        // 常驻属性Buff无触发逻辑
    }
}