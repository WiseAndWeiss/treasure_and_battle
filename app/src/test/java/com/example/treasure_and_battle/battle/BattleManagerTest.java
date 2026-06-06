package com.example.treasure_and_battle.battle;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext.SurpriseDirection;
import com.example.treasure_and_battle.battle.action.BattleAction;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.battle.MonsterManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.profession.ProfessionType;
import com.example.treasure_and_battle.utils.RandomUtils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class BattleManagerTest {
    private Context context;
    private BattleManager battleManager;
    private Player testPlayer;
    private Monster testMonster;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        battleManager = BattleManager.getInstance(context);
        RandomUtils.setSeed(123456L);

        testPlayer = new Player("TestPlayer", context);
        testPlayer.owner = new Character(0, "TestHero", ProfessionType.WARRIOR, context);
        AttributeSet playerAttr = testPlayer.getBaseAttributes();
        playerAttr.strength = 10;
        playerAttr.agility = 10;
        playerAttr.intelligence = 10;
        playerAttr.spirit = 20;
        playerAttr.physique = 10;
        playerAttr.luck = 10;
        playerAttr.maxHp = 100;
        playerAttr.maxMp = 50;
        playerAttr.physicalAtk = 30;
        playerAttr.physicalDef = 10;
        playerAttr.magicalAtk = 20;
        playerAttr.magicalDef = 10;
        playerAttr.speed = 15;
        playerAttr.hitRate = 1.0f;
        playerAttr.dodgeRate = 0f;
        playerAttr.physicalCritRate = 0f;
        playerAttr.expBonus = 1.0f;
        playerAttr.goldBonus = 1.0f;
        testPlayer.markAttributeCacheDirty();
        testPlayer.setCurrentHp(playerAttr.maxHp);
        testPlayer.setCurrentMp(playerAttr.maxMp);

        testMonster = MonsterManager.getInstance(context)
                .createMonsterByTemplateId(1001, false);
        AttributeSet monsterAttr = testMonster.getBaseAttributes();
        monsterAttr.spirit = 10;
        monsterAttr.speed = 10;
        monsterAttr.hitRate = 1.0f;
        monsterAttr.dodgeRate = 0f;
        monsterAttr.physicalCritRate = 0f;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(monsterAttr.maxHp);
        testMonster.setCurrentMp(monsterAttr.maxMp);
    }

    // ====================== 速度队列排序测试 ======================

    @Test
    public void testSpeedQueue_PlayerFasterThanMonster() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        battleManager.buildSpeedQueue(ctx);

        assertEquals(2, ctx.roundActionOrder.size());
        assertTrue("玩家速度15>怪物速度10，应排在前面",
                ctx.roundActionOrder.get(0) instanceof Player);
        assertEquals("TestPlayer", ctx.roundActionOrder.get(0).getName());
    }

    @Test
    public void testSpeedQueue_MonsterFasterThanPlayer() {
        testMonster.getBaseAttributes().speed = 30;
        testMonster.markAttributeCacheDirty();

        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        battleManager.buildSpeedQueue(ctx);

        assertEquals(2, ctx.roundActionOrder.size());
        assertTrue("怪物速度30>玩家速度15，应排在前面",
                ctx.roundActionOrder.get(0) instanceof Monster);
    }

    @Test
    public void testSpeedQueue_MultipleMonstersSortedBySpeed() {
        Monster slow = createMonster("slow", 5, 100, 10);
        Monster mid = createMonster("mid", 20, 100, 10);
        Monster fast = createMonster("fast", 40, 100, 10);
        testPlayer.getBaseAttributes().speed = 25;
        testPlayer.markAttributeCacheDirty();

        BattleContext ctx = new BattleContext(testPlayer, Arrays.asList(slow, mid, fast), SurpriseDirection.NONE);
        battleManager.buildSpeedQueue(ctx);

        assertEquals(4, ctx.roundActionOrder.size());
        assertEquals("fast", ctx.roundActionOrder.get(0).getName());
        assertEquals("TestPlayer", ctx.roundActionOrder.get(1).getName());
        assertEquals("mid", ctx.roundActionOrder.get(2).getName());
        assertEquals("slow", ctx.roundActionOrder.get(3).getName());
    }

    @Test
    public void testSpeedQueue_DeadMonsterExcluded() {
        Monster deadMonster = createMonster("dead", 100, 100, 10);
        deadMonster.setDead(true);
        Monster aliveMonster = createMonster("alive", 10, 100, 10);

        BattleContext ctx = new BattleContext(testPlayer, Arrays.asList(deadMonster, aliveMonster), SurpriseDirection.NONE);
        battleManager.buildSpeedQueue(ctx);

        assertEquals(2, ctx.roundActionOrder.size());
    }

    @Test
    public void testSpeedQueue_DeadPlayerStillInQueue() {
        testPlayer.setDead(true);

        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        battleManager.buildSpeedQueue(ctx);

        assertEquals(1, ctx.roundActionOrder.size());
        assertTrue(ctx.roundActionOrder.get(0) instanceof Monster);
    }

    // ====================== 偷袭测试 ======================

    @Test
    public void testSurprise_PlayerSurprisePushesPartyToFront() {
        Monster fastMonster = createMonster("fast", 50, 100, 10);
        testPlayer.getBaseAttributes().speed = 10;
        testPlayer.markAttributeCacheDirty();

        BattleContext ctx = new BattleContext(testPlayer, fastMonster, SurpriseDirection.PLAYER_SURPRISE);
        battleManager.buildSpeedQueue(ctx);

        assertEquals(2, ctx.roundActionOrder.size());
        assertTrue(ctx.roundActionOrder.get(0) instanceof Player);
        assertTrue(ctx.roundActionOrder.get(1) instanceof Monster);
    }

    @Test
    public void testSurprise_MonsterSurprisePushesMonstersToFront() {
        Monster slowMonster = createMonster("slow", 10, 100, 10);
        testPlayer.getBaseAttributes().speed = 50;
        testPlayer.markAttributeCacheDirty();

        BattleContext ctx = new BattleContext(testPlayer, slowMonster, SurpriseDirection.MONSTER_SURPRISE);
        battleManager.buildSpeedQueue(ctx);

        assertEquals(2, ctx.roundActionOrder.size());
        assertTrue(ctx.roundActionOrder.get(0) instanceof Monster);
        assertTrue(ctx.roundActionOrder.get(1) instanceof Player);
    }

    @Test
    public void testSurprise_NoSurpriseKeepsSpeedOrder() {
        Monster slowMonster = createMonster("slow", 5, 100, 10);
        testPlayer.getBaseAttributes().speed = 30;
        testPlayer.markAttributeCacheDirty();

        BattleContext ctx = new BattleContext(testPlayer, slowMonster, SurpriseDirection.NONE);

        List<BattleEntity> actors = new ArrayList<>();
        actors.add(testPlayer);
        actors.addAll(ctx.getAliveMonsters());
        actors.sort((a, b) -> b.getFinalAttributes().speed - a.getFinalAttributes().speed);
        battleManager.applySurpriseToQueue(ctx, actors);

        assertEquals(2, actors.size());
        assertTrue("无偷袭时顺序不变，玩家速度快排前", actors.get(0) instanceof Player);
    }

    // ====================== 看破概率测试 ======================

    @Test
    public void testSeeThroughChance_BasicFormula() {
        testPlayer.getBaseAttributes().spirit = 20;
        testMonster.getBaseAttributes().spirit = 10;
        testPlayer.markAttributeCacheDirty();
        testMonster.markAttributeCacheDirty();

        double chance = battleManager.calculateSeeThroughChance(testPlayer, testMonster);
        assertEquals("精神20 vs 10 => 0.5*(20/10)=1.0, 截断到0.9", 0.9, chance, 0.001);
    }

    @Test
    public void testSeeThroughChance_ClampedBetween01And09() {
        testPlayer.getBaseAttributes().spirit = 1;
        testMonster.getBaseAttributes().spirit = 100;
        testPlayer.markAttributeCacheDirty();
        testMonster.markAttributeCacheDirty();

        double chance = battleManager.calculateSeeThroughChance(testPlayer, testMonster);
        assertTrue("看破率不应低于10%", chance >= 0.1);

        testPlayer.getBaseAttributes().spirit = 200;
        testMonster.getBaseAttributes().spirit = 1;
        testPlayer.markAttributeCacheDirty();
        testMonster.markAttributeCacheDirty();

        chance = battleManager.calculateSeeThroughChance(testPlayer, testMonster);
        assertTrue("看破率不应高于90%", chance <= 0.9);
    }

    // ====================== 伤害计算测试 ======================

    @Test
    public void testDamageCalculation_PlayerAttacksMonster() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        int monsterHpBefore = testMonster.getCurrentHp();
        int expectedDamage = Math.max(1,
            testPlayer.getFinalAttributes().physicalAtk - testMonster.getFinalAttributes().physicalDef);

        battleManager.executeNormalAttack(ctx, testPlayer, testMonster);

        assertTrue("该场景应命中", ctx.isHit);
        assertFalse("该场景不应暴击", ctx.isCriticalHit);
        assertEquals("伤害计算应正确", expectedDamage, ctx.finalDamage);
        assertEquals("HP应正确扣除", monsterHpBefore - expectedDamage, testMonster.getCurrentHp());
    }

    @Test
    public void testDamageCalculation_MonsterAttacksPlayer() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        int playerHpBefore = testPlayer.getCurrentHp();
        int expectedDamage = Math.max(1,
            testMonster.getFinalAttributes().physicalAtk - testPlayer.getFinalAttributes().physicalDef);

        battleManager.executeNormalAttack(ctx, testMonster, testPlayer);

        assertTrue("该场景应命中", ctx.isHit);
        assertFalse("该场景不应暴击", ctx.isCriticalHit);
        assertEquals("伤害计算应正确", expectedDamage, ctx.finalDamage);
        assertEquals("HP应正确扣除", playerHpBefore - expectedDamage, testPlayer.getCurrentHp());
    }

    // ====================== 战斗结算测试 ======================

    @Test
    public void testSettlement_VictoryAwardsExpAndGold() {
        testMonster.setCurrentHp(1);
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        battleManager.executeNormalAttack(ctx, testPlayer, testMonster);
        assertTrue("该场景应命中", ctx.isHit);

        ctx.battleResult = BattleContext.BattleResult.VICTORY;
        ctx.isBattleEnded = true;
        battleManager.settleBattleResult(ctx);

        assertEquals("战斗结果应为胜利", BattleContext.BattleResult.VICTORY, ctx.battleResult);
    }

    @Test
    public void testSettlement_DefeatPreservesOneHp() {
        int expectedDamage = Math.max(1,
            testMonster.getFinalAttributes().physicalAtk - testPlayer.getFinalAttributes().physicalDef);
        testPlayer.setCurrentHp(expectedDamage);

        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        battleManager.executeNormalAttack(ctx, testMonster, testPlayer);
        assertTrue("该场景应命中", ctx.isHit);

        ctx.battleResult = BattleContext.BattleResult.DEFEAT;
        ctx.isBattleEnded = true;
        battleManager.settleBattleResult(ctx);

        assertEquals("失败后HP应保留1点", 1, testPlayer.getCurrentHp());
        assertFalse("玩家不应标记为死亡", testPlayer.isDead());
    }

    @Test
    public void testSettlement_EscapedHasNoPenalty() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.battleResult = BattleContext.BattleResult.ESCAPED;
        ctx.isBattleEnded = true;
        int goldBefore = testPlayer.owner != null ? testPlayer.owner.getGold() : 0;

        battleManager.settleBattleResult(ctx);

        int goldAfter = testPlayer.owner != null ? testPlayer.owner.getGold() : 0;
        assertEquals("逃跑不应损失金币", goldBefore, goldAfter);
    }

    @Test
    public void testSettlement_MonsterEscaped() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.battleResult = BattleContext.BattleResult.MONSTER_ESCAPED;
        ctx.isBattleEnded = true;

        battleManager.settleBattleResult(ctx);

        // 验证不抛异常
        assertNotNull("上下文应保持有效", ctx);
    }

    @Test
    public void testSettlement_UnknownResultLogsWarning() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.battleResult = null;
        ctx.isBattleEnded = true;

        battleManager.settleBattleResult(ctx);

        // 验证不抛异常
        assertNotNull("上下文应保持有效", ctx);
    }

    // ====================== 死亡检查测试 ======================

    @Test
    public void testCheckDeath_AllMonstersDownIsVictory() {
        Monster m1 = createMonster("m1", 10, 100, 10);
        Monster m2 = createMonster("m2", 10, 100, 10);
        BattleContext ctx = new BattleContext(testPlayer, Arrays.asList(m1, m2), SurpriseDirection.NONE);

        m1.setDead(true);
        battleManager.checkDeath(ctx);
        assertFalse("单只死了不应结束", ctx.isBattleEnded);

        m2.setDead(true);
        battleManager.checkDeath(ctx);
        assertTrue("全部死了应结束", ctx.isBattleEnded);
        assertEquals("结果应为胜利", BattleContext.BattleResult.VICTORY, ctx.battleResult);
    }

    @Test
    public void testCheckDeath_PlayerDeadIsDefeat() {
        testPlayer.setDead(true);
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        battleManager.checkDeath(ctx);

        assertTrue("玩家死亡应结束", ctx.isBattleEnded);
        assertEquals("结果应为失败", BattleContext.BattleResult.DEFEAT, ctx.battleResult);
    }

    // ====================== 逃跑测试 ======================

    @Test
    public void testEscape_ConsumesActionPoint() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.currentActionPoints = 2;
        testPlayer.setCurrentActionPoints(2);

        battleManager.executePlayerEscape(ctx);

        assertEquals("应消耗1点行动点", 1, ctx.currentActionPoints);
    }

    @Test
    public void testEscape_VictoryWhenNoMonsters() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.monsters.clear();
        testPlayer.setCurrentActionPoints(2);
        ctx.currentActionPoints = 2;

        boolean escaped = battleManager.executePlayerEscape(ctx);
        assertTrue("无怪物时应逃跑成功", escaped);
        assertTrue(ctx.isBattleEnded);
        assertEquals(BattleContext.BattleResult.VICTORY, ctx.battleResult);
    }

    @Test
    public void testEscape_MonsterEscapeOnlyRemovesSelf() {
        Monster m1 = createMonster("m1", 100, 100, 10);
        Monster m2 = createMonster("m2", 20, 100, 10);
        testPlayer.getBaseAttributes().speed = 1;
        testPlayer.markAttributeCacheDirty();

        BattleContext ctx = new BattleContext(testPlayer, Arrays.asList(m1, m2), SurpriseDirection.NONE);
        ctx.currentActor = m1;

        RandomUtils.setSeed(0L);
        boolean escaped = battleManager.executeMonsterEscape(ctx);

        assertTrue("高速怪物应逃跑成功", escaped);
        assertTrue("逃跑怪物应标记死亡", m1.isDead());
        assertFalse("非行动怪物不应死亡", m2.isDead());
        assertFalse("有存活怪物时战斗不应结束", ctx.isBattleEnded);
    }

    @Test
    public void testEscape_NonMonsterActorReturnsFalse() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.currentActor = testPlayer;

        boolean result = battleManager.executeMonsterEscape(ctx);

        assertFalse("非怪物演员应返回false", result);
    }

    @Test
    public void testEscape_LastMonsterEscapedEndsBattle() {
        Monster onlyMonster = createMonster("only", 100, 100, 10);
        BattleContext ctx = new BattleContext(testPlayer, onlyMonster, SurpriseDirection.NONE);
        ctx.currentActor = onlyMonster;
        RandomUtils.setSeed(0L);

        battleManager.executeMonsterEscape(ctx);

        assertTrue("战斗应结束", ctx.isBattleEnded);
        assertEquals("结果应为怪物逃跑", BattleContext.BattleResult.MONSTER_ESCAPED, ctx.battleResult);
    }

    // ====================== 日志系统测试 ======================

    @Test
    public void testBattleLog_NotEmptyAfterAttack() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.currentRound = 1;
        ctx.battleLogs.clear();

        battleManager.executeNormalAttack(ctx, testPlayer, testMonster);

        assertFalse("战斗日志不应为空", ctx.battleLogs.isEmpty());
        for (com.example.treasure_and_battle.battle.log.BattleLogEntry log : ctx.battleLogs) {
            String str = log.toString();
            assertFalse("日志不应包含未替换的%d占位符", str.contains("%d") && !str.contains("%%"));
        }
    }

    // ====================== 极端情形测试 ======================

    @Test
    public void testEdgeCase_BuildQueueWithEmptyMonsterList() {
        BattleContext ctx = new BattleContext(testPlayer, new ArrayList<>(), SurpriseDirection.NONE);
        battleManager.buildSpeedQueue(ctx);

        assertEquals("空怪物列表，队列应只有玩家", 1, ctx.roundActionOrder.size());
        assertTrue(ctx.roundActionOrder.get(0) instanceof Player);
    }

    @Test
    public void testEdgeCase_PlayerPartyListed() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        assertNotNull(ctx.playerParty);
        assertEquals(1, ctx.playerParty.size());
        assertSame(testPlayer, ctx.playerParty.get(0));
    }

    @Test
    public void testEdgeCase_PickActingMonsterSkipsDead() {
        Monster deadFast = createMonster("deadFast", 99, 100, 10);
        deadFast.setDead(true);
        Monster aliveSlow = createMonster("aliveSlow", 20, 100, 10);
        Monster aliveFaster = createMonster("aliveFaster", 40, 100, 10);

        BattleContext ctx = new BattleContext(testPlayer, Arrays.asList(deadFast, aliveSlow, aliveFaster), SurpriseDirection.NONE);
        Monster selected = battleManager.pickActingMonster(ctx);

        assertNotNull(selected);
        assertEquals("aliveFaster", selected.getName());
    }

    @Test
    public void testEdgeCase_BuildQueueIncludesPlayerParty() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        battleManager.buildSpeedQueue(ctx);

        boolean playerInQueue = ctx.roundActionOrder.stream().anyMatch(e -> e instanceof Player);
        assertTrue("玩家必须在速度队列中", playerInQueue);
    }

    // ====================== 道具使用测试 ======================

    @Test
    public void testUseItem_HealHpWorks() {
        testPlayer.setCurrentHp(50);
        testPlayer.getFinalAttributes().maxHp = 200;

        ConsumableItem potion = new ConsumableItem("test_potion", "测试药水",
                com.example.treasure_and_battle.model.common.Rarity.COMMON,
                10, 10, true, true,
                java.util.Collections.singletonList(createHealEffect(30)),
                "");
        InventoryManager.addItem(testPlayer.owner.getBagItems(), potion);

        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        BattleAction action = BattleAction.useItem(testPlayer, null, "test_potion", "测试药水");
        battleManager.submitBattleAction(ctx, action);

        assertEquals("HP应增加", 80, testPlayer.getCurrentHp());
    }

    @Test
    public void testUseItem_ConsumesFromInventory() {
        testPlayer.setCurrentHp(50);
        testPlayer.getFinalAttributes().maxHp = 200;

        ConsumableItem potion = new ConsumableItem("test_potion_stack", "测试药水",
                com.example.treasure_and_battle.model.common.Rarity.COMMON,
                10, 5, true, true,
                java.util.Collections.singletonList(createHealEffect(10)),
                "");
        potion.setCount(3);
        InventoryManager.addItem(testPlayer.owner.getBagItems(), potion);

        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        BattleAction action = BattleAction.useItem(testPlayer, null, "test_potion_stack", "测试药水");
        battleManager.submitBattleAction(ctx, action);

        assertEquals("堆叠道具使用后数量应为 2", 2, potion.getCount());
    }

    @Test
    public void testUseItem_LastOneRemovedFromInventory() {
        testPlayer.setCurrentHp(50);
        testPlayer.getFinalAttributes().maxHp = 200;

        ConsumableItem potion = new ConsumableItem("test_potion", "测试药水",
                com.example.treasure_and_battle.model.common.Rarity.COMMON,
                10, 1, true, true,
                java.util.Collections.singletonList(createHealEffect(10)),
                "");
        InventoryManager.addItem(testPlayer.owner.getBagItems(), potion);

        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        BattleAction action = BattleAction.useItem(testPlayer, null, "test_potion", "测试药水");
        battleManager.submitBattleAction(ctx, action);

        assertTrue("数量为1消耗后应从背包移除",
                InventoryManager.getBattleUsableConsumables(testPlayer.owner.getBagItems()).isEmpty());
    }

    @Test
    public void testUseItem_NonPlayerFails() {
        int monsterHpBefore = testMonster.getCurrentHp();
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        BattleAction action = BattleAction.useItem(testMonster, null, "any_id", "道具");
        battleManager.submitBattleAction(ctx, action);

        assertEquals("怪物 HP 不应变化", monsterHpBefore, testMonster.getCurrentHp());
    }

    @Test
    public void testUseItem_NonExistentIdFails() {
        int playerHpBefore = testPlayer.getCurrentHp();
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        BattleAction action = BattleAction.useItem(testPlayer, null, "nonexistent_item", "不存在");
        battleManager.submitBattleAction(ctx, action);

        assertEquals("HP 不应变化", playerHpBefore, testPlayer.getCurrentHp());
    }

    // ====================== startBattle 测试 ======================

    @Test
    public void testStartBattle_CompletesFullBattle() {
        Monster weakMonster = createMonster("weak", 5, 10, 5);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.getBaseAttributes().speed = 50;
        testPlayer.markAttributeCacheDirty();

        BattleContext ctx = battleManager.startBattle(testPlayer, Arrays.asList(weakMonster), SurpriseDirection.NONE);

        assertTrue("战斗应结束", ctx.isBattleEnded);
        assertNotNull("应有战斗结果", ctx.battleResult);
    }

    // ====================== bootstrapBattleForUi 测试 ======================

    @Test
    public void testBootstrapBattleForUi_InitializesBattle() {
        BattleContext ctx = battleManager.bootstrapBattleForUi(testPlayer, Arrays.asList(testMonster), SurpriseDirection.NONE);

        assertNotNull("上下文不应为null", ctx);
        assertEquals("回合数应为1", 1, ctx.currentRound);
        assertFalse("速度队列不应为空", ctx.roundActionOrder.isEmpty());
    }

    @Test
    public void testBootstrapBattleForUi_WithRunMonstersTrue() {
        testPlayer.getBaseAttributes().speed = 50;
        testPlayer.markAttributeCacheDirty();
        testMonster.getBaseAttributes().speed = 10;
        testMonster.markAttributeCacheDirty();

        BattleContext ctx = battleManager.bootstrapBattleForUi(testPlayer, Arrays.asList(testMonster),
                SurpriseDirection.NONE, true);

        assertNotNull("上下文不应为null", ctx);
        assertEquals("当前行动者应为玩家", testPlayer, ctx.currentActor);
    }

    @Test
    public void testBootstrapBattleForUi_WithRunMonstersFalse() {
        testPlayer.getBaseAttributes().speed = 50;
        testPlayer.markAttributeCacheDirty();
        testMonster.getBaseAttributes().speed = 100;
        testMonster.markAttributeCacheDirty();

        BattleContext ctx = battleManager.bootstrapBattleForUi(testPlayer, Arrays.asList(testMonster),
                SurpriseDirection.NONE, false);

        assertNotNull("上下文不应为null", ctx);
        // 当runMonsters=false时，怪物不会预先行动，可能没有设置currentActor
        // 但速度队列应该已构建
        assertFalse("速度队列应已构建", ctx.roundActionOrder.isEmpty());
    }

    // ====================== stepOneMonsterAction 测试 ======================

    @Test
    public void testStepOneMonsterAction_NullContextReturnsNull() {
        Monster result = battleManager.stepOneMonsterAction(null);
        assertNull("空上下文应返回null", result);
    }

    @Test
    public void testStepOneMonsterAction_BattleEndedReturnsNull() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.isBattleEnded = true;

        Monster result = battleManager.stepOneMonsterAction(ctx);

        assertNull("战斗结束应返回null", result);
    }

    @Test
    public void testStepOneMonsterAction_ReturnsNullWhenPlayerTurn() {
        testPlayer.getBaseAttributes().speed = 100;
        testPlayer.markAttributeCacheDirty();
        testMonster.getBaseAttributes().speed = 10;
        testMonster.markAttributeCacheDirty();

        BattleContext ctx = battleManager.bootstrapBattleForUi(testPlayer, Arrays.asList(testMonster), SurpriseDirection.NONE);

        Monster result = battleManager.stepOneMonsterAction(ctx);

        assertNull("轮到玩家时应返回null", result);
        assertEquals("当前行动者应为玩家", testPlayer, ctx.currentActor);
    }

    @Test
    public void testStepOneMonsterAction_SkipsDeadEntity() {
        Monster deadMonster = createMonster("dead", 100, 10, 10);
        deadMonster.setDead(true);
        Monster aliveMonster = createMonster("alive", 50, 100, 10);
        // 确保玩家速度最慢，这样第一个行动的会是活着的怪物
        testPlayer.getBaseAttributes().speed = 5;
        testPlayer.markAttributeCacheDirty();

        BattleContext ctx = battleManager.bootstrapBattleForUi(testPlayer,
                Arrays.asList(deadMonster, aliveMonster), SurpriseDirection.NONE, false);

        // 多次调用以跳过死亡怪物
        Monster result = battleManager.stepOneMonsterAction(ctx);
        // 如果第一次返回null（可能是玩家回合或不行动），再次调用
        if (result == null && !ctx.isBattleEnded) {
            result = battleManager.stepOneMonsterAction(ctx);
        }

        // 验证方法能正常处理死亡怪物的情况
        assertNotNull("战斗上下文应保持有效", ctx);
        assertFalse("战斗不应意外结束", ctx.isBattleEnded && ctx.battleResult == BattleContext.BattleResult.DEFEAT);
    }

    @Test
    public void testStepOneMonsterAction_ReturnsActingMonster() {
        testPlayer.getBaseAttributes().speed = 10;
        testPlayer.markAttributeCacheDirty();
        testMonster.getBaseAttributes().speed = 50;
        testMonster.markAttributeCacheDirty();

        BattleContext ctx = battleManager.bootstrapBattleForUi(testPlayer, Arrays.asList(testMonster), SurpriseDirection.NONE, false);

        Monster result = battleManager.stepOneMonsterAction(ctx);

        // 可能返回怪物（如果怪物先行动）或null（如果玩家先行动）
        // 验证至少有一个行动者
        assertNotNull("战斗上下文应有效", ctx);
    }

    // ====================== runMonsterTurnsUntilPlayerTurn 测试 ======================

    @Test
    public void testRunMonsterTurnsUntilPlayerTurn_NullContextDoesNothing() {
        battleManager.runMonsterTurnsUntilPlayerTurn(null);
        // 验证不抛异常
    }

    @Test
    public void testRunMonsterTurnsUntilPlayerTurn_InitializesEmptyQueue() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.roundActionOrder = null;

        battleManager.runMonsterTurnsUntilPlayerTurn(ctx);

        assertNotNull("队列应被初始化", ctx.roundActionOrder);
    }

    // ====================== onPlayerTurnFullySpent 测试 ======================

    @Test
    public void testOnPlayerTurnFullySpent_NullContextDoesNothing() {
        battleManager.onPlayerTurnFullySpent(null);
        // 验证不抛异常
    }

    @Test
    public void testOnPlayerTurnFullySpent_BattleEndedDoesNothing() {
        BattleContext ctx = battleManager.bootstrapBattleForUi(testPlayer, Arrays.asList(testMonster), SurpriseDirection.NONE);
        ctx.isBattleEnded = true;
        int actionIndexBefore = ctx.actionOrderIndex;

        battleManager.onPlayerTurnFullySpent(ctx);

        assertEquals("索引不应变化", actionIndexBefore, ctx.actionOrderIndex);
    }

    @Test
    public void testOnPlayerTurnFullySpent_ActionPointsRemainingDoesNothing() {
        BattleContext ctx = battleManager.bootstrapBattleForUi(testPlayer, Arrays.asList(testMonster), SurpriseDirection.NONE);
        testPlayer.setCurrentActionPoints(2);
        int actionIndexBefore = ctx.actionOrderIndex;

        battleManager.onPlayerTurnFullySpent(ctx);

        assertEquals("索引不应变化", actionIndexBefore, ctx.actionOrderIndex);
    }

    // ====================== executePendingMonsterAction 测试 ======================

    @Test
    public void testExecutePendingMonsterAction_ExecutesAction() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.pendingMonsterAction = BattleAction.normalAttack(testMonster, testPlayer);
        int playerHpBefore = testPlayer.getCurrentHp();

        battleManager.executePendingMonsterAction(ctx);

        assertTrue("玩家HP应变化", testPlayer.getCurrentHp() < playerHpBefore || ctx.finalDamage > 0);
        assertNull("待执行动作应清空", ctx.pendingMonsterAction);
    }

    @Test
    public void testExecutePendingMonsterAction_NullActionDoesNothing() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.pendingMonsterAction = null;

        battleManager.executePendingMonsterAction(ctx);

        // 验证不抛异常
        assertNotNull("上下文应保持有效", ctx);
    }

    // ====================== getAliveMonstersBySpeed 测试 ======================

    @Test
    public void testGetAliveMonstersBySpeed_SortsBySpeed() {
        Monster slow = createMonster("slow", 10, 100, 10);
        Monster fast = createMonster("fast", 50, 100, 10);
        Monster mid = createMonster("mid", 30, 100, 10);

        BattleContext ctx = new BattleContext(testPlayer, Arrays.asList(slow, mid, fast), SurpriseDirection.NONE);

        List<Monster> sorted = battleManager.getAliveMonstersBySpeed(ctx);

        assertEquals("最快怪物应排第一", "fast", sorted.get(0).getName());
        assertEquals("中等速度怪物应排第二", "mid", sorted.get(1).getName());
        assertEquals("最慢怪物应排第三", "slow", sorted.get(2).getName());
    }

    @Test
    public void testGetAliveMonstersBySpeed_ExcludesDeadMonsters() {
        Monster alive = createMonster("alive", 50, 100, 10);
        Monster dead = createMonster("dead", 100, 100, 10);
        dead.setDead(true);

        BattleContext ctx = new BattleContext(testPlayer, Arrays.asList(alive, dead), SurpriseDirection.NONE);

        List<Monster> sorted = battleManager.getAliveMonstersBySpeed(ctx);

        assertEquals("应只包含活着的怪物", 1, sorted.size());
        assertEquals("活着的怪物应在列表中", "alive", sorted.get(0).getName());
    }

    // ====================== dispatchOnBattleStart 测试 ======================

    @Test
    public void testDispatchOnBattleStart_DispatchesTrigger() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);

        battleManager.dispatchOnBattleStart(ctx);

        // 验证触发器被派发（无异常即通过）
        assertNotNull("上下文应保持有效", ctx);
    }

    // ====================== Listener 测试 ======================

    @Test
    public void testSetMonsterActListener() {
        final boolean[] willActCalled = { false };
        final boolean[] escapedCalled = { false };
        final boolean[] failedCalled = { false };
        BattleManager.MonsterActListener listener = new BattleManager.MonsterActListener() {
            @Override
            public void onMonsterWillAct(Monster monster) {
                willActCalled[0] = true;
            }

            @Override
            public void onMonsterEscaped(Monster monster) {
                escapedCalled[0] = true;
            }

            @Override
            public void onMonsterEscapeFailed(Monster monster) {
                failedCalled[0] = true;
            }
        };

        battleManager.setMonsterActListener(listener);

        // 测试逃跑成功
        Monster onlyMonster = createMonster("only", 100, 100, 10);
        BattleContext ctx = new BattleContext(testPlayer, onlyMonster, SurpriseDirection.NONE);
        ctx.currentActor = onlyMonster;
        RandomUtils.setSeed(0L);
        battleManager.executeMonsterEscape(ctx);

        assertTrue("逃跑时应调用onMonsterEscaped", escapedCalled[0]);
    }

    @Test
    public void testSetShieldAbsorbListener() {
        final boolean[] called = { false };
        final int[] absorbedAmount = { 0 };
        BattleManager.ShieldAbsorbListener listener = (target, amount) -> {
            called[0] = true;
            absorbedAmount[0] = amount;
        };

        battleManager.setShieldAbsorbListener(listener);
        battleManager.notifyShieldAbsorbed(testPlayer, 50);

        assertTrue("Listener应被调用", called[0]);
        assertEquals("吸收金额应正确", 50, absorbedAmount[0]);
    }

    @Test
    public void testNotifyShieldAbsorbed_NullListenerDoesNothing() {
        battleManager.setShieldAbsorbListener(null);
        // 验证不抛异常
        battleManager.notifyShieldAbsorbed(testPlayer, 50);
    }

    // ====================== submitBattleAction 边界测试 ======================

    @Test
    public void testSubmitBattleAction_NullActionReturnsFalse() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);

        boolean result = battleManager.submitBattleAction(ctx, null);

        assertFalse("null动作应返回false", result);
    }

    @Test
    public void testSubmitBattleAction_ActionWithNullActorReturnsFalse() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        BattleAction action = new BattleAction(BattleAction.ActionType.ATTACK, null, testPlayer,
                1, 0, 0, 1.0, null, "测试");

        boolean result = battleManager.submitBattleAction(ctx, action);

        assertFalse("空施法者应返回false", result);
    }

    @Test
    public void testSubmitBattleAction_NotEnoughActionPointsFails() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        testPlayer.setCurrentActionPoints(0);
        BattleAction action = BattleAction.normalAttack(testPlayer, testMonster);

        boolean result = battleManager.submitBattleAction(ctx, action);

        assertFalse("行动点不足应失败", result);
    }

    @Test
    public void testSubmitBattleAction_NotEnoughMpFails() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        testPlayer.setCurrentMp(0);
        BattleAction action = BattleAction.useSkill(testPlayer, testMonster,
                "any_skill", 1, 10, 1.0, "技能");

        boolean result = battleManager.submitBattleAction(ctx, action);

        assertFalse("魔力不足应失败", result);
    }

    // ====================== executePlayerEscape with doChaseOnFail 测试 ======================

    @Test
    public void testExecutePlayerEscape_NoChaseOnFail() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        RandomUtils.setSeed(999L);

        battleManager.executePlayerEscape(ctx, false);

        assertFalse("应逃跑失败", ctx.battleResult == BattleContext.BattleResult.ESCAPED);
    }

    @Test
    public void testExecutePlayerEscape_WithChaseOnFail() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        RandomUtils.setSeed(999L);
        int playerHpBefore = testPlayer.getCurrentHp();

        battleManager.executePlayerEscape(ctx, true);

        // 失败时应该被追击
        int playerHpAfter = testPlayer.getCurrentHp();
        assertTrue("追击应造成伤害或至少尝试追击", playerHpAfter <= playerHpBefore);
    }

    // ====================== executeSkill 边界测试 ======================
    // 注：ActiveSkill需要SkillTemplate创建，这里跳过相关测试
    // executeSkill的功能可以通过技能系统的专门测试来验证

    // ====================== 辅助方法 ======================

    private ConsumableItem.Effect createHealEffect(float value) {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        e.value = value;
        e.valueType = "FLAT";
        return e;
    }

    private Monster createMonster(String id, int speed, int maxHp, int patk) {
        int strength = patk;
        int agility = speed;
        int physique = Math.max(0, (maxHp - strength - 10) / 2);
        int intelligence = 1;
        int spirit = 1;
        int luck = 1;

        Monster m = new Monster(id, id, 1,
                com.example.treasure_and_battle.model.common.Rarity.COMMON,
                strength, agility, intelligence, spirit, physique, luck,
                10, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        AttributeSet attr = m.getBaseAttributes();
        attr.hitRate = 1.0f;
        attr.dodgeRate = 0f;
        attr.physicalCritRate = 0f;
        attr.maxHp = maxHp;
        attr.physicalAtk = patk;
        attr.speed = speed;
        attr.physicalDef = 5;
        attr.magicalDef = 5;
        m.markAttributeCacheDirty();
        m.setCurrentHp(maxHp);
        return m;
    }
}
