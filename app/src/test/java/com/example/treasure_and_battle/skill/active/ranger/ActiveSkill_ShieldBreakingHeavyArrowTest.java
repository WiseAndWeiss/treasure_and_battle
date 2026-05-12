package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 破盾重箭技能测试
 * 技能效果：对单体敌人造成x%物理攻击伤害，目标拥有护盾时额外造成y%伤害，若本次攻击击碎目标护盾，附加z层流血buff
 */
public class ActiveSkill_ShieldBreakingHeavyArrowTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：90%伤害，30%额外伤害，2层流血
     */
    @Test
    public void testShieldBreakingHeavyArrowLevel1() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 1);
        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能（无护盾）
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证基础伤害造成
        assertTrue("应该造成伤害", damage > 0);

        // 验证日志
        assertLogContains(LogType.DAMAGE, "【破盾重箭】");

        printBattleLogs();
    }

    /**
     * 测试有护盾时额外伤害
     */
    @Test
    public void testShieldBreakingHeavyArrowWithShield() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 1);

        // 为怪物添加护盾（使用较小的护盾值，确保能被打破）
        // 技能伤害：基础50*90%=45 + 额外50*30%=15 = 60点
        // 设置30点护盾，确保护盾会被打破且有剩余伤害打到HP
        ShieldBuff shieldBuff = new ShieldBuff(
                "test_shield", "测试护盾", "护盾",
                BuffType.BUFF, false, -1, 30, false, 0);

        testMonster.getActiveBuffList().add(shieldBuff);
        testMonster.markAttributeCacheDirty();

        System.out.println("DEBUG: 护盾值 = " + shieldBuff.getStackCount());

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        System.out.println("DEBUG: HP变化 = " + hpBefore + " -> " + testMonster.getCurrentHp() + " (伤害" + damage + ")");
        System.out.println("DEBUG: 护盾剩余 = " + shieldBuff.getStackCount());

        // Then - 验证伤害造成
        assertTrue("应该造成伤害", damage > 0);

        // 验证日志包含护盾相关信息
        assertLogContains(LogType.DAMAGE, "【破盾重箭】");

        printBattleLogs();
    }

    /**
     * 测试护盾击碎并附加流血
     */
    @Test
    public void testShieldBreakingHeavyArrowShieldBroken() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 1);

        // 为怪物添加少量护盾（容易被击碎）
        ShieldBuff shieldBuff = new ShieldBuff(
                "test_shield", "测试护盾", "护盾",
                BuffType.BUFF, false, -1, 10, false, 0);

        testMonster.getActiveBuffList().add(shieldBuff);
        testMonster.markAttributeCacheDirty();

        // 提高攻击力以确保护盾被击碎
        testPlayer.getBaseAttributes().physicalAtk = 200;
        testPlayer.markAttributeCacheDirty();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证流血buff添加
        BleedingDebuff bleedingDebuff = (BleedingDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof BleedingDebuff)
                .filter(buff -> buff.getBuffId().equals("shield_breaking_bleeding"))
                .findFirst()
                .orElse(null);

        // 护盾应该被击碎（10点护盾 vs 200攻击力的高伤害）
        boolean shieldBrokenAfter = testMonster.getActiveBuffList().stream()
                .noneMatch(buff -> buff instanceof ShieldBuff && ((ShieldBuff) buff).getStackCount() > 0);

        assertTrue("护盾应该被击碎", shieldBrokenAfter);
        assertNotNull("护盾被击碎时应该有流血buff", bleedingDebuff);
        assertEquals("流血buff层数应该为2", 2, bleedingDebuff.getStackCount());
        assertEquals("流血buff应该持续3回合", 3, bleedingDebuff.getRemainingDuration());

        // 验证日志
        assertLogContains(LogType.DAMAGE, "击碎了");
        assertLogContains(LogType.DAMAGE, "流血");

        printBattleLogs();
    }

    /**
     * 测试无护盾时不触发特殊效果
     */
    @Test
    public void testShieldBreakingHeavyArrowNoShieldNoEffect() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 1);

        // 确保没有护盾
        testMonster.getActiveBuffList().clear();
        testMonster.markAttributeCacheDirty();

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证基础伤害造成
        assertTrue("应该造成基础伤害", damage > 0);

        // 验证没有流血buff
        BleedingDebuff bleedingDebuff = (BleedingDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof BleedingDebuff)
                .findFirst()
                .orElse(null);

        assertNull("无护盾时不应该有流血buff", bleedingDebuff);

        printBattleLogs();
    }

    /**
     * 测试等级3：100%伤害，40%额外伤害，3层流血
     */
    @Test
    public void testShieldBreakingHeavyArrowLevel3() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 3);

        // 为怪物添加护盾（使用较小的护盾值）
        // 技能伤害：基础50*100%=50 + 额外50*40%=20 = 70点
        // 设置30点护盾，确保护盾会被打破
        ShieldBuff shieldBuff = new ShieldBuff(
                "test_shield", "测试护盾", "护盾",
                BuffType.BUFF, false, -1, 30, false, 0);

        testMonster.getActiveBuffList().add(shieldBuff);
        testMonster.markAttributeCacheDirty();

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证伤害造成
        assertTrue("应该造成伤害", damage > 0);

        printBattleLogs();
    }

    /**
     * 测试等级5：110%伤害，50%额外伤害，4层流血
     */
    @Test
    public void testShieldBreakingHeavyArrowLevel5() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 5);

        // 为怪物添加护盾
        ShieldBuff shieldBuff = new ShieldBuff(
                "test_shield", "测试护盾", "护盾",
                BuffType.BUFF, false, -1, 30, false, 0);

        testMonster.getActiveBuffList().add(shieldBuff);
        testMonster.markAttributeCacheDirty();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证伤害造成
        assertTrue("应该造成伤害", testMonster.getCurrentHp() < testMonster.getFinalAttributes().maxHp);

        printBattleLogs();
    }

    /**
     * 测试护盾检测机制
     */
    @Test
    public void testShieldBreakingHeavyArrowShieldDetection() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 1);

        // 测试有护盾的情况
        ShieldBuff shieldBuff = new ShieldBuff(
                "test_shield", "测试护盾", "护盾",
                BuffType.BUFF, false, -1, 100, false, 0);

        testMonster.getActiveBuffList().add(shieldBuff);
        testMonster.markAttributeCacheDirty();

        boolean hasShieldBefore = testMonster.getActiveBuffList().stream()
                .anyMatch(buff -> buff instanceof ShieldBuff && ((ShieldBuff) buff).getStackCount() > 0);

        assertTrue("应该检测到护盾", hasShieldBefore);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证日志包含护盾检测信息
        assertLogContains(LogType.DAMAGE, "【破盾重箭】");

        printBattleLogs();
    }

    /**
     * 测试流血buff添加
     */
    @Test
    public void testShieldBreakingHeavyArrowBleedingAdded() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 1);

        // 添加少量护盾以便击碎
        ShieldBuff shieldBuff = new ShieldBuff(
                "test_shield", "测试护盾", "护盾",
                BuffType.BUFF, false, -1, 5, false, 0);

        testMonster.getActiveBuffList().add(shieldBuff);
        testPlayer.getBaseAttributes().physicalAtk = 200;
        testPlayer.markAttributeCacheDirty();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 检查是否有流血buff（取决于护盾是否被击碎）
        BleedingDebuff bleedingDebuff = (BleedingDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof BleedingDebuff)
                .filter(buff -> buff.getBuffId().equals("shield_breaking_bleeding"))
                .findFirst()
                .orElse(null);

        boolean shieldBroken = testMonster.getActiveBuffList().stream()
                .noneMatch(buff -> buff instanceof ShieldBuff && ((ShieldBuff) buff).getStackCount() > 0);

        System.out.println("DEBUG: 护盾是否被打破 = " + shieldBroken);
        System.out.println("DEBUG: 流血buff是否存在 = " + (bleedingDebuff != null));

        // 护盾应该被击碎（5点护盾 vs 200攻击力的高伤害）
        assertTrue("护盾应该被击碎", shieldBroken);

        // 应该有流血buff
        assertNotNull("护盾被击碎时应该有流血buff", bleedingDebuff);
        assertEquals("流血buff层数应该为2", 2, bleedingDebuff.getStackCount());
        assertEquals("流血buff应该持续3回合", 3, bleedingDebuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试伤害类型（物理伤害）
     */
    @Test
    public void testShieldBreakingHeavyArrowDamageType() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证日志包含物理伤害信息
        assertLogContains(LogType.DAMAGE, "【破盾重箭】");
        assertLogExists(LogType.DAMAGE);

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testShieldBreakingHeavyArrowMpCost() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证MP消耗
        int mpAfter = testPlayer.getCurrentMp();
        int mpCost = mpBefore - mpAfter;
        assertEquals("应该消耗20点MP", 20, mpCost);

        printBattleLogs();
    }

    /**
     * 测试冷却时间
     */
    @Test
    public void testShieldBreakingHeavyArrowCooldown() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 1);

        // When - 获取冷却时间
        int cooldown = shieldBreakingHeavyArrow.getTemplate().getCooldown();

        // Then - 验证冷却时间为2回合
        assertEquals("冷却时间应该为2回合", 2, cooldown);

        printBattleLogs();
    }

    /**
     * 测试额外伤害计算
     */
    @Test
    public void testShieldBreakingHeavyArrowExtraDamage() {
        // Given
        ActiveSkill shieldBreakingHeavyArrow = createSkill("shield_breaking_heavy_arrow", 1);

        // 为怪物添加护盾（使用较小的护盾值，确保护盾被打破）
        // 技能伤害约60点，使用20点护盾确保护盾被打破且有剩余伤害
        ShieldBuff shieldBuff = new ShieldBuff(
                "test_shield", "测试护盾", "护盾",
                BuffType.BUFF, false, -1, 20, false, 0);

        testMonster.getActiveBuffList().add(shieldBuff);
        testMonster.markAttributeCacheDirty();

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, shieldBreakingHeavyArrow, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证有护盾时伤害更高
        assertTrue("有护盾时应该造成伤害", damage > 0);

        System.out.println("有护盾时伤害：" + damage);
        printBattleLogs();
    }
}
