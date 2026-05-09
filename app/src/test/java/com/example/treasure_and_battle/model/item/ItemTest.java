package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.item.material.MaterialItem;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ItemTest {

    @Test
    public void testGetters() {
        MaterialItem item = new MaterialItem("mat_1", "测试材料",
                Rarity.COMMON, 10, 99, "来源");

        assertEquals("mat_1", item.getId());
        assertEquals("测试材料", item.getName());
        assertEquals(Rarity.COMMON, item.getRarity());
        assertEquals(10, item.getBaseValue());
        assertEquals(ItemType.MATERIAL, item.getType());
        assertEquals(1, item.getCount());
        assertEquals(99, item.getMaxStack());
    }

    @Test
    public void testSetCount() {
        MaterialItem item = new MaterialItem("mat_2", "测试",
                Rarity.COMMON, 10, 99, "来源");
        item.setCount(50);
        assertEquals(50, item.getCount());
    }

    @Test
    public void testSetCountZero() {
        MaterialItem item = new MaterialItem("mat_3", "测试",
                Rarity.COMMON, 10, 99, "来源");
        item.setCount(0);
        assertEquals(0, item.getCount());
    }

    @Test
    public void testSetCountNegative() {
        MaterialItem item = new MaterialItem("mat_4", "测试",
                Rarity.COMMON, 10, 99, "来源");
        item.setCount(-5);
        assertEquals(-5, item.getCount());
    }

    @Test
    public void testCanStackTrue() {
        MaterialItem item = new MaterialItem("mat_5", "可堆叠",
                Rarity.COMMON, 10, 99, "来源");
        assertTrue(item.canStack());
    }

    @Test
    public void testCanStackFalseWhenMaxStackIsOne() {
        GemItem gem = new GemItem("gem_1", "宝石",
                Rarity.COMMON, 50, "RUBY");
        assertFalse(gem.canStack());
    }

    @Test
    public void testCanStackFalseWhenMaxStackIsZero() {
        EquipItem equip = new EquipItem("eq_1", "装备",
                Rarity.COMMON, 100, 1, EquipSlot.WEAPON);
        assertFalse(equip.canStack());
    }

    @Test
    public void testDescriptionDefaultsToEmpty() {
        MaterialItem item = new MaterialItem("mat_6", "测试",
                Rarity.COMMON, 10, 99, "来源");
        assertEquals("", item.getDescription());
    }

    @Test
    public void testSetDescription() {
        MaterialItem item = new MaterialItem("mat_7", "测试",
                Rarity.COMMON, 10, 99, "来源");
        item.setDescription("描述文字");
        assertEquals("描述文字", item.getDescription());
    }

    @Test
    public void testIconResIdDefault() {
        MaterialItem item = new MaterialItem("mat_8", "测试",
                Rarity.COMMON, 10, 99, "来源");
        assertEquals(android.R.drawable.ic_menu_gallery, item.getIconResId());
    }

    @Test
    public void testSetIconResId() {
        MaterialItem item = new MaterialItem("mat_9", "测试",
                Rarity.COMMON, 10, 99, "来源");
        item.setIconResId(123);
        assertEquals(123, item.getIconResId());
    }
}
