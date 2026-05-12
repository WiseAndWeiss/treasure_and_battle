package com.example.treasure_and_battle.profession;

import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.SkillTree;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class ProfessionManagerTest {

    private ProfessionManager professionManager;

    @Before
    public void setUp() {
        professionManager = ProfessionManager.getInstance(RuntimeEnvironment.application);
    }

    /**
     * 测试通过职业类型获取职业模板
     */
    @Test
    public void testGetProfessionTemplate() {
        ProfessionTemplate template = professionManager.getProfessionTemplate(ProfessionType.WARRIOR);

        assertNotNull("职业模板不应该为null", template);
        assertEquals("职业名称应该是战士", "战士", template.getProfessionName());
        assertEquals("职业类型应该是WARRIOR", ProfessionType.WARRIOR, template.getProfessionType());
    }

    /**
     * 测试检查职业是否存在
     */
    @Test
    public void testHasProfession() {
        assertTrue("应该存在战士职业", professionManager.hasProfession(ProfessionType.WARRIOR));
        assertTrue("应该存在法师职业", professionManager.hasProfession(ProfessionType.MAGE));
        assertTrue("应该存在游侠职业", professionManager.hasProfession(ProfessionType.RANGER));
    }

    /**
     * 测试通过职业类型创建Profession实例
     */
    @Test
    public void testCreateProfession() {
        Profession warrior = professionManager.createProfession(ProfessionType.WARRIOR);

        assertNotNull("职业实例不应该为null", warrior);
        assertEquals("职业名称应该是战士", "战士", warrior.getProfessionName());
        assertEquals("职业类型应该是WARRIOR", ProfessionType.WARRIOR, warrior.getProfessionType());
    }

    /**
     * 测试创建不存在的职业
     */
    @Test
    public void testCreateNonExistentProfession() {
        Profession mage = professionManager.createProfession(ProfessionType.MAGE);

        assertNotNull("法师职业现在应存在", mage);
        assertEquals("应该是法师", "法师", mage.getProfessionName());
    }

    /**
     * 测试职业的技能树
     */
    @Test
    public void testProfessionSkillTrees() {
        Profession warrior = professionManager.createProfession(ProfessionType.WARRIOR);

        assertNotNull("主动技能树不应该为null", warrior.getActiveSkillTree());
        assertNotNull("被动技能树不应该为null", warrior.getPassiveSkillTree());
        assertNotNull("事件技能树不应该为null", warrior.getEventSkillTree());
    }

    /**
     * 测试获取职业的所有技能ID
     */
    @Test
    public void testGetAllSkillIds() {
        Profession warrior = professionManager.createProfession(ProfessionType.WARRIOR);

        // 测试主动技能
        assertTrue("应该包含主动技能slash", warrior.getProfessionAllActiveSkillIds().contains("slash"));
        assertTrue("应该包含主动技能battle_stance", warrior.getProfessionAllActiveSkillIds().contains("battle_stance"));

        // 测试被动技能
        assertTrue("应该包含被动技能strong_body", warrior.getProfessionAllPassiveSkillIds().contains("strong_body"));
        assertTrue("应该包含被动技能iron_will", warrior.getProfessionAllPassiveSkillIds().contains("iron_will"));
        assertTrue("应该包含被动技能brave_growth", warrior.getProfessionAllPassiveSkillIds().contains("brave_growth"));

        // 事件技能树目前为空
        assertNotNull("事件技能ID列表不应为null", warrior.getProfessionAllEventSkillIds());
    }

    /**
     * 测试职业技能学习
     */
    @Test
    public void testProfessionSkillLearning() {
        Profession warrior = professionManager.createProfession(ProfessionType.WARRIOR);

        // 学习技能
        boolean success = warrior.levelUpSkill("slash");
        assertTrue("学习slash应该成功", success);
        assertTrue("slash应该已学习", warrior.isLearnedSkill("slash"));

        // 检查技能是否可学习
        assertTrue("battle_stance应该可学习", warrior.isLearnableSkill("battle_stance"));

        // 检查技能是否可以升级
        Skill slashSkill = warrior.getLearnedSkillById("slash");
        assertNotNull("slash技能不应该为null", slashSkill);
        assertTrue("slash应该可以升级", warrior.canLevelUpSkill("slash"));
    }

    /**
     * 测试获取所有职业类型
     */
    @Test
    public void testGetAllProfessionTypes() {
        var professionTypes = professionManager.getAllProfessionTypes();

        assertNotNull("职业类型列表不应该为null", professionTypes);
        assertEquals("应该有3个职业", 3, professionTypes.size());
        assertTrue("应该包含WARRIOR", professionTypes.contains(ProfessionType.WARRIOR));
        assertTrue("应该包含MAGE", professionTypes.contains(ProfessionType.MAGE));
        assertTrue("应该包含RANGER", professionTypes.contains(ProfessionType.RANGER));
    }

    /**
     * 测试获取所有职业模板
     */
    @Test
    public void testGetAllProfessionTemplates() {
        var templates = professionManager.getAllProfessionTemplates();

        assertNotNull("职业模板列表不应该为null", templates);
        assertTrue("应该有至少3个职业模板", templates.size() >= 3);
    }

    /**
     * 测试技能树模板ID的获取
     */
    @Test
    public void testSkillTreeTemplateIds() {
        ProfessionTemplate template = professionManager.getProfessionTemplate(ProfessionType.WARRIOR);

        assertEquals("主动技能树模板ID", "warrior_active_tree", template.getActiveSkillTreeTemplates());
        assertEquals("被动技能树模板ID", "warrior_passive_tree", template.getPassiveSkillTreeTemplates());
        assertEquals("事件技能树模板ID", "warrior_event_tree", template.getEventSkillTreeTemplates());
    }

    /**
     * 测试职业的完整技能学习流程
     */
    @Test
    public void testCompleteSkillLearningFlow() {
        Profession warrior = professionManager.createProfession(ProfessionType.WARRIOR);
        assertNotNull("战士职业不应该为null", warrior);

        // 学习第0层主动技能（无前置）
        boolean success = warrior.levelUpSkill("slash");
        assertTrue("学习slash应该成功", success);

        success = warrior.levelUpSkill("battle_stance");
        assertTrue("学习battle_stance应该成功", success);

        // 学习第0层被动技能（无前置）
        success = warrior.levelUpSkill("strong_body");
        assertTrue("学习strong_body应该成功", success);

        success = warrior.levelUpSkill("tough_guard");
        assertTrue("学习tough_guard应该成功", success);

        // 验证技能已学习
        assertTrue("slash应该已学习", warrior.isLearnedSkill("slash"));
        assertTrue("battle_stance应该已学习", warrior.isLearnedSkill("battle_stance"));
        assertTrue("strong_body应该已学习", warrior.isLearnedSkill("strong_body"));
        assertTrue("tough_guard应该已学习", warrior.isLearnedSkill("tough_guard"));

        // 验证技能等级
        Skill slashSkill = warrior.getLearnedSkillById("slash");
        assertNotNull("slash技能不应该为null", slashSkill);
        assertEquals("slash技能应为1级", 1, slashSkill.getLevel());

        // 验证可以升级已学习的技能
        assertTrue("slash应该可以升级", warrior.canLevelUpSkill("slash"));
        success = warrior.levelUpSkill("slash");
        assertTrue("升级slash到2级应该成功", success);
        assertEquals("slash技能应为2级", 2, slashSkill.getLevel());

        // 验证iron_will不可学习（需要前置bloodthirsty，且层级2未解锁）
        assertFalse("iron_will在未满足前置前不可学习", warrior.isLearnableSkill("iron_will"));

        // 检查已学习的技能数量
        var learnedActiveSkills = warrior.getLearnedActiveSkill();
        var learnedPassiveSkills = warrior.getLearnedPassiveSkill();

        assertTrue("应该有已学习的主动技能", learnedActiveSkills.size() > 0);
        assertTrue("应该有已学习的被动技能", learnedPassiveSkills.size() > 0);

        // 检查总已学习技能
        var allLearnedSkills = warrior.getLearnedSkill();
        assertTrue("应该有已学习的技能", allLearnedSkills.size() >= 4);
    }
}
