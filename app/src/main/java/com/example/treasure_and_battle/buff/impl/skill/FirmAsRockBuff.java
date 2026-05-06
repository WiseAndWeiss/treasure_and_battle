package com.example.treasure_and_battle.buff.impl.skill;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 坚如磐石Buff
 * 效果：双防提升，持续2回合
 */
public class FirmAsRockBuff extends BaseBuff {
    private final int defenseBoostPercent; // 双防提升百分比

    public FirmAsRockBuff(String buffId, String buffName, String descriptionFormat,
                          BuffType buffType, boolean isDispellable, int maxDuration,
                          int maxStackCount, boolean refreshOnApply, float buffValue,
                          int defenseBoostPercent) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.PERMANENT,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.defenseBoostPercent = defenseBoostPercent;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 通过百分比修饰池提升双防
        attributeSet.percentPhysicalDef += defenseBoostPercent / 100.0f;
        attributeSet.percentMagicalDef += defenseBoostPercent / 100.0f;
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        // 坚如磐石buff不需要触发效果，只通过applyAttributeBonus影响属性
    }
}
