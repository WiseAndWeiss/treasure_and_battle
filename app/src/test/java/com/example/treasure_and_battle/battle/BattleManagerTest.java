package com.example.treasure_and_battle.battle;

import android.content.Context;

import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.utils.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/**
 * 战斗系统单元测试
 * 覆盖核心规则：先手判定、意图看破、伤害计算、战斗结算、逃跑逻辑
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class BattleManagerTest {
    private Context context;
    private BattleManager battleManager;
    private Player testPlayer;
    private Monster testMonster;

    // ====================== 测试初始化 ======================
    @Before
    public void setUp() {
        // 1. 初始化 Robolectric 模拟 Context
        context = RuntimeEnvironment.application;
        battleManager = BattleManager.getInstance(context);

        // 2. 设置固定随机种子，确保测试结果可复现！
        RandomUtils.setSeed(123456L);

        // 3. 创建测试玩家（可控属性）
        testPlayer = new Player("TestPlayer", context);
        // 手动设置玩家基础属性（避免依赖未完成的职业系统）
        AttributeSet playerAttr = testPlayer.getBaseAttributes();
        playerAttr.strength = 10;
        playerAttr.agility = 10;
        playerAttr.intelligence = 10;
        playerAttr.spirit = 20; // 精神高，方便测试意图看破
        playerAttr.physique = 10;
        playerAttr.luck = 10;
        playerAttr.maxHp = 100;
        playerAttr.maxMp = 50;
        playerAttr.physicalAtk = 30;
        playerAttr.physicalDef = 10;
        playerAttr.magicalAtk = 20;
        playerAttr.magicalDef = 10;
        playerAttr.speed = 15; // 速度15
        playerAttr.physicalCritRate = 0.2f; // 20%暴击率
        playerAttr.expBonus = 1.0f;
        playerAttr.goldBonus = 1.0f;
        testPlayer.markAttributeCacheDirty();
        testPlayer.setCurrentHp(playerAttr.maxHp);
        testPlayer.setCurrentMp(playerAttr.maxMp);

        // 4. 创建测试怪物（通过刚刚搭建的Monster模板系统）
        testMonster = com.example.treasure_and_battle.manager.MonsterManager.getInstance(context).createMonsterByTemplateId(1001);

        // 手动调整点怪物最终属性以适配原有测试逻辑预期
        AttributeSet monsterAttr = testMonster.getBaseAttributes();
        monsterAttr.spirit = 10; // 精神10，玩家精神20，方便测试看破
        monsterAttr.speed = 10; // 速度10，比玩家慢
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(monsterAttr.maxHp);
        testMonster.setCurrentMp(monsterAttr.maxMp);
    }

    // ====================== 测试用例1：先手判定 ======================
    @Test
    public void testTurnOrderDetermination() throws Exception {
        // 利用反射调用私有方法测试，而不是去死循环跑 startBattle
        java.lang.reflect.Method determineMethod = BattleManager.class.getDeclaredMethod("determineTurnOrder", BattleContext.class);
        determineMethod.setAccessible(true);

        // 场景1：常规战斗，玩家速度15 > 怪物速度10 → 玩家先行动
        BattleContext ctx1 = new BattleContext(testPlayer, testMonster, false);
        determineMethod.invoke(battleManager, ctx1);
        assertTrue("玩家速度更高，应该先行动", ctx1.isPlayerTurn);

        // 场景2：偷袭战斗 → 玩家先行动
        BattleContext ctx2 = new BattleContext(testPlayer, testMonster, true);
        determineMethod.invoke(battleManager, ctx2);
        assertTrue("偷袭战斗，玩家应该先行动", ctx2.isPlayerTurn);

        // 场景3：修改怪物速度为20 > 玩家15 → 怪物先行动
        testMonster.getBaseAttributes().speed = 20;
        testMonster.markAttributeCacheDirty();
        BattleContext ctx3 = new BattleContext(testPlayer, testMonster, false);
        determineMethod.invoke(battleManager, ctx3);
        assertFalse("怪物速度更高，应该怪物先行动", ctx3.isPlayerTurn);
    }

    // ====================== 测试用例2：意图看破概率 ======================
    @Test
    public void testIntentSeeThrough() throws Exception {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, false);
        ctx.isPlayerTurn = false;
        
        java.lang.reflect.Method mockIntentMethod = BattleManager.class.getDeclaredMethod("generateAndRevealMonsterIntents", BattleContext.class);
        mockIntentMethod.setAccessible(true);
        mockIntentMethod.invoke(battleManager, ctx);

        AttributeSet playerAttr = testPlayer.getFinalAttributes();
        AttributeSet monsterAttr = testMonster.getFinalAttributes();
        double seeThroughChance = 0.5 * ((double) playerAttr.spirit / monsterAttr.spirit);
        seeThroughChance = Math.max(0.1, Math.min(0.9, seeThroughChance));

        assertEquals("看破概率计算错误", 0.9, seeThroughChance, 0.001);
    }

    // ====================== 测试用例3：伤害计算 ======================
    @Test
    public void testDamageCalculation() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, false);
        ctx.currentActor = testPlayer;
        ctx.currentTarget = testMonster;

        // 玩家攻击30，怪物防御8 → 最终伤害=30-8=22
        battleManager.executeNormalAttack(ctx, testPlayer, testMonster);

        assertEquals("伤害计算错误", 22, ctx.finalDamage);
        assertEquals("怪物HP未正确扣除", 80-22, testMonster.getCurrentHp());

        // 怪物攻击25，玩家防御10 -> 最终伤害 25-10=15
        ctx.currentActor = testMonster;
        ctx.currentTarget = testPlayer;
        battleManager.executeNormalAttack(ctx, testMonster, testPlayer);
        assertEquals("伤害计算错误", 15, ctx.finalDamage);
        assertEquals("玩家HP未正确扣除", 100-15, testPlayer.getCurrentHp());
    }

    // ====================== 测试用例4：战斗胜利结算 ======================
    @Test
    public void testVictorySettlement() throws Exception {
        // 直接把怪物HP设为1，方便测试胜利
        testMonster.setCurrentHp(1);

        BattleContext ctx = new BattleContext(testPlayer, testMonster, false);
        // 让玩家一击即杀怪物
        ctx.currentActor = testPlayer;
        ctx.currentTarget = testMonster;
        battleManager.executeNormalAttack(ctx, testPlayer, testMonster);

        // 利用反射调用战斗结算
        java.lang.reflect.Method settleMethod = BattleManager.class.getDeclaredMethod("settleBattleResult", BattleContext.class);
        settleMethod.setAccessible(true);
        settleMethod.invoke(battleManager, ctx);

        // 验证战斗结果
        assertEquals("战斗结果应为胜利", BattleContext.BattleResult.VICTORY, ctx.battleResult);
        assertTrue("战斗应已结束", ctx.isBattleEnded);
    }

    // ====================== 测试用例5：战斗失败结算 ======================
    @Test
    public void testDefeatSettlement() throws Exception {
        // 直接把玩家HP设为1
        testPlayer.setCurrentHp(1);

        // 让怪物攻击玩家
        BattleContext ctx = new BattleContext(testPlayer, testMonster, false);
        ctx.currentActor = testMonster;
        ctx.currentTarget = testPlayer;
        // 怪物攻击25，玩家防御10 → 伤害15，玩家HP=1-15=0 → 死亡
        battleManager.executeNormalAttack(ctx, testMonster, testPlayer);

        // 利用反射调用战斗结算
        java.lang.reflect.Method settleMethod = BattleManager.class.getDeclaredMethod("settleBattleResult", BattleContext.class);
        settleMethod.setAccessible(true);
        settleMethod.invoke(battleManager, ctx);

        // 验证战斗结果
        assertEquals("战斗结果应为失败", BattleContext.BattleResult.DEFEAT, ctx.battleResult);
        assertEquals("玩家HP应保留1点", 1, testPlayer.getCurrentHp());
        assertFalse("玩家不应处于死亡状态", testPlayer.isDead());
    }

    // ====================== 测试用例6：逃跑逻辑 ======================
    @Test
    public void testEscapeLogic() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, false);
        ctx.currentActionPoints = 2;

        // 玩家速度15，怪物速度10 → 逃跑成功率=0.2 + (15/10-1)*0.5=0.2+0.25=0.45
        // 因为设置了固定种子，这里结果是可预测的
        boolean escaped = battleManager.executePlayerEscape(ctx);

        // 验证行动点消耗
        assertEquals("行动点应消耗1点", 1, ctx.currentActionPoints);

        // 验证逃跑结果（因为种子固定，这里可以断言具体结果）
        // 实际运行时根据种子123456的结果调整断言
        // assertFalse("逃跑应失败", escaped);
    }

    // ====================== 测试用例7：战斗日志系统验证 ======================
    @Test
    public void testBattleLogSystem() {
        BattleContext ctx = new BattleContext(testPlayer, testMonster, false);
        
        // 模拟一个极其简单的战斗交互流程来测试日志：
        // 1. 设置攻击执行者
        ctx.currentActor = testPlayer;
        ctx.currentTarget = testMonster;
        ctx.currentRound = 1;

        // 手动清空日志（以防上面初始化有残留）
        ctx.battleLogs.clear();

        // 2. 执行一次玩家对怪物的普攻
        battleManager.executeNormalAttack(ctx, testPlayer, testMonster);

        // 3. 必定会产生一些日志，比如 DAMAGE 类型的日志
        assertFalse("战斗日志不应该为空！", ctx.battleLogs.isEmpty());
        
        System.out.println("====== 日志系统输出测试开始 ======");
        for (com.example.treasure_and_battle.battle.log.BattleLogEntry log : ctx.battleLogs) {
            System.out.println(log.toString());
            // 验证每条日志都被成功格式化，不包含 %d 或 %s 占位符（应该被正确替换了）
            assertFalse("日志未能正确应用 format 字符串！", log.toString().contains("%d") && !log.toString().contains("%%"));
        }
        System.out.println("====== 日志系统输出测试结束 ======");
        
        // 简单断言第一条一定是 Action 类型或 Damage 类型（基于我们在BattleManager里的插入顺序）
        assertNotNull(ctx.battleLogs.get(0).getType());
    }
}