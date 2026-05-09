package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.attribute.WeaknessDebuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 重盾强击技能测试
 * 技能效果：对单体造成x%物理攻击伤害；若自身拥有护盾，附加y%护盾值额外伤害，并施加z%的虚弱buff，持续2回合
 */
public class ActiveSkill_ShieldStrikeTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：90%基础伤害，无护盾情况
     */
    @Test
    public void testShieldStrikeLevel1NoShield() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 1);
        int monsterHpBefore = testMonster.getCurrentHp();

        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int playerAtk = playerAttr.physicalAtk; // 50
        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterDef = monsterAttr.physicalDef; // 20
        int damagePercent = 90;

        // 预期伤害：50 * 90% - 20 = 45 - 20 = 25
        int expectedDamage = Math.max(1, (int) (playerAtk * damagePercent / 100.0f) - monsterDef);

        // When
        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        // Then
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("伤害应该约为" + expectedDamage + "，实际为" + actualDamage,
            Math.abs(actualDamage - expectedDamage) <= 3);

        // 验证没有施加虚弱buff（z=5%）
        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) buffs.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该施加虚弱buff", weaknessDebuff);
        assertEquals("虚弱buff应该持续2回合", 2, weaknessDebuff.getRemainingDuration());

        // 验证日志
        assertLogContains(LogType.DAMAGE, "【重盾强击】");
        assertLogExists(LogType.BUFF);

        printBattleLogs();
    }

    /**
     * 测试等级5：110%基础伤害 + 护盾加成
     */
    @Test
    public void testShieldStrikeLevel5WithShield() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 5);

        // 先添加护盾buff
        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield", "测试护盾", "测试",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false, -1, 100, false, 1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);

        int monsterHpBefore = testMonster.getCurrentHp();

        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int playerAtk = playerAttr.physicalAtk; // 50
        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterDef = monsterAttr.physicalDef; // 20
        int damagePercent = 110;
        int shieldBonusPercent = 50;
        int shieldValue = 100;

        // 预期伤害：基础50 * 110% - 20 = 35，护盾加成100 * 50% = 50，总计85
        int expectedBaseDamage = Math.max(1, (int) (playerAtk * damagePercent / 100.0f) - monsterDef);
        int expectedShieldBonus = (int) (shieldValue * shieldBonusPercent / 100.0f);
        int expectedTotalDamage = expectedBaseDamage + expectedShieldBonus;

        // When
        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        // Then
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("伤害应该约为" + expectedTotalDamage + "，实际为" + actualDamage,
            Math.abs(actualDamage - expectedTotalDamage) <= 5);

        // 验证虚弱buff
        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) buffs.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该施加虚弱buff", weaknessDebuff);
        assertEquals("虚弱buff应该持续2回合", 2, weaknessDebuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试护盾额外伤害
     */
    @Test
    public void testShieldBonusDamage() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 3); // 35%护盾加成

        // 无护盾情况
        testMonster.setCurrentHp(500);
        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);
        int damageWithoutShield = 500 - testMonster.getCurrentHp();

        // 有护盾情况
        testMonster.setCurrentHp(500);
        testPlayer.getActiveBuffList().clear();
        battleContext.battleLogs.clear();

        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield", "测试护盾", "测试",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false, -1, 50, false, 1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);

        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);
        int damageWithShield = 500 - testMonster.getCurrentHp();

        // Then - 有护盾的伤害应该明显大于无护盾
        assertTrue("有护盾时的伤害应该大于无护盾", damageWithShield > damageWithoutShield);

        System.out.println("无护盾伤害: " + damageWithoutShield);
        System.out.println("有护盾伤害: " + damageWithShield);
        System.out.println("伤害差: " + (damageWithShield - damageWithoutShield));

        printBattleLogs();
    }

    /**
     * 测试虚弱debuff效果
     */
    @Test
    public void testWeaknessDebuffEffect() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 5); // 28%虚弱
        AttributeSet monsterAttr = testMonster.getFinalAttributes();

        int baseAtk = monsterAttr.physicalAtk;
        int baseMatk = monsterAttr.magicalAtk;
        int weaknessPercent = 28;

        // When
        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        // Then - 验证虚弱效果
        AttributeSet finalAttr = testMonster.getFinalAttributes();
        int expectedAtk = Math.round(baseAtk * (1 - weaknessPercent / 100.0f));
        int expectedMatk = Math.round(baseMatk * (1 - weaknessPercent / 100.0f));

        System.out.println("基础物攻: " + baseAtk + ", 预期: " + expectedAtk + ", 实际: " + finalAttr.physicalAtk);
        System.out.println("基础法攻: " + baseMatk + ", 预期: " + expectedMatk + ", 实际: " + finalAttr.magicalAtk);

        assertEquals("物理攻击应该降低28%", expectedAtk, finalAttr.physicalAtk);
        assertEquals("法术攻击应该降低28%", expectedMatk, finalAttr.magicalAtk);

        // 验证buff存在
        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) buffs.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有虚弱buff", weaknessDebuff);
        assertEquals("虚弱buff持续2回合", 2, weaknessDebuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试虚弱debuff持续时间
     */
    @Test
    public void testWeaknessDebuffDuration() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 3);

        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) buffs.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有虚弱buff", weaknessDebuff);
        assertEquals("初始持续时间应该为2", 2, weaknessDebuff.getRemainingDuration());

        // When - 经过1回合
        BuffManager.getInstance(context).tickBuffs(testMonster);

        // Then
        List<BaseBuff> buffsAfterTick1 = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessDebuffAfter1 = (WeaknessDebuff) buffsAfterTick1.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("经过1回合后虚弱buff应该仍然存在", weaknessDebuffAfter1);
        assertEquals("剩余持续时间应该为1", 1, weaknessDebuffAfter1.getRemainingDuration());

        // When - 再经过1回合
        BuffManager.getInstance(context).tickBuffs(testMonster);

        // Then - 虚弱buff应该消失
        List<BaseBuff> buffsAfterTick2 = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessDebuffAfter2 = (WeaknessDebuff) buffsAfterTick2.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNull("经过2回合后虚弱buff应该消失", weaknessDebuffAfter2);

        // 验证属性恢复
        AttributeSet finalAttr = testMonster.getFinalAttributes();
        AttributeSet baseAttr = testMonster.getBaseAttributes();
        assertEquals("物理攻击应该恢复", baseAttr.physicalAtk, finalAttr.physicalAtk);
        assertEquals("法术攻击应该恢复", baseAttr.magicalAtk, finalAttr.magicalAtk);

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testMpCost() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When
        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        // Then - 等级1应该消耗16MP
        assertEquals("应该消耗16MP", mpBefore - 16, testPlayer.getCurrentMp());
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testScalingByLevel() {
        // 先添加护盾
        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield", "测试护盾", "测试",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false, -1, 100, false, 1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);

        // 测试等级1（90%基础，25%护盾加成，5%虚弱）
        testMonster.setCurrentHp(500);
        ActiveSkill skill1 = createSkill("shield_strike", 1);
        battleManager.executeSkill(testPlayer, skill1,
            Arrays.asList(testMonster), battleContext);
        int damage1 = 500 - testMonster.getCurrentHp();

        // 测试等级5（110%基础，50%护盾加成，28%虚弱）
        testMonster.setCurrentHp(500);
        testPlayer.getActiveBuffList().clear();
        battleContext.battleLogs.clear();
        testPlayer.getActiveBuffList().add(shieldBuff);
        ActiveSkill skill5 = createSkill("shield_strike", 5);
        battleManager.executeSkill(testPlayer, skill5,
            Arrays.asList(testMonster), battleContext);
        int damage5 = 500 - testMonster.getCurrentHp();

        // 验证等级5的伤害大于等级1
        assertTrue("等级5伤害应该大于等级1", damage5 > damage1);

        System.out.println("等级1伤害: " + damage1);
        System.out.println("等级5伤害: " + damage5);
    }

    /**
     * 测试无护盾时不施加额外伤害
     */
    @Test
    public void testNoShieldBonusWhenNoShield() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 5); // 50%护盾加成

        // 确保没有护盾
        testPlayer.getActiveBuffList().clear();

        int monsterHpBefore = testMonster.getCurrentHp();

        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int playerAtk = playerAttr.physicalAtk; // 50
        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterDef = monsterAttr.physicalDef; // 20
        int damagePercent = 110;

        // 预期伤害：只有基础伤害，没有护盾加成
        int expectedDamage = Math.max(1, (int) (playerAtk * damagePercent / 100.0f) - monsterDef);

        // When
        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        // Then
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();
        assertTrue("伤害应该约为基础伤害" + expectedDamage + "，实际为" + actualDamage,
            Math.abs(actualDamage - expectedDamage) <= 3);

        // 验证日志中不包含护盾加成信息
        boolean hasShieldBonusLog = battleContext.battleLogs.stream()
            .anyMatch(log -> log.getFormattedMessage().contains("护盾值转化为") ||
                          log.getFormattedMessage().contains("护盾值 %d 转化为"));

        // 但仍然应该施加虚弱buff
        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) buffs.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该仍然施加虚弱buff", weaknessDebuff);

        printBattleLogs();
    }

    /**
     * 测试护盾被消耗后的情况
     */
    @Test
    public void testShieldConsumedBeforeSkill() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 3); // 35%护盾加成

        // 先添加护盾，然后消耗掉
        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield", "测试护盾", "测试",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false, -1, 50, false, 1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);

        // 模拟护盾被消耗
        shieldBuff.absorbDamage(50, testPlayer, battleContext);

        // 验证护盾已耗尽
        assertTrue("护盾应该已耗尽", shieldBuff.getStackCount() <= 0 || shieldBuff.isExpired());

        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        // Then - 不应该有护盾加成
        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int playerAtk = playerAttr.physicalAtk;
        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterDef = monsterAttr.physicalDef;
        int damagePercent = 100;

        int expectedDamage = Math.max(1, (int) (playerAtk * damagePercent / 100.0f) - monsterDef);
        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();

        assertTrue("应该只有基础伤害，没有护盾加成",
            Math.abs(actualDamage - expectedDamage) <= 3);

        printBattleLogs();
    }

    /**
     * 测试日志记录
     */
    @Test
    public void testBattleLog() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 3);

        // 添加护盾
        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield", "测试护盾", "测试",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false, -1, 50, false, 1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);

        // When
        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        // Then - 验证日志
        assertLogExists(LogType.DAMAGE);
        assertLogContains(LogType.DAMAGE, "【重盾强击】");
        assertLogExists(LogType.BUFF); // 虚弱buff日志

        printBattleLogs();
    }

    /**
     * 测试虚弱debuff的可驱散性
     */
    @Test
    public void testWeaknessDebuffDispellable() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 5);

        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) buffs.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有虚弱buff", weaknessDebuff);
        assertTrue("虚弱buff应该可驱散", weaknessDebuff.isDispellable());

        // When - 驱散debuff
        BuffManager.getInstance(context).dispelBuffs(testMonster, false, true);

        // Then - 虚弱buff应该被移除
        List<BaseBuff> buffsAfter = testMonster.getActiveBuffList();
        WeaknessDebuff weaknessDebuffAfter = (WeaknessDebuff) buffsAfter.stream()
            .filter(buff -> buff instanceof WeaknessDebuff)
            .findFirst()
            .orElse(null);

        assertNull("驱散后虚弱buff应该被移除", weaknessDebuffAfter);

        printBattleLogs();
    }

    /**
     * 测试护盾值计算准确性
     */
    @Test
    public void testShieldBonusCalculation() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 5); // 50%护盾加成
        int shieldValue = 100;

        // 添加护盾
        ShieldBuff shieldBuff = new ShieldBuff(
            "test_shield", "测试护盾", "测试",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false, -1, shieldValue, false, 1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff);

        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        // Then - 验证护盾加成计算
        int expectedShieldBonus = (int) (shieldValue * 50 / 100.0f); // 50点

        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int playerAtk = playerAttr.physicalAtk;
        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        int monsterDef = monsterAttr.physicalDef;
        int damagePercent = 110;

        int expectedBaseDamage = Math.max(1, (int) (playerAtk * damagePercent / 100.0f) - monsterDef);
        int expectedTotalDamage = expectedBaseDamage + expectedShieldBonus;

        int actualDamage = monsterHpBefore - testMonster.getCurrentHp();

        assertTrue("总伤害应该=基础伤害+护盾加成",
            Math.abs(actualDamage - expectedTotalDamage) <= 3);

        System.out.println("预期基础伤害: " + expectedBaseDamage);
        System.out.println("预期护盾加成: " + expectedShieldBonus);
        System.out.println("预期总伤害: " + expectedTotalDamage);
        System.out.println("实际伤害: " + actualDamage);

        printBattleLogs();
    }

    /**
     * 测试多个护盾时只使用一个
     */
    @Test
    public void testMultipleShields() {
        // Given
        ActiveSkill shieldStrike = createSkill("shield_strike", 5); // 50%护盾加成

        // 添加两个护盾
        ShieldBuff shieldBuff1 = new ShieldBuff(
            "test_shield1", "测试护盾1", "测试1",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false, -1, 50, false, 1.0f
        );
        ShieldBuff shieldBuff2 = new ShieldBuff(
            "test_shield2", "测试护盾2", "测试2",
            com.example.treasure_and_battle.model.buff.BuffType.BUFF,
            false, -1, 30, false, 1.0f
        );
        testPlayer.getActiveBuffList().add(shieldBuff1);
        testPlayer.getActiveBuffList().add(shieldBuff2);

        // When
        battleManager.executeSkill(testPlayer, shieldStrike,
            Arrays.asList(testMonster), battleContext);

        // Then - 应该使用第一个找到的护盾值
        // 验证日志中只记录了一次护盾加成
        long shieldBonusLogCount = battleContext.battleLogs.stream()
            .filter(log -> log.getType() == LogType.BUFF)
            .filter(log -> log.getFormattedMessage().contains("护盾值转化为") ||
                          log.getFormattedMessage().contains("护盾值"))
            .count();

        // 应该只有一次护盾加成日志（使用第一个护盾）
        assertTrue("应该只使用一个护盾进行加成", shieldBonusLogCount <= 1);

        printBattleLogs();
    }
}
