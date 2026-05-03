package com.example.treasure_and_battle.buff.impl.control;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 减速Debuff
 * 效果：降低目标的速度属性
 * 衰减机制：持续指定回合数后自动移除
 */
public class SlowDebuff extends BaseBuff {
    private final float speedReductionPercent;  // 速度降低百分比

    public SlowDebuff(String buffId, String buffName, String descriptionFormat,
                       BuffType buffType, boolean isDispellable, int maxDuration,
                       int maxStackCount, boolean refreshOnApply, float buffValue,
                       float speedReductionPercent) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.PERMANENT,
              isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.speedReductionPercent = speedReductionPercent;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 降低速度
        attributeSet.speed *= (1.0f - speedReductionPercent / 100.0f);
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        // 减速buff主要在applyAttributeBonus中生效
    }

    public float getSpeedReductionPercent() {
        return speedReductionPercent;
    }
}
