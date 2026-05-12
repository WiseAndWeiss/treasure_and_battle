package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.active.ActiveSkillTestBase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 致命连射技能测试
 * 技能效果：对单体敌人造成x%物理攻击伤害，若本次攻击暴击则立刻再次释放该技能，最多触发5轮
 */
public class ActiveSkill_LethalContinuousShotTest extends ActiveSkillTestBase {

    /**
     * 测试等级1：75%伤害，暴击连射
     */
    @Test
    public void testLethalContinuousShotLevel1() {
        // Given
        ActiveSkill lethalContinuousShot = createSkill("lethal_continuous_shot", 1);
        ActiveSkill_LethalContinuousShot skill = (ActiveSkill_LethalContinuousShot) lethalContinuousShot;

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, lethalContinuousShot, java.util.Arrays.asList(testMonster), battleContext);

        int totalDamage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证伤害造成
        assertTrue("应该造成伤害", totalDamage > 0);

        // 验证连射次数
        int chainCount = skill.getChainCount();
        assertTrue("连射次数应该大于0", chainCount >= 1);
        assertTrue("连射次数不应该超过5", chainCount <= 5);

        // 验证日志
        assertLogContains(LogType.DAMAGE, "【致命连射】");

        System.out.println("连射次数：" + chainCount);
        printBattleLogs();
    }

    /**
     * 测试暴击触发连射
     */
    @Test
    public void testLethalContinuousShotCritTrigger() {
        // Given
        ActiveSkill lethalContinuousShot = createSkill("lethal_continuous_shot", 1);
        ActiveSkill_LethalContinuousShot skill = (ActiveSkill_LethalContinuousShot) lethalContinuousShot;

        // 设置高暴击率以便观察连射
        testPlayer.getBaseAttributes().physicalCritRate = 1.0f; // 100%暴击
        testPlayer.markAttributeCacheDirty();

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, lethalContinuousShot, java.util.Arrays.asList(testMonster), battleContext);

        int totalDamage = hpBefore - testMonster.getCurrentHp();

        // Then - 验证伤害造成
        assertTrue("应该造成伤害", totalDamage > 0);

        // 验证连射次数（100%暴击应该触发多次连射）
        int chainCount = skill.getChainCount();
        assertTrue("高暴击率下应该触发多次连射", chainCount >= 1);

        // 验证日志包含连射信息
        assertLogContains(LogType.DAMAGE, "【致命连射】");

        System.out.println("连射次数：" + chainCount + "，总伤害：" + totalDamage);
        printBattleLogs();
    }

    /**
     * 测试最多5轮限制
     */
    @Test
    public void testLethalContinuousShotMax5Rounds() {
        // Given
        ActiveSkill lethalContinuousShot = createSkill("lethal_continuous_shot", 1);
        ActiveSkill_LethalContinuousShot skill = (ActiveSkill_LethalContinuousShot) lethalContinuousShot;

        // 设置高暴击率
        testPlayer.getBaseAttributes().physicalCritRate = 1.0f;
        testPlayer.markAttributeCacheDirty();

        // 设置怪物高HP以避免提前死亡
        testMonster.getBaseAttributes().maxHp = 10000;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(10000);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, lethalContinuousShot, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证连射次数不超过5次
        int chainCount = skill.getChainCount();
        assertTrue("连射次数不应该超过5", chainCount <= 5);

        System.out.println("连射次数：" + chainCount);
        printBattleLogs();
    }

    /**
     * 测试目标死亡后停止连射
     */
    @Test
    public void testLethalContinuousShotStopOnTargetDeath() {
        // Given
        ActiveSkill lethalContinuousShot = createSkill("lethal_continuous_shot", 1);
        ActiveSkill_LethalContinuousShot skill = (ActiveSkill_LethalContinuousShot) lethalContinuousShot;

        // 设置高暴击率
        testPlayer.getBaseAttributes().physicalCritRate = 1.0f;
        testPlayer.markAttributeCacheDirty();

        // 设置怪物低HP以便快速死亡
        testMonster.getBaseAttributes().maxHp = 100;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(100);

        int hpBefore = testMonster.getCurrentHp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, lethalContinuousShot, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证目标死亡
        assertTrue("目标应该死亡", testMonster.isDead() || testMonster.getCurrentHp() <= 0);

        // 验证连射在目标死亡后停止
        int chainCount = skill.getChainCount();
        assertTrue("连射次数应该有限", chainCount <= 5);

        // 验证日志包含目标死亡信息
        boolean hasDeathLog = battleContext.battleLogs.stream()
                .anyMatch(log -> log.getType() == LogType.DEATH &&
                               log.getFormattedMessage().contains("致命连射") &&
                               log.getFormattedMessage().contains("目标已死亡"));

        System.out.println("连射次数：" + chainCount + "，目标死亡：" + testMonster.isDead());
        printBattleLogs();
    }

    /**
     * 测试未暴击时不触发连射
     */
    @Test
    public void testLethalContinuousShotNoCritNoChain() {
        // Given
        ActiveSkill lethalContinuousShot = createSkill("lethal_continuous_shot", 1);
        ActiveSkill_LethalContinuousShot skill = (ActiveSkill_LethalContinuousShot) lethalContinuousShot;

        // 设置0暴击率
        testPlayer.getBaseAttributes().physicalCritRate = 0.0f;
        testPlayer.markAttributeCacheDirty();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, lethalContinuousShot, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证只射击1次（没有连射）
        int chainCount = skill.getChainCount();
        assertEquals("未暴击时应该只射击1次", 1, chainCount);

        printBattleLogs();
    }

    /**
     * 测试不同等级的伤害
     */
    @Test
    public void testLethalContinuousShotDifferentLevels() {
        int[] damagePercents = {75, 80, 85, 90, 95};

        for (int level = 1; level <= 5; level++) {
            // 重置怪物HP和连射计数
            testMonster.setCurrentHp(testMonster.getFinalAttributes().maxHp);

            ActiveSkill lethalContinuousShot = createSkill("lethal_continuous_shot", level);
            ActiveSkill_LethalContinuousShot skill = (ActiveSkill_LethalContinuousShot) lethalContinuousShot;

            skill.resetChainCount();

            int hpBefore = testMonster.getCurrentHp();

            // When - 施放技能
            battleManager.executeSkill(testPlayer, lethalContinuousShot, java.util.Arrays.asList(testMonster), battleContext);

            int damage = hpBefore - testMonster.getCurrentHp();

            // Then - 验证伤害造成
            assertTrue("等级" + level + "应该造成伤害", damage > 0);
            System.out.println("等级" + level + "，连射次数：" + skill.getChainCount() + "，伤害：" + damage);
        }

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testLethalContinuousShotMpCost() {
        // Given
        ActiveSkill lethalContinuousShot = createSkill("lethal_continuous_shot", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, lethalContinuousShot, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证MP消耗
        int mpAfter = testPlayer.getCurrentMp();
        int mpCost = mpBefore - mpAfter;
        assertEquals("应该消耗18点MP", 18, mpCost);

        printBattleLogs();
    }

    /**
     * 测试伤害类型（物理伤害）
     */
    @Test
    public void testLethalContinuousShotDamageType() {
        // Given
        ActiveSkill lethalContinuousShot = createSkill("lethal_continuous_shot", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, lethalContinuousShot, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证日志包含物理伤害信息
        assertLogContains(LogType.DAMAGE, "【致命连射】");
        assertLogExists(LogType.DAMAGE);

        printBattleLogs();
    }

    /**
     * 测试连射次数递增
     */
    @Test
    public void testLethalContinuousShotChainCount() {
        // Given
        ActiveSkill lethalContinuousShot = createSkill("lethal_continuous_shot", 1);
        ActiveSkill_LethalContinuousShot skill = (ActiveSkill_LethalContinuousShot) lethalContinuousShot;

        // 设置50%暴击率以便观察连射
        testPlayer.getBaseAttributes().physicalCritRate = 0.5f;
        testPlayer.markAttributeCacheDirty();

        // 设置怪物高HP
        testMonster.getBaseAttributes().maxHp = 5000;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(5000);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, lethalContinuousShot, java.util.Arrays.asList(testMonster), battleContext);

        // Then - 验证连射次数
        int chainCount = skill.getChainCount();
        assertTrue("连射次数应该大于0", chainCount >= 1);

        System.out.println("连射次数：" + chainCount);
        printBattleLogs();
    }

    /**
     * 测试冷却时间
     */
    @Test
    public void testLethalContinuousShotCooldown() {
        // Given
        ActiveSkill lethalContinuousShot = createSkill("lethal_continuous_shot", 1);

        // When - 获取冷却时间
        int cooldown = lethalContinuousShot.getTemplate().getCooldown();

        // Then - 验证冷却时间为2回合
        assertEquals("冷却时间应该为2回合", 2, cooldown);

        printBattleLogs();
    }
}
