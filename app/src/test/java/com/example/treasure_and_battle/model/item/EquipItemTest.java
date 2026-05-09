package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EquipItemTest {

    @Test
    public void testCreation() {
        EquipItem equip = new EquipItem("sword_1", "铁剑",
                Rarity.COMMON, 50, 5, EquipSlot.WEAPON);

        assertEquals("sword_1", equip.getId());
        assertEquals("铁剑", equip.getName());
        assertEquals(Rarity.COMMON, equip.getRarity());
        assertEquals(50, equip.getBaseValue());
        assertEquals(ItemType.EQUIPMENT, equip.getType());
        assertEquals(5, equip.getLevel());
        assertEquals(EquipSlot.WEAPON, equip.getSlot());
        assertFalse(equip.canStack());
    }

    @Test
    public void testBaseAttributesNonNull() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.COMMON, 10, 1, EquipSlot.WEAPON);
        assertNotNull(equip.getBaseAttributes());
    }

    @Test
    public void testMaxSocketsCommon() {
        EquipItem equip = new EquipItem("eq", "白装",
                Rarity.COMMON, 10, 1, EquipSlot.WEAPON);
        assertEquals(1, equip.getMaxSockets());
    }

    @Test
    public void testMaxSocketsUncommon() {
        EquipItem equip = new EquipItem("eq", "绿装",
                Rarity.UNCOMMON, 10, 1, EquipSlot.WEAPON);
        assertEquals(2, equip.getMaxSockets());
    }

    @Test
    public void testMaxSocketsRare() {
        EquipItem equip = new EquipItem("eq", "蓝装",
                Rarity.RARE, 10, 1, EquipSlot.WEAPON);
        assertEquals(3, equip.getMaxSockets());
    }

    @Test
    public void testMaxSocketsEpic() {
        EquipItem equip = new EquipItem("eq", "紫装",
                Rarity.EPIC, 10, 1, EquipSlot.WEAPON);
        assertEquals(4, equip.getMaxSockets());
    }

    @Test
    public void testMaxSocketsLegendary() {
        EquipItem equip = new EquipItem("eq", "橙装",
                Rarity.LEGENDARY, 10, 1, EquipSlot.WEAPON);
        assertEquals(5, equip.getMaxSockets());
    }

    @Test
    public void testSocketedGemsInitiallyEmpty() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.COMMON, 10, 1, EquipSlot.WEAPON);
        assertNotNull(equip.getSocketedGems());
        assertTrue(equip.getSocketedGems().isEmpty());
    }

    @Test
    public void testSocketGemSuccess() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.UNCOMMON, 10, 1, EquipSlot.WEAPON);
        GemItem ruby = new GemItem("ruby", "红宝石", Rarity.COMMON, 50, "RUBY");

        assertTrue(equip.socketGem(ruby));
        assertEquals(1, equip.getSocketedGems().size());
    }

    @Test
    public void testSocketGemNull() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.COMMON, 10, 1, EquipSlot.WEAPON);

        assertFalse(equip.socketGem(null));
    }

    @Test
    public void testSocketGemFull() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.COMMON, 10, 1, EquipSlot.WEAPON);
        GemItem ruby = new GemItem("ruby", "红宝石", Rarity.COMMON, 50, "RUBY");
        GemItem emerald = new GemItem("emerald", "绿宝石", Rarity.COMMON, 50, "EMERALD");

        assertTrue(equip.socketGem(ruby));
        assertFalse(equip.socketGem(emerald));
    }

    @Test
    public void testSocketGemDuplicateType() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.RARE, 10, 1, EquipSlot.WEAPON);
        GemItem ruby1 = new GemItem("ruby1", "红宝石1", Rarity.COMMON, 50, "RUBY");
        GemItem ruby2 = new GemItem("ruby2", "红宝石2", Rarity.COMMON, 50, "RUBY");

        assertTrue(equip.socketGem(ruby1));
        assertFalse(equip.socketGem(ruby2));
        assertEquals(1, equip.getSocketedGems().size());
    }

    @Test
    public void testUnsocketGemValid() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.UNCOMMON, 10, 1, EquipSlot.WEAPON);
        GemItem ruby = new GemItem("ruby", "红宝石", Rarity.COMMON, 50, "RUBY");
        equip.socketGem(ruby);

        GemItem removed = equip.unsocketGem(0);
        assertNotNull(removed);
        assertEquals("RUBY", removed.getGemType());
        assertTrue(equip.getSocketedGems().isEmpty());
    }

    @Test
    public void testUnsocketGemInvalidIndex() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.COMMON, 10, 1, EquipSlot.WEAPON);

        assertNull(equip.unsocketGem(0));
        assertNull(equip.unsocketGem(-1));
        assertNull(equip.unsocketGem(5));
    }

    @Test
    public void testGetTotalGemBonusesEmpty() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.COMMON, 10, 1, EquipSlot.WEAPON);

        AttributeSet bonuses = equip.getTotalGemBonuses();
        assertNotNull(bonuses);
        assertEquals(0, bonuses.physicalAtk);
    }

    @Test
    public void testGetTotalGemBonusesWithGems() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.RARE, 10, 1, EquipSlot.WEAPON);

        GemItem ruby = new GemItem("ruby", "红宝石", Rarity.COMMON, 50, "RUBY");
        ruby.getWeaponBonus().physicalAtk = 2;
        equip.socketGem(ruby);

        GemItem emerald = new GemItem("emerald", "绿宝石", Rarity.COMMON, 50, "EMERALD");
        emerald.getWeaponBonus().hitRate = 0.006f;
        equip.socketGem(emerald);

        AttributeSet bonuses = equip.getTotalGemBonuses();
        assertEquals(2, bonuses.physicalAtk);
        assertEquals(0.006f, bonuses.hitRate, 0.0001f);
    }

    @Test
    public void testGetTotalGemBonusesWithCategorySwap() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.RARE, 10, 1, EquipSlot.RING);

        GemItem ruby = new GemItem("ruby", "红宝石", Rarity.COMMON, 50, "RUBY");
        ruby.getAccessoryBonus().strength = 1;
        ruby.getWeaponBonus().physicalAtk = 2;
        equip.socketGem(ruby);

        AttributeSet bonuses = equip.getTotalGemBonuses();
        assertEquals(1, bonuses.strength);
        assertEquals(0, bonuses.physicalAtk);
    }

    @Test
    public void testAffixesInitiallyEmpty() {
        EquipItem equip = new EquipItem("eq", "装备",
                Rarity.COMMON, 10, 1, EquipSlot.WEAPON);
        assertNotNull(equip.getAffixes());
        assertTrue(equip.getAffixes().isEmpty());
    }
}
