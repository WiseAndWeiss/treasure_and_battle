package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNotNull;

public class GemItemTest {

    @Test
    public void testCreation() {
        GemItem gem = new GemItem("ruby_普通", "普通红宝石",
                Rarity.COMMON, 50, "RUBY");

        assertEquals("ruby_普通", gem.getId());
        assertEquals("普通红宝石", gem.getName());
        assertEquals(Rarity.COMMON, gem.getRarity());
        assertEquals(50, gem.getBaseValue());
        assertEquals(ItemType.GEM, gem.getType());
        assertEquals("RUBY", gem.getGemType());
        assertFalse(gem.canStack());
    }

    @Test
    public void testBonusSetsNonNullAfterConstruction() {
        GemItem gem = new GemItem("test_gem", "测试宝石",
                Rarity.COMMON, 10, "TEST");

        assertNotNull(gem.getAccessoryBonus());
        assertNotNull(gem.getWeaponBonus());
        assertNotNull(gem.getArmorBonus());
    }

    @Test
    public void testGetBonusForCategoryWeapon() {
        GemItem gem = new GemItem("test_gem", "测试宝石",
                Rarity.COMMON, 10, "TEST");
        gem.getWeaponBonus().physicalAtk = 5;

        AttributeSet bonus = gem.getBonusForCategory(EquipCategory.WEAPON);
        assertEquals(5, bonus.physicalAtk);
    }

    @Test
    public void testGetBonusForCategoryAccessory() {
        GemItem gem = new GemItem("test_gem", "测试宝石",
                Rarity.COMMON, 10, "TEST");
        gem.getAccessoryBonus().strength = 3;

        AttributeSet bonus = gem.getBonusForCategory(EquipCategory.ACCESSORY);
        assertEquals(3, bonus.strength);
    }

    @Test
    public void testGetBonusForCategoryArmor() {
        GemItem gem = new GemItem("test_gem", "测试宝石",
                Rarity.COMMON, 10, "TEST");
        gem.getArmorBonus().physicalDef = 2;

        AttributeSet bonus = gem.getBonusForCategory(EquipCategory.ARMOR);
        assertEquals(2, bonus.physicalDef);
    }

    @Test
    public void testBonusSetsAreDistinctObjects() {
        GemItem gem = new GemItem("test_gem", "测试宝石",
                Rarity.COMMON, 10, "TEST");

        AttributeSet acc = gem.getAccessoryBonus();
        AttributeSet wpn = gem.getWeaponBonus();
        AttributeSet arm = gem.getArmorBonus();

        assertNotSame(acc, wpn);
        assertNotSame(wpn, arm);
        assertNotSame(acc, arm);
    }

    @Test
    public void testGemTypeStored() {
        GemItem ruby = new GemItem("ruby", "红宝石", Rarity.COMMON, 50, "RUBY");
        GemItem emerald = new GemItem("emerald", "绿宝石", Rarity.COMMON, 50, "EMERALD");

        assertEquals("RUBY", ruby.getGemType());
        assertEquals("EMERALD", emerald.getGemType());
    }

    @Test
    public void testLegendaryGemCreation() {
        GemItem gem = new GemItem("ruby_传说", "传说红宝石",
                Rarity.LEGENDARY, 2000, "RUBY");

        assertEquals(Rarity.LEGENDARY, gem.getRarity());
        assertEquals(2000, gem.getBaseValue());
    }

    @Test
    public void testCountAndMaxStackAlwaysOne() {
        GemItem gem = new GemItem("test", "测试", Rarity.COMMON, 10, "TEST");

        assertEquals(1, gem.getCount());
        assertEquals(1, gem.getMaxStack());
        assertFalse(gem.canStack());
    }
}
