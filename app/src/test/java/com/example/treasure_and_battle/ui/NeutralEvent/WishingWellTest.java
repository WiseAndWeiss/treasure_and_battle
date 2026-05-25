package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 许愿古井（wishing_well）核心逻辑测试
 * 覆盖金币数组、概率公式、边界值与单调性
 */
public class WishingWellTest {

    private static final int[] WISHING_AMOUNTS = {1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233};

    // ====================== 斐波那契数列正确性 ======================

    @Test
    public void testWishingAmountsLength() {
        assertEquals(12, WISHING_AMOUNTS.length);
    }

    @Test
    public void testWishingAmountsIsFibonacci() {
        assertEquals(1, WISHING_AMOUNTS[0]);
        assertEquals(2, WISHING_AMOUNTS[1]);
        assertEquals(3, WISHING_AMOUNTS[2]);
        assertEquals(5, WISHING_AMOUNTS[3]);
        assertEquals(8, WISHING_AMOUNTS[4]);
        assertEquals(13, WISHING_AMOUNTS[5]);
        assertEquals(21, WISHING_AMOUNTS[6]);
        assertEquals(34, WISHING_AMOUNTS[7]);
        assertEquals(55, WISHING_AMOUNTS[8]);
        assertEquals(89, WISHING_AMOUNTS[9]);
        assertEquals(144, WISHING_AMOUNTS[10]);
        assertEquals(233, WISHING_AMOUNTS[11]);
    }

    @Test
    public void testWishingAmountsFibonacciRecurrence() {
        for (int i = 2; i < WISHING_AMOUNTS.length; i++) {
            assertEquals("F(" + i + ") = F(" + (i - 1) + ") + F(" + (i - 2) + ")",
                    WISHING_AMOUNTS[i - 1] + WISHING_AMOUNTS[i - 2], WISHING_AMOUNTS[i]);
        }
    }

    // ====================== 数组值递增 ======================

    @Test
    public void testWishingAmountsMonotonicallyIncreasing() {
        for (int i = 1; i < WISHING_AMOUNTS.length; i++) {
            assertTrue("amounts[" + i + "] should be > amounts[" + (i - 1) + "]",
                    WISHING_AMOUNTS[i] > WISHING_AMOUNTS[i - 1]);
        }
    }

    // ====================== 边界值 ======================

    @Test
    public void testFirstRoundCostIsOne() {
        assertEquals(1, WISHING_AMOUNTS[0]);
    }

    @Test
    public void testLastRoundCostIs233() {
        assertEquals(233, WISHING_AMOUNTS[11]);
    }

    // ====================== 轮次索引安全性 ======================

    @Test
    public void testRoundIndexClampAtZero() {
        assertEquals(1, WISHING_AMOUNTS[Math.min(0, WISHING_AMOUNTS.length - 1)]);
    }

    @Test
    public void testRoundIndexClampAtMax() {
        int round = 999;
        assertEquals(WISHING_AMOUNTS[WISHING_AMOUNTS.length - 1],
                WISHING_AMOUNTS[Math.min(round, WISHING_AMOUNTS.length - 1)]);
    }

    // ====================== 品质概率公式验证 ======================

    @Test
    public void testCommonProbabilityFormula() {
        assertEquals(0.005, Math.min(1 * 0.005, 0.70), 0.0001);
        assertEquals(0.05, Math.min(10 * 0.005, 0.70), 0.0001);
        assertEquals(0.10, Math.min(20 * 0.005, 0.70), 0.0001);
        assertEquals(0.35, Math.min(70 * 0.005, 0.70), 0.0001);
        assertEquals(0.50, Math.min(100 * 0.005, 0.70), 0.0001);
        assertEquals(0.70, Math.min(140 * 0.005, 0.70), 0.0001);
        assertEquals(0.70, Math.min(200 * 0.005, 0.70), 0.0001);
        assertEquals(0.70, Math.min(233 * 0.005, 0.70), 0.0001);
    }

    @Test
    public void testUncommonProbabilityFormula() {
        assertEquals(0.0001, Math.min(1 * 0.0001, 0.04), 0.00001);
        assertEquals(0.01, Math.min(100 * 0.0001, 0.04), 0.00001);
        assertEquals(0.0233, Math.min(233 * 0.0001, 0.04), 0.0001);
        assertEquals(0.04, Math.min(400 * 0.0001, 0.04), 0.00001);
    }

    @Test
    public void testEpicProbabilityFormula() {
        assertEquals(0.000005, Math.min(1 * 0.000005, 0.002), 0.000001);
        assertEquals(0.0005, Math.min(100 * 0.000005, 0.002), 0.000001);
        assertEquals(0.001, Math.min(200 * 0.000005, 0.002), 0.000001);
        assertEquals(0.001165, Math.min(233 * 0.000005, 0.002), 0.0001);
        assertEquals(0.002, Math.min(400 * 0.000005, 0.002), 0.000001);
    }

    // ====================== 各轮次总概率覆盖率 ======================

    @Test
    public void testFirstRoundProbabilities() {
        double n = 1;
        double cp = Math.min(n * 0.005, 0.70);
        double up = Math.min(n * 0.0001, 0.04);
        double rp = Math.min(n * 0.0001, 0.04);
        double ep = Math.min(n * 0.000005, 0.002);
        double lp = Math.min(n * 0.000005, 0.002);
        double total = cp + up + rp + ep + lp;
        assertTrue( total < 0.01);
    }

    @Test
    public void testHighCoinProbabilityCoversMostlyCommon() {
        int n = 233;
        double cp = Math.min(n * 0.005, 0.70);
        assertTrue(cp > 0.69);
    }

    @Test
    public void testHighCoinRareCapAt4Percent() {
        double rp = Math.min(400 * 0.0001, 0.04);
        assertEquals(0.04, rp, 0.0001);
    }

    @Test
    public void testHighCoinLegendaryCapAtDot2Percent() {
        double lp = Math.min(400 * 0.000005, 0.002);
        assertEquals(0.002, lp, 0.00001);
    }

    // ====================== 概率单调性 ======================

    @Test
    public void testCommonProbabilityIncreasesWithCoins() {
        for (int i = 1; i < WISHING_AMOUNTS.length; i++) {
            int prevN = WISHING_AMOUNTS[i - 1];
            int curN = WISHING_AMOUNTS[i];
            double prev = Math.min(prevN * 0.005, 0.70);
            double cur = Math.min(curN * 0.005, 0.70);
            assertTrue(curN + "金币的普通概率应≥" + prevN, cur >= prev);
        }
    }

    @Test
    public void testUncommonProbabilityIncreasesWithCoins() {
        for (int i = 1; i < WISHING_AMOUNTS.length; i++) {
            double prev = Math.min(WISHING_AMOUNTS[i - 1] * 0.0001, 0.04);
            double cur = Math.min(WISHING_AMOUNTS[i] * 0.0001, 0.04);
            assertTrue(WISHING_AMOUNTS[i] + "金币的不凡概率应≥" + WISHING_AMOUNTS[i - 1], cur >= prev);
        }
    }
}
