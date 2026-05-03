package com.example.treasure_and_battle.skill.passive.general;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 嗜血技能测试
 * 技能效果：你造成伤害的{x}%会转化为生命值治疗自己
 */
public class PassiveSkill_BloodthirstyTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：5%吸血
     */
    @Test
    public void testBloodthirstyLevel1() {
        resetEntityStates();

        // Given
        PassiveSkill bloodthirsty = createPassiveSkill("bloodthirsty", 1);
        addPassiveSkillToEntity(testPlayer, bloodthirsty);

        testPlayer.setCurrentHp(100); // 设置较低血量便于观察吸血
        int hpBefore = testPlayer.getCurrentHp();
        int damage = 100; // 造成100点伤害

        // When - 模拟攻击造成伤害
        simulateAttackAndDamage(testPlayer, testMonster, damage);

        // Then - 验证吸血效果（5%）
        int expectedHeal = (int) (damage * 5 / 100.0f); // 100 * 5% = 5
        int hpAfter = testPlayer.getCurrentHp();
        int actualHealed = hpAfter - hpBefore;

        assertTrue("应该吸血5点", actualHealed >= 4 && actualHealed <= 6); // 允许误差
        assertEquals("玩家血量应该增加", expectedHeal, actualHealed, 1);

        // 验证日志
        assertLogExists(LogType.HEAL);
        assertLogContains(LogType.HEAL, "【嗜血】");

        printBattleLogs();
    }

    /**
     * 测试等级3：15%吸血
     */
    @Test
    public void testBloodthirstyLevel3() {
        resetEntityStates();

        // Given
        PassiveSkill bloodthirsty = createPassiveSkill("bloodthirsty", 3);
        addPassiveSkillToEntity(testPlayer, bloodthirsty);

        testPlayer.setCurrentHp(150);
        int damage = 100;

        // When
        simulateAttackAndDamage(testPlayer, testMonster, damage);

        // Then - 验证15%吸血
        int expectedHeal = (int) (damage * 15 / 100.0f); // 100 * 15% = 15
        int hpAfter = testPlayer.getCurrentHp();
        int actualHealed = hpAfter - 150;

        assertEquals("应该吸血15点", expectedHeal, actualHealed, 1);

        printBattleLogs();
    }

    /**
     * 测试等级5：25%吸血
     */
    @Test
    public void testBloodthirstyLevel5() {
        resetEntityStates();

        // Given
        PassiveSkill bloodthirsty = createPassiveSkill("bloodthirsty", 5);
        addPassiveSkillToEntity(testPlayer, bloodthirsty);

        testPlayer.setCurrentHp(100);
        testPlayer.getBaseAttributes().maxHp = 200; // 设置最大血量，避免溢出
        testPlayer.markAttributeCacheDirty();
        int damage = 80;

        // When
        simulateAttackAndDamage(testPlayer, testMonster, damage);

        // Then - 验证25%吸血
        int expectedHeal = (    int) (damage * 25 / 100.0f); // 80 * 25% = 20
        int hpAfter = testPlayer.getCurrentHp();
        int actualHealed = hpAfter - 100;

        assertEquals("应该吸血20点", expectedHeal, actualHealed, 1);
        assertTrue("血量不应超过最大值", hpAfter <= 200);

        printBattleLogs();
    }

    /**
     * 测试不造成伤害时不吸血
     */
    @Test
    public void testNoDamageNoLifesteal() {
        resetEntityStates();

        // Given
        PassiveSkill bloodthirsty = createPassiveSkill("bloodthirsty", 5);
        addPassiveSkillToEntity(testPlayer, bloodthirsty);

        testPlayer.setCurrentHp(100);
        int hpBefore = testPlayer.getCurrentHp();

        // When - 造成0伤害
        simulateAttackAndDamage(testPlayer, testMonster, 0);

        // Then - 不应该吸血
        int hpAfter = testPlayer.getCurrentHp();
        assertEquals("造成0伤害时不应该吸血", hpBefore, hpAfter);
    }

    /**
     * 测试满血时不吸血（不会超过最大血量）
     */
    @Test
    public void testFullHpNoOverflow() {
        resetEntityStates();

        // Given
        PassiveSkill bloodthirsty = createPassiveSkill("bloodthirsty", 5);
        addPassiveSkillToEntity(testPlayer, bloodthirsty);

        int maxHp = testPlayer.getFinalAttributes().maxHp;
        testPlayer.setCurrentHp(maxHp); // 满血
        int damage = 100;

        // When
        simulateAttackAndDamage(testPlayer, testMonster, damage);

        // Then - 血量不会超过最大值
        int hpAfter = testPlayer.getCurrentHp();
        assertEquals("满血时吸血不应该超过最大值", maxHp, hpAfter);
    }

    /**
     * 测试吸血不会让死人复活
     */
    @Test
    public void testDeadPlayerNoLifesteal() {
        resetEntityStates();

        // Given
        PassiveSkill bloodthirsty = createPassiveSkill("bloodthirsty", 5);
        addPassiveSkillToEntity(testPlayer, bloodthirsty);

        testPlayer.setDead(true);
        testPlayer.setCurrentHp(0);

        // When
        simulateAttackAndDamage(testPlayer, testMonster, 100);

        // Then - 死亡的玩家不应该吸血
        assertEquals("死亡时不应该吸血", 0, testPlayer.getCurrentHp());
    }

    /**
     * 测试吸血随等级递增
     */
    @Test
    public void testLifestealScaling() {
        resetEntityStates();

        int damage = 100;

        // 测试等级1
        resetEntityStates();
        testPlayer.setCurrentHp(100);
        PassiveSkill bloodthirsty1 = createPassiveSkill("bloodthirsty", 1);
        addPassiveSkillToEntity(testPlayer, bloodthirsty1);
        simulateAttackAndDamage(testPlayer, testMonster, damage);
        int heal1 = testPlayer.getCurrentHp() - 100;

        // 测试等级5
        resetEntityStates();
        testPlayer.setCurrentHp(100);
        PassiveSkill bloodthirsty5 = createPassiveSkill("bloodthirsty", 5);
        addPassiveSkillToEntity(testPlayer, bloodthirsty5);
        simulateAttackAndDamage(testPlayer, testMonster, damage);
        int heal5 = testPlayer.getCurrentHp() - 100;

        // 验证等级5的吸血量大于等级1
        assertTrue("等级5吸血应该大于等级1", heal5 > heal1);

        System.out.println("等级1吸血: " + heal1 + ", 等级5吸血: " + heal5);
    }
}
