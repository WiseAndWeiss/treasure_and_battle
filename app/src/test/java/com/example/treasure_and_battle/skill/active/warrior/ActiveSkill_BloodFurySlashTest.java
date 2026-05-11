package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 血怒斩技能测试
 * 技能效果：消耗生命，对单体造成x%物理攻击伤害，为敌人附加y层流血debuff
 */
public class ActiveSkill_BloodFurySlashTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：120%伤害，2层流血，消耗8HP
     */
    @Test
    public void testBloodFurySlashLevel1() {
        // Given
        ActiveSkill bloodFurySlash = createSkill("blood_fury_slash", 1);
        int monsterHpBefore = testMonster.getCurrentHp();
        testPlayer.setCurrentHp(200);

        // When
        battleManager.executeSkill(testPlayer, bloodFurySlash,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证伤害
        int damageDealt = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("应该造成伤害", damageDealt > 0);

        // 验证HP消耗
        assertEquals("应该消耗8HP", 192, testPlayer.getCurrentHp());

        // 验证流血debuff
        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        BleedingDebuff bleedingDebuff = (BleedingDebuff) buffs.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有流血debuff", bleedingDebuff);
        assertEquals("流血层数应该为2", 2, bleedingDebuff.getStackCount());

        // 验证日志
        assertLogContains(LogType.ACTION, "【血怒斩】");
        assertLogContains(LogType.ACTION, "附加了 2 层流血debuff");

        printBattleLogs();
    }

    /**
     * 测试等级5：150%伤害，4层流血，消耗12HP
     */
    @Test
    public void testBloodFurySlashLevel5() {
        // Given
        ActiveSkill bloodFurySlash = createSkill("blood_fury_slash", 5);
        int monsterHpBefore = testMonster.getCurrentHp();
        testPlayer.setCurrentHp(200);

        // When
        battleManager.executeSkill(testPlayer, bloodFurySlash,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        int damageDealt = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("应该造成伤害", damageDealt > 0);

        assertEquals("应该消耗12HP", 188, testPlayer.getCurrentHp());

        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        BleedingDebuff bleedingDebuff = (BleedingDebuff) buffs.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有流血debuff", bleedingDebuff);
        assertEquals("流血层数应该为4", 4, bleedingDebuff.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试流血debuff效果
     */
    @Test
    public void testBleedingDebuffEffect() {
        // Given
        ActiveSkill bloodFurySlash = createSkill("blood_fury_slash", 3); // 3层流血
        testMonster.setCurrentHp(300);

        battleManager.executeSkill(testPlayer, bloodFurySlash,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        BleedingDebuff bleedingDebuff = (BleedingDebuff) buffs.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有流血debuff", bleedingDebuff);
        assertEquals("流血层数应该为3", 3, bleedingDebuff.getStackCount());

        int hpBeforeTick = testMonster.getCurrentHp();
        int maxHp = testMonster.getFinalAttributes().maxHp;

        // When - 回合结束触发流血
        BuffManager.getInstance(context).triggerBuffs(testMonster, battleContext,
            com.example.treasure_and_battle.model.common.TriggerType.ON_ROUND_END);

        // Then - 验证流血伤害（每层1%最大HP）
        int expectedDamage = (int) (maxHp * 0.01 * 3);
        int hpAfterTick = testMonster.getCurrentHp();
        int actualDamage = hpBeforeTick - hpAfterTick;

        assertEquals("流血伤害应该正确", expectedDamage, actualDamage);

        // 验证层数减少（需要调用tickBuffs来减少层数）
        BuffManager.getInstance(context).tickBuffs(testMonster);
        assertEquals("流血层数应该减少1层", 2, bleedingDebuff.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试流血层数递减
     */
    @Test
    public void testBleedingStackDecay() {
        // Given
        ActiveSkill bloodFurySlash = createSkill("blood_fury_slash", 5); // 4层
        battleManager.executeSkill(testPlayer, bloodFurySlash,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        BleedingDebuff bleedingDebuff = (BleedingDebuff) buffs.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);

        // When - 经过多个回合
        for (int i = 0; i < 3; i++) {
            BuffManager.getInstance(context).triggerBuffs(testMonster, battleContext,
                com.example.treasure_and_battle.model.common.TriggerType.ON_ROUND_END);
            BuffManager.getInstance(context).tickBuffs(testMonster);
        }

        // Then - 流血debuff应该还存在
        List<BaseBuff> buffsAfter = testMonster.getActiveBuffList();
        BleedingDebuff bleedingDebuffAfter = (BleedingDebuff) buffsAfter.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("流血debuff应该仍然存在", bleedingDebuffAfter);
        assertEquals("流血层数应该为1", 1, bleedingDebuffAfter.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试流血debuff消失
     */
    @Test
    public void testBleedingDebuffRemoval() {
        // Given
        ActiveSkill bloodFurySlash = createSkill("blood_fury_slash", 1); // 2层
        battleManager.executeSkill(testPlayer, bloodFurySlash,
            java.util.Arrays.asList(testMonster), battleContext);

        // When - 经过足够回合让流血消失
        for (int i = 0; i < 3; i++) {
            BuffManager.getInstance(context).triggerBuffs(testMonster, battleContext,
                com.example.treasure_and_battle.model.common.TriggerType.ON_ROUND_END);
            BuffManager.getInstance(context).tickBuffs(testMonster);
        }

        // Then - 流血debuff应该消失
        List<BaseBuff> buffsAfter = testMonster.getActiveBuffList();
        BleedingDebuff bleedingDebuff = (BleedingDebuff) buffsAfter.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);

        assertNull("流血debuff应该消失", bleedingDebuff);

        printBattleLogs();
    }

    /**
     * 测试多次施放流血叠加
     */
    @Test
    public void testMultipleBleedingStacking() {
        // Given
        ActiveSkill bloodFurySlash = createSkill("blood_fury_slash", 1);

        // When - 施放两次
        battleManager.executeSkill(testPlayer, bloodFurySlash,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs1 = testMonster.getActiveBuffList();
        BleedingDebuff bleedingDebuff1 = (BleedingDebuff) buffs1.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);

        int stacksAfterFirst = bleedingDebuff1.getStackCount();

        battleContext.battleLogs.clear();
        battleManager.executeSkill(testPlayer, bloodFurySlash,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 应该只有一个流血debuff，但层数叠加
        List<BaseBuff> buffs2 = testMonster.getActiveBuffList();
        long bleedingCount = buffs2.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .count();

        assertEquals("应该只有1个流血debuff", 1, bleedingCount);

        BleedingDebuff bleedingDebuff2 = (BleedingDebuff) buffs2.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);

        assertEquals("流血层数应该叠加", stacksAfterFirst + 2, bleedingDebuff2.getStackCount());

        System.out.println("第一次施放后层数: " + stacksAfterFirst);
        System.out.println("第二次施放后层数: " + bleedingDebuff2.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试HP消耗
     */
    @Test
    public void testHpCost() {
        // Given
        ActiveSkill bloodFurySlash = createSkill("blood_fury_slash", 1);
        testPlayer.setCurrentHp(200);

        // When
        battleManager.executeSkill(testPlayer, bloodFurySlash,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 等级1应该消耗8HP
        assertEquals("应该消耗8HP", 192, testPlayer.getCurrentHp());

        printBattleLogs();
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testScalingByLevel() {
        testPlayer.setCurrentHp(200);

        // 测试等级1（2层流血）
        ActiveSkill skill1 = createSkill("blood_fury_slash", 1);
        battleManager.executeSkill(testPlayer, skill1,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs1 = testMonster.getActiveBuffList();
        BleedingDebuff bleeding1 = (BleedingDebuff) buffs1.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);
        int stacks1 = bleeding1.getStackCount();

        // 测试等级5（4层流血）
        testMonster.getActiveBuffList().clear();
        battleContext.battleLogs.clear();
        ActiveSkill skill5 = createSkill("blood_fury_slash", 5);
        battleManager.executeSkill(testPlayer, skill5,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs5 = testMonster.getActiveBuffList();
        BleedingDebuff bleeding5 = (BleedingDebuff) buffs5.stream()
            .filter(buff -> buff instanceof BleedingDebuff)
            .findFirst()
            .orElse(null);
        int stacks5 = bleeding5.getStackCount();

        // 验证流血层数递增
        assertTrue("等级5流血层数应该大于等级1", stacks5 > stacks1);

        System.out.println("等级1流血层数: " + stacks1);
        System.out.println("等级5流血层数: " + stacks5);

        printBattleLogs();
    }

    /**
     * 测试日志记录
     */
    @Test
    public void testBattleLog() {
        // Given
        ActiveSkill bloodFurySlash = createSkill("blood_fury_slash", 3);

        // When
        battleManager.executeSkill(testPlayer, bloodFurySlash,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证日志
        assertLogExists(LogType.ACTION);
        assertLogContains(LogType.ACTION, "【血怒斩】");
        assertLogContains(LogType.ACTION, "附加了");
        assertLogContains(LogType.ACTION, "层流血debuff");

        printBattleLogs();
    }
}
