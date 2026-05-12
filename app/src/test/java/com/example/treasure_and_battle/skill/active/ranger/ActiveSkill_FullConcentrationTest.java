package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 全神贯注技能测试
 * 技能效果：自身暴击率提升x%，暴击伤害提升y%
 */
public class ActiveSkill_FullConcentrationTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：暴击率提升2%、暴击伤害提升4%
     */
    @Test
    public void testFullConcentrationLevel1() {
        // Given
        ActiveSkill fullConcentration = createSkill("full_concentration", 1);

        float critRateBefore = testPlayer.getFinalAttributes().physicalCritRate;
        float critDmgBefore = testPlayer.getFinalAttributes().physicalCritDmg;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

        // Then
        float critRateAfter = testPlayer.getFinalAttributes().physicalCritRate;
        float critDmgAfter = testPlayer.getFinalAttributes().physicalCritDmg;

        assertTrue("暴击率应该提升", critRateAfter > critRateBefore);
        assertTrue("暴击伤害应该提升", critDmgAfter > critDmgBefore);

        // 验证提升幅度
        float critRateIncrease = critRateAfter - critRateBefore;
        float critDmgIncrease = critDmgAfter - critDmgBefore;

        assertTrue("暴击率提升应该接近2%", Math.abs(critRateIncrease - 0.02f) < 0.01f);
        assertTrue("暴击伤害提升应该接近4%", Math.abs(critDmgIncrease - 0.04f) < 0.01f);

        // 验证日志
        assertLogContains(LogType.BUFF, "【全神贯注】");

        printBattleLogs();
    }

    /**
     * 测试等级3：暴击率提升4%、暴击伤害提升10%
     */
    @Test
    public void testFullConcentrationLevel3() {
        // Given
        ActiveSkill fullConcentration = createSkill("full_concentration", 3);

        float critRateBefore = testPlayer.getFinalAttributes().physicalCritRate;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

        // Then
        float critRateAfter = testPlayer.getFinalAttributes().physicalCritRate;

        // 验证暴击率提升
        float critRateIncrease = critRateAfter - critRateBefore;
        assertTrue("暴击率提升应该接近4%", Math.abs(critRateIncrease - 0.04f) < 0.01f);

        printBattleLogs();
    }

    /**
     * 测试等级5：暴击率提升8%、暴击伤害提升14%
     */
    @Test
    public void testFullConcentrationLevel5() {
        // Given
        ActiveSkill fullConcentration = createSkill("full_concentration", 5);

        float critRateBefore = testPlayer.getFinalAttributes().physicalCritRate;
        float critDmgBefore = testPlayer.getFinalAttributes().physicalCritDmg;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

        // Then
        float critRateAfter = testPlayer.getFinalAttributes().physicalCritRate;
        float critDmgAfter = testPlayer.getFinalAttributes().physicalCritDmg;

        // 验证提升幅度
        float critRateIncrease = critRateAfter - critRateBefore;
        float critDmgIncrease = critDmgAfter - critDmgBefore;

        assertTrue("暴击率提升应该接近8%", Math.abs(critRateIncrease - 0.08f) < 0.01f);
        assertTrue("暴击伤害提升应该接近14%", Math.abs(critDmgIncrease - 0.14f) < 0.01f);

        printBattleLogs();
    }

    /**
     * 测试buff是否正确添加
     */
    @Test
    public void testFullConcentrationBuffsAdded() {
        // Given
        ActiveSkill fullConcentration = createSkill("full_concentration", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

        // Then - 验证暴击率提升buff存在
        AttributeBuff critRateBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("full_concentration_crit_rate"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有暴击率提升buff", critRateBuff);
        assertEquals("暴击率buff应该持续1回合", 1, critRateBuff.getRemainingDuration());

        // 验证暴击伤害提升buff存在
        AttributeBuff critDamageBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("full_concentration_crit_damage"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有暴击伤害提升buff", critDamageBuff);
        assertEquals("暴击伤害buff应该持续1回合", 1, critDamageBuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试buff持续时间
     */
    @Test
    public void testFullConcentrationBuffDuration() {
        // Given
        ActiveSkill fullConcentration = createSkill("full_concentration", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

        // Then - 验证buff持续时间
        AttributeBuff critRateBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("full_concentration_crit_rate"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有暴击率提升buff", critRateBuff);
        assertEquals("buff应该持续1回合", 1, critRateBuff.getRemainingDuration());

        // 模拟回合结束
        critRateBuff.tick();
        assertEquals("回合结束后buff应该被移除", 0, critRateBuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试buff效果确实生效
     */
    @Test
    public void testFullConcentrationBuffEffectiveness() {
        // Given
        ActiveSkill fullConcentration = createSkill("full_concentration", 1);

        // 记录初始暴击率和暴击伤害
        float initialCritRate = testPlayer.getFinalAttributes().physicalCritRate;
        float initialCritDmg = testPlayer.getFinalAttributes().physicalCritDmg;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

        // Then - 验证暴击率和暴击伤害确实改变了
        float currentCritRate = testPlayer.getFinalAttributes().physicalCritRate;
        float currentCritDmg = testPlayer.getFinalAttributes().physicalCritDmg;

        assertNotEquals("暴击率应该改变", initialCritRate, currentCritRate);
        assertNotEquals("暴击伤害应该改变", initialCritDmg, currentCritDmg);

        printBattleLogs();
    }

    /**
     * 测试对自身施放（无目标）
     */
    @Test
    public void testFullConcentrationSelfTarget() {
        // Given
        ActiveSkill fullConcentration = createSkill("full_concentration", 1);

        // When - 对自身施放技能（空目标列表）
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

        // Then - 验证buff添加到施法者
        AttributeBuff critRateBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("full_concentration_crit_rate"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有暴击率提升buff", critRateBuff);

        printBattleLogs();
    }

    /**
     * 测试多次施放不重复添加buff
     */
    @Test
    public void testFullConcentrationNoDuplicateBuffs() {
        // Given
        ActiveSkill fullConcentration = createSkill("full_concentration", 1);

        // When - 施放技能两次
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

        // Then - 应该不会重复添加buff
        long critRateBuffCount = testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("full_concentration_crit_rate"))
                .count();

        long critDamageBuffCount = testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("full_concentration_crit_damage"))
                .count();

        assertEquals("应该只有1个暴击率buff", 1, critRateBuffCount);
        assertEquals("应该只有1个暴击伤害buff", 1, critDamageBuffCount);

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testFullConcentrationMpCost() {
        // Given
        ActiveSkill fullConcentration = createSkill("full_concentration", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

        // Then - 验证MP消耗
        int mpAfter = testPlayer.getCurrentMp();
        int mpCost = mpBefore - mpAfter;
        assertEquals("应该消耗5点MP", 5, mpCost);

        printBattleLogs();
    }

    /**
     * 测试不同等级的MP消耗
     */
    @Test
    public void testFullConcentrationMpCostDifferentLevels() {
        int[] expectedMpCosts = {5, 5, 6, 6, 7};

        for (int level = 1; level <= 5; level++) {
            // 重置MP
            testPlayer.setCurrentMp(testPlayer.getFinalAttributes().maxMp);

            ActiveSkill fullConcentration = createSkill("full_concentration", level);
            int mpBefore = testPlayer.getCurrentMp();

            // When - 施放技能
            battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

            // Then - 验证MP消耗
            int mpAfter = testPlayer.getCurrentMp();
            int mpCost = mpBefore - mpAfter;

            System.out.println("等级" + level + "，MP消耗：" + mpCost + "，预期：" + expectedMpCosts[level - 1]);
            assertEquals("等级" + level + "的MP消耗不正确", expectedMpCosts[level - 1], mpCost);
        }

        printBattleLogs();
    }

    /**
     * 测试暴击属性提升
     */
    @Test
    public void testFullConcentrationCritAttributes() {
        // Given
        ActiveSkill fullConcentration = createSkill("full_concentration", 1);

        // 设置较低的初始暴击率以便观察提升效果
        testPlayer.getBaseAttributes().physicalCritRate = 0.1f;
        testPlayer.markAttributeCacheDirty();

        float initialCritRate = testPlayer.getFinalAttributes().physicalCritRate;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, fullConcentration, java.util.Arrays.asList(), battleContext);

        // Then - 验证暴击率提升
        float finalCritRate = testPlayer.getFinalAttributes().physicalCritRate;
        assertTrue("暴击率应该提升", finalCritRate > initialCritRate);

        printBattleLogs();
    }
}
