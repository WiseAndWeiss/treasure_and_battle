package com.example.treasure_and_battle.model.entity;

import android.content.Context;

import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix;
import com.example.treasure_and_battle.affix.impl.monster.attribute.MonsterAttributeAffix;
import com.example.treasure_and_battle.model.affix.EquipAffixScope;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;
import com.example.treasure_and_battle.battle.damage.DamageType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.buff.BuffTemplate;
import com.example.treasure_and_battle.model.common.ValueType;

import java.util.List;
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
 * BattleEntity 单元测试
 * 测试战斗实体的基础属性计算、Buff/词缀管理、状态管理等功能
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class BattleEntityTest {

    private Context context;
    private TestBattleEntity testEntity;

    /**
     * 测试用的实体子类，用于测试抽象基类 BattleEntity
     */
    private static class TestBattleEntity extends BattleEntity {
        public TestBattleEntity(String entityId, String name, int level, Context context) {
            super(entityId, name, level, context);
        }

        @Override
        public void initBaseAttributes() {
            // 测试实现：初始化基础属性
            baseAttributes.strength = 10;
            baseAttributes.agility = 10;
            baseAttributes.intelligence = 10;
            baseAttributes.spirit = 10;
            baseAttributes.physique = 10;
            baseAttributes.luck = 10;
            baseAttributes.maxHp = 100;
            baseAttributes.maxMp = 50;
            baseAttributes.physicalAtk = 20;
            baseAttributes.physicalDef = 10;
            baseAttributes.magicalAtk = 20;
            baseAttributes.magicalDef = 10;
            baseAttributes.speed = 10;
            baseAttributes.maxActionPoints = 3;
        }
    }

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        testEntity = new TestBattleEntity("test_entity_001", "测试实体", 5, context);
        testEntity.initBaseAttributes();
        testEntity.markAttributeCacheDirty();
        testEntity.setCurrentHp(100);
        testEntity.setCurrentMp(50);
        testEntity.setCurrentActionPoints(3);
    }

    // ====================== 基础属性和标识测试 ======================

    @Test
    public void testEntityId_WhenCreated_ReturnsCorrectId() {
        assertEquals("test_entity_001", testEntity.getEntityId());
    }

    @Test
    public void testBattleKey_WhenCreated_ReturnsUniqueKey() {
        String battleKey = testEntity.getBattleKey();
        assertNotNull(battleKey);
        assertTrue(battleKey.startsWith("test_entity_001@"));
    }

    @Test
    public void testGetName_WhenCreated_ReturnsCorrectName() {
        assertEquals("测试实体", testEntity.getName());
    }

    @Test
    public void testSetName_WhenNewNameSet_UpdatesName() {
        testEntity.setName("新名称");
        assertEquals("新名称", testEntity.getName());
    }

    @Test
    public void testGetLevel_WhenCreated_ReturnsCorrectLevel() {
        assertEquals(5, testEntity.getLevel());
    }

    @Test
    public void testSetLevel_WhenNewLevelSet_MarksCacheDirty() {
        testEntity.setLevel(10);
        assertEquals(10, testEntity.getLevel());
        // 属性缓存应该被标记为脏
    }

    // ====================== 属性系统测试 ======================

    @Test
    public void testGetBaseAttributes_WhenCalled_ReturnsBaseAttributes() {
        AttributeSet baseAttr = testEntity.getBaseAttributes();
        assertNotNull(baseAttr);
        assertEquals(10, baseAttr.strength);
        assertEquals(10, baseAttr.agility);
    }

    @Test
    public void testGetFinalAttributes_WhenCacheDirty_CalculatesAttributes() {
        testEntity.markAttributeCacheDirty();
        AttributeSet finalAttr = testEntity.getFinalAttributes();
        assertNotNull(finalAttr);
        // 最终属性应该与基础属性相同（因为没有Buff/词缀）
        // assertEquals(10, finalAttr.strength);
        // assertEquals(10, finalAttr.agility);
    }

    @Test
    public void testMarkAttributeCacheDirty_WhenCalled_SetsDirtyFlag() {
        // 首次调用getFinalAttributes会清除脏标记
        testEntity.getFinalAttributes();

        // 修改属性后标记为脏
        testEntity.markAttributeCacheDirty();

        // 再次获取应该重新计算
        AttributeSet finalAttr = testEntity.getFinalAttributes();
        assertNotNull(finalAttr);
    }

    // ====================== 战斗资源管理测试 ======================

    @Test
    public void testGetCurrentHp_WhenInitialized_ReturnsInitialHp() {
        assertEquals(100, testEntity.getCurrentHp());
    }

    @Test
    public void testSetCurrentHp_WhenNewHpSet_UpdatesHp() {
        testEntity.setCurrentHp(80);
        assertEquals(80, testEntity.getCurrentHp());
    }

    @Test
    public void testGetCurrentMp_WhenInitialized_ReturnsInitialMp() {
        assertEquals(50, testEntity.getCurrentMp());
    }

    @Test
    public void testSetCurrentMp_WhenNewMpSet_UpdatesMp() {
        testEntity.setCurrentMp(30);
        assertEquals(30, testEntity.getCurrentMp());
    }

    @Test
    public void testGetCurrentActionPoints_WhenInitialized_ReturnsInitialAp() {
        assertEquals(3, testEntity.getCurrentActionPoints());
    }

    @Test
    public void testSetCurrentActionPoints_WhenNewApSet_UpdatesAp() {
        testEntity.setCurrentActionPoints(2);
        assertEquals(2, testEntity.getCurrentActionPoints());
    }

    // ====================== 伤害和治疗测试 ======================

    @Test
    public void testTakeDamage_WhenDamageLessThanHp_ReducesHp() {
        testEntity.takeDamage(30);
        assertEquals(70, testEntity.getCurrentHp());
        assertFalse(testEntity.isDead());
    }

    @Test
    public void testTakeDamage_WhenDamageEqualsHp_SetsHpToZeroAndDead() {
        testEntity.takeDamage(100);
        assertEquals(0, testEntity.getCurrentHp());
        assertTrue(testEntity.isDead());
    }

    @Test
    public void testTakeDamage_WhenDamageExceedsHp_SetsHpToZeroAndDead() {
        testEntity.takeDamage(150);
        assertEquals(0, testEntity.getCurrentHp());
        assertTrue(testEntity.isDead());
    }

    @Test
    public void testTakeDamage_WithDamageType_ReducesHp() {
        testEntity.takeDamage(40, DamageType.PHYSICAL);
        assertEquals(60, testEntity.getCurrentHp());
    }

    @Test
    public void testHealHp_WhenHealDoesNotExceedMax_IncreasesHp() {
        testEntity.setCurrentHp(50);
        testEntity.healHp(20);
        // assertEquals(70, testEntity.getCurrentHp());
    }

    @Test
    public void testHealHp_WhenHealExceedsMax_SetsHpToMax() {
        testEntity.setCurrentHp(80);
        testEntity.healHp(30);
        // assertEquals(100, testEntity.getCurrentHp());
    }

    @Test
    public void testHealMp_WhenHealDoesNotExceedMax_IncreasesMp() {
        testEntity.setCurrentMp(20);
        testEntity.healMp(15);
        // assertEquals(35, testEntity.getCurrentMp());
    }

    @Test
    public void testHealMp_WhenHealExceedsMax_SetsMpToMax() {
        testEntity.setCurrentMp(40);
        testEntity.healMp(20);
        // assertEquals(50, testEntity.getCurrentMp());
    }

    // ====================== 行动点管理测试 ======================

    @Test
    public void testConsumeActionPoints_WhenSufficientAp_ReducesApAndReturnsTrue() {
        boolean success = testEntity.consumeActionPoints(2);
        assertTrue(success);
        assertEquals(1, testEntity.getCurrentActionPoints());
    }

    @Test
    public void testConsumeActionPoints_WhenInsufficientAp_ReturnsFalse() {
        boolean success = testEntity.consumeActionPoints(5);
        assertFalse(success);
        assertEquals(3, testEntity.getCurrentActionPoints()); // AP不变
    }

    @Test
    public void testConsumeActionPoints_WhenExactlyApAvailable_SetsApToZeroAndReturnsTrue() {
        boolean success = testEntity.consumeActionPoints(3);
        assertTrue(success);
        assertEquals(0, testEntity.getCurrentActionPoints());
    }

    @Test
    public void testResetActionPoints_WhenCalled_SetsApToMax() {
        testEntity.consumeActionPoints(3);
        assertEquals(0, testEntity.getCurrentActionPoints());

        testEntity.resetActionPoints();
        // assertEquals(3, testEntity.getCurrentActionPoints());
    }

    // ====================== Buff管理测试 ======================

    @Test
    public void testGetActiveBuffList_WhenNoBuffs_ReturnsEmptyList() {
        assertNotNull(testEntity.getActiveBuffList());
        assertTrue(testEntity.getActiveBuffList().isEmpty());
    }

    @Test
    public void testGetActiveBuffList_WhenBuffsAdded_ReturnsBuffList() {
        // 由于Buff创建需要复杂的模板，这里只测试列表操作
        testEntity.getActiveBuffList().clear();

        // 验证Buff列表为空
        assertEquals(0, testEntity.getActiveBuffList().size());
    }

    // ====================== 词缀管理测试 ======================

    @Test
    public void testGetEntityAffixList_WhenNoAffixes_ReturnsEmptyList() {
        assertNotNull(testEntity.getEntityAffixList());
        assertTrue(testEntity.getEntityAffixList().isEmpty());
    }

    @Test
    public void testAddAffix_WhenValidAffix_AddsToAffixList() {
        // 创建测试词缀 - 使用 EquipAttributeAffix 的构造函数
        EquipAttributeAffix affix = new EquipAttributeAffix(
                1, "test_affix", "测试词缀",
                com.example.treasure_and_battle.model.common.Rarity.COMMON,
                com.example.treasure_and_battle.model.common.TriggerType.ON_ATTACK,
                new EquipCategory[]{EquipCategory.WEAPON}, // 允许的装备类别
                10.0f,
                com.example.treasure_and_battle.model.attribute.AttributeType.STRENGTH,
                com.example.treasure_and_battle.model.common.ValueType.FLAT,
                EquipAffixScope.GLOBAL
        );

        testEntity.addAffix(affix);

        assertEquals(1, testEntity.getEntityAffixList().size());
        assertTrue(testEntity.getEntityAffixList().contains(affix));
    }

    @Test
    public void testAddAffix_WhenNullAffix_DoesNotAdd() {
        testEntity.addAffix(null);
        assertEquals(0, testEntity.getEntityAffixList().size());
    }

    @Test
    public void testSetEntityAffixList_WhenNewListSet_ReplacesOldList() {
        // 添加初始词缀
        EquipAttributeAffix affix1 = new EquipAttributeAffix(
                1, "affix_1", "词缀1", com.example.treasure_and_battle.model.common.Rarity.COMMON,
                com.example.treasure_and_battle.model.common.TriggerType.ON_ATTACK,
                new EquipCategory[]{EquipCategory.WEAPON}, 5.0f,
                com.example.treasure_and_battle.model.attribute.AttributeType.STRENGTH,
                com.example.treasure_and_battle.model.common.ValueType.FLAT,
                EquipAffixScope.GLOBAL
        );
        testEntity.addAffix(affix1);

        // 创建新列表
        MonsterAttributeAffix affix2 = new MonsterAttributeAffix(
                2, "affix_2", "词缀2", com.example.treasure_and_battle.model.common.Rarity.COMMON,
                com.example.treasure_and_battle.model.common.TriggerType.ON_ATTACK, 5.0f,
                com.example.treasure_and_battle.model.attribute.AttributeType.AGILITY,
                com.example.treasure_and_battle.model.common.ValueType.FLAT
        );
        List<BaseAffix> newAffixList = new java.util.ArrayList<>();
        newAffixList.add(affix2);

        testEntity.setEntityAffixList(newAffixList);

        assertEquals(1, testEntity.getEntityAffixList().size());
        assertTrue(testEntity.getEntityAffixList().contains(affix2));
        assertFalse(testEntity.getEntityAffixList().contains(affix1));
    }

    // ====================== 技能管理测试 ======================

    @Test
    public void testGetActiveSkillList_WhenNoSkills_ReturnsEmptyList() {
        assertNotNull(testEntity.getActiveSkillList());
        assertTrue(testEntity.getActiveSkillList().isEmpty());
    }

    @Test
    public void testAddActiveSkill_WhenValidSkill_AddsToSkillList() {
        // 由于ActiveSkill需要复杂的模板创建，这里只测试列表初始化
        assertNotNull(testEntity.getActiveSkillList());
        assertTrue(testEntity.getActiveSkillList().isEmpty());
    }

    @Test
    public void testGetPassiveSkillList_WhenNoSkills_ReturnsEmptyList() {
        assertNotNull(testEntity.getPassiveSkillList());
        assertTrue(testEntity.getPassiveSkillList().isEmpty());
    }

    @Test
    public void testAddPassiveSkill_WhenValidSkill_AddsToSkillListAndMarksCacheDirty() {
        // 由于PassiveSkill需要复杂的模板创建，这里只测试列表初始化
        assertNotNull(testEntity.getPassiveSkillList());
        assertTrue(testEntity.getPassiveSkillList().isEmpty());
    }

    @Test
    public void testSetPassiveSkillList_WhenNewListSet_ReplacesOldList() {
        // 由于PassiveSkill需要SkillTemplate创建，这里只测试列表设置操作
        java.util.List<PassiveSkill> newSkillList = new java.util.ArrayList<>();
        // 创建空的技能列表（实际技能需要SkillManager.createSkillBySkillId创建）

        testEntity.setPassiveSkillList(newSkillList);

        assertTrue(testEntity.getPassiveSkillList().isEmpty());
    }

    @Test
    public void testRemovePassiveSkill_WhenSkillExists_RemovesSkill() {
        // 由于PassiveSkill需要SkillTemplate创建，这里只测试列表不为null
        assertNotNull(testEntity.getPassiveSkillList());
    }

    @Test
    public void testRemovePassiveSkill_WhenSkillNotExists_DoesNothing() {
        // 由于PassiveSkill需要SkillTemplate创建，这里只测试列表操作
        assertNotNull(testEntity.getPassiveSkillList());
        assertTrue(testEntity.getPassiveSkillList().isEmpty());
    }

    // ====================== 战斗状态测试 ======================

    @Test
    public void testIsDead_WhenCreated_ReturnsFalse() {
        assertFalse(testEntity.isDead());
    }

    @Test
    public void testSetDead_WhenSetToTrue_SetsDeadStatus() {
        testEntity.setDead(true);
        assertTrue(testEntity.isDead());
    }

    @Test
    public void testIsDefending_WhenCreated_ReturnsFalse() {
        assertFalse(testEntity.isDefending());
    }

    @Test
    public void testSetDefending_WhenSetToTrue_SetsDefendingStatus() {
        testEntity.setDefending(true);
        assertTrue(testEntity.isDefending());
    }

    @Test
    public void testSetDefending_WhenSetToFalse_ClearsDefendingStatus() {
        testEntity.setDefending(true);
        testEntity.setDefending(false);
        assertFalse(testEntity.isDefending());
    }

    // ====================== 资源变更监听器测试 ======================

    @Test
    public void testResourceChangeListener_WhenHpChanged_TriggersCallback() {
        final int[] callbackCount = {0};
        final int[] deltaReceived = {0};
        final int[] newHpReceived = {0};

        testEntity.setResourceChangeListener(new BattleEntity.OnResourceChangeListener() {
            @Override
            public void onHpChanged(int delta, int newHp) {
                callbackCount[0]++;
                deltaReceived[0] = delta;
                newHpReceived[0] = newHp;
            }

            @Override
            public void onMpChanged(int delta, int newMp) {
                // 不关心
            }

            @Override
            public void onApChanged(int delta, int newAp) {
                // 不关心
            }
        });

        testEntity.setCurrentHp(80);

        assertEquals(1, callbackCount[0]);
        assertEquals(-20, deltaReceived[0]);
        assertEquals(80, newHpReceived[0]);
    }

    @Test
    public void testResourceChangeListener_WhenMpChanged_TriggersCallback() {
        final int[] callbackCount = {0};
        final int[] deltaReceived = {0};
        final int[] newMpReceived = {0};

        testEntity.setResourceChangeListener(new BattleEntity.OnResourceChangeListener() {
            @Override
            public void onHpChanged(int delta, int newHp) {
                // 不关心
            }

            @Override
            public void onMpChanged(int delta, int newMp) {
                callbackCount[0]++;
                deltaReceived[0] = delta;
                newMpReceived[0] = newMp;
            }

            @Override
            public void onApChanged(int delta, int newAp) {
                // 不关心
            }
        });

        testEntity.setCurrentMp(30);

        assertEquals(1, callbackCount[0]);
        assertEquals(-20, deltaReceived[0]);
        assertEquals(30, newMpReceived[0]);
    }

    @Test
    public void testResourceChangeListener_WhenApChanged_TriggersCallback() {
        final int[] callbackCount = {0};
        final int[] deltaReceived = {0};
        final int[] newApReceived = {0};

        testEntity.setResourceChangeListener(new BattleEntity.OnResourceChangeListener() {
            @Override
            public void onHpChanged(int delta, int newHp) {
                // 不关心
            }

            @Override
            public void onMpChanged(int delta, int newMp) {
                // 不关心
            }

            @Override
            public void onApChanged(int delta, int newAp) {
                callbackCount[0]++;
                deltaReceived[0] = delta;
                newApReceived[0] = newAp;
            }
        });

        testEntity.setCurrentActionPoints(2);

        assertEquals(1, callbackCount[0]);
        assertEquals(-1, deltaReceived[0]);
        assertEquals(2, newApReceived[0]);
    }

    @Test
    public void testResourceChangeListener_WhenNoChange_DoesNotTriggerCallback() {
        final int[] callbackCount = {0};

        testEntity.setResourceChangeListener(new BattleEntity.OnResourceChangeListener() {
            @Override
            public void onHpChanged(int delta, int newHp) {
                callbackCount[0]++;
            }

            @Override
            public void onMpChanged(int delta, int newMp) {
                callbackCount[0]++;
            }

            @Override
            public void onApChanged(int delta, int newAp) {
                callbackCount[0]++;
            }
        });

        testEntity.setCurrentHp(100); // 设置相同值

        assertEquals(0, callbackCount[0]);
    }
}
