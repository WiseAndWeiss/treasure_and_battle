package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 斩击技能测试
 * 技能效果：对目标造成{x}%物理攻击伤害，附加{y}%无视防御与护盾的破甲伤害
 */
public class ActiveSkillSlashTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：100%物理伤害，0%破甲
     * 预期：造成 (50 * 100% - 20防御) = 30点物理伤害
     */
    @Test
    public void testSlashLevel1() {
        // Given
        ActiveSkill slash = createSkill("slash", 1);
        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        int damage = executeSkillAndDamage(slash, testPlayer, testMonster);

        // Then
        // 预期伤害：50(攻击) * 100% - 20(防御) = 30
        int expectedDamage = 30;
        assertTrue("等级1斩击应该造成 " + expectedDamage + " 点伤害，实际造成 " + damage,
            Math.abs(damage - expectedDamage) <= 2); // 允许2点误差（可能有暴击）

        // 验证HP正确扣除
        assertEquals("怪物HP应该正确扣除", monsterHpBefore - damage, testMonster.getCurrentHp());

        // 验证日志记录
        assertLogExists(LogType.DAMAGE);
        assertLogContains(LogType.DAMAGE, "【斩击】");
        printBattleLogs();
    }

    /**
     * 测试等级3：110%物理伤害，7%破甲
     * 预期：造成 (50 * 110% - 20防御) = 35点物理伤害 + (50 * 7%) = 3.5 ≈ 3点破甲伤害 = 38点总伤害
     */
    @Test
    public void testSlashLevel3() {
        // Given
        ActiveSkill slash = createSkill("slash", 3);
        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        int damage = executeSkillAndDamage(slash, testPlayer, testMonster);

        // Then
        // 物理伤害：50 * 110% - 20 = 35
        // 破甲伤害：50 * 7% = 3.5 ≈ 3或4
        int expectedPhysical = 35;
        int expectedPiercing = (int) (50 * 7 / 100.0f);
        int expectedTotal = expectedPhysical + expectedPiercing;

        assertTrue("等级3斩击应该造成约 " + expectedTotal + " 点伤害，实际造成 " + damage,
            Math.abs(damage - expectedTotal) <= 5); // 允许5点误差

        // 验证HP正确扣除
        assertEquals("怪物HP应该正确扣除", monsterHpBefore - damage, testMonster.getCurrentHp());

        // 验证日志记录
        assertLogContains(LogType.DAMAGE, "物理");
        assertLogContains(LogType.DAMAGE, "破甲");
        printBattleLogs();
    }

    /**
     * 测试等级5：120%物理伤害，15%破甲
     * 预期：造成 (50 * 120% - 20防御) = 40点物理伤害 + (50 * 15%) = 7.5 ≈ 7点破甲伤害 = 47点总伤害
     */
    @Test
    public void testSlashLevel5() {
        // Given
        ActiveSkill slash = createSkill("slash", 5);
        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        int damage = executeSkillAndDamage(slash, testPlayer, testMonster);

        // Then
        // 物理伤害：50 * 120% - 20 = 40
        // 破甲伤害：50 * 15% = 7.5 ≈ 7或8
        int expectedPhysical = 40;
        int expectedPiercing = (int) (50 * 15 / 100.0f);
        int expectedTotal = expectedPhysical + expectedPiercing;

        assertTrue("等级5斩击应该造成约 " + expectedTotal + " 点伤害，实际造成 " + damage,
            Math.abs(damage - expectedTotal) <= 5);

        // 验证HP正确扣除
        assertEquals("怪物HP应该正确扣除", monsterHpBefore - damage, testMonster.getCurrentHp());

        // 验证破甲效果生效（破甲伤害应该无视防御和护盾）
        assertLogContains(LogType.DAMAGE, "破甲");
        printBattleLogs();
    }

    /**
     * 测试斩击的消耗
     * 预期：消耗1行动点，不消耗MP和HP
     */
    @Test
    public void testSlashCost() {
        // Given
        ActiveSkill slash = createSkill("slash", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, slash,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        assertEquals("应该消耗1行动点", apBefore - 1, testPlayer.getCurrentActionPoints());
        assertEquals("不应该消耗MP", mpBefore, testPlayer.getCurrentMp());
        assertEquals("不应该消耗HP", hpBefore, testPlayer.getCurrentHp());
    }

    /**
     * 测试斩击对无防御目标的效果
     * 使用物理防御为0的怪物
     */
    @Test
    public void testSlashAgainstZeroDefense() {
        // Given
        testMonster.getBaseAttributes().physicalDef = 0;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(testMonster.getBaseAttributes().maxHp);

        ActiveSkill slash = createSkill("slash", 1);

        // When
        int damage = executeSkillAndDamage(slash, testPlayer, testMonster);

        // Then
        // 无防御时：50 * 100% = 50点伤害
        int expectedDamage = 50;
        assertTrue("对0防御目标应该造成 " + expectedDamage + " 点伤害，实际造成 " + damage,
            Math.abs(damage - expectedDamage) <= 2);
        printBattleLogs();
    }
}
