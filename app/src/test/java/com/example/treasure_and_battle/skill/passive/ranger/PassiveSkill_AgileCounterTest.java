package com.example.treasure_and_battle.skill.passive.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 轻灵反击技能测试
 * 技能效果：战斗开始时，闪避率提升x%；每次成功闪避攻击时，对攻击者造成y%物理攻击伤害
 */
public class PassiveSkill_AgileCounterTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：战斗开始时闪避率提升2%，反击造成20%伤害
     */
    @Test
    public void testAgileCounterLevel1() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 1);

        float initialDodgeRate = testPlayer.getFinalAttributes().dodgeRate;

        // When - 战斗开始触发
        agileCounter.onBattleStart(testPlayer, battleContext);

        // Then - 验证闪避率提升
        float currentDodgeRate = testPlayer.getFinalAttributes().dodgeRate;
        assertTrue("闪避率应该提升", currentDodgeRate > initialDodgeRate);
        assertEquals("闪避率应该提升2%", 0.02f, currentDodgeRate - initialDodgeRate, 0.001f);

        // 验证日志
        assertLogContains(LogType.BUFF, "【轻灵反击】");

        printBattleLogs();
    }

    /**
     * 测试等级3：战斗开始时闪避率提升6%，反击造成40%伤害
     */
    @Test
    public void testAgileCounterLevel3() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 3);

        float initialDodgeRate = testPlayer.getFinalAttributes().dodgeRate;

        // When - 战斗开始触发
        agileCounter.onBattleStart(testPlayer, battleContext);

        // Then - 验证闪避率提升
        float currentDodgeRate = testPlayer.getFinalAttributes().dodgeRate;
        assertEquals("闪避率应该提升6%", 0.06f, currentDodgeRate - initialDodgeRate, 0.001f);

        printBattleLogs();
    }

    /**
     * 测试等级5：战斗开始时闪避率提升10%，反击造成60%伤害
     */
    @Test
    public void testAgileCounterLevel5() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 5);

        float initialDodgeRate = testPlayer.getFinalAttributes().dodgeRate;

        // When - 战斗开始触发
        agileCounter.onBattleStart(testPlayer, battleContext);

        // Then - 验证闪避率提升
        float currentDodgeRate = testPlayer.getFinalAttributes().dodgeRate;
        assertEquals("闪避率应该提升10%", 0.10f, currentDodgeRate - initialDodgeRate, 0.001f);

        printBattleLogs();
    }

    /**
     * 测试闪避反击伤害
     */
    @Test
    public void testAgileCounterOnDodge() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 1);

        int attackerHpBefore = testMonster.getCurrentHp();

        // When - 闪避触发反击
        agileCounter.onDodge(testPlayer, testMonster, battleContext);

        int attackerHpAfter = testMonster.getCurrentHp();
        int damage = attackerHpBefore - attackerHpAfter;

        // Then - 验证反击造成伤害
        assertTrue("反击应该造成伤害", damage > 0);

        // 验证日志
        assertLogContains(LogType.DAMAGE, "【轻灵反击】");
        assertLogContains(LogType.DAMAGE, "闪避后反击");

        printBattleLogs();
    }

    /**
     * 测试反击伤害数值（等级1为20%物理攻击）
     */
    @Test
    public void testAgileCounterCounterDamage() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 1);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();

        // 设置怪物防御为0以避免伤害减免影响测试
        testMonster.getBaseAttributes().physicalDef = 0;
        testMonster.markAttributeCacheDirty();

        int attackerHpBefore = testMonster.getCurrentHp();

        // When - 闪避触发反击
        agileCounter.onDodge(testPlayer, testMonster, battleContext);

        int attackerHpAfter = testMonster.getCurrentHp();
        int actualDamage = attackerHpBefore - attackerHpAfter;

        // Then - 验证反击伤害约为20点（100物理攻击 * 20% = 20）
        assertTrue("反击伤害应该约为20点", Math.abs(actualDamage - 20) <= 2);

        printBattleLogs();
    }

    /**
     * 测试等级5反击伤害（60%物理攻击）
     */
    @Test
    public void testAgileCounterCounterDamageLevel5() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 5);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();

        // 设置怪物防御为0以避免伤害减免影响测试
        testMonster.getBaseAttributes().physicalDef = 0;
        testMonster.markAttributeCacheDirty();

        int attackerHpBefore = testMonster.getCurrentHp();

        // When - 闪避触发反击
        agileCounter.onDodge(testPlayer, testMonster, battleContext);

        int attackerHpAfter = testMonster.getCurrentHp();
        int actualDamage = attackerHpBefore - attackerHpAfter;

        // Then - 验证反击伤害约为60点（100物理攻击 * 60% = 60）
        assertTrue("反击伤害应该约为60点", Math.abs(actualDamage - 60) <= 3);

        printBattleLogs();
    }

    /**
     * 测试战斗开始和闪避两个触发时机
     */
    @Test
    public void testAgileCounterBothTriggers() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 1);

        // 设置怪物防御为0以避免伤害减免影响测试
        testMonster.getBaseAttributes().physicalDef = 0;
        testMonster.markAttributeCacheDirty();

        float initialDodgeRate = testPlayer.getFinalAttributes().dodgeRate;

        // When - 战斗开始
        agileCounter.onBattleStart(testPlayer, battleContext);

        // Then - 验证闪避率提升
        float dodgeRateAfterBattleStart = testPlayer.getFinalAttributes().dodgeRate;
        assertTrue("战斗开始时闪避率应该提升", dodgeRateAfterBattleStart > initialDodgeRate);

        // When - 闪避反击
        int attackerHpBefore = testMonster.getCurrentHp();
        agileCounter.onDodge(testPlayer, testMonster, battleContext);
        int attackerHpAfter = testMonster.getCurrentHp();

        // Then - 验证反击造成伤害
        assertTrue("反击应该造成伤害", attackerHpAfter < attackerHpBefore);

        printBattleLogs();
    }

    /**
     * 测试多次闪避多次反击
     */
    @Test
    public void testAgileCounterMultipleDodges() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 1);

        // 设置怪物防御为0以避免伤害减免影响测试
        testMonster.getBaseAttributes().physicalDef = 0;
        testMonster.markAttributeCacheDirty();

        int attackerHpBefore = testMonster.getCurrentHp();

        // When - 多次闪避触发反击
        agileCounter.onDodge(testPlayer, testMonster, battleContext);
        agileCounter.onDodge(testPlayer, testMonster, battleContext);
        agileCounter.onDodge(testPlayer, testMonster, battleContext);

        int attackerHpAfter = testMonster.getCurrentHp();
        int totalDamage = attackerHpBefore - attackerHpAfter;

        // Then - 验证每次闪避都造成反击伤害
        // 3次反击，每次10%伤害（默认50物攻的20% = 10点），总计约30点
        assertTrue("多次反击应该造成更高伤害，实际伤害: " + totalDamage, totalDamage >= 30);

        printBattleLogs();
    }

    /**
     * 测试对怪物生效
     */
    @Test
    public void testAgileCounterOnMonster() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 1);

        float initialDodgeRate = testMonster.getFinalAttributes().dodgeRate;

        // When - 战斗开始触发
        agileCounter.onBattleStart(testMonster, battleContext);

        // Then - 验证怪物闪避率也提升
        float currentDodgeRate = testMonster.getFinalAttributes().dodgeRate;
        assertTrue("怪物闪避率应该提升", currentDodgeRate > initialDodgeRate);

        printBattleLogs();
    }
}
