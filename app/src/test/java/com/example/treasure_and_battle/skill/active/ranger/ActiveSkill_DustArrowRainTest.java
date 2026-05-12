package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.control.BlindnessDebuff;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.manager.MonsterManager;
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
 * 扬尘箭雨技能测试
 * 技能效果：对所有敌人造成x%物理攻击伤害，附加2回合y%减速与z%致盲效果
 */
public class ActiveSkill_DustArrowRainTest extends ActiveSkillTestBase {

    private List<BattleEntity> multipleMonsters;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        // 创建多个怪物用于AOE测试
        multipleMonsters = new ArrayList<>();
        multipleMonsters.add(testMonster);

        Monster monster2 = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
        monster2.getBaseAttributes().maxHp = 300;
        monster2.getBaseAttributes().physicalDef = 20;
        monster2.getBaseAttributes().speed = 12;
        monster2.getBaseAttributes().dodgeRate = 0f;  // 确保不闪避
        monster2.markAttributeCacheDirty();
        monster2.setCurrentHp(monster2.getBaseAttributes().maxHp);
        multipleMonsters.add(monster2);
    }

    /**
     * 测试等级1：65%伤害、3%减速、3%致盲
     */
    @Test
    public void testDustArrowRainLevel1() {
        // Given
        ActiveSkill dustArrowRain = createSkill("dust_arrow_rain", 1);

        // When - 对多个目标施放技能
        battleManager.executeSkill(testPlayer, dustArrowRain, multipleMonsters, battleContext);

        // Then - 验证所有目标都受到伤害
        int damagedCount = 0;
        for (BattleEntity entity : multipleMonsters) {
            if (entity.getCurrentHp() < entity.getFinalAttributes().maxHp) {
                damagedCount++;
            }
        }
        assertTrue("所有目标都应该受到伤害", damagedCount == multipleMonsters.size());

        // 验证减速和致盲debuff添加
        for (BattleEntity entity : multipleMonsters) {
            SlowDebuff slowDebuff = (SlowDebuff) entity.getActiveBuffList().stream()
                    .filter(buff -> buff instanceof SlowDebuff)
                    .filter(buff -> buff.getBuffId().equals("dust_arrow_rain_slow"))
                    .findFirst()
                    .orElse(null);

            assertNotNull("应该有减速debuff", slowDebuff);
            assertEquals("减速debuff应该持续2回合", 2, slowDebuff.getRemainingDuration());

            BlindnessDebuff blindDebuff = (BlindnessDebuff) entity.getActiveBuffList().stream()
                    .filter(buff -> buff instanceof BlindnessDebuff)
                    .filter(buff -> buff.getBuffId().equals("dust_arrow_rain_blind"))
                    .findFirst()
                    .orElse(null);

            assertNotNull("应该有致盲debuff", blindDebuff);
            assertEquals("致盲debuff应该持续2回合", 2, blindDebuff.getRemainingDuration());
        }

        // 验证日志
        assertLogContains(LogType.DAMAGE, "【扬尘箭雨】");

        printBattleLogs();
    }

    /**
     * 测试等级3：75%伤害、9%减速、9%致盲
     */
    @Test
    public void testDustArrowRainLevel3() {
        // Given
        ActiveSkill dustArrowRain = createSkill("dust_arrow_rain", 3);

        // When - 对多个目标施放技能
        battleManager.executeSkill(testPlayer, dustArrowRain, multipleMonsters, battleContext);

        // Then - 验证所有目标都受到伤害
        int totalTargets = multipleMonsters.size();
        int damagedTargets = 0;
        for (BattleEntity entity : multipleMonsters) {
            if (entity.getCurrentHp() < entity.getFinalAttributes().maxHp) {
                damagedTargets++;
            }
        }
        assertEquals("所有目标都应该受到伤害", totalTargets, damagedTargets);

        printBattleLogs();
    }

    /**
     * 测试等级5：85%伤害、15%减速、15%致盲
     */
    @Test
    public void testDustArrowRainLevel5() {
        // Given
        ActiveSkill dustArrowRain = createSkill("dust_arrow_rain", 5);

        // When - 对多个目标施放技能
        battleManager.executeSkill(testPlayer, dustArrowRain, multipleMonsters, battleContext);

        // Then - 验证减速和致盲效果
        for (BattleEntity entity : multipleMonsters) {
            SlowDebuff slowDebuff = (SlowDebuff) entity.getActiveBuffList().stream()
                    .filter(buff -> buff instanceof SlowDebuff)
                    .filter(buff -> buff.getBuffId().equals("dust_arrow_rain_slow"))
                    .findFirst()
                    .orElse(null);

            assertNotNull("应该有减速debuff", slowDebuff);

            BlindnessDebuff blindDebuff = (BlindnessDebuff) entity.getActiveBuffList().stream()
                    .filter(buff -> buff instanceof BlindnessDebuff)
                    .filter(buff -> buff.getBuffId().equals("dust_arrow_rain_blind"))
                    .findFirst()
                    .orElse(null);

            assertNotNull("应该有致盲debuff", blindDebuff);
        }

        printBattleLogs();
    }

    /**
     * 测试AOE伤害
     */
    @Test
    public void testDustArrowRainAOEDamage() {
        // Given
        ActiveSkill dustArrowRain = createSkill("dust_arrow_rain", 1);
        int totalHpBefore = 0;
        for (BattleEntity entity : multipleMonsters) {
            totalHpBefore += entity.getCurrentHp();
        }

        // When - 对多个目标施放技能
        battleManager.executeSkill(testPlayer, dustArrowRain, multipleMonsters, battleContext);

        // Then - 验证总伤害
        int totalHpAfter = 0;
        for (BattleEntity entity : multipleMonsters) {
            totalHpAfter += entity.getCurrentHp();
        }
        int totalDamage = totalHpBefore - totalHpAfter;

        assertTrue("应该对多个目标造成总伤害", totalDamage > 0);

        printBattleLogs();
    }

    /**
     * 测试减速debuff持续时间
     */
    @Test
    public void testDustArrowRainSlowDuration() {
        // Given
        ActiveSkill dustArrowRain = createSkill("dust_arrow_rain", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, dustArrowRain, multipleMonsters, battleContext);

        // Then - 验证减速debuff持续2回合
        for (BattleEntity entity : multipleMonsters) {
            SlowDebuff slowDebuff = (SlowDebuff) entity.getActiveBuffList().stream()
                    .filter(buff -> buff instanceof SlowDebuff)
                    .filter(buff -> buff.getBuffId().equals("dust_arrow_rain_slow"))
                    .findFirst()
                    .orElse(null);

            assertNotNull("应该有减速debuff", slowDebuff);
            assertEquals("减速debuff应该持续2回合", 2, slowDebuff.getRemainingDuration());

            // 模拟第一回合结束
            slowDebuff.tick();
            assertEquals("第一回合结束后应该剩余1回合", 1, slowDebuff.getRemainingDuration());

            // 模拟第二回合结束
            slowDebuff.tick();
            assertEquals("第二回合结束后应该剩余0回合", 0, slowDebuff.getRemainingDuration());
        }

        printBattleLogs();
    }

    /**
     * 测试致盲debuff持续时间
     */
    @Test
    public void testDustArrowRainBlindDuration() {
        // Given
        ActiveSkill dustArrowRain = createSkill("dust_arrow_rain", 1);

        // When - 施放技能
        battleManager.executeSkill(testPlayer, dustArrowRain, multipleMonsters, battleContext);

        // Then - 验证致盲debuff持续2回合
        for (BattleEntity entity : multipleMonsters) {
            BlindnessDebuff blindDebuff = (BlindnessDebuff) entity.getActiveBuffList().stream()
                    .filter(buff -> buff instanceof BlindnessDebuff)
                    .filter(buff -> buff.getBuffId().equals("dust_arrow_rain_blind"))
                    .findFirst()
                    .orElse(null);

            assertNotNull("应该有致盲debuff", blindDebuff);
            assertEquals("致盲debuff应该持续2回合", 2, blindDebuff.getRemainingDuration());
        }

        printBattleLogs();
    }

    /**
     * 测试debuff效果生效
     */
    @Test
    public void testDustArrowRainDebuffEffectiveness() {
        // Given
        ActiveSkill dustArrowRain = createSkill("dust_arrow_rain", 1);

        int initialSpeed = testMonster.getFinalAttributes().speed;
        float initialHitRate = testMonster.getFinalAttributes().hitRate;

        // When - 施放技能
        battleManager.executeSkill(testPlayer, dustArrowRain, multipleMonsters, battleContext);

        // Then - 验证速度和命中率确实改变了
        int currentSpeed = testMonster.getFinalAttributes().speed;
        float currentHitRate = testMonster.getFinalAttributes().hitRate;

        // 注意：由于取整问题，3%的减速对10点速度可能无法体现（10 * 0.97 = 9.7 ≈ 10）
        // 但命中率应该明确降低
        assertTrue("命中率应该降低", currentHitRate < initialHitRate);

        // 检查减速buff是否正确应用
        SlowDebuff slowDebuff = (SlowDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof SlowDebuff)
                .findFirst()
                .orElse(null);
        assertNotNull("减速buff应该存在", slowDebuff);
        assertEquals("减速百分比应该是3%", 3.0f, slowDebuff.getSpeedReductionPercent(), 0.1f);

        printBattleLogs();
    }

    /**
     * 测试对单个目标的效果
     */
    @Test
    public void testDustArrowRainSingleTarget() {
        // Given
        ActiveSkill dustArrowRain = createSkill("dust_arrow_rain", 1);

        // When - 对单个目标施放技能
        battleManager.executeSkill(testPlayer, dustArrowRain, Arrays.asList(testMonster), battleContext);

        // Then - 验证伤害和debuff
        assertTrue("应该造成伤害", testMonster.getCurrentHp() < testMonster.getFinalAttributes().maxHp);

        SlowDebuff slowDebuff = (SlowDebuff) testMonster.getActiveBuffList().stream()
                .filter(buff -> buff instanceof SlowDebuff)
                .filter(buff -> buff.getBuffId().equals("dust_arrow_rain_slow"))
                .findFirst()
                .orElse(null);

        assertNotNull("应该有减速debuff", slowDebuff);

        printBattleLogs();
    }

    /**
     * 测试MP消耗
     */
    @Test
    public void testDustArrowRainMpCost() {
        // Given
        ActiveSkill dustArrowRain = createSkill("dust_arrow_rain", 1);
        int mpBefore = testPlayer.getCurrentMp();

        // When - 施放技能
        battleManager.executeSkill(testPlayer, dustArrowRain, multipleMonsters, battleContext);

        // Then - 验证MP消耗
        int mpAfter = testPlayer.getCurrentMp();
        int mpCost = mpBefore - mpAfter;
        assertEquals("应该消耗15点MP", 15, mpCost);

        printBattleLogs();
    }

    /**
     * 测试冷却时间
     */
    @Test
    public void testDustArrowRainCooldown() {
        // Given
        ActiveSkill dustArrowRain = createSkill("dust_arrow_rain", 1);

        // When - 获取技能冷却时间
        int cooldown = dustArrowRain.getTemplate().getCooldown();

        // Then - 验证冷却时间为2回合
        assertEquals("冷却时间应该为2回合", 2, cooldown);

        printBattleLogs();
    }
}
