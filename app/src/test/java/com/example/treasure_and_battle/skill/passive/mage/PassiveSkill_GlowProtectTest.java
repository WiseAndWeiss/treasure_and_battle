package com.example.treasure_and_battle.skill.passive.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 辉光庇护技能测试
 * 技能效果：战斗开始时，提升自身精神属性千分之{x}的闪避率与异常抵抗率
 */
public class PassiveSkill_GlowProtectTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：精神 x 千分之15
     */
    @Test
    public void testGlowProtectLevel1() {
        // Given
        PassiveSkill glowProtect = createPassiveSkill("glow_protect", 1);
        addPassiveSkillToEntity(testPlayer, glowProtect);

        int spirit = testPlayer.getFinalAttributes().spirit;
        float dodgeBefore = testPlayer.getFinalAttributes().dodgeRate;
        float resistBefore = testPlayer.getFinalAttributes().debuffResist;

        // When - 触发战斗开始
        glowProtect.onBattleStart(testPlayer, battleContext);

        // Then
        int expectedBoost = (int) (spirit * 15 / 1000.0f);
        float dodgeAfter = testPlayer.getFinalAttributes().dodgeRate;
        float resistAfter = testPlayer.getFinalAttributes().debuffResist;

        assertEquals("闪避率应该提升", dodgeBefore + expectedBoost, dodgeAfter, 0.1f);
        assertEquals("异常抵抗率应该提升", resistBefore + expectedBoost, resistAfter, 0.1f);

        // 验证日志
        assertLogExists(LogType.BUFF);
        assertLogContains(LogType.BUFF, "【辉光庇护】");

        printBattleLogs();
    }

    /**
     * 测试等级3：精神 x 千分之35
     */
    @Test
    public void testGlowProtectLevel3() {
        // Given
        PassiveSkill glowProtect = createPassiveSkill("glow_protect", 3);
        addPassiveSkillToEntity(testPlayer, glowProtect);

        int spirit = testPlayer.getFinalAttributes().spirit;
        float dodgeBefore = testPlayer.getFinalAttributes().dodgeRate;
        float resistBefore = testPlayer.getFinalAttributes().debuffResist;

        // When - 触发战斗开始
        glowProtect.onBattleStart(testPlayer, battleContext);

        // Then
        int expectedBoost = (int) (spirit * 35 / 1000.0f);
        float dodgeAfter = testPlayer.getFinalAttributes().dodgeRate;
        float resistAfter = testPlayer.getFinalAttributes().debuffResist;

        assertEquals("闪避率应该提升", dodgeBefore + expectedBoost, dodgeAfter, 0.1f);
        assertEquals("异常抵抗率应该提升", resistBefore + expectedBoost, resistAfter, 0.1f);

        printBattleLogs();
    }

    /**
     * 测试等级5：精神 x 千分之70
     */
    @Test
    public void testGlowProtectLevel5() {
        // Given
        PassiveSkill glowProtect = createPassiveSkill("glow_protect", 5);
        addPassiveSkillToEntity(testPlayer, glowProtect);

        int spirit = testPlayer.getFinalAttributes().spirit;
        float dodgeBefore = testPlayer.getFinalAttributes().dodgeRate;
        float resistBefore = testPlayer.getFinalAttributes().debuffResist;

        // When - 触发战斗开始
        glowProtect.onBattleStart(testPlayer, battleContext);

        // Then
        int expectedBoost = (int) (spirit * 70 / 1000.0f);
        float dodgeAfter = testPlayer.getFinalAttributes().dodgeRate;
        float resistAfter = testPlayer.getFinalAttributes().debuffResist;

        assertEquals("闪避率应该提升", dodgeBefore + expectedBoost, dodgeAfter, 0.1f);
        assertEquals("异常抵抗率应该提升", resistBefore + expectedBoost, resistAfter, 0.1f);

        printBattleLogs();
    }

    /**
     * 测试不同精神属性的影响
     */
    @Test
    public void testGlowProtectDifferentSpirit() {
        // Given
        PassiveSkill glowProtect = createPassiveSkill("glow_protect", 1);
        addPassiveSkillToEntity(testPlayer, glowProtect);

        // 设置不同的精神值
        testPlayer.getBaseAttributes().spirit = 20;
        testPlayer.markAttributeCacheDirty();

        int spirit = testPlayer.getFinalAttributes().spirit;
        float dodgeBefore = testPlayer.getFinalAttributes().dodgeRate;

        // When - 触发战斗开始
        glowProtect.onBattleStart(testPlayer, battleContext);

        // Then
        int expectedBoost = (int) (spirit * 15 / 1000.0f);
        float dodgeAfter = testPlayer.getFinalAttributes().dodgeRate;

        assertEquals("闪避率应该基于精神属性计算", dodgeBefore + expectedBoost, dodgeAfter, 0.1f);

        System.out.println("精神：" + spirit);
        System.out.println("预期提升：" + expectedBoost);
        System.out.println("实际闪避率：" + dodgeAfter);

        printBattleLogs();
    }

    /**
     * 测试buff的持久性
     */
    @Test
    public void testGlowProtectBuffPersistence() {
        // Given
        PassiveSkill glowProtect = createPassiveSkill("glow_protect", 1);
        addPassiveSkillToEntity(testPlayer, glowProtect);

        // When - 触发战斗开始
        glowProtect.onBattleStart(testPlayer, battleContext);

        // Then
        float dodgeAfter = testPlayer.getFinalAttributes().dodgeRate;

        // 验证buff存在且持久
        AttributeBuff dodgeBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("glow_protect_dodge"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有闪避buff", dodgeBuff);
        assertEquals("buff应该是永久持续时间", -1, dodgeBuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试同时添加闪避和抵抗buff
     */
    @Test
    public void testGlowProtectBothBuffs() {
        // Given
        PassiveSkill glowProtect = createPassiveSkill("glow_protect", 1);
        addPassiveSkillToEntity(testPlayer, glowProtect);

        // When - 触发战斗开始
        glowProtect.onBattleStart(testPlayer, battleContext);

        // Then
        AttributeBuff dodgeBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("glow_protect_dodge"))
                .findFirst()
                .orElse(null);

        AttributeBuff resistBuff = (AttributeBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().equals("glow_protect_resist"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有闪避buff", dodgeBuff);
        assertNotNull("应该有抵抗buff", resistBuff);

        printBattleLogs();
    }

    /**
     * 测试多次触发不重复添加buff
     */
    @Test
    public void testGlowProtectNoDuplicateBuff() {
        // Given
        PassiveSkill glowProtect = createPassiveSkill("glow_protect", 1);
        addPassiveSkillToEntity(testPlayer, glowProtect);

        // When - 触发两次战斗开始
        glowProtect.onBattleStart(testPlayer, battleContext);

        long buffCount1 = testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().startsWith("glow_protect_"))
                .count();

        glowProtect.onBattleStart(testPlayer, battleContext);

        long buffCount2 = testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof AttributeBuff)
                .filter(buff -> buff.getBuffId().startsWith("glow_protect_"))
                .count();

        // Then - 应该还是只有2个buff（闪避和抵抗）
        assertEquals("第一次触发后应该有2个buff", 2, buffCount1);
        assertEquals("第二次触发后应该还是只有2个buff", 2, buffCount2);

        printBattleLogs();
    }
}
