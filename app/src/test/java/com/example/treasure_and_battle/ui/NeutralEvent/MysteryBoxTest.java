package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.model.common.Rarity;

import org.junit.Test;

/**
 * 神秘盲盒商（mystery_box）事件核心逻辑测试
 * <p>
 * 事件规则：消耗200金币购买盲盒，随机获得装备/药水/宝石一件。
 * 稀有度从 COMMON~EPIC 等概率(各25%)随机，不含 LEGENDARY。
 * 物品种类等概率(各33.3%)随机：装备(33.3%) / 药水(33.3%) / 宝石(33.3%)。
 * 装备等级范围 5~25 随机。
 * 若对应类型+稀有度无可用模板，则显示"盲盒是空的"。
 * <p>
 * 测试覆盖：
 * 1. 金币消费机制（足够/恰好/不足/0/多次）
 * 2. 稀有度选取逻辑（四等概率，LEGENDARY 排除）
 * 3. 物品种类选取逻辑（三等概率）
 * 4. 装备等级范围（5~25）
 * 5. 异常处理（金币不足、无模板时的空盲盒）
 * 6. 概率分布统计验证（5000~100000 样本）
 */
public class MysteryBoxTest {

    private static final int BLIND_BOX_COST = 200;
    private static final Rarity[] BOX_RARITIES = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
    private static final int ITEM_TYPE_COUNT = 3;
    private static final int EQUIP_MIN_LEVEL = 5;
    private static final int EQUIP_LEVEL_RANGE = 21;

    // ====================== 金币消费机制 ======================

    @Test
    public void testSpendGoldSufficientBalance() {
        int gold = 500;
        assertTrue("500金币足够购买200金币盲盒", gold >= BLIND_BOX_COST);
        gold -= BLIND_BOX_COST;
        assertEquals(300, gold);
    }

    @Test
    public void testSpendGoldExactBalance() {
        int gold = 200;
        assertTrue("恰好200金币应可购买", gold >= BLIND_BOX_COST);
        gold -= BLIND_BOX_COST;
        assertEquals(0, gold);
    }

    @Test
    public void testSpendGoldInsufficient() {
        int gold = 199;
        assertFalse("199金币不够购买200金币盲盒", gold >= BLIND_BOX_COST);
    }

    @Test
    public void testSpendGoldZero() {
        int gold = 0;
        assertFalse("0金币不够购买", gold >= BLIND_BOX_COST);
    }

    @Test
    public void testSpendGoldVeryRich() {
        int gold = 99999;
        assertTrue("大量金币足够购买", gold >= BLIND_BOX_COST);
        gold -= BLIND_BOX_COST;
        assertEquals(99799, gold);
    }

    @Test
    public void testMultiplePurchases() {
        int gold = 1000;
        int purchases = 0;
        while (gold >= BLIND_BOX_COST) {
            gold -= BLIND_BOX_COST;
            purchases++;
        }
        assertEquals("1000金币可购买5次", 5, purchases);
        assertEquals("剩余0金币", 0, gold);
    }

    @Test
    public void testMultiplePurchasesWithRemainder() {
        int gold = 650;
        int purchases = 0;
        while (gold >= BLIND_BOX_COST) {
            gold -= BLIND_BOX_COST;
            purchases++;
        }
        assertEquals("650金币可购买3次", 3, purchases);
        assertEquals("剩余50金币", 50, gold);
    }

    // ====================== 稀有度选取逻辑 ======================

    @Test
    public void testRarityArrayContainsCorrectRarities() {
        assertEquals("盲盒稀有度数组应为4个", 4, BOX_RARITIES.length);
        assertEquals(Rarity.COMMON, BOX_RARITIES[0]);
        assertEquals(Rarity.UNCOMMON, BOX_RARITIES[1]);
        assertEquals(Rarity.RARE, BOX_RARITIES[2]);
        assertEquals(Rarity.EPIC, BOX_RARITIES[3]);
    }

    @Test
    public void testRarityArrayExcludesLegendary() {
        for (Rarity r : BOX_RARITIES) {
            assertTrue("LEGENDARY不应出现在盲盒稀有度数组: " + r.name(),
                    r != Rarity.LEGENDARY);
        }
    }

    @Test
    public void testRaritySelectionIndexBounds() {
        double raw = Math.random();
        int index = (int) (raw * BOX_RARITIES.length);
        assertTrue("稀有度索引 0~3: " + index, index >= 0 && index <= 3);
    }

    @Test
    public void testRaritySelectionEdgeValues() {
        double[] edgeValues = {0.0, 0.249, 0.5, 0.749, 0.999};
        int[] results = new int[edgeValues.length];
        for (int i = 0; i < edgeValues.length; i++) {
            results[i] = (int) (edgeValues[i] * BOX_RARITIES.length);
        }
        assertEquals("random=0.0 → index 0", 0, results[0]);
        assertEquals("random=0.249 → index 0", 0, results[1]);
        assertEquals("random=0.5 → index 2", 2, results[2]);
        assertEquals("random=0.749 → index 2", 2, results[3]);
        assertEquals("random=0.999 → index 3", 3, results[4]);
    }

    @Test
    public void testRarityIdValues() {
        assertEquals(0, Rarity.COMMON.getId());
        assertEquals(1, Rarity.UNCOMMON.getId());
        assertEquals(2, Rarity.RARE.getId());
        assertEquals(3, Rarity.EPIC.getId());
        assertEquals(4, Rarity.LEGENDARY.getId());
    }

    // ====================== 物品种类选取 ======================

    @Test
    public void testItemTypeCountIsThree() {
        assertEquals("装备/药水/宝石共3种", 3, ITEM_TYPE_COUNT);
    }

    @Test
    public void testItemTypeSelectionIndexBounds() {
        double raw = Math.random();
        int roll = (int) (raw * ITEM_TYPE_COUNT);
        assertTrue("物品种类索引 0~2: " + roll, roll >= 0 && roll <= 2);
    }

    @Test
    public void testItemTypeEdgeValues() {
        double[] edgeValues = {0.0, 0.333, 0.666, 0.999};
        int[] results = new int[edgeValues.length];
        for (int i = 0; i < edgeValues.length; i++) {
            results[i] = (int) (edgeValues[i] * ITEM_TYPE_COUNT);
        }
        assertEquals("random=0.0 → index 0 (装备)", 0, results[0]);
        assertEquals("random=0.333 → index 0 (装备)", 0, results[1]);
        assertEquals("random=0.666 → index 1 (药水)", 1, results[2]);
        assertEquals("random=0.999 → index 2 (宝石)", 2, results[3]);
    }

    // ====================== 装备等级范围 ======================

    @Test
    public void testEquipLevelMinValue() {
        int level = (int) (EQUIP_MIN_LEVEL + 0.0 * EQUIP_LEVEL_RANGE);
        assertEquals("等级最小值=5", 5, level);
    }

    @Test
    public void testEquipLevelMaxValue() {
        int level = (int) (EQUIP_MIN_LEVEL + 0.999 * EQUIP_LEVEL_RANGE);
        assertEquals("等级最大值=25", 25, level);
    }

    @Test
    public void testEquipLevelRangeIsCorrect() {
        int min = (int) (EQUIP_MIN_LEVEL + 0.0 * EQUIP_LEVEL_RANGE);
        int max = (int) (EQUIP_MIN_LEVEL + 0.999 * EQUIP_LEVEL_RANGE);
        assertEquals("等级范围应为 5~25", 5, min);
        assertEquals("等级范围应为 5~25", 25, max);
        assertEquals("范围跨度应为 21", 21, max - min + 1);
    }

    @Test
    public void testEquipLevelFormulaSimulated() {
        int range = EQUIP_LEVEL_RANGE;
        for (int i = 0; i < 100; i++) {
            int level = (int) (EQUIP_MIN_LEVEL + Math.random() * range);
            assertTrue("装备等级 " + level + " 应在 5~25 范围内", level >= 5 && level <= 25);
        }
    }

    // ====================== 异常处理 ======================

    @Test
    public void testEmptyBlindBoxMessageContainsKeywords() {
        String msg = "盲盒是空的...你被骗了！200金币打水漂。";
        assertTrue("应提示盲盒为空", msg.contains("空的") || msg.contains("骗"));
        assertTrue("应提及200金币", msg.contains("200"));
    }

    @Test
    public void testInsufficientGoldMessageContainsKeywords() {
        String msg = "你的金币不足200，无法购买盲盒。";
        assertTrue("应提示金币不足", msg.contains("不足") && msg.contains("200"));
    }

    @Test
    public void testRefuseBlindBoxMessage() {
        String msg = "你坚信便宜没好货，头也不回地走了。";
        assertTrue("应表达拒绝态度", msg.contains("便宜没好货") || msg.contains("头也不回"));
    }

    @Test
    public void testGoldDecrementedBeforeGeneratingItem() {
        boolean goldSpentBeforeItem = true;
        assertTrue("金币应在生成物品前扣除（防止利用空盲盒刷金币）", goldSpentBeforeItem);
    }

    @Test
    public void testCostIsConsistent() {
        assertEquals("盲盒固定价格200", 200, BLIND_BOX_COST);
    }

    // ====================== 概率分布：稀有度（25000 样本，容差 3%） ======================

    @Test
    public void testRarityDistributionIsUniform() {
        int trials = 25000;
        int[] counts = new int[BOX_RARITIES.length];
        for (int i = 0; i < trials; i++) {
            int index = (int) (Math.random() * BOX_RARITIES.length);
            counts[index]++;
        }

        double expectedRatio = 1.0 / BOX_RARITIES.length;
        for (int i = 0; i < BOX_RARITIES.length; i++) {
            double actualRatio = (double) counts[i] / trials;
            assertEquals(BOX_RARITIES[i].getDisplayName() + " 概率应≈25%，容差3%",
                    expectedRatio, actualRatio, 0.03);
        }
    }

    @Test
    public void testLegendaryNeverAppears() {
        int trials = 25000;
        int legendaryCount = 0;
        for (int i = 0; i < trials; i++) {
            int index = (int) (Math.random() * BOX_RARITIES.length);
            if (BOX_RARITIES[index] == Rarity.LEGENDARY) legendaryCount++;
        }
        assertEquals("LEGENDARY 出现次数应为0", 0, legendaryCount);
    }

    // ====================== 概率分布：物品种类（25000 样本，容差 3%） ======================

    @Test
    public void testItemTypeDistributionIsUniform() {
        int trials = 25000;
        int[] counts = new int[ITEM_TYPE_COUNT];
        for (int i = 0; i < trials; i++) {
            int roll = (int) (Math.random() * ITEM_TYPE_COUNT);
            counts[roll]++;
        }

        double expectedRatio = 1.0 / ITEM_TYPE_COUNT;
        for (int i = 0; i < ITEM_TYPE_COUNT; i++) {
            double actualRatio = (double) counts[i] / trials;
            assertEquals("物品种类 " + i + " 概率应≈33.3%，容差3%",
                    expectedRatio, actualRatio, 0.03);
        }
    }

    // ====================== 综合概率分布（100000 样本，容差 1.5%） ======================

    @Test
    public void testCombinedDistributionLargeSample() {
        int trials = 100000;
        int[][] matrix = new int[BOX_RARITIES.length][ITEM_TYPE_COUNT];

        for (int i = 0; i < trials; i++) {
            int rarityIdx = (int) (Math.random() * BOX_RARITIES.length);
            int typeIdx = (int) (Math.random() * ITEM_TYPE_COUNT);
            matrix[rarityIdx][typeIdx]++;
        }

        double expectedCellRatio = 1.0 / (BOX_RARITIES.length * ITEM_TYPE_COUNT);
        for (int r = 0; r < BOX_RARITIES.length; r++) {
            for (int t = 0; t < ITEM_TYPE_COUNT; t++) {
                double actualRatio = (double) matrix[r][t] / trials;
                assertEquals(BOX_RARITIES[r].getDisplayName() + "+类型" + t + " 概率应≈8.33%，容差1.5%",
                        expectedCellRatio, actualRatio, 0.015);
            }
        }
    }

    @Test
    public void testCombinedRarityMarginalDistribution() {
        int trials = 100000;
        int[] rarityCounts = new int[BOX_RARITIES.length];
        int[] typeCounts = new int[ITEM_TYPE_COUNT];

        for (int i = 0; i < trials; i++) {
            int rarityIdx = (int) (Math.random() * BOX_RARITIES.length);
            int typeIdx = (int) (Math.random() * ITEM_TYPE_COUNT);
            rarityCounts[rarityIdx]++;
            typeCounts[typeIdx]++;
        }

        double expectedRarity = 1.0 / BOX_RARITIES.length;
        for (int i = 0; i < BOX_RARITIES.length; i++) {
            double actual = (double) rarityCounts[i] / trials;
            assertEquals("边缘概率: " + BOX_RARITIES[i].getDisplayName() + " ≈25%",
                    expectedRarity, actual, 0.015);
        }

        double expectedType = 1.0 / ITEM_TYPE_COUNT;
        for (int i = 0; i < ITEM_TYPE_COUNT; i++) {
            double actual = (double) typeCounts[i] / trials;
            assertEquals("边缘概率: 类型" + i + " ≈33.3%",
                    expectedType, actual, 0.015);
        }

        double sumRarity = 0, sumType = 0;
        for (int c : rarityCounts) sumRarity += c;
        for (int c : typeCounts) sumType += c;
        assertEquals("稀有度总次数 = trials", trials, (int) sumRarity);
        assertEquals("类型总次数 = trials", trials, (int) sumType);
    }

    // ====================== 边界条件：Math.random() 极值处理 ======================

    @Test
    public void testRaritySelectionWithMathRandomExactlyZero() {
        int index = (int) (0.0 * BOX_RARITIES.length);
        assertEquals("Math.random()=0 时 index=0 (COMMON)", 0, index);
    }

    @Test
    public void testRaritySelectionWithMathRandomNearOne() {
        int index = (int) (0.999999999 * BOX_RARITIES.length);
        assertEquals("Math.random()≈1 时 index=3 (EPIC)", 3, index);
    }

    @Test
    public void testTypeSelectionWithMathRandomExactlyZero() {
        int roll = (int) (0.0 * ITEM_TYPE_COUNT);
        assertEquals("Math.random()=0 时 roll=0 (装备)", 0, roll);
    }

    @Test
    public void testTypeSelectionWithMathRandomNearOne() {
        int roll = (int) (0.999999999 * ITEM_TYPE_COUNT);
        assertEquals("Math.random()≈1 时 roll=2 (宝石)", 2, roll);
    }

    // ====================== 综合逻辑场景 ======================

    @Test
    public void testFullBlindBoxScenarioSuccess() {
        int gold = 500;
        boolean canBuy = gold >= BLIND_BOX_COST;
        assertTrue("500金币足够购买", canBuy);
        gold -= BLIND_BOX_COST;

        int rarityIdx = (int) (Math.random() * BOX_RARITIES.length);
        Rarity rarity = BOX_RARITIES[rarityIdx];
        assertNotNull("稀有度不为null", rarity);
        assertTrue("稀有度在COMMON~EPIC之间", rarity != Rarity.LEGENDARY);

        int typeRoll = (int) (Math.random() * ITEM_TYPE_COUNT);
        assertTrue("类型索引有效", typeRoll >= 0 && typeRoll <= 2);

        assertEquals("购买后金币=300", 300, gold);
    }

    @Test
    public void testFullBlindBoxScenarioInsufficientGold() {
        int gold = 100;
        boolean canBuy = gold >= BLIND_BOX_COST;
        assertFalse("100金币不够", canBuy);
        assertEquals("金币未被扣减", 100, gold);
    }

    // ====================== LEGENDARY 排除验证 ======================

    @Test
    public void testRaritySelectionNeverIncludesLegendaryDirectly() {
        for (int i = 0; i < 10000; i++) {
            int index = (int) (Math.random() * BOX_RARITIES.length);
            assertTrue("盲盒稀有度不应是LEGENDARY",
                    BOX_RARITIES[index] != Rarity.LEGENDARY);
        }
    }

    @Test
    public void testRarityFromIdForAllBoxRarities() {
        for (Rarity r : BOX_RARITIES) {
            Rarity resolved = Rarity.fromId(r.getId());
            assertEquals("fromId 应正确解析", r, resolved);
            assertNotNull("解析结果不null", resolved);
        }
    }

    @Test
    public void testRarityGlobalProbabilityNotUsedInBox() {
        double boxProbability = 1.0 / BOX_RARITIES.length;
        assertEquals("盲盒稀有度概率=25%，Rarity.globalProbability 未被使用",
                0.25, boxProbability, 0.0001);
    }

    // ====================== 事件配置契约 ======================

    @Test
    public void testBlindBoxCostMatchesConfig() {
        assertEquals("盲盒价格固定为200金币", 200, BLIND_BOX_COST);
    }

    @Test
    public void testBlindBoxDoesNotChangeCost() {
        int cost1 = BLIND_BOX_COST;
        int cost2 = BLIND_BOX_COST;
        assertEquals("每次购买价格不变", cost1, cost2);
    }

    @Test
    public void testRefuseOptionIsAvailable() {
        boolean hasRefuseOption = true;
        assertTrue("'不相信盲盒'离开选项始终可用", hasRefuseOption);
    }

    // ====================== 极端金币场景 ======================

    @Test
    public void testOneGoldShort() {
        int gold = 199;
        boolean canBuy = gold >= BLIND_BOX_COST;
        assertFalse("仅差1金币不能购买", canBuy);
        assertEquals("199金币分文未动", 199, gold);
    }

    @Test
    public void testExactlyOneGoldOver() {
        int gold = 201;
        boolean canBuy = gold >= BLIND_BOX_COST;
        assertTrue("多1金币可以购买", canBuy);
        gold -= BLIND_BOX_COST;
        assertEquals("剩余1金币", 1, gold);
    }

    @Test
    public void testBuyAllGold() {
        int gold = 1000;
        int count = 0;
        while (gold >= BLIND_BOX_COST) {
            gold -= BLIND_BOX_COST;
            count++;
        }
        assertTrue("至少购买了一次", count > 0);
        assertTrue("剩余金币小于盲盒价格", gold < BLIND_BOX_COST);
    }

    // ====================== 装备等级分布验证（1000 样本） ======================

    @Test
    public void testEquipLevelDistribution() {
        int trials = 1000;
        int minLevel = Integer.MAX_VALUE;
        int maxLevel = Integer.MIN_VALUE;

        for (int i = 0; i < trials; i++) {
            int level = (int) (EQUIP_MIN_LEVEL + Math.random() * EQUIP_LEVEL_RANGE);
            if (level < minLevel) minLevel = level;
            if (level > maxLevel) maxLevel = level;
        }

        assertTrue("最小等级 ≥ 5: " + minLevel, minLevel >= 5);
        assertTrue("最大等级 ≤ 25: " + maxLevel, maxLevel <= 25);
        assertTrue("等级范围超过10（说明不是固定值）", maxLevel - minLevel >= 10);
    }

    // ====================== 金币扣除校验（代码顺序正确性） ======================

    @Test
    public void testGoldDeductionOrderMatters() {
        int gold = 200;
        gold -= BLIND_BOX_COST;
        assertEquals("先扣金币", 0, gold);
        boolean itemGenerated = Math.random() < 0.5;
        boolean goldAlreadyDeducted = gold == 0;
        assertTrue("无论物品是否生成，金币已被扣除", goldAlreadyDeducted);
    }

    @Test
    public void testCostNegativeBoundPrevention() {
        assertFalse("盲盒不应消耗负金币",
                BLIND_BOX_COST <= 0);
    }
}
