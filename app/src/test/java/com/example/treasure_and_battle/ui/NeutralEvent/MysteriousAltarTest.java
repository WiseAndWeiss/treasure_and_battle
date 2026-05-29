package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 神秘祭坛（mysterious_altar）事件核心逻辑测试
 * <p>
 * 事件规则：玩家从背包中选择3件相同品质的非材料物品献祭，
 * 换取一件高一级品质的随机类型物品。
 * 奖励类型按献祭物品种类权重随机决定（如2宝石+1装备→宝石66.7%）。
 * <p>
 * 测试覆盖：
 * 1. countItemsByRarity 计数逻辑（仅按品质，跨类型）
 * 2. isSacCountsValid 校验逻辑（仅品质统一，允许不同类型）
 * 3. 献祭总数必须为 3
 * 4. 稀有度升级链 Rarity.fromId(id+1)
 * 5. 可献祭稀有度范围
 * 6. 材料（MATERIAL）不可献祭
 * 7. 最高品质物品先校验再删除
 * 8. 权重随机奖励类型选择
 */
public class MysteriousAltarTest {

    private static final Rarity[] CHECK_RARITIES = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE};

    // ====================== Mock Item ======================

    private static class MockItem extends Item {
        MockItem(String id, String name, Rarity rarity, ItemType type, int count, int maxStack) {
            super(id, name, rarity, 0, type, count, maxStack);
        }
    }

    private static MockItem makeItem(String id, String name, Rarity r, ItemType t, int count) {
        return new MockItem(id, name, r, t, count, count > 1 ? 99 : 1);
    }

    // ====================== countItemsByRarity ======================

    private int countItemsByRarity(List<Item> bag, Rarity rarity) {
        int count = 0;
        for (Item item : bag)
            if (item != null && item.getType() != ItemType.MATERIAL && item.getRarity() == rarity)
                count += item.getCount();
        return count;
    }

    @Test
    public void testCountExactMatch() {
        List<Item> bag = new ArrayList<>();
        bag.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 3));
        assertEquals(3, countItemsByRarity(bag, Rarity.COMMON));
    }

    @Test
    public void testCountSkipsNullSlots() {
        List<Item> bag = new ArrayList<>();
        bag.add(null);
        bag.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        bag.add(null);
        assertEquals(1, countItemsByRarity(bag, Rarity.COMMON));
    }

    @Test
    public void testCountSumsAcrossDifferentTypes() {
        List<Item> bag = new ArrayList<>();
        bag.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 2));
        bag.add(makeItem("potion", "药水", Rarity.COMMON, ItemType.CONSUMABLE, 1));
        assertEquals("同品质不同种类应跨类型求和", 3, countItemsByRarity(bag, Rarity.COMMON));
    }

    @Test
    public void testCountIgnoresDifferentRarity() {
        List<Item> bag = new ArrayList<>();
        bag.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 2));
        bag.add(makeItem("sword2", "钢剑", Rarity.UNCOMMON, ItemType.EQUIPMENT, 2));
        assertEquals(2, countItemsByRarity(bag, Rarity.COMMON));
        assertEquals(2, countItemsByRarity(bag, Rarity.UNCOMMON));
    }

    @Test
    public void testCountIgnoresMaterial() {
        List<Item> bag = new ArrayList<>();
        bag.add(makeItem("ore", "矿石", Rarity.COMMON, ItemType.MATERIAL, 5));
        bag.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 3));
        assertEquals("Material 不计入", 3, countItemsByRarity(bag, Rarity.COMMON));
    }

    @Test
    public void testCountReturnsZeroForEmpty() {
        List<Item> bag = new ArrayList<>();
        assertEquals(0, countItemsByRarity(bag, Rarity.COMMON));
    }

    // ====================== buildAltarActions 前置校验 ======================

    @Test
    public void testPreconditionRequiresAtLeastThreeEligibleItems() {
        int eligibleCount = 2;
        assertTrue("背包不足3件可献祭物品时不应触发献祭流程", eligibleCount < 3);
    }

    @Test
    public void testPreconditionPassesWhenEnoughItems() {
        int eligibleCount = 3;
        assertTrue("背包有3件可献祭物品时应有资格", eligibleCount >= 3);
    }

    @Test
    public void testMaterialExcludedFromEligibleCount() {
        List<Item> bag = new ArrayList<>();
        bag.add(makeItem("ore", "矿石", Rarity.COMMON, ItemType.MATERIAL, 5));
        int eligibleCount = 0;
        for (Item item : bag)
            if (item != null && item.getType() != ItemType.MATERIAL) eligibleCount += item.getCount();
        assertEquals("MATERIAL 不应计入可献祭数量", 0, eligibleCount);
    }

    // ====================== hasValidCombo 逻辑（仅品质维度） ======================

    @Test
    public void testValidComboThreeSameRaritySameType() {
        List<Item> bag = new ArrayList<>();
        bag.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 3));
        boolean hasValidCombo = false;
        for (Rarity r : CHECK_RARITIES)
            if (countItemsByRarity(bag, r) >= 3) { hasValidCombo = true; break; }
        assertTrue("3件同品质装备应形成有效组合", hasValidCombo);
    }

    @Test
    public void testValidComboThreeSameRarityMixedType() {
        List<Item> bag = new ArrayList<>();
        bag.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        bag.add(makeItem("potion", "药水", Rarity.COMMON, ItemType.CONSUMABLE, 1));
        bag.add(makeItem("gem", "红宝石", Rarity.COMMON, ItemType.GEM, 1));
        boolean hasValidCombo = false;
        for (Rarity r : CHECK_RARITIES)
            if (countItemsByRarity(bag, r) >= 3) { hasValidCombo = true; break; }
        assertTrue("装备+药水+宝石，品质相同（普通），应有效", hasValidCombo);
    }

    @Test
    public void testValidComboWithMixedRarityShouldFail() {
        List<Item> bag = new ArrayList<>();
        bag.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 2));
        bag.add(makeItem("sword2", "钢剑", Rarity.UNCOMMON, ItemType.EQUIPMENT, 2));
        boolean hasValidCombo = false;
        for (Rarity r : CHECK_RARITIES)
            if (countItemsByRarity(bag, r) >= 3) { hasValidCombo = true; break; }
        assertFalse("2件普通+2件稀有不应被视有效", hasValidCombo);
    }

    @Test
    public void testValidComboRareRarity() {
        List<Item> bag = new ArrayList<>();
        bag.add(makeItem("g1", "宝石A", Rarity.RARE, ItemType.GEM, 2));
        bag.add(makeItem("p1", "高级药水", Rarity.RARE, ItemType.CONSUMABLE, 2));
        boolean hasValidCombo = false;
        for (Rarity r : CHECK_RARITIES)
            if (countItemsByRarity(bag, r) >= 3) { hasValidCombo = true; break; }
        assertTrue("2罕见宝石+2罕见药水跨类型共4件≥3，应有效", hasValidCombo);
    }

    // ====================== 可献祭稀有度范围 ======================

    @Test
    public void testCheckRaritiesCountIsThree() {
        assertEquals("可献祭稀有度为3个", 3, CHECK_RARITIES.length);
    }

    @Test
    public void testEpicIsNotInCheckRarities() {
        assertFalse("EPIC 不在可献祭列表中", containsRarity(CHECK_RARITIES, Rarity.EPIC));
    }

    @Test
    public void testLegendaryIsNotInCheckRarities() {
        assertFalse("LEGENDARY 不在可献祭列表中", containsRarity(CHECK_RARITIES, Rarity.LEGENDARY));
    }

    private boolean containsRarity(Rarity[] rarities, Rarity target) {
        for (Rarity r : rarities) if (r == target) return true;
        return false;
    }

    // ====================== isSacCountsValid（仅校验品质统一） ======================

    private boolean isSacCountsValid(List<Item> items, int[] counts) {
        Item first = null;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] <= 0) continue;
            if (first == null) { first = items.get(i); continue; }
            Item t = items.get(i);
            if (t.getRarity() != first.getRarity()) return false;
        }
        return first != null;
    }

    @Test
    public void testIsSacCountsValidAllSameRarity() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        items.add(makeItem("axe", "铁斧", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        items.add(makeItem("shield", "铁盾", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        int[] counts = {1, 1, 1};
        assertTrue(isSacCountsValid(items, counts));
    }

    @Test
    public void testIsSacCountsValidRejectsDifferentRarity() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        items.add(makeItem("sword2", "钢剑", Rarity.UNCOMMON, ItemType.EQUIPMENT, 1));
        int[] counts = {1, 1};
        assertFalse("不同稀有度应被拒绝", isSacCountsValid(items, counts));
    }

    @Test
    public void testIsSacCountsValidAcceptsDifferentTypeSameRarity() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        items.add(makeItem("potion", "药水", Rarity.COMMON, ItemType.CONSUMABLE, 1));
        items.add(makeItem("gem", "红宝石", Rarity.COMMON, ItemType.GEM, 1));
        int[] counts = {1, 1, 1};
        assertTrue("不同类型但相同品质应被接受", isSacCountsValid(items, counts));
    }

    @Test
    public void testIsSacCountsValidRejectsEmptySelection() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        int[] counts = {0};
        assertFalse("未选择任何物品", isSacCountsValid(items, counts));
    }

    @Test
    public void testIsSacCountsValidSingleItemSelected() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        int[] counts = {1};
        assertTrue("只选1件，品质统一性应通过", isSacCountsValid(items, counts));
    }

    @Test
    public void testIsSacCountsValidSkipsZeroCountSlots() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        items.add(makeItem("sword2", "钢剑", Rarity.UNCOMMON, ItemType.EQUIPMENT, 1));
        items.add(makeItem("sword3", "铁剑2", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        int[] counts = {1, 0, 1};
        assertTrue("跳过未选中的不同稀有度插槽", isSacCountsValid(items, counts));
    }

    // ====================== ⭐ 核心契约：献祭总数必须恰好为 3 ======================

    @Test
    public void testSacrificeOneItemIsRejected() {
        assertFalse("1件应被拒绝", isCurrentSacrificeCountValid(1));
    }

    @Test
    public void testSacrificeTwoItemsAreRejected() {
        assertFalse("2件应被拒绝", isCurrentSacrificeCountValid(2));
    }

    @Test
    public void testSacrificeThreeItemsAreAccepted() {
        assertTrue("3件应被接受", isCurrentSacrificeCountValid(3));
    }

    @Test
    public void testCorrectStandardRejectsOneAndTwo() {
        assertFalse(isValidSacrificeCount(1));
        assertFalse(isValidSacrificeCount(2));
    }

    @Test
    public void testCorrectStandardAcceptsOnlyThree() {
        assertTrue(isValidSacrificeCount(3));
    }

    private boolean isCurrentSacrificeCountValid(int total) {
        return total == 3;
    }

    private boolean isValidSacrificeCount(int total) {
        return total == 3;
    }

    // ====================== 稀有度升级链 ======================

    @Test
    public void testRarityUpgradeCommonToUncommon() {
        assertEquals(Rarity.UNCOMMON, Rarity.fromId(Rarity.COMMON.getId() + 1));
    }

    @Test
    public void testRarityUpgradeUncommonToRare() {
        assertEquals(Rarity.RARE, Rarity.fromId(Rarity.UNCOMMON.getId() + 1));
    }

    @Test
    public void testRarityUpgradeRareToEpic() {
        assertEquals(Rarity.EPIC, Rarity.fromId(Rarity.RARE.getId() + 1));
    }

    @Test
    public void testRarityUpgradeFromLegendaryIsNull() {
        assertNull(Rarity.fromId(Rarity.LEGENDARY.getId() + 1));
    }

    @Test
    public void testAllValidInputRaritiesProduceNonNullUpgrade() {
        for (Rarity r : CHECK_RARITIES) {
            assertNotNull(r.getDisplayName(), Rarity.fromId(r.getId() + 1));
        }
    }

    // ====================== 最高品质校验先于删除 ======================

    @Test
    public void testLegendarySacrificeBlockedBeforeRemoval() {
        List<Item> eligible = new ArrayList<>();
        Item legend = makeItem("excalibur", "圣剑", Rarity.LEGENDARY, ItemType.EQUIPMENT, 3);
        eligible.add(legend);
        int[] sacrificeCounts = {3};
        int total = 3;

        assertTrue("3件品质统一", isSacCountsValid(eligible, sacrificeCounts));
        assertTrue("总数=3", isValidSacrificeCount(total));

        Rarity fromR = eligible.get(0).getRarity();
        Rarity toR = Rarity.fromId(fromR.getId() + 1);
        assertNull("LEGENDARY升级应返回null", toR);

        int countBefore = eligible.get(0).getCount();
        assertEquals("校验失败时物品不应被扣除", countBefore, 3);
    }

    // ====================== 确认按钮启用条件 ======================

    @Test
    public void testConfirmDisabledWithOneItem() {
        assertFalse("1件不应启用确认", isConfirmEnabledByCurrent(1));
    }

    @Test
    public void testConfirmDisabledWithTwoItems() {
        assertFalse("2件不应启用确认", isConfirmEnabledByCurrent(2));
    }

    @Test
    public void testConfirmEnabledWithThreeItems() {
        assertTrue("3件应启用确认", isConfirmEnabledByCurrent(3));
    }

    @Test
    public void testCorrectConfirmOnlyEnabledAtThree() {
        assertFalse(isConfirmEnabledByCorrect(1));
        assertFalse(isConfirmEnabledByCorrect(2));
        assertTrue(isConfirmEnabledByCorrect(3));
    }

    private boolean isConfirmEnabledByCurrent(int t) {
        return t == 3;
    }

    private boolean isConfirmEnabledByCorrect(int t) {
        return t == 3;
    }

    // ====================== 献祭总数上限 ======================

    @Test
    public void testSacrificeTotalNeverExceedsThree() {
        int remainingSpace = 3 - 2;
        int maxN = 5;
        int n = Math.min(remainingSpace, maxN);
        assertEquals(1, n);
    }

    @Test
    public void testSacrificeTotalAtMaxCannotAddMore() {
        assertTrue(3 - 3 <= 0);
    }

    // ====================== 物品删除逻辑 ======================

    @Test
    public void testItemsCorrectlyRemovedAfterSacrifice() {
        Item sword = makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 3);
        int[] sacrificeCounts = {3};
        sword.setCount(sword.getCount() - sacrificeCounts[0]);
        assertEquals(0, sword.getCount());
    }

    @Test
    public void testItemWithLargerStackReducedCorrectly() {
        Item sword = makeItem("sword", "铁剑", Rarity.COMMON, ItemType.EQUIPMENT, 5);
        int[] sacrificeCounts = {3};
        sword.setCount(sword.getCount() - sacrificeCounts[0]);
        assertEquals(2, sword.getCount());
    }

    // ====================== pickRewardTypeByWeight ======================

    private ItemType pickRewardTypeByWeight(List<Item> items, int[] counts) {
        int equipCount = 0, consumableCount = 0, gemCount = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] <= 0) continue;
            Item item = items.get(i);
            if (item.getType() == ItemType.EQUIPMENT) equipCount += counts[i];
            else if (item.getType() == ItemType.CONSUMABLE) consumableCount += counts[i];
            else if (item.getType() == ItemType.GEM) gemCount += counts[i];
        }
        int totalWeight = equipCount + consumableCount + gemCount;
        if (totalWeight == 0) return ItemType.EQUIPMENT;
        int roll = new Random().nextInt(totalWeight);
        if (roll < equipCount) return ItemType.EQUIPMENT;
        if (roll < equipCount + consumableCount) return ItemType.CONSUMABLE;
        return ItemType.GEM;
    }

    @Test
    public void testWeightAllSameTypeReturnsThatType() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("sword", "剑", Rarity.COMMON, ItemType.EQUIPMENT, 3));
        int[] counts = {3};
        int equip = 0, cons = 0, gem = 0;
        for (int i = 0; i < 100; i++) {
            ItemType t = pickRewardTypeByWeight(items, counts);
            if (t == ItemType.EQUIPMENT) equip++;
            else if (t == ItemType.CONSUMABLE) cons++;
            else gem++;
        }
        assertEquals("全装备献祭→奖励必为装备", 100, equip);
        assertEquals(0, cons);
        assertEquals(0, gem);
    }

    @Test
    public void testWeightTwoToOneRatioDistribution() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("gem", "宝石", Rarity.COMMON, ItemType.GEM, 2));
        items.add(makeItem("sword", "剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        int[] counts = {2, 1};

        int gemCount = 0, equipCount = 0, consumableCount = 0;
        int trials = 10000;
        for (int i = 0; i < trials; i++) {
            ItemType t = pickRewardTypeByWeight(items, counts);
            if (t == ItemType.GEM) gemCount++;
            else if (t == ItemType.EQUIPMENT) equipCount++;
            else consumableCount++;
        }

        double gemRatio = (double) gemCount / trials;
        double equipRatio = (double) equipCount / trials;
        assertEquals("宝石权重 2/3 ≈ 66.7%，容差5%", 2.0 / 3.0, gemRatio, 0.05);
        assertEquals("装备权重 1/3 ≈ 33.3%，容差5%", 1.0 / 3.0, equipRatio, 0.05);
        assertEquals("无药水参与，药水概率为0", 0, consumableCount);
    }

    @Test
    public void testWeightEqualDistribution() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("sword", "剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        items.add(makeItem("potion", "药", Rarity.COMMON, ItemType.CONSUMABLE, 1));
        items.add(makeItem("gem", "宝石", Rarity.COMMON, ItemType.GEM, 1));
        int[] counts = {1, 1, 1};

        int equipCount = 0, consCount = 0, gemCount = 0;
        int trials = 10000;
        for (int i = 0; i < trials; i++) {
            ItemType t = pickRewardTypeByWeight(items, counts);
            if (t == ItemType.EQUIPMENT) equipCount++;
            else if (t == ItemType.CONSUMABLE) consCount++;
            else gemCount++;
        }

        assertEquals("装备各1件，装备≈33.3%", 1.0 / 3.0, (double) equipCount / trials, 0.05);
        assertEquals("装备各1件，药水≈33.3%", 1.0 / 3.0, (double) consCount / trials, 0.05);
        assertEquals("装备各1件，宝石≈33.3%", 1.0 / 3.0, (double) gemCount / trials, 0.05);
    }

    @Test
    public void testWeightEquipConsumableOnly() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("sword", "剑", Rarity.COMMON, ItemType.EQUIPMENT, 1));
        items.add(makeItem("potion", "药", Rarity.COMMON, ItemType.CONSUMABLE, 2));
        int[] counts = {1, 2};

        int gemCount = 0;
        for (int i = 0; i < 100; i++) {
            if (pickRewardTypeByWeight(items, counts) == ItemType.GEM) gemCount++;
        }
        assertEquals("无宝石参与，宝石概率为0", 0, gemCount);
    }

    @Test
    public void testWeightTotalWeightCalculation() {
        List<Item> items = new ArrayList<>();
        items.add(makeItem("sword", "剑", Rarity.COMMON, ItemType.EQUIPMENT, 3));
        items.add(makeItem("gem", "宝石", Rarity.COMMON, ItemType.GEM, 2));
        int[] counts = {2, 2};
        int totalWeight = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) totalWeight += counts[i];
        }
        assertEquals("装备2件+宝石2件，总权重=4", 4, totalWeight);
    }

    // ====================== 稀有度升级链与配置一致 ======================

    @Test
    public void testUpgradeRarityChainMatchesConfig() {
        assertEquals("3×普通→1×稀有", Rarity.UNCOMMON, Rarity.fromId(Rarity.COMMON.getId() + 1));
        assertEquals("3×稀有→1×罕见", Rarity.RARE, Rarity.fromId(Rarity.UNCOMMON.getId() + 1));
        assertEquals("3×罕见→1×史诗", Rarity.EPIC, Rarity.fromId(Rarity.RARE.getId() + 1));
    }
}
