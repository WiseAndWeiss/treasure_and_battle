package com.example.treasure_and_battle.skill.passive.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.control.BlindnessDebuff;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.buff.impl.attribute.WeaknessDebuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 不屈之志技能测试
 * 技能效果：回合开始时，若自身持有减速/燃烧/中毒/流血/致盲/虚弱debuff，本回合提升x%物理攻击与y%暴击伤害
 */
public class PassiveSkill_IndomitableWillTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：有debuff时物攻提升8%，暴击伤害提升5%
     */
    @Test
    public void testIndomitableWillLevel1WithSlow() {
        // Given
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 1);

        // 添加减速debuff
        SlowDebuff slowDebuff = new SlowDebuff(
                "test_slow", "测试减速", "减速",
                BuffType.DEBUFF, false, -1, 1, false, 1.0f, 10.0f);

        testPlayer.getActiveBuffList().add(slowDebuff);
        testPlayer.markAttributeCacheDirty();

        // 设置基础攻击以便观察8%变化
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();

        int initialAttack = testPlayer.getFinalAttributes().physicalAtk;
        float initialCritDamage = testPlayer.getFinalAttributes().physicalCritDmg;

        // When - 回合开始触发
        indomitableWill.onRoundStart(testPlayer, battleContext);

        // Then - 验证攻击和暴击伤害提升
        int currentAttack = testPlayer.getFinalAttributes().physicalAtk;
        float currentCritDamage = testPlayer.getFinalAttributes().physicalCritDmg;

        assertEquals("物理攻击应该提升8% (100 -> 108)", 108, currentAttack);
        assertEquals("暴击伤害应该提升5%", 0.05f, currentCritDamage - initialCritDamage, 0.001f);

        // 验证日志
        assertLogContains(LogType.BUFF, "【不屈之志】");

        printBattleLogs();
    }

    /**
     * 测试等级3：有debuff时物攻提升12%，暴击伤害提升13%
     */
    @Test
    public void testIndomitableWillLevel3WithBurning() {
        // Given
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 3);

        // 添加燃烧debuff
        BurningDebuff burningDebuff = new BurningDebuff(
                "test_burning", "测试燃烧", "燃烧",
                BuffType.DEBUFF, false, -1, 5, false, 1.0f);

        testPlayer.getActiveBuffList().add(burningDebuff);
        testPlayer.markAttributeCacheDirty();

        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();
        int initialAttack = testPlayer.getFinalAttributes().physicalAtk;
        float initialCritDamage = testPlayer.getFinalAttributes().physicalCritDmg;

        // When - 回合开始触发
        indomitableWill.onRoundStart(testPlayer, battleContext);

        // Then - 验证攻击和暴击伤害提升
        int currentAttack = testPlayer.getFinalAttributes().physicalAtk;
        float currentCritDamage = testPlayer.getFinalAttributes().physicalCritDmg;

        assertEquals("物理攻击应该提升12% (100 -> 112)", 112, currentAttack);
        assertEquals("暴击伤害应该提升13%", 0.13f, currentCritDamage - initialCritDamage, 0.001f);

        printBattleLogs();
    }

    /**
     * 测试等级5：有debuff时物攻提升20%，暴击伤害提升24%
     */
    @Test
    public void testIndomitableWillLevel5WithPoison() {
        // Given
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 5);

        // 添加中毒debuff
        PoisoningDebuff poisonDebuff = new PoisoningDebuff(
                "test_poison", "测试中毒", "中毒",
                BuffType.DEBUFF, false, -1, 10, false, 1.0f);

        testPlayer.getActiveBuffList().add(poisonDebuff);
        testPlayer.markAttributeCacheDirty();

        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();
        int initialAttack = testPlayer.getFinalAttributes().physicalAtk;
        float initialCritDamage = testPlayer.getFinalAttributes().physicalCritDmg;

        // When - 回合开始触发
        indomitableWill.onRoundStart(testPlayer, battleContext);

        // Then - 验证攻击和暴击伤害提升
        int currentAttack = testPlayer.getFinalAttributes().physicalAtk;
        float currentCritDamage = testPlayer.getFinalAttributes().physicalCritDmg;

        assertEquals("物理攻击应该提升20% (100 -> 120)", 120, currentAttack);
        assertEquals("暴击伤害应该提升24%", 0.24f, currentCritDamage - initialCritDamage, 0.001f);

        printBattleLogs();
    }

    /**
     * 测试无debuff时不触发
     */
    @Test
    public void testIndomitableWillWithoutDebuff() {
        // Given
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 1);

        // 确保没有debuff
        testPlayer.getActiveBuffList().clear();
        testPlayer.markAttributeCacheDirty();

        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();
        int initialAttack = testPlayer.getFinalAttributes().physicalAtk;
        float initialCritDamage = testPlayer.getFinalAttributes().physicalCritDmg;

        // When - 回合开始触发
        indomitableWill.onRoundStart(testPlayer, battleContext);

        // Then - 验证没有提升
        int currentAttack = testPlayer.getFinalAttributes().physicalAtk;
        float currentCritDamage = testPlayer.getFinalAttributes().physicalCritDmg;

        assertEquals("无debuff时物理攻击不应该改变", initialAttack, currentAttack);
        assertEquals("无debuff时暴击伤害不应该改变", initialCritDamage, currentCritDamage, 0.001f);

        printBattleLogs();
    }

    /**
     * 测试所有支持的debuff类型
     */
    @Test
    public void testIndomitableWillWithAllDebuffTypes() {
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 1);

        // 测试每种debuff类型
        testBuffType(new SlowDebuff("test", "测试", "减速", BuffType.DEBUFF, false, -1, 1, false, 1.0f, 10.0f));
        testBuffType(new BurningDebuff("test", "测试", "燃烧", BuffType.DEBUFF, false, -1, 5, false, 1.0f));
        testBuffType(new PoisoningDebuff("test", "测试", "中毒", BuffType.DEBUFF, false, -1, 10, false, 1.0f));
        testBuffType(new BleedingDebuff("test", "测试", "流血", BuffType.DEBUFF, false, -1, 5, true, 1.0f));
        testBuffType(new BlindnessDebuff("test", "测试", "致盲", BuffType.DEBUFF, false, -1, 1, false, 1.0f, 10.0f));
        testBuffType(new WeaknessDebuff("test", "测试", "虚弱", BuffType.DEBUFF, false, -1, 1, false, 1.0f, 10));
    }

    private void testBuffType(com.example.treasure_and_battle.buff.BaseBuff buff) {
        // 清空并添加debuff
        testPlayer.getActiveBuffList().clear();
        testPlayer.getActiveBuffList().add(buff);
        testPlayer.markAttributeCacheDirty();

        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();
        int initialAttack = testPlayer.getFinalAttributes().physicalAtk;

        // 触发技能
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 1);
        indomitableWill.onRoundStart(testPlayer, battleContext);

        // 验证提升
        int currentAttack = testPlayer.getFinalAttributes().physicalAtk;
        assertTrue("有" + buff.getBuffName() + "时应该提升攻击", currentAttack > initialAttack);

        printBattleLogs();
    }

    /**
     * 测试多个debuff时也只触发一次
     */
    @Test
    public void testIndomitableWillWithMultipleDebuffs() {
        // Given
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 1);

        // 添加多个debuff
        testPlayer.getActiveBuffList().add(new SlowDebuff("slow1", "减速1", "减速", BuffType.DEBUFF, false, -1, 1, false, 1.0f, 10.0f));
        testPlayer.getActiveBuffList().add(new BurningDebuff("burn1", "燃烧1", "燃烧", BuffType.DEBUFF, false, -1, 5, false, 1.0f));
        testPlayer.getActiveBuffList().add(new BleedingDebuff("bleed1", "流血1", "流血", BuffType.DEBUFF, false, -1, 5, true, 1.0f));
        testPlayer.markAttributeCacheDirty();

        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();
        int initialAttack = testPlayer.getFinalAttributes().physicalAtk;

        // When - 回合开始触发
        indomitableWill.onRoundStart(testPlayer, battleContext);

        // Then - 验证只提升一次（不是叠加3次）
        int currentAttack = testPlayer.getFinalAttributes().physicalAtk;
        assertEquals("多个debuff时应该只提升一次8% (100 -> 108)", 108, currentAttack);

        printBattleLogs();
    }

    /**
     * 测试多回合累积效果
     */
    @Test
    public void testIndomitableWillStacking() {
        // Given
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 1);

        SlowDebuff slowDebuff = new SlowDebuff(
                "test_slow", "测试减速", "减速",
                BuffType.DEBUFF, false, -1, 1, false, 1.0f, 10.0f);

        testPlayer.getActiveBuffList().add(slowDebuff);
        testPlayer.markAttributeCacheDirty();

        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();
        int initialAttack = testPlayer.getFinalAttributes().physicalAtk;

        // When - 多回合触发
        indomitableWill.onRoundStart(testPlayer, battleContext);
        indomitableWill.onRoundStart(testPlayer, battleContext);
        indomitableWill.onRoundStart(testPlayer, battleContext);

        // Then - 验证每次触发都重新添加buff（不累积，因为maxStackCount=1）
        int currentAttack = testPlayer.getFinalAttributes().physicalAtk;
        assertEquals("maxStackCount=1，所以不累积，每次触发都是8% (100 -> 108)", 108, currentAttack);

        printBattleLogs();
    }

    /**
     * 测试日志内容
     */
    @Test
    public void testIndomitableWillLogContent() {
        // Given
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 1);

        SlowDebuff slowDebuff = new SlowDebuff(
                "test_slow", "测试减速", "减速",
                BuffType.DEBUFF, false, -1, 1, false, 1.0f, 10.0f);

        testPlayer.getActiveBuffList().add(slowDebuff);
        testPlayer.markAttributeCacheDirty();

        // When - 回合开始触发
        indomitableWill.onRoundStart(testPlayer, battleContext);

        // Then - 验证日志包含异常状态信息
        assertLogContains(LogType.BUFF, "身处异常状态");

        printBattleLogs();
    }

    /**
     * 测试对怪物生效
     */
    @Test
    public void testIndomitableWillOnMonster() {
        // Given
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 1);

        SlowDebuff slowDebuff = new SlowDebuff(
                "test_slow", "测试减速", "减速",
                BuffType.DEBUFF, false, -1, 1, false, 1.0f, 10.0f);

        testMonster.getActiveBuffList().add(slowDebuff);
        testMonster.markAttributeCacheDirty();

        testMonster.getBaseAttributes().physicalAtk = 50;
        testMonster.markAttributeCacheDirty();
        int initialAttack = testMonster.getFinalAttributes().physicalAtk;

        // When - 回合开始触发
        indomitableWill.onRoundStart(testMonster, battleContext);

        // Then - 验证怪物攻击也提升
        int currentAttack = testMonster.getFinalAttributes().physicalAtk;
        assertTrue("怪物物理攻击应该提升", currentAttack > initialAttack);

        printBattleLogs();
    }
}
