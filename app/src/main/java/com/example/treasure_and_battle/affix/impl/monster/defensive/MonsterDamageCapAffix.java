package com.example.treasure_and_battle.affix.impl.monster.defensive;

import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

public class MonsterDamageCapAffix extends BaseMonsterAffix {

    public MonsterDamageCapAffix(int affixId, String affixName, String description, Rarity rarity,
                                 TriggerType triggerType, float value) {
        super(affixId, affixName, description, rarity, triggerType, value);
    }

    public int capDamage(int incomingDamage, int maxHp) {
        int capValue = (int) (maxHp * affixValue);
        if (capValue <= 0) {
            return incomingDamage;
        }
        return Math.min(incomingDamage, capValue);
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100);
    }
}
