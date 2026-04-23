package com.example.treasure_and_battle.affix;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.BattleEntity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BaseMonsterAffixTest {

    @Test
    public void testMonsterAffixDefaultSlotsEmpty() {
        BaseMonsterAffix affix = new DummyMonsterAffix(
                2,
                "怪物测试词缀",
                "值%.0f",
                Rarity.EPIC,
                AffixTriggerType.ON_BATTLE_START,
                5f
        );

        assertEquals(0, affix.getAllowSlots().length);
        assertEquals(AffixTriggerType.ON_BATTLE_START, affix.getTriggerType());
        assertEquals(5f, affix.getAffixValue(), 0.0001f);
    }

    private static class DummyMonsterAffix extends BaseMonsterAffix {
        DummyMonsterAffix(int affixId, String affixName, String description, Rarity rarity,
                          AffixTriggerType triggerType, float affixValue) {
            super(affixId, affixName, description, rarity, triggerType, affixValue);
        }

        @Override
        public void onTrigger(BattleEntity owner, BattleContext context) {
            // no-op
        }

        @Override
        public void applyAttributeBonus(AttributeSet attributeSet) {
            // no-op
        }
    }
}
