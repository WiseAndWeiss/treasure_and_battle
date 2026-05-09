package com.example.treasure_and_battle.affix;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.BattleEntity;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BaseAffixTest {

    @Test
    public void testGettersAndDynamicDescription() {
        BaseAffix affix = new DummyAffix(
                1001,
                "测试词缀",
                "提升%.1f点",
                Rarity.RARE,
                TriggerType.PERMANENT,
                new int[]{1, 3},
                12.5f
        );

        assertEquals(1001, affix.getAffixId());
        assertEquals("测试词缀", affix.getAffixName());
        assertEquals("提升12.5点", affix.getDescription());
        assertEquals(Rarity.RARE, affix.getRarity());
        assertEquals(TriggerType.PERMANENT, affix.getTriggerType());
        assertArrayEquals(new int[]{1, 3}, affix.getAllowSlots());
        assertEquals(12.5f, affix.getAffixValue(), 0.0001f);
    }

    private static class DummyAffix extends BaseAffix {
        DummyAffix(int affixId, String affixName, String description, Rarity rarity,
                   TriggerType triggerType, int[] allowSlots, float affixValue) {
            super(affixId, affixName, description, rarity, triggerType, allowSlots, affixValue);
        }

        @Override
        public void onTrigger(BattleEntity owner, BattleContext context) {
            // no-op
        }

        @Override
        public void applyAttributeBonus(AttributeSet attributeSet) {
            attributeSet.strength += (int) getAffixValue();
        }
    }
}
