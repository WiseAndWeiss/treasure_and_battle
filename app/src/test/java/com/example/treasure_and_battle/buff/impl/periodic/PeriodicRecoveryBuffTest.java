package com.example.treasure_and_battle.buff.impl.periodic;

import android.content.Context;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class PeriodicRecoveryBuffTest {

    private Context context;
    private Player testPlayer;
    private BattleContext ctx;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        testPlayer = new Player("RecoveryTestPlayer", context);

        AttributeSet attr = testPlayer.getBaseAttributes();
        attr.maxHp = 1000;
        attr.maxMp = 500;
        testPlayer.markAttributeCacheDirty();
        
        // 刻意不满血不满蓝，用来测试回复是否正确截断
        testPlayer.setCurrentHp(500); 
        testPlayer.setCurrentMp(100);

        ctx = new BattleContext(testPlayer, (com.example.treasure_and_battle.model.entity.Monster) null, false);
    }

    @Test
    public void testRegeneratingBuff_Flat() {
        // 固定值恢复：每层提供50点HP恢复，持续3回合
        RegeneratingBuff flatHeal = new RegeneratingBuff("buff_regen_flat", "生命光环", "生命光环",
                BuffType.BUFF, true, 3, 99, true, 50.0f, ValueType.FLAT);

        // 叠加2层，预期回合末回复 100 点生命值
        flatHeal.tryStack(flatHeal);

        // 模拟回合结束触发
        flatHeal.onTrigger(testPlayer, ctx, BuffTriggerType.ON_ROUND_END);
        assertEquals(600, testPlayer.getCurrentHp()); // 500 + (50 * 2) = 600

        // 回合衰减（不掉层数，掉最大回合），此时持续回合应当剩2
        boolean isExpired = flatHeal.tick();
        assertFalse(isExpired);
        assertEquals(2, flatHeal.getStackCount()); // 层数不变
        assertEquals(2, flatHeal.getRemainingDuration());

        // 测试溢出治疗：直接设为950血
        testPlayer.setCurrentHp(950);
        flatHeal.onTrigger(testPlayer, ctx, BuffTriggerType.ON_ROUND_END);
        assertEquals(1000, testPlayer.getCurrentHp()); // 950 + 100 = 1050 -> 溢出应被maxHp截断
    }

    @Test
    public void testRegeneratingBuff_Percentage() {
        // 百分比恢复：每层恢复5%（0.05f）最大生命，持续2回合
        RegeneratingBuff pctHeal = new RegeneratingBuff("buff_regen_pct", "复苏之风", "复苏之风",
                BuffType.BUFF, true, 2, 99, true, 0.05f, ValueType.PERCENTAGE);

        // 叠加3层，预计回合恢复 15% (1000 * 15% = 150点)
        pctHeal.tryStack(pctHeal);
        pctHeal.tryStack(pctHeal);

        // 触发一次回复
        pctHeal.onTrigger(testPlayer, ctx, BuffTriggerType.ON_ROUND_END);
        assertEquals(650, testPlayer.getCurrentHp()); // 500 + 150 = 650

        // 测完2次回合衰减该buff应当消失
        assertFalse(pctHeal.tick()); // 剩余 1 回合
        assertTrue(pctHeal.tick());  // 剩余 0 回合，应当脱落
    }

    @Test
    public void testManaRegeneratingBuff_Percentage() {
        // 百分比蓝量恢复：每层恢复10%最大魔力值(500 * 10% = 50)，持续2回合
        ManaRegeneratingBuff mpHeal = new ManaRegeneratingBuff("buff_mana_regen", "法力涌动", "法力涌动",
                BuffType.BUFF, true, 2, 99, true, 0.1f, ValueType.PERCENTAGE);

        // 触发一次回复 (单层，恢复50)
        mpHeal.onTrigger(testPlayer, ctx, BuffTriggerType.ON_ROUND_END);
        assertEquals(150, testPlayer.getCurrentMp()); // 100 + 50 = 150

        // 测回合流失
        assertFalse(mpHeal.tick()); // 回合 2 -> 1
    }
}