package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 凝神定气技能测试
 * 技能效果：提升自身x点魔法攻击与y点法术暴击率，持续1回合
 */
public class ActiveSkill_ConcentrateSpiritTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：30点魔法攻击 + 2%法术暴击率
     */
    @Test
    public void testConcentrateSpiritLevel1() {
        // Given
        ActiveSkill concentrateSpirit = createSkill("concentrate_spirit", 1);
        int magAtkBefore = testPlayer.getFinalAttributes().magicalAtk;
        float magCritRateBefore = testPlayer.getFinalAttributes().magicalCritRate;

        // When
        battleManager.executeSkill(testPlayer, concentrateSpirit,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        int magAtkAfter = testPlayer.getFinalAttributes().magicalAtk;
        float magCritRateAfter = testPlayer.getFinalAttributes().magicalCritRate;

        assertEquals("魔法攻击应该提升30点", magAtkBefore + 30, magAtkAfter);
        assertEquals("法术暴击率应该提升2%", magCritRateBefore + 2, magCritRateAfter, 0.1f);

        // 验证有2个buff（魔法攻击和法术暴击）
        long buffCount = testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .count();
        assertEquals("应该有2个属性buff", 2, buffCount);

        // 验证日志
        assertLogExists(LogType.BUFF);
        assertLogContains(LogType.BUFF, "【凝神定气】");

        printBattleLogs();
    }

    /**
     * 测试等级3：60点魔法攻击 + 4%法术暴击率
     */
    @Test
    public void testConcentrateSpiritLevel3() {
        // Given
        ActiveSkill concentrateSpirit = createSkill("concentrate_spirit", 3);
        int magAtkBefore = testPlayer.getFinalAttributes().magicalAtk;
        float magCritRateBefore = testPlayer.getFinalAttributes().magicalCritRate;

        // When
        battleManager.executeSkill(testPlayer, concentrateSpirit,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        int magAtkAfter = testPlayer.getFinalAttributes().magicalAtk;
        float magCritRateAfter = testPlayer.getFinalAttributes().magicalCritRate;

        assertEquals("魔法攻击应该提升60点", magAtkBefore + 60, magAtkAfter);
        assertEquals("法术暴击率应该提升4%", magCritRateBefore + 4, magCritRateAfter, 0.1f);

        printBattleLogs();
    }

    /**
     * 测试等级5：90点魔法攻击 + 6%法术暴击率
     */
    @Test
    public void testConcentrateSpiritLevel5() {
        // Given
        ActiveSkill concentrateSpirit = createSkill("concentrate_spirit", 5);
        int magAtkBefore = testPlayer.getFinalAttributes().magicalAtk;
        float magCritRateBefore = testPlayer.getFinalAttributes().magicalCritRate;

        // When
        battleManager.executeSkill(testPlayer, concentrateSpirit,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        int magAtkAfter = testPlayer.getFinalAttributes().magicalAtk;
        float magCritRateAfter = testPlayer.getFinalAttributes().magicalCritRate;

        assertEquals("魔法攻击应该提升90点", magAtkBefore + 90, magAtkAfter);
        assertEquals("法术暴击率应该提升6%", magCritRateBefore + 6, magCritRateAfter, 0.1f);

        printBattleLogs();
    }

    /**
     * 测试凝神定气的消耗
     */
    @Test
    public void testConcentrateSpiritCost() {
        // Given
        ActiveSkill concentrateSpirit = createSkill("concentrate_spirit", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, concentrateSpirit,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then - 等级1消耗1AP和12MP
        assertEquals("应该消耗1行动点", apBefore - 1, testPlayer.getCurrentActionPoints());
        assertEquals("应该消耗12MP", mpBefore - 12, testPlayer.getCurrentMp());
        assertEquals("不应该消耗HP", hpBefore, testPlayer.getCurrentHp());
    }

    /**
     * 测试无冷却时间
     */
    @Test
    public void testConcentrateSpiritNoCooldown() {
        // Given
        ActiveSkill concentrateSpirit = createSkill("concentrate_spirit", 1);

        // When - 连续施放2次
        battleManager.executeSkill(testPlayer, concentrateSpirit,
                java.util.Arrays.asList(testPlayer), battleContext);

        // 清空buff列表
        testPlayer.getActiveBuffList().clear();

        // 再次施放
        battleManager.executeSkill(testPlayer, concentrateSpirit,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then - 应该能再次成功施放（无冷却）
        long buffCount = testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .count();
        assertEquals("第二次施放后应该有2个buff", 2, buffCount);

        printBattleLogs();
    }

    /**
     * 测试buff持续时间
     */
    @Test
    public void testConcentrateSpiritBuffDuration() {
        // Given
        ActiveSkill concentrateSpirit = createSkill("concentrate_spirit", 1);

        // When
        battleManager.executeSkill(testPlayer, concentrateSpirit,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        AttributeBuff magAtkBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("concentrate_spirit_magical_atk"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有魔法攻击buff", magAtkBuff);
        assertEquals("buff应该持续1回合", 1, magAtkBuff.getRemainingDuration());

        printBattleLogs();
    }
}
