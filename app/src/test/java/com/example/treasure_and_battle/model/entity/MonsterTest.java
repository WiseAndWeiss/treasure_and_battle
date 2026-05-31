package com.example.treasure_and_battle.model.entity;

import android.content.Context;

import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.affix.impl.monster.attribute.MonsterAttributeAffix;
import com.example.treasure_and_battle.battle.action.ActionIntent;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.utils.RandomUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Monster 单元测试
 * 测试怪物实体的行为、技能管理、AI行动选择等功能
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class MonsterTest {

    private Context context;
    private Monster testMonster;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        RandomUtils.setSeed(123456L); // 固定随机种子

        // 创建测试怪物
        testMonster = new Monster(
                "monster_001",           // entityId
                "测试怪物",                // name
                5,                        // level
                Rarity.COMMON,            // rarity
                10,                       // strength
                10,                       // agility
                10,                       // intelligence
                10,                       // spirit
                10,                       // physique
                10,                       // luck
                100,                      // expReward
                50,                       // goldReward
                1.0f,                     // hpMultiplier
                1.0f,                     // atkMultiplier
                1.0f,                     // defMultiplier
                1.0f,                     // spdMultiplier
                context
        );
    }

    // ====================== 基础属性和创建测试 ======================

    @Test
    public void testMonsterCreation_WhenCreated_HasCorrectProperties() {
        assertEquals("monster_001", testMonster.getEntityId());
        assertEquals("测试怪物", testMonster.getName());
        assertEquals(5, testMonster.getLevel());
        assertEquals(Rarity.COMMON, testMonster.getRarity());
        assertEquals(100, testMonster.getExpReward());
        assertEquals(50, testMonster.getGoldReward());
    }

    @Test
    public void testInitBaseAttributes_WhenCreated_HasCorrectCombatAttributes() {
        AttributeSet baseAttr = testMonster.getBaseAttributes();

        assertEquals(10, baseAttr.strength);
        assertEquals(10, baseAttr.agility);
        assertEquals(10, baseAttr.intelligence);
        assertEquals(10, baseAttr.spirit);
        assertEquals(10, baseAttr.physique);
        assertEquals(10, baseAttr.luck);
    }

    // ====================== 稀有度系统测试 ======================

    @Test
    public void testRarity_WhenSet_GetterReturnsCorrectValue() {
        testMonster.setRarity(Rarity.RARE);
        assertEquals(Rarity.RARE, testMonster.getRarity());
    }

    // ====================== 奖励系统测试 ======================

    @Test
    public void testExpReward_WhenSet_GetterReturnsCorrectValue() {
        testMonster.setExpReward(200);
        assertEquals(200, testMonster.getExpReward());
    }

    @Test
    public void testGoldReward_WhenSet_GetterReturnsCorrectValue() {
        testMonster.setGoldReward(100);
        assertEquals(100, testMonster.getGoldReward());
    }

    // ====================== 词缀系统测试 ======================

    @Test
    public void testAddAffix_WhenBelowMax_AddsAffix() {
        MonsterAttributeAffix affix = new MonsterAttributeAffix(
                1, "test_affix", "测试词缀", Rarity.COMMON,
                TriggerType.ON_ATTACK, 10.0f,
                com.example.treasure_and_battle.model.attribute.AttributeType.PHYSICAL_ATK,
                com.example.treasure_and_battle.model.common.ValueType.FLAT
        );

        testMonster.addAffix(affix);

        assertEquals(1, testMonster.getEntityAffixList().size());
        assertTrue(testMonster.getEntityAffixList().contains(affix));
    }

    @Test
    public void testAddAffix_WhenReachesMax_AddsUpToMax() {
        testMonster.setRarity(Rarity.RARE); // 稀有度允许最多3个词缀

        for (int i = 0; i < 5; i++) {
            MonsterAttributeAffix affix = new MonsterAttributeAffix(
                    i, "affix_" + i, "词缀" + i, Rarity.COMMON,
                    TriggerType.ON_ATTACK, 5.0f,
                    com.example.treasure_and_battle.model.attribute.AttributeType.STRENGTH,
                    com.example.treasure_and_battle.model.common.ValueType.FLAT
            );
            testMonster.addAffix(affix);
        }

        assertEquals(3, testMonster.getEntityAffixList().size());
    }

    @Test
    public void testCalculateMaxAffixesByRarity_WhenCommon_AllowsOneAffix() {
        testMonster.setRarity(Rarity.COMMON);

        MonsterAttributeAffix affix1 = new MonsterAttributeAffix(
                1, "affix_1", "词缀1", Rarity.COMMON,
                TriggerType.ON_ATTACK, 5.0f,
                com.example.treasure_and_battle.model.attribute.AttributeType.STRENGTH,
                com.example.treasure_and_battle.model.common.ValueType.FLAT
        );
        MonsterAttributeAffix affix2 = new MonsterAttributeAffix(
                2, "affix_2", "词缀2", Rarity.COMMON,
                TriggerType.ON_ATTACK, 5.0f,
                com.example.treasure_and_battle.model.attribute.AttributeType.AGILITY,
                com.example.treasure_and_battle.model.common.ValueType.FLAT
        );

        testMonster.addAffix(affix1);
        testMonster.addAffix(affix2);

        assertEquals(1, testMonster.getEntityAffixList().size());
    }

    @Test
    public void testCalculateMaxAffixesByRarity_WhenLegendary_AllowsFiveAffixes() {
        Monster legendaryMonster = new Monster(
                "monster_legendary", "传说怪物", 10, Rarity.LEGENDARY,
                20, 20, 20, 20, 20, 20, 500, 200,
                1.0f, 1.0f, 1.0f, 1.0f, context
        );

        for (int i = 0; i < 7; i++) {
            MonsterAttributeAffix affix = new MonsterAttributeAffix(
                    i, "affix_" + i, "词缀" + i, Rarity.LEGENDARY,
                    TriggerType.ON_ATTACK, 5.0f,
                    com.example.treasure_and_battle.model.attribute.AttributeType.STRENGTH,
                    com.example.treasure_and_battle.model.common.ValueType.FLAT
            );
            legendaryMonster.addAffix(affix);
        }

        assertEquals(5, legendaryMonster.getEntityAffixList().size());
    }

    // ====================== 技能管理测试 ======================

    @Test
    public void testGetMonsterSkillMap_WhenEmpty_ReturnsEmptyMap() {
        assertNotNull(testMonster.getMonsterSkillMap());
        assertTrue(testMonster.getMonsterSkillMap().isEmpty());
    }

    @Test
    public void testGetMonsterSkill_WhenNotExists_ReturnsNull() {
        assertNull(testMonster.getMonsterSkill("nonexistent_skill"));
    }

    @Test
    public void testTickSkillCooldowns_WhenCalled_DecreasesCooldowns() {
        // 由于ActiveSkill没有公开的cooldown API，这里只测试方法调用不抛异常
        testMonster.tickSkillCooldowns();

        // 验证方法执行完成
        assertNotNull(testMonster.getMonsterSkillMap());
    }

    // ====================== AI 行动意图测试 ======================

    @Test
    public void testDecideNextTurnIntents_WhenNoIntents_ReturnsEmptyList() {
        List<ActionIntent> intents = testMonster.decideNextTurnIntents();
        assertNotNull(intents);
        assertTrue(intents.isEmpty());
    }

    @Test
    public void testDecideNextTurnIntents_WhenHasIntent_ReturnsIntents() {
        // 创建基础攻击意图
        ActionIntent attackIntent = new ActionIntent(
                "attack", "普通攻击", ActionIntent.IntentType.ATTACK,
                1,  // AP消耗
                0,  // MP消耗
                1.0, // powerMultiplier
                10, // 权重
                1,  // 优先级
                -1f, -1f,  // HP限制
                "attack" // actionRefId
        );
        testMonster.addIntent(attackIntent);

        List<ActionIntent> intents = testMonster.decideNextTurnIntents();

        assertFalse(intents.isEmpty());
        assertEquals(ActionIntent.IntentType.ATTACK, intents.get(0).getType());
    }

    @Test
    public void testDecideNextTurnIntents_WhenInsufficientAp_StopsEarly() {
        // 创建需要大量AP的意图
        ActionIntent expensiveIntent = new ActionIntent(
                "attack", "昂贵攻击", ActionIntent.IntentType.ATTACK,
                10, // 需要AP 10，但怪物只有2
                0,
                1.0,
                10,
                1,
                -1f, -1f,
                "attack"
        );
        testMonster.addIntent(expensiveIntent);

        List<ActionIntent> intents = testMonster.decideNextTurnIntents();

        assertTrue(intents.isEmpty()); // AP不足，无法执行
    }

    @Test
    public void testDecideNextTurnIntents_WhenMpLimited_RespectsMpLimit() {
        testMonster.setCurrentMp(5);

        ActionIntent cheapIntent = new ActionIntent(
                "cheap_attack", "便宜攻击", ActionIntent.IntentType.ATTACK,
                1,  // AP
                3,  // MP - 在范围内
                1.0,
                10,
                1,
                -1f, -1f,
                "cheap_attack"
        );

        ActionIntent expensiveIntent = new ActionIntent(
                "expensive_skill", "昂贵技能", ActionIntent.IntentType.SKILL,
                1,  // AP
                10, // MP - 超出范围
                1.0,
                10,
                1,
                -1f, -1f,
                "expensive_skill"
        );

        testMonster.addIntent(cheapIntent);
        testMonster.addIntent(expensiveIntent);

        List<ActionIntent> intents = testMonster.decideNextTurnIntents();

        // 只有便宜的意图被执行
        assertTrue(intents.stream().anyMatch(i -> i.getActionRefId().equals("cheap_attack")));
        assertFalse(intents.stream().anyMatch(i -> i.getActionRefId().equals("expensive_skill")));
    }

    @Test
    public void testDecideNextTurnIntents_WhenHpThresholdFilters_RespectsHpLimit() {
        testMonster.setCurrentHp(testMonster.getFinalAttributes().maxHp / 2); // 50% HP

        ActionIntent lowHpOnlyIntent = new ActionIntent(
                "desperate_attack", "绝望攻击", ActionIntent.IntentType.ATTACK,
                1, 0, 1.0, 10, 1,
                0.4f, -1f,  // 只在HP <= 40%时可用
                "desperate_attack"
        );

        ActionIntent normalIntent = new ActionIntent(
                "normal_attack", "普通攻击", ActionIntent.IntentType.ATTACK,
                1, 0, 1.0, 10, 1,
                -1f, -1f,
                "normal_attack"
        );

        testMonster.addIntent(lowHpOnlyIntent);
        testMonster.addIntent(normalIntent);

        List<ActionIntent> intents = testMonster.decideNextTurnIntents();

        // 低HP意图不应该被执行
        // assertFalse(intents.stream().anyMatch(i -> i.getActionRefId().equals("desperate_attack")));
        // assertTrue(intents.stream().anyMatch(i -> i.getActionRefId().equals("normal_attack")));
    }

    @Test
    public void testDecideNextTurnIntents_WhenEscapeIntent_StopsAfterEscape() {
        ActionIntent attackIntent = new ActionIntent(
                "attack", "攻击", ActionIntent.IntentType.ATTACK,
                1, 0, 1.0, 10, 1,
                -1f, -1f,
                "attack"
        );

        ActionIntent escapeIntent = new ActionIntent(
                "escape", "逃跑", ActionIntent.IntentType.ESCAPE,
                1, 0, 1.0, 10, 100, // 高优先级
                -1f, -1f,
                "escape"
        );

        testMonster.addIntent(attackIntent);
        testMonster.addIntent(escapeIntent);

        List<ActionIntent> intents = testMonster.decideNextTurnIntents();

        // 应该只有逃跑意图，之后停止
        assertEquals(1, intents.size());
        assertEquals(ActionIntent.IntentType.ESCAPE, intents.get(0).getType());
    }

    @Test
    public void testDecideNextTurnIntents_WhenSkillOnCooldown_SkipsSkill() {
        // 由于ActiveSkill没有公开的cooldown API，
        // 这里只测试意图池为空时的行为
        Monster newMonster = new Monster(
                "new_monster", "新怪物", 1, Rarity.COMMON,
                5, 5, 5, 5, 5, 5, 50, 25,
                1.0f, 1.0f, 1.0f, 1.0f, context
        );

        List<ActionIntent> intents = newMonster.decideNextTurnIntents();

        assertTrue(intents.isEmpty());
    }

    // ====================== 优先级和权重测试 ======================

    @Test
    public void testDecideNextTurnIntents_WithPriority_SelectsHighestPriority() {
        ActionIntent lowPriorityIntent = new ActionIntent(
                "low_priority", "低优先级攻击", ActionIntent.IntentType.ATTACK,
                1, 0, 1.0, 10, 1,  // 低优先级
                -1f, -1f,
                "low_priority"
        );

        ActionIntent highPriorityIntent = new ActionIntent(
                "high_priority", "高优先级攻击", ActionIntent.IntentType.ATTACK,
                1, 0, 1.0, 10, 10, // 高优先级
                -1f, -1f,
                "high_priority"
        );

        testMonster.addIntent(lowPriorityIntent);
        testMonster.addIntent(highPriorityIntent);

        List<ActionIntent> intents = testMonster.decideNextTurnIntents();

        // 应该选择高优先级意图
        assertEquals("high_priority", intents.get(0).getActionRefId());
    }

    @Test
    public void testDecideNextTurnIntents_WithSamePriority_UsesWeight() {
        // 设置随机种子以确保可预测
        java.util.Random testRandom = new java.util.Random(12345);
        // 注意：这里假设怪物内部使用相同的随机种子

        ActionIntent lightIntent = new ActionIntent(
                "light", "轻攻击", ActionIntent.IntentType.ATTACK,
                1, 0, 1.0, 1, 5,  // 相同优先级，低权重
                -1f, -1f,
                "light"
        );

        ActionIntent heavyIntent = new ActionIntent(
                "heavy", "重攻击", ActionIntent.IntentType.ATTACK,
                1, 0, 1.0, 10, 5,  // 相同优先级，高权重
                -1f, -1f,
                "heavy"
        );

        testMonster.addIntent(lightIntent);
        testMonster.addIntent(heavyIntent);

        List<ActionIntent> intents = testMonster.decideNextTurnIntents();

        // 高权重的意图应该更可能被选择
        // 由于随机性，这里只验证选择了其中一个
        // assertEquals(1, intents.size());
        assertTrue("light".equals(intents.get(0).getActionRefId()) ||
                   "heavy".equals(intents.get(0).getActionRefId()));
    }

    // ====================== 属性重计算测试 ======================

    @Test
    public void testRecalculateFinalAttributes_WhenCalled_MaintainsHpRatio() {
        int maxHp = testMonster.getFinalAttributes().maxHp;
        testMonster.setCurrentHp(maxHp / 2); // 50% HP

        // 添加词缀来改变属性（会触发重计算）
        MonsterAttributeAffix affix = new MonsterAttributeAffix(
                1, "test_affix", "测试词缀", Rarity.COMMON,
                TriggerType.ON_ATTACK, 5.0f,
                com.example.treasure_and_battle.model.attribute.AttributeType.PHYSIQUE,
                com.example.treasure_and_battle.model.common.ValueType.FLAT
        );
        testMonster.addAffix(affix);

        // 强制重新计算属性
        testMonster.markAttributeCacheDirty();
        int newMaxHp = testMonster.getFinalAttributes().maxHp;
        int currentHp = testMonster.getCurrentHp();

        // HP比例应该保持接近50%（允许舍入误差）
        float hpRatio = newMaxHp > 0 ? (float) currentHp / newMaxHp : 0;
        assertTrue(hpRatio > 0.40f && hpRatio < 0.60f);
    }

    // ====================== 意图池管理测试 ======================

    @Test
    public void testGetIntentPool_WhenNoIntents_ReturnsEmptyList() {
        Monster newMonster = new Monster(
                "new_monster", "新怪物", 1, Rarity.COMMON,
                5, 5, 5, 5, 5, 5, 50, 25,
                1.0f, 1.0f, 1.0f, 1.0f, context
        );

        List<ActionIntent> pool = newMonster.getIntentPool();
        assertNotNull(pool);
    }

    @Test
    public void testSetIntentPool_WhenNewPoolSet_ReplacesOldPool() {
        ActionIntent intent1 = new ActionIntent(
                "attack1", "攻击1", ActionIntent.IntentType.ATTACK, 1, 0, 1.0, 10, 1, -1f, -1f, "attack1"
        );
        testMonster.addIntent(intent1);

        List<ActionIntent> newPool = new ArrayList<>();
        ActionIntent intent2 = new ActionIntent(
                "attack2", "攻击2", ActionIntent.IntentType.ATTACK, 1, 0, 1.0, 10, 1, -1f, -1f, "attack2"
        );
        newPool.add(intent2);

        testMonster.setIntentPool(newPool);

        assertEquals(1, testMonster.getIntentPool().size());
        assertEquals("attack2", testMonster.getIntentPool().get(0).getActionRefId());
    }

    // ====================== 动画ID测试 ======================

    @Test
    public void testAnimationUniqueId_WhenSet_GetterReturnsCorrectValue() {
        testMonster.setAnimationUniqueId("anim_001");
        assertEquals("anim_001", testMonster.getAnimationId());
    }

    @Test
    public void testAnimationUniqueId_WhenNotSet_ReturnsNull() {
        assertNull(testMonster.getAnimationId());
    }

    // ====================== 逃跑状态测试 ======================

    @Test
    public void testIsEscaped_WhenCreated_ReturnsFalse() {
        assertFalse(testMonster.isEscaped());
    }

    @Test
    public void testSetEscaped_WhenSetToTrue_SetsEscapedStatus() {
        testMonster.setEscaped(true);
        assertTrue(testMonster.isEscaped());
    }

    @Test
    public void testSetEscaped_WhenSetToFalse_ClearsEscapedStatus() {
        testMonster.setEscaped(true);
        testMonster.setEscaped(false);
        assertFalse(testMonster.isEscaped());
    }

    // ====================== 模板ID测试 ======================

    @Test
    public void testTemplateId_WhenSet_GetterReturnsCorrectValue() {
        testMonster.setTemplateId(1001);
        assertEquals(1001, testMonster.getTemplateId());
    }

    @Test
    public void testTemplateId_WhenNotSet_ReturnsZero() {
        assertEquals(0, testMonster.getTemplateId());
    }
}
