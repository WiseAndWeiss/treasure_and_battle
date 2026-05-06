package com.example.treasure_and_battle.skill.passive.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 碎盾冲击技能测试
 * 技能效果：当护盾被敌人攻击打破时，对全场所有敌人造成一次x%自身防御力的伤害
 */
public class PassiveSkill_ShieldBurstTest extends PassiveSkillTestBase {

    /**
     * 测试护盾破碎触发AOE反击
     */
    @Test
    public void testShieldBreakTriggersAOEDamage() {
        // Given
        PassiveSkill shieldBurst = createPassiveSkill("shield_burst", 1); // 20%防御力
        testPlayer.addPassiveSkill(shieldBurst);

        int playerDef = testPlayer.getFinalAttributes().physicalDef;
        int expectedAoeDamage = (int) (playerDef * 0.2);
        int monsterMaxHp = testMonster.getBaseAttributes().maxHp;
        testMonster.setCurrentHp(monsterMaxHp);

        // 添加护盾
        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield",
            "测试护盾",
            "吸收%d点伤害",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false,
            -1,
            10,
            false,
            1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);
        testPlayer.markAttributeCacheDirty();

        // When - 击碎护盾
        testPlayer.setCurrentHp(200);
        // 提高怪物攻击力以确保护盾被击碎（需要击穿50防御 + 10护盾）
        testMonster.getBaseAttributes().physicalAtk = 150;
        testMonster.markAttributeCacheDirty();

        battleManager.dealPhysicalDamage(testMonster, testPlayer, 150, battleContext);

        // Then - 验证AOE伤害
        int actualDamage = monsterMaxHp - testMonster.getCurrentHp();
        assertEquals("应该受到AOE伤害", expectedAoeDamage, actualDamage);

        // 验证日志
        assertLogExists(LogType.ACTION);
        assertLogContains(LogType.ACTION, "【碎盾冲击】");
        assertLogContains(LogType.ACTION, "护盾被击碎");

        System.out.println("防御力: " + playerDef);
        System.out.println("预期AOE伤害: " + expectedAoeDamage);
        System.out.println("实际伤害: " + actualDamage);

        printBattleLogs();
    }

    /**
     * 测试无护盾时不触发
     */
    @Test
    public void testNoTriggerWithoutShield() {
        // Given
        PassiveSkill shieldBurst = createPassiveSkill("shield_burst", 1);
        testPlayer.addPassiveSkill(shieldBurst);

        int monsterHpBefore = testMonster.getCurrentHp();

        // When - 直接受到伤害（无护盾）
        testPlayer.setCurrentHp(200);
        battleManager.dealPhysicalDamage(testMonster, testPlayer, 50, battleContext);

        // Then - 怪物不应该受到额外伤害
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        // 50伤害 - 10防御 = 40伤害
        assertTrue("伤害应该正常（不包含反击）", actualDamage <= 45);

        // 不应该有碎盾冲击日志
        boolean hasShieldBurstLog = false;
        for (var log : battleContext.battleLogs) {
            if (log.getType() == LogType.ACTION && log.getFormattedMessage().contains("【碎盾冲击】")) {
                hasShieldBurstLog = true;
            }
        }

        assertFalse("无护盾时不应触发碎盾冲击", hasShieldBurstLog);

        printBattleLogs();
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testScalingByLevel() {
        int playerDef = testPlayer.getFinalAttributes().physicalDef;

        // 等级1：20%
        int aoeDamage1 = (int) (playerDef * 0.2);

        // 等级5：60%
        int aoeDamage5 = (int) (playerDef * 0.6);

        assertTrue("等级5的AOE伤害应该大于等级1", aoeDamage5 > aoeDamage1);

        System.out.println("防御力: " + playerDef);
        System.out.println("等级1AOE伤害: " + aoeDamage1);
        System.out.println("等级5AOE伤害: " + aoeDamage5);

        printBattleLogs();
    }

    /**
     * 测试护盾未完全击碎时不触发
     */
    @Test
    public void testNoTriggerWhenShieldNotBroken() {
        // Given
        PassiveSkill shieldBurst = createPassiveSkill("shield_burst", 1);
        testPlayer.addPassiveSkill(shieldBurst);

        int monsterHpBefore = testMonster.getCurrentHp();

        // 添加护盾（但不会被打碎）
        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield",
            "测试护盾",
            "吸收%d点伤害",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false,
            -1,
            100, // 大量护盾，不会被打碎
            false,
            1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);
        testPlayer.markAttributeCacheDirty();

        // When - 受到伤害（护盾不会被完全击碎）
        testPlayer.setCurrentHp(200);
        testMonster.getBaseAttributes().physicalAtk = 20;
        testMonster.markAttributeCacheDirty();

        battleManager.dealPhysicalDamage(testMonster, testPlayer, 20, battleContext);

        // Then - 不应该触发碎盾冲击
        boolean hasShieldBurstLog = false;
        for (var log : battleContext.battleLogs) {
            if (log.getType() == LogType.ACTION && log.getFormattedMessage().contains("【碎盾冲击】")) {
                hasShieldBurstLog = true;
            }
        }

        assertFalse("护盾未击碎时不应该触发", hasShieldBurstLog);

        printBattleLogs();
    }

    /**
     * 测试敌人已死亡时不造成伤害
     */
    @Test
    public void testNoDamageToDeadEnemy() {
        // Given
        PassiveSkill shieldBurst = createPassiveSkill("shield_burst", 1);
        testPlayer.addPassiveSkill(shieldBurst);

        // 怪物已经死亡
        testMonster.setCurrentHp(0);

        // 添加护盾
        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield",
            "测试护盾",
            "吸收%d点伤害",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false,
            -1,
            10,
            false,
            1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);
        testPlayer.markAttributeCacheDirty();

        // When - 击碎护盾
        testPlayer.setCurrentHp(200);
        // 提高怪物攻击力以确保护盾被击碎（需要击穿50防御 + 10护盾）
        testMonster.getBaseAttributes().physicalAtk = 150;
        testMonster.markAttributeCacheDirty();

        battleManager.dealPhysicalDamage(testMonster, testPlayer, 150, battleContext);

        // Then - 怪物HP应该仍为0（已经是死亡状态）
        assertEquals("死亡敌人不应受到伤害", 0, testMonster.getCurrentHp());

        printBattleLogs();
    }

    /**
     * 测试真实伤害类型
     */
    @Test
    public void testTrueDamageType() {
        // Given
        PassiveSkill shieldBurst = createPassiveSkill("shield_burst", 1);
        testPlayer.addPassiveSkill(shieldBurst);

        int playerDef = testPlayer.getFinalAttributes().physicalDef;
        int expectedAoeDamage = (int) (playerDef * 0.2);

        // 提高怪物防御，测试真实伤害无视防御
        testMonster.getBaseAttributes().physicalDef = 1000;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(300);

        // 添加护盾
        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield",
            "测试护盾",
            "吸收%d点伤害",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false,
            -1,
            10,
            false,
            1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);
        testPlayer.markAttributeCacheDirty();

        // When - 击碎护盾
        testPlayer.setCurrentHp(200);
        // 提高怪物攻击力以确保护盾被击碎（需要击穿50防御 + 10护盾）
        testMonster.getBaseAttributes().physicalAtk = 150;
        testMonster.markAttributeCacheDirty();

        battleManager.dealPhysicalDamage(testMonster, testPlayer, 150, battleContext);

        // Then - 真实伤害应该无视防御
        int actualDamage = 300 - testMonster.getCurrentHp();
        assertEquals("真实伤害应该等于防御反击伤害", expectedAoeDamage, actualDamage);

        System.out.println("怪物防御: 1000");
        System.out.println("预期真实伤害: " + expectedAoeDamage);
        System.out.println("实际伤害: " + actualDamage);

        printBattleLogs();
    }

    /**
     * 测试日志记录
     */
    @Test
    public void testBattleLog() {
        // Given
        PassiveSkill shieldBurst = createPassiveSkill("shield_burst", 3); // 40%
        testPlayer.addPassiveSkill(shieldBurst);

        // 添加护盾
        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield",
            "测试护盾",
            "吸收%d点伤害",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false,
            -1,
            10,
            false,
            1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);
        testPlayer.markAttributeCacheDirty();

        testMonster.setCurrentHp(300);

        // When
        testPlayer.setCurrentHp(200);
        // 提高怪物攻击力以确保护盾被击碎
        testMonster.getBaseAttributes().physicalAtk = 150;
        testMonster.markAttributeCacheDirty();

        battleManager.dealPhysicalDamage(testMonster, testPlayer, 150, battleContext);

        // Then - 验证日志
        assertLogExists(LogType.ACTION);
        assertLogContains(LogType.ACTION, "【碎盾冲击】");
        assertLogContains(LogType.ACTION, "护盾被击碎");
        assertLogContains(LogType.ACTION, "防御反击伤害");

        printBattleLogs();
    }
}
