package com.example.treasure_and_battle.skill.passive.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 以攻为守技能测试
 * 技能效果：每次造成暴击伤害时，提升自身x%物理防御与法术防御
 */
public class PassiveSkill_AttackToDefendTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：暴击后物防和法防提升3%
     */
    @Test
    public void testAttackToDefendLevel1() {
        // Given
        PassiveSkill attackToDefend = createPassiveSkill("attack_to_defend", 1);

        // 设置更高的基础防御以便观察3%变化
        testPlayer.getBaseAttributes().physicalDef = 100;
        testPlayer.getBaseAttributes().magicalDef = 100;
        testPlayer.markAttributeCacheDirty();

        int initialPhysicalDef = testPlayer.getFinalAttributes().physicalDef;
        int initialMagicalDef = testPlayer.getFinalAttributes().magicalDef;

        // When - 暴击触发
        attackToDefend.onCrit(testPlayer, battleContext);

        // Then - 验证防御提升
        int currentPhysicalDef = testPlayer.getFinalAttributes().physicalDef;
        int currentMagicalDef = testPlayer.getFinalAttributes().magicalDef;

        assertTrue("物理防御应该提升", currentPhysicalDef > initialPhysicalDef);
        assertTrue("法术防御应该提升", currentMagicalDef > initialMagicalDef);

        assertEquals("物理防御应该提升3% (100 -> 103)", 103, currentPhysicalDef);
        assertEquals("法术防御应该提升3% (100 -> 103)", 103, currentMagicalDef);

        // 验证日志
        assertLogContains(LogType.BUFF, "【以攻为守】");

        printBattleLogs();
    }

    /**
     * 测试等级3：暴击后物防和法防提升5%
     */
    @Test
    public void testAttackToDefendLevel3() {
        // Given
        PassiveSkill attackToDefend = createPassiveSkill("attack_to_defend", 3);

        testPlayer.getBaseAttributes().physicalDef = 100;
        testPlayer.getBaseAttributes().magicalDef = 100;
        testPlayer.markAttributeCacheDirty();

        int initialPhysicalDef = testPlayer.getFinalAttributes().physicalDef;
        int initialMagicalDef = testPlayer.getFinalAttributes().magicalDef;

        // When - 暴击触发
        attackToDefend.onCrit(testPlayer, battleContext);

        // Then - 验证防御提升
        int currentPhysicalDef = testPlayer.getFinalAttributes().physicalDef;
        int currentMagicalDef = testPlayer.getFinalAttributes().magicalDef;

        assertEquals("物理防御应该提升5% (100 -> 105)", 105, currentPhysicalDef);
        assertEquals("法术防御应该提升5% (100 -> 105)", 105, currentMagicalDef);

        printBattleLogs();
    }

    /**
     * 测试等级5：暴击后物防和法防提升8%
     */
    @Test
    public void testAttackToDefendLevel5() {
        // Given
        PassiveSkill attackToDefend = createPassiveSkill("attack_to_defend", 5);

        testPlayer.getBaseAttributes().physicalDef = 100;
        testPlayer.getBaseAttributes().magicalDef = 100;
        testPlayer.markAttributeCacheDirty();

        int initialPhysicalDef = testPlayer.getFinalAttributes().physicalDef;
        int initialMagicalDef = testPlayer.getFinalAttributes().magicalDef;

        // When - 暴击触发
        attackToDefend.onCrit(testPlayer, battleContext);

        // Then - 验证防御提升
        int currentPhysicalDef = testPlayer.getFinalAttributes().physicalDef;
        int currentMagicalDef = testPlayer.getFinalAttributes().magicalDef;

        assertEquals("物理防御应该提升8% (100 -> 108)", 108, currentPhysicalDef);
        assertEquals("法术防御应该提升8% (100 -> 108)", 108, currentMagicalDef);

        printBattleLogs();
    }

    /**
     * 测试多次暴击累积效果
     */
    @Test
    public void testAttackToDefendStacking() {
        // Given
        PassiveSkill attackToDefend = createPassiveSkill("attack_to_defend", 1);

        testPlayer.getBaseAttributes().physicalDef = 100;
        testPlayer.getBaseAttributes().magicalDef = 100;
        testPlayer.markAttributeCacheDirty();

        int initialPhysicalDef = testPlayer.getFinalAttributes().physicalDef;
        int initialMagicalDef = testPlayer.getFinalAttributes().magicalDef;

        // When - 多次暴击触发
        attackToDefend.onCrit(testPlayer, battleContext);
        attackToDefend.onCrit(testPlayer, battleContext);
        attackToDefend.onCrit(testPlayer, battleContext);

        // Then - 验证防御累积提升
        int currentPhysicalDef = testPlayer.getFinalAttributes().physicalDef;
        int currentMagicalDef = testPlayer.getFinalAttributes().magicalDef;

        assertEquals("物理防御应该累积提升3次3% (100 -> 109)", 109, currentPhysicalDef);
        assertEquals("法术防御应该累积提升3次3% (100 -> 109)", 109, currentMagicalDef);

        printBattleLogs();
    }

    /**
     * 测试防御提升确实影响最终防御值
     */
    @Test
    public void testAttackToDefendAffectsFinalDefense() {
        // Given
        PassiveSkill attackToDefend = createPassiveSkill("attack_to_defend", 5);

        int basePhysicalDef = testPlayer.getFinalAttributes().physicalDef;
        int baseMagicalDef = testPlayer.getFinalAttributes().magicalDef;

        // When - 暴击触发
        attackToDefend.onCrit(testPlayer, battleContext);

        // Then - 验证最终防御值受百分比加成影响
        int finalPhysicalDef = testPlayer.getFinalAttributes().physicalDef;
        int finalMagicalDef = testPlayer.getFinalAttributes().magicalDef;

        assertTrue("物理防御值应该改变", finalPhysicalDef != basePhysicalDef);
        assertTrue("法术防御值应该改变", finalMagicalDef != baseMagicalDef);

        printBattleLogs();
    }

    /**
     * 测试对怪物生效
     */
    @Test
    public void testAttackToDefendOnMonster() {
        // Given
        PassiveSkill attackToDefend = createPassiveSkill("attack_to_defend", 1);

        testMonster.getBaseAttributes().physicalDef = 50;
        testMonster.getBaseAttributes().magicalDef = 50;
        testMonster.markAttributeCacheDirty();

        int initialPhysicalDef = testMonster.getFinalAttributes().physicalDef;
        int initialMagicalDef = testMonster.getFinalAttributes().magicalDef;

        // When - 暴击触发
        attackToDefend.onCrit(testMonster, battleContext);

        // Then - 验证怪物防御也提升
        int currentPhysicalDef = testMonster.getFinalAttributes().physicalDef;
        int currentMagicalDef = testMonster.getFinalAttributes().magicalDef;

        assertTrue("怪物物理防御应该提升", currentPhysicalDef > initialPhysicalDef);
        assertTrue("怪物法术防御应该提升", currentMagicalDef > initialMagicalDef);

        printBattleLogs();
    }

    /**
     * 测试日志内容
     */
    @Test
    public void testAttackToDefendLogContent() {
        // Given
        PassiveSkill attackToDefend = createPassiveSkill("attack_to_defend", 1);

        // When - 暴击触发
        attackToDefend.onCrit(testPlayer, battleContext);

        // Then - 验证日志包含暴击和防御信息
        assertLogContains(LogType.BUFF, "暴击后");
        assertLogContains(LogType.BUFF, "物理防御");
        assertLogContains(LogType.BUFF, "法术防御");

        printBattleLogs();
    }
}
