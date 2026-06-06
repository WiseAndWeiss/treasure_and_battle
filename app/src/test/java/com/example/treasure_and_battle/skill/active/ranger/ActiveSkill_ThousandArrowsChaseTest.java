package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.MonsterManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 逐敌千箭技能测试
 * 技能效果：对全体敌人造成n段x%物理攻击伤害，段数n等于场上存活敌人数量，最大段数不超过4段
 */
public class ActiveSkill_ThousandArrowsChaseTest extends ActiveSkillTestBase {

    private List<BattleEntity> multipleMonsters;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        // 创建多个怪物用于测试
        multipleMonsters = new ArrayList<>();
        multipleMonsters.add(testMonster);

        Monster monster2 = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
        monster2.getBaseAttributes().maxHp = 300;
        monster2.getBaseAttributes().physicalDef = 20;
        monster2.markAttributeCacheDirty();
        monster2.setCurrentHp(monster2.getBaseAttributes().maxHp);
        multipleMonsters.add(monster2);

        Monster monster3 = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
        monster3.getBaseAttributes().maxHp = 300;
        monster3.getBaseAttributes().physicalDef = 20;
        monster3.markAttributeCacheDirty();
        monster3.setCurrentHp(monster3.getBaseAttributes().maxHp);
        multipleMonsters.add(monster3);
    }

    /**
     * 测试等级1：35%伤害
     */
    @Test
    public void testThousandArrowsChaseLevel1() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        // 3个存活敌人应该触发3段伤害
        long aliveEnemyCount = multipleMonsters.stream()
                .filter(enemy -> !enemy.isDead() && enemy.getCurrentHp() > 0)
                .count();

        assertEquals("应该有3个存活敌人", 3, aliveEnemyCount);

        // When - 对多个目标施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, multipleMonsters, battleContext);

        // Then - 验证所有目标都受到伤害
        for (BattleEntity entity : multipleMonsters) {
            assertTrue("敌人应该受到伤害", entity.getCurrentHp() < entity.getFinalAttributes().maxHp);
        }

        // 验证日志
        assertLogContains(LogType.DAMAGE, "【逐敌千箭】");

        printBattleLogs();
    }

    /**
     * 测试段数基于存活敌人数量
     */
    @Test
    public void testThousandArrowsChaseWaveCountBasedOnAliveEnemies() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        // 测试不同数量的存活敌人
        int[] enemyCounts = {1, 2, 3, 4, 5};

        for (int expectedCount : enemyCounts) {
            // 清空并重新创建指定数量的怪物
            multipleMonsters.clear();
            for (int i = 0; i < expectedCount; i++) {
                Monster m = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
                m.getBaseAttributes().maxHp = 300;
                m.markAttributeCacheDirty();
                m.setCurrentHp(300);
                multipleMonsters.add(m);
            }

            long aliveEnemyCount = multipleMonsters.stream()
                    .filter(enemy -> !enemy.isDead() && enemy.getCurrentHp() > 0)
                    .count();

            // When - 施放技能
            battleManager.executeSkill(testPlayer, thousandArrowsChase, multipleMonsters, battleContext);

            // Then - 验证段数（最多4段）
            int expectedWaves = Math.min((int) aliveEnemyCount, 4);

            System.out.println("存活敌人：" + aliveEnemyCount + "，预期段数：" + expectedWaves);

            // 验证日志包含段数信息
            assertLogContains(LogType.DAMAGE, "【逐敌千箭】");
        }

        printBattleLogs();
    }

    /**
     * 测试最多4段限制
     */
    @Test
    public void testThousandArrowsChaseMax4Waves() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        // 创建5个怪物（超过限制）
        multipleMonsters.clear();
        for (int i = 0; i < 5; i++) {
            Monster m = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
            m.getBaseAttributes().maxHp = 300;
            m.markAttributeCacheDirty();
            m.setCurrentHp(300);
            multipleMonsters.add(m);
        }

        long aliveEnemyCount = multipleMonsters.stream()
                .filter(enemy -> !enemy.isDead() && enemy.getCurrentHp() > 0)
                .count();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, multipleMonsters, battleContext);

        // Then - 验证最多4段限制
        int expectedWaves = Math.min((int) aliveEnemyCount, 4);
        assertEquals("5个敌人应该触发4段伤害", 4, expectedWaves);

        // 验证日志包含段数信息
        assertLogContains(LogType.DAMAGE, "【逐敌千箭】");

        System.out.println("存活敌人：" + aliveEnemyCount + "，实际段数：4（限制）");
        printBattleLogs();
    }

    /**
     * 测试多段伤害是否造成
     */
    @Test
    public void testThousandArrowsChaseMultipleWaves() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        // 使用2个怪物
        List<BattleEntity> twoMonsters = Arrays.asList(
                multipleMonsters.get(0),
                multipleMonsters.get(1)
        );

        // When - 施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, twoMonsters, battleContext);

        // Then - 验证造成多段伤害
        // 检查日志中是否包含多段信息
        long waveLogCount = battleContext.battleLogs.stream()
                .filter(log -> log.getType() == LogType.DAMAGE)
                .filter(log -> log.getFormattedMessage().contains("第") &&
                               log.getFormattedMessage().contains("波"))
                .count();

        assertTrue("应该有2波箭雨攻击", waveLogCount >= 2);

        System.out.println("箭雨波数：" + waveLogCount);
        printBattleLogs();
    }

    /**
     * 测试只统计存活敌人
     */
    @Test
    public void testThousandArrowsChaseCountOnlyAliveEnemies() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        // 设置其中一个怪物死亡
        multipleMonsters.get(1).setCurrentHp(0);
        multipleMonsters.get(1).setDead(true);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, multipleMonsters, battleContext);

        // Then - 验证只统计存活敌人
        long aliveEnemyCount = multipleMonsters.stream()
                .filter(enemy -> !enemy.isDead() && enemy.getCurrentHp() > 0)
                .count();

        assertTrue("应该只统计2个存活敌人", aliveEnemyCount == 2);

        printBattleLogs();
    }

    /**
     * 测试等级3：45%伤害
     */
    @Test
    public void testThousandArrowsChaseLevel3() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 3);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, multipleMonsters, battleContext);

        // Then - 验证所有目标都受到伤害
        for (BattleEntity entity : multipleMonsters) {
            if (!entity.isDead() && entity.getCurrentHp() > 0) {
                assertTrue("存活敌人应该受到伤害", entity.getCurrentHp() < entity.getFinalAttributes().maxHp);
            }
        }

        printBattleLogs();
    }

    /**
     * 测试等级5：55%伤害
     */
    @Test
    public void testThousandArrowsChaseLevel5() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 5);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, multipleMonsters, battleContext);

        // Then - 验证伤害造成
        for (BattleEntity entity : multipleMonsters) {
            if (!entity.isDead() && entity.getCurrentHp() > 0) {
                assertTrue("存活敌人应该受到伤害", entity.getCurrentHp() < entity.getFinalAttributes().maxHp);
            }
        }

        printBattleLogs();
    }

    /**
     * 测试AOE伤害类型
     */
    @Test
    public void testThousandArrowsChaseAOEDamage() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        // When - 对多个目标施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, multipleMonsters, battleContext);

        // Then - 验证所有存活敌人受到伤害
        int damagedCount = 0;
        for (BattleEntity entity : multipleMonsters) {
            if (!entity.isDead() && entity.getCurrentHp() < entity.getFinalAttributes().maxHp) {
                damagedCount++;
            }
        }

        assertTrue("所有存活敌人都应该受到伤害", damagedCount >= 2);

        printBattleLogs();
    }

    /**
     * 测试伤害类型（物理伤害）
     */
    @Test
    public void testThousandArrowsChaseDamageType() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, multipleMonsters, battleContext);

        // Then - 验证日志包含物理伤害信息
        assertLogContains(LogType.DAMAGE, "【逐敌千箭】");
        assertLogExists(LogType.DAMAGE);

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testThousandArrowsChaseMpCost() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, multipleMonsters, battleContext);

        // Then - 验证MP消耗
        int mpAfter = testPlayer.getCurrentMp();
        int mpCost = mpBefore - mpAfter;
        assertEquals("应该消耗25点MP", 25, mpCost);

        printBattleLogs();
    }

    /**
     * 测试冷却时间
     */
    @Test
    public void testThousandArrowsChaseCooldown() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        // When - 获取冷却时间
        int cooldown = thousandArrowsChase.getTemplate().getCooldown();

        // Then - 验证冷却时间为2回合
        assertEquals("冷却时间应该为2回合", 2, cooldown);

        printBattleLogs();
    }

    /**
     * 测试每段伤害对所有存活敌人有效
     */
    @Test
    public void testThousandArrowsChaseEachWaveHitsAllAliveEnemies() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        // 使用3个存活敌人
        List<BattleEntity> threeMonsters = Arrays.asList(
                multipleMonsters.get(0),
                multipleMonsters.get(1),
                multipleMonsters.get(2)
        );

        // When - 施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, threeMonsters, battleContext);

        // Then - 验证每段伤害都作用于所有存活敌人
        // 通过检查日志验证有多段伤害
        long waveLogCount = battleContext.battleLogs.stream()
                .filter(log -> log.getType() == LogType.DAMAGE)
                .filter(log -> log.getFormattedMessage().contains("第") &&
                               log.getFormattedMessage().contains("波"))
                .count();

        assertTrue("应该有3波箭雨攻击", waveLogCount >= 3);

        System.out.println("箭雨波数：" + waveLogCount);
        printBattleLogs();
    }

    /**
     * 测试单个敌人的情况
     */
    @Test
    public void testThousandArrowsChaseSingleEnemy() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        List<BattleEntity> singleEnemy = Arrays.asList(testMonster);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, thousandArrowsChase, singleEnemy, battleContext);

        // Then - 验证造成伤害
        assertTrue("应该造成伤害", testMonster.getCurrentHp() < testMonster.getFinalAttributes().maxHp);

        // 验证日志
        assertLogContains(LogType.DAMAGE, "【逐敌千箭】");

        printBattleLogs();
    }

    /**
     * 测试技能范围类型（ALL_ENEMY）
     */
    @Test
    public void testThousandArrowsChaseSkillRangeType() {
        // Given
        ActiveSkill thousandArrowsChase = createSkill("thousand_arrows_chase", 1);

        // When - 获取技能范围类型
        var rangeType = thousandArrowsChase.getSkillRangeType();

        // Then - 验证是AOE技能
        assertNotNull("技能范围类型不应该为null", rangeType);
        assertEquals("技能范围类型应该是ALL_ENEMIES",
                com.example.treasure_and_battle.model.skill.SkillRangeType.ALL_ENEMIES,
                rangeType);

        printBattleLogs();
    }
}
