package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.control.BlindnessDebuff;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 狂风呼啸技能测试
 * 技能效果：对全体敌人造成x%法术伤害，附加2回合y%减速和z%致盲
 */
public class ActiveSkill_WindHowlTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：80%法术伤害 + 10%减速 + 10%致盲
     */
    @Test
    public void testWindHowlLevel1() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill windHowl = createSkill("wind_howl", 1);

        // When
        battleManager.executeSkill(testPlayer, windHowl,
            java.util.Arrays.asList(testMonster), battleContext);

        // 验证减速debuff
        SlowDebuff slowDebuff = (SlowDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof SlowDebuff)
            .findFirst()
            .orElse(null);
        assertNotNull("应该有减速debuff", slowDebuff);
        assertEquals("减速幅度应该为10%", 10.0f, slowDebuff.getSpeedReductionPercent(), 0.1f);

        // 验证致盲debuff
        BlindnessDebuff blindnessDebuff = (BlindnessDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof BlindnessDebuff)
            .findFirst()
            .orElse(null);
        assertNotNull("应该有致盲debuff", blindnessDebuff);
        assertEquals("致盲幅度应该为10%", 10.0f, blindnessDebuff.getHitRateReduction(), 0.1f);

        // 验证持续时间
        assertEquals("减速应该持续2回合", 2, slowDebuff.getRemainingDuration());
        assertEquals("致盲应该持续2回合", 2, blindnessDebuff.getRemainingDuration());

        // 验证日志
        assertLogExists(LogType.ACTION);
        assertLogContains(LogType.ACTION, "【狂风呼啸】");

        printBattleLogs();
    }

    /**
     * 测试等级3：100%法术伤害 + 15%减速 + 15%致盲
     */
    @Test
    public void testWindHowlLevel3() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill windHowl = createSkill("wind_howl", 3);

        // When
        battleManager.executeSkill(testPlayer, windHowl,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证造成伤害和控制效果
        int actualDamage = testMonster.getFinalAttributes().maxHp - testMonster.getCurrentHp();
        assertTrue("等级3狂风呼啸应该造成伤害", actualDamage > 0);

        // 验证减速和致盲幅度
        SlowDebuff slowDebuff = (SlowDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof SlowDebuff)
            .findFirst()
            .orElse(null);
        assertEquals("减速幅度应该为15%", 15.0f, slowDebuff.getSpeedReductionPercent(), 0.1f);

        BlindnessDebuff blindnessDebuff = (BlindnessDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof BlindnessDebuff)
            .findFirst()
            .orElse(null);
        assertEquals("致盲幅度应该为15%", 15.0f, blindnessDebuff.getHitRateReduction(), 0.1f);

        printBattleLogs();
    }

    /**
     * 测试等级5：120%法术伤害 + 25%减速 + 25%致盲
     */
    @Test
    public void testWindHowlLevel5() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill windHowl = createSkill("wind_howl", 5);

        // When
        battleManager.executeSkill(testPlayer, windHowl,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证造成伤害和控制效果
        int actualDamage = testMonster.getFinalAttributes().maxHp - testMonster.getCurrentHp();
        assertTrue("等级5狂风呼啸应该造成伤害", actualDamage > 0);

        // 验证减速和致盲幅度
        SlowDebuff slowDebuff = (SlowDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof SlowDebuff)
            .findFirst()
            .orElse(null);
        assertEquals("减速幅度应该为25%", 25.0f, slowDebuff.getSpeedReductionPercent(), 0.1f);

        BlindnessDebuff blindnessDebuff = (BlindnessDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof BlindnessDebuff)
            .findFirst()
            .orElse(null);
        assertEquals("致盲幅度应该为25%", 25.0f, blindnessDebuff.getHitRateReduction(), 0.1f);

        printBattleLogs();
    }

    /**
     * 测试减速效果
     */
    @Test
    public void testSlowEffect() {
        // Given
        int originalSpeed = testMonster.getFinalAttributes().speed;
        ActiveSkill windHowl = createSkill("wind_howl", 1);

        // When
        battleManager.executeSkill(testPlayer, windHowl,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证减速debuff存在
        SlowDebuff slowDebuff = (SlowDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof SlowDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有减速debuff", slowDebuff);
        assertEquals("减速幅度应该为10%", 10.0f, slowDebuff.getSpeedReductionPercent(), 0.1f);

        System.out.println("原始速度：" + originalSpeed);
        System.out.println("减速幅度：" + slowDebuff.getSpeedReductionPercent() + "%");
        printBattleLogs();
    }

    /**
     * 测试致盲效果
     */
    @Test
    public void testBlindnessEffect() {
        // Given
        float originalHitRate = testMonster.getFinalAttributes().hitRate;
        ActiveSkill windHowl = createSkill("wind_howl", 1);

        // When
        battleManager.executeSkill(testPlayer, windHowl,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证致盲debuff存在
        BlindnessDebuff blindnessDebuff = (BlindnessDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof BlindnessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有致盲debuff", blindnessDebuff);
        assertEquals("致盲幅度应该为10%", 10.0f, blindnessDebuff.getHitRateReduction(), 0.1f);

        System.out.println("原始命中率：" + originalHitRate);
        System.out.println("致盲幅度：" + blindnessDebuff.getHitRateReduction() + "%");
        printBattleLogs();
    }

    /**
     * 测试狂风呼啸的消耗
     */
    @Test
    public void testWindHowlCost() {
        // Given
        ActiveSkill windHowl = createSkill("wind_howl", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, windHowl,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 等级1消耗1AP和25MP
        assertEquals("应该消耗1行动点", apBefore - 1, testPlayer.getCurrentActionPoints());
        assertEquals("应该消耗25MP", mpBefore - 25, testPlayer.getCurrentMp());
        assertEquals("不应该消耗HP", hpBefore, testPlayer.getCurrentHp());
    }

    /**
     * 测试控制效果持续时间
     */
    @Test
    public void testControlDebuffDuration() {
        // Given
        ActiveSkill windHowl = createSkill("wind_howl", 1);

        // When
        battleManager.executeSkill(testPlayer, windHowl,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        SlowDebuff slowDebuff = (SlowDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof SlowDebuff)
            .findFirst()
            .orElse(null);
        BlindnessDebuff blindnessDebuff = (BlindnessDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof BlindnessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有减速debuff", slowDebuff);
        assertNotNull("应该有致盲debuff", blindnessDebuff);

        assertEquals("减速应该持续2回合", 2, slowDebuff.getRemainingDuration());
        assertEquals("致盲应该持续2回合", 2, blindnessDebuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试控制效果可被驱散
     */
    @Test
    public void testControlDebuffDispellable() {
        // Given
        ActiveSkill windHowl = createSkill("wind_howl", 1);

        // When
        battleManager.executeSkill(testPlayer, windHowl,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        SlowDebuff slowDebuff = (SlowDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof SlowDebuff)
            .findFirst()
            .orElse(null);
        BlindnessDebuff blindnessDebuff = (BlindnessDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof BlindnessDebuff)
            .findFirst()
            .orElse(null);

        assertTrue("减速应该可被驱散", slowDebuff.isDispellable());
        assertTrue("致盲应该可被驱散", blindnessDebuff.isDispellable());

        printBattleLogs();
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testWindHowlScaling() {
        // 等级1：80%伤害，10%减速+致盲
        int damage1 = 80;
        int slow1 = 10;
        int blind1 = 10;

        // 等级3：100%伤害，15%减速+致盲
        int damage3 = 100;
        int slow3 = 15;
        int blind3 = 15;

        // 等级5：120%伤害，25%减速+致盲
        int damage5 = 120;
        int slow5 = 25;
        int blind5 = 25;

        assertTrue("伤害应该递增", damage5 > damage3 && damage3 > damage1);
        assertTrue("控制效果应该递增", slow5 > slow3 && slow3 > slow1);
        assertTrue("致盲效果应该递增", blind5 > blind3 && blind3 > blind1);

        System.out.println("等级1：" + damage1 + "%伤害，" + slow1 + "%减速+" + blind1 + "%致盲");
        System.out.println("等级3：" + damage3 + "%伤害，" + slow3 + "%减速+" + blind3 + "%致盲");
        System.out.println("等级5：" + damage5 + "%伤害，" + slow5 + "%减速+" + blind5 + "%致盲");
    }
}
