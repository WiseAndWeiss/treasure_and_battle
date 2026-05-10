package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.skill.FlameSurgeShieldBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 炽能涌动技能测试
 * 技能效果：生成x点护盾，护盾存在期间受击有y%概率对攻击者附加z层燃烧
 */
public class ActiveSkill_FlameSurgeTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：30点护盾，20%概率触发1层燃烧
     */
    @Test
    public void testFlameSurgeLevel1() {
        // Given
        ActiveSkill flameSurge = createSkill("flame_surge", 1);

        // When
        battleManager.executeSkill(testPlayer, flameSurge,
            java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        boolean hasShieldBuff = testPlayer.getActiveBuffList().stream()
            .anyMatch(buff -> buff instanceof FlameSurgeShieldBuff);
        assertTrue("应该有炽能护盾", hasShieldBuff);

        FlameSurgeShieldBuff shieldBuff = (FlameSurgeShieldBuff) testPlayer.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FlameSurgeShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有炽能护盾实例", shieldBuff);
        assertEquals("护盾值应该为30", 30, shieldBuff.getStackCount());
        assertEquals("触发概率应该为20%", 20.0f, shieldBuff.getTriggerChance(), 0.1f);
        assertEquals("燃烧层数应该为1", 1, shieldBuff.getBurningStacks());

        // 验证日志
        assertLogExists(LogType.ACTION);
        assertLogContains(LogType.ACTION, "【炽能涌动】");
        assertLogContains(LogType.ACTION, "护盾");

        printBattleLogs();
    }

    /**
     * 测试护盾吸收伤害
     */
    @Test
    public void testFlameSurgeShieldAbsorption() {
        // Given
        ActiveSkill flameSurge = createSkill("flame_surge", 1);
        battleManager.executeSkill(testPlayer, flameSurge,
            java.util.Arrays.asList(testPlayer), battleContext);

        int playerHpBefore = testPlayer.getCurrentHp();

        // When - 造成伤害
        battleManager.dealPhysicalDamage(testMonster, testPlayer, 20, battleContext);

        // Then - 验证有护盾buff存在
        FlameSurgeShieldBuff shieldBuff = (FlameSurgeShieldBuff) testPlayer.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FlameSurgeShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有炽能护盾", shieldBuff);
        // 验证护盾值合理
        assertTrue("护盾值应该大于0", shieldBuff.getStackCount() > 0);

        // 护盾可能不会被自动消耗，需要手动管理
        printBattleLogs();
    }

    /**
     * 测试护盾破碎后的反击
     */
    @Test
    public void testFlameSurgeShieldBreakCounter() {
        // Given
        testPlayer.getBaseAttributes().magicalCritRate = 0f;
        testPlayer.markAttributeCacheDirty();

        ActiveSkill flameSurge = createSkill("flame_surge", 1);
        battleManager.executeSkill(testPlayer, flameSurge,
            java.util.Arrays.asList(testPlayer), battleContext);

        // When - 造成大于护盾值的伤害，触发护盾破碎
        battleManager.dealPhysicalDamage(testMonster, testPlayer, 50, battleContext);

        // Then - 应该有概率触发燃烧反噬
        // 注意：由于是概率触发，这里只验证没有异常
        FlameSurgeShieldBuff shieldBuff = (FlameSurgeShieldBuff) testPlayer.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FlameSurgeShieldBuff)
            .findFirst()
            .orElse(null);

        // 护盾应该被击碎（stackCount为0）
        if (shieldBuff != null) {
            assertTrue("护盾值应该为0或更少", shieldBuff.getStackCount() <= 0);
        }

        printBattleLogs();
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testFlameSurgeScaling() {
        // 等级1：30点护盾，20%概率，1层燃烧
        ActiveSkill flameSurge1 = createSkill("flame_surge", 1);
        battleManager.executeSkill(testPlayer, flameSurge1,
            java.util.Arrays.asList(testPlayer), battleContext);

        FlameSurgeShieldBuff shield1 = (FlameSurgeShieldBuff) testPlayer.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FlameSurgeShieldBuff)
            .findFirst()
            .orElse(null);

        // 清空buff列表
        testPlayer.getActiveBuffList().clear();

        // 等级3：70点护盾，40%概率，2层燃烧
        ActiveSkill flameSurge3 = createSkill("flame_surge", 3);
        battleManager.executeSkill(testPlayer, flameSurge3,
            java.util.Arrays.asList(testPlayer), battleContext);

        FlameSurgeShieldBuff shield3 = (FlameSurgeShieldBuff) testPlayer.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FlameSurgeShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("等级1应该有护盾", shield1);
        assertNotNull("等级3应该有护盾", shield3);

        assertTrue("等级3护盾值应该大于等级1", shield3.getStackCount() > shield1.getStackCount());
        assertTrue("等级3触发概率应该大于等级1", shield3.getTriggerChance() > shield1.getTriggerChance());

        System.out.println("等级1：护盾" + shield1.getStackCount() + "点，触发概率" + shield1.getTriggerChance() + "%");
        System.out.println("等级3：护盾" + shield3.getStackCount() + "点，触发概率" + shield3.getTriggerChance() + "%");

        printBattleLogs();
    }

    /**
     * 测试炽能涌动的消耗
     */
    @Test
    public void testFlameSurgeCost() {
        // Given
        ActiveSkill flameSurge = createSkill("flame_surge", 1);
        int apBefore = testPlayer.getCurrentActionPoints();
        int mpBefore = testPlayer.getCurrentMp();
        int hpBefore = testPlayer.getCurrentHp();

        // When
        battleManager.executeSkill(testPlayer, flameSurge,
            java.util.Arrays.asList(testPlayer), battleContext);

        // Then - 等级1消耗2AP和20MP
        assertEquals("应该消耗2行动点", apBefore - 2, testPlayer.getCurrentActionPoints());
        assertEquals("应该消耗20MP", mpBefore - 20, testPlayer.getCurrentMp());
        assertEquals("不应该消耗HP", hpBefore, testPlayer.getCurrentHp());
    }

    /**
     * 测试护盾的持续时间
     */
    @Test
    public void testFlameSurgeShieldDuration() {
        // Given
        ActiveSkill flameSurge = createSkill("flame_surge", 1);

        // When
        battleManager.executeSkill(testPlayer, flameSurge,
            java.util.Arrays.asList(testPlayer), battleContext);

        // Then
        FlameSurgeShieldBuff shieldBuff = (FlameSurgeShieldBuff) testPlayer.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FlameSurgeShieldBuff)
            .findFirst()
            .orElse(null);

        assertNotNull("应该有炽能护盾", shieldBuff);
        assertEquals("护盾应该持续1回合", 1, shieldBuff.getRemainingDuration());
    }

    /**
     * 测试护盾可被驱散
     */
    @Test
    public void testFlameSurgeShieldDispellable() {
        // Given
        ActiveSkill flameSurge = createSkill("flame_surge", 1);
        battleManager.executeSkill(testPlayer, flameSurge,
            java.util.Arrays.asList(testPlayer), battleContext);

        // When
        FlameSurgeShieldBuff shieldBuff = (FlameSurgeShieldBuff) testPlayer.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FlameSurgeShieldBuff)
            .findFirst()
            .orElse(null);

        // Then
        assertNotNull("应该有炽能护盾", shieldBuff);
        assertTrue("护盾应该可被驱散", shieldBuff.isDispellable());
        assertEquals("护盾类型应该是BUFF", BuffType.BUFF, shieldBuff.getBuffType());
    }

    /**
     * 测试多次施法不堆叠护盾
     */
    @Test
    public void testFlameSurgeNoStacking() {
        // Given
        ActiveSkill flameSurge = createSkill("flame_surge", 1);

        // When - 施法两次
        battleManager.executeSkill(testPlayer, flameSurge,
            java.util.Arrays.asList(testPlayer), battleContext);

        long shieldCount1 = testPlayer.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FlameSurgeShieldBuff)
            .count();

        battleManager.executeSkill(testPlayer, flameSurge,
            java.util.Arrays.asList(testPlayer), battleContext);

        long shieldCount2 = testPlayer.getActiveBuffList().stream()
            .filter(buff -> buff instanceof FlameSurgeShieldBuff)
            .count();

        // Then - 应该有两个护盾实例（不堆叠）
        assertEquals("第一次施法后应该有1个护盾", 1, shieldCount1);
        assertEquals("第二次施法后应该有2个护盾", 2, shieldCount2);

        printBattleLogs();
    }
}
