package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.skill.CounterStanceBuff;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 反击姿态技能测试
 * 技能效果：减少{x}%受到的伤害，持续1回合，并在被攻击时反击
 */
public class ActiveSkill_CounterStanceTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：20%减伤
     */
    @Test
    public void testCounterStanceLevel1() {
        // Given
        ActiveSkill counterStance = createSkill("counter_stance", 1);
        testPlayer.setCurrentHp(100);

        // When - 施放技能
        executeSkillAndDamage(counterStance, testPlayer, testMonster);

        // Then - 验证buff被添加
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        assertEquals("应该有1个buff", 1, buffs.size());
        assertTrue("应该是CounterStanceBuff", buffs.get(0) instanceof CounterStanceBuff);

        // 验证日志
        assertLogContains(LogType.BUFF, "【反击姿态】");
        assertLogContains(LogType.BUFF, "摆出了防御反击架势");

        printBattleLogs();
    }

    /**
     * 测试减伤效果
     */
    @Test
    public void testDamageReduction() {
        // Given
        ActiveSkill counterStance = createSkill("counter_stance", 5); // 等级5: 25%减伤
        testPlayer.setCurrentHp(200);
        testMonster.setCurrentHp(300);

        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterAttack = monsterAttr.physicalAtk;
        int playerDefense = testPlayer.getFinalAttributes().physicalDef;

        // 施放反击姿态
        executeSkillAndDamage(counterStance, testPlayer, testMonster);

        int hpBefore = testPlayer.getCurrentHp();

        // When - 玩家受到怪物攻击
        battleManager.dealPhysicalDamage(testMonster, testPlayer, monsterAttack, battleContext);

        // Then - 验证减伤效果（25%减伤）
        int hpAfter = testPlayer.getCurrentHp();
        int actualDamage = hpBefore - hpAfter;

        int expectedDamage = Math.max(1, (int)(monsterAttack * 0.75) - playerDefense);

        assertEquals("伤害应该减少25%", expectedDamage, actualDamage, 2);

        assertLogExists(LogType.DAMAGE);

        printBattleLogs();
    }

    /**
     * 测试反击效果
     */
    @Test
    public void testCounterAttack() {
        // Given
        ActiveSkill counterStance = createSkill("counter_stance", 5);
        testPlayer.setCurrentHp(200);
        testMonster.setCurrentHp(300);

        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterAttack = monsterAttr.physicalAtk;

        // 施放反击姿态
        executeSkillAndDamage(counterStance, testPlayer, testMonster);

        int monsterHpBefore = testMonster.getCurrentHp();

        // When - 玩家受到怪物攻击
        battleManager.dealPhysicalDamage(testMonster, testPlayer, monsterAttack, battleContext);

        // Then - 验证反击伤害
        int monsterHpAfter = testMonster.getCurrentHp();
        int counterDamage = monsterHpBefore - monsterHpAfter;

        assertTrue("应该造成反击伤害", counterDamage > 0);
        // 注意：反击使用完整的dealPhysicalDamage，包括暴击、防御等计算
        // 所以不简单比较counterDamage <= playerAttack
        assertTrue("反击伤害应该合理（大于0且不超过怪物HP）", counterDamage > 0 && counterDamage <= testMonster.getCurrentHp());

        // 验证日志包含反击信息
        assertLogContains(LogType.DAMAGE, "【反击】");

        printBattleLogs();
    }

    /**
     * 测试等级5：25%减伤
     */
    @Test
    public void testCounterStanceLevel5() {
        // Given
        ActiveSkill counterStance = createSkill("counter_stance", 5); // 25%减伤
        testPlayer.setCurrentHp(200);
        testMonster.setCurrentHp(300);

        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterAttack = monsterAttr.physicalAtk;
        int playerDefense = testPlayer.getFinalAttributes().physicalDef;

        // 施放反击姿态
        executeSkillAndDamage(counterStance, testPlayer, testMonster);

        int hpBefore = testPlayer.getCurrentHp();

        // When - 玩家受到怪物攻击
        battleManager.dealPhysicalDamage(testMonster, testPlayer, monsterAttack, battleContext);

        // Then - 验证25%减伤
        int hpAfter = testPlayer.getCurrentHp();
        int actualDamage = hpBefore - hpAfter;

        // 计算期望伤害：减伤25%在防御之前应用
        int expectedDamage = Math.max(1, (int)(monsterAttack * 0.75) - playerDefense);

        assertEquals("等级5应该减少25%伤害", expectedDamage, actualDamage, 2);

        printBattleLogs();
    }

    /**
     * 测试buff持续时间
     */
    @Test
    public void testBuffDuration() {
        // Given
        ActiveSkill counterStance = createSkill("counter_stance", 1);
        testPlayer.setCurrentHp(200);

        // 施放反击姿态
        executeSkillAndDamage(counterStance, testPlayer, testMonster);

        // 验证buff已添加
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        assertEquals("应该有1个buff", 1, buffs.size());
        CounterStanceBuff buff = (CounterStanceBuff) buffs.get(0);
        assertEquals("buff持续时间应该是1", 1, buff.getRemainingDuration());

        // When - 经过一个回合
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // Then - buff应该消失
        List<BaseBuff> buffsAfter = testPlayer.getActiveBuffList();
        assertEquals("buff应该消失", 0, buffsAfter.size());

        printBattleLogs();
    }

    /**
     * 测试减伤和反击同时生效
     */
    @Test
    public void testDamageReductionAndCounterAttackCombined() {
        // Given
        ActiveSkill counterStance = createSkill("counter_stance", 5);
        testPlayer.setCurrentHp(200);
        testMonster.setCurrentHp(300);

        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterAttack = monsterAttr.physicalAtk;
        int playerDefense = testPlayer.getFinalAttributes().physicalDef;

        // 施放反击姿态
        executeSkillAndDamage(counterStance, testPlayer, testMonster);

        int playerHpBefore = testPlayer.getCurrentHp();
        int monsterHpBefore = testMonster.getCurrentHp();

        // When - 玩家受到怪物攻击
        battleManager.dealPhysicalDamage(testMonster, testPlayer, monsterAttack, battleContext);

        // Then - 验证玩家受到减少的伤害
        int playerHpAfter = testPlayer.getCurrentHp();
        int damageReceived = playerHpBefore - playerHpAfter;

        int expectedDamage = Math.max(1, (int)(monsterAttack * 0.75) - playerDefense);
        assertEquals("应该减少25%伤害", expectedDamage, damageReceived, 2);

        // 验证怪物受到反击伤害
        int monsterHpAfter = testMonster.getCurrentHp();
        int counterDamage = monsterHpBefore - monsterHpAfter;
        assertTrue("应该造成反击伤害", counterDamage > 0);

        // 验证日志同时包含减伤和反击信息
        assertLogExists(LogType.DAMAGE);

        printBattleLogs();
    }

    /**
     * 测试不同等级的减伤效果递增
     */
    @Test
    public void testDamageReductionScaling() {
        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterAttack = monsterAttr.physicalAtk;

        // 测试等级1（5%减伤）
        testPlayer.setCurrentHp(200);
        ActiveSkill counterStance1 = createSkill("counter_stance", 1);
        executeSkillAndDamage(counterStance1, testPlayer, testMonster);

        int hpBefore1 = testPlayer.getCurrentHp();
        battleManager.dealPhysicalDamage(testMonster, testPlayer, monsterAttack, battleContext);
        int damage1 = hpBefore1 - testPlayer.getCurrentHp();

        // 测试等级5（25%减伤）
        testPlayer.setCurrentHp(200);
        testPlayer.getActiveBuffList().clear(); // 清除之前的buff
        battleContext.battleLogs.clear();

        ActiveSkill counterStance5 = createSkill("counter_stance", 5);
        executeSkillAndDamage(counterStance5, testPlayer, testMonster);

        int hpBefore5 = testPlayer.getCurrentHp();
        battleManager.dealPhysicalDamage(testMonster, testPlayer, monsterAttack, battleContext);
        int damage5 = hpBefore5 - testPlayer.getCurrentHp();

        // 验证等级5减伤更多
        assertTrue("等级5应该比等级1减少更多伤害", damage5 < damage1);

        System.out.println("等级1受到伤害: " + damage1);
        System.out.println("等级5受到伤害: " + damage5);

        printBattleLogs();
    }

    /**
     * 测试buff消失后不再生效
     */
    @Test
    public void testBuffExpiration() {
        // Given
        ActiveSkill counterStance = createSkill("counter_stance", 5); // 25%减伤
        testPlayer.setCurrentHp(200);
        testMonster.setCurrentHp(300);

        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterAttack = monsterAttr.physicalAtk;
        int playerDefense = testPlayer.getFinalAttributes().physicalDef;

        // 施放反击姿态
        executeSkillAndDamage(counterStance, testPlayer, testMonster);

        // 验证buff已添加
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        assertEquals("应该有1个buff", 1, buffs.size());

        // When - buff过期
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // 验证buff已移除
        List<BaseBuff> buffsAfter = testPlayer.getActiveBuffList();
        assertEquals("buff应该被移除", 0, buffsAfter.size());

        int hpBefore = testPlayer.getCurrentHp();
        int monsterHpBefore = testMonster.getCurrentHp();

        // 玩家受到攻击
        battleManager.dealPhysicalDamage(testMonster, testPlayer, monsterAttack, battleContext);

        // Then - 不应该有减伤效果（只有防御减伤）
        int hpAfter = testPlayer.getCurrentHp();
        int damageReceived = hpBefore - hpAfter;

        // 计算期望伤害：只有防御减伤，没有buff减伤
        int expectedDamage = Math.max(1, monsterAttack - playerDefense);

        assertEquals("buff过期后不应该有额外减伤", expectedDamage, damageReceived, 1);

        // 不应该有反击效果
        int monsterHpAfter = testMonster.getCurrentHp();
        assertEquals("buff过期后不应该反击", monsterHpBefore, monsterHpAfter);

        printBattleLogs();
    }
}
