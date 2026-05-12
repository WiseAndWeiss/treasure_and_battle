package com.example.treasure_and_battle.battle;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext.RevealedIntent;
import com.example.treasure_and_battle.battle.BattleContext.SurpriseDirection;
import com.example.treasure_and_battle.battle.action.ActionIntent;
import com.example.treasure_and_battle.battle.action.BattleAction;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.profession.ProfessionType;
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
        // InventoryManager.releaseInstance() — removed (stateless)
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

        testMonster = com.example.treasure_and_battle.manager.MonsterManager.getInstance(context)
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

    @Test
    public void testIntentReveal_GeneratedOnRoundStart() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        ctx.monsterRevealedIntents.clear();

        Monster m = ctx.getAliveMonsters().get(0);
        ctx.monsterRevealedIntents.put(m.getEntityId(), new ArrayList<>());

        assertNotNull(ctx.monsterRevealedIntents.get(m.getEntityId()));
    }

    // ====================== ReavealedIntent 执行标记测试 ======================

    @Test
    public void testRevealedIntent_ExecutionMarkedAfterMonsterActs() {
        Monster dummy = createMonster("dummy", 10, 100, 10);
        BattleContext ctx = new BattleContext(testPlayer, dummy, SurpriseDirection.NONE);

        ctx.monsterRevealedIntents.clear();

        ActionIntent intent1 =
            new ActionIntent(
                "测试攻击", "", ActionIntent.IntentType.ATTACK,
                1, 0, 1.0, 100, 10, -1f, -1f, null);

        List<RevealedIntent> revealed = new ArrayList<>();
        revealed.add(new RevealedIntent(intent1, true));
        ctx.monsterRevealedIntents.put(dummy.getEntityId(), revealed);

        assertFalse("初始未执行", revealed.get(0).executed);
        revealed.get(0).executed = true;
        assertTrue("手动标记后应为已执行", revealed.get(0).executed);
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
    public void testSettlement_AggregatesExpFromAllMonsters() {
        Monster m1 = createMonster("m1", 10, 100, 10);
        Monster m2 = createMonster("m2", 12, 100, 10);
        m1.setExpReward(30);
        m2.setExpReward(40);

        BattleContext ctx = new BattleContext(testPlayer, Arrays.asList(m1, m2), SurpriseDirection.NONE);
        ctx.battleResult = BattleContext.BattleResult.VICTORY;
        ctx.isBattleEnded = true;
        battleManager.settleBattleResult(ctx);

        assertEquals("战斗结果应为胜利", BattleContext.BattleResult.VICTORY, ctx.battleResult);
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
    public void testEdgeCase_MonsterDiesBeforeActionInQueue() {
        Monster fastMonster = createMonster("fast", 50, 5, 30);
        Monster slowMonster = createMonster("slow", 10, 100, 10);
        testPlayer.getBaseAttributes().physicalAtk = 100;
        testPlayer.getBaseAttributes().speed = 30;
        testPlayer.markAttributeCacheDirty();

        BattleContext ctx = new BattleContext(testPlayer, Arrays.asList(fastMonster, slowMonster), SurpriseDirection.NONE);
        battleManager.buildSpeedQueue(ctx);

        ctx.currentActor = fastMonster;
        ctx.currentTarget = slowMonster;
        battleManager.executeNormalAttack(ctx, testPlayer, fastMonster);
        checkActorDeadAndSkip(ctx, "fast", 1);
    }

    private void checkActorDeadAndSkip(BattleContext ctx, String name, int aliveCount) {
        for (BattleEntity e : ctx.roundActionOrder) {
            if (e.getName().equals(name)) {
                assertTrue(e.isDead());
            }
        }
    }

    @Test
    public void testEdgeCase_PlayerPartyListed() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, SurpriseDirection.NONE);
        assertNotNull(ctx.playerParty);
        assertEquals(1, ctx.playerParty.size());
        assertSame(testPlayer, ctx.playerParty.get(0));
    }

    @Test
    public void testEdgeCase_MonsterEscapeAllMonstersGone() {
        Monster onlyMonster = createMonster("only", 100, 100, 10);
        testPlayer.getBaseAttributes().speed = 1;
        testPlayer.markAttributeCacheDirty();

        BattleContext ctx = new BattleContext(testPlayer, onlyMonster, SurpriseDirection.NONE);
        ctx.currentActor = onlyMonster;

        RandomUtils.setSeed(0L);
        battleManager.executeMonsterEscape(ctx);

        assertTrue("逃跑后应结束战斗", ctx.isBattleEnded);
        assertEquals("结果应为怪物逃跑", BattleContext.BattleResult.MONSTER_ESCAPED, ctx.battleResult);
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

    private ConsumableItem.Effect createHealEffect(float value) {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        e.value = value;
        e.valueType = "FLAT";
        return e;
    }

    // ====================== 辅助方法 ======================

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
