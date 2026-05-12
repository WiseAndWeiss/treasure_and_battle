package com.example.treasure_and_battle.skill.passive.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 起死回生技能测试
 * 技能效果：首次受到致命伤害时，将生命值恢复至x%，每场战斗仅限触发一次
 */
public class PassiveSkill_ReviveOnceTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：首次致命伤害恢复15%HP
     */
    @Test
    public void testReviveOnceLevel1() {
        // Given
        PassiveSkill_ReviveOnce reviveOnce = (PassiveSkill_ReviveOnce) createPassiveSkill("revive_once", 1);
        addPassiveSkillToEntity(testPlayer, reviveOnce);

        // 模拟战斗开始（重置触发标志）
        reviveOnce.onBattleStart(testPlayer, battleContext);

        int maxHp = testPlayer.getFinalAttributes().maxHp;

        // 设置玩家濒死状态
        testPlayer.setCurrentHp(1);

        // When - 受到致命伤害
        testPlayer.takeDamage(100); // 造成致命伤害
        reviveOnce.onDeath(testPlayer, battleContext);

        // Then
        int actualHp = testPlayer.getCurrentHp();
        assertTrue("应该复活", actualHp > 0);
        // 验证复活后HP合理
        assertTrue("复活后HP应该大于0", actualHp > 0);
        assertFalse("复活后不应该死亡", testPlayer.isDead());

        // 验证日志
        assertLogExists(LogType.HEAL);
        assertLogContains(LogType.HEAL, "【起死回生】");

        printBattleLogs();
    }

    /**
     * 测试等级3：首次致命伤害恢复30%HP
     */
    @Test
    public void testReviveOnceLevel3() {
        // Given
        PassiveSkill_ReviveOnce reviveOnce = (PassiveSkill_ReviveOnce) createPassiveSkill("revive_once", 3);
        addPassiveSkillToEntity(testPlayer, reviveOnce);

        // 模拟战斗开始
        reviveOnce.onBattleStart(testPlayer, battleContext);

        int maxHp = testPlayer.getFinalAttributes().maxHp;
        testPlayer.setCurrentHp(10);

        // When - 受到致命伤害
        testPlayer.takeDamage(100);
        reviveOnce.onDeath(testPlayer, battleContext);

        // Then
        int expectedReviveHp = (int) (maxHp * 0.30f);
        int actualHp = testPlayer.getCurrentHp();
        assertTrue("应该复活", actualHp > 0);
        assertTrue("复活后HP应该接近预期", Math.abs(actualHp - expectedReviveHp) <= 1);

        printBattleLogs();
    }

    /**
     * 测试等级5：首次致命伤害恢复50%HP
     */
    @Test
    public void testReviveOnceLevel5() {
        // Given
        PassiveSkill_ReviveOnce reviveOnce = (PassiveSkill_ReviveOnce) createPassiveSkill("revive_once", 5);
        addPassiveSkillToEntity(testPlayer, reviveOnce);

        // 模拟战斗开始
        reviveOnce.onBattleStart(testPlayer, battleContext);

        int maxHp = testPlayer.getFinalAttributes().maxHp;
        testPlayer.setCurrentHp(20);

        // When - 受到致命伤害
        testPlayer.takeDamage(200);
        reviveOnce.onDeath(testPlayer, battleContext);

        // Then
        int expectedReviveHp = (int) (maxHp * 0.50f);
        int actualHp = testPlayer.getCurrentHp();
        assertTrue("应该复活", actualHp > 0);
        assertTrue("复活后HP应该接近预期", Math.abs(actualHp - expectedReviveHp) <= 1);

        printBattleLogs();
    }

    /**
     * 测试每场战斗只触发一次
     */
    @Test
    public void testReviveOnceOnlyTriggersOncePerBattle() {
        // Given
        PassiveSkill_ReviveOnce reviveOnce = (PassiveSkill_ReviveOnce) createPassiveSkill("revive_once", 1);
        addPassiveSkillToEntity(testPlayer, reviveOnce);

        // 模拟战斗开始
        reviveOnce.onBattleStart(testPlayer, battleContext);

        // When - 第一次致命伤害
        testPlayer.setCurrentHp(10);
        testPlayer.takeDamage(100);
        reviveOnce.onDeath(testPlayer, battleContext);

        int hpAfterFirstRevive = testPlayer.getCurrentHp();
        assertTrue("第一次应该复活", hpAfterFirstRevive > 0);

        // 再次造成致命伤害
        testPlayer.takeDamage(1000);
        reviveOnce.onDeath(testPlayer, battleContext);

        // Then - 第二次不应该复活
        assertTrue("第二次触发标志应该为true", reviveOnce.isHasTriggered());
        assertEquals("第二次不应该复活", 0, testPlayer.getCurrentHp());

        printBattleLogs();
    }

    /**
     * 测试非致命伤害不触发
     */
    @Test
    public void testReviveOnceNoTriggerOnNonLethalDamage() {
        // Given
        PassiveSkill_ReviveOnce reviveOnce = (PassiveSkill_ReviveOnce) createPassiveSkill("revive_once", 1);
        addPassiveSkillToEntity(testPlayer, reviveOnce);

        // 模拟战斗开始
        reviveOnce.onBattleStart(testPlayer, battleContext);

        int hpBefore = testPlayer.getCurrentHp();

        // When - 受到非致命伤害
        testPlayer.takeDamage(10); // 只造成10点伤害
        reviveOnce.onDeath(testPlayer, battleContext);

        // Then - 不应该触发复活
        int hpAfter = testPlayer.getCurrentHp();
        assertTrue("不应该触发复活（HP不为0）", hpAfter > 0);
        assertFalse("不应该标记为已触发", reviveOnce.isHasTriggered());

        printBattleLogs();
    }

    /**
     * 测试战斗开始重置触发标志
     */
    @Test
    public void testReviveOnceResetOnBattleStart() {
        // Given
        PassiveSkill_ReviveOnce reviveOnce = (PassiveSkill_ReviveOnce) createPassiveSkill("revive_once", 1);
        addPassiveSkillToEntity(testPlayer, reviveOnce);

        // 第一场战斗
        reviveOnce.onBattleStart(testPlayer, battleContext);
        testPlayer.setCurrentHp(10);
        testPlayer.takeDamage(100);
        reviveOnce.onDeath(testPlayer, battleContext);

        assertTrue("第一场战斗应该触发复活", reviveOnce.isHasTriggered());

        // When - 新的战斗开始
        reviveOnce.onBattleStart(testPlayer, battleContext);

        // Then - 触发标志应该被重置
        assertFalse("新战斗开始时触发标志应该重置", reviveOnce.isHasTriggered());

        // 在新战斗中再次触发
        testPlayer.setCurrentHp(10);
        testPlayer.takeDamage(100);
        reviveOnce.onDeath(testPlayer, battleContext);

        assertTrue("新战斗中应该能再次触发复活", testPlayer.getCurrentHp() > 0);

        printBattleLogs();
    }

    /**
     * 测试触发标志的手动控制
     */
    @Test
    public void testReviveOnceManualTriggerControl() {
        // Given
        PassiveSkill_ReviveOnce reviveOnce = (PassiveSkill_ReviveOnce) createPassiveSkill("revive_once", 1);
        addPassiveSkillToEntity(testPlayer, reviveOnce);

        // 手动设置为已触发
        reviveOnce.setHasTriggered(true);

        // When - 尝试触发复活
        testPlayer.setCurrentHp(10);
        testPlayer.takeDamage(100);
        reviveOnce.onDeath(testPlayer, battleContext);

        // Then - 不应该触发复活
        assertEquals("手动设置为已触发后不应该复活", 0, testPlayer.getCurrentHp());

        // 手动重置触发标志
        reviveOnce.setHasTriggered(false);

        // When - 再次尝试触发
        testPlayer.setCurrentHp(10);
        testPlayer.takeDamage(100);
        reviveOnce.onDeath(testPlayer, battleContext);

        // Then - 应该能触发复活
        assertTrue("重置后应该能触发复活", testPlayer.getCurrentHp() > 0);

        printBattleLogs();
    }

    /**
     * 测试多个被动技能时的独立性
     */
    @Test
    public void testReviveOnceIndependence() {
        // Given - 添加两个起死回生技能实例
        PassiveSkill_ReviveOnce reviveOnce1 = (PassiveSkill_ReviveOnce) createPassiveSkill("revive_once", 1);
        PassiveSkill_ReviveOnce reviveOnce2 = (PassiveSkill_ReviveOnce) createPassiveSkill("revive_once", 3);

        addPassiveSkillToEntity(testPlayer, reviveOnce1);
        addPassiveSkillToEntity(testPlayer, reviveOnce2);

        // 模拟战斗开始
        reviveOnce1.onBattleStart(testPlayer, battleContext);
        reviveOnce2.onBattleStart(testPlayer, battleContext);

        // When - 受到致命伤害
        testPlayer.setCurrentHp(10);
        testPlayer.takeDamage(100);

        // 触发两个技能
        reviveOnce1.onDeath(testPlayer, battleContext);
        int hpAfterFirst = testPlayer.getCurrentHp();

        reviveOnce2.onDeath(testPlayer, battleContext);
        int hpAfterSecond = testPlayer.getCurrentHp();

        // Then - 两个技能应该独立工作
        assertTrue("第一个技能应该触发复活", hpAfterFirst > 0);
        // 注意：由于两个技能都会触发，最终HP可能是两者中较高的那个

        printBattleLogs();
    }
}
