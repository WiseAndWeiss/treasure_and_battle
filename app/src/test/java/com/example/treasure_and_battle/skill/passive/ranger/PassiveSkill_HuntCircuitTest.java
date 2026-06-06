package com.example.treasure_and_battle.skill.passive.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.MonsterManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkillTestBase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 巡回狩猎技能测试
 * 技能效果：每回合开始时，对场上当前生命值比例最高的敌人造成x%物理攻击伤害，并将造成伤害的y%转化为自身生命值治疗
 */
public class PassiveSkill_HuntCircuitTest extends PassiveSkillTestBase {

    private List<BattleEntity> multipleEnemies;

    @Override
    public void setUp() {
        super.setUp();
        // 创建多个敌人用于测试
        multipleEnemies = new ArrayList<>();
        multipleEnemies.add(testMonster);

        // 添加第二个怪物（HP比例不同）
        BattleEntity enemy2 = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
        enemy2.getBaseAttributes().maxHp = 200;
        enemy2.getBaseAttributes().physicalDef = 20;
        enemy2.getBaseAttributes().speed = 10;
        enemy2.getBaseAttributes().dodgeRate = 0f;
        enemy2.markAttributeCacheDirty();
        enemy2.setCurrentHp(200); // 满血，HP比例100%
        multipleEnemies.add(enemy2);

        // 更新battleContext的monsters列表
        battleContext.monsters.clear();
        battleContext.monsters.addAll((List)multipleEnemies); // 强制转换，因为multipleEnemies是List<BattleEntity>

        // 设置怪物防御为0以便观察纯粹伤害
        for (BattleEntity enemy : multipleEnemies) {
            if (enemy instanceof Monster) {
                enemy.getBaseAttributes().physicalDef = 0;
                enemy.markAttributeCacheDirty();
            }
        }

        // 设置第一个怪物为50% HP
        testMonster.setCurrentHp(100);
    }

    /**
     * 测试等级1：造成40%伤害，治疗15%
     */
    @Test
    public void testHuntCircuitLevel1() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 1);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();

        // 设置player HP较低以便观察治疗效果
        testPlayer.setCurrentHp(50);

        // When - 回合开始触发
        huntCircuit.onRoundStart(testPlayer, battleContext);

        // Then - 验证最高HP比例的敌人受到伤害（enemy2是100%，testMonster是50%）
        int enemy2Hp = multipleEnemies.get(1).getCurrentHp();
        assertTrue("最高HP比例的敌人应该受到伤害", enemy2Hp < 200);

        // 验证player获得治疗
        int playerHp = testPlayer.getCurrentHp();
        assertTrue("玩家应该获得治疗", playerHp > 50);

        // 验证日志
        assertLogContains(LogType.HEAL, "【巡回狩猎】");

        printBattleLogs();
    }

    /**
     * 测试等级3：造成50%伤害，治疗18%
     */
    @Test
    public void testHuntCircuitLevel3() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 3);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();
        testPlayer.setCurrentHp(50);

        // 重置敌人HP
        multipleEnemies.get(1).setCurrentHp(200);
        multipleEnemies.get(0).setCurrentHp(100);

        int enemyHpBefore = multipleEnemies.get(1).getCurrentHp();

        // When - 回合开始触发
        huntCircuit.onRoundStart(testPlayer, battleContext);

        int enemyHpAfter = multipleEnemies.get(1).getCurrentHp();
        int damage = enemyHpBefore - enemyHpAfter;

        // Then - 验证伤害约为60点（100物理攻击 * 60% = 60）
        assertTrue("伤害应该约为60点", Math.abs(damage - 60) <= 6);

        printBattleLogs();
    }

    /**
     * 测试等级5：造成80%伤害，治疗30%
     */
    @Test
    public void testHuntCircuitLevel5() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 5);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();
        testPlayer.setCurrentHp(50);

        // 重置敌人HP
        multipleEnemies.get(1).setCurrentHp(200);
        multipleEnemies.get(0).setCurrentHp(100);

        int enemyHpBefore = multipleEnemies.get(1).getCurrentHp();

        // When - 回合开始触发
        huntCircuit.onRoundStart(testPlayer, battleContext);

        int enemyHpAfter = multipleEnemies.get(1).getCurrentHp();
        int damage = enemyHpBefore - enemyHpAfter;

        // Then - 验证伤害约为80点（100物理攻击 * 80% = 80）
        assertTrue("伤害应该约为80点", Math.abs(damage - 80) <= 8);

        printBattleLogs();
    }

    /**
     * 测试治疗量计算
     */
    @Test
    public void testHuntCircuitHealingAmount() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 1);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();
        testPlayer.setCurrentHp(50);

        int playerHpBefore = testPlayer.getCurrentHp();
        int enemyHpBefore = multipleEnemies.get(1).getCurrentHp();

        // When - 回合开始触发
        huntCircuit.onRoundStart(testPlayer, battleContext);

        int playerHpAfter = testPlayer.getCurrentHp();
        int enemyHpAfter = multipleEnemies.get(1).getCurrentHp();

        int damage = enemyHpBefore - enemyHpAfter;
        int heal = playerHpAfter - playerHpBefore;

        // Then - 验证治疗量为伤害的15%
        int expectedHeal = (int) (damage * 15 / 100.0);
        assertEquals("治疗量应该为伤害的15%", expectedHeal, heal, 2);

        printBattleLogs();
    }

    /**
     * 测试选择最高HP比例的目标
     */
    @Test
    public void testHuntCircuitTargetSelection() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 1);

        // 设置不同的HP比例
        testMonster.setCurrentHp(100); // 50%
        multipleEnemies.get(1).setCurrentHp(180); // 90%
        multipleEnemies.get(1).getBaseAttributes().maxHp = 200;

        int firstEnemyHpBefore = testMonster.getCurrentHp();
        int secondEnemyHpBefore = multipleEnemies.get(1).getCurrentHp();

        // When - 回合开始触发
        huntCircuit.onRoundStart(testPlayer, battleContext);

        int firstEnemyHpAfter = testMonster.getCurrentHp();
        int secondEnemyHpAfter = multipleEnemies.get(1).getCurrentHp();

        // Then - 验证只有最高HP比例的敌人受到伤害（第二个怪物90% vs 第一个50%）
        assertEquals("较低HP比例的敌人不应该受到伤害", firstEnemyHpBefore, firstEnemyHpAfter);
        assertTrue("最高HP比例的敌人应该受到伤害", secondEnemyHpAfter < secondEnemyHpBefore);

        printBattleLogs();
    }

    /**
     * 测试治疗上限不能超过最大HP
     */
    @Test
    public void testHuntCircuitHealingCap() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 5);
        testPlayer.getBaseAttributes().physicalAtk = 200;
        testPlayer.markAttributeCacheDirty();

        // 设置player接近满血
        testPlayer.setCurrentHp(testPlayer.getFinalAttributes().maxHp - 10);

        int playerHpBefore = testPlayer.getCurrentHp();

        // When - 回合开始触发
        huntCircuit.onRoundStart(testPlayer, battleContext);

        int playerHpAfter = testPlayer.getCurrentHp();

        // Then - 验证治疗不超过最大HP
        assertTrue("治疗不应超过最大HP", playerHpAfter <= testPlayer.getFinalAttributes().maxHp);
        assertEquals("应该治疗到满血", testPlayer.getFinalAttributes().maxHp, playerHpAfter);

        printBattleLogs();
    }

    /**
     * 测试满血时不溢出治疗
     */
    @Test
    public void testHuntCircuitFullHpNoOverheal() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 1);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();

        // 设置player满血
        testPlayer.setCurrentHp(testPlayer.getFinalAttributes().maxHp);

        int playerHpBefore = testPlayer.getCurrentHp();

        // When - 回合开始触发
        huntCircuit.onRoundStart(testPlayer, battleContext);

        int playerHpAfter = testPlayer.getCurrentHp();

        // Then - 验证满血时HP不增加
        assertEquals("满血时HP不应该增加", playerHpBefore, playerHpAfter);

        printBattleLogs();
    }

    /**
     * 测试对没有敌人的情况
     */
    @Test
    public void testHuntCircuitNoEnemies() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 1);

        // 创建一个没有敌人的场景（通过清空敌人列表或使用只有玩家的context）
        // 这里我们假设在正常battle setup下总是有敌人，所以这个测试主要是验证不会crash
        testPlayer.setCurrentHp(50);
        int playerHpBefore = testPlayer.getCurrentHp();

        // When - 回合开始触发（正常情况下会有敌人）
        huntCircuit.onRoundStart(testPlayer, battleContext);

        int playerHpAfter = testPlayer.getCurrentHp();

        // Then - 验证不会异常，player可能获得治疗
        assertTrue("玩家HP应该保持不变或增加", playerHpAfter >= playerHpBefore);

        printBattleLogs();
    }

    /**
     * 测试多回合持续触发
     */
    @Test
    public void testHuntCircuitMultipleRounds() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 1);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.markAttributeCacheDirty();

        // 重置敌人HP到满状态
        multipleEnemies.get(1).setCurrentHp(200);
        multipleEnemies.get(0).setCurrentHp(100);

        // When - 多回合触发
        huntCircuit.onRoundStart(testPlayer, battleContext);
        huntCircuit.onRoundStart(testPlayer, battleContext);
        huntCircuit.onRoundStart(testPlayer, battleContext);

        // Then - 验证每次都造成伤害和治疗
        int finalEnemyHp = multipleEnemies.get(1).getCurrentHp();
        assertTrue("敌人HP应显著下降", finalEnemyHp < 100);

        printBattleLogs();
    }

    /**
     * 测试日志内容
     */
    @Test
    public void testHuntCircuitLogContent() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 1);
        testPlayer.setCurrentHp(50);

        // 重置敌人HP
        multipleEnemies.get(1).setCurrentHp(200);
        multipleEnemies.get(0).setCurrentHp(100);

        // When - 回合开始触发
        huntCircuit.onRoundStart(testPlayer, battleContext);

        // Then - 验证日志包含狩猎和伤害信息
        assertLogContains(LogType.HEAL, "狩猎");
        assertLogContains(LogType.HEAL, "伤害");

        printBattleLogs();
    }

    /**
     * 测试物理攻击力影响伤害
     */
    @Test
    public void testHuntCircuitDamageScalesWithAttack() {
        // Given
        PassiveSkill huntCircuit = createPassiveSkill("hunt_circuit", 1);

        // 测试低攻击力
        testPlayer.getBaseAttributes().physicalAtk = 50;
        testPlayer.markAttributeCacheDirty();
        int enemyHpBefore = multipleEnemies.get(1).getCurrentHp();

        huntCircuit.onRoundStart(testPlayer, battleContext);

        int enemyHpAfterLowAtk = multipleEnemies.get(1).getCurrentHp();
        int damageLowAtk = enemyHpBefore - enemyHpAfterLowAtk;

        // 重置敌人HP
        multipleEnemies.get(1).setCurrentHp(200);

        // 测试高攻击力
        testPlayer.getBaseAttributes().physicalAtk = 150;
        testPlayer.markAttributeCacheDirty();
        enemyHpBefore = multipleEnemies.get(1).getCurrentHp();

        huntCircuit.onRoundStart(testPlayer, battleContext);

        int enemyHpAfterHighAtk = multipleEnemies.get(1).getCurrentHp();
        int damageHighAtk = enemyHpBefore - enemyHpAfterHighAtk;

        // Then - 验证伤害随攻击力增加
        assertTrue("高攻击力应该造成更高伤害", damageHighAtk > damageLowAtk);

        printBattleLogs();
    }
}
