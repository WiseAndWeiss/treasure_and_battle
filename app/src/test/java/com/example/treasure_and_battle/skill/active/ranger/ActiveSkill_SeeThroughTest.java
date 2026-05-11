package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 看破技能测试
 * 技能效果：降低目标x%物理防御和y%法术防御
 */
public class ActiveSkill_SeeThroughTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：降低8%物防和8%法防
     */
    @Test
    public void testSeeThroughLevel1() {
        // Given
        ActiveSkill seeThrough = createSkill("see_through", 1);

        int physicalDefBefore = testMonster.getFinalAttributes().physicalDef;
        int magicalDefBefore = testMonster.getFinalAttributes().magicalDef;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, seeThrough, java.util.Arrays.asList(testMonster), battleContext);

        // Then
        int physicalDefAfter = testMonster.getFinalAttributes().physicalDef;
        int magicalDefAfter = testMonster.getFinalAttributes().magicalDef;

        assertTrue("物理防御应该降低", physicalDefAfter < physicalDefBefore);
        assertTrue("法术防御应该降低", magicalDefAfter < magicalDefBefore);

        // 验证防御降低约8%
        float physicalReduction = (float) (physicalDefBefore - physicalDefAfter) / physicalDefBefore;
        float magicalReduction = (float) (magicalDefBefore - magicalDefAfter) / magicalDefBefore;

        assertTrue("物理防御应该降低", physicalDefAfter < physicalDefBefore);
        assertTrue("法术防御应该降低", magicalDefAfter < magicalDefBefore);
        // 放宽容差范围，因为取整会导致误差（20点防御的8%是1.6，四舍五入为2点，实际降低10%）
        assertTrue("物理防御降低应该接近8%或10%（取整误差）",
                  Math.abs(physicalReduction - 0.08f) < 0.03f || Math.abs(physicalReduction - 0.10f) < 0.01f);
        assertTrue("法术防御降低应该接近8%或10%（取整误差）",
                  Math.abs(magicalReduction - 0.08f) < 0.03f || Math.abs(magicalReduction - 0.10f) < 0.01f);

        // 验证日志
        assertLogContains(LogType.BUFF, "【看破】");

        printBattleLogs();
    }

    /**
     * 测试等级3：降低16%物防和16%法防
     */
    @Test
    public void testSeeThroughLevel3() {
        // Given
        ActiveSkill seeThrough = createSkill("see_through", 3);

        int physicalDefBefore = testMonster.getFinalAttributes().physicalDef;
        int magicalDefBefore = testMonster.getFinalAttributes().magicalDef;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, seeThrough, java.util.Arrays.asList(testMonster), battleContext);

        // Then
        int physicalDefAfter = testMonster.getFinalAttributes().physicalDef;
        int magicalDefAfter = testMonster.getFinalAttributes().magicalDef;

        // 验证防御降低约16%
        float physicalReduction = (float) (physicalDefBefore - physicalDefAfter) / physicalDefBefore;
        float magicalReduction = (float) (magicalDefBefore - magicalDefAfter) / magicalDefBefore;

        assertTrue("物理防御应该降低", physicalDefAfter < physicalDefBefore);
        assertTrue("法术防御应该降低", magicalDefAfter < magicalDefBefore);
        // 放宽容差范围，因为取整会导致误差（20点防御的16%是3.2，四舍五入为3点，实际降低15%）
        assertTrue("物理防御降低应该接近16%或15%（取整误差）",
                  Math.abs(physicalReduction - 0.16f) < 0.02f || Math.abs(physicalReduction - 0.15f) < 0.01f);
        assertTrue("法术防御降低应该接近16%或15%（取整误差）",
                  Math.abs(magicalReduction - 0.16f) < 0.02f || Math.abs(magicalReduction - 0.15f) < 0.01f);

        printBattleLogs();
    }

    /**
     * 测试等级5：降低25%物防和25%法防
     */
    @Test
    public void testSeeThroughLevel5() {
        // Given
        ActiveSkill seeThrough = createSkill("see_through", 5);

        int physicalDefBefore = testMonster.getFinalAttributes().physicalDef;
        int magicalDefBefore = testMonster.getFinalAttributes().magicalDef;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, seeThrough, java.util.Arrays.asList(testMonster), battleContext);

        // Then
        int physicalDefAfter = testMonster.getFinalAttributes().physicalDef;
        int magicalDefAfter = testMonster.getFinalAttributes().magicalDef;

        // 验证防御降低约25%
        float physicalReduction = (float) (physicalDefBefore - physicalDefAfter) / physicalDefBefore;
        float magicalReduction = (float) (magicalDefBefore - magicalDefAfter) / magicalDefBefore;

        assertTrue("物理防御应该降低", physicalDefAfter < physicalDefBefore);
        assertTrue("法术防御应该降低", magicalDefAfter < magicalDefBefore);
        // 等级5正好是25%，无取整误差（20 * 0.25 = 5.0）
        assertTrue("物理防御降低应该是25%", Math.abs(physicalReduction - 0.25f) < 0.02f);
        assertTrue("法术防御降低应该是25%", Math.abs(magicalReduction - 0.25f) < 0.02f);

        printBattleLogs();
    }

    /**
     * 测试buff是否正确添加
     */
    @Test
    public void testSeeThroughBuffsAdded() {
        // Given
        ActiveSkill seeThrough = createSkill("see_through", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, seeThrough, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证物防降低buff存在
        AttributeBuff physicalDefBuff = (AttributeBuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("see_through_physical_def_reduction"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有物防降低buff", physicalDefBuff);
        assertEquals("物防buff应该持续1回合", 1, physicalDefBuff.getRemainingDuration());

        // 验证法防降低buff存在
        AttributeBuff magicalDefBuff = (AttributeBuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("see_through_magical_def_reduction"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有法防降低buff", magicalDefBuff);
        assertEquals("法防buff应该持续1回合", 1, magicalDefBuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试buff持续时间
     */
    @Test
    public void testSeeThroughBuffDuration() {
        // Given
        ActiveSkill seeThrough = createSkill("see_through", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, seeThrough, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证buff持续时间
        AttributeBuff physicalDefBuff = (AttributeBuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("see_through_physical_def_reduction"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有物防降低buff", physicalDefBuff);
        assertEquals("buff应该持续1回合", 1, physicalDefBuff.getRemainingDuration());

        // 模拟回合结束，buff应该消失
        physicalDefBuff.tick();
        assertEquals("buff持续回合后应该被移除", 0, physicalDefBuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试buff效果确实生效
     */
    @Test
    public void testSeeThroughBuffEffectiveness() {
        // Given
        ActiveSkill seeThrough = createSkill("see_through", 1);

        // 记录初始防御
        int initialPhysicalDef = testMonster.getFinalAttributes().physicalDef;
        int initialMagicalDef = testMonster.getFinalAttributes().magicalDef;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, seeThrough, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证防御确实改变了
        int currentPhysicalDef = testMonster.getFinalAttributes().physicalDef;
        int currentMagicalDef = testMonster.getFinalAttributes().magicalDef;

        assertNotEquals("物理防御应该改变", initialPhysicalDef, currentPhysicalDef);
        assertNotEquals("法术防御应该改变", initialMagicalDef, currentMagicalDef);

        printBattleLogs();
    }

    /**
     * 测试多次施放不重复添加buff
     */
    @Test
    public void testSeeThroughNoDuplicateBuffs() {
        // Given
        ActiveSkill seeThrough = createSkill("see_through", 1);

        // When - 施放技能两次
        battleManager.executeSkill(testPlayer, seeThrough, java.util.Arrays.asList(testMonster), battleContext);
        battleManager.executeSkill(testPlayer, seeThrough, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 应该不会重复添加buff
        long physicalBuffCount = testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("see_through_physical_def_reduction"))
                .count();

        long magicalBuffCount = testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("see_through_magical_def_reduction"))
                .count();

        assertEquals("应该只有1个物防buff", 1, physicalBuffCount);
        assertEquals("应该只有1个法防buff", 1, magicalBuffCount);

        printBattleLogs();
    }
}
