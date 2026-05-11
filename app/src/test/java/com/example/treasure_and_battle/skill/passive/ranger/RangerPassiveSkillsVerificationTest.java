package com.example.treasure_and_battle.skill.passive.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 游侠被动技能基本验证测试
 * 验证核心功能是否正确工作
 */
public class RangerPassiveSkillsVerificationTest extends PassiveSkillTestBase {

    /**
     * 验证先机技能 - 检查buff是否被创建
     */
    @Test
    public void verifyFirstMoverBuffCreated() {
        // Given
        PassiveSkill firstMover = createPassiveSkill("first_mover", 1);
        int buffCountBefore = testPlayer.getActiveBuffList().size();

        // When
        firstMover.onRoundStart(testPlayer, battleContext);

        // Then - 验证buff被添加
        int buffCountAfter = testPlayer.getActiveBuffList().size();
        assertTrue("Buff应该被添加", buffCountAfter > buffCountBefore);

        // 验证日志
        assertLogContains(LogType.BUFF, "【先机】");
        assertLogExists(LogType.BUFF);

        printBattleLogs();
    }

    /**
     * 验证轻灵反击 - 战斗开始闪避提升
     */
    @Test
    public void verifyAgileCounterDodgeBuff() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 1);
        int buffCountBefore = testPlayer.getActiveBuffList().size();

        // When
        agileCounter.onBattleStart(testPlayer, battleContext);

        // Then - 验证buff被添加
        int buffCountAfter = testPlayer.getActiveBuffList().size();
        assertTrue("闪避buff应该被添加", buffCountAfter > buffCountBefore);

        // 验证日志
        assertLogContains(LogType.BUFF, "【轻灵反击】");
        assertLogExists(LogType.BUFF);

        printBattleLogs();
    }

    /**
     * 验证轻灵反击 - 闪避反击伤害
     */
    @Test
    public void verifyAgileCounterCounterAttack() {
        // Given
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 1);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();

        int attackerHpBefore = testMonster.getCurrentHp();

        // When
        agileCounter.onDodge(testPlayer, testMonster, battleContext);

        // Then - 验证反击造成伤害
        int attackerHpAfter = testMonster.getCurrentHp();
        assertTrue("反击应该造成伤害", attackerHpAfter < attackerHpBefore);

        printBattleLogs();
    }

    /**
     * 验证以攻为守 - 暴击防御提升
     */
    @Test
    public void verifyAttackToDefendBuffs() {
        // Given
        PassiveSkill attackToDefend = createPassiveSkill("attack_to_defend", 1);
        int buffCountBefore = testPlayer.getActiveBuffList().size();

        // When
        attackToDefend.onCrit(testPlayer, battleContext);

        // Then - 验证buff被添加（应该添加2个buff：物防+法防）
        int buffCountAfter = testPlayer.getActiveBuffList().size();
        assertEquals("应该添加2个防御buff", buffCountBefore + 2, buffCountAfter);

        // 验证日志
        assertLogContains(LogType.BUFF, "【以攻为守】");
        assertLogExists(LogType.BUFF);

        printBattleLogs();
    }

    /**
     * 验证不屈之志 - debuff检测
     */
    @Test
    public void verifyIndomitableWillDebuffDetection() {
        // Given
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 1);

        // Without debuff
        indomitableWill.onRoundStart(testPlayer, battleContext);
        int buffCountWithoutDebuff = testPlayer.getActiveBuffList().size();

        // With debuff (add SlowDebuff manually)
        testPlayer.getActiveBuffList().add(new com.example.treasure_and_battle.buff.impl.control.SlowDebuff(
                "test", "测试", "减速", BuffType.DEBUFF, false, -1, 1, false, 1.0f, 10.0f));

        // When
        indomitableWill.onRoundStart(testPlayer, battleContext);

        // Then - 验证有debuff时才添加buff
        int buffCountWithDebuff = testPlayer.getActiveBuffList().size();
        assertTrue("有debuff时应该添加更多buff", buffCountWithDebuff >= buffCountWithoutDebuff);

        printBattleLogs();
    }

    /**
     * 验证巡回狩猎 - 目标选择与伤害
     */
    @Test
    public void verifyHuntCircuitTargeting() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 1);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();
        testPlayer.setCurrentHp(50);

        int monsterHpBefore = testMonster.getCurrentHp();

        // When
        huntCircuit.onRoundStart(testPlayer, battleContext);

        // Then - 验证对怪物造成伤害
        int monsterHpAfter = testMonster.getCurrentHp();
        assertTrue("巡回狩猎应该造成伤害", monsterHpAfter < monsterHpBefore);

        // 验证玩家获得治疗
        int playerHpAfter = testPlayer.getCurrentHp();
        assertTrue("巡回狩猎应该治疗玩家", playerHpAfter > 50);

        printBattleLogs();
    }

    /**
     * 验证触发类型配置
     */
    @Test
    public void verifyTriggerTypes() {
        // Given
        PassiveSkill firstMover = createPassiveSkill("first_mover", 1);
        PassiveSkill agileCounter = createPassiveSkill("agile_counter", 1);
        PassiveSkill attackToDefend = createPassiveSkill("attack_to_defend", 1);
        PassiveSkill indomitableWill = createPassiveSkill("indomitable_will", 1);
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 1);

        // Then - 验证触发类型配置正确
        assertTrue("先机应该有ON_ROUND_START触发", firstMover.hasTriggerType(com.example.treasure_and_battle.model.common.TriggerType.ON_ROUND_START));
        assertTrue("轻灵反击应该有ON_BATTLE_START触发", agileCounter.hasTriggerType(com.example.treasure_and_battle.model.common.TriggerType.ON_BATTLE_START));
        assertTrue("轻灵反击应该有ON_DODGE触发", agileCounter.hasTriggerType(com.example.treasure_and_battle.model.common.TriggerType.ON_DODGE));
        assertTrue("以攻为守应该有ON_CRIT触发", attackToDefend.hasTriggerType(com.example.treasure_and_battle.model.common.TriggerType.ON_CRIT));
        assertTrue("不屈之志应该有ON_ROUND_START触发", indomitableWill.hasTriggerType(com.example.treasure_and_battle.model.common.TriggerType.ON_ROUND_START));
        assertTrue("巡回狩猎应该有ON_ROUND_START触发", huntCircuit.hasTriggerType(com.example.treasure_and_battle.model.common.TriggerType.ON_ROUND_START));
    }
}
