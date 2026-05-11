package com.example.treasure_and_battle.model.item;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.treasure_and_battle.model.item.equip.EquipCategory;

public class EquipCategoryTest {

    @Test
    public void testThreeCategoriesExist() {
        assertEquals(3, EquipCategory.values().length);
    }

    @Test
    public void testWeaponExists() {
        assertNotNull(EquipCategory.valueOf("WEAPON"));
    }

    @Test
    public void testArmorExists() {
        assertNotNull(EquipCategory.valueOf("ARMOR"));
    }

    @Test
    public void testAccessoryExists() {
        assertNotNull(EquipCategory.valueOf("ACCESSORY"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidThrows() {
        EquipCategory.valueOf("INVALID");
    }

    @Test
    public void testOrdinalOrder() {
        assertNotNull(EquipCategory.WEAPON);
        assertNotNull(EquipCategory.ARMOR);
        assertNotNull(EquipCategory.ACCESSORY);
    }
}
