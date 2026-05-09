package com.example.treasure_and_battle.affix.impl;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.affix.BaseAffix;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.BattleEntity;

// 常驻力量加成词缀
public class PermanentStrengthAffix extends BaseAffix {

    public PermanentStrengthAffix(int affixId, String affixName, String description, Rarity rarity, int[] allowSlots, float affixValue) {
        super(affixId, affixName, description, rarity, TriggerType.PERMANENT, allowSlots, affixValue);
    }

    // 常驻属性词缀：实现属性加成
    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 直接给力量属性加词缀数值
        attributeSet.strength += (int) this.affixValue;
    }

    // 常驻词缀不需要触发逻辑，空实现即可
    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        // 无逻辑
    }
}