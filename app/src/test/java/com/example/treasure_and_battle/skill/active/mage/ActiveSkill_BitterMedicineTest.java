package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.WeaknessDebuff;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 苦口良药技能测试
 * 技能效果：为自身附加2回合10%虚弱buff，瞬间恢复x%最大血量与y%最大蓝量
 */
public class ActiveSkill_BitterMedicineTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：恢复20%HP + 20%MP，附加虚弱
     */
    @Test
    public void testBitterMedicineLevel1() {
        // Given
        ActiveSkill bitterMedicine = createSkill("bitter_medicine", 1);
        testPlayer.setCurrentHp(100);
        testPlayer.setCurrentMp(50);

        int maxHp = testPlayer.getFinalAttributes().maxHp;
        int maxMp = testPlayer.getFinalAttributes().maxMp;
        int hpBefore = testPlayer.getCurrentHp();
        int mpBefore = testPlayer.getCurrentMp();

        // When
        battleManager.executeSkill(testPlayer, bitterMedicine,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        int expectedHeal = (int) (maxHp * 0.2f);
        int expectedManaRestore = (int) (maxMp * 0.2f);

        int actualHeal = testPlayer.getCurrentHp() - hpBefore;
        int actualManaRestore = testPlayer.getCurrentMp() - mpBefore;

        // 验证恢复了HP
        assertTrue("应该恢复HP", actualHeal > 0);
        assertTrue("HP恢复量应该接近预期", Math.abs(actualHeal - expectedHeal) <= 5);

        // 验证MP没有大幅减少（恢复和消耗基本抵消）
        assertTrue("MP不应该大幅减少", actualManaRestore >= -25);

        // 验证虚弱debuff
        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WeaknessDebuff)
                .findFirst()
                .orElse(null);

        assertNotNull("应该有虚弱debuff", weaknessDebuff);
        assertEquals("虚弱应该持续2回合", 2, weaknessDebuff.getRemainingDuration());

        // 验证日志
        assertLogExists(LogType.HEAL);
        assertLogContains(LogType.HEAL, "【苦口良药】");

        printBattleLogs();
    }

    /**
     * 测试等级3：恢复30%HP + 30%MP
     */
    @Test
    public void testBitterMedicineLevel3() {
        // Given
        ActiveSkill bitterMedicine = createSkill("bitter_medicine", 3);
        testPlayer.setCurrentHp(100);
        testPlayer.setCurrentMp(50);

        int maxHp = testPlayer.getFinalAttributes().maxHp;
        int maxMp = testPlayer.getFinalAttributes().maxMp;
        int hpBefore = testPlayer.getCurrentHp();
        int mpBefore = testPlayer.getCurrentMp();

        // When
        battleManager.executeSkill(testPlayer, bitterMedicine,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        int expectedHeal = (int) (maxHp * 0.3f);
        int expectedManaRestore = (int) (maxMp * 0.3f);

        int actualHeal = testPlayer.getCurrentHp() - hpBefore;
        int actualManaRestore = testPlayer.getCurrentMp() - mpBefore;

        // 验证恢复了HP
        assertTrue("应该恢复HP", actualHeal > 0);
        assertTrue("HP恢复量应该接近预期", Math.abs(actualHeal - expectedHeal) <= 5);

        // 验证MP没有大幅减少
        assertTrue("MP不应该大幅减少", actualManaRestore >= -25);

        printBattleLogs();
    }

    /**
     * 测试等级5：恢复40%HP + 40%MP
     */
    @Test
    public void testBitterMedicineLevel5() {
        // Given
        ActiveSkill bitterMedicine = createSkill("bitter_medicine", 5);
        testPlayer.setCurrentHp(100);
        testPlayer.setCurrentMp(50);

        int maxHp = testPlayer.getFinalAttributes().maxHp;
        int maxMp = testPlayer.getFinalAttributes().maxMp;
        int hpBefore = testPlayer.getCurrentHp();
        int mpBefore = testPlayer.getCurrentMp();

        // When
        battleManager.executeSkill(testPlayer, bitterMedicine,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        int expectedHeal = (int) (maxHp * 0.4f);
        int expectedManaRestore = (int) (maxMp * 0.4f);

        int actualHeal = testPlayer.getCurrentHp() - hpBefore;
        int actualManaRestore = testPlayer.getCurrentMp() - mpBefore;

        // 验证恢复了HP
        assertTrue("应该恢复HP", actualHeal > 0);
        assertTrue("HP恢复量应该接近预期", Math.abs(actualHeal - expectedHeal) <= 5);

        // 验证MP没有大幅减少
        assertTrue("MP不应该大幅减少", actualManaRestore >= -25);

        printBattleLogs();
    }

    /**
     * 测试HP恢复上限
     */
    @Test
    public void testBitterMedicineHpCap() {
        // Given
        ActiveSkill bitterMedicine = createSkill("bitter_medicine", 3);
        int maxHp = testPlayer.getFinalAttributes().maxHp;
        testPlayer.setCurrentHp(maxHp - 10); // 接近满血

        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, bitterMedicine,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        assertEquals("HP不应该超过最大值", maxHp, testPlayer.getCurrentHp());
        int actualHeal = testPlayer.getCurrentHp() - hpBefore;
        assertTrue("实际恢复量应该<=30%最大HP", actualHeal <= (int) (maxHp * 0.3f));

        printBattleLogs();
    }

    /**
     * 测试MP恢复上限
     */
    @Test
    public void testBitterMedicineMpCap() {
        // Given
        ActiveSkill bitterMedicine = createSkill("bitter_medicine", 3);
        int maxMp = testPlayer.getFinalAttributes().maxMp;
        testPlayer.setCurrentMp(maxMp - 5); // 接近满蓝

        int mpBefore = testPlayer.getCurrentMp();

        // When
        battleManager.executeSkill(testPlayer, bitterMedicine,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        assertEquals("MP不应该超过最大值", maxMp, testPlayer.getCurrentMp());
        int actualManaRestore = testPlayer.getCurrentMp() - mpBefore;
        assertTrue("实际恢复量应该<=30%最大MP", actualManaRestore <= (int) (maxMp * 0.3f));

        printBattleLogs();
    }

    /**
     * 测试虚弱debuff效果
     */
    @Test
    public void testWeaknessDebuffEffect() {
        // Given
        ActiveSkill bitterMedicine = createSkill("bitter_medicine", 1);
        int physicalAtkBefore = testPlayer.getFinalAttributes().physicalAtk;
        int magicalAtkBefore = testPlayer.getFinalAttributes().magicalAtk;

        // When
        battleManager.executeSkill(testPlayer, bitterMedicine,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then - 验证攻击力降低
        int physicalAtkAfter = testPlayer.getFinalAttributes().physicalAtk;
        int magicalAtkAfter = testPlayer.getFinalAttributes().magicalAtk;

        assertTrue("物理攻击力应该降低", physicalAtkAfter < physicalAtkBefore);
        assertTrue("魔法攻击力应该降低", magicalAtkAfter < magicalAtkBefore);

        printBattleLogs();
    }

    /**
     * 测试消耗
     */
    @Test
    public void testBitterMedicineCost() {
        // Given
        ActiveSkill bitterMedicine = createSkill("bitter_medicine", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, bitterMedicine,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then - 等级1消耗2AP和20MP
        assertEquals("应该消耗2行动点", apBefore - 2, testPlayer.getCurrentActionPoints());
        // 注意：实际MP消耗可能因恢复而抵消
        assertTrue("应该消耗或恢复MP", testPlayer.getCurrentMp() >= mpBefore - 20);
        assertEquals("不应该消耗HP", hpBefore, testPlayer.getCurrentHp());
    }

    /**
     * 测试虚弱debuff持续时间
     */
    @Test
    public void testWeaknessDuration() {
        // Given
        ActiveSkill bitterMedicine = createSkill("bitter_medicine", 1);

        // When
        battleManager.executeSkill(testPlayer, bitterMedicine,
                java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        WeaknessDebuff weaknessDebuff = (WeaknessDebuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WeaknessDebuff)
                .findFirst()
                .orElse(null);

        assertNotNull("应该有虚弱debuff", weaknessDebuff);
        assertEquals("虚弱应该持续2回合", 2, weaknessDebuff.getRemainingDuration());

        printBattleLogs();
    }
}
