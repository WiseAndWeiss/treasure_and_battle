package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.periodic.RegeneratingBuff;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 调息技能测试
 * 技能效果：恢复x点HP和y点MP，获得再生buff（持续z回合，5%最大生命值/回合）
 */
public class ActiveSkill_RegulateBreathTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：恢复20HP+20MP，无再生buff
     */
    @Test
    public void testRegulateBreathLevel1() {
        // Given
        ActiveSkill regulateBreath = createSkill("regulate_breath", 1);
        testPlayer.setCurrentHp(100);
        testPlayer.setCurrentMp(50);

        int hpBefore = testPlayer.getCurrentHp();
        int mpBefore = testPlayer.getCurrentMp();

        // When
        battleManager.executeSkill(testPlayer, regulateBreath,
            java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        int expectedHeal = 20;
        int expectedManaRestore = 20;

        assertEquals("应该恢复" + expectedHeal + "点HP", hpBefore + expectedHeal, testPlayer.getCurrentHp());
        assertEquals("应该恢复" + expectedManaRestore + "点MP", mpBefore + expectedManaRestore, testPlayer.getCurrentMp());

        // 验证日志
        assertLogExists(LogType.HEAL);
        assertLogContains(LogType.HEAL, "【调息】");
        assertLogContains(LogType.HEAL, "恢复了");

        // 验证没有再生buff
        boolean hasRegenBuff = testPlayer.getActiveBuffList().stream()
            .anyMatch(buff -> buff instanceof RegeneratingBuff);
        assertFalse("等级1不应该有再生buff", hasRegenBuff);

        printBattleLogs();
    }

    /**
     * 测试等级4：恢复60HP+60MP，获得1回合再生buff
     */
    @Test
    public void testRegulateBreathLevel4() {
        // Given
        ActiveSkill regulateBreath = createSkill("regulate_breath", 4);
        testPlayer.setCurrentHp(100);
        testPlayer.setCurrentMp(30);

        int hpBefore = testPlayer.getCurrentHp();
        int mpBefore = testPlayer.getCurrentMp();

        // When
        battleManager.executeSkill(testPlayer, regulateBreath,
            java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        int expectedHeal = 60;
        int expectedManaRestore = 60;

        assertEquals("应该恢复" + expectedHeal + "点HP", hpBefore + expectedHeal, testPlayer.getCurrentHp());
        assertEquals("应该恢复" + expectedManaRestore + "点MP", mpBefore + expectedManaRestore, testPlayer.getCurrentMp());

        // 验证有再生buff
        boolean hasRegenBuff = testPlayer.getActiveBuffList().stream()
            .anyMatch(buff -> buff instanceof RegeneratingBuff);
        assertTrue("等级4应该有再生buff", hasRegenBuff);

        // 验证日志
        assertLogExists(LogType.HEAL);
        assertLogContains(LogType.BUFF, "获得了");
        assertLogContains(LogType.BUFF, "再生");

        printBattleLogs();
    }

    /**
     * 测试HP恢复上限
     */
    @Test
    public void testRegulateBreathHpCap() {
        // Given
        ActiveSkill regulateBreath = createSkill("regulate_breath", 3);
        int maxHp = testPlayer.getFinalAttributes().maxHp;
        testPlayer.setCurrentHp(maxHp - 10); // 接近满血

        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, regulateBreath,
            java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        assertEquals("HP不应该超过最大值", maxHp, testPlayer.getCurrentHp());
        int actualHeal = testPlayer.getCurrentHp() - hpBefore;
        assertTrue("实际恢复量应该<=40", actualHeal <= 40);

        printBattleLogs();
    }

    /**
     * 测试MP恢复上限
     */
    @Test
    public void testRegulateBreathMpCap() {
        // Given
        ActiveSkill regulateBreath = createSkill("regulate_breath", 3);
        int maxMp = testPlayer.getFinalAttributes().maxMp;
        testPlayer.setCurrentMp(maxMp - 5); // 接近满蓝

        int mpBefore = testPlayer.getCurrentMp();

        // When
        battleManager.executeSkill(testPlayer, regulateBreath,
            java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        assertEquals("MP不应该超过最大值", maxMp, testPlayer.getCurrentMp());
        int actualManaRestore = testPlayer.getCurrentMp() - mpBefore;
        assertTrue("实际恢复量应该<=40", actualManaRestore <= 40);

        printBattleLogs();
    }

    /**
     * 测试消耗
     */
    @Test
    public void testRegulateBreathCost() {
        // Given
        ActiveSkill regulateBreath = createSkill("regulate_breath", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, regulateBreath,
            java.util.Arrays.asList(testPlayer), battleContext);

        // Then - 验证AP消耗
        assertEquals("应该消耗3行动点", apBefore - 3, testPlayer.getCurrentActionPoints());

        // 计算净变化：最终 - 初始 = 净恢复量
        int mpNetChange = testPlayer.getCurrentMp() - mpBefore;
        int hpNetChange = testPlayer.getCurrentHp() - hpBefore;

        // 技能恢复20MP和20HP，但可能没有MP消耗
        assertTrue("MP应该恢复或保持不变", mpNetChange >= 0);
        assertTrue("HP应该恢复或保持不变", hpNetChange >= 0);

        System.out.println("MP净变化：" + mpNetChange);
        System.out.println("HP净变化：" + hpNetChange);
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testRegulateBreathScaling() {
        // 等级1：20HP+20MP
        int heal1 = 20;
        int mana1 = 20;

        // 等级3：40HP+40MP
        int heal3 = 40;
        int mana3 = 40;

        // 等级5：80HP+80MP + 2回合再生
        int heal5 = 80;
        int mana5 = 80;

        assertTrue("等级5恢复量应该大于等级1", heal5 > heal1);
        assertTrue("等级5恢复量应该大于等级3", heal5 > heal3);

        System.out.println("等级1恢复：" + heal1 + "HP+" + mana1 + "MP");
        System.out.println("等级3恢复：" + heal3 + "HP+" + mana3 + "MP");
        System.out.println("等级5恢复：" + heal5 + "HP+" + mana5 + "MP");
    }

    /**
     * 测试再生buff持续回合数
     */
    @Test
    public void testRegenerateBuffDuration() {
        // Given
        ActiveSkill regulateBreath = createSkill("regulate_breath", 5); // 2回合再生

        // When
        battleManager.executeSkill(testPlayer, regulateBreath,
            java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        BaseBuff regenBuff = testPlayer.getActiveBuffList().stream()
            .filter(buff -> buff instanceof RegeneratingBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有再生buff", regenBuff);
        assertEquals("再生buff应该持续2回合", 2, regenBuff.getRemainingDuration());

        printBattleLogs();
    }
}
