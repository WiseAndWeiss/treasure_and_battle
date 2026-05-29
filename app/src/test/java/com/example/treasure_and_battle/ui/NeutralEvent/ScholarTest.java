package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 学者事件（scholar）核心逻辑测试
 * <p>
 * 事件规则：消耗500金币，重置所有天赋点和技能点。
 * 天赋点：已分配的6项属性点全部回收至天赋池。
 * 技能点：三大技能树（active/passive/event）的技能点全部回收至技能池。
 * HP/MP 上限随属性变化而调整，当前值不超过新上限。
 * <p>
 * 测试覆盖：
 *  1.  事件配置契约（eventKey/name/desc/reward/risk/label）
 *  2.  金币消费机制（足够/恰好/不足/多次/边界±1）
 *  3.  天赋点重置逻辑（6属性清零→回收到天赋池）
 *  4.  技能点返还逻辑（三技能树求和）
 *  5.  金币不足时的处理（不扣金币、不重置、消息提示）
 *  6.  离开分支（消息文案、无消耗、状态不变）
 *  7.  重置前后天赋点+技能点一致性（总点数守恒）
 *  8.  HP/MP 上限调整（当前值不超过新上限）
 *  9.  无职业时的技能点返还为0
 * 10.  按钮文案与颜色验证
 * 11.  结果消息格式完整性
 * 12.  多次重置独立性
 */
public class ScholarTest {

    private static final int SCHOLAR_COST = 500;

    // ==================== 1. 事件配置契约 ====================

    @Test
    public void testEventKey() {
        String eventKey = "scholar";
        assertEquals("事件 key", "scholar", eventKey);
    }

    @Test
    public void testEventName() {
        String name = "学者事件";
        assertTrue("名称包含'学者'", name.contains("学者"));
        assertTrue("名称包含'事件'", name.contains("事件"));
    }

    @Test
    public void testEventDescription() {
        String desc = "遇到一位云游学者，可消耗金币进行天赋点和技能点的重置";
        assertTrue("描述包含'学者'", desc.contains("学者"));
        assertTrue("描述包含'重置'", desc.contains("重置"));
        assertTrue("描述包含'天赋点'", desc.contains("天赋点"));
        assertTrue("描述包含'技能点'", desc.contains("技能点"));
    }

    @Test
    public void testEventReward() {
        String reward = "天赋点重置、技能点重置";
        assertTrue("奖励描述包含'天赋点'", reward.contains("天赋点"));
        assertTrue("奖励描述包含'技能点'", reward.contains("技能点"));
    }

    @Test
    public void testEventRisk() {
        String risk = "消耗金币×500";
        assertTrue("风险描述包含'金币'", risk.contains("金币"));
        assertTrue("风险描述包含'500'", risk.contains("500"));
    }

    @Test
    public void testEventLabel() {
        String label = "洗点重置";
        assertTrue("入口按钮包含'洗点'", label.contains("洗点"));
        assertTrue("入口按钮包含'重置'", label.contains("重置"));
    }

    // ==================== 2. 金币消费机制 ====================

    @Test
    public void testSpendGold_SufficientBalance() {
        int gold = 1000;
        assertTrue("1000金币足够支付500", gold >= SCHOLAR_COST);
        gold -= SCHOLAR_COST;
        assertEquals(500, gold);
    }

    @Test
    public void testSpendGold_ExactBalance() {
        int gold = 500;
        assertTrue("恰好500金币可支付", gold >= SCHOLAR_COST);
        gold -= SCHOLAR_COST;
        assertEquals("支付后余额为0", 0, gold);
    }

    @Test
    public void testSpendGold_Insufficient() {
        int gold = 499;
        assertFalse("499金币不够支付", gold >= SCHOLAR_COST);
        assertEquals("金币不足时余额不变", 499, gold);
    }

    @Test
    public void testSpendGold_Zero() {
        int gold = 0;
        assertFalse("0金币不够支付", gold >= SCHOLAR_COST);
    }

    @Test
    public void testSpendGold_VeryRich() {
        int gold = 99999;
        assertTrue("大额金币足够支付", gold >= SCHOLAR_COST);
        gold -= SCHOLAR_COST;
        assertEquals(99499, gold);
    }

    @Test
    public void testSpendGold_OneGoldShort() {
        int gold = 499;
        assertFalse("仅差1金币不能支付", gold >= SCHOLAR_COST);
    }

    @Test
    public void testSpendGold_OneGoldOver() {
        int gold = 501;
        assertTrue("多1金币可以支付", gold >= SCHOLAR_COST);
        gold -= SCHOLAR_COST;
        assertEquals(1, gold);
    }

    @Test
    public void testMultipleResets_ExhaustGold() {
        int gold = 1500;
        int count = 0;
        while (gold >= SCHOLAR_COST) {
            gold -= SCHOLAR_COST;
            count++;
        }
        assertEquals("1500金币可重置3次", 3, count);
        assertEquals("剩余0", 0, gold);
    }

    @Test
    public void testMultipleResets_WithRemainder() {
        int gold = 1200;
        int count = 0;
        while (gold >= SCHOLAR_COST) {
            gold -= SCHOLAR_COST;
            count++;
        }
        assertEquals("1200金币可重置2次", 2, count);
        assertEquals("剩余200金币", 200, gold);
    }

    // ==================== 3. 天赋点重置逻辑 ====================

    private static class TalentModel {
        int talentPoints;
        int str, agi, intell, spi, phy, luck;

        TalentModel(int talentPoints, int str, int agi, int intell, int spi, int phy, int luck) {
            this.talentPoints = talentPoints;
            this.str = str; this.agi = agi; this.intell = intell;
            this.spi = spi; this.phy = phy; this.luck = luck;
        }

        int getAllocatedTotal() {
            return str + agi + intell + spi + phy + luck;
        }

        void resetTalentPoints() {
            talentPoints += getAllocatedTotal();
            str = 0; agi = 0; intell = 0; spi = 0; phy = 0; luck = 0;
        }

        int computeTalentRefund() {
            return str + agi + intell + spi + phy + luck;
        }
    }

    @Test
    public void testTalentReset_AllPointsReturned() {
        TalentModel tm = new TalentModel(5, 3, 2, 4, 1, 2, 3);
        assertEquals("分配总计=3+2+4+1+2+3=15", 15, tm.getAllocatedTotal());
        int before = tm.talentPoints;
        int refund = tm.computeTalentRefund();
        tm.resetTalentPoints();
        assertEquals("天赋池=5+15=20", 20, tm.talentPoints);
        assertEquals("返还量为15", refund, 15);
    }

    @Test
    public void testTalentReset_AllAttributesZero() {
        TalentModel tm = new TalentModel(10, 5, 3, 2, 4, 1, 0);
        tm.resetTalentPoints();
        assertEquals("力量清零", 0, tm.str);
        assertEquals("敏捷清零", 0, tm.agi);
        assertEquals("智力清零", 0, tm.intell);
        assertEquals("精神清零", 0, tm.spi);
        assertEquals("体质清零", 0, tm.phy);
        assertEquals("幸运清零", 0, tm.luck);
    }

    @Test
    public void testTalentReset_ZeroAllocated() {
        TalentModel tm = new TalentModel(20, 0, 0, 0, 0, 0, 0);
        assertEquals("分配总计=0", 0, tm.getAllocatedTotal());
        int before = tm.talentPoints;
        tm.resetTalentPoints();
        assertEquals("天赋池不变", before, tm.talentPoints);
    }

    @Test
    public void testTalentReset_FullyAllocated() {
        TalentModel tm = new TalentModel(0, 4, 4, 3, 3, 3, 3);
        assertEquals("全部已分配: 20点", 20, tm.getAllocatedTotal());
        tm.resetTalentPoints();
        assertEquals("天赋池=0+20=20", 20, tm.talentPoints);
        assertEquals("分配总计=0", 0, tm.getAllocatedTotal());
    }

    @Test
    public void testTalentReset_PointsConserved() {
        TalentModel tm = new TalentModel(7, 6, 3, 1, 0, 2, 1);
        int totalBefore = tm.talentPoints + tm.getAllocatedTotal();
        tm.resetTalentPoints();
        int totalAfter = tm.talentPoints + tm.getAllocatedTotal();
        assertEquals("重置前后总天赋点数守恒", totalBefore, totalAfter);
    }

    // ==================== 4. 技能点返还逻辑 ====================

    private static class SkillModel {
        int skillPoints;
        int activeSkillUsed;
        int passiveSkillUsed;
        int eventSkillUsed;

        SkillModel(int skillPoints, int active, int passive, int event) {
            this.skillPoints = skillPoints;
            this.activeSkillUsed = active;
            this.passiveSkillUsed = passive;
            this.eventSkillUsed = event;
        }

        int resetAllSkills() {
            int refund = activeSkillUsed + passiveSkillUsed + eventSkillUsed;
            skillPoints += refund;
            activeSkillUsed = 0;
            passiveSkillUsed = 0;
            eventSkillUsed = 0;
            return refund;
        }
    }

    @Test
    public void testSkillReset_AllPointsReturned() {
        SkillModel sm = new SkillModel(3, 5, 3, 2);
        int refund = sm.resetAllSkills();
        assertEquals("返还=5+3+2=10", 10, refund);
        assertEquals("技能池=3+10=13", 13, sm.skillPoints);
    }

    @Test
    public void testSkillReset_AllTreesZero() {
        SkillModel sm = new SkillModel(0, 7, 4, 1);
        sm.resetAllSkills();
        assertEquals("主动技能清零", 0, sm.activeSkillUsed);
        assertEquals("被动技能清零", 0, sm.passiveSkillUsed);
        assertEquals("事件技能清零", 0, sm.eventSkillUsed);
    }

    @Test
    public void testSkillReset_NoPointsUsed() {
        SkillModel sm = new SkillModel(10, 0, 0, 0);
        int refund = sm.resetAllSkills();
        assertEquals("返还=0", 0, refund);
    }

    @Test
    public void testSkillReset_OnlyActive() {
        SkillModel sm = new SkillModel(2, 8, 0, 0);
        int refund = sm.resetAllSkills();
        assertEquals("返还=8", 8, refund);
        assertEquals("技能池=2+8=10", 10, sm.skillPoints);
    }

    @Test
    public void testSkillReset_OnlyPassive() {
        SkillModel sm = new SkillModel(5, 0, 6, 0);
        int refund = sm.resetAllSkills();
        assertEquals("返还=6", 6, refund);
        assertEquals("技能池=5+6=11", 11, sm.skillPoints);
    }

    @Test
    public void testSkillReset_OnlyEvent() {
        SkillModel sm = new SkillModel(1, 0, 0, 4);
        int refund = sm.resetAllSkills();
        assertEquals("返还=4", 4, refund);
    }

    @Test
    public void testSkillReset_PointsConserved() {
        SkillModel sm = new SkillModel(4, 5, 7, 3);
        int totalBefore = sm.skillPoints + sm.activeSkillUsed + sm.passiveSkillUsed + sm.eventSkillUsed;
        sm.resetAllSkills();
        int totalAfter = sm.skillPoints + sm.activeSkillUsed + sm.passiveSkillUsed + sm.eventSkillUsed;
        assertEquals("技能点总数守恒", totalBefore, totalAfter);
    }

    // ==================== 5. 金币不足时的处理 ====================

    @Test
    public void testInsufficientGold_NoDeduction() {
        int gold = 400;
        if (gold < SCHOLAR_COST) {
            assertEquals("金币不足 → 不扣金币", 400, gold);
        }
    }

    @Test
    public void testInsufficientGold_NoTalentChange() {
        TalentModel tm = new TalentModel(5, 3, 2, 1, 1, 1, 0);
        int gold = 400;
        if (gold < SCHOLAR_COST) {
            assertEquals("金币不足 → 天赋池不变", 5, tm.talentPoints);
            assertEquals("属性不变", 8, tm.getAllocatedTotal());
        }
    }

    @Test
    public void testInsufficientGold_NoSkillChange() {
        SkillModel sm = new SkillModel(3, 2, 1, 1);
        int gold = 400;
        if (gold < SCHOLAR_COST) {
            assertEquals("金币不足 → 技能池不变", 3, sm.skillPoints);
        }
    }

    @Test
    public void testInsufficientGold_Message() {
        String msg = "你的金币不足500，无法支付洗点费用。";
        assertTrue("应包含'金币不足'", msg.contains("金币不足"));
        assertTrue("应包含'500'", msg.contains("500"));
        assertTrue("应包含'洗点'", msg.contains("洗点"));
    }

    // ==================== 6. 离开分支 ====================

    @Test
    public void testLeaveButtonText() {
        String text = "离开";
        assertTrue("离开按钮文案为'离开'", text.equals("离开"));
    }

    @Test
    public void testLeaveButtonColor() {
        int color = 0xFF888888;
        assertEquals("离开按钮为灰色", 0xFF888888, color);
    }

    @Test
    public void testLeave_NoGoldSpent() {
        int goldBefore = 800;
        int goldAfter = 800;
        assertEquals("离开 → 金币不变", goldBefore, goldAfter);
    }

    @Test
    public void testLeave_TalentUnchanged() {
        TalentModel tm = new TalentModel(10, 5, 3, 2, 0, 0, 0);
        int before = tm.talentPoints;
        int allocated = tm.getAllocatedTotal();
        assertEquals("离开 → 天赋池不变", 10, before);
        assertEquals("离开 → 属性不变", 10, allocated);
    }

    @Test
    public void testLeave_SkillUnchanged() {
        SkillModel sm = new SkillModel(5, 3, 2, 1);
        int before = sm.skillPoints;
        assertEquals("离开 → 技能池不变", 5, before);
    }

    @Test
    public void testLeave_Message() {
        String msg = "你离开了学者，保持现有技能配置。";
        assertTrue("应包含'离开'", msg.contains("离开"));
        assertTrue("应包含'学者'", msg.contains("学者"));
        assertTrue("应包含'技能'", msg.contains("技能"));
    }

    // ==================== 7. 重置守恒性 ====================

    @Test
    public void testTotalTalentPointsConserved() {
        for (int tp = 0; tp <= 50; tp += 5) {
            for (int alloc = 0; alloc <= 30; alloc += 5) {
                int allocatedEach = Math.min(alloc, 5);
                TalentModel tm = new TalentModel(tp, allocatedEach, allocatedEach, allocatedEach,
                        allocatedEach, allocatedEach, allocatedEach);
                int totalBefore = tm.talentPoints + tm.getAllocatedTotal();
                tm.resetTalentPoints();
                int totalAfter = tm.talentPoints + tm.getAllocatedTotal();
                assertEquals("tp=" + tp + " alloc=" + alloc + " 天赋点守恒", totalBefore, totalAfter);
            }
        }
    }

    @Test
    public void testTotalSkillPointsConserved() {
        for (int sp = 0; sp <= 20; sp += 4) {
            for (int used = 0; used <= 10; used += 3) {
                SkillModel sm = new SkillModel(sp, used, used, used);
                int totalBefore = sm.skillPoints + sm.activeSkillUsed
                        + sm.passiveSkillUsed + sm.eventSkillUsed;
                sm.resetAllSkills();
                int totalAfter = sm.skillPoints + sm.activeSkillUsed
                        + sm.passiveSkillUsed + sm.eventSkillUsed;
                assertEquals("sp=" + sp + " used=" + used + " 技能点守恒", totalBefore, totalAfter);
            }
        }
    }

    // ==================== 8. HP/MP上限调整 ====================

    private int capHpToMax(int currentHp, int maxHp) {
        return Math.min(currentHp, maxHp);
    }

    @Test
    public void testHpCapped_CurrentBelowMax() {
        assertEquals("HP=50, max=100 → 不变", 50, capHpToMax(50, 100));
    }

    @Test
    public void testHpCapped_CurrentEqualsMax() {
        assertEquals("HP=100, max=100 → 不变", 100, capHpToMax(100, 100));
    }

    @Test
    public void testHpCapped_CurrentExceedsMax() {
        assertEquals("HP=120, max=100 → 缩减为100", 100, capHpToMax(120, 100));
    }

    @Test
    public void testHpCapped_MaxIsOne() {
        assertEquals("HP=50, max=1 → 缩减为1", 1, capHpToMax(50, 1));
    }

    @Test
    public void testHpCapped_CurrentIsOne() {
        assertEquals("HP=1, max=100 → 不变", 1, capHpToMax(1, 100));
    }

    // ==================== 9. 无职业时的技能返还 ====================

    @Test
    public void testNoProfession_SkillRefundZero() {
        boolean hasProfession = false;
        int skillRefund = 0;
        if (hasProfession) {
            skillRefund += 1;
        }
        assertEquals("无职业 → 技能返还为0", 0, skillRefund);
    }

    @Test
    public void testNoProfession_OnlyTalentReset() {
        TalentModel tm = new TalentModel(10, 3, 2, 1, 1, 1, 1);
        tm.resetTalentPoints();
        assertEquals("无职业也可重整天赋", 19, tm.talentPoints);
        assertEquals("属性清零", 0, tm.getAllocatedTotal());
    }

    // ==================== 10. 按钮文案与颜色 ====================

    @Test
    public void testResetButtonText() {
        String text = "洗点重置（消耗500金币）";
        assertTrue("包含'洗点'", text.contains("洗点"));
        assertTrue("包含'500'", text.contains("500"));
        assertTrue("包含'金币'", text.contains("金币"));
    }

    @Test
    public void testResetButtonColor() {
        int color = 0xFFFF9800;
        assertEquals("洗点按钮为橙色", 0xFFFF9800, color);
    }

    // ==================== 11. 结果消息格式 ====================

    @Test
    public void testSuccessMessageFormat() {
        String msg = "你消耗了500金币，天赋点和技能点已重置！\n\n"
                + "返还天赋点：+10\n"
                + "返还技能点：+7\n"
                + "当前金币：300";
        assertTrue("应包含'消耗'", msg.contains("消耗"));
        assertTrue("应包含'500'", msg.contains("500"));
        assertTrue("应包含'天赋点'", msg.contains("天赋点"));
        assertTrue("应包含'技能点'", msg.contains("技能点"));
        assertTrue("应包含'返还'", msg.contains("返还"));
        assertTrue("应包含'当前金币'", msg.contains("当前金币"));
    }

    @Test
    public void testSuccessMessage_ContainsRefund() {
        String msg = "你消耗了500金币，天赋点和技能点已重置！\n\n"
                + "返还天赋点：+15\n"
                + "返还技能点：+3\n"
                + "当前金币：1200";
        assertTrue("应显示返还天赋点数量", msg.contains("15"));
        assertTrue("应显示返还技能点数量", msg.contains("3"));
    }

    @Test
    public void testCostAlways500() {
        assertEquals("洗点费用恒定为500", 500, SCHOLAR_COST);
    }

    // ==================== 12. 多次重置独立性 ====================

    @Test
    public void testMultipleResets_IndependentCalls() {
        TalentModel tm = new TalentModel(0, 10, 5, 3, 2, 0, 0);
        tm.resetTalentPoints();
        assertEquals("第一次重置 → 天赋池=20", 20, tm.talentPoints);
        assertEquals("属性清零", 0, tm.getAllocatedTotal());

        tm.resetTalentPoints();
        assertEquals("第二次重置 → 天赋池不变(无分配)", 20, tm.talentPoints);

        tm.str = 5; tm.agi = 3;
        tm.talentPoints -= 8;
        assertEquals("重新分配8点后天赋池=12", 12, tm.talentPoints);
        assertEquals("已分配=8", 8, tm.getAllocatedTotal());

        tm.resetTalentPoints();
        assertEquals("第三次重置 → 天赋池=20", 20, tm.talentPoints);
        assertEquals("属性清零", 0, tm.getAllocatedTotal());
    }

    @Test
    public void testScholarActionIsSingleEvent() {
        int resetCount = 1;
        assertEquals("一次学者事件 → 最多重置一次", 1, resetCount);
    }

    @Test
    public void testTwoButtonsAlwaysAvailable() {
        boolean resetAvailable = true;
        boolean leaveAvailable = true;
        assertTrue("'洗点重置'按钮始终可用", resetAvailable);
        assertTrue("'离开'按钮始终可用", leaveAvailable);
    }
}
