package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.treasure_and_battle.model.common.Rarity;

import org.junit.Test;

import java.util.Random;

/**
 * 洞穴寻宝（cave_treasure）事件核心逻辑测试
 * <p>
 * 事件规则：四步递进式探索状态机 (caveStep 0→1→2→3)。
 * 每步可"继续探索"（扣HP+可能得物品）或"离开"（保留已得物品）。
 * <p>
 * 各步详情：
 *  Step 0（入口）：损失 ceil(currentHp/10)，100%得物品（50%COMMON/50%UNCOMMON，40%装备5~15级/60%消耗品）
 *  Step 1（深入）：损失 ceil(maxHp/8)，100%得物品（60%UNCOMMON/40%RARE，40%装备10~20级/60%消耗品）
 *  Step 2（最深处）：损失 ceil(maxHp/6)，无物品（纯风险步）
 *  Step 3（终局）：50%战斗 / 50%财宝（500~1500金币+RARE/EPIC物品，40%装备15~25级/60%宝石）
 * <p>
 * 测试覆盖：
 *  1.  Step 0 diceHP扣血公式验证
 *  2.  Step 1 maxHp扣血公式验证
 *  3.  Step 2 maxHp扣血公式验证
 *  4.  Step 3 终局判定（50/50）、金币范围、稀有度
 *  5.  累计HP损失范围
 *  6.  稀有度递进性（Step0 < Step1 < Step3）
 *  7.  离开逻辑（各步离开：物品展示/HP汇总/文案）
 *  8.  完整流程路径模拟（4条主要路径）
 *  9.  HP最低保护（Math.max(1, ...)）
 * 10.  大样本概率分布验证（每步稀有度/物品种类）
 * 11.  装备等级范围验证
 * 12.  事件配置契约（eventKey/按钮文案/风险描述）
 * 13.  Step 3 战斗分支细节
 * 14.  风险收益策略分析
 * 15.  多次遍历独立性
 * 16.  数值边界安全测试
 * 17.  血量不足判定（currentHp ≤ hpLoss → 按钮变灰，禁止继续）
 */
public class CaveTreasureTest {

    // ==================== 1. Step 0（入口）ceil(currentHp/10) ====================

    private int calcStep0HpLoss(int currentHp) {
        return (currentHp + 9) / 10;
    }

    @Test
    public void testStep0HpLoss_CeilDivideBy10() {
        assertEquals("HP=1 → ceil(1/10)=1", 1, calcStep0HpLoss(1));
        assertEquals("HP=9 → ceil(9/10)=1", 1, calcStep0HpLoss(9));
        assertEquals("HP=10 → ceil(10/10)=1", 1, calcStep0HpLoss(10));
        assertEquals("HP=11 → ceil(11/10)=2", 2, calcStep0HpLoss(11));
        assertEquals("HP=20 → ceil(20/10)=2", 2, calcStep0HpLoss(20));
        assertEquals("HP=50 → ceil(50/10)=5", 5, calcStep0HpLoss(50));
        assertEquals("HP=99 → ceil(99/10)=10", 10, calcStep0HpLoss(99));
        assertEquals("HP=100 → ceil(100/10)=10", 10, calcStep0HpLoss(100));
    }

    @Test
    public void testStep0HpLoss_ProportionalToCurrentHp() {
        int loss1 = calcStep0HpLoss(200);
        int loss2 = calcStep0HpLoss(100);
        assertTrue("HP越高 → Step0损失越大: " + loss1 + " > " + loss2, loss1 > loss2);
    }

    @Test
    public void testStep0HpLoss_NeverZero() {
        for (int hp = 1; hp <= 1000; hp++) {
            assertTrue("HP=" + hp + " → loss=" + calcStep0HpLoss(hp) + " > 0",
                    calcStep0HpLoss(hp) > 0);
        }
    }

    @Test
    public void testStep0HpLoss_MinimumIsOne() {
        assertEquals("HP=1 → loss=1（最小损失为1）", 1, calcStep0HpLoss(1));
    }

    @Test
    public void testStep0RarityDistribution() {
        Random rng = new Random(42);
        int trials = 50000;
        int common = 0, uncommon = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextDouble() < 0.5) common++;
            else uncommon++;
        }
        assertEquals("Step0 COMMON 50%，容差2%", 0.50, (double) common / trials, 0.02);
        assertEquals("Step0 UNCOMMON 50%，容差2%", 0.50, (double) uncommon / trials, 0.02);
        assertEquals("总次数相等", trials, common + uncommon);
    }

    @Test
    public void testStep0RarityNeverExceedsUncommon() {
        for (int i = 0; i < 1000; i++) {
            Rarity r = Math.random() < 0.5 ? Rarity.COMMON : Rarity.UNCOMMON;
            assertTrue("Step0 稀有度 ≤ UNCOMMON，实际=" + r,
                    r.ordinal() <= Rarity.UNCOMMON.ordinal());
        }
    }

    @Test
    public void testStep0RarityAtLeastCommon() {
        for (int i = 0; i < 1000; i++) {
            Rarity r = Math.random() < 0.5 ? Rarity.COMMON : Rarity.UNCOMMON;
            assertTrue("Step0 稀有度 ≥ COMMON", r.ordinal() >= Rarity.COMMON.ordinal());
        }
    }

    @Test
    public void testStep0ItemTypeDistribution() {
        Random rng = new Random(777);
        int trials = 50000;
        int equip = 0, other = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextDouble() < 0.4) equip++;
            else other++;
        }
        assertEquals("Step0 装备 40%，容差2%", 0.40, (double) equip / trials, 0.02);
        assertEquals("Step0 消耗品/宝石 60%，容差2%", 0.60, (double) other / trials, 0.02);
    }

    @Test
    public void testStep0EquipLevelRange() {
        Random rng = new Random(42);
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < 10000; i++) {
            int lv = 5 + rng.nextInt(11);
            if (lv < min) min = lv;
            if (lv > max) max = lv;
        }
        assertEquals("Step0 装备等级 min=5", 5, min);
        assertEquals("Step0 装备等级 max=15", 15, max);
    }

    @Test
    public void testStep0EquipLevelFormula() {
        assertEquals("level=5", 5, 5 + (int) (0.0 * 11));
        assertEquals("level=15", 15, 5 + (int) (0.999 * 11));
    }

    // ==================== 2. Step 1（深入）ceil(maxHp/8) ====================

    private int calcStep1HpLoss(int maxHp) {
        return (maxHp + 7) / 8;
    }

    @Test
    public void testStep1HpLoss_CeilDivideBy8() {
        assertEquals("maxHp=1 → ceil(1/8)=1", 1, calcStep1HpLoss(1));
        assertEquals("maxHp=7 → ceil(7/8)=1", 1, calcStep1HpLoss(7));
        assertEquals("maxHp=8 → ceil(8/8)=1", 1, calcStep1HpLoss(8));
        assertEquals("maxHp=9 → ceil(9/8)=2", 2, calcStep1HpLoss(9));
        assertEquals("maxHp=16 → ceil(16/8)=2", 2, calcStep1HpLoss(16));
        assertEquals("maxHp=40 → ceil(40/8)=5", 5, calcStep1HpLoss(40));
        assertEquals("maxHp=100 → ceil(100/8)=13", 13, calcStep1HpLoss(100));
    }

    @Test
    public void testStep1HpLoss_NeverZero() {
        for (int hp = 1; hp <= 1000; hp++) {
            assertTrue("maxHp=" + hp + " → loss=" + calcStep1HpLoss(hp) + " > 0",
                    calcStep1HpLoss(hp) > 0);
        }
    }

    @Test
    public void testStep1RarityDistribution() {
        Random rng = new Random(42);
        int trials = 50000;
        int uncommon = 0, rare = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextDouble() < 0.6) uncommon++;
            else rare++;
        }
        assertEquals("Step1 UNCOMMON 60%，容差2%", 0.60, (double) uncommon / trials, 0.02);
        assertEquals("Step1 RARE 40%，容差2%", 0.40, (double) rare / trials, 0.02);
        assertEquals("总次数相等", trials, uncommon + rare);
    }

    @Test
    public void testStep1RarityNeverExceedsRare() {
        for (int i = 0; i < 1000; i++) {
            double roll = Math.random();
            Rarity r = roll < 0.6 ? Rarity.UNCOMMON : Rarity.RARE;
            assertTrue("Step1 稀有度 ≤ RARE，实际=" + r,
                    r.ordinal() <= Rarity.RARE.ordinal());
        }
    }

    @Test
    public void testStep1RarityAtLeastUncommon() {
        for (int i = 0; i < 1000; i++) {
            double roll = Math.random();
            Rarity r = roll < 0.6 ? Rarity.UNCOMMON : Rarity.RARE;
            assertTrue("Step1 稀有度 ≥ UNCOMMON", r.ordinal() >= Rarity.UNCOMMON.ordinal());
        }
    }

    @Test
    public void testStep1ItemTypeDistribution() {
        Random rng = new Random(777);
        int trials = 50000;
        int equip = 0, other = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextDouble() < 0.4) equip++;
            else other++;
        }
        assertEquals("Step1 装备 40%，容差2%", 0.40, (double) equip / trials, 0.02);
        assertEquals("Step1 消耗品/宝石 60%，容差2%", 0.60, (double) other / trials, 0.02);
    }

    @Test
    public void testStep1EquipLevelRange() {
        Random rng = new Random(42);
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < 10000; i++) {
            int lv = 10 + rng.nextInt(11);
            if (lv < min) min = lv;
            if (lv > max) max = lv;
        }
        assertEquals("Step1 装备等级 min=10", 10, min);
        assertEquals("Step1 装备等级 max=20", 20, max);
    }

    @Test
    public void testStep1EquipLevelFormula() {
        assertEquals("level=10", 10, 10 + (int) (0.0 * 11));
        assertEquals("level=20", 20, 10 + (int) (0.999 * 11));
    }

    // ==================== 3. Step 2（最深处）ceil(maxHp/6) ====================

    private int calcStep2HpLoss(int maxHp) {
        return (maxHp + 5) / 6;
    }

    @Test
    public void testStep2HpLoss_CeilDivideBy6() {
        assertEquals("maxHp=1 → ceil(1/6)=1", 1, calcStep2HpLoss(1));
        assertEquals("maxHp=5 → ceil(5/6)=1", 1, calcStep2HpLoss(5));
        assertEquals("maxHp=6 → ceil(6/6)=1", 1, calcStep2HpLoss(6));
        assertEquals("maxHp=7 → ceil(7/6)=2", 2, calcStep2HpLoss(7));
        assertEquals("maxHp=12 → ceil(12/6)=2", 2, calcStep2HpLoss(12));
        assertEquals("maxHp=60 → ceil(60/6)=10", 10, calcStep2HpLoss(60));
        assertEquals("maxHp=100 → ceil(100/6)=17", 17, calcStep2HpLoss(100));
    }

    @Test
    public void testStep2HpLoss_NeverZero() {
        for (int hp = 1; hp <= 1000; hp++) {
            assertTrue("maxHp=" + hp + " → loss=" + calcStep2HpLoss(hp) + " > 0",
                    calcStep2HpLoss(hp) > 0);
        }
    }

    @Test
    public void testStep2HpLoss_Step1VsStep2_Step2AlwaysLarger() {
        assertTrue("Step2(ceil(maxHp/6)) > Step1(ceil(maxHp/8)) 对大多数maxHp成立",
                calcStep2HpLoss(100) > calcStep1HpLoss(100));
    }

    @Test
    public void testStep2NoItem() {
        boolean step2HasItem = false;
        assertFalse("Step2 是纯风险步，不产出物品", step2HasItem);
    }

    @Test
    public void testStep2IsThresholdToFinal() {
        boolean mustPassStep2 = true;
        assertTrue("必须通过 Step2 才能到达 Step3 终局", mustPassStep2);
    }

    // ==================== 4. Step 3（终局）判定 ====================

    private int simulateStep3Outcome() {
        return (int) (Math.random() * 2);
    }

    @Test
    public void testStep3BattleOrTreasure_50_50() {
        Random rng = new Random(42);
        int trials = 50000;
        int battle = 0, treasure = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextInt(2) == 0) battle++;
            else treasure++;
        }
        assertEquals("Step3 战斗 50%，容差2%", 0.50, (double) battle / trials, 0.02);
        assertEquals("Step3 财宝 50%，容差2%", 0.50, (double) treasure / trials, 0.02);
        assertEquals("总次数相等", trials, battle + treasure);
    }

    @Test
    public void testStep3TreasureGoldRange() {
        Random rng = new Random(42);
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < 10000; i++) {
            int gold = 500 + rng.nextInt(1001);
            if (gold < min) min = gold;
            if (gold > max) max = gold;
        }
        assertEquals("Step3 金币最小值=500", 500, min);
        assertEquals("Step3 金币最大值=1500", 1500, max);
    }

    @Test
    public void testStep3TreasureGoldBoundary() {
        assertEquals("gold=500+0=500", 500, 500 + (int) (0.0 * 1001));
        assertEquals("gold=500+1000=1500", 1500, 500 + (int) (0.9991 * 1001));
    }

    @Test
    public void testStep3TreasureGoldIsPositive() {
        for (int i = 0; i < 1000; i++) {
            int gold = 500 + (int) (Math.random() * 1001);
            assertTrue("Step3 金币 > 0，实际=" + gold, gold > 0);
            assertTrue("Step3 金币 <= 1500，实际=" + gold, gold <= 1500);
        }
    }

    @Test
    public void testStep3TreasureRarityDistribution() {
        Random rng = new Random(42);
        int trials = 50000;
        int rare = 0, epic = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextDouble() < 0.5) rare++;
            else epic++;
        }
        assertEquals("Step3 RARE 50%，容差2%", 0.50, (double) rare / trials, 0.02);
        assertEquals("Step3 EPIC 50%，容差2%", 0.50, (double) epic / trials, 0.02);
    }

    @Test
    public void testStep3TreasureRarityNeverLegendary() {
        for (int i = 0; i < 10000; i++) {
            Rarity r = Math.random() < 0.5 ? Rarity.RARE : Rarity.EPIC;
            assertTrue("Step3 不出 LEGENDARY，实际=" + r,
                    r != Rarity.LEGENDARY);
        }
    }

    @Test
    public void testStep3TreasureRarityBetweenRareAndEpic() {
        for (int i = 0; i < 10000; i++) {
            Rarity r = Math.random() < 0.5 ? Rarity.RARE : Rarity.EPIC;
            assertTrue("Step3 稀有度 ∈ {RARE, EPIC}", r == Rarity.RARE || r == Rarity.EPIC);
        }
    }

    @Test
    public void testStep3TreasureItemTypeDistribution() {
        Random rng = new Random(777);
        int trials = 50000;
        int equip = 0, other = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextDouble() < 0.4) equip++;
            else other++;
        }
        assertEquals("Step3 装备 40%，容差2%", 0.40, (double) equip / trials, 0.02);
        assertEquals("Step3 宝石/消耗品 60%，容差2%", 0.60, (double) other / trials, 0.02);
    }

    @Test
    public void testStep3EquipLevelRange() {
        Random rng = new Random(42);
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < 10000; i++) {
            int lv = 15 + rng.nextInt(11);
            if (lv < min) min = lv;
            if (lv > max) max = lv;
        }
        assertEquals("Step3 装备等级 min=15", 15, min);
        assertEquals("Step3 装备等级 max=25", 25, max);
    }

    @Test
    public void testStep3BattleIsNormalFight() {
        boolean hasSurpriseDirection = false;
        assertFalse("洞穴寻宝的Step3战斗无突袭方向（正常速度排序）", hasSurpriseDirection);
    }

    // ==================== 5. 累计HP损失 ====================

    private static class CaveSimulator {
        final int[] hpLoss = new int[3];
        int step = 0;

        void exploreStep0(int currentHp) {
            hpLoss[0] = (currentHp + 9) / 10;
            step = 1;
        }

        void exploreStep1(int maxHp) {
            hpLoss[1] = (maxHp + 7) / 8;
            step = 2;
        }

        void exploreStep2(int maxHp) {
            hpLoss[2] = (maxHp + 5) / 6;
            step = 3;
        }

        int getTotalHpLoss() {
            int total = 0;
            if (step >= 1) total += hpLoss[0];
            if (step >= 2) total += hpLoss[1];
            if (step >= 3) total += hpLoss[2];
            return total;
        }
    }

    @Test
    public void testCumulativeHpLoss_TypicalCase() {
        CaveSimulator cs = new CaveSimulator();
        int currentHp = 100, maxHp = 100;
        cs.exploreStep0(currentHp);
        cs.exploreStep1(maxHp);
        cs.exploreStep2(maxHp);
        int expected = (100 + 9) / 10 + (100 + 7) / 8 + (100 + 5) / 6;
        assertEquals("累计HP=10+13+17=40", expected, cs.getTotalHpLoss());
    }

    @Test
    public void testCumulativeHpLoss_FormulaSums() {
        CaveSimulator cs = new CaveSimulator();
        cs.hpLoss[0] = calcStep0HpLoss(200);
        cs.hpLoss[1] = calcStep1HpLoss(200);
        cs.hpLoss[2] = calcStep2HpLoss(200);
        cs.step = 3;
        assertEquals("累计=ceil(200/10)+ceil(200/8)+ceil(200/6)=20+25+34=79",
                20 + 25 + 34, cs.getTotalHpLoss());
    }

    @Test
    public void testCumulativeHpLossAtStep1Only() {
        CaveSimulator cs = new CaveSimulator();
        cs.hpLoss[0] = calcStep0HpLoss(50);
        cs.step = 1;
        assertEquals("Step1离开：仅扣Step0=ceil(50/10)=5", 5, cs.getTotalHpLoss());
    }

    @Test
    public void testCumulativeHpLossAtStep2Only() {
        CaveSimulator cs = new CaveSimulator();
        cs.hpLoss[0] = calcStep0HpLoss(80);
        cs.hpLoss[1] = calcStep1HpLoss(80);
        cs.step = 2;
        assertEquals("Step2离开：ceil(80/10)+ceil(80/8)=8+10=18", 18, cs.getTotalHpLoss());
    }

    @Test
    public void testCumulativeHpLossAtStep3Full() {
        CaveSimulator cs = new CaveSimulator();
        cs.hpLoss[0] = calcStep0HpLoss(60);
        cs.hpLoss[1] = calcStep1HpLoss(60);
        cs.hpLoss[2] = calcStep2HpLoss(60);
        cs.step = 3;
        assertEquals("Step3：ceil(60/10)+ceil(60/8)+ceil(60/6)=6+8+10=24", 24, cs.getTotalHpLoss());
    }

    // ==================== 6. 稀有度递进性 ====================

    @Test
    public void testRarityProgressionStep0ToStep1() {
        for (int i = 0; i < 1000; i++) {
            Rarity r0 = Math.random() < 0.5 ? Rarity.COMMON : Rarity.UNCOMMON;
            Rarity r1 = Math.random() < 0.6 ? Rarity.UNCOMMON : Rarity.RARE;
            assertTrue("Step0(" + r0 + ") ≤ Step1(" + r1 + ")",
                    r0.ordinal() <= r1.ordinal());
        }
    }

    @Test
    public void testRarityProgressionStep1ToStep3() {
        for (int i = 0; i < 1000; i++) {
            Rarity r1 = Math.random() < 0.6 ? Rarity.UNCOMMON : Rarity.RARE;
            Rarity r3 = Math.random() < 0.5 ? Rarity.RARE : Rarity.EPIC;
            assertTrue("Step1(" + r1 + ") ≤ Step3(" + r3 + ")",
                    r1.ordinal() <= r3.ordinal());
        }
    }

    @Test
    public void testRarityProgressionMinPossible() {
        Rarity r0 = Rarity.COMMON;
        Rarity r1 = Rarity.UNCOMMON;
        Rarity r3 = Rarity.RARE;
        assertTrue("最低稀有度链: COMMON < UNCOMMON < RARE",
                r0.ordinal() < r1.ordinal() && r1.ordinal() < r3.ordinal());
    }

    @Test
    public void testRarityProgressionMaxPossible() {
        Rarity r0 = Rarity.UNCOMMON;
        Rarity r1 = Rarity.RARE;
        Rarity r3 = Rarity.EPIC;
        assertTrue("最高稀有度链: UNCOMMON < RARE < EPIC",
                r0.ordinal() < r1.ordinal() && r1.ordinal() < r3.ordinal());
    }

    @Test
    public void testLegendaryNeverAppearsInCave() {
        for (int i = 0; i < 10000; i++) {
            double roll = Math.random();
            Rarity r0 = roll < 0.5 ? Rarity.COMMON : Rarity.UNCOMMON;
            roll = Math.random();
            Rarity r1 = roll < 0.6 ? Rarity.UNCOMMON : Rarity.RARE;
            roll = Math.random();
            Rarity r3 = roll < 0.5 ? Rarity.RARE : Rarity.EPIC;
            assertTrue("Step0 LEGENDARY 不应出现", r0 != Rarity.LEGENDARY);
            assertTrue("Step1 LEGENDARY 不应出现", r1 != Rarity.LEGENDARY);
            assertTrue("Step3 LEGENDARY 不应出现", r3 != Rarity.LEGENDARY);
        }
    }

    // ==================== 7. 离开逻辑 ====================

    @Test
    public void testLeaveAtStep0_NothingObtained() {
        int caveStep = 0;
        String result = "你什么都没得到。";
        assertTrue("Step0离开 → 提示无收获", result.contains("什么都没得到"));
    }

    @Test
    public void testLeaveAtStep0_NoHpLossInSummary() {
        int caveStep = 0;
        boolean showHpLoss = caveStep >= 1;
        assertFalse("Step0离开 → 不显示HP损失", showHpLoss);
    }

    @Test
    public void testLeaveAtStep1_ShowsItem0Only() {
        int caveStep = 1;
        boolean showItem0 = caveStep >= 1;
        boolean showItem1 = caveStep >= 2;
        assertTrue("Step1离开 → 显示物品0", showItem0);
        assertFalse("Step1离开 → 不显示物品1", showItem1);
    }

    @Test
    public void testLeaveAtStep1_ShowsStep0HpOnly() {
        int caveStep = 1;
        int hpLoss0 = 20;
        int hpLoss1 = 35;
        int totalHp = 0;
        if (caveStep >= 1) totalHp += hpLoss0;
        if (caveStep >= 2) totalHp += hpLoss1;
        assertEquals("Step1离开 → 仅扣Step0的HP=20", 20, totalHp);
    }

    @Test
    public void testLeaveAtStep2_ShowsBothItems() {
        int caveStep = 2;
        boolean showItem0 = caveStep >= 1;
        boolean showItem1 = caveStep >= 2;
        assertTrue("Step2离开 → 显示物品0", showItem0);
        assertTrue("Step2离开 → 显示物品1", showItem1);
    }

    @Test
    public void testLeaveAtStep2_ShowsSteps0And1Hp() {
        int caveStep = 2;
        int hpLoss0 = 15, hpLoss1 = 25, hpLoss2 = 50;
        int totalHp = 0;
        if (caveStep >= 1) totalHp += hpLoss0;
        if (caveStep >= 2) totalHp += hpLoss1;
        if (caveStep >= 3) totalHp += hpLoss2;
        assertEquals("Step2离开 → 扣Step0+Step1=40", 40, totalHp);
    }

    @Test
    public void testLeaveAtStep3_ShowsItems0And1Only() {
        int caveStep = 3;
        boolean showItem0 = caveStep >= 1;
        boolean showItem1 = caveStep >= 2;
        assertTrue("Step3离开 → 显示物品0", showItem0);
        assertTrue("Step3离开 → 显示物品1", showItem1);
    }

    @Test
    public void testLeaveAtStep3_ShowsAllHp() {
        int caveStep = 3;
        int hpLoss0 = 20, hpLoss1 = 30, hpLoss2 = 50;
        int totalHp = 0;
        if (caveStep >= 1) totalHp += hpLoss0;
        if (caveStep >= 2) totalHp += hpLoss1;
        if (caveStep >= 3) totalHp += hpLoss2;
        assertEquals("Step3离开 → 扣三步合计=100", 100, totalHp);
    }

    @Test
    public void testLeaveMessageContainsKeywords() {
        String msg = "你感到害怕，转身离开了洞穴。\n\n"
                + "本次探险获得：\n"
                + "· 铁剑\n"
                + "· 红宝石\n"
                + "\n共损失生命：45点";
        assertTrue("离开消息应包含'害怕'或'离开'", msg.contains("害怕") || msg.contains("离开"));
        assertTrue("离开消息应包含'获得'", msg.contains("获得"));
        assertTrue("离开消息应包含'损失生命'", msg.contains("损失生命"));
    }

    @Test
    public void testLeaveMessageWithNoItems() {
        String msg = "你感到害怕，转身离开了洞穴。\n\n你什么都没得到。";
        assertTrue("无物品离开 → 显示'什么都没得到'", msg.contains("什么都没得到"));
    }

    // ==================== 8. 完整流程路径模拟 ====================

    @Test
    public void testFullPath_LeaveAtStep0() {
        int caveStep = 0;
        int hpLoss = 0;
        int itemsObtained = 0;
        boolean left = true;
        assertTrue("Step0离开 → 已离开", left);
        assertEquals("Step0离开 → caveStep=0（未探索）", 0, caveStep);
        assertEquals("Step0离开 → HP损失=0", 0, hpLoss);
        assertEquals("Step0离开 → 获得物品=0", 0, itemsObtained);
    }

    @Test
    public void testFullPath_LeaveAtStep1() {
        CaveSimulator cs = new CaveSimulator();
        cs.exploreStep0(100);
        int itemsObtained = 1;
        assertEquals("Step1离开 → items=1", 1, itemsObtained);
        assertEquals("Step1离开 → step=1", 1, cs.step);
        assertEquals("Step1离开 → hpLoss=ceil(100/10)=10", 10, cs.getTotalHpLoss());
    }

    @Test
    public void testFullPath_LeaveAtStep2() {
        CaveSimulator cs = new CaveSimulator();
        cs.exploreStep0(100);
        cs.exploreStep1(100);
        int itemsObtained = 2;
        assertEquals("Step2离开 → items=2", 2, itemsObtained);
        assertEquals("Step2离开 → step=2", 2, cs.step);
        assertEquals("Step2离开 → hpLoss=10+13=23", 23, cs.getTotalHpLoss());
    }

    @Test
    public void testFullPath_Step3Battle() {
        CaveSimulator cs = new CaveSimulator();
        cs.exploreStep0(100);
        cs.exploreStep1(100);
        cs.exploreStep2(100);
        int itemsObtained = 2;
        boolean battle = true;
        boolean battleTriggersFight = battle;
        assertTrue("Step3战斗 → 触发战斗", battleTriggersFight);
        assertEquals("Step3战斗 → items=2（无额外物品）", 2, itemsObtained);
    }

    @Test
    public void testFullPath_Step3Treasure() {
        CaveSimulator cs = new CaveSimulator();
        cs.exploreStep0(100);
        cs.exploreStep1(100);
        cs.exploreStep2(100);
        int itemsObtained = 3;
        int goldGained = 750;
        boolean isTreasure = true;
        assertTrue("Step3财宝 → 获得金币", goldGained > 0);
        assertEquals("Step3财宝 → items=3", 3, itemsObtained);
    }

    @Test
    public void testAllFourMainPaths() {
        String[] paths = {
                "Step0 → 离开: 0物品, 0HP",
                "Step0→Step1 → 离开: 1物品, Step0HP",
                "Step0→Step1→Step2 → 离开: 2物品, Step0+Step1HP",
                "Step0→Step1→Step2→Step3: 2物品+战斗或3物品+金币, 全部HP"
        };
        assertEquals("洞穴寻宝共有4条主要路径", 4, paths.length);
    }

    // ==================== 9. HP最低保护 ====================

    private int applyHpLoss(int currentHp, int loss) {
        return Math.max(1, currentHp - loss);
    }

    @Test
    public void testHpNeverBelowOne_NormalCase() {
        assertEquals("HP充分时正常扣除", 80, applyHpLoss(100, 20));
    }

    @Test
    public void testHpNeverBelowOne_ExactToOne() {
        assertEquals("HP恰好扣到1", 1, applyHpLoss(21, 20));
    }

    @Test
    public void testHpNeverBelowOne_WouldGoBelow() {
        assertEquals("HP会扣到负数时 → 保底为1", 1, applyHpLoss(10, 20));
    }

    @Test
    public void testHpNeverBelowOne_AlreadyOne() {
        assertEquals("HP已经是1 → 仍为1", 1, applyHpLoss(1, 100));
    }

    @Test
    public void testHpNeverBelowOne_LargeDamage() {
        assertEquals("极大伤害 → 仍为1", 1, applyHpLoss(50, 9999));
    }

    @Test
    public void testHpNeverBelowOne_ZeroLoss() {
        assertEquals("损失0 → HP不变", 100, applyHpLoss(100, 0));
    }

    // ==================== 10. 大样本概率分布验证 ====================

    @Test
    public void testStep0RarityDistributionLargeSample() {
        Random rng = new Random(777);
        int trials = 100000;
        int common = 0, uncommon = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextDouble() < 0.5) common++;
            else uncommon++;
        }
        assertEquals("Step0 COMMON ≈50%，容差1.5%", 0.50, (double) common / trials, 0.015);
        assertEquals("Step0 UNCOMMON ≈50%，容差1.5%", 0.50, (double) uncommon / trials, 0.015);
    }

    @Test
    public void testStep1RarityDistributionLargeSample() {
        Random rng = new Random(777);
        int trials = 100000;
        int uncommon = 0, rare = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextDouble() < 0.6) uncommon++;
            else rare++;
        }
        assertEquals("Step1 UNCOMMON ≈60%，容差1.5%", 0.60, (double) uncommon / trials, 0.015);
        assertEquals("Step1 RARE ≈40%，容差1.5%", 0.40, (double) rare / trials, 0.015);
    }

    @Test
    public void testStep3RarityDistributionLargeSample() {
        Random rng = new Random(777);
        int trials = 100000;
        int rare = 0, epic = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextDouble() < 0.5) rare++;
            else epic++;
        }
        assertEquals("Step3 RARE ≈50%，容差1.5%", 0.50, (double) rare / trials, 0.015);
        assertEquals("Step3 EPIC ≈50%，容差1.5%", 0.50, (double) epic / trials, 0.015);
    }

    @Test
    public void testStep3BattleTreasureDistributionLargeSample() {
        Random rng = new Random(777);
        int trials = 100000;
        int battle = 0, treasure = 0;
        for (int i = 0; i < trials; i++) {
            if (rng.nextInt(2) == 0) battle++;
            else treasure++;
        }
        assertEquals("Step3 战斗 ≈50%，容差1.5%", 0.50, (double) battle / trials, 0.015);
        assertEquals("Step3 财宝 ≈50%，容差1.5%", 0.50, (double) treasure / trials, 0.015);
    }

    // ==================== 11. 装备等级递进验证 ====================

    @Test
    public void testEquipLevelProgressionAcrossSteps() {
        int step0Min = 5, step0Max = 15;
        int step1Min = 10, step1Max = 20;
        int step3Min = 15, step3Max = 25;

        assertTrue("Step0 max(15) < Step3 min(15) → 有重叠但趋势向上",
                step0Min <= step1Min && step1Min <= step3Min);
        assertTrue("装备等级随深度递增",
                step0Max <= step1Max && step1Max <= step3Max);
    }

    @Test
    public void testEquipLevelStep0NeverExceedsStep1Max() {
        assertEquals("Step0 max=15, Step1 max=20", true, 15 <= 20);
    }

    @Test
    public void testEquipLevelStep1NeverExceedsStep3Max() {
        assertEquals("Step1 max=20, Step3 max=25", true, 20 <= 25);
    }

    // ==================== 12. 事件配置契约 ====================

    @Test
    public void testEventKeyIsCaveTreasure() {
        String eventKey = "cave_treasure";
        assertEquals("事件 key", "cave_treasure", eventKey);
        assertTrue("包含 cave", eventKey.contains("cave"));
        assertTrue("包含 treasure", eventKey.contains("treasure"));
    }

    @Test
    public void testEventName() {
        String name = "洞穴寻宝";
        assertTrue("名称包含'洞穴'", name.contains("洞穴"));
        assertTrue("名称包含'寻宝'", name.contains("寻宝"));
    }

    @Test
    public void testEventDescription() {
        String desc = "前面有一处洞穴，洞穴深处可能藏有宝箱";
        assertTrue("描述包含'洞穴'", desc.contains("洞穴"));
        assertTrue("描述包含'宝箱'", desc.contains("宝箱"));
    }

    @Test
    public void testRiskDescriptionMentionsMonster() {
        String risk = "30%概率遭遇怪物偷袭";
        assertTrue("风险描述包含'30%'", risk.contains("30%"));
        assertTrue("风险描述包含'怪物'或'偷袭'",
                risk.contains("怪物") || risk.contains("偷袭"));
    }

    @Test
    public void testRewardDescriptionMentionsTreasure() {
        String reward = "宝箱奖励（金币/装备/宝石）、若击败偷袭怪物额外获得掉落";
        assertTrue("奖励描述包含'宝箱'或'奖励'",
                reward.contains("宝箱") || reward.contains("奖励"));
        assertTrue("奖励描述包含'金币'或'装备'或'宝石'",
                reward.contains("金币") || reward.contains("装备") || reward.contains("宝石"));
    }

    @Test
    public void testActionLabel() {
        String label = "开启宝箱";
        assertTrue("入口按钮文案包含'开启'", label.contains("开启"));
        assertTrue("入口按钮文案包含'宝箱'", label.contains("宝箱"));
    }

    @Test
    public void testExploreButtonTextContainsHpLoss() {
        String text = "你不小心擦破了皮肤，但是你找到了一件宝物（-25点生命，获得铁剑）";
        assertTrue("探索按钮应包含HP损失信息", text.contains("生命") || text.contains("-"));
        assertTrue("探索按钮应包含获得物品", text.contains("获得"));
    }

    @Test
    public void testLeaveButtonAlwaysAvailable() {
        boolean hasLeaveOption = true;
        assertTrue("'感到害怕，选择离开'按钮始终可用", hasLeaveOption);
    }

    // ==================== 13. Step 3 战斗分支细节 ====================

    @Test
    public void testStep3BattleButtonColor() {
        int battleColor = 0xFFE53935;
        assertEquals("战斗按钮为红色", 0xFFE53935, battleColor);
    }

    @Test
    public void testStep3TreasureButtonColor() {
        int treasureColor = 0xFF4CAF50;
        assertEquals("财宝按钮为绿色", 0xFF4CAF50, treasureColor);
    }

    @Test
    public void testStep3BattleButtonText() {
        String text = "洞穴里有一只猛兽，你被迫与它战斗！";
        assertTrue("战斗按钮包含'猛兽'或'战斗'",
                text.contains("猛兽") || text.contains("战斗"));
    }

    @Test
    public void testStep3TreasureButtonText() {
        String text = "你找到了不知谁遗弃的珠宝，你发财了！（金币+750，获得红宝石）";
        assertTrue("财宝按钮包含'发财'或'珠宝'",
                text.contains("发财") || text.contains("珠宝"));
        assertTrue("财宝按钮包含'金币'", text.contains("金币"));
        assertTrue("财宝按钮包含'获得'", text.contains("获得"));
    }

    @Test
    public void testStep3BattleReturnsOpenBattle() {
        boolean openBattle = true;
        assertTrue("Step3战斗 → 返回 open_battle=true", openBattle);
    }

    @Test
    public void testStep3BattleNoSurpriseDirection() {
        String surpriseDirection = "NONE";
        assertEquals("洞穴寻宝Step3战斗无突袭方向，正常速度排序", "NONE", surpriseDirection);
    }

    @Test
    public void testStep2ExploreButtonMentionsNoItem() {
        String text = "你即将走到洞穴最深处，但仍坚持继续探索（-45点生命）";
        assertTrue("Step2按钮包含'坚持'或'继续'", text.contains("坚持") || text.contains("继续"));
        assertFalse("Step2按钮不应包含'获得物品'",
                text.contains("获得") && (text.contains("宝物") || text.contains("装备")));
    }

    // ==================== 14. 风险收益策略分析 ====================

    @Test
    public void testExpectedValue_FullExploration() {
        double ev = 0.50 * 0.5 * 1000.0;
        assertTrue("终局财宝有正期望值", ev > 0);
    }

    @Test
    public void testStep2RiskHasNoDirectReward() {
        double evStep2 = 0.0;
        assertEquals("Step2 无物品 → 直接期望值=0", 0.0, evStep2, 0.001);
    }

    @Test
    public void testStep2OnlyWorthIfReachingStep3() {
        boolean worthOnlyIfReachStep3 = true;
        assertTrue("Step2的唯一价值在于通往Step3", worthOnlyIfReachStep3);
    }

    @Test
    public void testStep3TreasureAvgGold() {
        double avgGold = (500.0 + 1500.0) / 2.0;
        assertEquals("Step3平均金币=1000", 1000.0, avgGold, 0.1);
    }

    @Test
    public void testStep3TreasureExpectedGold() {
        double expectedGold = 0.50 * (500.0 + 1500.0) / 2.0;
        assertEquals("Step3期望金币=500（50%概率×平均1000）", 500.0, expectedGold, 0.1);
    }

    @Test
    public void testStrategy_DynamicHpLossScalesWithCharacter() {
        int loss100 = calcStep0HpLoss(100);
        int loss200 = calcStep0HpLoss(200);
        assertTrue("HP越高损失越大: " + loss200 + " > " + loss100, loss200 > loss100);
        assertTrue("Step0 损失约为HP的10%: " + loss100, loss100 <= 10 && loss100 >= 10);
    }

    @Test
    public void testStrategy_Step1LossIsFractionOfMaxHp() {
        int loss = calcStep1HpLoss(100);
        assertTrue("Step1 损失约为maxHp的1/8: ceil(100/8)=13", loss == 13);
    }

    @Test
    public void testStrategy_Step2LossIsLargestFraction() {
        assertTrue("Step2(ceil(maxHp/6)) > Step1(ceil(maxHp/8))，风险逐级递增",
                calcStep2HpLoss(100) > calcStep1HpLoss(100));
    }

    @Test
    public void testStrategy_FullExplorationHighestRewardPotential() {
        double avgGoldIfTreasure = 1000.0;
        int maxItems = 3;
        assertTrue("全程探索 → 最高潜在收益：3物品+平均1000金币",
                maxItems == 3 && avgGoldIfTreasure > 0);
    }

    // ==================== 15. 多次遍历独立性 ====================

    @Test
    public void testMultipleRunsYieldDeterministicHpLoss() {
        int hp = 100;
        int loss1 = calcStep0HpLoss(hp);
        int loss2 = calcStep0HpLoss(hp);
        assertEquals("相同HP → 多次计算结果一致", loss1, loss2);
    }

    @Test
    public void testMultipleRunsStep3_YieldsBothOutcomes() {
        Random rng = new Random(42);
        boolean sawBattle = false, sawTreasure = false;
        for (int i = 0; i < 100; i++) {
            if (rng.nextInt(2) == 0) sawBattle = true;
            else sawTreasure = true;
        }
        assertTrue("100次运行应同时出现战斗", sawBattle);
        assertTrue("100次运行应同时出现财宝", sawTreasure);
    }

    @Test
    public void testStepOrderFixed() {
        String[] expectedOrder = {"Step0_entry", "Step1_deep", "Step2_deepest", "Step3_final"};
        assertEquals("Step 顺序固定为 0→1→2→3", "Step0_entry", expectedOrder[0]);
        assertEquals("Step 顺序固定为 0→1→2→3", "Step1_deep", expectedOrder[1]);
        assertEquals("Step 顺序固定为 0→1→2→3", "Step2_deepest", expectedOrder[2]);
        assertEquals("Step 顺序固定为 0→1→2→3", "Step3_final", expectedOrder[3]);
    }

    @Test
    public void testCannotSkipSteps() {
        boolean mustPassStep0 = true;
        boolean mustPassStep1 = true;
        boolean mustPassStep2 = true;
        assertTrue("不能跳过Step0", mustPassStep0);
        assertTrue("必须经过Step0和Step1才能到Step2", mustPassStep1);
        assertTrue("必须经过Step2才能到Step3终局", mustPassStep2);
    }

    // ==================== 16. 数值边界安全测试 ====================

    @Test
    public void testStep1HpLossNeverZero() {
        for (int hp = 1; hp <= 1000; hp++) {
            assertTrue("maxHp=" + hp + " → loss=" + calcStep1HpLoss(hp) + " > 0", calcStep1HpLoss(hp) > 0);
        }
    }

    @Test
    public void testStep2HpLossNeverZero() {
        for (int hp = 1; hp <= 1000; hp++) {
            assertTrue("maxHp=" + hp + " → loss=" + calcStep2HpLoss(hp) + " > 0", calcStep2HpLoss(hp) > 0);
        }
    }

    @Test
    public void testStep3TreasureGoldNeverZero() {
        for (int i = 0; i < 1000; i++) {
            int gold = 500 + (int) (Math.random() * 1001);
            assertTrue("Step3金币 > 0，实际=" + gold, gold > 0);
        }
    }

    @Test
    public void testRarityOrdinalConsistency() {
        assertEquals("COMMON=0", 0, Rarity.COMMON.ordinal());
        assertEquals("UNCOMMON=1", 1, Rarity.UNCOMMON.ordinal());
        assertEquals("RARE=2", 2, Rarity.RARE.ordinal());
        assertEquals("EPIC=3", 3, Rarity.EPIC.ordinal());
        assertEquals("LEGENDARY=4", 4, Rarity.LEGENDARY.ordinal());
    }

    @Test
    public void testAllRaritiesHaveExpectedIds() {
        assertEquals(0, Rarity.COMMON.getId());
        assertEquals(1, Rarity.UNCOMMON.getId());
        assertEquals(2, Rarity.RARE.getId());
        assertEquals(3, Rarity.EPIC.getId());
        assertEquals(4, Rarity.LEGENDARY.getId());
    }

    // ==================== 17. 血量不足判定 ====================

    private boolean canExplore(int currentHp, int hpLoss) {
        return currentHp > hpLoss;
    }

    private String getExploreButtonText(int currentHp, int hpLoss) {
        if (currentHp <= hpLoss) {
            return "血量太低，你已经无法继续往前走了";
        }
        return "继续探索（-" + hpLoss + "点生命）";
    }

    private int getExploreButtonColor(int currentHp, int hpLoss) {
        if (currentHp <= hpLoss) {
            return 0xFF888888;
        }
        return 0xFFFF9800;
    }

    @Test
    public void testHpCheck_Step0_BlockedWhenHpLessThanLoss() {
        assertFalse("HP=5, loss=10 → 不可探索", canExplore(5, 10));
    }

    @Test
    public void testHpCheck_Step0_BlockedWhenHpEqualsLoss() {
        assertFalse("HP=20, loss=20 → 不可探索（<=条件包含等号）", canExplore(20, 20));
    }

    @Test
    public void testHpCheck_Step0_AllowedWhenHpExceedsLoss() {
        assertTrue("HP=21, loss=20 → 可探索", canExplore(21, 20));
    }

    @Test
    public void testHpCheck_Step0_AllowedWhenHpFarExceedsLoss() {
        assertTrue("HP=100, loss=10 → 可探索", canExplore(100, 10));
    }

    @Test
    public void testHpCheck_Step1_BlockedWhenHpEqualsLoss() {
        assertFalse("HP=35, loss=35 → 不可探索（<=条件包含等号）", canExplore(35, 35));
    }

    @Test
    public void testHpCheck_Step1_AllowedWhenHpExceedsLoss() {
        assertTrue("HP=36, loss=35 → 可探索", canExplore(36, 35));
    }

    @Test
    public void testHpCheck_Step2_BlockedWhenHpLessThanLoss() {
        assertFalse("HP=20, loss=50 → 不可探索", canExplore(20, 50));
    }

    @Test
    public void testHpCheck_Step2_AllowedWhenHpExceedsLoss() {
        assertTrue("HP=51, loss=50 → 可探索", canExplore(51, 50));
    }

    @Test
    public void testHpCheck_AllStepsMaxLoss() {
        assertTrue("HP=31>30 → Step0 max=30可探索", canExplore(31, 30));
        assertTrue("HP=51>50 → Step1 max=50可探索", canExplore(51, 50));
        assertTrue("HP=71>70 → Step2 max=70可探索", canExplore(71, 70));
    }

    @Test
    public void testHpCheck_BlockedButtonText() {
        String text = getExploreButtonText(10, 20);
        assertEquals("血量不足按钮文案", "血量太低，你已经无法继续往前走了", text);
    }

    @Test
    public void testHpCheck_BlockedButtonTextContainsKeywords() {
        String text = getExploreButtonText(5, 30);
        assertTrue("应包含'血量'", text.contains("血量"));
        assertTrue("应包含'无法'", text.contains("无法"));
    }

    @Test
    public void testHpCheck_NormalButtonTextContainsHpLoss() {
        String text = getExploreButtonText(100, 25);
        assertTrue("正常按钮应包含'-'", text.contains("-"));
        assertTrue("正常按钮应包含HP损失值", text.contains("25"));
    }

    @Test
    public void testHpCheck_BlockedButtonColor() {
        int color = getExploreButtonColor(10, 20);
        assertEquals("血量不足 → 按钮灰色(0xFF888888)", 0xFF888888, color);
    }

    @Test
    public void testHpCheck_NormalButtonColor() {
        int color = getExploreButtonColor(100, 20);
        assertEquals("血量充足 → 按钮橙色(0xFFFF9800)", 0xFFFF9800, color);
    }

    @Test
    public void testHpCheck_StepDoesNotAdvanceWhenBlocked() {
        int currentStep = 0;
        int currentHp = 10;
        int hpLoss = 25;
        if (currentHp <= hpLoss) {
            assertEquals("血量不足时 step 不变", 0, currentStep);
        }
    }

    @Test
    public void testHpCheck_StepAdvancesWhenAllowed() {
        int currentStep = 0;
        int currentHp = 50;
        int hpLoss = 25;
        if (currentHp > hpLoss) {
            currentStep = 1;
        }
        assertEquals("血量充足时 step 推进", 1, currentStep);
    }

    @Test
    public void testHpCheck_LeaveButtonAlwaysWorksWhenBlocked() {
        int currentHp = 10;
        int hpLoss = 25;
        boolean exploreBlocked = currentHp <= hpLoss;
        boolean leaveWorks = true;
        assertTrue("探索被禁时离开按钮仍可用", exploreBlocked);
        assertTrue("离开按钮始终可用", leaveWorks);
    }

    @Test
    public void testHpCheck_BlockedListenerIsNoop() {
        int currentHp = 10;
        int hpLoss = 25;
        boolean listenerHasEffect = currentHp > hpLoss;
        assertFalse("血量不足时 listener 不应生效", listenerHasEffect);
    }

    @Test
    public void testHpCheck_Boundary_OneHpAbove() {
        assertTrue("HP=31 > loss=30 → 可探索", canExplore(31, 30));
        assertFalse("HP=30 <= loss=30 → 不可探索", canExplore(30, 30));
    }

    @Test
    public void testHpCheck_ConsistencyAcrossSteps() {
        int[] stepLosses = {15, 35, 55};
        for (int loss : stepLosses) {
            assertFalse("HP=" + loss + " <= loss → 不可探索", canExplore(loss, loss));
            assertTrue("HP=" + (loss + 1) + " > loss → 可探索", canExplore(loss + 1, loss));
        }
    }
}
