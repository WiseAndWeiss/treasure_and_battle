package com.example.treasure_and_battle.manager;

import com.example.treasure_and_battle.manager.skill.SkillManager;
import com.example.treasure_and_battle.model.skill.SkillType;
import com.example.treasure_and_battle.model.skill.SkillTreeTemplate;
import com.example.treasure_and_battle.skill.SkillTree;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class SkillManagerSkillTreeTest {

    private SkillManager skillManager;

    @Before
    public void setUp() {
        skillManager = SkillManager.getInstance(RuntimeEnvironment.application);
    }

    /**
     * 测试通过技能树ID获取技能树模板
     */
    @Test
    public void testGetSkillTreeTemplateBySkillTreeId() {
        SkillTreeTemplate template = skillManager.getSkillTreeTemplateBySkillTreeId("warrior_active_tree");

        assertNotNull("技能树模板不应该为null", template);
        assertEquals("技能树ID应该匹配", "warrior_active_tree", template.getSkillTreeId());
        assertEquals("模板ID应该匹配", 10101, template.getTemplateId());
        assertEquals("技能树类型应该为ACTIVE", SkillType.ACTIVE, template.getSkillTreeType());
        assertEquals("最大层级应该为3", 3, template.getMaxLayer());
    }

    /**
     * 测试通过模板ID获取技能树模板
     */
    @Test
    public void testGetSkillTreeTemplateByTemplateId() {
        SkillTreeTemplate template = skillManager.getSkillTreeTemplate(10101);

        assertNotNull("技能树模板不应该为null", template);
        assertEquals("技能树ID应该匹配", "warrior_active_tree", template.getSkillTreeId());
        assertEquals("模板ID应该匹配", 10101, template.getTemplateId());
    }

    /**
     * 测试检查技能树是否存在
     */
    @Test
    public void testHasSkillTree() {
        assertTrue("应该存在warrior_active_tree", skillManager.hasSkillTree("warrior_active_tree"));
        assertFalse("不应该存在不存在的技能树", skillManager.hasSkillTree("non_existent_tree"));
    }

    /**
     * 测试通过技能树ID创建技能树实例
     */
    @Test
    public void testCreateSkillTreeBySkillTreeId() {
        SkillTree skillTree = skillManager.createSkillTreeBySkillTreeId("warrior_active_tree");

        assertNotNull("技能树实例不应该为null", skillTree);
        assertTrue("技能树应该包含slash技能", skillTree.hasSkill("slash"));
        assertTrue("技能树应该包含battle_stance技能", skillTree.hasSkill("battle_stance"));
    }

    /**
     * 测试通过模板ID创建技能树实例
     */
    @Test
    public void testCreateSkillTreeByTemplateId() {
        SkillTree skillTree = skillManager.createSkillTreeByTemplateId(10101);

        assertNotNull("技能树实例不应该为null", skillTree);
        assertTrue("技能树应该包含whirlwind_slash技能", skillTree.hasSkill("whirlwind_slash"));
    }

    /**
     * 测试获取技能树中的所有技能ID
     */
    @Test
    public void testGetAllSkillIds() {
        SkillTree skillTree = skillManager.createSkillTreeBySkillTreeId("warrior_active_tree");

        List<String> allSkillIds = skillTree.getAllSkillIds();

        assertNotNull("技能ID列表不应该为null", allSkillIds);
        assertEquals("应该包含11个技能", 11, allSkillIds.size());
        assertTrue("应该包含slash", allSkillIds.contains("slash"));
        assertTrue("应该包含battle_stance", allSkillIds.contains("battle_stance"));
        assertTrue("应该包含whirlwind_slash", allSkillIds.contains("whirlwind_slash"));
    }

    /**
     * 测试技能节点的前置技能
     */
    @Test
    public void testSkillPrerequisites() {
        SkillTreeTemplate template = skillManager.getSkillTreeTemplateBySkillTreeId("warrior_active_tree");

        // whirlwind_slash的前置技能应该是slash和battle_stance
        List<String> prerequisites = template.getPrerequisiteSkillIds("whirlwind_slash");

        assertNotNull("前置技能列表不应该为null", prerequisites);
        assertEquals("whirlwind_slash应该有2个前置技能", 2, prerequisites.size());
        assertTrue("前置技能应该包含slash", prerequisites.contains("slash"));
        assertTrue("前置技能应该包含battle_stance", prerequisites.contains("battle_stance"));
    }

    /**
     * 测试技能层级
     */
    @Test
    public void testSkillLayers() {
        SkillTreeTemplate template = skillManager.getSkillTreeTemplateBySkillTreeId("warrior_active_tree");

        assertEquals("slash应该在第0层", 0, template.getLayerOfSkill("slash"));
        assertEquals("battle_stance应该在第0层", 0, template.getLayerOfSkill("battle_stance"));
        assertEquals("whirlwind_slash应该在第1层", 1, template.getLayerOfSkill("whirlwind_slash"));
        assertEquals("impenetrable应该在第2层", 2, template.getLayerOfSkill("impenetrable"));
    }

    /**
     * 测试层级解锁所需点数
     */
    @Test
    public void testLayerUnlockPoints() {
        SkillTreeTemplate template = skillManager.getSkillTreeTemplateBySkillTreeId("warrior_active_tree");

        List<Integer> unlockPoints = template.getUnlockLayerNeededPoints();

        assertNotNull("解锁点数列表不应该为null", unlockPoints);
        assertEquals("应该有3个层级的数据", 3, unlockPoints.size());
        assertEquals("第0层解锁需要0点", 0, (int) unlockPoints.get(0));
        assertEquals("第1层解锁需要5点", 5, (int) unlockPoints.get(1));
        assertEquals("第2层解锁需要15点", 15, (int) unlockPoints.get(2));
    }

    /**
     * 测试获取特定层级的技能
     */
    @Test
    public void testGetSkillsInLayer() {
        SkillTreeTemplate template = skillManager.getSkillTreeTemplateBySkillTreeId("warrior_active_tree");

        List<String> layer0Skills = template.getSkillIdsInLayer(0);
        List<String> layer1Skills = template.getSkillIdsInLayer(1);
        List<String> layer2Skills = template.getSkillIdsInLayer(2);

        assertEquals("第0层应该有2个技能", 2, layer0Skills.size());
        assertEquals("第1层应该有5个技能", 5, layer1Skills.size());
        assertEquals("第2层应该有4个技能", 4, layer2Skills.size());

        assertTrue("第0层应该包含slash", layer0Skills.contains("slash"));
        assertTrue("第1层应该包含whirlwind_slash", layer1Skills.contains("whirlwind_slash"));
        assertTrue("第2层应该包含impenetrable", layer2Skills.contains("impenetrable"));
    }

    /**
     * 测试创建不存在的技能树
     */
    @Test
    public void testCreateNonExistentSkillTree() {
        SkillTree skillTree = skillManager.createSkillTreeBySkillTreeId("non_existent_tree");

        assertNull("不存在的技能树应该返回null", skillTree);
    }

    /**
     * 测试学习技能流程
     */
    @Test
    public void testLearnSkillFlow() {
        SkillTree skillTree = skillManager.createSkillTreeBySkillTreeId("warrior_active_tree");

        // 第0层的技能应该可以直接学习
        assertTrue("slash应该可学习", skillTree.isLearnable("slash"));
        assertTrue("battle_stance应该可学习", skillTree.isLearnable("battle_stance"));

        // 第1层的技能需要学习前置技能后才能学习
        assertFalse("whirlwind_slash暂时不可学（需要前置技能）", skillTree.isLearnable("whirlwind_slash"));

        // 学习slash
        boolean learned = skillTree.levelUpSkill("slash");
        assertTrue("学习slash应该成功", learned);
        assertTrue("slash应该已学习", skillTree.isLearned("slash"));

        // 学习battle_stance
        learned = skillTree.levelUpSkill("battle_stance");
        assertTrue("学习battle_stance应该成功", learned);
        assertTrue("battle_stance应该已学习", skillTree.isLearned("battle_stance"));

        // 现在whirlwind_slash应该可学习了（需要足够的技能点解锁第1层）
        // 需要学习足够的技能来解锁第1层（需要5点）
        for (int i = 0; i < 3; i++) {
            skillTree.levelUpSkill("slash"); // 升级slash到4级
        }

        assertTrue("满足条件后whirlwind_slash应该可学习", skillTree.isLearnable("whirlwind_slash"));
    }
}
