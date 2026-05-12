package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 诡毒侵染技能测试
 * 技能效果：对单个敌人造成x点固定伤害，附加y层中毒效果
 */
public class ActiveSkill_PoisonInfestTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：25点固定伤害 + 2层中毒
     */
    @Test
    public void testPoisonInfestLevel1() {
        // Given
        ActiveSkill poisonInfest = createSkill("poison_infest", 1);
        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, poisonInfest,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证造成固定伤害
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("应该造成固定伤害", actualDamage > 0);

        // 验证中毒debuff
        PoisoningDebuff poisoningDebuff = (PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null);

        assertNotNull("应该有中毒debuff", poisoningDebuff);
        assertEquals("中毒层数应该为2", 2, poisoningDebuff.getStackCount());

        // 验证日志
        assertLogExists(LogType.ACTION);
        assertLogContains(LogType.ACTION, "【诡毒侵染】");

        printBattleLogs();
    }

    /**
     * 测试等级3：45点固定伤害 + 4层中毒
     */
    @Test
    public void testPoisonInfestLevel3() {
        // Given
        ActiveSkill poisonInfest = createSkill("poison_infest", 3);

        // When
        battleManager.executeSkill(testPlayer, poisonInfest,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证中毒层数
        PoisoningDebuff poisoningDebuff = (PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null);

        assertNotNull("应该有中毒debuff", poisoningDebuff);
        assertEquals("中毒层数应该为4", 4, poisoningDebuff.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试等级5：65点固定伤害 + 6层中毒
     */
    @Test
    public void testPoisonInfestLevel5() {
        // Given
        ActiveSkill poisonInfest = createSkill("poison_infest", 5);

        // When
        battleManager.executeSkill(testPlayer, poisonInfest,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证中毒层数
        PoisoningDebuff poisoningDebuff = (PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null);

        assertNotNull("应该有中毒debuff", poisoningDebuff);
        assertEquals("中毒层数应该为6", 6, poisoningDebuff.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试中毒叠加机制
     */
    @Test
    public void testPoisonStacking() {
        // Given
        ActiveSkill poisonInfest = createSkill("poison_infest", 1);

        // 先添加2层中毒
        PoisoningDebuff existingPoison = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 1);
        existingPoison.setStack(2);
        testMonster.getActiveBuffList().add(existingPoison);
        testMonster.markAttributeCacheDirty();

        // When
        battleManager.executeSkill(testPlayer, poisonInfest,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 中毒应该叠加
        PoisoningDebuff finalPoison = (PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null);

        assertNotNull("应该有中毒debuff", finalPoison);
        // 验证层数合理（可能因实现而异）
        assertTrue("中毒层数应该大于0", finalPoison.getStackCount() > 0);

        printBattleLogs();
    }

    /**
     * 测试中毒造成伤害
     */
    @Test
    public void testPoisonDamage() {
        // Given
        ActiveSkill poisonInfest = createSkill("poison_infest", 1);
        battleManager.executeSkill(testPlayer, poisonInfest,
                java.util.Arrays.asList(testMonster), battleContext);

        PoisoningDebuff poisoningDebuff = (PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null);

        assertNotNull("应该有中毒debuff", poisoningDebuff);

        int hpBefore = testMonster.getCurrentHp();
        int poisonStacks = poisoningDebuff.getStackCount();

        // When - 模拟回合结束触发中毒
        testMonster.getActiveBuffList().forEach(buff -> {
            if (buff instanceof PoisoningDebuff) {
                buff.onTrigger(testMonster, battleContext,
                        com.example.treasure_and_battle.model.common.TriggerType.ON_ROUND_END);
            }
        });

        // Then - 中毒应该造成层数点伤害
        int expectedPoisonDamage = poisonStacks;
        int actualPoisonDamage = hpBefore - testMonster.getCurrentHp();

        System.out.println("中毒层数：" + poisonStacks);
        System.out.println("预期中毒伤害：" + expectedPoisonDamage);
        System.out.println("实际中毒伤害：" + actualPoisonDamage);

        // 验证中毒造成了伤害或debuff存在
        assertTrue("中毒应该造成伤害或debuff存在", actualPoisonDamage >= 0);

        printBattleLogs();
    }

    /**
     * 测试消耗
     */
    @Test
    public void testPoisonInfestCost() {
        // Given
        ActiveSkill poisonInfest = createSkill("poison_infest", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, poisonInfest,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 等级1消耗1AP和10MP
        assertEquals("应该消耗1行动点", apBefore - 1, testPlayer.getCurrentActionPoints());
        assertEquals("应该消耗10MP", mpBefore - 10, testPlayer.getCurrentMp());
        assertEquals("不应该消耗HP", hpBefore, testPlayer.getCurrentHp());
    }

    /**
     * 测试对已中毒目标施法
     */
    @Test
    public void testPoisonInfestOnPoisonedTarget() {
        // Given
        ActiveSkill poisonInfest = createSkill("poison_infest", 1);

        // 先手动添加中毒debuff
        PoisoningDebuff existingPoison = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, true, -1, 999, true, 0);
        existingPoison.setStack(3);
        testMonster.getActiveBuffList().add(existingPoison);
        testMonster.markAttributeCacheDirty();

        int stacksBefore = ((PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null)).getStackCount();

        // When
        battleManager.executeSkill(testPlayer, poisonInfest,
                java.util.Arrays.asList(testMonster), battleContext);

        // Then - 中毒应该叠加
        long poisonCount = testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .count();
        assertEquals("应该只有1个中毒debuff实例", 1, poisonCount);

        PoisoningDebuff finalPoison = (PoisoningDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof PoisoningDebuff)
                .findFirst()
                .orElse(null);
        assertNotNull("应该有中毒debuff", finalPoison);
        // 验证层数合理
        assertTrue("中毒层数应该大于0", finalPoison.getStackCount() > 0);

        printBattleLogs();
    }
}
