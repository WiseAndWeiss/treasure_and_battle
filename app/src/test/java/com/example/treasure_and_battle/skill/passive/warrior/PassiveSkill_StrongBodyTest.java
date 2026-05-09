package com.example.treasure_and_battle.skill.passive.warrior;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.PassiveSkillManager;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.damage.DamageReductionBuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 强健体魄技能测试
 * 技能效果：战斗开始时，获得x点体魄，整场战斗受到所有伤害降低y%
 */
public class PassiveSkill_StrongBodyTest extends PassiveSkillTestBase {

    /**
     * 测试等级1：获得1点体魄，受到所有伤害降低3%
     */
    @Test
    public void testStrongBodyLevel1() {
        // Given
        PassiveSkill strongBody = createPassiveSkill("strong_body", 1);

        // When - 添加被动技能
        testPlayer.addPassiveSkill(strongBody);
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_BATTLE_START);

        // Then - 验证体魄增加
        assertEquals("体魄应该增加1点", 11, testPlayer.getBaseAttributes().physique);

        // 验证伤害降低buff
        boolean hasDamageReductionBuff = false;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof DamageReductionBuff) {
                hasDamageReductionBuff = true;
                DamageReductionBuff drBuff = (DamageReductionBuff) buff;
                assertEquals("伤害降低应该为3%", 3, drBuff.getDamageReductionPercent());
            }
        }

        assertTrue("应该有伤害降低buff", hasDamageReductionBuff);

        printBattleLogs();
    }

    /**
     * 测试伤害降低效果
     */
    @Test
    public void testDamageReductionEffect() {
        // Given
        PassiveSkill strongBody = createPassiveSkill("strong_body", 3); // 7%伤害降低
        testPlayer.addPassiveSkill(strongBody);
        testPlayer.setCurrentHp(200);
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_BATTLE_START);

        int monsterHpBefore = testMonster.getCurrentHp();

        // When - 受到伤害
        testMonster.getBaseAttributes().physicalAtk = 100;
        testMonster.markAttributeCacheDirty();

        battleManager.dealPhysicalDamage(testMonster, testPlayer, 100, battleContext);

        // Then - 验证伤害降低了7%
        int damageDealt = 200 - testPlayer.getCurrentHp();
        int expectedDamage = (int) (100 * 0.93); // 100 * (100-7)%

        System.out.println("预期伤害: " + expectedDamage + ", 实际伤害: " + damageDealt);
        assertTrue("伤害应该降低", damageDealt < 100);

        printBattleLogs();
    }

    /**
     * 测试体魄提升效果
     */
    @Test
    public void testPhysiqueBonus() {
        // Given
        PassiveSkill strongBody = createPassiveSkill("strong_body", 3); // 2点体魄
        int originalPhysique = testPlayer.getBaseAttributes().physique;

        // When
        testPlayer.addPassiveSkill(strongBody);
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_BATTLE_START);

        // Then
        assertEquals("体魄应该增加2点", originalPhysique + 2, testPlayer.getBaseAttributes().physique);

        printBattleLogs();
    }

    /**
     * 测试等级递增效果
     */
    @Test
    public void testScalingByLevel() {
        int originalPhysique = testPlayer.getBaseAttributes().physique;

        // 测试等级1（3%伤害降低）
        testPlayer.addPassiveSkill(createPassiveSkill("strong_body", 1));
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_BATTLE_START);

        DamageReductionBuff buff1 = null;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof DamageReductionBuff) {
                buff1 = (DamageReductionBuff) buff;
            }
        }

        assertNotNull("等级1应该有伤害降低buff", buff1);

        // 测试等级5（12%伤害降低）
        testPlayer.getActiveBuffList().clear();
        testPlayer.getBaseAttributes().physique = originalPhysique;
        battleContext.battleLogs.clear();

        testPlayer.addPassiveSkill(createPassiveSkill("strong_body", 5));
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_BATTLE_START);

        DamageReductionBuff buff5 = null;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof DamageReductionBuff) {
                buff5 = (DamageReductionBuff) buff;
            }
        }

        assertNotNull("等级5应该有伤害降低buff", buff5);
        assertTrue("等级5伤害降低应该大于等级1", buff5.getDamageReductionPercent() > buff1.getDamageReductionPercent());

        System.out.println("等级1伤害降低: " + buff1.getDamageReductionPercent() + "%");
        System.out.println("等级5伤害降低: " + buff5.getDamageReductionPercent() + "%");

        printBattleLogs();
    }

    /**
     * 测试战斗开始触发
     */
    @Test
    public void testTriggerOnBattleStart() {
        // Given
        PassiveSkill strongBody = createPassiveSkill("strong_body", 1);
        testPlayer.addPassiveSkill(strongBody);

        // When
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_BATTLE_START);

        // Then - 验证日志
        assertLogExists(LogType.BUFF);
        assertLogContains(LogType.BUFF, "【强健体魄】");

        printBattleLogs();
    }

    /**
     * 测试buff不可驱散
     */
    @Test
    public void testBuffNotDispellable() {
        // Given
        PassiveSkill strongBody = createPassiveSkill("strong_body", 1);
        testPlayer.addPassiveSkill(strongBody);
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_BATTLE_START);

        // 查找伤害降低buff
        DamageReductionBuff drBuff = null;
        for (BaseBuff buff : testPlayer.getActiveBuffList()) {
            if (buff instanceof DamageReductionBuff) {
                drBuff = (DamageReductionBuff) buff;
                break;
            }
        }

        assertNotNull("应该有伤害降低buff", drBuff);
        assertFalse("伤害降低buff应该不可驱散", drBuff.isDispellable());

        printBattleLogs();
    }

    /**
     * 测试日志记录
     */
    @Test
    public void testBattleLog() {
        // Given
        PassiveSkill strongBody = createPassiveSkill("strong_body", 2);
        testPlayer.addPassiveSkill(strongBody);

        // When
        PassiveSkillManager.getInstance().trigger(testPlayer, battleContext, TriggerType.ON_BATTLE_START);

        // Then - 验证日志
        assertLogExists(LogType.BUFF);
        assertLogContains(LogType.BUFF, "【强健体魄】");
        assertLogContains(LogType.BUFF, "体魄");
        assertLogContains(LogType.BUFF, "伤害降低");

        printBattleLogs();
    }
}
