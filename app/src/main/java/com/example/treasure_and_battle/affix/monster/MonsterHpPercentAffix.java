package com.example.treasure_and_battle.affix.monster;

import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;

/**
 * 怪物词缀：生命值百分比提高
 */
public class MonsterHpPercentAffix extends BaseAffix {

    public MonsterHpPercentAffix(int affixId, String affixName, String description, Rarity rarity,
                                 AffixTriggerType triggerType, int[] allowSlots, float value) {
        super(
            affixId, 
            affixName, 
            description, 
            rarity, 
            triggerType, 
            allowSlots, 
            value
        );
    }

    @Override
    public void onTrigger(BattleContext context) {
        // 常驻属性词缀不会在战斗中按特定时机触发行为，仅在属性计算时生效
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // value 传入 0.20f 代表 20%
        attributeSet.percentMaxHp += this.affixValue;
    }

    @Override
    public String getDescription() {
        // 将小数转换成百分比显示，例如 0.20f -> 20%
        return String.format(description, affixValue * 100);
    }
}
