package com.example.treasure_and_battle.skill.passive.general;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 净化技能测试
 * 技能效果：回合开始时，有{x}%几率移除自身某一种debuff的所有层数
 */
public class PassiveSkill_PurifyTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：20%概率移除debuff
     */
    @Test
    public void testPurifyLevel1() {
        resetEntityStates();

        // Given
        PassiveSkill purify = createPassiveSkill("purify", 1);
        addPassiveSkillToEntity(testPlayer, purify);

        // 添加一个debuff
        SlowDebuff slowDebuff = new SlowDebuff(
            "test_slow", "测试减速", "速度降低",
            BuffType.DEBUFF, true, 3, 1, false, 1.0f, 20f
        );
        testPlayer.getActiveBuffList().add(slowDebuff);
        assertEquals("应该有1个debuff", 1, testPlayer.getActiveBuffList().size());

        // When - 多次触发回合开始，期望至少有一次成功
        boolean purifyTriggered = false;
        for (int i = 0; i < 20; i++) {
            battleContext.battleLogs.clear();
            triggerRoundStart();

            boolean hasSlowDebuff = testPlayer.getActiveBuffList().stream()
                .anyMatch(buff -> buff instanceof SlowDebuff);

            if (!hasSlowDebuff) {
                purifyTriggered = true;
                break;
            }
        }

        // Then - 由于有20%概率，执行20次应该至少触发一次
        assertTrue("20%概率在20次测试中应该至少触发一次净化", purifyTriggered);

        printBattleLogs();
    }

    /**
     * 测试等级5：100%概率移除debuff
     */
    @Test
    public void testPurifyLevel5() {
        resetEntityStates();

        // Given
        PassiveSkill purify = createPassiveSkill("purify", 5);
        addPassiveSkillToEntity(testPlayer, purify);

        // 添加debuff
        SlowDebuff slowDebuff = new SlowDebuff(
            "test_slow", "测试减速", "速度降低",
            BuffType.DEBUFF, true, 3, 1, false, 1.0f, 20f
        );
        testPlayer.getActiveBuffList().add(slowDebuff);

        // When - 触发回合开始
        assertEquals("应该有1个debuff", 1, testPlayer.getActiveBuffList().size());
        triggerRoundStart();

        // Then - 等级5应该100%移除debuff
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        boolean hasSlowDebuff = buffs.stream()
            .anyMatch(buff -> buff instanceof SlowDebuff);
        assertFalse("等级5应该100%移除debuff", hasSlowDebuff);
        assertEquals("应该没有debuff了", 0, buffs.size());

        // 验证日志
        assertLogContains(LogType.BUFF, "【净化】");
        assertLogContains(LogType.BUFF, "移除了");

        printBattleLogs();
    }

    /**
     * 测试没有debuff时无效果
     */
    @Test
    public void testPurifyNoDebuff() {
        resetEntityStates();

        // Given
        PassiveSkill purify = createPassiveSkill("purify", 5);
        addPassiveSkillToEntity(testPlayer, purify);

        // 确保没有debuff
        testPlayer.getActiveBuffList().clear();
        assertEquals("应该没有debuff", 0, testPlayer.getActiveBuffList().size());

        // When
        int buffCountBefore = testPlayer.getActiveBuffList().size();
        triggerRoundStart();

        // Then - 不应该有任何问题
        int buffCountAfter = testPlayer.getActiveBuffList().size();
        assertEquals("没有debuff时应该正常工作", buffCountBefore, buffCountAfter);
    }

    /**
     * 测试只能移除一种debuff
     */
    @Test
    public void testPurifyRemovesOnlyOne() {
        resetEntityStates();

        // Given
        PassiveSkill purify = createPassiveSkill("purify", 5);
        addPassiveSkillToEntity(testPlayer, purify);

        // 添加两个不同的debuff
        SlowDebuff slowDebuff = new SlowDebuff(
            "test_slow", "测试减速", "速度降低",
            BuffType.DEBUFF, true, 3, 1, false, 1.0f, 20f
        );
        testPlayer.getActiveBuffList().add(slowDebuff);

        // 添加另一个debuff（这里用一个简单的属性buff模拟）
        BaseBuff debuff2 = new BaseBuff(
            "test_poison", "测试中毒", "中毒伤害",
            BuffType.DEBUFF, com.example.treasure_and_battle.model.common.TriggerType.PERMANENT,
            true, 3, 1, false, 5.0f
        ) {
            @Override
            public void applyAttributeBonus(com.example.treasure_and_battle.model.attribute.AttributeSet attributeSet) {
                // 不做任何事
            }

            @Override
            public void onTrigger(com.example.treasure_and_battle.model.entity.BattleEntity owner,
                                BattleContext context,
                                com.example.treasure_and_battle.model.common.TriggerType triggerType) {
                // 不做任何事
            }
        };
        testPlayer.getActiveBuffList().add(debuff2);

        // 确保有两个debuff
        assertEquals("应该有2个debuff", 2, testPlayer.getActiveBuffList().size());

        // When
        triggerRoundStart();

        // Then - 应该只移除一个debuff
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        assertEquals("应该剩余1个debuff", 1, buffs.size());

        printBattleLogs();
    }

    /**
     * 测试概率递增
     */
    @Test
    public void testPurifyChanceScaling() {
        resetEntityStates();

        // 添加debuff
        SlowDebuff slowDebuff = new SlowDebuff(
            "test_slow", "测试减速", "速度降低",
            BuffType.DEBUFF, true, 3, 1, false, 1.0f, 20f
        );
        testPlayer.getActiveBuffList().add(slowDebuff);

        // 测试低概率（等级1: 20%）
        resetEntityStates();
        testPlayer.getActiveBuffList().add(slowDebuff);
        PassiveSkill purify1 = createPassiveSkill("purify", 1);
        addPassiveSkillToEntity(testPlayer, purify1);

        int successCount1 = 0;
        for (int i = 0; i < 10; i++) {
            battleContext.battleLogs.clear();
            triggerRoundStart();
            if (testPlayer.getActiveBuffList().isEmpty()) {
                successCount1++;
            }
            // 重新添加debuff
            testPlayer.getActiveBuffList().add(slowDebuff);
        }
        System.out.println("等级1净化成功率: " + successCount1 + "/10");

        // 测试高概率（等级5: 100%）
        resetEntityStates();
        testPlayer.getActiveBuffList().add(slowDebuff);
        PassiveSkill purify5 = createPassiveSkill("purify", 5);
        addPassiveSkillToEntity(testPlayer, purify5);

        int successCount5 = 0;
        for (int i = 0; i < 10; i++) {
            battleContext.battleLogs.clear();
            triggerRoundStart();
            if (testPlayer.getActiveBuffList().isEmpty()) {
                successCount5++;
            }
            // 重新添加debuff
            testPlayer.getActiveBuffList().add(slowDebuff);
        }
        System.out.println("等级5净化成功率: " + successCount5 + "/10");

        // 验证等级5成功率更高
        assertTrue("等级5应该100%成功", successCount5 == 10);
    }
}
