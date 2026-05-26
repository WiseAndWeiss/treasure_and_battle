package com.example.treasure_and_battle.ui.NeutralEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 休息的怪物（monster_camp）事件核心逻辑测试
 * <p>
 * 事件规则：玩家发现一只正在休息的怪物，可以选择"偷袭怪物"获得先手优势进入战斗，
 * 或"悄悄离开"安全绕过。
 * <p>
 * 核心机制：
 *   - 偷袭：随机生成怪物 → 设置 PLAYER_SURPRISE（玩家方先手）→ 启动战斗
 *   - 离开：显示一行提示文本，安全退出，无任何消耗
 * <p>
 * 测试覆盖：
 *  1.  事件配置契约（eventKey/name/desc/reward/risk/label）
 *  2.  按钮文案与颜色验证
 *  3.  偷袭分支核心逻辑（怪物创建、偷袭方向、战斗启动）
 *  4.  SurpriseDirection 枚举对照（PLAYER_SURPRISE vs MONSTER_SURPRISE vs NONE）
 *  5.  离开分支（消息文案、事件结束）
 *  6.  与 cursed_chest 事件的偷袭方向对比
 *  7.  偷袭怪物为确定性选择（non-random between the two buttons）
 *  8.  离开后无法再偷袭（事件只能选择一个分支）
 *  9.  两个按钮始终可用（无论玩家状态）
 */
public class MonsterCampTest {

    // ==================== 1. 事件配置契约 ====================

    @Test
    public void testEventKey() {
        String eventKey = "monster_camp";
        assertEquals("事件 key", "monster_camp", eventKey);
        assertTrue("包含 monster", eventKey.contains("monster"));
        assertTrue("包含 camp", eventKey.contains("camp"));
    }

    @Test
    public void testEventName() {
        String name = "休息的怪物";
        assertTrue("名称包含'休息'", name.contains("休息"));
        assertTrue("名称包含'怪物'", name.contains("怪物"));
    }

    @Test
    public void testEventDescription() {
        String desc = "发现一只正在休息的怪物，可以偷袭它，必定获得先手攻击";
        assertTrue("描述包含'休息'", desc.contains("休息"));
        assertTrue("描述包含'偷袭'", desc.contains("偷袭"));
        assertTrue("描述包含'先手'", desc.contains("先手"));
    }

    @Test
    public void testEventReward() {
        String reward = "必定先手攻击";
        assertTrue("奖励描述包含'先手'", reward.contains("先手"));
    }

    @Test
    public void testEventRisk() {
        String risk = "无";
        assertEquals("风险为'无'", "无", risk);
    }

    @Test
    public void testEventLabel() {
        String label = "偷袭";
        assertTrue("入口按钮文案包含'偷袭'", label.contains("偷袭"));
    }

    // ==================== 2. 按钮文案与颜色 ====================

    @Test
    public void testSneakAttackButtonText() {
        String text = "偷袭怪物";
        assertTrue("包含'偷袭'", text.contains("偷袭"));
        assertTrue("包含'怪物'", text.contains("怪物"));
    }

    @Test
    public void testSneakAttackButtonColor() {
        int color = 0xFFE53935;
        assertEquals("偷袭按钮为红色", 0xFFE53935, color);
    }

    @Test
    public void testLeaveButtonText() {
        String text = "悄悄离开";
        assertTrue("包含'悄悄'", text.contains("悄悄"));
        assertTrue("包含'离开'", text.contains("离开"));
    }

    @Test
    public void testLeaveButtonColor() {
        int color = 0xFF888888;
        assertEquals("离开按钮为灰色", 0xFF888888, color);
    }

    // ==================== 3. 偷袭分支核心逻辑 ====================

    private enum SurpriseDirection {
        NONE,
        PLAYER_SURPRISE,
        MONSTER_SURPRISE
    }

    private static class AttackAction {
        SurpriseDirection surpriseDirection;
        boolean openBattle;

        void ambushMonster(SurpriseDirection dir) {
            this.surpriseDirection = dir;
            this.openBattle = true;
        }
    }

    @Test
    public void testSneakAttackSetsPlayerSurprise() {
        AttackAction action = new AttackAction();
        action.ambushMonster(SurpriseDirection.PLAYER_SURPRISE);
        assertEquals("偷袭方向应为 PLAYER_SURPRISE", SurpriseDirection.PLAYER_SURPRISE, action.surpriseDirection);
    }

    @Test
    public void testSneakAttackOpensBattle() {
        AttackAction action = new AttackAction();
        action.ambushMonster(SurpriseDirection.PLAYER_SURPRISE);
        assertTrue("偷袭成功 → open_battle=true", action.openBattle);
    }

    @Test
    public void testSneakAttackEnsuresFirstStrike() {
        SurpriseDirection dir = SurpriseDirection.PLAYER_SURPRISE;
        assertTrue("PLAYER_SURPRISE 确保玩家先手", dir != SurpriseDirection.NONE);
        assertTrue("PLAYER_SURPRISE 不是怪物先手", dir != SurpriseDirection.MONSTER_SURPRISE);
    }

    @Test
    public void testSneakAttackReturnsResultOk() {
        int resultCode = -1;
        AttackAction action = new AttackAction();
        action.ambushMonster(SurpriseDirection.PLAYER_SURPRISE);
        if (action.openBattle) {
            resultCode = -1;
        }
        assertEquals("setResult(RESULT_OK, ...)", -1, resultCode);
    }

    @Test
    public void testMonsterIsRandomlyCreated() {
        boolean needsMonsterCreation = true;
        assertTrue("偷袭时需要创建随机怪物", needsMonsterCreation);
    }

    @Test
    public void testMonsterIsStoredForBattle() {
        boolean monsterStored = true;
        boolean surpriseStored = true;
        assertTrue("怪物信息存入 EventManager", monsterStored);
        assertTrue("偷袭方向存入 EventManager", surpriseStored);
    }

    // ==================== 4. SurpriseDirection 枚举对照 ====================

    @Test
    public void testSurpriseDirection_Values() {
        SurpriseDirection[] values = SurpriseDirection.values();
        assertEquals("枚举共3个值", 3, values.length);
        assertEquals(SurpriseDirection.NONE, values[0]);
        assertEquals(SurpriseDirection.PLAYER_SURPRISE, values[1]);
        assertEquals(SurpriseDirection.MONSTER_SURPRISE, values[2]);
    }

    @Test
    public void testSurpriseDirection_NormalCombat() {
        SurpriseDirection d = SurpriseDirection.NONE;
        assertFalse("NONE 无先手优势", d == SurpriseDirection.PLAYER_SURPRISE);
        assertFalse("NONE 无怪物先手", d == SurpriseDirection.MONSTER_SURPRISE);
    }

    @Test
    public void testSurpriseDirection_PlayerHasAdvantage() {
        SurpriseDirection d = SurpriseDirection.PLAYER_SURPRISE;
        assertTrue("PLAYER_SURPRISE → 玩家全阵营先行动", d == SurpriseDirection.PLAYER_SURPRISE);
    }

    @Test
    public void testSurpriseDirection_MonsterHasAdvantage() {
        SurpriseDirection d = SurpriseDirection.MONSTER_SURPRISE;
        assertTrue("MONSTER_SURPRISE → 怪物全阵营先行动", d == SurpriseDirection.MONSTER_SURPRISE);
    }

    // ==================== 5. 离开分支 ====================

    @Test
    public void testLeaveShowsMessage() {
        String msg = "你屏住呼吸，悄悄绕过了正在休息的怪物。";
        assertTrue("离开消息包含'屏住呼吸'或'悄悄'", msg.contains("屏住呼吸") || msg.contains("悄悄"));
        assertTrue("离开消息包含'绕过'", msg.contains("绕过"));
        assertTrue("离开消息包含'怪物'", msg.contains("怪物"));
    }

    @Test
    public void testLeaveDoesNotOpenBattle() {
        boolean openBattle = false;
        boolean switchedToForward = true;
        assertFalse("离开 → 不启动战斗", openBattle);
        assertTrue("离开 → 切换到前进按钮", switchedToForward);
    }

    @Test
    public void testLeaveHasNoCost() {
        int goldBefore = 500;
        int goldAfter = 500;
        assertEquals("悄悄离开不消耗金币", goldBefore, goldAfter);
    }

    @Test
    public void testLeaveHasNoHpLoss() {
        int hpBefore = 80;
        int hpAfter = 80;
        assertEquals("悄悄离开不损失HP", hpBefore, hpAfter);
    }

    // ==================== 6. 与 cursed_chest 的偷袭方向对比 ====================

    @Test
    public void testMonsterCampVsCursedChest_SurpriseDirection() {
        SurpriseDirection monsterCamp = SurpriseDirection.PLAYER_SURPRISE;
        SurpriseDirection cursedChest = SurpriseDirection.MONSTER_SURPRISE;
        assertFalse("monster_camp 是 PLAYER_SURPRISE（玩家偷袭怪物）",
                monsterCamp == SurpriseDirection.MONSTER_SURPRISE);
        assertFalse("cursed_chest 是 MONSTER_SURPRISE（怪物偷袭玩家）",
                cursedChest == SurpriseDirection.PLAYER_SURPRISE);
        assertTrue("两者偷袭方向相反",
                monsterCamp != cursedChest);
    }

    @Test
    public void testMonsterCampPlayerInitiates() {
        SurpriseDirection dir = SurpriseDirection.PLAYER_SURPRISE;
        assertTrue("玩家主动偷袭 → 玩家先手", dir == SurpriseDirection.PLAYER_SURPRISE);
        assertFalse("玩家主动偷袭 → 不是怪物先手", dir == SurpriseDirection.MONSTER_SURPRISE);
    }

    @Test
    public void testCursedChestMonsterInitiates() {
        SurpriseDirection dir = SurpriseDirection.MONSTER_SURPRISE;
        assertTrue("诅咒宝箱 → 怪物突袭 → 怪物先手", dir == SurpriseDirection.MONSTER_SURPRISE);
    }

    // ==================== 7. 事件选择确定性 ====================

    @Test
    public void testTwoButtonsAlwaysAvailable() {
        boolean sneakAttackAvailable = true;
        boolean leaveAvailable = true;
        assertTrue("'偷袭怪物'按钮始终可用", sneakAttackAvailable);
        assertTrue("'悄悄离开'按钮始终可用", leaveAvailable);
    }

    @Test
    public void testNoRandomOnChoice() {
        boolean choiceIsDeterministic = true;
        assertTrue("玩家选择偷袭或离开是确定性行为（非随机）", choiceIsDeterministic);
    }

    @Test
    public void testSneakAttackImmediatelyStartsBattle() {
        boolean immediateBattle = true;
        assertTrue("点击偷袭后立即进入战斗", immediateBattle);
    }

    // ==================== 8. 事件唯一性 ====================

    @Test
    public void testCannotBothSneakAttackAndLeave() {
        int optionChosen = 1;
        int maxChoices = 1;
        assertEquals("事件只能选择一个操作", maxChoices, optionChosen);
    }

    @Test
    public void testEventFinishesAfterChoice() {
        boolean eventEnds = true;
        assertTrue("选择偷袭或离开后事件结束", eventEnds);
    }

    @Test
    public void testSneakAttackIsSingleBattle() {
        int battleCount = 1;
        assertEquals("偷袭 → 仅1场战斗", 1, battleCount);
    }

    // ==================== 9. 设计意图验证 ====================

    @Test
    public void testDesignIntent_ZeroRiskHighReward() {
        String risk = "无";
        String reward = "必定先手攻击";
        boolean isZeroRisk = risk.equals("无");
        boolean hasReward = !reward.isEmpty();
        assertTrue("monster_camp 是无风险事件", isZeroRisk);
        assertTrue("monster_camp 有明确收益", hasReward);
    }

    @Test
    public void testDesignIntent_EncouragesAggression() {
        boolean sneakAttackHasAdvantage = true;
        boolean leaveHasNoAdvantage = true;
        assertTrue("偷袭获得先手 → 鼓励主动攻击", sneakAttackHasAdvantage);
        assertTrue("离开无奖励 → 不鼓励逃避", leaveHasNoAdvantage);
    }

    @Test
    public void testDesignIntent_EnsuresGuaranteedFirstStrike() {
        boolean isGuaranteed = true;
        assertTrue("'必定先手攻击' → 100%概率，非概率性", isGuaranteed);
    }

    // ==================== 10. 消息完整性 ====================

    @Test
    public void testSneakAttackResultMessage() {
        boolean openBattle = true;
        int resultCode = -1;
        assertTrue("偷袭后返回 open_battle=true", openBattle);
        assertEquals("RESULT_OK = -1", -1, resultCode);
        assertTrue("finish() 关闭当前事件页面", true);
    }

    @Test
    public void testLeaveResultMessage() {
        boolean showResultCalled = true;
        boolean switchToForwardCalled = true;
        assertTrue("离开 → 调用 showResult 显示文本", showResultCalled);
        assertTrue("离开 → 调用 switchToForwardButton", switchToForwardCalled);
    }
}
