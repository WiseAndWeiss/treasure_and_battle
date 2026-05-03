package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.SkillManager;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 点射技能测试
 * 技能效果：对目标造成{x}%物理攻击伤害，该次攻击以{y}%的概率增加一轮额外的暴击判定
 */
public class ActiveSkillShotTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：100%物理伤害，0%额外暴击概率
     * 预期：造成 (50 * 100% - 20防御) = 30点物理伤害，只有基础暴击判定
     */
    @Test
    public void testShotLevel1() {
        // Given
        ActiveSkill shot = createSkill("shot", 1);
        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        int damage = executeSkillAndDamage(shot, testPlayer, testMonster);

        // Then
        // 预期伤害：50 * 100% - 20 = 30（可能暴击x1.5）
        int expectedDamage = 30;
        assertTrue("等级1点射应该造成约 " + expectedDamage + " 点伤害，实际造成 " + damage,
            damage >= expectedDamage); // 可能暴击，所以只验证下限

        // 验证HP正确扣除
        assertEquals("怪物HP应该正确扣除", monsterHpBefore - damage, testMonster.getCurrentHp());

        // 验证日志
        assertLogExists(LogType.DAMAGE);
        assertLogContains(LogType.DAMAGE, "【点射】");
        printBattleLogs();
    }

    /**
     * 测试等级3：110%物理伤害，10%额外暴击概率
     * 预期：造成约35点物理伤害，有10%概率触发额外暴击判定
     */
    @Test
    public void testShotLevel3() {
        // Given
        ActiveSkill shot = createSkill("shot", 3);
        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        int damage = executeSkillAndDamage(shot, testPlayer, testMonster);

        // Then
        // 基础伤害：50 * 110% - 20 = 35
        int expectedBaseDamage = 35;
        assertTrue("等级3点射应该造成至少 " + expectedBaseDamage + " 点伤害，实际造成 " + damage,
            damage >= expectedBaseDamage - 2);

        // 验证HP正确扣除
        assertEquals("怪物HP应该正确扣除", monsterHpBefore - damage, testMonster.getCurrentHp());

        // 验证日志（可能包含额外暴击判定的日志）
        assertLogContains(LogType.DAMAGE, "【点射】");
        printBattleLogs();
    }

    /**
     * 测试等级5：120%物理伤害，20%额外暴击概率
     * 预期：造成约40点物理伤害，有20%概率触发额外暴击判定
     */
    @Test
    public void testShotLevel5() {
        // Given
        ActiveSkill shot = createSkill("shot", 5);
        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        int damage = executeSkillAndDamage(shot, testPlayer, testMonster);

        // Then
        // 基础伤害：50 * 120% - 20 = 40
        int expectedBaseDamage = 40;
        assertTrue("等级5点射应该造成至少 " + expectedBaseDamage + " 点伤害，实际造成 " + damage,
            damage >= expectedBaseDamage - 2);

        // 验证HP正确扣除
        assertEquals("怪物HP应该正确扣除", monsterHpBefore - damage, testMonster.getCurrentHp());

        // 验证日志
        assertLogContains(LogType.DAMAGE, "【点射】");
        printBattleLogs();
    }

    /**
     * 测试点射的额外暴击机制
     * 通过设置高暴击率和多次测试来验证额外暴击判定
     */
    @Test
    public void testShotExtraCritMechanic() {
        // Given
        testPlayer.getBaseAttributes().physicalCritRate = 1.0f; // 100%基础暴击
        testPlayer.getBaseAttributes().physicalCritDmg = 2.0f; // 2倍暴击伤害
        testPlayer.markAttributeCacheDirty();

        ActiveSkill shot = createSkill("shot", 5); // 20%额外暴击概率

        // When - 执行多次以期望触发额外暴击
        boolean triggeredExtraCrit = false;
        for (int i = 0; i < 20; i++) {
            testMonster.setCurrentHp(testMonster.getBaseAttributes().maxHp);
            battleContext.battleLogs.clear();

            battleManager.executeSkill(testPlayer, shot,
                java.util.Arrays.asList(testMonster), battleContext);

            // 检查日志中是否有额外暴击判定的记录
            boolean hasExtraCritLog = battleContext.battleLogs.stream()
                .anyMatch(log -> log.getType() == LogType.DODGE_CRIT
                    && log.getFormattedMessage().contains("额外暴击判定"));

            if (hasExtraCritLog) {
                triggeredExtraCrit = true;
                break;
            }
        }

        // Then - 由于有20%概率，执行20次应该至少触发一次
        assertTrue("20%额外暴击概率在20次测试中应该至少触发一次", triggeredExtraCrit);
        printBattleLogs();
    }

    /**
     * 测试点射对无防御目标的效果
     */
    @Test
    public void testShotAgainstZeroDefense() {
        // Given
        testMonster.getBaseAttributes().physicalDef = 0;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(testMonster.getBaseAttributes().maxHp);

        ActiveSkill shot = createSkill("shot", 1);

        // When
        int damage = executeSkillAndDamage(shot, testPlayer, testMonster);

        // Then
        // 无防御时：50 * 100% = 50点伤害（可能暴击）
        int expectedDamage = 50;
        assertTrue("对0防御目标应该造成至少 " + expectedDamage + " 点伤害，实际造成 " + damage,
            damage >= expectedDamage - 2);
        printBattleLogs();
    }

    /**
     * 测试点射的消耗
     * 预期：消耗1行动点，不消耗MP和HP
     */
    @Test
    public void testShotCost() {
        // Given
        ActiveSkill shot = createSkill("shot", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, shot,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        assertEquals("应该消耗1行动点", apBefore - 1, testPlayer.getCurrentActionPoints());
        assertEquals("不应该消耗MP", mpBefore, testPlayer.getCurrentMp());
        assertEquals("不应该消耗HP", hpBefore, testPlayer.getCurrentHp());
    }

    /**
     * 测试点射在不同暴击设置下的表现
     */
    @Test
    public void testShotWithDifferentCritRates() {
        // 测试低暴击率
        testPlayer.getBaseAttributes().physicalCritRate = 0.0f; // 0%暴击
        testPlayer.getBaseAttributes().physicalCritDmg = 2.0f;
        testPlayer.markAttributeCacheDirty();

        testMonster.getBaseAttributes().physicalDef = 0;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(testMonster.getBaseAttributes().maxHp);

        ActiveSkill shot = createSkill("shot", 1);
        int damageNoCrit = executeSkillAndDamage(shot, testPlayer, testMonster);

        // 验证无暴击时伤害正确
        int expectedNoCrit = 50; // 50 * 100% = 50
        assertEquals("无暴击时应该造成 " + expectedNoCrit + " 点伤害", expectedNoCrit, damageNoCrit);

        printBattleLogs();
    }

    /**
     * 测试不同等级的伤害递增
     */
    @Test
    public void testShotDamageScaling() {
        // 测试等级1
        testMonster.setCurrentHp(testMonster.getBaseAttributes().maxHp);
        ActiveSkill shot1 = createSkill("shot", 1);
        int damage1 = executeSkillAndDamage(shot1, testPlayer, testMonster);

        // 测试等级5
        testMonster.setCurrentHp(testMonster.getBaseAttributes().maxHp);
        ActiveSkill shot5 = createSkill("shot", 5);
        int damage5 = executeSkillAndDamage(shot5, testPlayer, testMonster);

        // 验证等级5的伤害明显大于等级1
        assertTrue("等级5伤害应该大于等级1", damage5 > damage1);
        System.out.println("等级1伤害: " + damage1 + ", 等级5伤害: " + damage5);
        printBattleLogs();
    }
}
