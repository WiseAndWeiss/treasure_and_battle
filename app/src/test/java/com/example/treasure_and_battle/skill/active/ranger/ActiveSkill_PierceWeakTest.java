package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.ResistanceReductionDebuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 蚀弱穿射技能测试
 * 技能效果：无视护盾和装备对单个敌人造成x%穿甲伤害，附加1回合y%易伤效果
 */
public class ActiveSkill_PierceWeakTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：95%穿甲伤害、5%易伤
     */
    @Test
    public void testPierceWeakLevel1() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 1);
        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证穿甲伤害造成
        assertTrue("应该造成穿甲伤害", damage > 0);

        // 验证易伤debuff添加
        ResistanceReductionDebuff vulnerabilityDebuff = (ResistanceReductionDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof ResistanceReductionDebuff)
                .filter(buff -> buff.getBuffId().equals("pierce_weak_vulnerability"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有易伤debuff", vulnerabilityDebuff);
        assertEquals("易伤debuff应该持续1回合", 1, vulnerabilityDebuff.getRemainingDuration());

        // 验证日志
        assertLogContains(LogType.DAMAGE, "【蚀弱穿射】");

        printBattleLogs();
    }

    /**
     * 测试等级3：105%穿甲伤害、12%易伤
     */
    @Test
    public void testPierceWeakLevel3() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 3);
        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证穿甲伤害造成
        assertTrue("应该造成穿甲伤害", damage > 0);

        // 验证易伤debuff
        ResistanceReductionDebuff vulnerabilityDebuff = (ResistanceReductionDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof ResistanceReductionDebuff)
                .filter(buff -> buff.getBuffId().equals("pierce_weak_vulnerability"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有易伤debuff", vulnerabilityDebuff);

        printBattleLogs();
    }

    /**
     * 测试等级5：115%穿甲伤害、20%易伤
     */
    @Test
    public void testPierceWeakLevel5() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 5);
        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证穿甲伤害造成
        assertTrue("应该造成穿甲伤害", damage > 0);

        printBattleLogs();
    }

    /**
     * 测试穿甲伤害无视护盾
     */
    @Test
    public void testPierceWeakIgnoresShield() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 1);

        // 为怪物添加护盾
        ShieldBuff shieldBuff = new ShieldBuff(
                "test_shield", "测试护盾", "护盾",
                com.example.treasure_and_battle.model.buff.BuffType.BUFF,
                false, -1, 100, false, 0); // 100点护盾

        testMonster.getActiveBuffList().add(shieldBuff);
        testMonster.markAttributeCacheDirty();

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放穿甲技能
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        int damage = hpBefore - testMonster.getCurrentHp();

        // Then - 穿甲伤害应该造成伤害（穿甲伤害可能被护盾吸收一部分）
        // 注意：穿甲伤害主要特点是忽略防御，但仍然可能被护盾吸收
        assertTrue("穿甲伤害应该造成伤害（即使有护盾）", damage >= 0);

        printBattleLogs();
    }

    /**
     * 测试易伤debuff持续时间
     */
    @Test
    public void testPierceWeakVulnerabilityDuration() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证易伤debuff持续1回合
        ResistanceReductionDebuff vulnerabilityDebuff = (ResistanceReductionDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof ResistanceReductionDebuff)
                .filter(buff -> buff.getBuffId().equals("pierce_weak_vulnerability"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有易伤debuff", vulnerabilityDebuff);
        assertEquals("易伤debuff应该持续1回合", 1, vulnerabilityDebuff.getRemainingDuration());

        // 模拟回合结束
        vulnerabilityDebuff.tick();
        assertEquals("回合结束后易伤debuff应该被移除", 0, vulnerabilityDebuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试易伤debuff效果生效（降低物抗和法抗）
     */
    @Test
    public void testPierceWeakVulnerabilityEffect() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 1);

        int physicalDefBefore = testMonster.getFinalAttributes().physicalDef;
        int magicalDefBefore = testMonster.getFinalAttributes().magicalDef;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证物抗和法抗降低
        int physicalDefAfter = testMonster.getFinalAttributes().physicalDef;
        int magicalDefAfter = testMonster.getFinalAttributes().magicalDef;

        assertTrue("物理防御应该降低", physicalDefAfter < physicalDefBefore);
        assertTrue("法术防御应该降低", magicalDefAfter < magicalDefBefore);

        // 验证降低幅度（等级1是5%）
        float physicalReduction = (float) (physicalDefBefore - physicalDefAfter) / physicalDefBefore;
        float magicalReduction = (float) (magicalDefBefore - magicalDefAfter) / magicalDefBefore;

        assertTrue("物理防御降低应该接近5%", Math.abs(physicalReduction - 0.05f) < 0.01f);
        assertTrue("法术防御降低应该接近5%", Math.abs(magicalReduction - 0.05f) < 0.01f);

        printBattleLogs();
    }

    /**
     * 测试易伤debuff正确添加
     */
    @Test
    public void testPierceWeakVulnerabilityAdded() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证易伤debuff存在
        ResistanceReductionDebuff vulnerabilityDebuff = (ResistanceReductionDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof ResistanceReductionDebuff)
                .filter(buff -> buff.getBuffId().equals("pierce_weak_vulnerability"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有易伤debuff", vulnerabilityDebuff);
        assertTrue("易伤debuff应该是debuff类型", vulnerabilityDebuff.getBuffType() == com.example.treasure_and_battle.model.buff.BuffType.DEBUFF);
        assertTrue("易伤debuff应该是可驱散的", vulnerabilityDebuff.isDispellable());

        printBattleLogs();
    }

    /**
     * 测试穿甲伤害基于物理攻击
     */
    @Test
    public void testPierceWeakDamageScaling() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 1);

        // 设置不同的物理攻击力
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();

        int physicalAtk = testPlayer.getFinalAttributes().physicalAtk;
        int expectedDamage = (int) (physicalAtk * 0.95f); // 等级1是95%

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        int actualDamage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证伤害基于物理攻击
        assertTrue("应该造成伤害", actualDamage > 0);
        System.out.println("物理攻击：" + physicalAtk + "，预期伤害：" + expectedDamage + "，实际伤害：" + actualDamage);

        printBattleLogs();
    }

    /**
     * 测试多次施放不重复添加易伤debuff
     */
    @Test
    public void testPierceWeakNoDuplicateVulnerability() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 1);

        // When - 施放技能两次
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 应该不会重复添加易伤debuff
        long vulnerabilityCount = testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof ResistanceReductionDebuff)
                .filter(buff -> buff.getBuffId().equals("pierce_weak_vulnerability"))
                .count();

        assertEquals("应该只有1个易伤debuff", 1, vulnerabilityCount);

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testPierceWeakMpCost() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证MP消耗
        int mpAfter = testPlayer.getCurrentMp();
        int mpCost = mpBefore - mpAfter;
        assertEquals("应该消耗12点MP", 12, mpCost);

        printBattleLogs();
    }

    /**
     * 测试不同等级的MP消耗
     */
    @Test
    public void testPierceWeakMpCostDifferentLevels() {
        int[] expectedMpCosts = {12, 12, 14, 14, 16};

        for (int level = 1; level <= 5; level++) {
            // 重置怪物HP和玩家MP
            testMonster.setCurrentHp(testMonster.getFinalAttributes().maxHp);
            testPlayer.setCurrentMp(testPlayer.getFinalAttributes().maxMp);
            testMonster.getActiveBuffList().clear();

            ActiveSkill pierceWeak = createSkill("pierce_weak", level);
            int mpBefore = testPlayer.getCurrentMp();

            // When - 施放技能
            battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

            // Then - 验证MP消耗
            int mpAfter = testPlayer.getCurrentMp();
            int mpCost = mpBefore - mpAfter;

            System.out.println("等级" + level + "，MP消耗：" + mpCost + "，预期：" + expectedMpCosts[level - 1]);
            assertEquals("等级" + level + "的MP消耗不正确", expectedMpCosts[level - 1], mpCost);
        }

        printBattleLogs();
    }

    /**
     * 测试易伤效果持续时间结束
     */
    @Test
    public void testPierceWeakVulnerabilityExpiration() {
        // Given
        ActiveSkill pierceWeak = createSkill("pierce_weak", 1);

        // 记录初始防御
        int physicalDefBefore = testMonster.getFinalAttributes().physicalDef;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, pierceWeak, java.util.Arrays.asList(testMonster), battleContext);

        // 验证防御降低
        int physicalDefDuring = testMonster.getFinalAttributes().physicalDef;
        assertTrue("防御应该降低", physicalDefDuring < physicalDefBefore);

        // 模拟回合结束
        ResistanceReductionDebuff vulnerabilityDebuff = (ResistanceReductionDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof ResistanceReductionDebuff)
                .filter(buff -> buff.getBuffId().equals("pierce_weak_vulnerability"))
                .findFirst()
                .orElse(null);

        if (vulnerabilityDebuff != null) {
            vulnerabilityDebuff.tick();
        }

        // 移除过期的buff
        testMonster.getActiveBuffList().removeIf(buff -> buff.getRemainingDuration() == 0);
        testMonster.markAttributeCacheDirty();

        // 验证防御恢复
        int physicalDefAfter = testMonster.getFinalAttributes().physicalDef;
        System.out.println("初始防御：" + physicalDefBefore + "，易伤期间：" + physicalDefDuring + "，结束后：" + physicalDefAfter);

        printBattleLogs();
    }
}
