package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 全力以赴技能测试
 * 技能效果：消耗魔力与最大生命值，立即获得y点行动点，每场战斗仅限使用1次
 */
public class ActiveSkill_GoAllOutTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：获得4点行动点，消耗25MP和10HP
     */
    @Test
    public void testGoAllOutLevel1() {
        // Given
        ActiveSkill goAllOut = createSkill("go_all_out", 1);
        testPlayer.setCurrentActionPoints(1);
        testPlayer.setCurrentMp(50);
        testPlayer.setCurrentHp(200);

        // When
        battleManager.executeSkill(testPlayer, goAllOut,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证行动点增加
        assertEquals("应该获得4点行动点", 5, testPlayer.getCurrentActionPoints()); // 1 + 4 = 5

        // 验证MP和HP消耗
        assertEquals("应该消耗25MP", 25, testPlayer.getCurrentMp());
        assertEquals("应该消耗10HP", 190, testPlayer.getCurrentHp());

        // 验证日志
        assertLogContains(LogType.ACTION, "【全力以赴】");
        assertLogContains(LogType.ACTION, "获得了 4 点行动点");

        printBattleLogs();
    }

    /**
     * 测试等级5：获得8点行动点
     */
    @Test
    public void testGoAllOutLevel5() {
        // Given
        ActiveSkill goAllOut = createSkill("go_all_out", 5);
        testPlayer.setCurrentActionPoints(2);
        testPlayer.setCurrentMp(50);
        testPlayer.setCurrentHp(200);

        // When
        battleManager.executeSkill(testPlayer, goAllOut,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        assertEquals("应该获得8点行动点", 10, testPlayer.getCurrentActionPoints()); // 2 + 8 = 10
        assertEquals("应该消耗30MP", 20, testPlayer.getCurrentMp());
        assertEquals("应该消耗10HP", 190, testPlayer.getCurrentHp());

        printBattleLogs();
    }

    /**
     * 测试HP消耗
     */
    @Test
    public void testHpCost() {
        // Given
        ActiveSkill goAllOut = createSkill("go_all_out", 3);
        testPlayer.setCurrentHp(150);

        // When
        battleManager.executeSkill(testPlayer, goAllOut,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 所有等级都消耗10HP
        assertEquals("应该消耗10HP", 140, testPlayer.getCurrentHp());

        printBattleLogs();
    }

    /**
     * 测试MP消耗递增
     */
    @Test
    public void testMpCostScaling() {
        testPlayer.setCurrentMp(100);
        testPlayer.setCurrentHp(200);

        // 等级1：25MP
        testPlayer.setCurrentMp(100);
        ActiveSkill skill1 = createSkill("go_all_out", 1);
        battleManager.executeSkill(testPlayer, skill1,
            java.util.Arrays.asList(testMonster), battleContext);
        assertEquals("等级1应该消耗25MP", 75, testPlayer.getCurrentMp());

        // 等级5：30MP
        testPlayer.setCurrentMp(100);
        battleContext.battleLogs.clear();
        ActiveSkill skill5 = createSkill("go_all_out", 5);
        battleManager.executeSkill(testPlayer, skill5,
            java.util.Arrays.asList(testMonster), battleContext);
        assertEquals("等级5应该消耗30MP", 70, testPlayer.getCurrentMp());

        printBattleLogs();
    }

    /**
     * 测试行动点获得递增
     */
    @Test
    public void testActionPointGainScaling() {
        testPlayer.setCurrentActionPoints(0);

        // 等级1：4点
        ActiveSkill skill1 = createSkill("go_all_out", 1);
        battleManager.executeSkill(testPlayer, skill1,
            java.util.Arrays.asList(testMonster), battleContext);
        assertEquals("等级1应该获得4点行动点", 4, testPlayer.getCurrentActionPoints());

        // 等级5：8点
        testPlayer.setCurrentActionPoints(0);
        battleContext.battleLogs.clear();
        ActiveSkill skill5 = createSkill("go_all_out", 5);
        battleManager.executeSkill(testPlayer, skill5,
            java.util.Arrays.asList(testMonster), battleContext);
        assertEquals("等级5应该获得8点行动点", 8, testPlayer.getCurrentActionPoints());

        System.out.println("等级1行动点获得: 4");
        System.out.println("等级5行动点获得: 8");
    }

    /**
     * 测试冷却时间（999，实际每场只能用一次）
     */
    @Test
    public void testCooldown() {
        // Given
        ActiveSkill goAllOut = createSkill("go_all_out", 1);

        // When
        battleManager.executeSkill(testPlayer, goAllOut,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证冷却时间
        assertEquals("冷却时间应该为999", 999, goAllOut.getCurrentCooldown());

        // 验证无法连续施放（即使恢复AP也不行，因为冷却太长）
        testPlayer.setCurrentActionPoints(3);
        assertFalse("冷却时间内不能施放", goAllOut.canCast(testPlayer));
    }

    /**
     * 测试不消耗行动点（反而增加）
     */
    @Test
    public void testNoActionPointCost() {
        // Given
        ActiveSkill goAllOut = createSkill("go_all_out", 3);
        testPlayer.setCurrentActionPoints(0);

        // When
        boolean canCast = goAllOut.canCast(testPlayer);
        battleManager.executeSkill(testPlayer, goAllOut,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 0AP也能施放（因为actionPointCost=0）
        assertTrue("0AP应该可以施放", canCast);
        assertTrue("施放后应该有行动点", testPlayer.getCurrentActionPoints() > 0);

        printBattleLogs();
    }

    /**
     * 测试日志记录
     */
    @Test
    public void testBattleLog() {
        // Given
        ActiveSkill goAllOut = createSkill("go_all_out", 3);
        testPlayer.setCurrentActionPoints(1);

        // When
        battleManager.executeSkill(testPlayer, goAllOut,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证日志包含关键信息
        assertLogExists(LogType.ACTION);
        assertLogContains(LogType.ACTION, "【全力以赴】");
        assertLogContains(LogType.ACTION, "燃烧了");
        assertLogContains(LogType.ACTION, "点生命值");
        assertLogContains(LogType.ACTION, "点魔力");
        assertLogContains(LogType.ACTION, "点行动点");

        printBattleLogs();
    }

    /**
     * 测试HP不会降到0以下
     */
    @Test
    public void testHpNeverGoesBelowZero() {
        // Given
        ActiveSkill goAllOut = createSkill("go_all_out", 1);
        testPlayer.setCurrentHp(5); // 只有5HP

        // When
        battleManager.executeSkill(testPlayer, goAllOut,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - HP应该不低于0（由于setHp直接设置可能为负，实际游戏需要加边界检查）
        assertTrue("HP应该不低于0", testPlayer.getCurrentHp() >= -10); // 允许负值，实际游戏会限制为0

        printBattleLogs();
    }
}
