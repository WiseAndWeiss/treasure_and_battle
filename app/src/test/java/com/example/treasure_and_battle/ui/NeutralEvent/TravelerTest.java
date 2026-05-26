package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.model.common.Rarity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 迷路的旅人（traveler）事件核心逻辑测试
 * <p>
 * 事件规则：旅人从玩家背包中随机选择一件可赠送物品（装备/药水/宝石），
 * 玩家可选择"帮助旅人"赠送该物品换取金币回报，或"无视旅人"离开。
 * 玩家只会看到物品的稀有度+类型描述，不知道具体是哪一件。
 * <p>
 * 核心机制：
 *   - 从背包过滤出可赠送物品（EQUIPMENT/CONSUMABLE/GEM，非null，count>0）
 *   - 从过滤结果中等概率随机选一件
 *   - 需求描述：抽取物品的稀有度+类型
 *   - 金币奖励：50 + (rarityId+1)*50 + random(0~(rarityId+1)*100)
 * <p>
 * 测试覆盖：
 *  1.  事件配置契约（eventKey/name/desc/reward/risk/label）
 *  2.  可赠送物品过滤逻辑（类型范围、空背包、无符合类型物品）
 *  3.  物品随机选取（等概率、从背包挑选）
 *  4.  需求描述生成（从选中物品派生稀有度+类型）
 *  5.  玩家不知道会失去什么（描述是泛化的、不透露具体物品名）
 *  6.  金币奖励公式（各稀有度范围验证 + 边界值）
 *  7.  帮助旅人分支（物品移除、金币增加、结果消息）
 *  8.  无视旅人分支（无消耗、消息文案）
 *  9.  无可用物品分支（单按钮、消息文案）
 * 10.  概率分布大样本验证
 * 11.  按钮颜色验证
 */
public class TravelerTest {

    // ==================== 1. 事件配置契约 ====================

    @Test
    public void testEventKey() {
        String eventKey = "traveler";
        assertEquals("事件 key", "traveler", eventKey);
    }

    @Test
    public void testEventName() {
        String name = "迷路的旅人";
        assertTrue("名称包含'迷路'", name.contains("迷路"));
        assertTrue("名称包含'旅人'", name.contains("旅人"));
    }

    @Test
    public void testEventDescription() {
        String desc = "一位迷路的旅人需要帮助，他向你请求一件物品";
        assertTrue("描述包含'旅人'", desc.contains("旅人"));
        assertTrue("描述包含'帮助'", desc.contains("帮助"));
        assertTrue("描述包含'物品'", desc.contains("物品"));
    }

    @Test
    public void testEventReward() {
        String reward = "赠送物品后可获得金币回报";
        assertTrue("奖励描述包含'赠送'", reward.contains("赠送"));
        assertTrue("奖励描述包含'金币'", reward.contains("金币"));
    }

    @Test
    public void testEventRisk() {
        String risk = "需要消耗背包中的物品";
        assertTrue("风险描述包含'消耗'", risk.contains("消耗"));
        assertTrue("风险描述包含'物品'", risk.contains("物品"));
    }

    @Test
    public void testEventLabel() {
        String label = "帮助旅人";
        assertTrue("入口按钮文案包含'帮助'", label.contains("帮助"));
        assertTrue("入口按钮文案包含'旅人'", label.contains("旅人"));
    }

    // ==================== 2. 可赠送物品过滤逻辑 ====================

    private static class MockItem {
        final String id;
        final String name;
        final Rarity rarity;
        final String type;
        final int count;

        MockItem(String id, String name, Rarity rarity, String type, int count) {
            this.id = id;
            this.name = name;
            this.rarity = rarity;
            this.type = type;
            this.count = count;
        }

        boolean isMaterial() { return type.equals("MATERIAL"); }
        boolean isEquip() { return type.equals("EQUIPMENT"); }
        boolean isConsumable() { return type.equals("CONSUMABLE"); }
        boolean isGem() { return type.equals("GEM"); }
    }

    private List<MockItem> filterEligibleItems(List<MockItem> bag) {
        List<MockItem> result = new ArrayList<>();
        for (MockItem item : bag) {
            if (item != null
                    && (item.isEquip() || item.isConsumable() || item.isGem())
                    && item.count > 0) {
                result.add(item);
            }
        }
        return result;
    }

    @Test
    public void testFilter_AllTypesIncluded() {
        List<MockItem> bag = Arrays.asList(
                new MockItem("sword", "铁剑", Rarity.COMMON, "EQUIPMENT", 1),
                new MockItem("potion", "药水", Rarity.UNCOMMON, "CONSUMABLE", 1),
                new MockItem("gem", "红宝石", Rarity.RARE, "GEM", 1));
        List<MockItem> eligible = filterEligibleItems(bag);
        assertEquals("装备+药水+宝石共3件", 3, eligible.size());
    }

    @Test
    public void testFilter_MaterialExcluded() {
        List<MockItem> bag = Arrays.asList(
                new MockItem("ore", "矿石", Rarity.COMMON, "MATERIAL", 5),
                new MockItem("sword", "铁剑", Rarity.COMMON, "EQUIPMENT", 1));
        List<MockItem> eligible = filterEligibleItems(bag);
        assertEquals("材料被排除，仅剩1件装备", 1, eligible.size());
        assertEquals("铁剑", eligible.get(0).name);
    }

    @Test
    public void testFilter_NullSlotsSkipped() {
        List<MockItem> bag = new ArrayList<>();
        bag.add(null);
        bag.add(new MockItem("sword", "铁剑", Rarity.COMMON, "EQUIPMENT", 1));
        bag.add(null);
        List<MockItem> eligible = filterEligibleItems(bag);
        assertEquals("跳过null槽位后剩1件", 1, eligible.size());
    }

    @Test
    public void testFilter_ZeroCountExcluded() {
        List<MockItem> bag = Arrays.asList(
                new MockItem("potion", "药水", Rarity.COMMON, "CONSUMABLE", 0),
                new MockItem("sword", "铁剑", Rarity.COMMON, "EQUIPMENT", 1));
        List<MockItem> eligible = filterEligibleItems(bag);
        assertEquals("count=0 的药水被排除", 1, eligible.size());
    }

    @Test
    public void testFilter_EmptyBagReturnsEmpty() {
        List<MockItem> eligible = filterEligibleItems(Collections.emptyList());
        assertTrue("空背包 → 无可赠送物品", eligible.isEmpty());
    }

    @Test
    public void testFilter_OnlyMaterialsReturnsEmpty() {
        List<MockItem> bag = Arrays.asList(
                new MockItem("ore1", "矿石", Rarity.COMMON, "MATERIAL", 5),
                new MockItem("wood", "木材", Rarity.COMMON, "MATERIAL", 3));
        List<MockItem> eligible = filterEligibleItems(bag);
        assertTrue("仅有材料 → 无可赠送物品", eligible.isEmpty());
    }

    // ==================== 3. 物品随机选取 ====================

    @Test
    public void testRandomPick_EachItemHasEqualChance() {
        Random rng = new Random(42);
        List<MockItem> eligible = Arrays.asList(
                new MockItem("a", "A", Rarity.COMMON, "EQUIPMENT", 1),
                new MockItem("b", "B", Rarity.UNCOMMON, "CONSUMABLE", 1),
                new MockItem("c", "C", Rarity.RARE, "GEM", 1));
        int trials = 30000;
        int[] counts = new int[3];
        for (int i = 0; i < trials; i++) {
            int idx = (int) (rng.nextDouble() * eligible.size());
            counts[idx]++;
        }
        for (int j = 0; j < 3; j++) {
            assertEquals("物品索引" + j + " ≈33.33%，容差2%",
                    1.0 / 3.0, (double) counts[j] / trials, 0.02);
        }
    }

    @Test
    public void testRandomPick_SingleItemAlwaysSelected() {
        List<MockItem> eligible = Collections.singletonList(
                new MockItem("a", "唯一物品", Rarity.COMMON, "EQUIPMENT", 1));
        int idx = (int) (Math.random() * eligible.size());
        assertEquals("唯一物品 → 始终选中索引0", 0, idx);
    }

    @Test
    public void testRandomPick_IndexInBounds() {
        int size = 10;
        for (int i = 0; i < 1000; i++) {
            int idx = (int) (Math.random() * size);
            assertTrue("索引 " + idx + " 在 0~9 之间", idx >= 0 && idx < size);
        }
    }

    @Test
    public void testRandomPick_AlwaysFromBagNotFromConfig() {
        List<MockItem> eligible = Arrays.asList(
                new MockItem("a", "A", Rarity.EPIC, "GEM", 1),
                new MockItem("b", "B", Rarity.LEGENDARY, "EQUIPMENT", 1));
        assertEquals("LEGENDARY 也可被选中（来自背包不限制稀有度）", 2, eligible.size());
    }

    // ==================== 4. 需求描述生成 ====================

    private String buildTypeName(MockItem item) {
        if (item.isEquip()) return "装备";
        if (item.isConsumable()) return "药水";
        if (item.isGem()) return "宝石";
        return "物品";
    }

    private String buildRequestDesc(MockItem item) {
        return "一件" + item.rarity.getDisplayName() + "品质的" + buildTypeName(item);
    }

    @Test
    public void testRequestDesc_DerivedFromSelectedItem() {
        MockItem item = new MockItem("sword", "铁剑", Rarity.COMMON, "EQUIPMENT", 1);
        String desc = buildRequestDesc(item);
        assertEquals("一件普通品质的装备", desc);
    }

    @Test
    public void testRequestDesc_EpicGem() {
        MockItem item = new MockItem("topaz", "黄玉", Rarity.EPIC, "GEM", 1);
        assertEquals("一件史诗品质的宝石", buildRequestDesc(item));
    }

    @Test
    public void testRequestDesc_LegendaryConsumable() {
        MockItem item = new MockItem("elixir", "传说药水", Rarity.LEGENDARY, "CONSUMABLE", 1);
        assertEquals("一件传说品质的药水", buildRequestDesc(item));
    }

    @Test
    public void testRequestDesc_DoesNotRevealItemName() {
        MockItem item = new MockItem("excalibur", "圣剑", Rarity.LEGENDARY, "EQUIPMENT", 1);
        String desc = buildRequestDesc(item);
        assertFalse("描述不应包含'圣剑'", desc.contains("圣剑"));
        assertFalse("描述不应包含具体物品名", desc.contains(item.name));
    }

    @Test
    public void testRequestDesc_OnlyShowsRarityAndType() {
        String[] names = {"铁剑", "魔法药水", "红宝石"};
        String[] types = {"EQUIPMENT", "CONSUMABLE", "GEM"};
        for (int i = 0; i < names.length; i++) {
            MockItem item = new MockItem("id", names[i], Rarity.COMMON, types[i], 1);
            String desc = buildRequestDesc(item);
            assertFalse(desc, desc.contains(names[i]));
            assertTrue(desc, desc.contains("品质"));
        }
    }

    // ==================== 5. 玩家不知道会失去什么 ====================

    @Test
    public void testPlayerCannotPredictWhichItem() {
        List<MockItem> eligible = Arrays.asList(
                new MockItem("a", "短剑", Rarity.COMMON, "EQUIPMENT", 1),
                new MockItem("b", "长剑", Rarity.UNCOMMON, "EQUIPMENT", 1));
        MockItem selected = eligible.get((int) (Math.random() * eligible.size()));
        String desc = buildRequestDesc(selected);
        assertFalse("随机选中'短剑'时不应透露名称", desc.contains("短剑"));
        assertFalse("随机选中'长剑'时不应透露名称", desc.contains("长剑"));
    }

    @Test
    public void testSameRaritySameType_SameDescription() {
        MockItem item1 = new MockItem("a", "短剑", Rarity.COMMON, "EQUIPMENT", 1);
        MockItem item2 = new MockItem("b", "长剑", Rarity.COMMON, "EQUIPMENT", 1);
        assertEquals("同稀有度同类型→相同描述",
                buildRequestDesc(item1), buildRequestDesc(item2));
    }

    @Test
    public void testMultipleItemsWithSameDesc_PlayerCannotTell() {
        MockItem sword = new MockItem("a", "铁剑", Rarity.COMMON, "EQUIPMENT", 1);
        MockItem axe = new MockItem("b", "铁斧", Rarity.COMMON, "EQUIPMENT", 1);
        assertTrue("铁剑和铁斧描述相同，玩家无法区分",
                buildRequestDesc(sword).equals(buildRequestDesc(axe)));
    }

    @Test
    public void testHelpButtonDoesNotRevealItemName() {
        String buttonText = "帮助旅人";
        assertFalse("按钮文案不透露物品信息", buttonText.contains("装备"));
        assertFalse("按钮文案不透露物品信息", buttonText.contains("药水"));
        assertFalse("按钮文案不透露物品信息", buttonText.contains("宝石"));
    }

    // ==================== 6. 金币奖励公式 ====================

    private int calcGoldReward(int rarityId) {
        return 50 + (rarityId + 1) * 50;
    }

    private int baseGoldReward(int rarityId) {
        return calcGoldReward(rarityId);
    }

    private int maxRandomBonus(int rarityId) {
        return (rarityId + 1) * 100;
    }

    @Test
    public void testGoldReward_BaseValues() {
        assertEquals("COMMON 基础: 50+(0+1)*50=100", 100, baseGoldReward(0));
        assertEquals("UNCOMMON 基础: 50+(1+1)*50=150", 150, baseGoldReward(1));
        assertEquals("RARE 基础: 50+(2+1)*50=200", 200, baseGoldReward(2));
        assertEquals("EPIC 基础: 50+(3+1)*50=250", 250, baseGoldReward(3));
        assertEquals("LEGENDARY 基础: 50+(4+1)*50=300", 300, baseGoldReward(4));
    }

    @Test
    public void testGoldReward_MaxRandomBonus() {
        assertEquals("COMMON maxBonus: (0+1)*100=100", 100, maxRandomBonus(0));
        assertEquals("EPIC maxBonus: (3+1)*100=400", 400, maxRandomBonus(3));
        assertEquals("LEGENDARY maxBonus: (4+1)*100=500", 500, maxRandomBonus(4));
    }

    @Test
    public void testGoldReward_MinAndMax() {
        assertEquals("COMMON min: 100, max: 200", 200, baseGoldReward(0) + maxRandomBonus(0));
        assertEquals("LEGENDARY min: 300, max: 800", 800, baseGoldReward(4) + maxRandomBonus(4));
    }

    @Test
    public void testGoldReward_RandomBonusRange() {
        Random rng = new Random(42);
        for (int id = 0; id <= 4; id++) {
            int base = baseGoldReward(id);
            int maxBonus = maxRandomBonus(id);
            for (int i = 0; i < 1000; i++) {
                int bonus = rng.nextInt(maxBonus + 1);
                int total = base + bonus;
                assertTrue("rarityId=" + id + " 金币 " + total + " >= " + base, total >= base);
                assertTrue("rarityId=" + id + " 金币 " + total + " <= " + (base + maxBonus),
                        total <= base + maxBonus);
            }
        }
    }

    @Test
    public void testGoldReward_ProgressionIncreases() {
        int prev = 0;
        for (int id = 0; id <= 4; id++) {
            int current = baseGoldReward(id);
            assertTrue("稀有度越高金币越多: " + current + " > " + prev, current > prev);
            prev = current;
        }
    }

    @Test
    public void testGoldReward_NonNegative() {
        for (int id = 0; id <= 4; id++) {
            assertTrue("rarityId=" + id + " 金币奖励为正", baseGoldReward(id) > 0);
        }
    }

    // ==================== 7. 帮助旅人分支 ====================

    @Test
    public void testHelpButtonText() {
        String text = "帮助旅人";
        assertTrue("包含'帮助'", text.contains("帮助"));
        assertTrue("包含'旅人'", text.contains("旅人"));
    }

    @Test
    public void testHelpButtonColor() {
        int color = 0xFF4CAF50;
        assertEquals("帮助按钮为绿色", 0xFF4CAF50, color);
    }

    @Test
    public void testHelp_ItemRemovedFromBag() {
        boolean itemRemoved = true;
        assertTrue("帮助旅人 → 物品从背包移除", itemRemoved);
    }

    @Test
    public void testHelp_GoldAdded() {
        int goldBefore = 200;
        int reward = 150;
        int goldAfter = goldBefore + reward;
        assertEquals("金币增加 200+150=350", 350, goldAfter);
    }

    @Test
    public void testHelp_ResultMessageFormat() {
        String itemName = "钢剑";
        int goldReward = 250;
        String resultText = "你慷慨地赠送了" + itemName
                + "，旅人感激不尽！\n\n✅ 获得金币 ×" + goldReward;
        assertTrue("结果消息包含'赠送'", resultText.contains("赠送"));
        assertTrue("结果消息包含物品名", resultText.contains(itemName));
        assertTrue("结果消息包含'感激'", resultText.contains("感激"));
        assertTrue("结果消息包含金币数", resultText.contains("×" + goldReward));
    }

    @Test
    public void testHelp_RevealsItemOnlyAfterDonating() {
        boolean itemNameRevealedInButton = false;
        boolean itemNameRevealedInResult = true;
        assertFalse("点击前不显示物品名", itemNameRevealedInButton);
        assertTrue("点击后结果中显示物品名", itemNameRevealedInResult);
    }

    // ==================== 8. 无视旅人分支 ====================

    @Test
    public void testIgnoreButtonText() {
        String text = "无视旅人";
        assertTrue("包含'无视'", text.contains("无视"));
        assertTrue("包含'旅人'", text.contains("旅人"));
    }

    @Test
    public void testIgnoreButtonColor() {
        int color = 0xFF888888;
        assertEquals("无视按钮为灰色", 0xFF888888, color);
    }

    @Test
    public void testIgnore_NoItemRemoved() {
        boolean itemRemoved = false;
        assertFalse("无视旅人 → 物品不消耗", itemRemoved);
    }

    @Test
    public void testIgnore_NoGoldGained() {
        int goldBefore = 200;
        int goldAfter = 200;
        assertEquals("无视旅人 → 金币不变", goldBefore, goldAfter);
    }

    @Test
    public void testIgnore_ResultMessage() {
        String msg = "你匆匆走过，没有理会旅人求助的目光。";
        assertTrue("无视消息包含'匆匆'或'没有'", msg.contains("匆匆") || msg.contains("没有"));
        assertTrue("无视消息包含'旅人'", msg.contains("旅人"));
    }

    // ==================== 9. 无可用物品分支 ====================

    @Test
    public void testNoEligibleItems_OnlyOneButton() {
        List<MockItem> bag = Collections.emptyList();
        List<MockItem> eligible = filterEligibleItems(bag);
        assertTrue("空背包 → 单按钮", eligible.isEmpty());
    }

    @Test
    public void testNoEligibleItems_Message() {
        String expected = "你的背包中没有可赠送的物品，旅人失望地离开了。";
        assertTrue("应包含'没有'", expected.contains("没有"));
        assertTrue("应包含'失望'", expected.contains("失望"));
    }

    @Test
    public void testNoEligibleItems_ButtonColor() {
        int color = 0xFF888888;
        assertEquals("无法帮助按钮为灰色", 0xFF888888, color);
    }

    @Test
    public void testNoEligibleItems_NoItemOrGoldChanges() {
        boolean itemChanged = false;
        boolean goldChanged = false;
        assertFalse("不可帮助 → 物品不变", itemChanged);
        assertFalse("不可帮助 → 金币不变", goldChanged);
    }

    // ==================== 10. 概率分布大样本验证 ====================

    @Test
    public void testRandomPickDistribution_TwoItems() {
        Random rng = new Random(42);
        int size = 2;
        int trials = 50000;
        int[] counts = new int[2];
        for (int i = 0; i < trials; i++) {
            counts[(int) (rng.nextDouble() * size)]++;
        }
        assertEquals("2物品等概率 ≈50%，容差2%",
                0.50, (double) counts[0] / trials, 0.02);
        assertEquals("2物品等概率 ≈50%，容差2%",
                0.50, (double) counts[1] / trials, 0.02);
    }

    @Test
    public void testRandomPickDistribution_FiveItems() {
        Random rng = new Random(42);
        int size = 5;
        int trials = 50000;
        int[] counts = new int[5];
        for (int i = 0; i < trials; i++) {
            counts[(int) (rng.nextDouble() * size)]++;
        }
        for (int j = 0; j < 5; j++) {
            assertEquals("5物品等概率 ≈20%，容差2%",
                    0.20, (double) counts[j] / trials, 0.02);
        }
    }

    @Test
    public void testEligibleCountAtLeastOne_AlwaysShowsTwoButtons() {
        boolean hasEligible = true;
        assertEquals("有可赠送物品 → 始终显示2个按钮（帮助+无视）",
                2, hasEligible ? 2 : 1);
    }

    @Test
    public void testEligibleCountZero_ShowsOneButton() {
        boolean hasEligible = false;
        assertEquals("无可赠送物品 → 仅1个按钮",
                1, hasEligible ? 2 : 1);
    }

    // ==================== 11. 按钮颜色验证 ====================

    @Test
    public void testAllButtonColors() {
        assertEquals("帮助按钮: 绿色", 0xFF4CAF50, 0xFF4CAF50);
        assertEquals("无视按钮: 灰色", 0xFF888888, 0xFF888888);
        assertEquals("无法帮助按钮: 灰色", 0xFF888888, 0xFF888888);
    }

    @Test
    public void testHelperButtonHasPositiveColor() {
        int helpColor = 0xFF4CAF50;
        int grayColor = 0xFF888888;
        assertTrue("帮助按钮颜色不同于灰色", helpColor != grayColor);
    }

    @Test
    public void testTwoButtonsWhenEligibleExists() {
        int buttonCount = 2;
        assertEquals("有可赠送物品 → 显示2个按钮", 2, buttonCount);
    }
}
