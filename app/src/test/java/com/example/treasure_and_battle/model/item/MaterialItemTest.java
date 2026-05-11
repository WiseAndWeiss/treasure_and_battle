package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.material.MaterialItem;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MaterialItemTest {

    @Test
    public void testCreation() {
        MaterialItem item = new MaterialItem("slime_gel_common", "史莱姆凝胶",
                Rarity.COMMON, 5, 99, "史莱姆");

        assertEquals("slime_gel_common", item.getId());
        assertEquals("史莱姆凝胶", item.getName());
        assertEquals(Rarity.COMMON, item.getRarity());
        assertEquals(5, item.getBaseValue());
        assertEquals(ItemType.MATERIAL, item.getType());
        assertEquals(99, item.getMaxStack());
        assertTrue(item.canStack());
        assertEquals("史莱姆", item.getDropFrom());
    }

    @Test
    public void testCreationDefaultCountIsOne() {
        MaterialItem item = new MaterialItem("mat_a", "材料A",
                Rarity.COMMON, 5, 99, "来源");
        assertEquals(1, item.getCount());
    }

    @Test
    public void testSetDropFrom() {
        MaterialItem item = new MaterialItem("mat_b", "材料B",
                Rarity.COMMON, 5, 99, "史莱姆");
        item.setDropFrom("狼");
        assertEquals("狼", item.getDropFrom());
    }

    @Test
    public void testSetDropFromNull() {
        MaterialItem item = new MaterialItem("mat_c", "材料C",
                Rarity.COMMON, 5, 99, "来源");
        item.setDropFrom(null);
        assertEquals(null, item.getDropFrom());
    }

    @Test
    public void testRarityCorrectlyStored() {
        MaterialItem common = new MaterialItem("mat_d", "D",
                Rarity.COMMON, 5, 99, "来源");
        MaterialItem legendary = new MaterialItem("mat_e", "E",
                Rarity.LEGENDARY, 1000, 1, "来源");

        assertEquals(Rarity.COMMON, common.getRarity());
        assertEquals(Rarity.LEGENDARY, legendary.getRarity());
    }

    @Test
    public void testMaxStackBoundaryMin() {
        MaterialItem item = new MaterialItem("mat_f", "F",
                Rarity.COMMON, 5, 1, "来源");
        assertEquals(1, item.getMaxStack());
        assertFalse(item.canStack());
    }

    @Test
    public void testMaxStackBoundaryLarge() {
        MaterialItem item = new MaterialItem("mat_g", "G",
                Rarity.COMMON, 5, 9999, "来源");
        assertEquals(9999, item.getMaxStack());
        assertTrue(item.canStack());
    }
}
