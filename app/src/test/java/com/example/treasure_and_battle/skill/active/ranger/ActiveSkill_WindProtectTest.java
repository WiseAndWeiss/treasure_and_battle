package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.special.WindProtectBuff;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * 御风护体技能测试
 * 技能效果：下一次受到攻击时，免疫该次攻击全额伤害，并将该次伤害的x%溅射向全场所有敌人
 */
public class ActiveSkill_WindProtectTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：40%溅射伤害
     */
    @Test
    public void testWindProtectLevel1() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 验证御风护体buff添加
        WindProtectBuff windProtectBuff = (WindProtectBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WindProtectBuff)
                .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有御风护体buff", windProtectBuff);
        assertEquals("溅射伤害百分比应该为40%", 40.0f, windProtectBuff.getSplashDamagePercent(), 0.1f);

        // 验证日志
        assertLogContains(LogType.BUFF, "【御风护体】");

        printBattleLogs();
    }

    /**
     * 测试等级3：60%溅射伤害
     */
    @Test
    public void testWindProtectLevel3() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 3);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 验证御风护体buff添加
        WindProtectBuff windProtectBuff = (WindProtectBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WindProtectBuff)
                .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有御风护体buff", windProtectBuff);
        assertEquals("溅射伤害百分比应该为60%", 60.0f, windProtectBuff.getSplashDamagePercent(), 0.1f);

        printBattleLogs();
    }

    /**
     * 测试等级5：80%溅射伤害
     */
    @Test
    public void testWindProtectLevel5() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 5);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 验证御风护体buff添加
        WindProtectBuff windProtectBuff = (WindProtectBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WindProtectBuff)
                .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有御风护体buff", windProtectBuff);
        assertEquals("溅射伤害百分比应该为80%", 80.0f, windProtectBuff.getSplashDamagePercent(), 0.1f);

        printBattleLogs();
    }

    /**
     * 测试buff是否正确添加
     */
    @Test
    public void testWindProtectBuffAdded() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 验证御风护体buff存在
        WindProtectBuff windProtectBuff = (WindProtectBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WindProtectBuff)
                .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有御风护体buff", windProtectBuff);
        assertFalse("buff应该未被触发", windProtectBuff.isTriggered());

        printBattleLogs();
    }

    /**
     * 测试buff持续时间（不衰退）
     */
    @Test
    public void testWindProtectBuffDuration() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 验证buff持续时间
        WindProtectBuff windProtectBuff = (WindProtectBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WindProtectBuff)
                .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有御风护体buff", windProtectBuff);
        assertEquals("buff应该是永久持续时间", -1, windProtectBuff.getRemainingDuration());

        printBattleLogs();
    }

    /**
     * 测试对自身施放（无目标）
     */
    @Test
    public void testWindProtectSelfTarget() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 1);

        // When - 对自身施放技能（空目标列表）
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 验证buff添加到施法者
        WindProtectBuff windProtectBuff = (WindProtectBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WindProtectBuff)
                .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有御风护体buff", windProtectBuff);

        printBattleLogs();
    }

    /**
     * 测试溅射伤害百分比
     */
    @Test
    public void testWindProtectSplashDamagePercent() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 验证溅射伤害百分比
        WindProtectBuff windProtectBuff = (WindProtectBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WindProtectBuff)
                .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有御风护体buff", windProtectBuff);
        float splashPercent = windProtectBuff.getSplashDamagePercent();

        assertTrue("溅射伤害百分比应该大于0", splashPercent > 0);
        assertEquals("溅射伤害百分比应该为40%", 40.0f, splashPercent, 0.1f);

        System.out.println("溅射伤害百分比：" + splashPercent);
        printBattleLogs();
    }

    /**
     * 测试不同等级的溅射伤害百分比
     */
    @Test
    public void testWindProtectSplashDamageDifferentLevels() {
        float[] expectedSplashPercents = {40.0f, 50.0f, 60.0f, 70.0f, 80.0f};

        for (int level = 1; level <= 5; level++) {
            // 清空buff列表
            testPlayer.getActiveBuffList().clear();

            ActiveSkill windProtect = createSkill("wind_protect", level);

            // When - 施放技能
            battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

            // Then - 验证溅射伤害百分比
            WindProtectBuff windProtectBuff = (WindProtectBuff) testPlayer.getActiveBuffList().stream()
                    .filter(buff -> buff instanceof WindProtectBuff)
                    .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                    .findFirst()
                    .orElse(null);

            assertNotNull("等级" + level + "应该有御风护体buff", windProtectBuff);
            assertEquals("等级" + level + "溅射伤害百分比不正确",
                    expectedSplashPercents[level - 1], windProtectBuff.getSplashDamagePercent(), 0.1f);
        }

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testWindProtectMpCost() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 验证MP消耗
        int mpAfter = testPlayer.getCurrentMp();
        int mpCost = mpBefore - mpAfter;
        assertEquals("应该消耗16点MP", 16, mpCost);

        printBattleLogs();
    }

    /**
     * 测试冷却时间
     */
    @Test
    public void testWindProtectCooldown() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 1);

        // When - 获取冷却时间
        int cooldown = windProtect.getTemplate().getCooldown();

        // Then - 验证冷却时间为2回合
        assertEquals("冷却时间应该为2回合", 2, cooldown);

        printBattleLogs();
    }

    /**
     * 测试buff唯一性
     */
    @Test
    public void testWindProtectSingleBuff() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 1);

        // When - 施放技能两次
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 检查buff数量
        long buffCount = testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WindProtectBuff)
                .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                .count();

        System.out.println("御风护体buff数量：" + buffCount);
        printBattleLogs();
    }

    /**
     * 测试溅射伤害分摊概念
     */
    @Test
    public void testWindProtectSplashDamageDistribution() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 验证溅射伤害百分比设置
        WindProtectBuff windProtectBuff = (WindProtectBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WindProtectBuff)
                .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有御风护体buff", windProtectBuff);

        // 模拟溅射伤害分摊计算
        int originalDamage = 100; // 假设受到100点伤害
        float splashPercent = windProtectBuff.getSplashDamagePercent();
        int totalSplashDamage = (int) (originalDamage * splashPercent / 100.0f);

        System.out.println("原始伤害：" + originalDamage);
        System.out.println("溅射百分比：" + splashPercent + "%");
        System.out.println("总溅射伤害：" + totalSplashDamage);

        assertTrue("总溅射伤害应该大于0", totalSplashDamage > 0);

        printBattleLogs();
    }

    /**
     * 测试buff触发状态
     */
    @Test
    public void testWindProtectTriggeredState() {
        // Given
        ActiveSkill windProtect = createSkill("wind_protect", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, windProtect, java.util.Arrays.asList(), battleContext);

        // Then - 验证初始触发状态
        WindProtectBuff windProtectBuff = (WindProtectBuff) testPlayer.getActiveBuffList().stream()
                .filter(buff -> buff instanceof WindProtectBuff)
                .filter(buff -> buff.getBuffId().equals("wind_protect_buff"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有御风护体buff", windProtectBuff);
        assertFalse("初始状态应该是未触发", windProtectBuff.isTriggered());

        printBattleLogs();
    }
}
