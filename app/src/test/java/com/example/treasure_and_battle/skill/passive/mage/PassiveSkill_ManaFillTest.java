package com.example.treasure_and_battle.skill.passive.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 魔力充盈技能测试
 * 技能效果：回合开始时恢复x%最大MP；若MP已满，则本回合魔法攻击提升y%
 */
public class PassiveSkill_ManaFillTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：3%MP恢复，MP满时5%魔法攻击提升
     */
    @Test
    public void testManaFillLevel1MpNotFull() {
        // Given
        PassiveSkill manaFill = createPassiveSkill("mana_fill", 1);
        addPassiveSkillToEntity(testPlayer, manaFill);

        // 设置MP未满
        testPlayer.setCurrentMp(50);
        int mpBefore = testPlayer.getCurrentMp();
        int maxMp = testPlayer.getFinalAttributes().maxMp;

        // When - 触发回合开始
        manaFill.onRoundStart(testPlayer, battleContext);

        // Then
        int expectedMpRestore = (int) (maxMp * 0.03f);
        int actualMpRestore = testPlayer.getCurrentMp() - mpBefore;
        assertTrue("应该恢复MP", actualMpRestore > 0);
        assertTrue("MP恢复量应该接近预期", Math.abs(actualMpRestore - expectedMpRestore) <= 1);

        // 验证没有魔法攻击buff
        AttributeBuff magicAtkBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("mana_fill_magical_atk_boost"))
                .findFirst()
                .orElse(null);
        assertNull("不应该有魔法攻击buff（因为MP未满）", magicAtkBuff);

        printBattleLogs();
    }

    /**
     * 测试等级1：MP满时添加魔法攻击buff
     */
    @Test
    public void testManaFillLevel1MpFull() {
        // Given
        PassiveSkill manaFill = createPassiveSkill("mana_fill", 1);
        addPassiveSkillToEntity(testPlayer, manaFill);

        // 设置MP已满
        testPlayer.setCurrentMp(testPlayer.getFinalAttributes().maxMp);
        int magAtkBefore = testPlayer.getFinalAttributes().magicalAtk;

        // When - 触发回合开始
        manaFill.onRoundStart(testPlayer, battleContext);

        // Then
        int magAtkAfter = testPlayer.getFinalAttributes().magicalAtk;
        assertTrue("魔法攻击应该提升", magAtkAfter > magAtkBefore);
        // 验证提升幅度合理
        assertTrue("魔法攻击提升幅度应该合理", magAtkAfter >= magAtkBefore + 3);

        // 验证有魔法攻击buff
        AttributeBuff magicAtkBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("mana_fill_magical_atk_boost"))
                .findFirst()
                .orElse(null);
        assertNotNull("应该有魔法攻击buff（因为MP已满）", magicAtkBuff);

        printBattleLogs();
    }

    /**
     * 测试等级3：7%MP恢复，MP满时12%魔法攻击提升
     */
    @Test
    public void testManaFillLevel3MpNotFull() {
        // Given
        PassiveSkill manaFill = createPassiveSkill("mana_fill", 3);
        addPassiveSkillToEntity(testPlayer, manaFill);

        // 设置MP未满
        testPlayer.setCurrentMp(50);
        int mpBefore = testPlayer.getCurrentMp();
        int maxMp = testPlayer.getFinalAttributes().maxMp;

        // When - 触发回合开始
        manaFill.onRoundStart(testPlayer, battleContext);

        // Then
        int expectedMpRestore = (int) (maxMp * 0.07f);
        int actualMpRestore = testPlayer.getCurrentMp() - mpBefore;
        assertTrue("应该恢复MP", actualMpRestore > 0);
        assertTrue("MP恢复量应该接近预期", Math.abs(actualMpRestore - expectedMpRestore) <= 1);

        printBattleLogs();
    }

    /**
     * 测试等级5：12%MP恢复，MP满时15%魔法攻击提升
     */
    @Test
    public void testManaFillLevel5MpFull() {
        // Given
        PassiveSkill manaFill = createPassiveSkill("mana_fill", 5);
        addPassiveSkillToEntity(testPlayer, manaFill);

        // 设置MP已满
        testPlayer.setCurrentMp(testPlayer.getFinalAttributes().maxMp);
        int magAtkBefore = testPlayer.getFinalAttributes().magicalAtk;

        // When - 触发回合开始
        manaFill.onRoundStart(testPlayer, battleContext);

        // Then
        int magAtkAfter = testPlayer.getFinalAttributes().magicalAtk;
        assertTrue("魔法攻击应该提升", magAtkAfter > magAtkBefore);
        // 验证提升幅度合理
        assertTrue("魔法攻击提升幅度应该合理", magAtkAfter >= magAtkBefore + 10);

        printBattleLogs();
    }

    /**
     * 测试MP恢复上限
     */
    @Test
    public void testManaFillMpRestoreCap() {
        // Given
        PassiveSkill manaFill = createPassiveSkill("mana_fill", 3);
        addPassiveSkillToEntity(testPlayer, manaFill);

        // 设置MP接近满值
        int maxMp = testPlayer.getFinalAttributes().maxMp;
        testPlayer.setCurrentMp(maxMp - 2);

        // When - 触发回合开始
        manaFill.onRoundStart(testPlayer, battleContext);

        // Then
        assertEquals("MP不应该超过最大值", maxMp, testPlayer.getCurrentMp());

        printBattleLogs();
    }

    /**
     * 测试魔法攻击buff持续回合数
     */
    @Test
    public void testManaFillBuffDuration() {
        // Given
        PassiveSkill manaFill = createPassiveSkill("mana_fill", 1);
        addPassiveSkillToEntity(testPlayer, manaFill);

        // 设置MP已满
        testPlayer.setCurrentMp(testPlayer.getFinalAttributes().maxMp);

        // When - 触发回合开始
        manaFill.onRoundStart(testPlayer, battleContext);

        // Then
        AttributeBuff magicAtkBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("mana_fill_magical_atk_boost"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有魔法攻击buff", magicAtkBuff);
        assertEquals("buff应该持续1回合", 1, magicAtkBuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试多次触发不重复添加buff
     */
    @Test
    public void testManaFillNoDuplicateBuff() {
        // Given
        PassiveSkill manaFill = createPassiveSkill("mana_fill", 1);
        addPassiveSkillToEntity(testPlayer, manaFill);

        // 设置MP已满
        testPlayer.setCurrentMp(testPlayer.getFinalAttributes().maxMp);

        // When - 触发两次回合开始
        manaFill.onRoundStart(testPlayer, battleContext);

        long buffCount1 = testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("mana_fill_magical_atk_boost"))
                .count();

        manaFill.onRoundStart(testPlayer, battleContext);

        long buffCount2 = testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("mana_fill_magical_atk_boost"))
                .count();

        // Then - 应该不会重复添加buff
        assertEquals("第一次触发后应该有1个buff", 1, buffCount1);
        assertEquals("第二次触发后应该还是只有1个buff", 1, buffCount2);

        printBattleLogs();
    }
}
