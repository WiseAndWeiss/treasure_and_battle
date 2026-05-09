package com.example.treasure_and_battle.affix;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.EquipCategory;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BaseEquipAffixTest {

    @Test
    public void testAllowCategoriesAndDefaultAllowSlots() {
        BaseEquipAffix affix = new DummyEquipAffix(
                1,
                "装备测试词缀",
                "值%.0f",
                Rarity.UNCOMMON,
                TriggerType.ON_HIT,
                new EquipCategory[]{EquipCategory.WEAPON, EquipCategory.ACCESSORY},
                3f
        );

        assertArrayEquals(new EquipCategory[]{EquipCategory.WEAPON, EquipCategory.ACCESSORY}, affix.getAllowCategories());
        assertEquals(0, affix.getAllowSlots().length);
        assertEquals(3f, affix.getAffixValue(), 0.0001f);
    }

    private static class DummyEquipAffix extends BaseEquipAffix {
        DummyEquipAffix(int affixId, String affixName, String description, Rarity rarity,
                        TriggerType triggerType, EquipCategory[] allowCategories, float affixValue) {
            super(affixId, affixName, description, rarity, triggerType, allowCategories, affixValue);
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
