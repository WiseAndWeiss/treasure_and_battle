package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 刮骨疗毒技能测试
 * 技能效果：对单个敌人造成x点基础伤害，若目标存在中毒buff，清空所有中毒层数，每层额外造成y%法术攻击伤害
 */
public class ActiveSkill_ScrapePoisonTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：40点基础伤害 + 无中毒
     */
    @Test
    public void testScrapePoisonLevel1NoPoison() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill scrapePoison = createSkill("scrape_poison", 1);
        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, scrapePoison,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证造成基础伤害
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("应该造成基础伤害", actualDamage > 0);

        // 验证没有中毒debuff
        PoisoningDebuff poisoningDebuff = (PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null);
        assertNull("不应该有中毒debuff", poisoningDebuff);

        printBattleLogs();
    }

    /**
     * 测试等级3：60点基础伤害 + 中毒爆发
     */
    @Test
    public void testScrapePoisonLevel3WithPoison() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill scrapePoison = createSkill("scrape_poison", 3);

        // 先添加中毒debuff
        PoisoningDebuff existingPoison = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 0);
        existingPoison.setStack(5);
        testMonster.getActiveBuffList().add(existingPoison);
        testMonster.markAttributeCacheDirty();

        int monsterHpBefore = testMonster.getCurrentHp();
        int magicalAtk = testPlayer.getFinalAttributes().magicalAtk;

        // When
        battleManager.executeSkill(testPlayer, scrapePoison,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证造成基础伤害和额外伤害
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("应该造成伤害", actualDamage > 0);

        // 验证中毒debuff被移除
        PoisoningDebuff poisoningDebuff = (PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null);
        assertNull("中毒debuff应该被移除", poisoningDebuff);

        // 验证日志包含爆发信息
        assertLogExists(LogType.ACTION);
        assertLogContains(LogType.ACTION, "【刮骨疗毒】");

        System.out.println("法术攻击：" + magicalAtk);
        System.out.println("实际造成伤害：" + actualDamage);

        printBattleLogs();
    }

    /**
     * 测试等级5：80点基础伤害 + 高层数中毒爆发
     */
    @Test
    public void testScrapePoisonLevel5HighStacks() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill scrapePoison = createSkill("scrape_poison", 5);

        // 先添加高层数中毒debuff
        PoisoningDebuff existingPoison = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 0);
        existingPoison.setStack(10);
        testMonster.getActiveBuffList().add(existingPoison);
        testMonster.markAttributeCacheDirty();

        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, scrapePoison,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证造成大量伤害
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("应该造成大量伤害", actualDamage > 0);

        // 验证中毒debuff被移除
        PoisoningDebuff poisoningDebuff = (PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null);
        assertNull("中毒debuff应该被移除", poisoningDebuff);

        System.out.println("高层数中毒爆发伤害：" + actualDamage);

        printBattleLogs();
    }

    /**
     * 测试中毒爆发伤害计算
     */
    @Test
    public void testScrapePoisonExplosionDamage() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill scrapePoison = createSkill("scrape_poison", 1);

        // 先添加已知层数的中毒debuff
        PoisoningDebuff existingPoison = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 0);
        existingPoison.setStack(4);
        testMonster.getActiveBuffList().add(existingPoison);
        testMonster.markAttributeCacheDirty();

        int monsterHpBefore = testMonster.getCurrentHp();
        int magicalAtk = testPlayer.getFinalAttributes().magicalAtk;

        // When
        battleManager.executeSkill(testPlayer, scrapePoison,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证造成伤害
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("应该造成伤害", actualDamage > 0);

        // 验证中毒debuff被移除
        PoisoningDebuff poisoningDebuff = (PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null);
        assertNull("中毒debuff应该被移除", poisoningDebuff);

        System.out.println("法术攻击：" + magicalAtk);
        System.out.println("实际总伤害：" + actualDamage);

        printBattleLogs();
    }

    /**
     * 测试不同层数的中毒爆发
     */
    @Test
    public void testScrapePoisonDifferentStacks() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill scrapePoison = createSkill("scrape_poison", 1);

        int[] stackLevels = {1, 3, 5, 10};
        int[] damages = new int[stackLevels.length];

        for (int i = 0; i < stackLevels.length; i++) {
            // 重置怪物状态
            testMonster.getActiveBuffList().clear();
            testMonster.markAttributeCacheDirty();
            testMonster.setCurrentHp(testMonster.getFinalAttributes().maxHp);

            // 添加中毒debuff
            PoisoningDebuff existingPoison = new PoisoningDebuff(
                    "test_poison", "测试中毒", "中毒",
                    BuffType.DEBUFF, true, -1, 999, true, 0);
            existingPoison.setStack(stackLevels[i]);
            testMonster.getActiveBuffList().add(existingPoison);
            testMonster.markAttributeCacheDirty();

            int monsterHpBefore = testMonster.getCurrentHp();

            // 施法
            battleManager.executeSkill(testPlayer, scrapePoison,
                    java.util.Arrays.asList(testMonster), battleContext);

            damages[i] = monsterHpBefore - testMonster.getCurrentHp();
        }

        // 验证层数越高伤害越大
        assertTrue("5层伤害应该大于3层", damages[2] > damages[1]);
        assertTrue("10层伤害应该大于5层", damages[3] > damages[2]);

        System.out.println("1层伤害：" + damages[0]);
        System.out.println("3层伤害：" + damages[1]);
        System.out.println("5层伤害：" + damages[2]);
        System.out.println("10层伤害：" + damages[3]);

        printBattleLogs();
    }

    /**
     * 测试消耗
     */
    @Test
    public void testScrapePoisonCost() {
        // Given
        ActiveSkill scrapePoison = createSkill("scrape_poison", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, scrapePoison,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 等级1消耗1AP和14MP
        assertEquals("应该消耗1行动点", apBefore - 1, testPlayer.getCurrentActionPoints());
        assertEquals("应该消耗14MP", mpBefore - 14, testPlayer.getCurrentMp());
        assertEquals("不应该消耗HP", hpBefore, testPlayer.getCurrentHp());
    }

    /**
     * 测试无中毒目标时不移除任何debuff
     */
    @Test
    public void testScrapePoisonNoPoisonNoRemoval() {
        // Given
        ActiveSkill scrapePoison = createSkill("scrape_poison", 1);

        // 添加其他类型的debuff（燃烧）
        com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff burningDebuff =
                new com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff(
                        "test_burning", "测试燃烧", "燃烧",
                        BuffType.DEBUFF, true, -1, 999, true, 1);
        burningDebuff.setStack(2);
        testMonster.getActiveBuffList().add(burningDebuff);
        testMonster.markAttributeCacheDirty();

        // When
        battleManager.executeSkill(testPlayer, scrapePoison,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 燃烧debuff应该仍然存在
        com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff finalBurning =
                (com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff) testMonster.getActiveBuffList().stream()
                        .filter(buff -> buff instanceof com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff)
                        .findFirst()
                        .orElse(null);

        assertNotNull("燃烧debuff应该仍然存在", finalBurning);
        assertEquals("燃烧层数应该不变", 2, finalBurning.getStackCount());

        printBattleLogs();
    }
}
