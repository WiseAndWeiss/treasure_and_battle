package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.manager.skill.SkillManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 咏唱技能测试
 * 技能效果：对目标造成{x}%法术攻击伤害，附带{y}%减速，持续一回合
 */
public class ActiveSkillSpellTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：95%法术伤害，0%减速
     * 预期：造成 (50 * 95% - 20法术防御) = 27.5 ≈ 27或28点法术伤害，不施加减速
     */
    @Test
    public void testSpellLevel1() {
        // Given
        ActiveSkill spell = createSkill("spell", 1);
        int monsterHpBefore = testMonster.getCurrentHp();
        int mpBefore = testPlayer.getCurrentMp();

        // When
        int damage = executeSkillAndDamage(spell, testPlayer, testMonster);

        // Then
        // 预期伤害：50 * 95% - 20 = 27.5
        int expectedDamage = (int) (50 * 95 / 100.0f) - 20;
        assertTrue("等级1咏唱应该造成约 " + expectedDamage + " 点伤害，实际造成 " + damage,
            Math.abs(damage - expectedDamage) <= 2);

        // 验证MP消耗（等级1消耗10MP）
        assertEquals("应该消耗10MP", mpBefore - 10, testPlayer.getCurrentMp());

        // 验证不施加减速buff
        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        boolean hasSlowDebuff = buffs.stream()
            .anyMatch(buff -> buff instanceof SlowDebuff);
        assertFalse("等级1咏唱不应该施加减速buff", hasSlowDebuff);

        // 验证日志
        assertLogExists(LogType.DAMAGE);
        assertLogContains(LogType.DAMAGE, "【咏唱】");
        printBattleLogs();
    }

    /**
     * 测试等级3：105%法术伤害，7%减速
     * 预期：造成约32点法术伤害，施加7%减速buff
     */
    @Test
    public void testSpellLevel3() {
        // Given
        ActiveSkill spell = createSkill("spell", 3);
        int monsterHpBefore = testMonster.getCurrentHp();
        AttributeSet monsterAttrBefore = testMonster.getFinalAttributes();

        // When
        int damage = executeSkillAndDamage(spell, testPlayer, testMonster);

        // Then
        // 预期伤害：50 * 105% - 20 = 32.5
        int expectedDamage = (int) (50 * 105 / 100.0f) - 20;
        assertTrue("等级3咏唱应该造成约 " + expectedDamage + " 点伤害，实际造成 " + damage,
            Math.abs(damage - expectedDamage) <= 5);

        // 验证施加了减速buff
        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        SlowDebuff slowDebuff = (SlowDebuff) buffs.stream()
            .filter(buff -> buff instanceof SlowDebuff)
            .findFirst()
            .orElse(null);
        assertNotNull("等级3咏唱应该施加减速buff", slowDebuff);

        // 验证buff数值（7%减速）
        assertEquals("减速百分比应该为7%", 7, (int) slowDebuff.getSpeedReductionPercent());

        // 验证速度属性被标记为脏（需要重新计算）
        // 注意：这里我们检查buff是否存在，而不检查实际速度值，因为那是buff系统的职责
        printBattleLogs();
    }

    /**
     * 测试等级5：115%法术伤害，15%减速
     * 预期：造成约37点法术伤害，施加15%减速buff
     */
    @Test
    public void testSpellLevel5() {
        // Given
        ActiveSkill spell = createSkill("spell", 5);
        int monsterSpeedBefore = testMonster.getBaseAttributes().speed;

        // When
        int damage = executeSkillAndDamage(spell, testPlayer, testMonster);

        // Then
        // 预期伤害：50 * 115% - 20 = 37.5
        int expectedDamage = (int) (50 * 115 / 100.0f) - 20;
        assertTrue("等级5咏唱应该造成约 " + expectedDamage + " 点伤害，实际造成 " + damage,
            Math.abs(damage - expectedDamage) <= 5);

        // 验证施加了减速buff
        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        SlowDebuff slowDebuff = (SlowDebuff) buffs.stream()
            .filter(buff -> buff instanceof SlowDebuff)
            .findFirst()
            .orElse(null);
        assertNotNull("等级5咏唱应该施加减速buff", slowDebuff);

        // 验证减速百分比为15%
        assertEquals("减速百分比应该为15%", 15, (int) slowDebuff.getSpeedReductionPercent());

        // 验证日志记录了减速效果
        assertLogContains(LogType.BUFF, "减速");
        printBattleLogs();
    }

    /**
     * 测试咏唱的MP消耗随等级递增
     * 等级1-2：10MP，等级3-4：12MP，等级5：14MP
     */
    @Test
    public void testSpellMpCostByLevel() {
        // 测试等级1
        testPlayer.setCurrentMp(100);
        battleManager.executeSkill(testPlayer,
            (ActiveSkill) SkillManager.getInstance(context).createSkillBySkillId("spell", 1),
            java.util.Arrays.asList(testMonster), battleContext);
        assertEquals("等级1应该消耗10MP", 90, testPlayer.getCurrentMp());

        // 测试等级3
        testPlayer.setCurrentMp(100);
        ActiveSkill spell3 = createSkill("spell", 3);
        battleManager.executeSkill(testPlayer, spell3,
            java.util.Arrays.asList(testMonster), battleContext);
        assertEquals("等级3应该消耗12MP", 88, testPlayer.getCurrentMp());

        // 测试等级5
        testPlayer.setCurrentMp(100);
        ActiveSkill spell5 = createSkill("spell", 5);
        battleManager.executeSkill(testPlayer, spell5,
            java.util.Arrays.asList(testMonster), battleContext);
        assertEquals("等级5应该消耗14MP", 86, testPlayer.getCurrentMp());
    }

    /**
     * 测试咏唱对无魔防目标的效果
     */
    @Test
    public void testSpellAgainstZeroMagicDefense() {
        // Given
        testMonster.getBaseAttributes().magicalDef = 0;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(testMonster.getBaseAttributes().maxHp);

        ActiveSkill spell = createSkill("spell", 1);

        // When
        int damage = executeSkillAndDamage(spell, testPlayer, testMonster);

        // Then
        // 无魔防时：50 * 95% = 47.5
        int expectedDamage = (int) (50 * 95 / 100.0f);
        assertTrue("对0魔防目标应该造成约 " + expectedDamage + " 点伤害，实际造成 " + damage,
            Math.abs(damage - expectedDamage) <= 2);
        printBattleLogs();
    }

    /**
     * 测试减速buff的持续时间
     */
    @Test
    public void testSlowDebuffDuration() {
        // Given
        ActiveSkill spell = createSkill("spell", 5);

        // When
        battleManager.executeSkill(testPlayer, spell,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        SlowDebuff slowDebuff = (SlowDebuff) buffs.stream()
            .filter(buff -> buff instanceof SlowDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该存在减速buff", slowDebuff);
        assertEquals("减速buff应该持续1回合", 1, slowDebuff.getRemainingDuration());
        printBattleLogs();
    }
}
