package com.example.treasure_and_battle.affix.impl.monster.attribute;

import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MonsterAttributeAffixTest {

    @Test
    public void testFlatAndPercentageApplyToDifferentFields() {
        MonsterAttributeAffix flat = new MonsterAttributeAffix(
                201,
                "怪物物攻固定",
                "物攻+%.0f",
                Rarity.UNCOMMON,
                TriggerType.PERMANENT,
                12f,
                AttributeType.PHYSICAL_ATK,
                ValueType.FLAT
        );

        MonsterAttributeAffix percent = new MonsterAttributeAffix(
                202,
                "怪物物攻百分比",
                "物攻+%.0f%%",
                Rarity.UNCOMMON,
                TriggerType.PERMANENT,
                0.15f,
                AttributeType.PHYSICAL_ATK,
                ValueType.PERCENTAGE
        );

        AttributeSet set = new AttributeSet();
        flat.applyAttributeBonus(set);
        percent.applyAttributeBonus(set);

        assertEquals(12, set.physicalAtk);
        assertEquals(0.15f, set.percentPhysicalAtk, 0.0001f);
    }

    @Test
    public void testDescriptionForPercentageShouldMultiplyBy100() {
        MonsterAttributeAffix affix = new MonsterAttributeAffix(
                203,
                "怪物命中",
                "命中提高%.0f%%",
                Rarity.RARE,
                TriggerType.PERMANENT,
                0.2f,
                AttributeType.HIT_RATE,
                ValueType.PERCENTAGE
        );

        assertEquals("命中提高20%", affix.getDescription());
    }
}
