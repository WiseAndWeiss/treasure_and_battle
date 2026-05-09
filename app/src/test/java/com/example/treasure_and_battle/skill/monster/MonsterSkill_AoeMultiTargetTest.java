package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.SkillTargetResolver;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.skill.SkillRangeType;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MonsterSkill_AoeMultiTargetTest extends MonsterSkillTestBase {

    private Monster monsterA;
    private Monster monsterB;
    private Monster monsterC;
    private BattleContext multiCtx;

    @Override
    @Before
    public void setUp() {
        super.setUp();

        monsterA = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
        monsterA.getBaseAttributes().maxHp = 200;
        monsterA.setCurrentHp(200);
        monsterA.setDead(false);

        monsterB = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1002);
        monsterB.getBaseAttributes().maxHp = 150;
        monsterB.setCurrentHp(150);
        monsterB.setDead(false);

        monsterC = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1003);
        monsterC.getBaseAttributes().maxHp = 100;
        monsterC.setCurrentHp(100);
        monsterC.setDead(false);

        List<Monster> monsters = new ArrayList<>();
        monsters.add(monsterA);
        monsters.add(monsterB);
        monsters.add(monsterC);

        multiCtx = new BattleContext(testPlayer, monsters, BattleContext.SurpriseDirection.NONE);
        multiCtx.currentTarget = monsterA;
    }

    @Test
    public void testAllEnemies_ResolvesAllAliveMonsters() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(
                SkillRangeType.ALL_ENEMIES, testPlayer, multiCtx);

        assertEquals("ALL_ENEMIES 应解析出3个活着的怪物", 3, targets.size());
        assertTrue(targets.contains(monsterA));
        assertTrue(targets.contains(monsterB));
        assertTrue(targets.contains(monsterC));
    }

    @Test
    public void testAllEnemies_ExcludesDeadMonster() {
        monsterB.setDead(true);

        List<BattleEntity> targets = SkillTargetResolver.resolve(
                SkillRangeType.ALL_ENEMIES, testPlayer, multiCtx);

        assertEquals("死了的怪物不应出现", 2, targets.size());
        assertTrue(targets.contains(monsterA));
        assertTrue(targets.contains(monsterC));
        assertFalse(targets.contains(monsterB));
    }

    @Test
    public void testAllAllies_ByMonster_ResolvesAllAliveMonsters() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(
                SkillRangeType.ALL_ALLIES, monsterA, multiCtx);

        assertEquals("怪物侧的 ALL_ALLIES 应包含所有活着的怪物", 3, targets.size());
        assertTrue(targets.contains(monsterA));
        assertTrue(targets.contains(monsterB));
        assertTrue(targets.contains(monsterC));
    }

    @Test
    public void testSingleEnemy_ByPlayer_PicksCurrentTarget() {
        multiCtx.currentTarget = monsterB;

        List<BattleEntity> targets = SkillTargetResolver.resolve(
                SkillRangeType.SINGLE_ENEMY, testPlayer, multiCtx);

        assertEquals(1, targets.size());
        assertSame(monsterB, targets.get(0));
    }

    @Test
    public void testSweepDealsDamageToMultipleTargets() {
        int hpBefore = testMonster.getCurrentHp();

        ActiveSkill sweep = createMonsterSkill("monster_sweep", 1);
        List<BattleEntity> targets = SkillTargetResolver.resolve(
                SkillRangeType.ALL_ENEMIES, testPlayer, battleContext);

        for (BattleEntity t : targets) {
            assertFalse("目标不应已死亡", t.isDead());
        }

        battleManager.executeSkill(testPlayer, sweep, targets, battleContext);

        assertTrue("横扫应对目标造成伤害", testMonster.getCurrentHp() < hpBefore);
        assertLogContains(LogType.ACTION, "【横扫】");
    }

    @Test
    public void testDragonBreathLogsAOE() {
        ActiveSkill breath = createMonsterSkill("monster_dragon_breath", 1);
        List<BattleEntity> targets = SkillTargetResolver.resolve(
                SkillRangeType.ALL_ENEMIES, testPlayer, battleContext);

        battleManager.executeSkill(testPlayer, breath, targets, battleContext);

        assertLogContains(LogType.ACTION, "【龙息】");
    }
}
