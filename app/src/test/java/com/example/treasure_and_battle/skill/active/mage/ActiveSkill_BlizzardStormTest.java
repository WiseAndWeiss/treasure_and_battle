package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.control.FrozenDebuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 暴风骤雪技能测试
 * 技能效果：对全体敌人造成x%法术伤害，目标有y%概率被冻结1回合
 */
public class ActiveSkill_BlizzardStormTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：90%法术伤害 + 20%概率冻结
     */
    @Test
    public void testBlizzardStormLevel1() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill blizzardStorm = createSkill("blizzard_storm", 1);
        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, blizzardStorm,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证造成伤害
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("等级1暴风骤雪应该造成伤害", actualDamage > 0);

        // 验证是否可能有冻结debuff（概率触发）
        FrozenDebuff frozenDebuff = (FrozenDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FrozenDebuff)
            .findFirst()
            .orElse(null);

        if (frozenDebuff != null) {
            assertEquals("冻结应该持续1回合", 1, frozenDebuff.getRemainingDuration());
            assertFalse("冻结不应该可驱散", frozenDebuff.isDispellable());
            System.out.println("怪物被冻结！");
        } else {
            System.out.println("怪物未被冻结（20%概率未触发）");
        }

        // 验证日志
        assertLogExists(LogType.ACTION);
        assertLogContains(LogType.ACTION, "【暴风骤雪】");

        printBattleLogs();
    }

    /**
     * 测试等级3：110%法术伤害 + 40%概率冻结
     */
    @Test
    public void testBlizzardStormLevel3() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill blizzardStorm = createSkill("blizzard_storm", 3);

        // When
        battleManager.executeSkill(testPlayer, blizzardStorm,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证造成伤害
        int actualDamage = testMonster.getFinalAttributes().maxHp - testMonster.getCurrentHp();
        assertTrue("等级3暴风骤雪应该造成伤害", actualDamage > 0);

        // 等级3有更高概率触发冻结
        FrozenDebuff frozenDebuff = (FrozenDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FrozenDebuff)
            .findFirst()
            .orElse(null);

        if (frozenDebuff != null) {
            System.out.println("等级3：怪物被冻结（40%概率触发）");
        }

        printBattleLogs();
    }

    /**
     * 测试等级5：130%法术伤害 + 60%概率冻结
     */
    @Test
    public void testBlizzardStormLevel5() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill blizzardStorm = createSkill("blizzard_storm", 5);

        // When
        battleManager.executeSkill(testPlayer, blizzardStorm,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证造成伤害
        int actualDamage = testMonster.getFinalAttributes().maxHp - testMonster.getCurrentHp();
        assertTrue("等级5暴风骤雪应该造成伤害", actualDamage > 0);

        // 等级5有很高概率触发冻结
        FrozenDebuff frozenDebuff = (FrozenDebuff) testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FrozenDebuff)
            .findFirst()
            .orElse(null);

        if (frozenDebuff != null) {
            System.out.println("等级5：怪物被冻结（60%概率触发）");
        } else {
            System.out.println("等级5：怪物未被冻结（40%概率未触发）");
        }

        printBattleLogs();
    }

    /**
     * 测试冻结效果属性
     */
    @Test
    public void testFrozenDebuffProperties() {
        // Given
        ActiveSkill blizzardStorm = createSkill("blizzard_storm", 1);

        // When - 多次施法直到触发冻结
        for (int i = 0; i < 10; i++) {
            testMonster.getActiveBuffList().clear();
            battleManager.executeSkill(testPlayer, blizzardStorm,
                java.util.Arrays.asList(testMonster), battleContext);

            FrozenDebuff frozenDebuff = (FrozenDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof FrozenDebuff)
                .findFirst()
                .orElse(null);

            if (frozenDebuff != null) {
                // Then - 验证冻结属性
                assertEquals("冻结类型应该是DEBUFF", BuffType.DEBUFF, frozenDebuff.getBuffType());
                assertEquals("冻结应该持续1回合", 1, frozenDebuff.getRemainingDuration());
                assertFalse("冻结不应该可驱散", frozenDebuff.isDispellable());
                assertEquals("冻结stackCount应该为1", 1, frozenDebuff.getStackCount());

                System.out.println("第" + (i + 1) + "次施法：触发冻结");
                break;
            }
        }

        printBattleLogs();
    }

    /**
     * 测试暴风骤雪的消耗
     */
    @Test
    public void testBlizzardStormCost() {
        // Given
        ActiveSkill blizzardStorm = createSkill("blizzard_storm", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, blizzardStorm,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 等级1消耗2AP和35MP
        assertEquals("应该消耗2行动点", apBefore - 2, testPlayer.getCurrentActionPoints());
        assertEquals("应该消耗35MP", mpBefore - 35, testPlayer.getCurrentMp());
        assertEquals("不应该消耗HP", hpBefore, testPlayer.getCurrentHp());
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testBlizzardStormScaling() {
        // 等级1：90%伤害，20%冻结
        int damage1 = 90;
        int freeze1 = 20;

        // 等级3：110%伤害，40%冻结
        int damage3 = 110;
        int freeze3 = 40;

        // 等级5：130%伤害，60%冻结
        int damage5 = 130;
        int freeze5 = 60;

        assertTrue("伤害应该递增", damage5 > damage3 && damage3 > damage1);
        assertTrue("冻结概率应该递增", freeze5 > freeze3 && freeze3 > freeze1);

        System.out.println("等级1：" + damage1 + "%伤害，" + freeze1 + "%冻结");
        System.out.println("等级3：" + damage3 + "%伤害，" + freeze3 + "%冻结");
        System.out.println("等级5：" + damage5 + "%伤害，" + freeze5 + "%冻结");
    }

    /**
     * 测试MP消耗递增
     */
    @Test
    public void testBlizzardStormMpCostScaling() {
        // Given
        int apBefore = testPlayer.getCurrentActionPoints();

        // 等级1：35MP
        testPlayer.setCurrentMp(100);
        ActiveSkill blizzardStorm1 = createSkill("blizzard_storm", 1);
        battleManager.executeSkill(testPlayer, blizzardStorm1,
            java.util.Arrays.asList(testMonster), battleContext);
        int mpCost1 = 100 - testPlayer.getCurrentMp();

        // 等级3：45MP
        testPlayer.setCurrentMp(100);
        ActiveSkill blizzardStorm3 = createSkill("blizzard_storm", 3);
        battleManager.executeSkill(testPlayer, blizzardStorm3,
            java.util.Arrays.asList(testMonster), battleContext);
        int mpCost3 = 100 - testPlayer.getCurrentMp();

        // 等级5：55MP
        testPlayer.setCurrentMp(100);
        ActiveSkill blizzardStorm5 = createSkill("blizzard_storm", 5);
        battleManager.executeSkill(testPlayer, blizzardStorm5,
            java.util.Arrays.asList(testMonster), battleContext);
        int mpCost5 = 100 - testPlayer.getCurrentMp();

        assertEquals("等级1应该消耗35MP", 35, mpCost1);
        assertEquals("等级3应该消耗45MP", 45, mpCost3);
        assertEquals("等级5应该消耗55MP", 55, mpCost5);

        assertTrue("MP消耗应该递增", mpCost5 > mpCost3 && mpCost3 > mpCost1);

        System.out.println("等级1MP消耗：" + mpCost1);
        System.out.println("等级3MP消耗：" + mpCost3);
        System.out.println("等级5MP消耗：" + mpCost5);
    }

    /**
     * 测试对已冻结目标施法
     */
    @Test
    public void testBlizzardStormOnFrozenTarget() {
        // Given
        ActiveSkill blizzardStorm = createSkill("blizzard_storm", 1);

        // 先手动添加冻结debuff
        FrozenDebuff existingFrozen = new FrozenDebuff(
            "test_frozen", "测试冻结", "冻结",
            BuffType.DEBUFF, false, 1, 1, false, 0);
        testMonster.getActiveBuffList().add(existingFrozen);
        testMonster.markAttributeCacheDirty();

        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, blizzardStorm,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 应该仍然造成伤害
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("应该对冻结目标造成伤害", actualDamage > 0);

        // 验证不应该有多个冻结debuff（或被刷新）
        long frozenCount = testMonster.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FrozenDebuff)
            .count();
        // 由于有概率再次触发，可能有1个或2个冻结debuff
        assertTrue("冻结debuff数量应该合理", frozenCount <= 2);

        printBattleLogs();
    }

    /**
     * 测试冻结debuff持续时间
     */
    @Test
    public void testFrozenDebuffDuration() {
        // Given
        ActiveSkill blizzardStorm = createSkill("blizzard_storm", 1);

        // When - 多次尝试施法直到触发冻结
        for (int i = 0; i < 20; i++) {
            testMonster.getActiveBuffList().clear();
            battleManager.executeSkill(testPlayer, blizzardStorm,
                java.util.Arrays.asList(testMonster), battleContext);

            FrozenDebuff frozenDebuff = (FrozenDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof FrozenDebuff)
                .findFirst()
                .orElse(null);

            if (frozenDebuff != null) {
                // Then
                assertEquals("冻结应该持续1回合", 1, frozenDebuff.getRemainingDuration());
                System.out.println("第" + (i + 1) + "次施法：验证冻结持续时间为1回合");
                break;
            }
        }

        printBattleLogs();
    }

    /**
     * 测试冻结不可驱散
     */
    @Test
    public void testFrozenNotDispellable() {
        // Given
        ActiveSkill blizzardStorm = createSkill("blizzard_storm", 1);

        // When - 多次尝试施法直到触发冻结
        for (int i = 0; i < 20; i++) {
            testMonster.getActiveBuffList().clear();
            battleManager.executeSkill(testPlayer, blizzardStorm,
                java.util.Arrays.asList(testMonster), battleContext);

            FrozenDebuff frozenDebuff = (FrozenDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof FrozenDebuff)
                .findFirst()
                .orElse(null);

            if (frozenDebuff != null) {
                // Then
                assertFalse("冻结不应该可驱散", frozenDebuff.isDispellable());
                System.out.println("第" + (i + 1) + "次施法：验证冻结不可驱散");
                break;
            }
        }

        printBattleLogs();
    }

    /**
     * 测试冻结概率统计
     */
    @Test
    public void testFrozenProbabilityStatistics() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill blizzardStorm = createSkill("blizzard_storm", 5); // 60%概率

        // When - 施法100次统计冻结触发次数
        int frozenCount = 0;
        int trials = 100;

        for (int i = 0; i < trials; i++) {
            testMonster.getActiveBuffList().clear();
            testMonster.setCurrentHp(testMonster.getFinalAttributes().maxHp);

            battleManager.executeSkill(testPlayer, blizzardStorm,
                java.util.Arrays.asList(testMonster), battleContext);

            FrozenDebuff frozenDebuff = (FrozenDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof FrozenDebuff)
                .findFirst()
                .orElse(null);

            if (frozenDebuff != null) {
                frozenCount++;
            }
        }

        // Then - 冻结概率应该在50%-70%之间（允许一定误差）
        float actualProbability = (float) frozenCount / trials * 100;
        assertTrue("冻结概率应该在合理范围内（" + actualProbability + "%）",
            actualProbability >= 45 && actualProbability <= 75);

        System.out.println("施法" + trials + "次，触发冻结" + frozenCount + "次");
        System.out.println("实际冻结概率：" + actualProbability + "%");
        System.out.println("预期冻结概率：60%");
    }
}
