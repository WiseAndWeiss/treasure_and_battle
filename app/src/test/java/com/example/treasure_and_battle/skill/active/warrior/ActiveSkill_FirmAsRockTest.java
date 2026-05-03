package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.buff.impl.skill.FirmAsRockBuff;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 坚如磐石技能测试
 * 技能效果：生成x%最大生命值的护盾，并获得持续两回合物防、法防提升y%
 */
public class ActiveSkill_FirmAsRockTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：5%最大生命值护盾，6%双防提升
     */
    @Test
    public void testFirmAsRockLevel1() {
        // Given
        ActiveSkill firmAsRock = createSkill("firm_as_rock", 1);
        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int maxHp = playerAttr.maxHp; // 200

        int expectedShieldPercent = 5;
        int expectedShieldValue = (int) (maxHp * expectedShieldPercent / 100.0f); // 10点护盾
        int expectedDefenseBoost = 6;

        int baseDef = playerAttr.physicalDef;
        int baseMdef = playerAttr.magicalDef;

        // When
        battleManager.executeSkill(testPlayer, firmAsRock,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证护盾buff
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        assertEquals("应该有2个buff（护盾+双防）", 2, buffs.size());

        ShieldBuff shieldBuff = (ShieldBuff) buffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有护盾buff", shieldBuff);
        assertEquals("护盾值应该正确", expectedShieldValue, shieldBuff.getStackCount());
        assertFalse("护盾buff应该不可驱散", shieldBuff.isDispellable());

        // 验证双防提升
        AttributeSet finalAttr = testPlayer.getFinalAttributes();
        int expectedDef = Math.round(baseDef * (1 + expectedDefenseBoost / 100.0f));
        int expectedMdef = Math.round(baseMdef * (1 + expectedDefenseBoost / 100.0f));

        assertEquals("物理防御应该提升6%", expectedDef, finalAttr.physicalDef);
        assertEquals("法术防御应该提升6%", expectedMdef, finalAttr.magicalDef);

        // 验证日志
        assertLogContains(LogType.BUFF, "【坚如磐石】");
        assertLogContains(LogType.BUFF, "生成了 " + expectedShieldValue + " 点护盾");

        printBattleLogs();
    }

    /**
     * 测试等级5：20%最大生命值护盾，15%双防提升
     */
    @Test
    public void testFirmAsRockLevel5() {
        // Given
        ActiveSkill firmAsRock = createSkill("firm_as_rock", 5);
        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int maxHp = playerAttr.maxHp; // 200

        int expectedShieldPercent = 20;
        int expectedShieldValue = (int) (maxHp * expectedShieldPercent / 100.0f); // 40点护盾
        int expectedDefenseBoost = 15;

        int baseDef = playerAttr.physicalDef;
        int baseMdef = playerAttr.magicalDef;

        // When
        battleManager.executeSkill(testPlayer, firmAsRock,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff = (ShieldBuff) buffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有护盾buff", shieldBuff);
        assertEquals("护盾值应该正确", expectedShieldValue, shieldBuff.getStackCount());

        AttributeSet finalAttr = testPlayer.getFinalAttributes();
        int expectedDef = Math.round(baseDef * (1 + expectedDefenseBoost / 100.0f));
        int expectedMdef = Math.round(baseMdef * (1 + expectedDefenseBoost / 100.0f));

        assertEquals("物理防御应该提升15%", expectedDef, finalAttr.physicalDef);
        assertEquals("法术防御应该提升15%", expectedMdef, finalAttr.magicalDef);

        printBattleLogs();
    }

    /**
     * 测试护盾吸收伤害
     */
    @Test
    public void testShieldAbsorption() {
        // Given
        ActiveSkill firmAsRock = createSkill("firm_as_rock", 3); // 12%护盾 = 24点
        testPlayer.setCurrentHp(200);

        battleManager.executeSkill(testPlayer, firmAsRock,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff = (ShieldBuff) buffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        int shieldValue = shieldBuff.getStackCount();
        int hpBefore = testPlayer.getCurrentHp();
        int actualDefense = testPlayer.getFinalAttributes().physicalDef; // 获取实际防御（已提升）

        // When - 造成小于护盾值的伤害
        int smallDamage = 50;  // 需要足够穿透实际防御
        testMonster.getBaseAttributes().physicalAtk = smallDamage;
        testMonster.markAttributeCacheDirty();

        battleManager.dealPhysicalDamage(testMonster, testPlayer, smallDamage, battleContext);

        // Then - 护盾应该吸收部分伤害，HP应该受到剩余伤害
        int hpAfter = testPlayer.getCurrentHp();
        int actualDamageToShield = smallDamage - actualDefense; // 实际伤害
        int expectedHpLoss = Math.max(0, actualDamageToShield - shieldValue); // 护盾吸收后的剩余伤害
        assertEquals("HP应该减少" + expectedHpLoss + "点", hpBefore - expectedHpLoss, hpAfter);

        int remainingShield = shieldBuff.getStackCount();
        int expectedShieldRemaining = Math.max(0, shieldValue - actualDamageToShield); // 护盾被击碎或减少
        assertEquals("护盾应该被击碎", 0, remainingShield);

        // When - 造成大于护盾值的伤害
        int largeDamage = 80;  // 足够击碎剩余护盾并对HP造成伤害
        testMonster.getBaseAttributes().physicalAtk = largeDamage;
        testMonster.markAttributeCacheDirty();

        int hpBefore2 = testPlayer.getCurrentHp();
        battleManager.dealPhysicalDamage(testMonster, testPlayer, largeDamage, battleContext);

        // Then - 护盾应该被击碎，HP应该受到剩余伤害
        int hpAfter2 = testPlayer.getCurrentHp();
        int actualDamage2 = largeDamage - actualDefense; // 实际伤害
        int expectedHpDamage = Math.max(0, actualDamage2 - expectedShieldRemaining);

        assertTrue("护盾应该被击碎", shieldBuff.isExpired() || shieldBuff.getStackCount() == 0);
        assertEquals("HP应该受到剩余伤害", hpBefore2 - expectedHpDamage, hpAfter2);

        printBattleLogs();
    }

    /**
     * 测试护盾永久持续直到被击碎
     */
    @Test
    public void testShieldPersistence() {
        // Given
        ActiveSkill firmAsRock = createSkill("firm_as_rock", 1);
        battleManager.executeSkill(testPlayer, firmAsRock,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff = (ShieldBuff) buffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有护盾buff", shieldBuff);

        // When - 经过多个回合（只要没有受到伤害）
        BuffManager.getInstance(context).tickBuffs(testPlayer);
        BuffManager.getInstance(context).tickBuffs(testPlayer);
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // Then - 护盾buff应该仍然存在（永久持续直到被击碎）
        List<BaseBuff> buffsAfter = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuffAfter = (ShieldBuff) buffsAfter.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("护盾buff应该仍然存在", shieldBuffAfter);
        assertEquals("护盾值应该保持不变", shieldBuff.getStackCount(), shieldBuffAfter.getStackCount());

        printBattleLogs();
    }

    /**
     * 测试双防buff持续时间
     */
    @Test
    public void testDefenseBuffDuration() {
        // Given
        ActiveSkill firmAsRock = createSkill("firm_as_rock", 3);
        battleManager.executeSkill(testPlayer, firmAsRock,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        FirmAsRockBuff defenseBuff = (FirmAsRockBuff) buffs.stream()
            .filter(buff -> buff instanceof FirmAsRockBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有双防buff", defenseBuff);
        assertEquals("双防buff应该持续2回合", 2, defenseBuff.getRemainingDuration());

        // When - 经过1回合
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // Then
        List<BaseBuff> buffsAfterTick1 = testPlayer.getActiveBuffList();
        FirmAsRockBuff defenseBuffAfter1 = (FirmAsRockBuff) buffsAfterTick1.stream()
            .filter(buff -> buff instanceof FirmAsRockBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("经过1回合后双防buff应该仍然存在", defenseBuffAfter1);
        assertEquals("剩余持续时间应该为1", 1, defenseBuffAfter1.getRemainingDuration());

        // When - 再经过1回合
        BuffManager.getInstance(context).tickBuffs(testPlayer);

        // Then - 双防buff应该消失，但护盾buff应该保留
        List<BaseBuff> buffsAfterTick2 = testPlayer.getActiveBuffList();
        FirmAsRockBuff defenseBuffAfter2 = (FirmAsRockBuff) buffsAfterTick2.stream()
            .filter(buff -> buff instanceof FirmAsRockBuff)
            .findFirst()
            .orElse(null);

        assertNull("经过2回合后双防buff应该消失", defenseBuffAfter2);

        ShieldBuff shieldBuff = (ShieldBuff) buffsAfterTick2.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("护盾buff应该仍然存在", shieldBuff);

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testMpCost() {
        // Given
        ActiveSkill firmAsRock = createSkill("firm_as_rock", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When
        battleManager.executeSkill(testPlayer, firmAsRock,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 等级1应该消耗15MP
        assertEquals("应该消耗15MP", mpBefore - 15, testPlayer.getCurrentMp());
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testScalingByLevel() {
        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int maxHp = playerAttr.maxHp; // 200

        // 测试等级1（5%护盾，6%双防）
        testPlayer.getActiveBuffList().clear();
        battleContext.battleLogs.clear();
        ActiveSkill skill1 = createSkill("firm_as_rock", 1);
        battleManager.executeSkill(testPlayer, skill1,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs1 = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff1 = (ShieldBuff) buffs1.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);
        int shieldValue1 = shieldBuff1.getStackCount();

        // 测试等级5（20%护盾，15%双防）
        testPlayer.getActiveBuffList().clear();
        battleContext.battleLogs.clear();
        ActiveSkill skill5 = createSkill("firm_as_rock", 5);
        battleManager.executeSkill(testPlayer, skill5,
            java.util.Arrays.asList(testMonster), battleContext);

        List<BaseBuff> buffs5 = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff5 = (ShieldBuff) buffs5.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);
        int shieldValue5 = shieldBuff5.getStackCount();

        // 验证护盾值递增
        assertTrue("等级5护盾值应该大于等级1", shieldValue5 > shieldValue1);

        System.out.println("等级1护盾值: " + shieldValue1);
        System.out.println("等级5护盾值: " + shieldValue5);
    }

    /**
     * 测试护盾值计算准确性
     */
    @Test
    public void testShieldValueCalculation() {
        // Given
        ActiveSkill firmAsRock = createSkill("firm_as_rock", 5); // 20%
        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        int maxHp = playerAttr.maxHp; // 200

        int expectedShieldValue = (int) (maxHp * 20 / 100.0f); // 40点

        // When
        battleManager.executeSkill(testPlayer, firmAsRock,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        ShieldBuff shieldBuff = (ShieldBuff) buffs.stream()
            .filter(buff -> buff instanceof ShieldBuff)
            .findFirst()
            .orElse(null);

        assertEquals("护盾值应该为最大生命值的20%", expectedShieldValue, shieldBuff.getStackCount());
    }

    /**
     * 测试护盾与双防buff的正确类型
     */
    @Test
    public void testBuffTypes() {
        // Given
        ActiveSkill firmAsRock = createSkill("firm_as_rock", 3);

        // When
        battleManager.executeSkill(testPlayer, firmAsRock,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证buff类型
        List<BaseBuff> buffs = testPlayer.getActiveBuffList();
        assertEquals("应该有2个buff", 2, buffs.size());

        boolean hasShieldBuff = buffs.stream().anyMatch(buff -> buff instanceof ShieldBuff);
        boolean hasFirmAsRockBuff = buffs.stream().anyMatch(buff -> buff instanceof FirmAsRockBuff);

        assertTrue("应该有护盾buff", hasShieldBuff);
        assertTrue("应该有双防buff", hasFirmAsRockBuff);

        printBattleLogs();
    }

    /**
     * 测试日志记录
     */
    @Test
    public void testBattleLog() {
        // Given
        ActiveSkill firmAsRock = createSkill("firm_as_rock", 3);

        // When
        battleManager.executeSkill(testPlayer, firmAsRock,
            java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证日志
        assertLogExists(LogType.BUFF);
        assertLogContains(LogType.BUFF, "【坚如磐石】");
        assertLogContains(LogType.BUFF, "生成了");
        assertLogContains(LogType.BUFF, "点护盾");
        assertLogContains(LogType.BUFF, "物理防御与法术防御提升");

        printBattleLogs();
    }
}
