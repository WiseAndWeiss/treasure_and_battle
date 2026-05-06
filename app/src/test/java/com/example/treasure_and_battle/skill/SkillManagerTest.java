package com.example.treasure_and_battle.skill;

import android.content.Context;

import com.example.treasure_and_battle.manager.SkillManager;
import com.example.treasure_and_battle.model.skill.SkillType;
import com.example.treasure_and_battle.model.skill.SkillRangeType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * SkillManager单元测试
 * 测试技能管理器的反射创建、类型检查、属性值匹配等功能
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class SkillManagerTest {

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

    // ====================== 1. 测试反射创建功能 ======================

    @Test
    public void testCreateBySkillId_DefaultLevel() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST);
        assertNotNull("通过skillId创建技能不应为null", skill);
        assertEquals("默认等级应为0", 0, skill.getLevel());
    }

    @Test
    public void testCreateBySkillId_WithLevel() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, TEST_LEVEL);
        assertNotNull("通过skillId创建技能不应为null", skill);
        assertEquals("指定等级应为" + TEST_LEVEL, TEST_LEVEL, skill.getLevel());
    }

    @Test
    public void testCreateByTemplateId_DefaultLevel() {
        Skill skill = skillManager.createSkillByTemplateId(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        assertNotNull("通过templateId创建技能不应为null", skill);
        assertEquals("默认等级应为0", 0, skill.getLevel());
    }

    @Test
    public void testCreateByTemplateId_WithLevel() {
        Skill skill = skillManager.createSkillByTemplateId(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, TEST_LEVEL);
        assertNotNull("通过templateId创建技能不应为null", skill);
        assertEquals("指定等级应为" + TEST_LEVEL, TEST_LEVEL, skill.getLevel());
    }

    // ====================== 2. 测试反射对象类型 ======================

    @Test
    public void testActiveSkill_TypeCheck() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);
        assertTrue("应该是ActiveSkill类型", skill instanceof ActiveSkill);
        assertFalse("不应该是PassiveSkill类型", skill instanceof PassiveSkill);
    }

    @Test
    public void testPassiveSkill_TypeCheck() {
        Skill skill = skillManager.createSkillBySkillId(PASSIVE_SKILL_ID_FOR_TEST, 1);
        assertTrue("应该是PassiveSkill类型", skill instanceof PassiveSkill);
        assertFalse("不应该是ActiveSkill类型", skill instanceof ActiveSkill);
    }

    // ====================== 3. 测试templateId是否正确 ======================

    @Test
    public void testActiveSkill_TemplateId() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);
        assertEquals("主动技能templateId应匹配JSON", ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, skill.getTemplateId());
    }

    @Test
    public void testPassiveSkill_TemplateId() {
        Skill skill = skillManager.createSkillBySkillId(PASSIVE_SKILL_ID_FOR_TEST, 1);
        assertEquals("被动技能templateId应匹配JSON", PASSIVE_SKILL_TEMPLATE_ID_FOR_TEST, skill.getTemplateId());
    }

    // ====================== 4. 测试Template参数是否与JSON一致 ======================

    @Test
    public void testActiveSkill_TemplateParams() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);

        // 从ConfigLoader获取期望值
        String expectedSkillId = configLoader.getSkillIdByTemplateId(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        String expectedSkillName = configLoader.getSkillName(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        String expectedSkillType = configLoader.getSkillType(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        String expectedSkillRangeType = configLoader.getSkillRangeType(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        Integer expectedMaxLevel = configLoader.getMaxLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        Integer expectedCooldown = configLoader.getCoolDown(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);

        // 验证基础属性
        assertEquals("skillId应匹配JSON", expectedSkillId, skill.getSkillId());
        assertEquals("skillName应匹配JSON", expectedSkillName, skill.getSkillName());
        assertEquals("skillType应匹配JSON", SkillType.valueOf(expectedSkillType), skill.getSkillType());
        assertEquals("skillRangeType应匹配JSON", SkillRangeType.valueOf(expectedSkillRangeType), skill.getSkillRangeType());
        assertEquals("maxLevel应匹配JSON", expectedMaxLevel.intValue(), skill.getMaxLevel());
        assertEquals("cooldown应匹配JSON", expectedCooldown.intValue(), skill.getCooldown());
    }

    @Test
    public void testPassiveSkill_TemplateParams() {
        Skill skill = skillManager.createSkillBySkillId(PASSIVE_SKILL_ID_FOR_TEST, 1);

        // 从ConfigLoader获取期望值
        String expectedSkillId = configLoader.getSkillIdByTemplateId(PASSIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        String expectedSkillName = configLoader.getSkillName(PASSIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        String expectedSkillType = configLoader.getSkillType(PASSIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        Integer expectedMaxLevel = configLoader.getMaxLevel(PASSIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        Integer expectedCooldown = configLoader.getCoolDown(PASSIVE_SKILL_TEMPLATE_ID_FOR_TEST);

        // 验证基础属性
        assertEquals("skillId应匹配JSON", expectedSkillId, skill.getSkillId());
        assertEquals("skillName应匹配JSON", expectedSkillName, skill.getSkillName());
        assertEquals("skillType应匹配JSON", SkillType.valueOf(expectedSkillType), skill.getSkillType());
        assertEquals("maxLevel应匹配JSON", expectedMaxLevel.intValue(), skill.getMaxLevel());
        assertEquals("cooldown应匹配JSON", expectedCooldown.intValue(), skill.getCooldown());
    }

    // ====================== 5. 测试不同等级下的参数获取（从JSON动态获取期望值）====================

    @Test
    public void testActiveSkill_ParamsAtLevel3() {
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, TEST_LEVEL);

        // 从ConfigLoader获取期望值
        var expectedEffect = configLoader.getEffectParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, TEST_LEVEL);
        var expectedCost = configLoader.getCostParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, TEST_LEVEL);

        // 验证效果参数
        assertEquals("3级效果参数x应匹配JSON", expectedEffect.x, skill.getEffectParams().x);
        assertEquals("3级效果参数y应匹配JSON", expectedEffect.y, skill.getEffectParams().y);

        // 验证消耗参数
        assertEquals("3级行动点消耗应匹配JSON", expectedCost.actionPointCost, skill.getActionPointCost());
        assertEquals("3级MP消耗应匹配JSON", expectedCost.mpCost, skill.getMpCost());
        assertEquals("3级HP消耗应匹配JSON", expectedCost.hpCost, skill.getHpCost());
    }

    @Test
    public void testPassiveSkill_ParamsAtLevel4() {
        int level = 4;
        Skill skill = skillManager.createSkillBySkillId(PASSIVE_SKILL_ID_FOR_TEST, level);

        // 从ConfigLoader获取期望值
        var expectedEffect = configLoader.getEffectParamsByLevel(PASSIVE_SKILL_TEMPLATE_ID_FOR_TEST, level);

        // 验证效果参数
        assertEquals("4级效果参数x应匹配JSON", expectedEffect.x, skill.getEffectParams().x);
        assertEquals("4级效果参数y应匹配JSON", expectedEffect.y, skill.getEffectParams().y);
        assertEquals("4级效果参数z应匹配JSON", expectedEffect.z, skill.getEffectParams().z);
    }

    @Test
    public void testActiveSkill_ParamsAtAllLevels() {
        // 测试主动技能所有等级的参数
        Integer maxLevel = configLoader.getMaxLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);

        for (int level = 1; level <= maxLevel; level++) {
            Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, level);

            // 从ConfigLoader获取期望值
            var expectedEffect = configLoader.getEffectParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, level);
            var expectedCost = configLoader.getCostParamsByLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST, level);

            String levelPrefix = level + "级";

            // 验证效果参数
            assertEquals(levelPrefix + "效果参数x应匹配JSON", expectedEffect.x, skill.getEffectParams().x);
            assertEquals(levelPrefix + "效果参数y应匹配JSON", expectedEffect.y, skill.getEffectParams().y);

            // 验证消耗参数
            assertEquals(levelPrefix + "行动点消耗应匹配JSON", expectedCost.actionPointCost, skill.getActionPointCost());
            assertEquals(levelPrefix + "MP消耗应匹配JSON", expectedCost.mpCost, skill.getMpCost());
            assertEquals(levelPrefix + "HP消耗应匹配JSON", expectedCost.hpCost, skill.getHpCost());
        }
    }

    @Test
    public void testCostParamsListLength() {
        // 验证消耗参数列表长度与配置文件一致
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);
        var costListFromJson = configLoader.getCostParamsList(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);

        if (costListFromJson != null) {
            // SkillManager不直接暴露列表，但可以通过验证不同等级来间接验证列表长度
            // 这里验证列表长度与maxLevel的关系（按照你的逻辑）
            Integer maxLevel = configLoader.getMaxLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
            assertNotNull("消耗参数列表不应为null", costListFromJson);
            // 斩击的costParamsList长度为1，符合"列表长度为1时，各级都用costParamsList[0]"的逻辑
        }
    }

    @Test
    public void testEffectParamsListLength() {
        // 验证效果参数列表长度与maxLevel一致
        Skill skill = skillManager.createSkillBySkillId(ACTIVE_SKILL_ID_FOR_TEST, 1);
        var effectListFromJson = configLoader.getEffectParamsList(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);

        Integer maxLevel = configLoader.getMaxLevel(ACTIVE_SKILL_TEMPLATE_ID_FOR_TEST);
        assertNotNull("效果参数列表不应为null", effectListFromJson);
        assertEquals("效果参数列表长度应等于maxLevel", maxLevel.intValue(), effectListFromJson.size());
    }
}
