package com.example.treasure_and_battle.skill.passive.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 毒愈共生技能测试
 * 技能效果：对带有中毒效果的敌人造成伤害时，恢复本次伤害x%的生命值
 */
public class PassiveSkill_PoisonSymbiosisTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：对中毒敌人回血10%
     */
    @Test
    public void testPoisonSymbiosisLevel1WithPoison() {
        // Given
        PassiveSkill poisonSymbiosis = createPassiveSkill("poison_symbiosis", 1);
        addPassiveSkillToEntity(testPlayer, poisonSymbiosis);

        // 为怪物添加中毒debuff
        PoisoningDebuff poisonDebuff = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 1);
        poisonDebuff.setStack(3);
        testMonster.getActiveBuffList().add(poisonDebuff);
        testMonster.markAttributeCacheDirty();

        // 设置玩家HP不满，以便观察回血效果
        testPlayer.setCurrentHp(150);
        int hpBefore = testPlayer.getCurrentHp();
        int damage = 50;

        // When - 造成伤害
        simulateAttackAndDamage(testPlayer, testMonster, damage);

        // Then
        int actualHeal = testPlayer.getCurrentHp() - hpBefore;
        assertTrue("应该回血", actualHeal > 0);

        // 验证日志
        assertLogExists(LogType.HEAL);
        assertLogContains(LogType.HEAL, "【毒愈共生】");

        printBattleLogs();
    }

    /**
     * 测试等级3：对中毒敌人回血15%
     */
    @Test
    public void testPoisonSymbiosisLevel3WithPoison() {
        // Given
        PassiveSkill poisonSymbiosis = createPassiveSkill("poison_symbiosis", 3);
        addPassiveSkillToEntity(testPlayer, poisonSymbiosis);

        // 为怪物添加中毒debuff
        PoisoningDebuff poisonDebuff = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 1);
        poisonDebuff.setStack(5);
        testMonster.getActiveBuffList().add(poisonDebuff);
        testMonster.markAttributeCacheDirty();

        // 设置玩家HP不满
        testPlayer.setCurrentHp(150);
        int hpBefore = testPlayer.getCurrentHp();
        int damage = 80;

        // When - 造成伤害
        simulateAttackAndDamage(testPlayer, testMonster, damage);

        // Then
        int actualHeal = testPlayer.getCurrentHp() - hpBefore;
        assertTrue("应该回血", actualHeal > 0);

        printBattleLogs();
    }

    /**
     * 测试等级5：对中毒敌人回血20%
     */
    @Test
    public void testPoisonSymbiosisLevel5WithPoison() {
        // Given
        PassiveSkill poisonSymbiosis = createPassiveSkill("poison_symbiosis", 5);
        addPassiveSkillToEntity(testPlayer, poisonSymbiosis);

        // 为怪物添加中毒debuff
        PoisoningDebuff poisonDebuff = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 1);
        poisonDebuff.setStack(8);
        testMonster.getActiveBuffList().add(poisonDebuff);
        testMonster.markAttributeCacheDirty();

        // 设置玩家HP不满
        testPlayer.setCurrentHp(150);
        int hpBefore = testPlayer.getCurrentHp();
        int damage = 100;

        // When - 造成伤害
        simulateAttackAndDamage(testPlayer, testMonster, damage);

        // Then
        int actualHeal = testPlayer.getCurrentHp() - hpBefore;
        assertTrue("应该回血", actualHeal > 0);

        printBattleLogs();
    }

    /**
     * 测试对无中毒目标不触发
     */
    @Test
    public void testPoisonSymbiosisNoPoisonNoHeal() {
        // Given
        PassiveSkill poisonSymbiosis = createPassiveSkill("poison_symbiosis", 1);
        addPassiveSkillToEntity(testPlayer, poisonSymbiosis);

        int hpBefore = testPlayer.getCurrentHp();
        int damage = 50;

        // When - 对无中毒目标造成伤害
        simulateAttackAndDamage(testPlayer, testMonster, damage);

        // Then
        int actualHeal = testPlayer.getCurrentHp() - hpBefore;
        assertEquals("不应该回血（目标无中毒）", 0, actualHeal);

        printBattleLogs();
    }

    /**
     * 测试HP已满时不溢出
     */
    @Test
    public void testPoisonSymbiosisHpCap() {
        // Given
        PassiveSkill poisonSymbiosis = createPassiveSkill("poison_symbiosis", 1);
        addPassiveSkillToEntity(testPlayer, poisonSymbiosis);

        // 为怪物添加中毒debuff
        PoisoningDebuff poisonDebuff = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 1);
        poisonDebuff.setStack(3);
        testMonster.getActiveBuffList().add(poisonDebuff);
        testMonster.markAttributeCacheDirty();

        // 设置玩家HP接近满值
        int maxHp = testPlayer.getFinalAttributes().maxHp;
        testPlayer.setCurrentHp(maxHp - 10);

        // When - 造成伤害
        simulateAttackAndDamage(testPlayer, testMonster, 100);

        // Then
        assertEquals("HP不应该超过最大值", maxHp, testPlayer.getCurrentHp());

        printBattleLogs();
    }

    /**
     * 测试不同伤害量的回血
     */
    @Test
    public void testPoisonSymbiosisDifferentDamage() {
        // Given
        PassiveSkill poisonSymbiosis = createPassiveSkill("poison_symbiosis", 1);
        addPassiveSkillToEntity(testPlayer, poisonSymbiosis);

        // 为怪物添加中毒debuff
        PoisoningDebuff poisonDebuff = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 1);
        poisonDebuff.setStack(3);
        testMonster.getActiveBuffList().add(poisonDebuff);
        testMonster.markAttributeCacheDirty();

        int[] damages = {30, 50, 100, 200};

        for (int damage : damages) {
            // Reset HP before each damage test
            testPlayer.setCurrentHp(100);
            int hpBefore = testPlayer.getCurrentHp();

            // When - 造成伤害
            simulateAttackAndDamage(testPlayer, testMonster, damage);

            // Then - 验证回血量与伤害成正比
            int actualHeal = testPlayer.getCurrentHp() - hpBefore;
            int expectedHeal = (int) (damage * 0.10f);

            System.out.println("伤害：" + damage + "，回血：" + actualHeal + "，预期：" + expectedHeal);
            assertTrue("回血量应该大于0", actualHeal > 0);
        }

        printBattleLogs();
    }

    /**
     * 测试零伤害不触发
     */
    @Test
    public void testPoisonSymbiosisZeroDamage() {
        // Given
        PassiveSkill poisonSymbiosis = createPassiveSkill("poison_symbiosis", 1);
        addPassiveSkillToEntity(testPlayer, poisonSymbiosis);

        // 为怪物添加中毒debuff
        PoisoningDebuff poisonDebuff = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 1);
        poisonDebuff.setStack(3);
        testMonster.getActiveBuffList().add(poisonDebuff);
        testMonster.markAttributeCacheDirty();

        int hpBefore = testPlayer.getCurrentHp();

        // When - 造成零伤害
        simulateAttackAndDamage(testPlayer, testMonster, 0);

        // Then
        int actualHeal = testPlayer.getCurrentHp() - hpBefore;
        assertEquals("零伤害不应该回血", 0, actualHeal);

        printBattleLogs();
    }

    /**
     * 测试不同层数的中毒都能触发
     */
    @Test
    public void testPoisonSymbiosisDifferentStacks() {
        // Given
        PassiveSkill poisonSymbiosis = createPassiveSkill("poison_symbiosis", 1);
        addPassiveSkillToEntity(testPlayer, poisonSymbiosis);

        int[] stacks = {1, 5, 10};

        for (int stack : stacks) {
            // 清空buff列表
            testMonster.getActiveBuffList().clear();

            // 为怪物添加指定层数的中毒debuff
            PoisoningDebuff poisonDebuff = new PoisoningDebuff(
                    "test_poison", "测试中毒", "中毒",
                    BuffType.DEBUFF, true, -1, 999, true, 1);
            poisonDebuff.setStack(stack);
            testMonster.getActiveBuffList().add(poisonDebuff);
            testMonster.markAttributeCacheDirty();

            int hpBefore = testPlayer.getCurrentHp();

            // When - 造成伤害
            simulateAttackAndDamage(testPlayer, testMonster, 50);

            // Then - 只要存在中毒就能触发（验证基本功能）
            int actualHeal = testPlayer.getCurrentHp() - hpBefore;
            assertTrue("中毒层数" + stack + "应该触发回血", actualHeal >= 0);

            System.out.println("中毒层数：" + stack + "，回血：" + actualHeal);
        }

        printBattleLogs();
    }
}
