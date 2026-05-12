package com.example.treasure_and_battle.skill.passive.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 先机技能测试
 * 技能效果：每回合开始时，自身速度提升x%
 */
public class PassiveSkill_FirstMoverTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：速度提升2%
     */
    @Test
    public void testFirstMoverLevel1() {
        // Given
        PassiveSkill firstMover = createPassiveSkill("first_mover", 1);

        // 设置更高的基础速度以便观察2%变化
        testPlayer.getBaseAttributes().speed = 100;
        testPlayer.markAttributeCacheDirty();
        int initialSpeed = testPlayer.getFinalAttributes().speed;

        System.out.println("DEBUG: initialSpeed = " + initialSpeed);
        System.out.println("DEBUG: buff count before = " + testPlayer.getActiveBuffList().size());

        // When - 回合开始触发
        firstMover.onRoundStart(testPlayer, battleContext);

        System.out.println("DEBUG: buff count after = " + testPlayer.getActiveBuffList().size());
        System.out.println("DEBUG: buff list = " + testPlayer.getActiveBuffList());

        // Then - 验证速度提升
        int currentSpeed = testPlayer.getFinalAttributes().speed;
        System.out.println("DEBUG: currentSpeed = " + currentSpeed);
        System.out.println("DEBUG: difference = " + (currentSpeed - initialSpeed));

        assertTrue("速度应该提升", currentSpeed > initialSpeed);
        assertEquals("速度应该提升2% (100 -> 102)", 102, currentSpeed);

        // 验证日志
        assertLogContains(LogType.BUFF, "【先机】");

        printBattleLogs();
    }

    /**
     * 测试等级3：速度提升4%
     */
    @Test
    public void testFirstMoverLevel3() {
        // Given
        PassiveSkill firstMover = createPassiveSkill("first_mover", 3);

        testPlayer.getBaseAttributes().speed = 100;
        testPlayer.markAttributeCacheDirty();
        int initialSpeed = testPlayer.getFinalAttributes().speed;

        // When - 回合开始触发
        firstMover.onRoundStart(testPlayer, battleContext);

        // Then - 验证速度提升
        int currentSpeed = testPlayer.getFinalAttributes().speed;
        assertEquals("速度应该提升4% (100 -> 104)", 104, currentSpeed);

        printBattleLogs();
    }

    /**
     * 测试等级5：速度提升7%
     */
    @Test
    public void testFirstMoverLevel5() {
        // Given
        PassiveSkill firstMover = createPassiveSkill("first_mover", 5);

        testPlayer.getBaseAttributes().speed = 100;
        testPlayer.markAttributeCacheDirty();
        int initialSpeed = testPlayer.getFinalAttributes().speed;

        // When - 回合开始触发
        firstMover.onRoundStart(testPlayer, battleContext);

        // Then - 验证速度提升
        int currentSpeed = testPlayer.getFinalAttributes().speed;
        assertEquals("速度应该提升7% (100 -> 107)", 107, currentSpeed);

        printBattleLogs();
    }

    /**
     * 测试多回合累积效果
     */
    @Test
    public void testFirstMoverStacking() {
        // Given
        PassiveSkill firstMover = createPassiveSkill("first_mover", 1);

        testPlayer.getBaseAttributes().speed = 100;
        testPlayer.markAttributeCacheDirty();
        int initialSpeed = testPlayer.getFinalAttributes().speed;

        // When - 多回合触发
        firstMover.onRoundStart(testPlayer, battleContext);
        firstMover.onRoundStart(testPlayer, battleContext);
        firstMover.onRoundStart(testPlayer, battleContext);

        // Then - 验证速度累积提升
        int currentSpeed = testPlayer.getFinalAttributes().speed;
        assertEquals("速度应该累积提升3次2% (100 -> 106)", 106, currentSpeed);

        printBattleLogs();
    }

    /**
     * 测试对怪物生效
     */
    @Test
    public void testFirstMoverOnMonster() {
        // Given
        PassiveSkill firstMover = createPassiveSkill("first_mover", 1);

        testMonster.getBaseAttributes().speed = 50;
        testMonster.markAttributeCacheDirty();
        int initialSpeed = testMonster.getFinalAttributes().speed;

        // When - 回合开始触发
        firstMover.onRoundStart(testMonster, battleContext);

        // Then - 验证怪物速度也提升
        int currentSpeed = testMonster.getFinalAttributes().speed;
        assertTrue("怪物速度应该提升", currentSpeed > initialSpeed);

        printBattleLogs();
    }

    /**
     * 测试速度确实影响行动顺序（通过验证最终速度值）
     */
    @Test
    public void testFirstMoverAffectsFinalSpeed() {
        // Given
        PassiveSkill firstMover = createPassiveSkill("first_mover", 5);

        testPlayer.getBaseAttributes().speed = 100;
        testPlayer.markAttributeCacheDirty();
        int baseSpeed = testPlayer.getFinalAttributes().speed;

        // When - 回合开始触发
        firstMover.onRoundStart(testPlayer, battleContext);

        // Then - 验证最终速度值受百分比加成影响
        int finalSpeed = testPlayer.getFinalAttributes().speed;
        assertTrue("最终速度值应该改变", finalSpeed != baseSpeed);
        assertEquals("速度应该提升7% (100 -> 107)", 107, finalSpeed);

        printBattleLogs();
    }
}
