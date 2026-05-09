package com.example.treasure_and_battle.battle;

import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.skill.SkillRangeType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class SkillTargetResolverTest {

    private BattleContext ctx;
    private Player player;
    private Monster monster;

    @Before
    public void setUp() {
        android.content.Context appCtx = RuntimeEnvironment.application;
        player = new Player("TestPlayer", appCtx);
        player.getBaseAttributes().maxHp = 200;
        player.setCurrentHp(200);
        player.setDead(false);

        monster = new Monster("m1", "Monster1", 3,
                com.example.treasure_and_battle.model.common.Rarity.COMMON,
                5, 5, 5, 5, 5, 5,
                10, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                appCtx);
        monster.getBaseAttributes().maxHp = 100;
        monster.setCurrentHp(100);
        monster.setDead(false);

        List<Monster> monsters = new ArrayList<>();
        monsters.add(monster);
        ctx = new BattleContext(player, monsters, BattleContext.SurpriseDirection.NONE);
        ctx.currentTarget = monster;
    }

    @Test
    public void testResolve_Self_Player() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(SkillRangeType.SELF, player, ctx);
        assertEquals(1, targets.size());
        assertSame(player, targets.get(0));
    }

    @Test
    public void testResolve_Self_Monster() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(SkillRangeType.SELF, monster, ctx);
        assertEquals(1, targets.size());
        assertSame(monster, targets.get(0));
    }

    @Test
    public void testResolve_SingleEnemy_ByPlayer() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(SkillRangeType.SINGLE_ENEMY, player, ctx);
        assertEquals(1, targets.size());
        assertSame(monster, targets.get(0));
    }

    @Test
    public void testResolve_SingleEnemy_ByMonster() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(SkillRangeType.SINGLE_ENEMY, monster, ctx);
        assertEquals(1, targets.size());
        assertSame(player, targets.get(0));
    }

    @Test
    public void testResolve_AllEnemies_ByPlayer() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(SkillRangeType.ALL_ENEMIES, player, ctx);
        assertEquals(1, targets.size());
        assertSame(monster, targets.get(0));
    }

    @Test
    public void testResolve_AllEnemies_ByMonster() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(SkillRangeType.ALL_ENEMIES, monster, ctx);
        assertEquals(1, targets.size());
        assertSame(player, targets.get(0));
    }

    @Test
    public void testResolve_AllAllies_ByPlayer() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(SkillRangeType.ALL_ALLIES, player, ctx);
        assertTrue(targets.contains(player));
    }

    @Test
    public void testResolve_AllAllies_ByMonster() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(SkillRangeType.ALL_ALLIES, monster, ctx);
        assertTrue(targets.contains(monster));
    }

    @Test
    public void testResolve_AllAllies_ExcludesDead() {
        monster.setDead(true);
        List<BattleEntity> targets = SkillTargetResolver.resolve(SkillRangeType.ALL_ALLIES, monster, ctx);
        assertFalse("死亡怪物不应出现在目标列表中", targets.contains(monster));
    }

    @Test
    public void testResolve_None_ReturnsEmpty() {
        List<BattleEntity> targets = SkillTargetResolver.resolve(SkillRangeType.NONE, player, ctx);
        assertTrue(targets.isEmpty());
    }
}
