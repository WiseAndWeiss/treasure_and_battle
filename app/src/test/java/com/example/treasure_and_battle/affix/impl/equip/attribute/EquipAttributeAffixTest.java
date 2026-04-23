package com.example.treasure_and_battle.affix.impl.equip.attribute;

import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.affix.EquipAffixScope;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.item.EquipCategory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EquipAttributeAffixTest {

    @Test
    public void testGlobalFlatAffixApplyToGlobalAttributeSet() {
        EquipAttributeAffix affix = new EquipAttributeAffix(
                2001,
                "力量词缀",
                "力量+%.0f",
                Rarity.UNCOMMON,
                AffixTriggerType.PERMANENT,
                new EquipCategory[]{EquipCategory.WEAPON},
                8f,
                AttributeType.STRENGTH,
                ValueType.FLAT,
                EquipAffixScope.GLOBAL
        );

        AttributeSet set = new AttributeSet();
        affix.applyAttributeBonus(set);

        assertEquals(8, set.strength);
    }

    @Test
    public void testEquipmentOnlyDoesNotApplyToGlobalButAppliesToEquipment() {
        EquipAttributeAffix affix = new EquipAttributeAffix(
                2002,
                "武器攻击词缀",
                "武器攻击+%.0f%%",
                Rarity.RARE,
                AffixTriggerType.PERMANENT,
                new EquipCategory[]{EquipCategory.WEAPON},
                0.20f,
                AttributeType.PHYSICAL_ATK,
                ValueType.PERCENTAGE,
                EquipAffixScope.EQUIPMENT_ONLY
        );

        AttributeSet globalSet = new AttributeSet();
        affix.applyAttributeBonus(globalSet);
        assertEquals(0f, globalSet.percentPhysicalAtk, 0.0001f);

        AttributeSet equipSet = new AttributeSet();
        affix.applyToEquipmentAttributeBonus(equipSet);
        assertEquals(0.20f, equipSet.percentPhysicalAtk, 0.0001f);
    }

    @Test
    public void testPercentageDescriptionShouldMultiplyBy100() {
        EquipAttributeAffix affix = new EquipAttributeAffix(
                2003,
                "命中词缀",
                "命中提高%.0f%%",
                Rarity.EPIC,
                AffixTriggerType.PERMANENT,
                new EquipCategory[]{EquipCategory.ACCESSORY},
                0.15f,
                AttributeType.HIT_RATE,
                ValueType.PERCENTAGE,
                EquipAffixScope.GLOBAL
        );

        assertEquals("命中提高15%", affix.getDescription());
    }
}
