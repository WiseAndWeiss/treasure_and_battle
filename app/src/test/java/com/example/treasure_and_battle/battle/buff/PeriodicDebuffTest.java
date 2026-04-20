package com.example.treasure_and_battle.battle.buff;

import android.content.Context;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class PeriodicDebuffTest {

    private Context context;
    private Player testPlayer;
    private BattleContext ctx;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        testPlayer = new Player("PeriodicTestPlayer", context);
        
        AttributeSet attr = testPlayer.getBaseAttributes();
        attr.maxHp = 1000;
        attr.magicalDef = 0; // 默认魔防为0，方便测试灼烧
        testPlayer.markAttributeCacheDirty();
        testPlayer.setCurrentHp(1000);

        ctx = new BattleContext(testPlayer, null, false);
    }

    @Test
    public void testPoisoningDebuff() {
        // 中毒：每层流失1点生命，衰减：每回合减半向上取整
        PoisoningDebuff poisoning = new PoisoningDebuff("debuff_poison", "中毒", "中毒",
                BuffType.DEBUFF, true, -1, 99, true, 1.0f);
        
        poisoning.tryStack(poisoning);
        poisoning.tryStack(poisoning); // 3层
        assertEquals(3, poisoning.getStackCount());
        
        // 第一回合触发
        poisoning.onTrigger(testPlayer, ctx, BuffTriggerType.ON_ROUND_END);
        assertEquals(997, testPlayer.getCurrentHp()); // 3层扣3点 (1000 -> 997)
        
        boolean isExpired = poisoning.tick();
        assertEquals(1, poisoning.getStackCount()); // 3层减半向上取整，衰减2，剩余1
        assertFalse(isExpired);
        
        // 第二回合触发
        poisoning.onTrigger(testPlayer, ctx, BuffTriggerType.ON_ROUND_END);
        assertEquals(996, testPlayer.getCurrentHp()); // 1层扣1点 (997 -> 996)
        
        isExpired = poisoning.tick();
        assertEquals(0, poisoning.getStackCount()); // 1层减半1，剩余0
        assertTrue(isExpired);
    }

    @Test
    public void testBleedingDebuff() {
        // 流血：每层流失1%最大生命值，衰减：每回合层数减1
        BleedingDebuff bleeding = new BleedingDebuff("debuff_bleed", "流血", "流血",
                BuffType.DEBUFF, true, -1, 99, true, 1.0f);

        bleeding.tryStack(bleeding, 2); // 3层
        assertEquals(3, bleeding.getStackCount());

        // 第一回合触发
        bleeding.onTrigger(testPlayer, ctx, BuffTriggerType.ON_ROUND_END);
        // 最大生命1000，1%是10，3层是30。
        assertEquals(970, testPlayer.getCurrentHp()); // (1000 -> 970)

        boolean isExpired = bleeding.tick();
        assertEquals(2, bleeding.getStackCount()); // 3层减1层，剩余2
        assertFalse(isExpired);

        // 第二回合触发
        bleeding.onTrigger(testPlayer, ctx, BuffTriggerType.ON_ROUND_END);
        assertEquals(950, testPlayer.getCurrentHp()); // 2层流失20点 (970 -> 950)

        isExpired = bleeding.tick();
        assertEquals(1, bleeding.getStackCount()); // 剩余1层
        assertFalse(isExpired);
    }

    @Test
    public void testBurningDebuff() {
        // 灼烧：每层造成1点魔法伤害，每回合减半向下取整衰减
        // 注意：基类的衰减逻辑如果和类实现不符，这里严格按照实现逻辑：
        // int decayAmount = (int) Math.ceil(this.stackCount / 2.0); this.stackCount -= decayAmount;
        // 计算得：3层 decay=2（剩余1），1层 decay=1（剩余0）

        testPlayer.getBaseAttributes().magicalDef = 1; // 增加1点魔法防御测试减免
        testPlayer.markAttributeCacheDirty(); // 刷新防御面板

        BurningDebuff burning = new BurningDebuff("debuff_burn", "灼烧", "灼烧",
                BuffType.DEBUFF, true, -1, 99, true, 1.0f);

        burning.tryStack(burning);
        burning.tryStack(burning); // 3层
        
        // 第一回合触发
        burning.onTrigger(testPlayer, ctx, BuffTriggerType.ON_ROUND_END);
        // 3层造成3点魔法伤害，魔防1点，所以实际伤害2
        assertEquals(998, testPlayer.getCurrentHp()); // (1000 -> 998)

        boolean isExpired = burning.tick();
        assertEquals(1, burning.getStackCount()); // 3层 decay=2，剩余1层
        assertFalse(isExpired);

        // 第二回合触发
        burning.onTrigger(testPlayer, ctx, BuffTriggerType.ON_ROUND_END);
        // 1层造成1点魔法伤害，魔防1点，实际伤害0 (确保为最大值 > 0)
        assertEquals(998, testPlayer.getCurrentHp()); // (998 -> 998)

        isExpired = burning.tick();
        assertEquals(0, burning.getStackCount()); // 1层 decay=1，剩余0层
        assertTrue(isExpired);
    }
}