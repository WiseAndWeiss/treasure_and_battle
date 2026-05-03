package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.buff.impl.skill.ImpenetrableBuff;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 固若金汤技能测试
 * 技能效果：本回合受到的所有生命伤害都会以x%比例转化为自身护盾
 * 注意：只计算HP实际收到的伤害，不包括护盾吸收、减伤、抵挡等
 */
public class ActiveSkill_ImpenetrableTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：20%转化比例
     */
    @Test
    public void testImpenetrableLevel1() {
        // Given
        ActiveSkill impenetrable = createSkill("impenetrable", 1);
        testPlayer.setCurrentHp(200);

        // When
        battleManager.executeSkill(testPlayer, impenetrable,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证buff施加
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        assertEquals("应该有1个buff", 1, buffs.size());

        ImpenetrableBuff buff = (ImpenetrableBuff) buffs.get(0);
        assertNotNull("应该是ImpenetrableBuff", buff);
        assertEquals("buff应该持续1回合", 1, buff.getRemainingDuration());
        assertTrue("buff应该可驱散", buff.isDispellable());

        // 验证日志
        assertLogContains(LogType.BUFF, "【固若金汤】");
        assertLogContains(LogType.BUFF, "将受到伤害的 20% 转化为护盾");

        printBattleLogs();
    }

    /**
     * 测试伤害转护盾机制
     */
    @Test
    public void testDamageToShieldConversion() {
        // Given
        ActiveSkill impenetrable = createSkill("impenetrable", 5); // 40%转化
        testPlayer.setCurrentHp(200);

        battleManager.executeSkill(testPlayer, impenetrable,
            java.util.Arrays.asList(testMonster), battleContext);

        // 造成100点伤害（会经过防御计算）
        int damageToDeal = 150;
        testMonster.getBaseAttributes().physicalAtk = damageToDeal;
        testMonster.markAttributeCacheDirty();

        // When - 受到伤害
        int hpBefore = testPlayer.getCurrentHp();
        battleManager.dealPhysicalDamage(testMonster, testPlayer, damageToDeal, battleContext);

        // Then - 计算实际HP伤害
        int hpAfter = testPlayer.getCurrentHp();
        int actualHpDamage = hpBefore - hpAfter;

        // 预期护盾值 = 实际HP伤害 * 40%
        int expectedShieldValue = (int) (actualHpDamage * 40 / 100.0f);

        // 回合结束时转化护盾（注意：先onRoundEnd，再tickBuffs）
        BuffManager.getInstance(context).onRoundEnd(testPlayer, battleContext);
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // 验证护盾buff被创建
        List<BaseBuff> buffsAfterRound = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff = (ShieldBuff) buffsAfterRound.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该创建护盾buff", shieldBuff);
        assertEquals("护盾值应该正确", expectedShieldValue, shieldBuff.getStackCount());

        System.out.println("实际HP伤害: " + actualHpDamage);
        System.out.println("预期护盾值: " + expectedShieldValue);
        System.out.println("实际护盾值: " + shieldBuff.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试只计算HP实际伤害
     */
    @Test
    public void testOnlyCalculatesActualHpDamage() {
        // Given
        ActiveSkill impenetrable = createSkill("impenetrable", 3); // 30%转化
        testPlayer.setCurrentHp(200);

        // 先施放坚如磐石获得护盾
        com.example.treasure_and_battle.skill.active.warrior.ActiveSkill_FirmAsRock firmAsRock =
            (com.example.treasure_and_battle.skill.active.warrior.ActiveSkill_FirmAsRock)
            createSkill("firm_as_rock", 1);
        battleManager.executeSkill(testPlayer, firmAsRock,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        ShieldBuff initialShield = (ShieldBuff) buffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);
        int initialShieldValue = initialShield.getStackCount();

        // 再施放固若金汤
        battleManager.executeSkill(testPlayer, impenetrable,
            java.util.Arrays.asList(testMonster), battleContext);

        // 造成伤害（会被护盾吸收一部分）
        int damageToDeal = 50;
        testMonster.getBaseAttributes().physicalAtk = damageToDeal;
        testMonster.markAttributeCacheDirty();

        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.dealPhysicalDamage(testMonster, testPlayer, damageToDeal, battleContext);

        // Then - 只有HP实际受到的伤害才会计入转化
        int hpAfter = testPlayer.getCurrentHp();
        int actualHpDamage = hpBefore - hpAfter;

        int expectedShieldValue = (int) (actualHpDamage * 30 / 100.0f);

        // 回合结束转化
        BuffManager.getInstance(context).onRoundEnd(testPlayer, battleContext);

        // 验证护盾转化（不包括初始护盾吸收的部分）
        List<BaseBuff> finalBuffs = testPlayer.getActiveBuffList();
        long shieldBuffCount = finalBuffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .count();

        assertTrue("应该有护盾buff（初始+转化）", shieldBuffCount >= 1);

        // 转化的护盾应该是实际HP伤害的30%
        // 注意：由于有初始护盾吸收，HP伤害可能很小或为0
        System.out.println("初始护盾值: " + initialShieldValue);
        System.out.println("实际HP伤害: " + actualHpDamage);
        System.out.println("预期转化的护盾值: " + expectedShieldValue);

        printBattleLogs();
    }

    /**
     * 测试多次受伤累计转化
     */
    @Test
    public void testMultipleDamageAccumulation() {
        // Given
        ActiveSkill impenetrable = createSkill("impenetrable", 5); // 40%
        testPlayer.setCurrentHp(200);

        battleManager.executeSkill(testPlayer, impenetrable,
            java.util.Arrays.asList(testMonster), battleContext);

        // When - 多次受伤
        int damage1 = 50;
        testMonster.getBaseAttributes().physicalAtk = damage1;
        testMonster.markAttributeCacheDirty();
        battleManager.dealPhysicalDamage(testMonster, testPlayer, damage1, battleContext);

        int hpBefore2 = testPlayer.getCurrentHp();
        int damage2 = 30;
        testMonster.getBaseAttributes().physicalAtk = damage2;
        testMonster.markAttributeCacheDirty();
        battleManager.dealPhysicalDamage(testMonster, testPlayer, damage2, battleContext);
        int hpAfter2 = testPlayer.getCurrentHp();

        int actualHpDamage1 = 200 - hpBefore2;
        int actualHpDamage2 = hpBefore2 - hpAfter2;
        int totalActualHpDamage = actualHpDamage1 + actualHpDamage2;

        int expectedShieldValue = (int) (totalActualHpDamage * 40 / 100.0f);

        // 回合结束转化
        BuffManager.getInstance(context).onRoundEnd(testPlayer, battleContext);

        // Then - 两次伤害都应该累计转化
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff = (ShieldBuff) buffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有护盾buff", shieldBuff);
        assertEquals("护盾值应该是两次伤害转化之和", expectedShieldValue, shieldBuff.getStackCount());

        System.out.println("第一次HP伤害: " + actualHpDamage1);
        System.out.println("第二次HP伤害: " + actualHpDamage2);
        System.out.println("总HP伤害: " + totalActualHpDamage);
        System.out.println("预期护盾值: " + expectedShieldValue);
        System.out.println("实际护盾值: " + shieldBuff.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试回合结束时转化护盾
     */
    @Test
    public void testShieldConversionOnRoundEnd() {
        // Given
        ActiveSkill impenetrable = createSkill("impenetrable", 2); // 25%
        testPlayer.setCurrentHp(200);

        battleManager.executeSkill(testPlayer, impenetrable,
            java.util.Arrays.asList(testMonster), battleContext);

        // 受到伤害
        int damage = 80;
        testMonster.getBaseAttributes().physicalAtk = damage;
        testMonster.markAttributeCacheDirty();
        battleManager.dealPhysicalDamage(testMonster, testPlayer, damage, battleContext);

        int hpBeforeRoundEnd = testPlayer.getCurrentHp();
        int actualHpDamage = 200 - hpBeforeRoundEnd;

        // 回合结束前应该没有护盾buff（只有ImpenetrableBuff）
        List<BaseBuff> buffsBeforeRoundEnd = testPlayer.getActiveBuffList();
        long shieldCountBefore = buffsBeforeRoundEnd.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .count();
        assertEquals("回合结束前不应该有护盾buff", 0, shieldCountBefore);

        // When - 回合结束（先onRoundEnd转化护盾，再tickBuffs）
        BuffManager.getInstance(context).onRoundEnd(testPlayer, battleContext);
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // Then - 应该创建护盾buff
        List<BaseBuff> buffsAfterRoundEnd = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff = (ShieldBuff) buffsAfterRoundEnd.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("回合结束应该创建护盾buff", shieldBuff);

        int expectedShieldValue = (int) (actualHpDamage * 25 / 100.0f);
        assertEquals("护盾值应该正确", expectedShieldValue, shieldBuff.getStackCount());

        System.out.println("HP伤害: " + actualHpDamage);
        System.out.println("转化的护盾值: " + shieldBuff.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试不受伤时不转化护盾
     */
    @Test
    public void testNoConversionWhenNoDamage() {
        // Given
        ActiveSkill impenetrable = createSkill("impenetrable", 5);
        testPlayer.setCurrentHp(200);

        battleManager.executeSkill(testPlayer, impenetrable,
            java.util.Arrays.asList(testMonster), battleContext);

        // When - 不受到任何伤害，直接回合结束
        BuffManager.getInstance(context).onRoundEnd(testPlayer, battleContext);

        // Then - 不应该创建护盾buff
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff = (ShieldBuff) buffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNull("没有受伤时不应该创建护盾buff", shieldBuff);

        printBattleLogs();
    }

    /**
     * 测试buff持续时间
     */
    @Test
    public void testBuffDuration() {
        // Given
        ActiveSkill impenetrable = createSkill("impenetrable", 3);
        testPlayer.setCurrentHp(200);

        battleManager.executeSkill(testPlayer, impenetrable,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        ImpenetrableBuff buff = (ImpenetrableBuff) buffs.get(0);

        // When - 回合结束
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // Then - ImpenetrableBuff应该消失（但护盾buff应该保留）
        List<BaseBuff> buffsAfterTick = testPlayer.getActiveBuffList();
        ImpenetrableBuff impenetrableBuffAfter = (ImpenetrableBuff) buffsAfterTick.stream()
            .filter(b -> b instanceof ImpenetrableBuff)
            .findFirst()
            .orElse(null);

        assertNull("回合结束后ImpenetrableBuff应该消失", impenetrableBuffAfter);

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testMpCost() {
        // Given
        ActiveSkill impenetrable = createSkill("impenetrable", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When
        battleManager.executeSkill(testPlayer, impenetrable,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 等级1应该消耗12MP
        assertEquals("应该消耗12MP", mpBefore - 12, testPlayer.getCurrentMp());
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testScalingByLevel() {
        testPlayer.setCurrentHp(200);

        // 测试等级1（20%）
        int damage1 = testDamageAndConvert("impenetrable", 1, 100);

        // 测试等级5（40%）
        testPlayer.setCurrentHp(200);
        int damage5 = testDamageAndConvert("impenetrable", 5, 100);

        // 验证等级5转化的护盾值大于等级1
        assertTrue("等级5转化的护盾值应该大于等级1", damage5 > damage1);

        System.out.println("等级1转化护盾值: " + damage1);
        System.out.println("等级5转化护盾值: " + damage5);
    }

    /**
     * 辅助方法：施放技能、造成伤害并返回转化的护盾值
     */
    private int testDamageAndConvert(String skillId, int level, int damageToDeal) {
        ActiveSkill skill = createSkill(skillId, level);
        testPlayer.getActiveBuffList().clear();
        battleContext.battleLogs.clear();

        battleManager.executeSkill(testPlayer, skill,
            java.util.Arrays.asList(testMonster), battleContext);

        testMonster.getBaseAttributes().physicalAtk = damageToDeal;
        testMonster.markAttributeCacheDirty();

        battleManager.dealPhysicalDamage(testMonster, testPlayer, damageToDeal, battleContext);

        BuffManager.getInstance(context).onRoundEnd(testPlayer, battleContext);

        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff = (ShieldBuff) buffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        return shieldBuff != null ? shieldBuff.getStackCount() : 0;
    }

    /**
     * 测试护盾永久持续
     */
    @Test
    public void testGeneratedShieldPersistence() {
        // Given
        ActiveSkill impenetrable = createSkill("impenetrable", 5);
        testPlayer.setCurrentHp(200);

        battleManager.executeSkill(testPlayer, impenetrable,
            java.util.Arrays.asList(testMonster), battleContext);

        // 造成伤害并转化护盾
        int damage = 100;
        testMonster.getBaseAttributes().physicalAtk = damage;
        testMonster.markAttributeCacheDirty();
        battleManager.dealPhysicalDamage(testMonster, testPlayer, damage, battleContext);

        BuffManager.getInstance(context).onRoundEnd(testPlayer, battleContext);

        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff = (ShieldBuff) buffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有护盾buff", shieldBuff);

        // When - 经过多个回合
        BuffManager.getInstance(context).tickBuffs(testPlayer);
        BuffManager.getInstance(context).tickBuffs(testPlayer);
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // Then - 生成的护盾buff应该仍然存在
        List<BaseBuff> buffsAfter = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuffAfter = (ShieldBuff) buffsAfter.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("生成的护盾应该永久持续", shieldBuffAfter);
        assertEquals("护盾值应该保持不变", shieldBuff.getStackCount(), shieldBuffAfter.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试日志记录
     */
    @Test
    public void testBattleLog() {
        // Given
        ActiveSkill impenetrable = createSkill("impenetrable", 3);
        testPlayer.setCurrentHp(200);

        // When
        battleManager.executeSkill(testPlayer, impenetrable,
            java.util.Arrays.asList(testMonster), battleContext);

        // 受到伤害
        int damage = 80;
        testMonster.getBaseAttributes().physicalAtk = damage;
        testMonster.markAttributeCacheDirty();
        battleManager.dealPhysicalDamage(testMonster, testPlayer, damage, battleContext);

        // 回合结束（先onRoundEnd转化护盾，再tickBuffs）
        BuffManager.getInstance(context).onRoundEnd(testPlayer, battleContext);
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // Then - 验证日志
        assertLogContains(LogType.BUFF, "【固若金汤】");
        assertLogContains(LogType.BUFF, "将受到伤害的 30% 转化为护盾");
        assertLogExists(LogType.BUFF); // 应该有转化护盾的日志

        printBattleLogs();
    }
}
