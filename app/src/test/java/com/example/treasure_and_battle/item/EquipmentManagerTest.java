package com.example.treasure_and_battle.item;

import android.content.Context;

import com.example.treasure_and_battle.manager.item.EquipmentManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class EquipmentManagerTest {

    // new config: SWORD=1001, BOW=2001, STAFF=3001
    // HEAVY: helmet=4001, chest=5001, leggings=6001, boots=7001
    // LIGHT: helmet=4002, chest=5002, leggings=6002, boots=7002
    // CLOTH: helmet=4003, chest=5003, leggings=6003, boots=7003
    // ACCESSORY: necklace=8001, ring=9001, bracelet=10001

    private Context context;
    private EquipmentManager manager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        manager = EquipmentManager.getInstance(context);
    }

    // ====================== 基础生成 ======================

    @Test
    public void testGenerateEquip_InvalidTemplateReturnsNull() {
        assertNull(manager.generateEquip(99999, 1, Rarity.COMMON));
    }

    @Test
    public void testGenerateEquip_BasicFields() {
        EquipItem item = manager.generateEquip(1001, 10, Rarity.COMMON);
        assertNotNull(item);
        assertEquals("equip_weapon_sword_red", item.getId());
        assertEquals("赤铁剑", item.getName());
        assertEquals(Rarity.COMMON, item.getRarity());
        assertEquals(10, item.getLevel());
        assertEquals(EquipSlot.WEAPON, item.getSlot());
    }

    @Test
    public void testGenerateEquip_RarityAffectsSockets() {
        EquipItem common = manager.generateEquip(1001, 1, Rarity.COMMON);
        EquipItem legendary = manager.generateEquip(1001, 1, Rarity.LEGENDARY);
        assertEquals("普通装备应 1 孔", 1, common.getMaxSockets());
        assertEquals("传说装备应 5 孔", 5, legendary.getMaxSockets());
    }

    // ====================== 武器属性 ======================

    @Test
    public void testSword_Attributes() {
        EquipItem item = manager.generateEquip(1001, 10, Rarity.COMMON);
        AttributeSet a = item.getBaseAttributes();
        assertTrue("剑应有物攻", a.physicalAtk > 0);
        assertTrue("剑应有物防", a.physicalDef > 0);
        assertTrue("剑应有力量", a.strength > 0);
        assertEquals("剑不应有魔攻", 0, a.magicalAtk);
    }

    @Test
    public void testBow_Attributes() {
        EquipItem item = manager.generateEquip(2001, 10, Rarity.COMMON);
        AttributeSet a = item.getBaseAttributes();
        assertTrue("弓应有物攻", a.physicalAtk > 0);
        assertTrue("弓应有暴击率", a.physicalCritRate > 0);
        assertTrue("弓应有敏捷", a.agility > 0);
        assertEquals("弓不应有力量", 0, a.strength);
    }

    @Test
    public void testStaff_Attributes() {
        EquipItem item = manager.generateEquip(3001, 10, Rarity.COMMON);
        AttributeSet a = item.getBaseAttributes();
        assertTrue("法杖应有魔攻", a.magicalAtk > 0);
        assertTrue("法杖应有魔法暴击率", a.magicalCritRate > 0);
        assertTrue("法杖应有智力", a.intelligence > 0);
        assertEquals("法杖不应有物攻", 0, a.physicalAtk);
    }

    @Test
    public void testSword_NoMagicAttack() {
        EquipItem item = manager.generateEquip(1001, 10, Rarity.COMMON);
        assertEquals(0, item.getBaseAttributes().magicalAtk);
        assertEquals(0f, item.getBaseAttributes().magicalCritRate, 0.001f);
    }

    @Test
    public void testStaff_NoPhysicalAttack() {
        EquipItem item = manager.generateEquip(3001, 10, Rarity.COMMON);
        assertEquals(0, item.getBaseAttributes().physicalAtk);
        assertEquals(0f, item.getBaseAttributes().physicalCritRate, 0.001f);
    }

    // ====================== 重甲属性 ======================

    @Test
    public void testHeavyChest_Attributes() {
        EquipItem item = manager.generateEquip(5001, 10, Rarity.COMMON);
        AttributeSet a = item.getBaseAttributes();
        assertTrue("重甲应有 HP", a.maxHp > 0);
        assertTrue("重甲应有物防", a.physicalDef > 0);
        assertTrue("重甲应有减伤", a.damageReductionRate > 0);
        assertTrue("重甲应有体魄", a.physique > 0);
        assertEquals("重甲不应有魔防", 0, a.magicalDef);
    }

    @Test
    public void testHeavyHelmet_Attributes() {
        EquipItem item = manager.generateEquip(4001, 10, Rarity.COMMON);
        AttributeSet a = item.getBaseAttributes();
        assertTrue(a.maxHp > 0);
        assertTrue(a.physicalDef > 0);
        assertTrue(a.physique > 0);
    }

    @Test
    public void testHeavyLeggings_Attributes() {
        EquipItem item = manager.generateEquip(6001, 10, Rarity.COMMON);
        AttributeSet a = item.getBaseAttributes();
        assertTrue(a.maxHp > 0);
        assertTrue(a.physicalDef > 0);
        assertTrue(a.physique > 0);
    }

    @Test
    public void testHeavyBoots_Attributes() {
        EquipItem item = manager.generateEquip(7001, 10, Rarity.COMMON);
        AttributeSet a = item.getBaseAttributes();
        assertTrue(a.maxHp > 0);
        assertTrue(a.physicalDef > 0);
        assertTrue(a.physique > 0);
    }

    @Test
    public void testHeavyChest_HasDamageReduction() {
        EquipItem chest = manager.generateEquip(5001, 10, Rarity.COMMON);
        EquipItem helmet = manager.generateEquip(4001, 10, Rarity.COMMON);
        assertEquals("只有胸甲有减伤", 0.02f, chest.getBaseAttributes().damageReductionRate, 0.001f);
        assertEquals("头盔无减伤", 0f, helmet.getBaseAttributes().damageReductionRate, 0.001f);
    }

    // ====================== 轻甲属性 ======================

    @Test
    public void testLightChest_Attributes() {
        EquipItem item = manager.generateEquip(5002, 10, Rarity.COMMON);
        AttributeSet a = item.getBaseAttributes();
        assertTrue(a.maxHp > 0);
        assertTrue(a.physicalDef > 0);
        assertTrue(a.magicalDef > 0);
        assertTrue(a.dodgeRate > 0);
        assertTrue(a.luck > 0);
    }

    @Test
    public void testLightBoots_HasDodgeRate() {
        EquipItem boots = manager.generateEquip(7002, 10, Rarity.COMMON);
        assertTrue("轻甲靴子应有闪避", boots.getBaseAttributes().dodgeRate > 0);
    }

    // ====================== 布甲属性 ======================

    @Test
    public void testClothChest_Attributes() {
        EquipItem item = manager.generateEquip(5003, 10, Rarity.COMMON);
        AttributeSet a = item.getBaseAttributes();
        assertTrue(a.maxHp > 0);
        assertTrue(a.maxMp > 0);
        assertTrue(a.magicalDef > 0);
        assertTrue(a.debuffResist > 0);
        assertTrue(a.spirit > 0);
        assertEquals("布甲不应有物防", 0, a.physicalDef);
    }

    @Test
    public void testClothChest_HasDebufResist() {
        EquipItem chest = manager.generateEquip(5003, 10, Rarity.COMMON);
        assertEquals("布甲胸应有 DEBUFF 抗性", 0.03f, chest.getBaseAttributes().debuffResist, 0.001f);
    }

    @Test
    public void testClothHelmet_NoDebufResist() {
        EquipItem helmet = manager.generateEquip(4003, 10, Rarity.COMMON);
        assertEquals("布甲头盔无抗性", 0f, helmet.getBaseAttributes().debuffResist, 0.001f);
    }

    // ====================== 护甲层级数值验证 ======================

    @Test
    public void testArmor_HpScalesBySlot() {
        EquipItem chest = manager.generateEquip(5001, 10, Rarity.COMMON);
        EquipItem helmet = manager.generateEquip(4001, 10, Rarity.COMMON);
        assertTrue("胸甲 HP 应大于头盔", chest.getBaseAttributes().maxHp > helmet.getBaseAttributes().maxHp);
    }

    @Test
    public void testArmor_RarityScalesPower() {
        EquipItem commonBoots = manager.generateEquip(7001, 1, Rarity.COMMON);
        EquipItem rareBoots = manager.generateEquip(7001, 1, Rarity.RARE);
        assertTrue("稀有装备属性应高于普通",
                rareBoots.getBaseAttributes().maxHp > commonBoots.getBaseAttributes().maxHp);
    }

    // ====================== 饰品属性 ======================

    @Test
    public void testAccessory_HasAtLeastTwoStats() {
        EquipItem necklace = manager.generateEquip(8001, 10, Rarity.COMMON);
        AttributeSet a = necklace.getBaseAttributes();
        int nonZeroCount = countNonZeroStats(a);
        assertTrue("饰品应有至少 2 种属性，实际=" + nonZeroCount, nonZeroCount >= 2);
    }

    @Test
    public void testAccessory_RingHasAtLeastTwoStats() {
        EquipItem ring = manager.generateEquip(9001, 10, Rarity.COMMON);
        int nonZeroCount = countNonZeroStats(ring.getBaseAttributes());
        assertTrue("戒指应有至少 2 种属性，实际=" + nonZeroCount, nonZeroCount >= 2);
    }

    @Test
    public void testAccessory_BraceletHasAtLeastTwoStats() {
        EquipItem bracelet = manager.generateEquip(10001, 10, Rarity.COMMON);
        int nonZeroCount = countNonZeroStats(bracelet.getBaseAttributes());
        assertTrue("手镯应有至少 2 种属性，实际=" + nonZeroCount, nonZeroCount >= 2);
    }

    @Test
    public void testAccessory_DifferentRunsMayDiffer() {
        EquipItem a = manager.generateEquip(8001, 10, Rarity.COMMON);
        EquipItem b = manager.generateEquip(8001, 10, Rarity.COMMON);
        assertNotNull(a);
        assertNotNull(b);
    }

    // ====================== 随机生成 ======================

    @Test
    public void testGenerateRandomEquip_ReturnsValidItem() {
        EquipItem item = manager.generateRandomEquip(5, Rarity.UNCOMMON);
        assertNotNull(item);
        assertNotNull(item.getId());
        assertNotNull(item.getSlot());
    }

    @Test
    public void testGenerateRandomEquip_HasAttributes() {
        EquipItem item = manager.generateRandomEquip(20, Rarity.EPIC);
        assertNotNull(item);
    }

    private int countNonZeroStats(AttributeSet a) {
        int count = 0;
        if (a.strength > 0) count++;
        if (a.agility > 0) count++;
        if (a.intelligence > 0) count++;
        if (a.spirit > 0) count++;
        if (a.physique > 0) count++;
        if (a.luck > 0) count++;
        if (a.maxHp > 0) count++;
        if (a.maxMp > 0) count++;
        if (a.physicalAtk > 0) count++;
        if (a.magicalAtk > 0) count++;
        if (a.physicalDef > 0) count++;
        if (a.magicalDef > 0) count++;
        if (a.speed > 0) count++;
        if (a.physicalCritRate > 0) count++;
        if (a.magicalCritRate > 0) count++;
        if (a.dodgeRate > 0) count++;
        if (a.hitRate > 0) count++;
        if (a.debuffResist > 0) count++;
        if (a.damageReductionRate > 0) count++;
        return count;
    }
}
