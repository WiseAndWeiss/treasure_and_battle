package com.example.treasure_and_battle.model.merchant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.ItemType;

import org.junit.Test;

import java.util.List;
import java.util.Random;

/**
 * 四种商人类型完整测试
 * <p>
 * §1  WV（流浪商贩）：商品栏位结构、稀有度规则、类型分布、交易限制、背包过滤
 * §2  EM（装备商人）：首栏传说、末栏普通、全装备、稀有度分布、背包仅装备
 * §3  CV（商队）：首栏传说宝石、次栏传说药水、≥罕见、稀有度分布
 * §4  MM（材料商人）：首栏传说宝石、次栏传说药水、仅宝石药水、背包仅宝石药水材料
 * §5  跨类型通用：eventKey/name/desc/canBuy/canSell/等级范围/SLOTS
 */
public class MerchantTest {

    private static final int SLOTS = 8;

    // ==================== 1. 流浪商贩 WANDERING_VENDOR ====================

    @Test
    public void testWV_GenerateSlots_Count() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.WANDERING_VENDOR, new Random(42));
        assertEquals("8 栏位", 8, slots.size());
    }

    @Test
    public void testWV_SlotTypes_Rotate() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.WANDERING_VENDOR, new Random(42));
        assertEquals("第0栏 EQUIPMENT", ItemType.EQUIPMENT, slots.get(0).itemType);
        assertEquals("第1栏 GEM", ItemType.GEM, slots.get(1).itemType);
        assertEquals("第2栏 CONSUMABLE", ItemType.CONSUMABLE, slots.get(2).itemType);
        assertEquals("第3栏 EQUIPMENT", ItemType.EQUIPMENT, slots.get(3).itemType);
    }

    @Test
    public void testWV_RarityPool() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.WANDERING_VENDOR, new Random(42));
        assertEquals("第0栏 COMMON", Rarity.COMMON, slots.get(0).rarity);
        assertEquals("第1栏 COMMON", Rarity.COMMON, slots.get(1).rarity);
        assertEquals("第2栏 UNCOMMON", Rarity.UNCOMMON, slots.get(2).rarity);
        assertEquals("第3栏 RARE", Rarity.RARE, slots.get(3).rarity);
    }

    @Test
    public void testWV_NoSlotExceedsRare() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.WANDERING_VENDOR, new Random(42));
        for (MerchantConfig.MerchantSlot slot : slots) {
            assertTrue("不高于 RARE: " + slot.rarity,
                    slot.rarity.ordinal() <= Rarity.RARE.ordinal());
        }
    }

    @Test
    public void testWV_RollRarity_Distribution() {
        Random rng = new Random(42);
        int trials = 50000;
        int common = 0, uncommon = 0, rare = 0;
        for (int i = 0; i < trials; i++) {
            Rarity r = MerchantConfig.rollWanderingVendorRarity(rng);
            if (r == Rarity.COMMON) common++;
            else if (r == Rarity.UNCOMMON) uncommon++;
            else rare++;
        }
        assertEquals("COMMON 50%", 0.50, (double) common / trials, 0.02);
        assertEquals("UNCOMMON 25%", 0.25, (double) uncommon / trials, 0.02);
        assertEquals("RARE 25%", 0.25, (double) rare / trials, 0.02);
    }

    @Test
    public void testWV_RollType_Distribution() {
        Random rng = new Random(42);
        int trials = 30000;
        int[] counts = new int[3];
        for (int i = 0; i < trials; i++) {
            ItemType t = MerchantConfig.rollWanderingVendorType(rng);
            if (t == ItemType.EQUIPMENT) counts[0]++;
            else if (t == ItemType.GEM) counts[1]++;
            else counts[2]++;
        }
        for (int j = 0; j < 3; j++) {
            assertEquals("类型等概率 33.3%", 1.0 / 3.0, (double) counts[j] / trials, 0.02);
        }
    }

    @Test
    public void testWV_CanBuy() {
        assertTrue(MerchantConfig.canBuy(MerchantConfig.Type.WANDERING_VENDOR));
    }

    @Test
    public void testWV_CannotSell() {
        assertFalse(MerchantConfig.canSell(MerchantConfig.Type.WANDERING_VENDOR));
    }

    @Test
    public void testWV_SellRejection() {
        String msg = MerchantConfig.getSellRejectionMessage(MerchantConfig.Type.WANDERING_VENDOR);
        assertEquals("小贩表示不需要这些东西，你无法出售", msg);
    }

    @Test
    public void testWV_BagFilter_AllTypes() {
        List<ItemType> types = MerchantConfig.getPlayerBagFilterTypes(
                MerchantConfig.Type.WANDERING_VENDOR);
        assertEquals(4, types.size());
        assertTrue(types.contains(ItemType.EQUIPMENT));
        assertTrue(types.contains(ItemType.GEM));
        assertTrue(types.contains(ItemType.CONSUMABLE));
        assertTrue(types.contains(ItemType.MATERIAL));
    }

    @Test
    public void testWV_SlotsHaveCorrectIndex() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.WANDERING_VENDOR, new Random(42));
        for (int i = 0; i < slots.size(); i++) {
            assertEquals("槽位索引 " + i, i, slots.get(i).slotIndex);
        }
    }

    // ==================== 2. 装备商人 EQUIPMENT_MERCHANT ====================

    @Test
    public void testEM_GenerateSlots_Count() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.EQUIPMENT_MERCHANT, new Random(42));
        assertEquals("8 栏位", 8, slots.size());
    }

    @Test
    public void testEM_FirstSlotLegendary() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.EQUIPMENT_MERCHANT, new Random(42));
        assertEquals(Rarity.LEGENDARY, slots.get(0).rarity);
        assertEquals(ItemType.EQUIPMENT, slots.get(0).itemType);
    }

    @Test
    public void testEM_LastTwoSlotsCommon() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.EQUIPMENT_MERCHANT, new Random(42));
        assertEquals(Rarity.COMMON, slots.get(6).rarity);
        assertEquals(Rarity.COMMON, slots.get(7).rarity);
    }

    @Test
    public void testEM_AllSlotsAreEquipment() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.EQUIPMENT_MERCHANT, new Random(42));
        for (MerchantConfig.MerchantSlot slot : slots) {
            assertEquals("仅装备", ItemType.EQUIPMENT, slot.itemType);
        }
    }

    @Test
    public void testEM_RollRarity_Distribution() {
        Random rng = new Random(42);
        int trials = 100000;
        int legend = 0, epic = 0, rare = 0, uncommon = 0, common = 0;
        for (int i = 0; i < trials; i++) {
            Rarity r = MerchantConfig.rollEquipmentMerchantRarity(rng);
            if (r == Rarity.LEGENDARY) legend++;
            else if (r == Rarity.EPIC) epic++;
            else if (r == Rarity.RARE) rare++;
            else if (r == Rarity.UNCOMMON) uncommon++;
            else common++;
        }
        assertEquals("传说 3%，容差1%", 0.03, (double) legend / trials, 0.01);
        assertEquals("史诗 12%，容差1.5%", 0.12, (double) epic / trials, 0.015);
        assertEquals("罕见 20%，容差1.5%", 0.20, (double) rare / trials, 0.015);
        assertEquals("稀有 25%，容差1.5%", 0.25, (double) uncommon / trials, 0.015);
        assertEquals("普通 40%，容差1.5%", 0.40, (double) common / trials, 0.015);
    }

    @Test
    public void testEM_CanBuy() {
        assertTrue(MerchantConfig.canBuy(MerchantConfig.Type.EQUIPMENT_MERCHANT));
    }

    @Test
    public void testEM_CanSell() {
        assertTrue(MerchantConfig.canSell(MerchantConfig.Type.EQUIPMENT_MERCHANT));
    }

    @Test
    public void testEM_SellRejectionNull() {
        assertNull(MerchantConfig.getSellRejectionMessage(MerchantConfig.Type.EQUIPMENT_MERCHANT));
    }

    @Test
    public void testEM_BagFilter_OnlyEquipment() {
        List<ItemType> types = MerchantConfig.getPlayerBagFilterTypes(
                MerchantConfig.Type.EQUIPMENT_MERCHANT);
        assertEquals(1, types.size());
        assertEquals(ItemType.EQUIPMENT, types.get(0));
    }

    @Test
    public void testEM_MiddleSlotsAreEquipment() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.EQUIPMENT_MERCHANT, new Random(42));
        for (int i = 1; i < 6; i++) {
            assertEquals("栏位" + i + " 为装备", ItemType.EQUIPMENT, slots.get(i).itemType);
        }
    }

    // ==================== 3. 商队 CARAVAN ====================

    @Test
    public void testCV_GenerateSlots_Count() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.CARAVAN, new Random(42));
        assertEquals("8 栏位", 8, slots.size());
    }

    @Test
    public void testCV_FirstSlotLegendaryGem() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.CARAVAN, new Random(42));
        assertEquals(Rarity.LEGENDARY, slots.get(0).rarity);
        assertEquals(ItemType.GEM, slots.get(0).itemType);
    }

    @Test
    public void testCV_SecondSlotLegendaryConsumable() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.CARAVAN, new Random(42));
        assertEquals(Rarity.LEGENDARY, slots.get(1).rarity);
        assertEquals(ItemType.CONSUMABLE, slots.get(1).itemType);
    }

    @Test
    public void testCV_OtherSlotsNotBelowRare() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.CARAVAN, new Random(42));
        for (int i = 2; i < slots.size(); i++) {
            assertTrue("栏位" + i + " ≥ RARE: " + slots.get(i).rarity,
                    slots.get(i).rarity.ordinal() >= Rarity.RARE.ordinal());
        }
    }

    @Test
    public void testCV_RollRarity_Distribution() {
        Random rng = new Random(42);
        int trials = 100000;
        int legend = 0, epic = 0, rare = 0, uncommon = 0;
        for (int i = 0; i < trials; i++) {
            Rarity r = MerchantConfig.rollCaravanRarity(rng);
            if (r == Rarity.LEGENDARY) legend++;
            else if (r == Rarity.EPIC) epic++;
            else if (r == Rarity.RARE) rare++;
            else uncommon++;
        }
        assertEquals("传说 10%，容差1.5%", 0.10, (double) legend / trials, 0.015);
        assertEquals("史诗 20%，容差1.5%", 0.20, (double) epic / trials, 0.015);
        assertEquals("罕见(RARE) 30%，容差1.5%", 0.30, (double) rare / trials, 0.015);
        assertEquals("稀有(UNCOMMON) 40%，容差1.5%", 0.40, (double) uncommon / trials, 0.015);
    }

    @Test
    public void testCV_RollRarity_NeverBelowUncommon() {
        Random rng = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Rarity r = MerchantConfig.rollCaravanRarity(rng);
            assertTrue("商队 ≥ UNCOMMON: " + r, r.ordinal() >= Rarity.UNCOMMON.ordinal());
        }
    }

    @Test
    public void testCV_CanBuy() {
        assertTrue(MerchantConfig.canBuy(MerchantConfig.Type.CARAVAN));
    }

    @Test
    public void testCV_CanSell() {
        assertTrue(MerchantConfig.canSell(MerchantConfig.Type.CARAVAN));
    }

    @Test
    public void testCV_SellRejectionNull() {
        assertNull(MerchantConfig.getSellRejectionMessage(MerchantConfig.Type.CARAVAN));
    }

    @Test
    public void testCV_BagFilter_AllTypes() {
        List<ItemType> types = MerchantConfig.getPlayerBagFilterTypes(
                MerchantConfig.Type.CARAVAN);
        assertEquals(4, types.size());
    }

    // ==================== 4. 材料商人 MATERIAL_MERCHANT ====================

    @Test
    public void testMM_GenerateSlots_Count() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.MATERIAL_MERCHANT, new Random(42));
        assertEquals("8 栏位", 8, slots.size());
    }

    @Test
    public void testMM_FirstSlotLegendaryGem() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.MATERIAL_MERCHANT, new Random(42));
        assertEquals(Rarity.LEGENDARY, slots.get(0).rarity);
        assertEquals(ItemType.GEM, slots.get(0).itemType);
    }

    @Test
    public void testMM_SecondSlotLegendaryConsumable() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.MATERIAL_MERCHANT, new Random(42));
        assertEquals(Rarity.LEGENDARY, slots.get(1).rarity);
        assertEquals(ItemType.CONSUMABLE, slots.get(1).itemType);
    }

    @Test
    public void testMM_OnlyGemOrConsumable() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.MATERIAL_MERCHANT, new Random(42));
        for (int i = 2; i < slots.size(); i++) {
            ItemType t = slots.get(i).itemType;
            assertTrue("仅宝石或药水，实际=" + t,
                    t == ItemType.GEM || t == ItemType.CONSUMABLE);
        }
    }

    @Test
    public void testMM_NoEquipment() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.MATERIAL_MERCHANT, new Random(42));
        for (MerchantConfig.MerchantSlot slot : slots) {
            assertTrue("不售装备", slot.itemType != ItemType.EQUIPMENT);
        }
    }

    @Test
    public void testMM_RollRarity_SameAsEquipment() {
        Random rng = new Random(42);
        for (int i = 0; i < 1000; i++) {
            rng = new Random(i);
            Rarity r1 = MerchantConfig.rollMaterialMerchantRarity(rng);
            rng = new Random(i);
            Rarity r2 = MerchantConfig.rollEquipmentMerchantRarity(rng);
            assertEquals("与装备商人同分布, seed=" + i, r1, r2);
        }
    }

    @Test
    public void testMM_CanBuy() {
        assertTrue(MerchantConfig.canBuy(MerchantConfig.Type.MATERIAL_MERCHANT));
    }

    @Test
    public void testMM_CanSell() {
        assertTrue(MerchantConfig.canSell(MerchantConfig.Type.MATERIAL_MERCHANT));
    }

    @Test
    public void testMM_SellRejectionNull() {
        assertNull(MerchantConfig.getSellRejectionMessage(MerchantConfig.Type.MATERIAL_MERCHANT));
    }

    @Test
    public void testMM_BagFilter_NoEquipment() {
        List<ItemType> types = MerchantConfig.getPlayerBagFilterTypes(
                MerchantConfig.Type.MATERIAL_MERCHANT);
        assertEquals(3, types.size());
        assertTrue(types.contains(ItemType.GEM));
        assertTrue(types.contains(ItemType.CONSUMABLE));
        assertTrue(types.contains(ItemType.MATERIAL));
        assertFalse(types.contains(ItemType.EQUIPMENT));
    }

    // ==================== 5. 跨类型通用测试 ====================

    @Test
    public void testEventKeys() {
        assertEquals("wandering_vendor",
                MerchantConfig.getEventKey(MerchantConfig.Type.WANDERING_VENDOR));
        assertEquals("equipment_merchant",
                MerchantConfig.getEventKey(MerchantConfig.Type.EQUIPMENT_MERCHANT));
        assertEquals("caravan",
                MerchantConfig.getEventKey(MerchantConfig.Type.CARAVAN));
        assertEquals("material_merchant",
                MerchantConfig.getEventKey(MerchantConfig.Type.MATERIAL_MERCHANT));
    }

    @Test
    public void testNames() {
        assertEquals("流浪商贩", MerchantConfig.getName(MerchantConfig.Type.WANDERING_VENDOR));
        assertEquals("装备商人", MerchantConfig.getName(MerchantConfig.Type.EQUIPMENT_MERCHANT));
        assertEquals("商队", MerchantConfig.getName(MerchantConfig.Type.CARAVAN));
        assertEquals("材料商人", MerchantConfig.getName(MerchantConfig.Type.MATERIAL_MERCHANT));
    }

    @Test
    public void testDescriptions() {
        assertTrue(MerchantConfig.getDesc(MerchantConfig.Type.WANDERING_VENDOR)
                .contains("不高于罕见"));
        assertTrue(MerchantConfig.getDesc(MerchantConfig.Type.EQUIPMENT_MERCHANT)
                .contains("装备"));
        assertTrue(MerchantConfig.getDesc(MerchantConfig.Type.CARAVAN)
                .contains("不低于稀有"));
        assertTrue(MerchantConfig.getDesc(MerchantConfig.Type.MATERIAL_MERCHANT)
                .contains("材料"));
    }

    @Test
    public void testAllCanBuy() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            assertTrue(type + " 可购买", MerchantConfig.canBuy(type));
        }
    }

    @Test
    public void testOnlyWV_CannotSell() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            if (type == MerchantConfig.Type.WANDERING_VENDOR) {
                assertFalse(type + " 不可出售", MerchantConfig.canSell(type));
            } else {
                assertTrue(type + " 可出售", MerchantConfig.canSell(type));
            }
        }
    }

    @Test
    public void testOnlyWV_HasRejectionMessage() {
        assertNotNull(MerchantConfig.getSellRejectionMessage(
                MerchantConfig.Type.WANDERING_VENDOR));
        assertNull(MerchantConfig.getSellRejectionMessage(
                MerchantConfig.Type.EQUIPMENT_MERCHANT));
        assertNull(MerchantConfig.getSellRejectionMessage(
                MerchantConfig.Type.CARAVAN));
        assertNull(MerchantConfig.getSellRejectionMessage(
                MerchantConfig.Type.MATERIAL_MERCHANT));
    }

    @Test
    public void testEquipLevelRange() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            assertTrue(type + " minLevel >= 5",
                    MerchantConfig.getEquipMinLevel(type) >= 5);
            assertTrue(type + " maxLevel >= minLevel",
                    MerchantConfig.getEquipMaxLevel(type) >= MerchantConfig.getEquipMinLevel(type));
        }
    }

    @Test
    public void testAllTypesGenerate8Slots() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(type, new Random(42));
            assertEquals(type + " 应生成 " + SLOTS + " 个栏位", SLOTS, slots.size());
        }
    }

    @Test
    public void testAllSlotsNonNull() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(type, new Random(42));
            for (MerchantConfig.MerchantSlot slot : slots) {
                assertNotNull(type + " 稀有度非 null", slot.rarity);
                assertNotNull(type + " 类型非 null", slot.itemType);
            }
        }
    }

    @Test
    public void testDeterministicWithSameSeed() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            List<MerchantConfig.MerchantSlot> s1 = MerchantConfig.generateSlots(type, new Random(12345));
            List<MerchantConfig.MerchantSlot> s2 = MerchantConfig.generateSlots(type, new Random(12345));
            for (int i = 0; i < s1.size(); i++) {
                assertEquals(type + " 栏位" + i + " 稀有度", s1.get(i).rarity, s2.get(i).rarity);
                assertEquals(type + " 栏位" + i + " 类型", s1.get(i).itemType, s2.get(i).itemType);
            }
        }
    }

    @Test
    public void testTypeEnum_AllFour() {
        assertEquals(4, MerchantConfig.Type.values().length);
    }

    @Test
    public void testWVRollRarity_NeverBelowCommon() {
        Random rng = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Rarity r = MerchantConfig.rollWanderingVendorRarity(rng);
            assertTrue(r.ordinal() >= Rarity.COMMON.ordinal());
        }
    }

    @Test
    public void testWVRollRarity_NeverAboveRare() {
        Random rng = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Rarity r = MerchantConfig.rollWanderingVendorRarity(rng);
            assertTrue(r.ordinal() <= Rarity.RARE.ordinal());
        }
    }
}
