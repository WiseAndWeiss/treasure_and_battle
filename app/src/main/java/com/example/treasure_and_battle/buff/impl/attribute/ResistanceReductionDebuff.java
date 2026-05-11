package com.example.treasure_and_battle.buff.impl.attribute;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 易伤Debuff - 降低物理防御和法术防御
 *
 * 效果：降低持有者的物理防御和法术防御一定百分比
 * 典型来源：游侠技能"蚀弱穿射"
 */
public class ResistanceReductionDebuff extends BaseBuff {

    private final int defenseReductionPercent; // 防御力降低百分比

    public ResistanceReductionDebuff(String buffId, String buffName, String descriptionFormat,
                                     BuffType buffType, boolean isDispellable, int maxDuration,
                                     int maxStackCount, boolean refreshOnApply, float buffValue,
                                     int defenseReductionPercent) {
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.PERMANENT,
              isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.defenseReductionPercent = defenseReductionPercent;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 通过百分比修饰池降低物理防御和法术防御
        attributeSet.percentPhysicalDef -= defenseReductionPercent / 100.0f;
        attributeSet.percentMagicalDef -= defenseReductionPercent / 100.0f;
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        // 易伤debuff不需要触发效果，只通过applyAttributeBonus影响属性
    }
}
