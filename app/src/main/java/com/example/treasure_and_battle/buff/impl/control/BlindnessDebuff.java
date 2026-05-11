package com.example.treasure_and_battle.buff.impl.control;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 致盲debuff - 降低目标的命中率
 *
 * 效果：降低持有者的命中率
 * 典型来源：法师技能"狂风呼啸"
 */
public class BlindnessDebuff extends BaseBuff {

    private final float hitRateReduction; // 命中率降低值（百分比）

    public BlindnessDebuff(String buffId, String buffName, String descriptionFormat,
                           BuffType buffType, boolean isDispellable, int maxDuration,
                           int maxStackCount, boolean refreshOnApply, float buffValue,
                           float hitRateReduction) {
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.PERMANENT,
              isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.hitRateReduction = hitRateReduction;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributes) {
        // 降低命中率
        attributes.hitRate -= hitRateReduction;
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        // 致盲效果是通过属性修饰实现的，不需要额外的触发逻辑
    }

    public float getHitRateReduction() {
        return hitRateReduction;
    }
}
