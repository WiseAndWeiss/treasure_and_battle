package com.example.treasure_and_battle.skill;

import android.content.Context;

import com.example.treasure_and_battle.manager.SkillManager;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Skill单元测试
 * 测试技能基类和主动技能的通用功能
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class SkillTest {

    // ====================== 测试常量配置 ======================
    private static final String ACTIVE_SKILL_ID_FOR_TEST = "slash";
    private static final int ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST = 110101;
    private static final String PASSIVE_SKILL_ID_FOR_TEST = "tough_guard";
    private static final int PASSIVE_SKILL_TEMPLATE_ID_FOR_TEST = 210001;
    private static final int TEST_LEVEL = 3;

    private Context context;
    private SkillManager skillManager;
    private ConfigLoader configLoader;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        skillManager = SkillManager.getInstance(context);
        configLoader = ConfigLoader.getInstance(context);
    }

    // ====================== 1. 测试Skill的getter接口 ======================

    @Test
    public void testSkill_Getters() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, TEST_LEVEL);

        // 从ConfigLoader获取期望值
        String expectedSkillName = configLoader.getSkillName(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        String expectedSkillId = configLoader.getSkillIdByTemplateId(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        Integer expectedMaxLevel = configLoader.getMaxLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);

        // 验证getter方法
        assertEquals("getTemplateId应匹配JSON", ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, skill.getTemplateId());
        assertEquals("getSkillId应匹配JSON", expectedSkillId, skill.getSkillId());
        assertEquals("getSkillName应匹配JSON", expectedSkillName, skill.getSkillName());
        assertEquals("getLevel应返回" + TEST_LEVEL, TEST_LEVEL, skill.getLevel());
        assertEquals("getMaxLevel应匹配JSON", expectedMaxLevel.intValue(), skill.getMaxLevel());
        assertNotNull("getTemplate不应返回null", skill.getTemplate());
        assertNotNull("getSimpleDesc不应返回null", skill.getSimpleDesc());
        assertNotNull("getDetailedDesc不应返回null", skill.getDetailedDesc());
    }

    @Test
    public void testSkill_GetCostParams() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);

        // 从ConfigLoader获取期望值
        var expectedCost = configLoader.getCostParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, 1);

        // 验证消耗参数getter
        assertEquals("getActionPointCost应匹配JSON", expectedCost.actionPointCost, skill.getActionPointCost());
        assertEquals("getMpCost应匹配JSON", expectedCost.mpCost, skill.getMpCost());
        assertEquals("getHpCost应匹配JSON", expectedCost.hpCost, skill.getHpCost());
    }

    @Test
    public void testSkill_GetEffectParams() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, TEST_LEVEL);

        // 从ConfigLoader获取期望值
        var expectedEffect = configLoader.getEffectParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, TEST_LEVEL);

        // 验证效果参数getter
        assertEquals("getEffectParams().x应匹配JSON", expectedEffect.x, skill.getEffectParams().x);
        assertEquals("getEffectParams().y应匹配JSON", expectedEffect.y, skill.getEffectParams().y);
    }

    // ====================== 2. 测试isLearned和levelUp ======================

    @Test
    public void testIsLearned_Level0() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 0);
        assertFalse("等级0时isLearned应返回false", skill.isLearned());
    }

    @Test
    public void testIsLearned_Level1() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);
        assertTrue("等级1时isLearned应返回true", skill.isLearned());
    }

    @Test
    public void testLevelUp_From0To1() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 0);

        skill.levelUp();
        assertEquals("升级后等级应为1", 1, skill.getLevel());
        assertTrue("升级后isLearned应返回true", skill.isLearned());
    }

    @Test
    public void testLevelUp_MultipleTimes() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);

        skill.levelUp();
        assertEquals("第1次升级后等级应为2", 2, skill.getLevel());

        skill.levelUp();
        assertEquals("第2次升级后等级应为3", 3, skill.getLevel());

        skill.levelUp();
        assertEquals("第3次升级后等级应为4", 4, skill.getLevel());
    }

    @Test
    public void testLevelUp_ToMaxLevel() {
        Integer maxLevel = configLoader.getMaxLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, maxLevel - 1);

        skill.levelUp();
        assertEquals("升级到最大等级后应为" + maxLevel, maxLevel.intValue(), skill.getLevel());
        assertEquals("等级应等于maxLevel", skill.getMaxLevel(), skill.getLevel());
    }

    @Test
    public void testLevelUp_AtMaxLevel_NoChange() {
        Integer maxLevel = configLoader.getMaxLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, maxLevel);

        skill.levelUp();
        assertEquals("在最大等级升级应保持不变", maxLevel.intValue(), skill.getLevel());
    }

    // ====================== 3. 测试升级后参数是否更新正确 ======================

    @Test
    public void testLevelUp_ParamsUpdate() {
        int fromLevel = 2;
        int toLevel = 3;
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, fromLevel);

        // 从ConfigLoader获取升级前的期望参数
        var expectedBefore = configLoader.getEffectParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, fromLevel);
        assertEquals(fromLevel + "级时x应匹配JSON", expectedBefore.x, skill.getEffectParams().x);
        assertEquals(fromLevel + "级时y应匹配JSON", expectedBefore.y, skill.getEffectParams().y);

        // 升级
        skill.levelUp();

        // 从ConfigLoader获取升级后的期望参数
        var expectedAfter = configLoader.getEffectParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, toLevel);
        assertEquals(toLevel + "级x应匹配JSON", expectedAfter.x, skill.getEffectParams().x);
        assertEquals(toLevel + "级y应匹配JSON", expectedAfter.y, skill.getEffectParams().y);
    }

    @Test
    public void testLevelUp_CostParamsUnchanged() {
        // 测试升级后消耗参数（斩击所有等级消耗相同）
        int fromLevel = 1;
        int toLevel = 2;
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, fromLevel);

        var expectedCostFrom = configLoader.getCostParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, fromLevel);
        int initialApCost = expectedCostFrom.actionPointCost;
        int initialMpCost = expectedCostFrom.mpCost;
        int initialHpCost = expectedCostFrom.hpCost;

        skill.levelUp();

        var expectedCostTo = configLoader.getCostParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, toLevel);
        assertEquals("升级后行动点消耗应匹配JSON", expectedCostTo.actionPointCost, skill.getActionPointCost());
        assertEquals("升级后MP消耗应匹配JSON", expectedCostTo.mpCost, skill.getMpCost());
        assertEquals("升级后HP消耗应匹配JSON", expectedCostTo.hpCost, skill.getHpCost());

        // 对于斩击，所有等级消耗相同
        assertEquals("行动点消耗应与升级前相同", initialApCost, skill.getActionPointCost());
        assertEquals("MP消耗应与升级前相同", initialMpCost, skill.getMpCost());
        assertEquals("HP消耗应与升级前相同", initialHpCost, skill.getHpCost());
    }

    @Test
    public void testLevelUp_AllLevels() {
        // 测试逐级升级，每级参数都正确
        Integer maxLevel = configLoader.getMaxLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 0);

        for (int level = 1; level <= maxLevel; level++) {
            skill.levelUp();
            assertEquals("升级后等级应为" + level, level, skill.getLevel());

            // 从ConfigLoader获取期望值并验证
            var expectedEffect = configLoader.getEffectParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, level);
            assertEquals(level + "级效果参数x应匹配JSON", expectedEffect.x, skill.getEffectParams().x);
            assertEquals(level + "级效果参数y应匹配JSON", expectedEffect.y, skill.getEffectParams().y);

            var expectedCost = configLoader.getCostParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, level);
            assertEquals(level + "级行动点消耗应匹配JSON", expectedCost.actionPointCost, skill.getActionPointCost());
        }
    }

    // ====================== 4. 测试ActiveSkill的CoolDown相关函数 ======================

    @Test
    public void testActiveSkillCooldown_InitialState() {
        ActiveSkill skill = (ActiveSkill) skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);

        assertEquals("初始冷却应为0", 0, skill.getCurrentCooldown());
        assertTrue("初始冷却应就绪", skill.isCooldownReady());
    }

    @Test
    public void testActiveSkillResetCooldown() {
        ActiveSkill skill = (ActiveSkill) skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);

        skill.resetCooldown();
        assertEquals("重置后冷却应等于技能冷却时间", skill.getCooldown(), skill.getCurrentCooldown());

        // 对于cooldown=0的技能，重置后仍然是就绪的
        // 对于cooldown>0的技能，重置后不应就绪
        if (skill.getCooldown() > 0) {
            assertFalse("重置后冷却不应就绪", skill.isCooldownReady());
        } else {
            assertTrue("cooldown=0的技能重置后应始终就绪", skill.isCooldownReady());
        }
    }

    @Test
    public void testActiveSkillClearCooldown() {
        ActiveSkill skill = (ActiveSkill) skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);

        skill.resetCooldown();
        skill.clearCooldown();
        assertEquals("清除后冷却应为0", 0, skill.getCurrentCooldown());
        assertTrue("清除后冷却应就绪", skill.isCooldownReady());
    }

    @Test
    public void testActiveSkillDecreaseCooldown() {
        ActiveSkill skill = (ActiveSkill) skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);

        skill.resetCooldown();
        int initialCooldown = skill.getCurrentCooldown();

        skill.decreaseCooldown();

        // 对于cooldown=0的技能，初始就是0，减少后仍为0
        // 对于cooldown>0的技能，会减少1
        if (initialCooldown > 0) {
            assertEquals("减少后冷却应降低", initialCooldown - 1, skill.getCurrentCooldown());
        } else {
            assertEquals("cooldown=0的技能减少冷却后仍为0", 0, skill.getCurrentCooldown());
        }
    }

    @Test
    public void testActiveSkillDecreaseCooldown_NotNegative() {
        ActiveSkill skill = (ActiveSkill) skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);

        skill.clearCooldown();
        skill.decreaseCooldown();
        assertEquals("冷却不应为负数", 0, skill.getCurrentCooldown());
    }

    @Test
    public void testActiveSkillGetCooldown() {
        ActiveSkill skill = (ActiveSkill) skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);
        Integer expectedCooldown = configLoader.getCoolDown(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        assertEquals("冷却时间应匹配JSON", expectedCooldown.intValue(), skill.getCooldown());
    }

    // ====================== 5. 测试不同等级的参数值（与JSON动态对比）====================

    @Test
    public void testSkill_ParamsAtDifferentLevels() {
        // 测试不同等级的参数值都与JSON一致
        Integer maxLevel = configLoader.getMaxLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);

        for (int level = 0; level <= maxLevel; level++) {
            Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, level);

            // 从ConfigLoader获取期望值
            var expectedEffect = configLoader.getEffectParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, level);
            var expectedCost = configLoader.getCostParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, level);

            String prefix = level + "级";

            // 验证效果参数
            assertEquals(prefix + "效果参数x应匹配JSON", expectedEffect.x, skill.getEffectParams().x);
            assertEquals(prefix + "效果参数y应匹配JSON", expectedEffect.y, skill.getEffectParams().y);

            // 验证消耗参数
            assertEquals(prefix + "行动点消耗应匹配JSON", expectedCost.actionPointCost, skill.getActionPointCost());
            assertEquals(prefix + "MP消耗应匹配JSON", expectedCost.mpCost, skill.getMpCost());
            assertEquals(prefix + "HP消耗应匹配JSON", expectedCost.hpCost, skill.getHpCost());
        }
    }

    @Test
    public void testSkill_Level0HasNoEffect() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 0);

        // 从ConfigLoader获取期望值
        var expectedEffect = configLoader.getEffectParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, 0);
        var expectedCost = configLoader.getCostParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, 0);

        // 验证等级0时参数都是0
        assertEquals("等级0时x应匹配JSON", expectedEffect.x, skill.getEffectParams().x);
        assertEquals("等级0时y应匹配JSON", expectedEffect.y, skill.getEffectParams().y);
        assertEquals("等级0时行动点消耗应匹配JSON", expectedCost.actionPointCost, skill.getActionPointCost());
        assertEquals("等级0时MP消耗应匹配JSON", expectedCost.mpCost, skill.getMpCost());
        assertEquals("等级0时HP消耗应匹配JSON", expectedCost.hpCost, skill.getHpCost());
    }

    @Test
    public void testSkill_EffectParamsMissingFields() {
        // 测试效果参数中缺失的字段默认为0
        // 坚韧护体的某些等级可能没有w字段
        int passiveTemplateId = PASSIVE_SKILL_TEMPLATE_ID_FOR_TEST;

        Skill skill = skillManager.createSkillBySkillId(PASSIVE_SKILL_ID_FOR_TEST, 1);
        var expectedEffect = configLoader.getEffectParamsByLevel(passiveTemplateId, 1);

        // 验证xyzw四个字段，ConfigLoader对缺失字段使用optInt默认为0
        assertEquals("效果参数x应匹配JSON", expectedEffect.x, skill.getEffectParams().x);
        assertEquals("效果参数y应匹配JSON", expectedEffect.y, skill.getEffectParams().y);
        assertEquals("效果参数z应匹配JSON", expectedEffect.z, skill.getEffectParams().z);
        assertEquals("效果参数w应匹配JSON（缺失时为0）", expectedEffect.w, skill.getEffectParams().w);
    }
}
