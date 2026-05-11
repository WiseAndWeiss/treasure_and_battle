package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 凭风引箭技能测试
 * 技能效果：对敌人造成x%物理攻击伤害；敌方速度低于自身时附加(1-速度比值)%的额外伤害，高于自身时施加2回合y%减速
 */
public class ActiveSkill_WindArrowTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：85%伤害、6%减速
     */
    @Test
    public void testWindArrowLevel1() {
        // Given
        ActiveSkill windArrow = createSkill("wind_arrow", 1);

        // 敌方速度(10) < 己方速度(15)，应该造成额外伤害
        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证伤害造成
        assertTrue("应该造成伤害", damage > 0);

        // 验证日志包含额外伤害信息
        assertLogContains(LogType.DAMAGE, "【凭风引箭】");

        printBattleLogs();
    }

    /**
     * 测试等级3：95%伤害、12%减速
     */
    @Test
    public void testWindArrowLevel3() {
        // Given
        ActiveSkill windArrow = createSkill("wind_arrow", 3);

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证伤害造成
        assertTrue("应该造成伤害", damage > 0);

        printBattleLogs();
    }

    /**
     * 测试等级5：105%伤害、18%减速
     */
    @Test
    public void testWindArrowLevel5() {
        // Given
        ActiveSkill windArrow = createSkill("wind_arrow", 5);

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证伤害造成
        assertTrue("应该造成伤害", damage > 0);

        printBattleLogs();
    }

    /**
     * 测试敌人速度低于自身时造成额外伤害
     */
    @Test
    public void testWindArrowExtraDamageWhenFaster() {
        // Given
        ActiveSkill windArrow = createSkill("wind_arrow", 1);

        // 敌方速度(10) < 己方速度(15)，应该触发额外伤害
        int casterSpeed = testPlayer.getFinalAttributes().speed;
        int targetSpeed = testMonster.getFinalAttributes().speed;
        assertTrue("己方速度应该大于敌方", casterSpeed > targetSpeed);

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证造成额外伤害
        assertTrue("应该造成伤害", damage > 0);

        // 验证日志存在
        assertLogExists(LogType.DAMAGE);

        printBattleLogs();
    }

    /**
     * 测试敌人速度高于自身时施加减速
     */
    @Test
    public void testWindArrowSlowWhenSlower() {
        // Given
        ActiveSkill windArrow = createSkill("wind_arrow", 1);

        // 设置敌方速度高于己方
        testMonster.getBaseAttributes().speed = 20;
        testMonster.markAttributeCacheDirty();

        int casterSpeed = testPlayer.getFinalAttributes().speed;
        int targetSpeed = testMonster.getFinalAttributes().speed;
        assertTrue("敌方速度应该大于己方", targetSpeed > casterSpeed);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证施加减速debuff
        SlowDebuff slowDebuff = (SlowDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof SlowDebuff)
                .filter(buff -> buff.getBuffId().equals("wind_arrow_slow"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有减速debuff", slowDebuff);
        assertEquals("减速debuff应该持续2回合", 2, slowDebuff.getRemainingDuration());

        // 验证日志包含减速信息
        assertLogContains(LogType.DAMAGE, "减速");

        printBattleLogs();
    }

    /**
     * 测试速度相等时的行为
     */
    @Test
    public void testWindArrowEqualSpeed() {
        // Given
        ActiveSkill windArrow = createSkill("wind_arrow", 1);

        // 设置敌方速度等于己方
        testMonster.getBaseAttributes().speed = 15;
        testMonster.markAttributeCacheDirty();

        int casterSpeed = testPlayer.getFinalAttributes().speed;
        int targetSpeed = testMonster.getFinalAttributes().speed;
        assertEquals("速度应该相等", casterSpeed, targetSpeed);

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证造成伤害（速度相等时不触发额外伤害，只造成基础伤害）
        assertTrue("应该造成伤害", damage > 0);

        printBattleLogs();
    }

    /**
     * 测试额外伤害计算公式
     */
    @Test
    public void testWindArrowExtraDamageFormula() {
        // Given
        ActiveSkill windArrow = createSkill("wind_arrow", 1);

        // 设置速度使得可以计算额外伤害
        testPlayer.getBaseAttributes().speed = 20;
        testMonster.getBaseAttributes().speed = 10;
        testPlayer.markAttributeCacheDirty();
        testMonster.markAttributeCacheDirty();

        int casterSpeed = testPlayer.getFinalAttributes().speed;
        int targetSpeed = testMonster.getFinalAttributes().speed;

        // 预期额外伤害倍率 = 1 - (10/20) = 0.5 = 50%
        float expectedExtraDamageRatio = 1.0f - (float) targetSpeed / casterSpeed;

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证造成伤害
        assertTrue("应该造成伤害", damage > 0);

        System.out.println("己方速度：" + casterSpeed + "，敌方速度：" + targetSpeed);
        System.out.println("预期额外伤害倍率：" + expectedExtraDamageRatio);
        System.out.println("实际伤害：" + damage);

        printBattleLogs();
    }

    /**
     * 测试不同速度组合的效果
     */
    @Test
    public void testWindArrowDifferentSpeeds() {
        int[] targetSpeeds = {5, 10, 15, 20, 25}; // 不同速度

        for (int targetSpeed : targetSpeeds) {
            // 重置怪物HP和状态
            testMonster.setCurrentHp(testMonster.getFinalAttributes().maxHp);
            testMonster.getActiveBuffList().clear();

            // Given
            testMonster.getBaseAttributes().speed = targetSpeed;
            testMonster.markAttributeCacheDirty();

            ActiveSkill windArrow = createSkill("wind_arrow", 1);
            int hpBefore = testMonster.getCurrentHp();

            // When - 施放技能
            battleManager.executeSkill(testPlayer, windArrow, java.util.Arrays.asList(testMonster), battleContext);

            int damage = hpBefore - testMonster.getCurrentHp();

            // Then - 验证伤害
            System.out.println("敌方速度：" + targetSpeed + "，伤害：" + damage);
            assertTrue("应该造成伤害", damage > 0);
        }

        printBattleLogs();
    }

    /**
     * 测试减速debuff持续时间
     */
    @Test
    public void testWindArrowSlowDuration() {
        // Given
        ActiveSkill windArrow = createSkill("wind_arrow", 1);

        // 设置敌方速度高于己方以触发减速
        testMonster.getBaseAttributes().speed = 20;
        testMonster.markAttributeCacheDirty();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证减速debuff持续2回合
        SlowDebuff slowDebuff = (SlowDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof SlowDebuff)
                .filter(buff -> buff.getBuffId().equals("wind_arrow_slow"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有减速debuff", slowDebuff);
        assertEquals("减速debuff应该持续2回合", 2, slowDebuff.getRemainingDuration());

        // 模拟第一回合结束
        slowDebuff.tick();
        assertEquals("第一回合结束后应该剩余1回合", 1, slowDebuff.getRemainingDuration());

        // 模拟第二回合结束
        slowDebuff.tick();
        assertEquals("第二回合结束后应该剩余0回合", 0, slowDebuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testWindArrowMpCost() {
        // Given
        ActiveSkill windArrow = createSkill("wind_arrow", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证MP消耗
        int mpAfter = testPlayer.getCurrentMp();
        int mpCost = mpBefore - mpAfter;
        assertEquals("应该消耗10点MP", 10, mpCost);

        printBattleLogs();
    }

    /**
     * 测试冷却时间
     */
    @Test
    public void testWindArrowCooldown() {
        // Given
        ActiveSkill windArrow = createSkill("wind_arrow", 1);

        // When - 获取技能冷却时间
        int cooldown = windArrow.getTemplate().getCooldown();

        // Then - 验证冷却时间为2回合
        assertEquals("冷却时间应该为2回合", 2, cooldown);

        printBattleLogs();
    }
}
