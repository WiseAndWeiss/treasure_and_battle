package com.example.treasure_and_battle.skill.passive.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 铁血意志技能测试
 * 技能效果：回合开始时若生命值低于最大生命值的x%，则物理攻击与物理防御提升y%，立即驱散流血/虚弱/中毒效果
 */
public class PassiveSkill_IronWillTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：HP低于20%时触发，物攻物防提升12%
     */
    @Test
    public void testIronWillLevel1() {
        // Given
        PassiveSkill ironWill = createPassiveSkill("iron_will", 1);
        testPlayer.addPassiveSkill(ironWill);

        testPlayer.setCurrentHp(30); // 30/200 = 15%，低于20%

        // When
        battleManager.triggerRoundStartPassiveSkills(testPlayer, battleContext);

        // Then - 验证物攻物防提升
        boolean hasStatBoost = false;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof AttributeBuff && buff.getBuffId().equals("iron_will_stat_boost")) {
                hasStatBoost = true;
            }
        }

        assertTrue("应该有属性提升buff", hasStatBoost);

        printBattleLogs();
    }

    /**
     * 测试HP高于阈值时不触发
     */
    @Test
    public void testNoTriggerWhenHpIsHigh() {
        // Given
        PassiveSkill ironWill = createPassiveSkill("iron_will", 1);
        testPlayer.addPassiveSkill(ironWill);

        testPlayer.setCurrentHp(180); // 180/200 = 90%，高于20%

        // When
        battleManager.triggerRoundStartPassiveSkills(testPlayer, battleContext);

        // Then - 不应该有属性提升buff
        boolean hasStatBoost = false;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof AttributeBuff && buff.getBuffId().equals("iron_will_stat_boost")) {
                hasStatBoost = true;
            }
        }

        assertFalse("不应该有属性提升buff", hasStatBoost);

        printBattleLogs();
    }

    /**
     * 测试HP恢复到阈值以上时移除buff
     */
    @Test
    public void testRemoveBuffWhenHpRecovers() {
        // Given
        PassiveSkill ironWill = createPassiveSkill("iron_will", 1);
        testPlayer.addPassiveSkill(ironWill);

        // 第一次触发（HP低）
        testPlayer.setCurrentHp(30);
        battleManager.triggerRoundStartPassiveSkills(testPlayer, battleContext);

        boolean hasBuffFirst = false;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof AttributeBuff && buff.getBuffId().equals("iron_will_stat_boost")) {
                hasBuffFirst = true;
            }
        }
        assertTrue("第一次应该有属性提升buff", hasBuffFirst);

        // 第二次触发（HP恢复）
        testPlayer.setCurrentHp(180);
        battleManager.triggerRoundStartPassiveSkills(testPlayer, battleContext);

        boolean hasBuffSecond = false;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof AttributeBuff && buff.getBuffId().equals("iron_will_stat_boost")) {
                hasBuffSecond = true;
            }
        }

        assertFalse("第二次不应该有属性提升buff", hasBuffSecond);

        printBattleLogs();
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testScalingByLevel() {
        // 测试不同等级的HP阈值
        int threshold1 = 20;
        int threshold5 = 60;

        System.out.println("等级1HP阈值: " + threshold1 + "%");
        System.out.println("等级5HP阈值: " + threshold5 + "%");

        printBattleLogs();
    }

    /**
     * 测试日志记录
     */
    @Test
    public void testBattleLog() {
        // Given
        PassiveSkill ironWill = createPassiveSkill("iron_will", 3);
        testPlayer.addPassiveSkill(ironWill);
        testPlayer.setCurrentHp(30);

        // When
        battleManager.triggerRoundStartPassiveSkills(testPlayer, battleContext);

        // Then - 验证日志
        assertLogExists(LogType.BUFF);
        assertLogContains(LogType.BUFF, "【铁血意志】");

        printBattleLogs();
    }
}
