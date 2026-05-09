package com.example.treasure_and_battle.model.item;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ItemTypeTest {

    @Test
    public void testFourTypesExist() {
        assertEquals(4, ItemType.values().length);
    }

    @Test
    public void testEquipmentExists() {
        assertNotNull(ItemType.valueOf("EQUIPMENT"));
    }

    @Test
    public void testConsumableExists() {
        assertNotNull(ItemType.valueOf("CONSUMABLE"));
    }

    @Test
    public void testMaterialExists() {
        assertNotNull(ItemType.valueOf("MATERIAL"));
    }

    @Test
    public void testGemExists() {
        assertNotNull(ItemType.valueOf("GEM"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidThrows() {
        ItemType.valueOf("NOT_A_TYPE");
    }

    @Test
    public void testValueOfCaseSensitive() {
        assertNotNull(ItemType.valueOf("EQUIPMENT"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfLowerCaseFails() {
        ItemType.valueOf("equipment");
    }

    @Test
    public void testOrdinalOrder() {
        assertTrue(ItemType.EQUIPMENT.ordinal() == 0);
        assertTrue(ItemType.CONSUMABLE.ordinal() == 1);
        assertTrue(ItemType.MATERIAL.ordinal() == 2);
        assertTrue(ItemType.GEM.ordinal() == 3);
    }
}
