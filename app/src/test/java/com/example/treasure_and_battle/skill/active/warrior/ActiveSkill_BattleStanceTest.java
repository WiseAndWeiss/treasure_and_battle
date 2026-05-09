package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 战斗姿态技能测试
 * 技能效果：自身物理攻击、物理防御与速度提升x%，持续2回合
 */
public class ActiveSkill_BattleStanceTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：5%提升
     */
    @Test
    public void testBattleStanceLevel1() {
        // Given
        ActiveSkill battleStance = createSkill("battle_stance", 1);
        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int expectedBoost = 5;

        int baseAtk = playerAttr.physicalAtk;
        int baseDef = playerAttr.physicalDef;
        int baseSpd = playerAttr.speed;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, battleStance,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证属性提升
        AttributeSet finalAttr = testPlayer.getFinalAttributes();
        int expectedAtk = Math.round(baseAtk * (1 + expectedBoost / 100.0f));
        int expectedDef = Math.round(baseDef * (1 + expectedBoost / 100.0f));
        int expectedSpd = Math.round(baseSpd * (1 + expectedBoost / 100.0f));

        assertEquals("物理攻击应该提升5%", expectedAtk, finalAttr.physicalAtk);
        assertEquals("物理防御应该提升5%", expectedDef, finalAttr.physicalDef);
        assertEquals("速度应该提升5%", expectedSpd, finalAttr.speed);

        // 验证日志
        assertLogContains(LogType.BUFF, "【战斗姿态】");
        assertLogContains(LogType.BUFF, "物理攻击、防御与速度提升");

        printBattleLogs();
    }

    /**
     * 测试等级3：11%提升
     */
    @Test
    public void testBattleStanceLevel3() {
        // Given
        ActiveSkill battleStance = createSkill("battle_stance", 3);
        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int expectedBoost = 11;

        int baseAtk = playerAttr.physicalAtk;
        int baseDef = playerAttr.physicalDef;
        int baseSpd = playerAttr.speed;

        // When
        battleManager.executeSkill(testPlayer, battleStance,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        AttributeSet finalAttr = testPlayer.getFinalAttributes();
        int expectedAtk = Math.round(baseAtk * (1 + expectedBoost / 100.0f));
        int expectedDef = Math.round(baseDef * (1 + expectedBoost / 100.0f));
        int expectedSpd = Math.round(baseSpd * (1 + expectedBoost / 100.0f));

        assertEquals("物理攻击应该提升11%", expectedAtk, finalAttr.physicalAtk);
        assertEquals("物理防御应该提升11%", expectedDef, finalAttr.physicalDef);
        assertEquals("速度应该提升11%", expectedSpd, finalAttr.speed);

        printBattleLogs();
    }

    /**
     * 测试等级5：18%提升
     */
    @Test
    public void testBattleStanceLevel5() {
        // Given
        ActiveSkill battleStance = createSkill("battle_stance", 5);
        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int expectedBoost = 18;

        int baseAtk = playerAttr.physicalAtk;
        int baseDef = playerAttr.physicalDef;
        int baseSpd = playerAttr.speed;

        // When
        battleManager.executeSkill(testPlayer, battleStance,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        AttributeSet finalAttr = testPlayer.getFinalAttributes();
        int expectedAtk = Math.round(baseAtk * (1 + expectedBoost / 100.0f));
        int expectedDef = Math.round(baseDef * (1 + expectedBoost / 100.0f));
        int expectedSpd = Math.round(baseSpd * (1 + expectedBoost / 100.0f));

        assertEquals("物理攻击应该提升18%", expectedAtk, finalAttr.physicalAtk);
        assertEquals("物理防御应该提升18%", expectedDef, finalAttr.physicalDef);
        assertEquals("速度应该提升18%", expectedSpd, finalAttr.speed);

        printBattleLogs();
    }

    /**
     * 测试buff施加
     */
    @Test
    public void testBuffApplication() {
        // Given
        ActiveSkill battleStance = createSkill("battle_stance", 3);

        // When
        battleManager.executeSkill(testPlayer, battleStance,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证施加了3个AttributeBuff
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        assertEquals("应该有3个buff", 3, buffs.size());

        // 验证都是AttributeBuff类型
        for (BaseBuff buff : buffs) {
            assertTrue("应该是AttributeBuff类型", buff instanceof AttributeBuff);
        }

        // 验证buff持续时间
        for (BaseBuff buff : buffs) {
            assertEquals("buff应该持续2回合", 2, buff.getRemainingDuration());
        }

        printBattleLogs();
    }

    /**
     * 测试资源消耗
     */
    @Test
    public void testResourceCost() {
        // Given
        ActiveSkill battleStance = createSkill("battle_stance", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, battleStance,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        assertEquals("应该消耗2行动点", apBefore - 2, testPlayer.getCurrentActionPoints());
        assertEquals("不应该消耗MP", mpBefore, testPlayer.getCurrentMp());
        assertEquals("不应该消耗HP", hpBefore, testPlayer.getCurrentHp());
    }

    /**
     * 测试buff持续时间
     */
    @Test
    public void testBuffDuration() {
        // Given
        ActiveSkill battleStance = createSkill("battle_stance", 3);
        battleManager.executeSkill(testPlayer, battleStance,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        assertEquals("应该有3个buff", 3, buffs.size());

        // When - 经过1回合
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // Then - buff仍然存在
        List<BaseBuff> buffsAfterTick1 = testPlayer.getActiveBuffList();
        assertEquals("经过1回合后buff应该仍然存在", 3, buffsAfterTick1.size());

        for (BaseBuff buff : buffsAfterTick1) {
            assertEquals("剩余持续时间应该为1", 1, buff.getRemainingDuration());
        }

        // When - 再经过1回合
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // Then - buff应该消失
        List<BaseBuff> buffsAfterTick2 = testPlayer.getActiveBuffList();
        assertEquals("经过2回合后buff应该消失", 0, buffsAfterTick2.size());

        printBattleLogs();
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testScalingByLevel() {
        AttributeSet baseAttr = testPlayer.getBaseAttributes();

        // 测试等级1（5%）
        testPlayer.getActiveBuffList().clear();
        battleContext.battleLogs.clear();
        ActiveSkill skill1 = createSkill("battle_stance", 1);
        battleManager.executeSkill(testPlayer, skill1,
            java.util.Arrays.asList(testMonster), battleContext);
        int atk1 = testPlayer.getFinalAttributes().physicalAtk;

        // 测试等级3（11%）
        testPlayer.getActiveBuffList().clear();
        battleContext.battleLogs.clear();
        ActiveSkill skill3 = createSkill("battle_stance", 3);
        battleManager.executeSkill(testPlayer, skill3,
            java.util.Arrays.asList(testMonster), battleContext);
        int atk3 = testPlayer.getFinalAttributes().physicalAtk;

        // 测试等级5（18%）
        testPlayer.getActiveBuffList().clear();
        battleContext.battleLogs.clear();
        ActiveSkill skill5 = createSkill("battle_stance", 5);
        battleManager.executeSkill(testPlayer, skill5,
            java.util.Arrays.asList(testMonster), battleContext);
        int atk5 = testPlayer.getFinalAttributes().physicalAtk;

        // 验证等级递增效果
        assertTrue("等级3攻击应该大于等级1", atk3 > atk1);
        assertTrue("等级5攻击应该大于等级3", atk5 > atk3);

        System.out.println("等级1攻击: " + atk1);
        System.out.println("等级3攻击: " + atk3);
        System.out.println("等级5攻击: " + atk5);
    }
}
