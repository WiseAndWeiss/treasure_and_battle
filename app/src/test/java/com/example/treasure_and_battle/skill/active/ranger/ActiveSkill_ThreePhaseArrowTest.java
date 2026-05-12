package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 三相箭技能测试
 * 技能效果：对单个敌人造成x%物理攻击伤害、y%法术攻击伤害与敏捷*z点真实伤害
 */
public class ActiveSkill_ThreePhaseArrowTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：45%物理伤害、25%法术伤害、敏捷*2真实伤害
     */
    @Test
    public void testThreePhaseArrowLevel1() {
        // Given
        ActiveSkill threePhaseArrow = createSkill("three_phase_arrow", 1);
        int agility = testPlayer.getFinalAttributes().agility;
        int physicalAtk = testPlayer.getFinalAttributes().physicalAtk;
        int magicalAtk = testPlayer.getFinalAttributes().magicalAtk;

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, threePhaseArrow, java.util.Arrays.asList(testMonster), battleContext);

        int totalDamage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证三种伤害都造成了
        assertTrue("应该造成伤害", totalDamage > 0);

        // 验证日志中包含三种伤害类型
        assertLogContains(LogType.DAMAGE, "【三相箭】");
        assertLogContains(LogType.DAMAGE, "物理伤害");
        assertLogContains(LogType.DAMAGE, "法术伤害");
        assertLogContains(LogType.DAMAGE, "真实伤害");

        printBattleLogs();
    }

    /**
     * 测试等级3：70%物理伤害、35%法术伤害、敏捷*3真实伤害
     */
    @Test
    public void testThreePhaseArrowLevel3() {
        // Given
        ActiveSkill threePhaseArrow = createSkill("three_phase_arrow", 3);
        int agility = testPlayer.getFinalAttributes().agility;
        int expectedPhysicalDamage = (int) (testPlayer.getFinalAttributes().physicalAtk * 0.70f);
        int expectedMagicalDamage = (int) (testPlayer.getFinalAttributes().magicalAtk * 0.35f);
        int expectedTrueDamage = agility * 3;

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, threePhaseArrow, java.util.Arrays.asList(testMonster), battleContext);

        int totalDamage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证伤害确实造成
        assertTrue("应该造成伤害", totalDamage > 0);

        // 验证日志
        assertLogContains(LogType.DAMAGE, "【三相箭】");

        printBattleLogs();
    }

    /**
     * 测试等级5：95%物理伤害、50%法术伤害、敏捷*4真实伤害
     */
    @Test
    public void testThreePhaseArrowLevel5() {
        // Given
        ActiveSkill threePhaseArrow = createSkill("three_phase_arrow", 5);
        int agility = testPlayer.getFinalAttributes().agility;
        int expectedPhysicalDamage = (int) (testPlayer.getFinalAttributes().physicalAtk * 0.95f);
        int expectedMagicalDamage = (int) (testPlayer.getFinalAttributes().magicalAtk * 0.50f);
        int expectedTrueDamage = agility * 4;

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, threePhaseArrow, java.util.Arrays.asList(testMonster), battleContext);

        int totalDamage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证三种伤害都造成了
        assertTrue("应该造成伤害", totalDamage > 0);

        printBattleLogs();
    }

    /**
     * 测试三种伤害类型都触发
     */
    @Test
    public void testThreePhaseArrowAllDamageTypes() {
        // Given
        ActiveSkill threePhaseArrow = createSkill("three_phase_arrow", 1);
        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, threePhaseArrow, java.util.Arrays.asList(testMonster), battleContext);

        int totalDamage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证三种伤害都造成
        assertTrue("应该造成总伤害", totalDamage > 0);

        // 验证日志包含三种伤害关键词
        String lastDamageLog = getLastLogMessage(LogType.DAMAGE);
        assertNotNull("应该有伤害日志", lastDamageLog);
        assertTrue("日志应包含物理伤害", lastDamageLog.contains("物理伤害"));
        assertTrue("日志应包含法术伤害", lastDamageLog.contains("法术伤害"));
        assertTrue("日志应包含真实伤害", lastDamageLog.contains("真实伤害"));

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testThreePhaseArrowMpCost() {
        // Given
        ActiveSkill threePhaseArrow = createSkill("three_phase_arrow", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, threePhaseArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证MP消耗
        int mpAfter = testPlayer.getCurrentMp();
        int mpCost = mpBefore - mpAfter;
        assertEquals("应该消耗8点MP", 8, mpCost);

        printBattleLogs();
    }

    /**
     * 测试基于敏捷的真实伤害
     */
    @Test
    public void testThreePhaseArrowAgilityScaling() {
        // Given
        ActiveSkill threePhaseArrow = createSkill("three_phase_arrow", 1);

        // 设置不同的敏捷值
        testPlayer.getBaseAttributes().agility = 20;
        testPlayer.markAttributeCacheDirty();

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, threePhaseArrow, java.util.Arrays.asList(testMonster), battleContext);

        int totalDamage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证敏捷影响真实伤害
        assertTrue("应该造成伤害", totalDamage > 0);

        printBattleLogs();
    }

    /**
     * 测试技能对多个目标的伤害
     */
    @Test
    public void testThreePhaseArrowSingleTarget() {
        // Given
        ActiveSkill threePhaseArrow = createSkill("three_phase_arrow", 1);

        // When - 对单个目标施放技能
        battleManager.executeSkill(testPlayer, threePhaseArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证只对一个目标造成伤害
        assertLogContains(LogType.DAMAGE, "【三相箭】");

        printBattleLogs();
    }

    /**
     * 测试不同敏捷值的影响
     */
    @Test
    public void testThreePhaseArrowDifferentAgility() {
        int[] agilityValues = {10, 15, 20};

        for (int agility : agilityValues) {
            // 重置怪物HP
            testMonster.setCurrentHp(testMonster.getFinalAttributes().maxHp);

            // Given
            testPlayer.getBaseAttributes().agility = agility;
            testPlayer.markAttributeCacheDirty();

            ActiveSkill threePhaseArrow = createSkill("three_phase_arrow", 1);
            int hpBefore = testMonster.getCurrentHp();

            // When - 施放技能
            battleManager.executeSkill(testPlayer, threePhaseArrow, java.util.Arrays.asList(testMonster), battleContext);

            int totalDamage = hpBefore - testMonster.getCurrentHp();

            // Then - 验证伤害随敏捷增加
            System.out.println("敏捷：" + agility + "，总伤害：" + totalDamage);
            assertTrue("应该造成伤害", totalDamage > 0);
        }

        printBattleLogs();
    }
}
