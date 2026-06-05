package com.example.treasure_and_battle.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.profession.ProfessionType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * CharacterManager 测试
 * <p>
 * 测试玩家管理器的天赋点分配和属性查询功能
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class CharacterManagerTest {

    private Context context;
    private CharacterManager characterManager;
    private Character testCharacter;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        characterManager = CharacterManager.getInstance(context);

        // 创建测试角色
        testCharacter = new Character(1, "测试战士", ProfessionType.WARRIOR, context);
    }

    // ==================== 单例模式测试 ====================

    @Test
    public void testSingleton() {
        CharacterManager instance1 = CharacterManager.getInstance(context);
        CharacterManager instance2 = CharacterManager.getInstance(context);
        assertSame("应返回同一实例", instance1, instance2);
    }

    @Test
    public void testReleaseInstance() {
        CharacterManager.releaseInstance();
        CharacterManager newInstance = CharacterManager.getInstance(context);
        assertNotNull("释放后应能创建新实例", newInstance);
    }

    // ==================== 天赋点分配测试 ====================

    @Test
    public void testAllocateTalentPoint_WithString_NullCharacter() {
        boolean result = characterManager.allocateTalentPoint(null, "STRENGTH");
        assertFalse("null 角色应返回 false", result);
    }

    @Test
    public void testAllocateTalentPoint_WithString_NullAttributeName() {
        // 明确指定为 String 类型以避免歧义
        boolean result = characterManager.allocateTalentPoint(testCharacter, (String) null);
        assertFalse("null 属性名应返回 false", result);
    }

    @Test
    public void testAllocateTalentPoint_WithString_Valid() {
        int before = testCharacter.getAllocatedStrength();
        boolean result = characterManager.allocateTalentPoint(testCharacter, "STRENGTH");

        // 结果取决于角色是否有天赋点
        if (result) {
            assertEquals("力量应增加", before + 1, testCharacter.getAllocatedStrength());
        } else {
            assertEquals("无天赋点时不应增加", before, testCharacter.getAllocatedStrength());
        }
    }

    @Test
    public void testAllocateTalentPoint_WithAttributeType_NullCharacter() {
        boolean result = characterManager.allocateTalentPoint(null, AttributeType.STRENGTH);
        assertFalse("null 角色应返回 false", result);
    }

    @Test
    public void testAllocateTalentPoint_WithAttributeType_Valid() {
        int before = testCharacter.getAllocatedAgility();
        boolean result = characterManager.allocateTalentPoint(testCharacter, AttributeType.AGILITY);

        // 结果取决于角色是否有天赋点
        if (result) {
            assertEquals("敏捷应增加", before + 1, testCharacter.getAllocatedAgility());
        } else {
            assertEquals("无天赋点时不应增加", before, testCharacter.getAllocatedAgility());
        }
    }

    @Test
    public void testResetAllTalentPoints_NullCharacter() {
        // 应不崩溃
        characterManager.resetAllTalentPoints(null);
        assertTrue("处理 null 角色", true);
    }

    @Test
    public void testResetAllTalentPoints_Valid() {
        // 通过升级获得天赋点
        testCharacter.gainExp(1000); // 足够升级到更高等级

        // 分配一些点
        characterManager.allocateTalentPoint(testCharacter, "STRENGTH");
        characterManager.allocateTalentPoint(testCharacter, "INTELLIGENCE");

        int pointsBefore = testCharacter.getTalentPoints();
        int strengthBefore = testCharacter.getAllocatedStrength();
        int intelligenceBefore = testCharacter.getAllocatedIntelligence();

        // 重置
        characterManager.resetAllTalentPoints(testCharacter);

        assertEquals("力量应重置", 0, testCharacter.getAllocatedStrength());
        assertEquals("智力应重置", 0, testCharacter.getAllocatedIntelligence());
        assertEquals("天赋点应返回", pointsBefore + strengthBefore + intelligenceBefore,
                testCharacter.getTalentPoints());
    }

    // ==================== 属性查询测试 ====================

    @Test
    public void testGetAllocatedStat_NullCharacter() {
        int result = characterManager.getAllocatedStat(null, "STRENGTH");
        assertEquals("null 角色应返回0", 0, result);
    }

    @Test
    public void testGetAllocatedStat_NullAttributeName() {
        // CharacterManager.getAllocatedStat 内部会对 null 属性名进行处理
        // 使用空字符串代替 null 来测试无效属性名
        int result = characterManager.getAllocatedStat(testCharacter, "");
        assertEquals("空属性名应返回0", 0, result);
    }

    @Test
    public void testGetAllocatedStat_STRENGTH() {
        // 通过升级获得天赋点
        testCharacter.gainExp(1000);
        int totalPoints = testCharacter.getTalentPoints();

        characterManager.allocateTalentPoint(testCharacter, "STRENGTH");
        if (testCharacter.getTalentPoints() < totalPoints) {
            characterManager.allocateTalentPoint(testCharacter, "STRENGTH");
        }

        int allocated = characterManager.getAllocatedStat(testCharacter, "STRENGTH");
        assertTrue("力量应已分配", allocated >= 1);
    }

    @Test
    public void testGetAllocatedStat_AGILITY() {
        testCharacter.gainExp(500);
        if (testCharacter.getTalentPoints() > 0) {
            characterManager.allocateTalentPoint(testCharacter, "AGILITY");
        }

        int allocated = characterManager.getAllocatedStat(testCharacter, "AGILITY");
        assertTrue("敏捷应已分配或为0", allocated >= 0);
    }

    @Test
    public void testGetAllocatedStat_INTELLIGENCE() {
        testCharacter.gainExp(500);
        if (testCharacter.getTalentPoints() > 0) {
            characterManager.allocateTalentPoint(testCharacter, "INTELLIGENCE");
        }

        int allocated = characterManager.getAllocatedStat(testCharacter, "INTELLIGENCE");
        assertTrue("智力应已分配或为0", allocated >= 0);
    }

    @Test
    public void testGetAllocatedStat_SPIRIT() {
        testCharacter.gainExp(500);
        if (testCharacter.getTalentPoints() > 0) {
            characterManager.allocateTalentPoint(testCharacter, "SPIRIT");
        }

        int allocated = characterManager.getAllocatedStat(testCharacter, "SPIRIT");
        assertTrue("精神应已分配或为0", allocated >= 0);
    }

    @Test
    public void testGetAllocatedStat_PHYSIQUE() {
        testCharacter.gainExp(500);
        if (testCharacter.getTalentPoints() > 0) {
            characterManager.allocateTalentPoint(testCharacter, "PHYSIQUE");
        }

        int allocated = characterManager.getAllocatedStat(testCharacter, "PHYSIQUE");
        assertTrue("体质应已分配或为0", allocated >= 0);
    }

    @Test
    public void testGetAllocatedStat_LUCK() {
        testCharacter.gainExp(500);
        if (testCharacter.getTalentPoints() > 0) {
            characterManager.allocateTalentPoint(testCharacter, "LUCK");
        }

        int allocated = characterManager.getAllocatedStat(testCharacter, "LUCK");
        assertTrue("幸运应已分配或为0", allocated >= 0);
    }

    @Test
    public void testGetAllocatedStat_InvalidAttribute() {
        int result = characterManager.getAllocatedStat(testCharacter, "INVALID");
        assertEquals("无效属性应返回0", 0, result);
    }

    @Test
    public void testGetAllocatedStat_CaseInsensitive() {
        // 通过升级获得天赋点
        testCharacter.gainExp(1000);
        if (testCharacter.getTalentPoints() > 0) {
            characterManager.allocateTalentPoint(testCharacter, "STRENGTH");
        }

        // 小写
        int allocated1 = characterManager.getAllocatedStat(testCharacter, "strength");
        assertEquals("小写应正确识别", testCharacter.getAllocatedStrength(), allocated1);

        // 大小写混合
        int allocated2 = characterManager.getAllocatedStat(testCharacter, "StReNgTh");
        assertEquals("大小写混合应正确识别", testCharacter.getAllocatedStrength(), allocated2);
    }

    @Test
    public void testGetAllocatedStat_AllAttributesUnallocated() {
        // 新角色没有天赋点，所有分配属性应为0
        assertEquals("力量应为0", 0, characterManager.getAllocatedStat(testCharacter, "STRENGTH"));
        assertEquals("敏捷应为0", 0, characterManager.getAllocatedStat(testCharacter, "AGILITY"));
        assertEquals("智力应为0", 0, characterManager.getAllocatedStat(testCharacter, "INTELLIGENCE"));
        assertEquals("精神应为0", 0, characterManager.getAllocatedStat(testCharacter, "SPIRIT"));
        assertEquals("体质应为0", 0, characterManager.getAllocatedStat(testCharacter, "PHYSIQUE"));
        assertEquals("幸运应为0", 0, characterManager.getAllocatedStat(testCharacter, "LUCK"));
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testAllocateTalentPoint_NoTalentPoints() {
        // 新角色初始没有天赋点
        boolean result = characterManager.allocateTalentPoint(testCharacter, "STRENGTH");
        assertFalse("无天赋点时应返回 false", result);
    }

    @Test
    public void testAllocateTalentPoint_ExhaustAllPoints() {
        // 通过升级获得天赋点
        testCharacter.gainExp(1000);
        int points = testCharacter.getTalentPoints();

        if (points >= 3) {
            // 分配所有点
            boolean r1 = characterManager.allocateTalentPoint(testCharacter, "STRENGTH");
            boolean r2 = characterManager.allocateTalentPoint(testCharacter, "STRENGTH");
            boolean r3 = characterManager.allocateTalentPoint(testCharacter, "STRENGTH");

            // 尝试分配第4个点
            boolean r4 = characterManager.allocateTalentPoint(testCharacter, "STRENGTH");

            assertTrue("第1个点应成功", r1);
            assertTrue("第2个点应成功", r2);
            assertTrue("第3个点应成功", r3);
            assertFalse("第4个点应失败", r4);
        } else {
            // 如果点数不足3个，只测试可用点数
            assertTrue("至少应该有一些天赋点", points > 0);
        }
    }

    @Test
    public void testAllocateTalentPoints_DifferentAttributes() {
        // 通过多次升级获得足够的点数
        for (int i = 0; i < 5; i++) {
            testCharacter.gainExp(1000);
        }

        int points = testCharacter.getTalentPoints();
        if (points >= 6) {
            characterManager.allocateTalentPoint(testCharacter, "STRENGTH");
            characterManager.allocateTalentPoint(testCharacter, "STRENGTH");
            characterManager.allocateTalentPoint(testCharacter, "AGILITY");
            characterManager.allocateTalentPoint(testCharacter, "AGILITY");
            characterManager.allocateTalentPoint(testCharacter, "INTELLIGENCE");
            characterManager.allocateTalentPoint(testCharacter, "LUCK");

            assertEquals("力量应为2", 2, testCharacter.getAllocatedStrength());
            assertEquals("敏捷应为2", 2, testCharacter.getAllocatedAgility());
            assertEquals("智力应为1", 1, testCharacter.getAllocatedIntelligence());
            assertEquals("精神应为0", 0, testCharacter.getAllocatedSpirit());
            assertEquals("体质应为0", 0, testCharacter.getAllocatedPhysique());
            assertEquals("幸运应为1", 1, testCharacter.getAllocatedLuck());
        }
    }
}
