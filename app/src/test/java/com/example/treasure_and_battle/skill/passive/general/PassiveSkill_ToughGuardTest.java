package com.example.treasure_and_battle.skill.passive.general;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 坚韧护体技能测试
 * 技能效果：在战斗中，持续提升{x}%物防，{y}%法防，每回合结束时回复{z}%的最大生命值
 */
public class PassiveSkill_ToughGuardTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：3%物防/法防，0%回血
     */
    @Test
    public void testToughGuardLevel1() {
        resetEntityStates();

        // Given
        int basePhysicalDef = testPlayer.getBaseAttributes().physicalDef;  // 50
        int baseMagicalDef = testPlayer.getBaseAttributes().magicalDef;    // 50

        PassiveSkill toughGuard = createPassiveSkill("tough_guard", 1);
        addPassiveSkillToEntity(testPlayer, toughGuard);

        // When - 触发战斗开始
        triggerBattleStart();

        // Then - 验证防御提升（应该 > 基础防御）
        AttributeSet playerDefAfter = testPlayer.getFinalAttributes();
        int expectedPhysicalDef = Math.round(basePhysicalDef * 1.03f);  // 50 * 1.03 = 51.5 → 52
        int expectedMagicalDef = Math.round(baseMagicalDef * 1.03f);
        assertEquals("物防应该提升到约52", expectedPhysicalDef, playerDefAfter.physicalDef);
        assertEquals("法防应该提升到约52", expectedMagicalDef, playerDefAfter.magicalDef);

        // 验证日志
        assertLogContains(LogType.BUFF, "【坚韧护体】");
        assertLogContains(LogType.BUFF, "防御提升了 3%");

        printBattleLogs();
    }

    /**
     * 测试等级3：5%物防/法防，2%回血
     */
    @Test
    public void testToughGuardLevel3_Healing() {
        resetEntityStates();

        // Given
        PassiveSkill toughGuard = createPassiveSkill("tough_guard", 3);
        addPassiveSkillToEntity(testPlayer, toughGuard);

        // 设置玩家血量较低，便于观察回血
        testPlayer.setCurrentHp(100);

        // When - 触发回合结束
        int hpBefore = testPlayer.getCurrentHp();
        triggerRoundEnd();

        // Then - 验证回血
        int hpAfter = testPlayer.getCurrentHp();
        int expectedHeal = (int) (testPlayer.getFinalAttributes().maxHp * 2 / 100.0f); // 2%回血
        assertTrue("应该回血", hpAfter > hpBefore);

        // 验证日志
        assertLogExists(LogType.HEAL);
        assertLogContains(LogType.HEAL, "【坚韧护体】");
        assertLogContains(LogType.HEAL, "回复了");

        printBattleLogs();
    }

    /**
     * 测试等级5：8%物防/法防，4%回血
     */
    @Test
    public void testToughGuardLevel5() {
        resetEntityStates();

        // Given
        int basePhysicalDef = testPlayer.getBaseAttributes().physicalDef;  // 50

        PassiveSkill toughGuard = createPassiveSkill("tough_guard", 5);
        addPassiveSkillToEntity(testPlayer, toughGuard);

        // When
        triggerBattleStart();

        // Then - 验证防御提升（等级5是8%）
        AttributeSet playerDefAfter = testPlayer.getFinalAttributes();
        int expectedPhysicalDef = Math.round(basePhysicalDef * 1.08f);  // 50 * 1.08 = 54
        assertEquals("物防应该提升到54", expectedPhysicalDef, playerDefAfter.physicalDef);

        // 验证回血（等级5是4%）
        testPlayer.setCurrentHp(100);
        int hpBefore = testPlayer.getCurrentHp();
        triggerRoundEnd();
        int hpAfter = testPlayer.getCurrentHp();
        assertTrue("等级5应该回血4%", hpAfter > hpBefore);

        printBattleLogs();
    }

    /**
     * 测试低等级不回血（z参数为0）
     */
    @Test
    public void testLowLevelNoHealing() {
        resetEntityStates();

        // Given
        PassiveSkill toughGuard = createPassiveSkill("tough_guard", 1); // z=0，不回血
        addPassiveSkillToEntity(testPlayer, toughGuard);

        testPlayer.setCurrentHp(100);
        int hpBefore = testPlayer.getCurrentHp();

        // When
        triggerRoundEnd();

        // Then - 不应该回血
        int hpAfter = testPlayer.getCurrentHp();
        assertEquals("等级1不应该回血", hpBefore, hpAfter);
    }

    /**
     * 测试防御提升的计算
     */
    @Test
    public void testDefenseCalculation() {
        resetEntityStates();

        // Given
        PassiveSkill toughGuard = createPassiveSkill("tough_guard", 1);
        addPassiveSkillToEntity(testPlayer, toughGuard);
        int basePhysicalDef = testPlayer.getBaseAttributes().physicalDef; // 10
        int baseMagicalDef = testPlayer.getBaseAttributes().magicalDef; // 10

        // When
        triggerBattleStart();

        // Then - 验证防御提升3%
        AttributeSet finalAttr = testPlayer.getFinalAttributes();
        int expectedPhysicalDef = (int) Math.round(basePhysicalDef * 1.03f); // 50 * 1.03 = 51.5 → 52
        int expectedMagicalDef = (int) Math.round(baseMagicalDef * 1.03f);

        // 验证防御提升
        assertEquals("物防应该提升到约52", expectedPhysicalDef, finalAttr.physicalDef);
        assertEquals("法防应该提升到约52", expectedMagicalDef, finalAttr.magicalDef);

        System.out.println("基础物防: " + basePhysicalDef + ", 最终物防: " + finalAttr.physicalDef);
        System.out.println("基础法防: " + baseMagicalDef + ", 最终法防: " + finalAttr.magicalDef);
    }
}
