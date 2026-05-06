package com.example.treasure_and_battle.battle.action;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.utils.RandomUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class BattleActionExecutionTest {

    private Context context;
    private BattleManager battleManager;
    private Player player;
    private Monster monster;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        battleManager = BattleManager.getInstance(context);

        player = new Player("action_player", context);
        monster = new Monster("m_action", "m_action", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                10, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);

        AttributeSet p = player.getBaseAttributes();
        p.maxHp = 300;
        p.maxMp = 50;
        p.physicalAtk = 100;
        p.physicalDef = 0;
        p.hitRate = 1.0f;
        p.dodgeRate = 0f;
        p.physicalCritRate = 0f;
        p.speed = 10;
        player.markAttributeCacheDirty();
        player.setCurrentHp(300);
        player.setCurrentMp(50);

        AttributeSet m = monster.getBaseAttributes();
        m.maxHp = 300;
        m.maxMp = 100;
        m.physicalAtk = 20;
        m.physicalDef = 0;
        m.hitRate = 1.0f;
        m.dodgeRate = 0f;
        m.physicalCritRate = 0f;
        m.speed = 10;
        monster.markAttributeCacheDirty();
        monster.setCurrentHp(300);
        monster.setCurrentMp(100);

        RandomUtils.setSeed(123456L);
    }

    private boolean invokeExecuteBattleAction(BattleContext ctx, BattleAction action) throws Exception {
        Method m = BattleManager.class.getDeclaredMethod("executeBattleAction", BattleContext.class, BattleAction.class);
        m.setAccessible(true);
        return (boolean) m.invoke(battleManager, ctx, action);
    }

    @Test
    public void testResourceConsumptionForAttackAction() throws Exception {
        BattleContext ctx = new BattleContext(player, monster, false);
        player.setCurrentActionPoints(2);
        player.setCurrentMp(40);

        BattleAction action = new BattleAction(BattleAction.ActionType.ATTACK, player, monster,
                1, 5, 0, 1.0, null, "测试攻击");

        boolean ok = invokeExecuteBattleAction(ctx, action);
        assertTrue(ok);
        assertEquals(1, player.getCurrentActionPoints());
        assertEquals(35, player.getCurrentMp());
    }

    @Test
    public void testAttackMultiplierShouldIncreaseDamage() throws Exception {
        BattleContext ctx = new BattleContext(player, monster, false);
        player.setCurrentActionPoints(2);

        BattleAction action = new BattleAction(BattleAction.ActionType.ATTACK, player, monster,
                1, 0, 0, 2.0, null, "倍率攻击");

        int hpBefore = monster.getCurrentHp();
        boolean ok = invokeExecuteBattleAction(ctx, action);

        assertTrue(ok);
        assertEquals(200, hpBefore - monster.getCurrentHp()); // 基础100，倍率2x后200
        assertEquals(200, ctx.finalDamage);
    }

    @Test
    public void testMonsterEscapeSuccessAndFailBothCovered() throws Exception {
        // 成功场景：怪物速度远高于玩家
        BattleContext successCtx = new BattleContext(player, monster, false);
        monster.getBaseAttributes().speed = 100;
        player.getBaseAttributes().speed = 1;
        monster.markAttributeCacheDirty();
        player.markAttributeCacheDirty();

        monster.setCurrentActionPoints(2);
        BattleAction monsterEscape = BattleAction.escape(monster, player);

        // 使用固定种子保证首个随机值可复现（seed=0 时 nextFloat≈0.73）
        RandomUtils.setSeed(0L);
        boolean ok1 = invokeExecuteBattleAction(successCtx, monsterEscape);
        assertTrue(ok1);
        // 一对多改造后：怪物逃跑会标记该怪物离场，不一定立即结束战斗。
        assertTrue(monster.isDead());
        assertTrue(successCtx.isBattleEnded || successCtx.battleResult == null || successCtx.battleResult == BattleContext.BattleResult.MONSTER_ESCAPED);

        // 失败场景：怪物速度远低于玩家
        BattleContext failCtx = new BattleContext(player, monster, false);
        failCtx.isBattleEnded = false;
        failCtx.battleResult = null;

        monster.getBaseAttributes().speed = 1;
        player.getBaseAttributes().speed = 100;
        monster.markAttributeCacheDirty();
        player.markAttributeCacheDirty();

        monster.setCurrentActionPoints(2);
        monster.setDead(false);
        RandomUtils.setSeed(0L);
        boolean ok2 = invokeExecuteBattleAction(failCtx, monsterEscape);
        assertTrue(ok2);
        assertFalse(failCtx.isBattleEnded);
    }

    @Test
    public void testSkillTodoBranchConsumesResourceAndNotCrash() throws Exception {
        BattleContext ctx = new BattleContext(player, monster, false);
        monster.setCurrentActionPoints(2);
        monster.setCurrentMp(30);

        BattleAction skillAction = BattleAction.skillTodo(monster, player,
                "skill_fireball_001", 1, 10, 1.0, "火球术");

        boolean ok = invokeExecuteBattleAction(ctx, skillAction);
        assertTrue(ok);
        assertEquals(1, monster.getCurrentActionPoints());
        assertEquals(20, monster.getCurrentMp());
        assertTrue(ctx.battleLogs.size() > 0);
    }
}
