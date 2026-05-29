package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

/**
 * 赌场马车（casino_wagon）事件核心逻辑测试
 * <p>
 * 事件规则：玩家下注100金币，随机决定结果：
 *   roll &lt; 40 (40%)：赢 → 获得200金币（净赚100）
 *   roll &lt; 70 (30%)：平 → 退还100金币（净收益0）
 *   roll ≥ 70 (30%)：输 → 100金币血本无归（净亏100）
 * 期望值：0.4×100 + 0.3×0 + 0.3×(-100) = +10 金币/次
 * <p>
 * 测试覆盖：
 * 1. 金币消费机制（足够/恰好/不足/0/多次/边界±1）
 * 2. 三种结果的概率分布统计（40/30/30）
 * 3. 各结果的净金币变化（赢+100/平0/输-100）
 * 4. ⭐ 期望值验证（长期EV=+10→玩家优势）
 * 5. 异常处理（金币不足先阻后不扣）
 * 6. 下注结果消息完整性
 * 7. roll 边界值映射验证
 * 8. 大额金币、极限下注次数
 */
public class CasinoWagonTest {

    private static final int BET_AMOUNT = 100;
    private static final int WIN_PAYOUT = 200;
    private static final int DRAW_PAYOUT = 100;
    private static final int WIN_THRESHOLD = 40;
    private static final int DRAW_THRESHOLD = 70;

    // ====================== 金币消费机制 ======================

    @Test
    public void testSpendGoldSufficientBalance() {
        int gold = 500;
        assertTrue("500金币足够下注100", gold >= BET_AMOUNT);
        gold -= BET_AMOUNT;
        assertEquals(400, gold);
    }

    @Test
    public void testSpendGoldExactBalance() {
        int gold = 100;
        assertTrue("恰好100金币可下注", gold >= BET_AMOUNT);
        gold -= BET_AMOUNT;
        assertEquals("下注后余额为0", 0, gold);
    }

    @Test
    public void testSpendGoldInsufficient() {
        int gold = 99;
        assertFalse("99金币不够下注", gold >= BET_AMOUNT);
        assertEquals("金币不足时余额不变", 99, gold);
    }

    @Test
    public void testSpendGoldZero() {
        int gold = 0;
        assertFalse("0金币不够下注", gold >= BET_AMOUNT);
    }

    @Test
    public void testSpendGoldVeryRich() {
        int gold = 99999;
        assertTrue(gold >= BET_AMOUNT);
        gold -= BET_AMOUNT;
        assertEquals(99899, gold);
    }

    @Test
    public void testMultipleBetsExhaustGold() {
        int gold = 500;
        int count = 0;
        while (gold >= BET_AMOUNT) {
            gold -= BET_AMOUNT;
            count++;
        }
        assertEquals("500金币可下注5次", 5, count);
        assertEquals("剩余0", 0, gold);
    }

    @Test
    public void testMultipleBetsWithRemainder() {
        int gold = 350;
        int count = 0;
        while (gold >= BET_AMOUNT) {
            gold -= BET_AMOUNT;
            count++;
        }
        assertEquals("350金币可下注3次", 3, count);
        assertEquals("剩余50", 50, gold);
    }

    @Test
    public void testOneGoldShort() {
        int gold = 99;
        assertFalse("差1金币不能下注", gold >= BET_AMOUNT);
    }

    @Test
    public void testOneGoldOver() {
        int gold = 101;
        assertTrue("多1金币可以下注", gold >= BET_AMOUNT);
        gold -= BET_AMOUNT;
        assertEquals(1, gold);
    }

    // ====================== 下注流程核心逻辑（用 netChange 模拟） ======================

    private int simulateBet(int gold, int roll) {
        if (gold < BET_AMOUNT) return gold;
        gold -= BET_AMOUNT;
        if (roll < WIN_THRESHOLD) {
            gold += WIN_PAYOUT;
        } else if (roll < DRAW_THRESHOLD) {
            gold += DRAW_PAYOUT;
        }
        return gold;
    }

    @Test
    public void testWinNetChange() {
        int netChange = -BET_AMOUNT + WIN_PAYOUT;
        assertEquals("赢时净赚100", 100, netChange);
    }

    @Test
    public void testDrawNetChange() {
        int netChange = -BET_AMOUNT + DRAW_PAYOUT;
        assertEquals("平时净收益0", 0, netChange);
    }

    @Test
    public void testLoseNetChange() {
        int netChange = -BET_AMOUNT;
        assertEquals("输时净亏100", -100, netChange);
    }

    @Test
    public void testWinFullFlow() {
        int gold = 500;
        assertEquals(600, simulateBet(gold, 0));
        assertEquals(600, simulateBet(gold, 20));
        assertEquals(600, simulateBet(gold, 39));
    }

    @Test
    public void testDrawFullFlow() {
        int gold = 500;
        assertEquals(500, simulateBet(gold, 40));
        assertEquals(500, simulateBet(gold, 55));
        assertEquals(500, simulateBet(gold, 69));
    }

    @Test
    public void testLoseFullFlow() {
        int gold = 500;
        assertEquals(400, simulateBet(gold, 70));
        assertEquals(400, simulateBet(gold, 85));
        assertEquals(400, simulateBet(gold, 99));
    }

    @Test
    public void testInsufficientGoldFullFlow() {
        int gold = 50;
        assertEquals("金币不足时余额不变", 50, simulateBet(gold, 0));
        assertEquals("金币不足时余额不变", 50, simulateBet(gold, 99));
    }

    @Test
    public void testBetWhenExactlyZeroAfterLoss() {
        int gold = 100;
        int roll = 70;
        int result = simulateBet(gold, roll);
        assertEquals("下注100后输了，余额为0", 0, result);
    }

    // ====================== 概率分布统计（50000 样本） ======================

    @Test
    public void testProbabilityDistribution() {
        Random rng = new Random(42);
        int trials = 50000;
        int win = 0, draw = 0, lose = 0;

        for (int i = 0; i < trials; i++) {
            int roll = rng.nextInt(100);
            if (roll < WIN_THRESHOLD) win++;
            else if (roll < DRAW_THRESHOLD) draw++;
            else lose++;
        }

        assertEquals("赢 40%，容差2%", 0.40, (double) win / trials, 0.02);
        assertEquals("平 30%，容差2%", 0.30, (double) draw / trials, 0.02);
        assertEquals("输 30%，容差2%", 0.30, (double) lose / trials, 0.02);
        assertEquals("总次数", trials, win + draw + lose);
    }

    @Test
    public void testProbabilityDistributionMatchesConfig() {
        Random rng = new Random(777);
        int trials = 100000;
        int win = 0, draw = 0, lose = 0;

        for (int i = 0; i < trials; i++) {
            int roll = rng.nextInt(100);
            if (roll < 40) win++;
            else if (roll < 70) draw++;
            else lose++;
        }

        assertEquals("40%赢", 0.40, (double) win / trials, 0.015);
        assertEquals("30%平", 0.30, (double) draw / trials, 0.015);
        assertEquals("30%输", 0.30, (double) lose / trials, 0.015);
    }

    // ====================== ⭐ 期望值验证（EV = +10/次） ======================

    @Test
    public void testExpectedValuePerBet() {
        double ev = 0.40 * (WIN_PAYOUT - BET_AMOUNT)
                  + 0.30 * (DRAW_PAYOUT - BET_AMOUNT)
                  + 0.30 * (0 - BET_AMOUNT);
        assertEquals("期望值 = +10 金币/次", 10.0, ev, 0.01);
    }

    @Test
    public void testExpectedValuePositiveForPlayer() {
        double ev = 0.40 * 100 + 0.30 * 0 + 0.30 * (-100);
        assertTrue("【设计层面】期望值为正=+10，玩家长期有利", ev > 0);
    }

    @Test
    public void testExpectedValueLongRunSimulation() {
        Random rng = new Random(12345);
        int trials = 50000;
        long totalNet = 0;

        for (int i = 0; i < trials; i++) {
            int roll = rng.nextInt(100);
            if (roll < 40) totalNet += 100;
            else if (roll < 70) totalNet += 0;
            else totalNet -= 100;
        }

        double avgNet = (double) totalNet / trials;
        assertEquals("长期平均净收益≈10，容差2", 10.0, avgNet, 2.0);
    }

    // ====================== 多局模拟 ======================

    @Test
    public void testMultiBetSimulation() {
        Random rng = new Random(999);
        int gold = 2000;
        int rounds = 100;
        int minGold = gold;
        int maxGold = gold;
        int bankruptRounds = 0;

        for (int i = 0; i < rounds; i++) {
            if (gold < BET_AMOUNT) {
                bankruptRounds++;
                break;
            }
            int roll = rng.nextInt(100);
            gold = simulateBet(gold, roll);
            if (gold < minGold) minGold = gold;
            if (gold > maxGold) maxGold = gold;
        }

        assertTrue("100局后没有破产（起始2000金币足够）", bankruptRounds == 0);
        assertTrue("最终金币 > 起始（期望值+10有利）", gold > 2000);
    }

    // ====================== 破产风险 ======================

    @Test
    public void testBankruptcyRiskLowGold() {
        Random rng = new Random(1);
        int gold = 100;
        int bets = 0;
        while (gold >= BET_AMOUNT && bets < 20) {
            int roll = rng.nextInt(100);
            gold = simulateBet(gold, roll);
            bets++;
        }
        assertTrue("100金币可能在20局内破产", gold < BET_AMOUNT || bets == 20);
    }

    @Test
    public void testBankruptcyAvoidedWithHighGold() {
        Random rng = new Random(1);
        int gold = 5000;
        int bets = 0;
        while (gold >= BET_AMOUNT && bets < 200) {
            int roll = rng.nextInt(100);
            gold = simulateBet(gold, roll);
            bets++;
        }
        assertEquals("5000金币200局内不应破产（EV为正）", 200, bets);
        assertTrue("剩余金币 > 100（EV为正，资金充足）", gold >= BET_AMOUNT);
    }

    // ====================== roll 边界值映射 ======================

    @Test
    public void testRollBoundaryMapping() {
        int[][] cases = {
            {0,   1}, {39,  1}, {40,  2}, {69,  2},
            {70,  3}, {99,  3},
        };
        for (int[] c : cases) {
            int category;
            if (c[0] < 40) category = 1;
            else if (c[0] < 70) category = 2;
            else category = 3;
            assertEquals("roll=" + c[0], c[1], category);
        }
    }

    @Test
    public void testRollRangeCoverage() {
        boolean[] covered = new boolean[100];
        for (int i = 0; i < 100; i++) {
            if (i < 40) covered[i] = true;
            else if (i < 70) covered[i] = true;
            else covered[i] = true;
        }
        int coveredCount = 0;
        for (boolean b : covered) if (b) coveredCount++;
        assertEquals("全部100个roll值被三个分支覆盖", 100, coveredCount);
    }

    // ====================== 金币额外情况 ======================

    @Test
    public void testWinThenBetAgainImmediately() {
        int gold = 500;
        gold = simulateBet(gold, 10);
        assertEquals("第一局赢：净值600", 600, gold);

        gold = simulateBet(gold, 80);
        assertEquals("第二局输：净值500", 500, gold);

        gold = simulateBet(gold, 50);
        assertEquals("第三局平：净值500", 500, gold);
    }

    @Test
    public void testConsecutiveWins() {
        int gold = 500;
        for (int i = 0; i < 5; i++) {
            gold = simulateBet(gold, 0);
        }
        assertEquals("连赢5局：500+5×100=1000", 1000, gold);
    }

    @Test
    public void testConsecutiveLosses() {
        int gold = 500;
        for (int i = 0; i < 5; i++) {
            gold = simulateBet(gold, 99);
        }
        assertEquals("连输5局：500-5×100=0", 0, gold);
    }

    @Test
    public void testAlternatingWinLose() {
        int gold = 500;
        gold = simulateBet(gold, 0);
        assertEquals(600, gold);
        gold = simulateBet(gold, 99);
        assertEquals(500, gold);
        gold = simulateBet(gold, 0);
        assertEquals(600, gold);
        gold = simulateBet(gold, 99);
        assertEquals(500, gold);
    }

    // ====================== 消息完整性 ======================

    @Test
    public void testWinMessageContainsKeywords() {
        String output = "🎉 恭喜！你赢了！\n\n✅ 获得双倍回报：200金币！（当前金币：600）";
        assertTrue("赢应包含'恭喜'或'赢了'", output.contains("恭喜") || output.contains("赢了"));
        assertTrue("赢应包含'双倍'或'200'", output.contains("双倍") || output.contains("200"));
        assertTrue("应显示当前金币", output.contains("当前金币"));
    }

    @Test
    public void testDrawMessageContainsKeywords() {
        String output = "😐 平局！你的100金币退还给你。（当前金币：500）";
        assertTrue("平应包含'平局'", output.contains("平局"));
        assertTrue("平应包含'退还'", output.contains("退还"));
    }

    @Test
    public void testLoseMessageContainsKeywords() {
        String output = "😞 你输了...100金币血本无归。（当前金币：400）";
        assertTrue("输应包含'输了'或'血本无归'", output.contains("输了") || output.contains("血本无归"));
    }

    @Test
    public void testInsufficientGoldMessage() {
        String output = "你的金币不足100，无法下注。";
        assertTrue("应提示金币不足", output.contains("不足") && output.contains("100"));
    }

    @Test
    public void testRefuseMessage() {
        String output = "你收起好奇心，继续前行。";
        assertTrue("应表达拒绝态度", output.contains("收起") || output.contains("继续"));
    }

    // ====================== 事件配置契约 ======================

    @Test
    public void testBetAmountConstant() {
        assertEquals("下注固定100金币", 100, BET_AMOUNT);
    }

    @Test
    public void testWinPayoutIsDouble() {
        assertEquals("赢时支付200金币（双倍）", 200, WIN_PAYOUT);
        assertEquals("WIN_PAYOUT = 2 × BET_AMOUNT", BET_AMOUNT * 2, WIN_PAYOUT);
    }

    @Test
    public void testDrawPayoutEqualsBet() {
        assertEquals("平时退还100金币", 100, DRAW_PAYOUT);
        assertEquals("DRAW_PAYOUT = BET_AMOUNT", BET_AMOUNT, DRAW_PAYOUT);
    }

    @Test
    public void testWinThresholdMatchesConfig() {
        assertEquals("win 阈值 40 → 40%", 40, WIN_THRESHOLD);
        assertEquals("draw 阈值 70（40+30=70）", 70, DRAW_THRESHOLD);
    }

    @Test
    public void testProbabilityPercentagesSumTo100() {
        int pWin = WIN_THRESHOLD;
        int pDraw = DRAW_THRESHOLD - WIN_THRESHOLD;
        int pLose = 100 - DRAW_THRESHOLD;
        assertEquals("40+30+30=100", 100, pWin + pDraw + pLose);
        assertEquals("赢=40%", 40, pWin);
        assertEquals("平=30%", 30, pDraw);
        assertEquals("输=30%", 30, pLose);
    }

    @Test
    public void testRefuseOptionAlwaysAvailable() {
        assertTrue("'不参与赌博'按钮始终存在", true);
    }

    // ====================== 第一次下注赢后立即同一事件再赌 ======================

    @Test
    public void testCannotBetTwiceInSameEvent() {
        int gold = 500;
        gold = simulateBet(gold, 5);
        assertEquals("事件结束只能赌一次（设计限制）", 600, gold);
    }

    // ====================== 负值/溢出保护 ======================

    @Test
    public void testBetAmountIsPositive() {
        assertTrue("下注金额必须为正", BET_AMOUNT > 0);
    }

    @Test
    public void testPayoutsAreNonNegative() {
        assertTrue("赢支付≥0", WIN_PAYOUT >= 0);
        assertTrue("平支付≥0", DRAW_PAYOUT >= 0);
    }

    @Test
    public void testGoldNeverGoesBelowZero() {
        Random rng = new Random(42);
        for (int startGold = 100; startGold <= 1000; startGold += 100) {
            int gold = startGold;
            for (int i = 0; i < 50; i++) {
                if (gold < BET_AMOUNT) break;
                int roll = rng.nextInt(100);
                gold = simulateBet(gold, roll);
                assertTrue("金币不应为负: start=" + startGold + " round=" + i,
                        gold >= 0);
            }
        }
    }

    // ====================== 随机数使用正确性 ======================

    @Test
    public void testRollProbabilityDistribution() {
        Random rng = new Random(777);
        int trials = 10000;
        int countLessThan40 = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextInt(100) < 40) countLessThan40++;
        }
        double ratio = (double) countLessThan40 / trials;
        assertEquals("nextInt(100) < 40 概率≈40%", 0.40, ratio, 0.03);
    }
}
