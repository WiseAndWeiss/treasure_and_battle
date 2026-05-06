package com.example.treasure_and_battle.buff.impl.attribute;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 虚弱Debuff
 * 效果：降低物理攻击力和法术攻击力
 */
public class WeaknessDebuff extends BaseBuff {
    private final int attackReductionPercent; // 攻击力降低百分比

    public WeaknessDebuff(String buffId, String buffName, String descriptionFormat,
                          BuffType buffType, boolean isDispellable, int maxDuration,
                          int maxStackCount, boolean refreshOnApply, float buffValue,
                          int attackReductionPercent) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.PERMANENT,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.attackReductionPercent = attackReductionPercent;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 通过百分比修饰池降低攻击力
        attributeSet.percentPhysicalAtk -= attackReductionPercent / 100.0f;
        attributeSet.percentMagicalAtk -= attackReductionPercent / 100.0f;
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        // 虚弱debuff不需要触发效果，只通过applyAttributeBonus影响属性
    }
}
