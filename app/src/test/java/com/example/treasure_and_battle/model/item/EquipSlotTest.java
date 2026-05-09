package com.example.treasure_and_battle.model.item;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EquipSlotTest {

    @Test
    public void testAllEightSlotsExist() {
        assertEquals(8, EquipSlot.values().length);
    }

    @Test
    public void testWeaponCategory() {
        assertEquals(EquipCategory.WEAPON, EquipSlot.WEAPON.getCategory());
    }

    @Test
    public void testHelmetCategory() {
        assertEquals(EquipCategory.ARMOR, EquipSlot.HELMET.getCategory());
    }

    @Test
    public void testChestCategory() {
        assertEquals(EquipCategory.ARMOR, EquipSlot.CHEST.getCategory());
    }

    @Test
    public void testLeggingsCategory() {
        assertEquals(EquipCategory.ARMOR, EquipSlot.LEGGINGS.getCategory());
    }

    @Test
    public void testBootsCategory() {
        assertEquals(EquipCategory.ARMOR, EquipSlot.BOOTS.getCategory());
    }

    @Test
    public void testNecklaceCategory() {
        assertEquals(EquipCategory.ACCESSORY, EquipSlot.NECKLACE.getCategory());
    }

    @Test
    public void testRingCategory() {
        assertEquals(EquipCategory.ACCESSORY, EquipSlot.RING.getCategory());
    }

    @Test
    public void testBraceletCategory() {
        assertEquals(EquipCategory.ACCESSORY, EquipSlot.BRACELET.getCategory());
    }

    @Test
    public void testValueOfValid() {
        assertNotNull(EquipSlot.valueOf("WEAPON"));
        assertNotNull(EquipSlot.valueOf("HELMET"));
        assertNotNull(EquipSlot.valueOf("CHEST"));
        assertNotNull(EquipSlot.valueOf("LEGGINGS"));
        assertNotNull(EquipSlot.valueOf("BOOTS"));
        assertNotNull(EquipSlot.valueOf("NECKLACE"));
        assertNotNull(EquipSlot.valueOf("RING"));
        assertNotNull(EquipSlot.valueOf("BRACELET"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidThrows() {
        EquipSlot.valueOf("INVALID");
    }
}
