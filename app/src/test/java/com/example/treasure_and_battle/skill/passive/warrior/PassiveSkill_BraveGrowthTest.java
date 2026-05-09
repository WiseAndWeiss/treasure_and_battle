package com.example.treasure_and_battle.skill.passive.warrior;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.skill.PassiveSkillManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 越战越勇技能测试
 * 技能效果：每经过一个回合，物理攻击与物理防御额外提升x%（叠加）
 */
public class PassiveSkill_BraveGrowthTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：每回合提升1%
     */
    @Test
    public void testBraveGrowthLevel1() {
        // Given
        PassiveSkill braveGrowth = createPassiveSkill("brave_growth", 1);
        testPlayer.addPassiveSkill(braveGrowth);

        AttributeSet baseAttr = testPlayer.getBaseAttributes();
        float originalPercentAtk = baseAttr.percentPhysicalAtk;
        float originalPercentDef = baseAttr.percentPhysicalDef;

        // When - 经过3个回合
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_ROUND_END);
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_ROUND_END);
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_ROUND_END);

        // Then - 验证百分比提升
        assertEquals("物攻百分比应该提升3%", originalPercentAtk + 0.03f, baseAttr.percentPhysicalAtk, 0.001f);
        assertEquals("物防百分比应该提升3%", originalPercentDef + 0.03f, baseAttr.percentPhysicalDef, 0.001f);

        printBattleLogs();
    }

    /**
     * 测试等级5：每回合提升5%
     */
    @Test
    public void testBraveGrowthLevel5() {
        // Given
        PassiveSkill braveGrowth = createPassiveSkill("brave_growth", 5);
        testPlayer.addPassiveSkill(braveGrowth);

        AttributeSet baseAttr = testPlayer.getBaseAttributes();
        float originalPercentAtk = baseAttr.percentPhysicalAtk;
        float originalPercentDef = baseAttr.percentPhysicalDef;

        // When - 经过1个回合
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_ROUND_END);

        // Then
        assertEquals("物攻百分比应该提升5%", originalPercentAtk + 0.05f, baseAttr.percentPhysicalAtk, 0.001f);
        assertEquals("物防百分比应该提升5%", originalPercentDef + 0.05f, baseAttr.percentPhysicalDef, 0.001f);

        printBattleLogs();
    }

    /**
     * 测试累积效果
     */
    @Test
    public void testCumulativeEffect() {
        // Given
        PassiveSkill braveGrowth = createPassiveSkill("brave_growth", 3); // 3%
        testPlayer.addPassiveSkill(braveGrowth);

        AttributeSet baseAttr = testPlayer.getBaseAttributes();
        float originalPercentAtk = baseAttr.percentPhysicalAtk;

        // When - 经过多个回合
        for (int i = 0; i < 5; i++) {
            PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_ROUND_END);
        }

        // Then - 验证累积提升
        float expectedIncrease = 0.03f * 5; // 5回合 * 3%
        assertEquals("物攻百分比应该累积提升", originalPercentAtk + expectedIncrease, baseAttr.percentPhysicalAtk, 0.001f);

        System.out.println("原始物攻百分比: " + originalPercentAtk);
        System.out.println("5回合后物攻百分比: " + baseAttr.percentPhysicalAtk);
        System.out.println("总提升: " + expectedIncrease);

        printBattleLogs();
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testScalingByLevel() {
        AttributeSet baseAttr = testPlayer.getBaseAttributes();
        float originalPercentAtk = baseAttr.percentPhysicalAtk;

        // 测试等级1（每回合1%）
        testPlayer.addPassiveSkill(createPassiveSkill("brave_growth", 1));
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_ROUND_END);
        float percentAtk1 = baseAttr.percentPhysicalAtk;

        // 测试等级5（每回合5%）
        baseAttr.percentPhysicalAtk = originalPercentAtk;
        battleContext.battleLogs.clear();
        testPlayer.addPassiveSkill(createPassiveSkill("brave_growth", 5));
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_ROUND_END);
        float percentAtk5 = baseAttr.percentPhysicalAtk;

        // 验证等级5的提升更大
        assertTrue("等级5每回合提升应该大于等级1", percentAtk5 > percentAtk1);

        System.out.println("等级1物攻百分比: " + percentAtk1);
        System.out.println("等级5物攻百分比: " + percentAtk5);

        printBattleLogs();
    }

    /**
     * 测试回合结束触发
     */
    @Test
    public void testTriggerOnRoundEnd() {
        // Given
        PassiveSkill braveGrowth = createPassiveSkill("brave_growth", 1);
        testPlayer.addPassiveSkill(braveGrowth);

        // When
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_ROUND_END);

        // Then - 验证日志
        assertLogExists(LogType.BUFF);
        assertLogContains(LogType.BUFF, "【越战越勇】");

        printBattleLogs();
    }

    /**
     * 测试日志记录
     */
    @Test
    public void testBattleLog() {
        // Given
        PassiveSkill braveGrowth = createPassiveSkill("brave_growth", 2);
        testPlayer.addPassiveSkill(braveGrowth);

        // When
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_ROUND_END);

        // Then - 验证日志
        assertLogExists(LogType.BUFF);
        assertLogContains(LogType.BUFF, "【越战越勇】");
        assertLogContains(LogType.BUFF, "物理攻击与物理防御");

        printBattleLogs();
    }

    /**
     * 测试百分比修饰正确应用
     */
    @Test
    public void testPercentageModifierApplied() {
        // Given
        PassiveSkill braveGrowth = createPassiveSkill("brave_growth", 1);
        testPlayer.addPassiveSkill(braveGrowth);

        int baseAtk = testPlayer.getBaseAttributes().physicalAtk; // 50

        // When - 经过1个回合
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_ROUND_END);

        // Then - 验证最终物攻提升
        AttributeSet finalAttr = testPlayer.getFinalAttributes();
        int expectedAtk = (int) (baseAtk * 1.01); // 50 * 1.01

        assertEquals("最终物攻应该提升", expectedAtk, finalAttr.physicalAtk);

        System.out.println("基础物攻: " + baseAtk);
        System.out.println("最终物攻: " + finalAttr.physicalAtk);
        System.out.println("预期物攻: " + expectedAtk);

        printBattleLogs();
    }
}
