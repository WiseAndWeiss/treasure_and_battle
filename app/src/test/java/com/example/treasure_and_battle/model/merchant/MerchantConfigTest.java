package com.example.treasure_and_battle.model.merchant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.ItemType;

import org.junit.Test;

import java.util.List;
import java.util.Random;

/**
 * 商人系统（MerchantConfig）核心逻辑测试
 * <p>
 * 测试覆盖四种商人类型：
 *  WANDERING_VENDOR（流浪商贩）、EQUIPMENT_MERCHANT（装备商人）、
 *  CARAVAN（商队）、MATERIAL_MERCHANT（材料商人）
 * <p>
 * 测试覆盖：
 *  1.  事件配置信息（eventKey/name/desc）
 *  2.  流浪商贩：零售品质随机（50%普通/25%稀有/25%罕见）
 *  3.  装备商人：商品栏位规则（首栏传说/末两栏普通/其余按概率）
 *  4.  商队：商品栏位规则（首栏传说宝石/次栏传说药水/其余品质≥稀有）
 *  5.  材料商人：商品栏位规则（首栏传说宝石/次栏传说药水/其余宝石药水）
 *  6.  背包过滤规则（各类型展示的物品类型）
 *  7.  交易限制（购买/出售权限）
 *  8.  商品生成结构验证（slot数量、类型、稀有度）
 *  9.  概率分布大样本验证
 * 10.  拒绝出售消息
 */
public class MerchantConfigTest {

    // ==================== 1. 事件配置信息 ====================

    @Test
    public void testWanderingVendor_EventKey() {
        assertEquals("wandering_vendor", MerchantConfig.getEventKey(MerchantConfig.Type.WANDERING_VENDOR));
    }

    @Test
    public void testEquipmentMerchant_EventKey() {
        assertEquals("equipment_merchant", MerchantConfig.getEventKey(MerchantConfig.Type.EQUIPMENT_MERCHANT));
    }

    @Test
    public void testCaravan_EventKey() {
        assertEquals("caravan", MerchantConfig.getEventKey(MerchantConfig.Type.CARAVAN));
    }

    @Test
    public void testMaterialMerchant_EventKey() {
        assertEquals("material_merchant", MerchantConfig.getEventKey(MerchantConfig.Type.MATERIAL_MERCHANT));
    }

    @Test
    public void testAllNamesChinese () {
        assertEquals("流浪商贩", MerchantConfig.getName(MerchantConfig.Type.WANDERING_VENDOR));
        assertEquals("装备商人", MerchantConfig.getName(MerchantConfig.Type.EQUIPMENT_MERCHANT));
        assertEquals("商队", MerchantConfig.getName(MerchantConfig.Type.CARAVAN));
        assertEquals("材料商人", MerchantConfig.getName(MerchantConfig.Type.MATERIAL_MERCHANT));
    }

    @Test
    public void testAllDescriptionsContainKeyInfo() {
        String desc = MerchantConfig.getDesc(MerchantConfig.Type.WANDERING_VENDOR);
        assertTrue("流浪商贩描述含'不高于罕见'", desc.contains("不高于罕见"));

        desc = MerchantConfig.getDesc(MerchantConfig.Type.EQUIPMENT_MERCHANT);
        assertTrue("装备商人描述含'装备'", desc.contains("装备"));

        desc = MerchantConfig.getDesc(MerchantConfig.Type.CARAVAN);
        assertTrue("商队描述含'商队'", desc.contains("商队"));

        desc = MerchantConfig.getDesc(MerchantConfig.Type.MATERIAL_MERCHANT);
        assertTrue("材料商人描述含'材料'", desc.contains("材料"));
    }

    // ==================== 2. 流浪商贩稀有度随机 ====================

    @Test
    public void testWanderingVendorRarity_Distribution() {
        Random rng = new Random(42);
        int trials = 50000;
        int common = 0, uncommon = 0, rare = 0;
        for (int i = 0; i < trials; i++) {
            Rarity r = MerchantConfig.rollWanderingVendorRarity(rng);
            if (r == Rarity.COMMON) common++;
            else if (r == Rarity.UNCOMMON) uncommon++;
            else if (r == Rarity.RARE) rare++;
        }
        assertEquals("普通 50%，容差2%", 0.50, (double) common / trials, 0.02);
        assertEquals("稀有 25%，容差2%", 0.25, (double) uncommon / trials, 0.02);
        assertEquals("罕见 25%，容差2%", 0.25, (double) rare / trials, 0.02);
        assertEquals("总次数", trials, common + uncommon + rare);
    }

    @Test
    public void testWanderingVendorRarity_NeverExceedsRare() {
        Random rng = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Rarity r = MerchantConfig.rollWanderingVendorRarity(rng);
            assertTrue("流浪商贩不出史诗/传说: " + r, r.ordinal() <= Rarity.RARE.ordinal());
        }
    }

    @Test
    public void testWanderingVendorRarity_AlwaysCommonOrAbove() {
        Random rng = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Rarity r = MerchantConfig.rollWanderingVendorRarity(rng);
            assertTrue("至少普通品质: " + r, r.ordinal() >= Rarity.COMMON.ordinal());
        }
    }

    // ==================== 3. 装备商人商品栏位规则 ====================

    @Test
    public void testEquipmentMerchant_FirstSlotIsLegendary() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.EQUIPMENT_MERCHANT, new Random(42));
        assertEquals("第1栏位为传说", Rarity.LEGENDARY, slots.get(0).rarity);
        assertEquals("第1栏位为装备", ItemType.EQUIPMENT, slots.get(0).itemType);
    }

    @Test
    public void testEquipmentMerchant_LastTwoSlotsAreCommon() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.EQUIPMENT_MERCHANT, new Random(42));
        int last = slots.size() - 1;
        assertEquals("倒数第1栏位为普通", Rarity.COMMON, slots.get(last).rarity);
        assertEquals("倒数第2栏位为普通", Rarity.COMMON, slots.get(last - 1).rarity);
    }

    @Test
    public void testEquipmentMerchant_AllSlotsAreEquipment() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.EQUIPMENT_MERCHANT, new Random(42));
        for (MerchantConfig.MerchantSlot slot : slots) {
            assertEquals("装备商人仅出售装备", ItemType.EQUIPMENT, slot.itemType);
        }
    }

    @Test
    public void testEquipmentMerchantRarity_Distribution() {
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

    // ==================== 4. 商队商品栏位规则 ====================

    @Test
    public void testCaravan_FirstSlotIsLegendaryGem() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.CARAVAN, new Random(42));
        assertEquals("第1栏位为传说", Rarity.LEGENDARY, slots.get(0).rarity);
        assertEquals("第1栏位为宝石", ItemType.GEM, slots.get(0).itemType);
    }

    @Test
    public void testCaravan_SecondSlotIsLegendaryConsumable() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.CARAVAN, new Random(42));
        assertEquals("第2栏位为传说", Rarity.LEGENDARY, slots.get(1).rarity);
        assertEquals("第2栏位为药水", ItemType.CONSUMABLE, slots.get(1).itemType);
    }

    @Test
    public void testCaravan_OtherSlotsNotBelowRare() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.CARAVAN, new Random(42));
        for (int i = 2; i < slots.size(); i++) {
            assertTrue("商队第" + i + "栏位品质不低于稀有: " + slots.get(i).rarity,
                    slots.get(i).rarity.ordinal() >= Rarity.RARE.ordinal());
        }
    }

    @Test
    public void testCaravanRarity_Distribution() {
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
        assertEquals("罕见 30%，容差1.5%", 0.30, (double) rare / trials, 0.015);
        assertEquals("稀有 40%，容差1.5%", 0.40, (double) uncommon / trials, 0.015);
    }

    @Test
    public void testCaravanRarity_NeverBelowRare() {
        Random rng = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Rarity r = MerchantConfig.rollCaravanRarity(rng);
            assertTrue("商队商品品质不低于稀有: " + r, r.ordinal() >= Rarity.RARE.ordinal());
        }
    }

    // ==================== 5. 材料商人商品栏位规则 ====================

    @Test
    public void testMaterialMerchant_FirstSlotIsLegendaryGem() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.MATERIAL_MERCHANT, new Random(42));
        assertEquals("第1栏位为传说", Rarity.LEGENDARY, slots.get(0).rarity);
        assertEquals("第1栏位为宝石", ItemType.GEM, slots.get(0).itemType);
    }

    @Test
    public void testMaterialMerchant_SecondSlotIsLegendaryConsumable() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.MATERIAL_MERCHANT, new Random(42));
        assertEquals("第2栏位为传说", Rarity.LEGENDARY, slots.get(1).rarity);
        assertEquals("第2栏位为药水", ItemType.CONSUMABLE, slots.get(1).itemType);
    }

    @Test
    public void testMaterialMerchant_OtherSlotsOnlyGemOrConsumable() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.MATERIAL_MERCHANT, new Random(42));
        for (int i = 2; i < slots.size(); i++) {
            ItemType t = slots.get(i).itemType;
            assertTrue("材料商人仅出售宝石或药水，实际=" + t,
                    t == ItemType.GEM || t == ItemType.CONSUMABLE);
        }
    }

    @Test
    public void testMaterialMerchant_NoEquipment() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.MATERIAL_MERCHANT, new Random(42));
        for (MerchantConfig.MerchantSlot slot : slots) {
            assertTrue("材料商人不售装备", slot.itemType != ItemType.EQUIPMENT);
        }
    }

    @Test
    public void testMaterialMerchantRarity_Distribution() {
        Random rng = new Random(42);
        int trials = 100000;
        int legend = 0, epic = 0, rare = 0, uncommon = 0, common = 0;
        for (int i = 0; i < trials; i++) {
            Rarity r = MerchantConfig.rollMaterialMerchantRarity(rng);
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

    // ==================== 6. 背包过滤规则 ====================

    @Test
    public void testPlayerBagFilter_WanderingVendor() {
        List<ItemType> types = MerchantConfig.getPlayerBagFilterTypes(MerchantConfig.Type.WANDERING_VENDOR);
        assertEquals("流浪商贩显示4类物品", 4, types.size());
        assertTrue("包含装备", types.contains(ItemType.EQUIPMENT));
        assertTrue("包含宝石", types.contains(ItemType.GEM));
        assertTrue("包含药水", types.contains(ItemType.CONSUMABLE));
        assertTrue("包含材料", types.contains(ItemType.MATERIAL));
    }

    @Test
    public void testPlayerBagFilter_EquipmentMerchant() {
        List<ItemType> types = MerchantConfig.getPlayerBagFilterTypes(MerchantConfig.Type.EQUIPMENT_MERCHANT);
        assertEquals("装备商人仅显示装备", 1, types.size());
        assertEquals(ItemType.EQUIPMENT, types.get(0));
    }

    @Test
    public void testPlayerBagFilter_Caravan() {
        List<ItemType> types = MerchantConfig.getPlayerBagFilterTypes(MerchantConfig.Type.CARAVAN);
        assertEquals("商队显示4类物品", 4, types.size());
    }

    @Test
    public void testPlayerBagFilter_MaterialMerchant() {
        List<ItemType> types = MerchantConfig.getPlayerBagFilterTypes(MerchantConfig.Type.MATERIAL_MERCHANT);
        assertEquals("材料商人仅显示材料", 1, types.size());
        assertEquals(ItemType.MATERIAL, types.get(0));
    }

    // ==================== 7. 交易限制 ====================

    @Test
    public void testCanBuy_AllTypes() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            assertTrue(type + " 应支持购买", MerchantConfig.canBuy(type));
        }
    }

    @Test
    public void testCanSell_WanderingVendor_False() {
        assertFalse("流浪商贩不支持出售", MerchantConfig.canSell(MerchantConfig.Type.WANDERING_VENDOR));
    }

    @Test
    public void testCanSell_OtherTypes_True() {
        assertTrue("装备商人支持出售", MerchantConfig.canSell(MerchantConfig.Type.EQUIPMENT_MERCHANT));
        assertTrue("商队支持出售", MerchantConfig.canSell(MerchantConfig.Type.CARAVAN));
        assertTrue("材料商人支持出售", MerchantConfig.canSell(MerchantConfig.Type.MATERIAL_MERCHANT));
    }

    @Test
    public void testSellRejectionMessage() {
        String msg = MerchantConfig.getSellRejectionMessage(MerchantConfig.Type.WANDERING_VENDOR);
        assertEquals("小贩表示不需要这些东西，你无法出售", msg);
    }

    @Test
    public void testSellRejectionMessage_OtherTypesNull() {
        assertTrue(MerchantConfig.getSellRejectionMessage(MerchantConfig.Type.EQUIPMENT_MERCHANT) == null);
        assertTrue(MerchantConfig.getSellRejectionMessage(MerchantConfig.Type.CARAVAN) == null);
        assertTrue(MerchantConfig.getSellRejectionMessage(MerchantConfig.Type.MATERIAL_MERCHANT) == null);
    }

    // ==================== 8. 商品生成结构验证 ====================

    @Test
    public void testGenerateSlots_AllTypesHave8Slots() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(type, new Random(42));
            assertEquals(type + " 应有8个栏位", 8, slots.size());
        }
    }

    @Test
    public void testGenerateSlots_AllSlotIndicesCorrect() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(type, new Random(42));
            for (int i = 0; i < slots.size(); i++) {
                assertEquals("栏位索引", i, slots.get(i).slotIndex);
            }
        }
    }

    @Test
    public void testGenerateSlots_AllSlotNonNull() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(type, new Random(42));
            for (int i = 0; i < slots.size(); i++) {
                assertNotNull("栏位 " + i + " 不应为null", slots.get(i));
                assertNotNull("栏位 " + i + " 稀有度不应为null", slots.get(i).rarity);
                assertNotNull("栏位 " + i + " 类型不应为null", slots.get(i).itemType);
            }
        }
    }

    @Test
    public void testGenerateSlots_DeterministicWithSameSeed() {
        Random rng1 = new Random(12345L);
        List<MerchantConfig.MerchantSlot> slots1 = MerchantConfig.generateSlots(
                MerchantConfig.Type.WANDERING_VENDOR, rng1);

        Random rng2 = new Random(12345L);
        List<MerchantConfig.MerchantSlot> slots2 = MerchantConfig.generateSlots(
                MerchantConfig.Type.WANDERING_VENDOR, rng2);

        for (int i = 0; i < slots1.size(); i++) {
            assertEquals("相同种子应产生相同商品", slots1.get(i).rarity, slots2.get(i).rarity);
            assertEquals("相同种子应产生相同商品", slots1.get(i).itemType, slots2.get(i).itemType);
        }
    }

    // ==================== 9. 商品类型随机 ====================

    @Test
    public void testWanderingVendorType_Distribution() {
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
            assertEquals("流浪商贩类型等概率 ≈33.33%，容差2%",
                    1.0 / 3.0, (double) counts[j] / trials, 0.02);
        }
    }

    // ==================== 10. 装备等级范围 ====================

    @Test
    public void testEquipLevelRange_AllTypes() {
        for (MerchantConfig.Type type : MerchantConfig.Type.values()) {
            assertTrue(type + " minLevel >= 5", MerchantConfig.getEquipMinLevel(type) >= 5);
            assertTrue(type + " maxLevel >= minLevel",
                    MerchantConfig.getEquipMaxLevel(type) >= MerchantConfig.getEquipMinLevel(type));
        }
    }

    // ==================== 11. Type枚举完整性 ====================

    @Test
    public void testTypeEnumHasFourValues() {
        assertEquals("四种商人类型", 4, MerchantConfig.Type.values().length);
        assertEquals(MerchantConfig.Type.WANDERING_VENDOR, MerchantConfig.Type.valueOf("WANDERING_VENDOR"));
        assertEquals(MerchantConfig.Type.EQUIPMENT_MERCHANT, MerchantConfig.Type.valueOf("EQUIPMENT_MERCHANT"));
        assertEquals(MerchantConfig.Type.CARAVAN, MerchantConfig.Type.valueOf("CARAVAN"));
        assertEquals(MerchantConfig.Type.MATERIAL_MERCHANT, MerchantConfig.Type.valueOf("MATERIAL_MERCHANT"));
    }

    // ==================== 12. 商品生成验证器 ====================

    @Test
    public void testWanderingVendorAllItemsWithinLimit() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.WANDERING_VENDOR, new Random(42));
        for (MerchantConfig.MerchantSlot slot : slots) {
            assertTrue("流浪商贩商品不超过罕见: " + slot.rarity,
                    slot.rarity.ordinal() <= Rarity.RARE.ordinal());
        }
    }

    @Test
    public void testCaravanAllItemsAtLeastRare() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.CARAVAN, new Random(42));
        for (MerchantConfig.MerchantSlot slot : slots) {
            assertTrue("商队商品不低于稀有: " + slot.rarity,
                    slot.rarity.ordinal() >= Rarity.RARE.ordinal());
        }
    }

    @Test
    public void testEquipmentMerchantAllItemsAreEquipment() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.EQUIPMENT_MERCHANT, new Random(42));
        for (MerchantConfig.MerchantSlot slot : slots) {
            assertEquals("装备商人仅售装备", ItemType.EQUIPMENT, slot.itemType);
        }
    }

    @Test
    public void testMaterialMerchantNoEquipmentSlot() {
        List<MerchantConfig.MerchantSlot> slots = MerchantConfig.generateSlots(
                MerchantConfig.Type.MATERIAL_MERCHANT, new Random(42));
        for (MerchantConfig.MerchantSlot slot : slots) {
            assertTrue("材料商人不售装备", slot.itemType != ItemType.EQUIPMENT);
        }
    }
}
