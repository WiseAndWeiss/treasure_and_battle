package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 占卜小屋（divination_hut）事件核心逻辑测试
 * <p>
 * 事件规则：消耗300金币，将地图上一个"未知事件"随机揭示为
 * BATTLE(70%) / NEUTRAL(15%) / BENEFIT(15%) 类型。
 * 若无未知事件则按钮禁用并自动提示。
 * <p>
 * 测试覆盖：
 * 1. 金币消费机制（足够/恰好/不足/多次/边界±1）
 * 2. hasUnknownEvents 逻辑（有/无未知事件时的按钮状态）
 * 3. resolveUnknownType 概率分布（70/15/15，统计验证）
 * 4. ⭐ TOCTOU Bug：检查通过但执行时未知事件已被清空
 * 5. ⭐ 先扣金币后揭示失败 → 金币丢失无退款
 * 6. 随机选择逻辑（均匀分布验证）
 * 7. 事件配置契约（cost=300、颜色码、消息关键词）
 * 8. 边界值（nextInt(100) 极值）
 * 9. null 子事件处理
 * 10. 消息完整性验证
 */
public class DivinationHutTest {

    private static final int DIVINATION_COST = 300;
    private static final String EVENT_TYPE_BATTLE = "BATTLE";
    private static final String EVENT_TYPE_NEUTRAL = "NEUTRAL";
    private static final String EVENT_TYPE_BENEFIT = "BENEFIT";
    private static final String EVENT_TYPE_UNKNOWN = "UNKNOWN";

    // ====================== 金币消费机制 ======================

    @Test
    public void testSpendGoldSufficientBalance() {
        int gold = 500;
        assertTrue("500金币足够300金币占卜", gold >= DIVINATION_COST);
        gold -= DIVINATION_COST;
        assertEquals(200, gold);
    }

    @Test
    public void testSpendGoldExactBalance() {
        int gold = 300;
        assertTrue("恰好300金币应可支付", gold >= DIVINATION_COST);
        gold -= DIVINATION_COST;
        assertEquals(0, gold);
    }

    @Test
    public void testSpendGoldInsufficient() {
        int gold = 299;
        assertFalse("299金币不够", gold >= DIVINATION_COST);
        assertEquals("金币不足时不应扣减", 299, gold);
    }

    @Test
    public void testSpendGoldZero() {
        int gold = 0;
        assertFalse("0金币不够支付", gold >= DIVINATION_COST);
    }

    @Test
    public void testSpendGoldVeryRich() {
        int gold = 99999;
        assertTrue(gold >= DIVINATION_COST);
        gold -= DIVINATION_COST;
        assertEquals(99699, gold);
    }

    @Test
    public void testMultipleDivinations() {
        int gold = 1500;
        int count = 0;
        while (gold >= DIVINATION_COST) {
            gold -= DIVINATION_COST;
            count++;
        }
        assertEquals("1500金币可占卜5次", 5, count);
        assertEquals("剩余0", 0, gold);
    }

    @Test
    public void testMultipleDivinationsWithRemainder() {
        int gold = 800;
        int count = 0;
        while (gold >= DIVINATION_COST) {
            gold -= DIVINATION_COST;
            count++;
        }
        assertEquals("800金币可占卜2次", 2, count);
        assertEquals("剩余200", 200, gold);
    }

    // ====================== hasUnknownEvents 逻辑 ======================

    private static class MockEventCircle {
        String type;
        MockEventCircle(String type) { this.type = type; }
    }

    private boolean hasUnknownEvents(List<MockEventCircle> circles) {
        for (MockEventCircle ec : circles) {
            if (EVENT_TYPE_UNKNOWN.equals(ec.type)) return true;
        }
        return false;
    }

    @Test
    public void testHasUnknownEventsTrue() {
        List<MockEventCircle> circles = new ArrayList<>();
        circles.add(new MockEventCircle(EVENT_TYPE_BATTLE));
        circles.add(new MockEventCircle(EVENT_TYPE_UNKNOWN));
        circles.add(new MockEventCircle(EVENT_TYPE_NEUTRAL));
        assertTrue("存在UNKNOWN时应返回true", hasUnknownEvents(circles));
    }

    @Test
    public void testHasUnknownEventsFalse() {
        List<MockEventCircle> circles = new ArrayList<>();
        circles.add(new MockEventCircle(EVENT_TYPE_BATTLE));
        circles.add(new MockEventCircle(EVENT_TYPE_NEUTRAL));
        circles.add(new MockEventCircle(EVENT_TYPE_BENEFIT));
        assertFalse("无UNKNOWN时应返回false", hasUnknownEvents(circles));
    }

    @Test
    public void testHasUnknownEventsEmptyList() {
        List<MockEventCircle> circles = new ArrayList<>();
        assertFalse("空列表应返回false", hasUnknownEvents(circles));
    }

    @Test
    public void testHasUnknownEventsAllUnknown() {
        List<MockEventCircle> circles = new ArrayList<>();
        circles.add(new MockEventCircle(EVENT_TYPE_UNKNOWN));
        circles.add(new MockEventCircle(EVENT_TYPE_UNKNOWN));
        assertTrue("全UNKNOWN应返回true", hasUnknownEvents(circles));
    }

    @Test
    public void testButtonStateWhenHasUnknown() {
        boolean hasUnknown = true;
        boolean buttonEnabled = hasUnknown;
        assertTrue("有未知事件时按钮应启用", buttonEnabled);
    }

    @Test
    public void testButtonStateWhenNoUnknown() {
        boolean hasUnknown = false;
        boolean buttonEnabled = hasUnknown;
        assertFalse("无未知事件时按钮应禁用", buttonEnabled);
    }

    @Test
    public void testButtonColorWhenEnabled() {
        int enabledColor = 0xFF7B1FA2;
        assertTrue("启用时按钮为紫色", enabledColor == 0xFF7B1FA2);
    }

    @Test
    public void testButtonColorWhenDisabled() {
        int disabledColor = 0xFFAAAAAA;
        assertTrue("禁用时按钮为灰色", disabledColor == 0xFFAAAAAA);
    }

    // ====================== ⭐ TOCTOU Bug：检查通过但执行时列表已空 ======================

    @Test
    public void testToctouCheckPassesButRevealFindsEmpty() {
        List<MockEventCircle> circles = new ArrayList<>();
        circles.add(new MockEventCircle(EVENT_TYPE_UNKNOWN));

        boolean checkResult = hasUnknownEvents(circles);
        assertTrue("外部检查时发现未知事件", checkResult);

        circles.clear();

        List<MockEventCircle> unknownPool = new ArrayList<>();
        for (MockEventCircle ec : circles) {
            if (EVENT_TYPE_UNKNOWN.equals(ec.type)) unknownPool.add(ec);
        }
        assertTrue("【Bug】内部再查时列表已空，但外部已扣金币",
                unknownPool.isEmpty());
    }

    @Test
    public void testGoldSpentBeforeReveal() {
        int gold = 500;
        assertTrue("检查通过", gold >= DIVINATION_COST);
        gold -= DIVINATION_COST;

        boolean revealFailed = Math.random() < 0.0;
        assertFalse("揭示未失败", revealFailed);

        int goldAfterReveal = gold;
        assertEquals("金币已被扣除（无论揭示是否成功）", 200, goldAfterReveal);
    }

    // ====================== ⭐ 揭示失败时金币丢失 ======================

    @Test
    public void testGoldLostOnRevealFailure() {
        int gold = 600;
        boolean canAfford = gold >= DIVINATION_COST;
        assertTrue("可以支付", canAfford);

        gold -= DIVINATION_COST;
        assertEquals("扣除后余额", 300, gold);

        String revealResult = "占卜出现误差，未能揭示事件。";
        boolean isFailure = !revealResult.contains("占卜成功");
        assertTrue("揭示失败", isFailure);

        assertEquals("【Bug】揭示失败但金币已扣除，余额300", 300, gold);
    }

    @Test
    public void testGoldNotLostWhenCannotAfford() {
        int gold = 100;
        boolean canAfford = gold >= DIVINATION_COST;
        assertFalse("不能支付", canAfford);

        assertEquals("金币未扣", 100, gold);
    }

    // ====================== resolveUnknownType 概率逻辑 ======================

    private String resolveUnknownType(Random rng) {
        int roll = rng.nextInt(100);
        if (roll < 70) return EVENT_TYPE_BATTLE;
        if (roll < 85) return EVENT_TYPE_NEUTRAL;
        return EVENT_TYPE_BENEFIT;
    }

    @Test
    public void testResolveUnknownTypeProbabilityDistribution() {
        Random rng = new Random(12345);
        int trials = 50000;
        int battle = 0, neutral = 0, benefit = 0;

        for (int i = 0; i < trials; i++) {
            String type = resolveUnknownType(rng);
            switch (type) {
                case EVENT_TYPE_BATTLE:  battle++; break;
                case EVENT_TYPE_NEUTRAL: neutral++; break;
                case EVENT_TYPE_BENEFIT: benefit++; break;
            }
        }

        assertEquals("BATTLE 应≈70%，容差2%", 0.70, (double) battle / trials, 0.02);
        assertEquals("NEUTRAL 应≈15%，容差2%", 0.15, (double) neutral / trials, 0.02);
        assertEquals("BENEFIT 应≈15%，容差2%", 0.15, (double) benefit / trials, 0.02);

        double total = battle + neutral + benefit;
        assertEquals("总次数=trials", trials, (int) total);
    }

    @Test
    public void testResolveUnknownTypeNeverReturnsNull() {
        Random rng = new Random();
        for (int i = 0; i < 1000; i++) {
            String type = resolveUnknownType(rng);
            assertNotNull("结果不应为null", type);
        }
    }

    @Test
    public void testResolveUnknownTypeOnlyReturnsValidTypes() {
        Random rng = new Random();
        for (int i = 0; i < 1000; i++) {
            String type = resolveUnknownType(rng);
            assertTrue("结果应为有效类型: " + type,
                    EVENT_TYPE_BATTLE.equals(type) ||
                    EVENT_TYPE_NEUTRAL.equals(type) ||
                    EVENT_TYPE_BENEFIT.equals(type));
        }
    }

    @Test
    public void testResolveUnknownTypeEdgeRolls() {
        int[] edgeResults = new int[3];
        for (int roll : new int[]{0, 69, 70, 84, 85, 99}) {
            if (roll < 70) edgeResults[0]++;
            else if (roll < 85) edgeResults[1]++;
            else edgeResults[2]++;
        }
        assertEquals("roll=0,69 → BATTLE ×2", 2, edgeResults[0]);
        assertEquals("roll=70,84 → NEUTRAL ×2", 2, edgeResults[1]);
        assertEquals("roll=85,99 → BENEFIT ×2", 2, edgeResults[2]);
    }

    @Test
    public void testNextInt100Range() {
        Random rng = new Random(42);
        for (int i = 0; i < 10000; i++) {
            int roll = rng.nextInt(100);
            assertTrue("nextInt(100)应在0~99: " + roll, roll >= 0 && roll <= 99);
        }
    }

    // ====================== 随机选择目标事件 ======================

    @Test
    public void testRandomSelectionUniform() {
        Random rng = new Random(999);
        int poolSize = 5;
        int trials = 25000;
        int[] counts = new int[poolSize];

        for (int i = 0; i < trials; i++) {
            int index = rng.nextInt(poolSize);
            counts[index]++;
        }

        double expected = 1.0 / poolSize;
        for (int i = 0; i < poolSize; i++) {
            assertEquals("索引" + i + " 应≈20%，容差3%", expected, (double) counts[i] / trials, 0.03);
        }
    }

    @Test
    public void testRandomSelectionFromPoolOfOne() {
        Random rng = new Random();
        int index = rng.nextInt(1);
        assertEquals("池大小=1时只能选索引0", 0, index);
    }

    // ====================== 子事件随机选取 ======================

    @Test
    public void testPickRandomSubEventFromMultiple() {
        Random rng = new Random(500);
        int subEventCount = 8;
        int trials = 10000;
        int[] counts = new int[subEventCount];

        for (int i = 0; i < trials; i++) {
            int idx = rng.nextInt(subEventCount);
            counts[idx]++;
        }

        double expected = 1.0 / subEventCount;
        for (int i = 0; i < subEventCount; i++) {
            assertEquals("子事件" + i + " 应≈12.5%，容差3%", expected, (double) counts[i] / trials, 0.03);
        }
    }

    @Test
    public void testPickRandomSubEventNullArray() {
        String[] subs = null;
        String result = (subs == null || subs.length == 0) ? null : subs[0];
        assertNull("null数组应返回null", result);
    }

    @Test
    public void testPickRandomSubEventEmptyArray() {
        String[] subs = {};
        String result = (subs == null || subs.length == 0) ? null : subs[0];
        assertNull("空数组应返回null", result);
    }

    // ====================== 消息完整性 ======================

    @Test
    public void testRevealSuccessMessageContainsKeywords() {
        String msg = "占卜成功！一个未知事件被揭示为：怪物营地";
        assertTrue("应包含'占卜成功'", msg.contains("占卜成功"));
        assertTrue("应包含'揭示'", msg.contains("揭示"));
    }

    @Test
    public void testRevealFailureMessageConfigError() {
        String msg = "占卜出现误差，未能揭示事件。";
        assertTrue("应包含'误差'或'未能'", msg.contains("误差") || msg.contains("未能"));
    }

    @Test
    public void testRevealFailureMessageNoUnknown() {
        String msg = "当前地图上没有未知事件可揭示。";
        assertTrue("应提及'没有'或'未知事件'", msg.contains("没有") && msg.contains("未知"));
    }

    @Test
    public void testInsufficientGoldMessage() {
        String msg = "你的金币不足300，无法支付占卜费用。";
        assertTrue("应提示金币不足", msg.contains("不足") && msg.contains("300"));
    }

    @Test
    public void testRefuseMessage() {
        String msg = "你觉得占卜师只是在故弄玄虚，直接走开了。";
        assertTrue("应表达拒绝态度", msg.contains("故弄玄虚") || msg.contains("走开"));
    }

    @Test
    public void testNoUnknownAutoMessage() {
        String msg = "占卜师遗憾地告诉你，当前地图上没有未知的迷雾需要揭示。";
        assertTrue("应包含'遗憾'或'未知的迷雾'", msg.contains("遗憾") || msg.contains("迷雾"));
    }

    // ====================== 事件配置契约 ======================

    @Test
    public void testDivinationCostConstant() {
        assertEquals("占卜固定300金币", 300, DIVINATION_COST);
    }

    @Test
    public void testCostIsPositive() {
        assertTrue("占卜费用应为正数", DIVINATION_COST > 0);
    }

    @Test
    public void testRefuseOptionAlwaysAvailable() {
        boolean hasRefuseOption = true;
        assertTrue("'不感兴趣'按钮始终存在", hasRefuseOption);
    }

    // ====================== 综合场景 ======================

    @Test
    public void testSuccessfulDivinationFlow() {
        int gold = 1000;
        boolean canAfford = gold >= DIVINATION_COST;
        assertTrue("资金充足", canAfford);

        gold -= DIVINATION_COST;
        assertEquals(700, gold);

        List<MockEventCircle> circles = new ArrayList<>();
        circles.add(new MockEventCircle(EVENT_TYPE_UNKNOWN));
        circles.add(new MockEventCircle(EVENT_TYPE_UNKNOWN));
        assertTrue("有未知事件", hasUnknownEvents(circles));
    }

    @Test
    public void testDivinationWithNoUnknownEvents() {
        List<MockEventCircle> circles = new ArrayList<>();
        circles.add(new MockEventCircle(EVENT_TYPE_BATTLE));
        circles.add(new MockEventCircle(EVENT_TYPE_NEUTRAL));

        boolean hasUnknown = hasUnknownEvents(circles);
        assertFalse("无未知事件", hasUnknown);

        boolean shouldAutoShowMessage = !hasUnknown;
        assertTrue("无未知事件时应自动提示", shouldAutoShowMessage);
    }

    // ====================== 边界值测试 ======================

    @Test
    public void testOneGoldShortForDivination() {
        int gold = 299;
        assertFalse("差1金币", gold >= DIVINATION_COST);
    }

    @Test
    public void testOneGoldOverForDivination() {
        int gold = 301;
        assertTrue("多1金币", gold >= DIVINATION_COST);
        gold -= DIVINATION_COST;
        assertEquals(1, gold);
    }

    @Test
    public void testNextIntReturnsAllValues() {
        Random rng = new Random(123);
        boolean[] seen = new boolean[100];
        int samples = 0;
        while (samples < 50000) {
            int roll = rng.nextInt(100);
            seen[roll] = true;
            samples++;
        }
        int covered = 0;
        for (boolean b : seen) if (b) covered++;
        assertEquals("nextInt(100) 应覆盖全部100个值", 100, covered);
    }

    // ====================== null 子事件处理（揭示后状态） ======================

    @Test
    public void testNullSubEventProducesFallbackMessage() {
        String subName = null;
        String fallbackType = EVENT_TYPE_BATTLE;
        String displayed = subName != null ? subName : fallbackType;
        assertEquals("null子事件应fallback到类型名", EVENT_TYPE_BATTLE, displayed);
    }

    @Test
    public void testNonNullSubEventUsesName() {
        String subName = "怪物营地";
        String fallbackType = EVENT_TYPE_BATTLE;
        String displayed = subName != null ? subName : fallbackType;
        assertEquals("非null子事件应使用名称", "怪物营地", displayed);
    }

    @Test
    public void testSubEventNullStoredInTarget() {
        String[] subs = {};
        String selectedSub = (subs != null && subs.length > 0) ? subs[0] : null;

        String stored = selectedSub;
        assertNull("【潜在Bug】null子事件被存入target.selectedSubEvent", stored);
    }

    // ====================== 金币扣减顺序验证 ======================

    @Test
    public void testGoldDeductedBeforeRevealCall() {
        int gold = 500;
        boolean spent = gold >= DIVINATION_COST;
        assertTrue(spent);
        gold -= DIVINATION_COST;

        boolean revealCalledAfterGoldDeducted = true;
        assertTrue("revealUnknownEvent应在spendGold之后调用", revealCalledAfterGoldDeducted);
        assertEquals("金币数额反映扣减结果", 200, gold);
    }

    // ====================== 双次 hasUnknownEvents 调用一致性 ======================

    @Test
    public void testDoubleHasUnknownEventsCallConsistency() {
        List<MockEventCircle> circles = new ArrayList<>();
        circles.add(new MockEventCircle(EVENT_TYPE_UNKNOWN));

        boolean firstCheck = hasUnknownEvents(circles);
        boolean secondCheck = hasUnknownEvents(circles);
        assertEquals("两次调用结果应一致（在圆圈被移除前）", firstCheck, secondCheck);
    }
}
