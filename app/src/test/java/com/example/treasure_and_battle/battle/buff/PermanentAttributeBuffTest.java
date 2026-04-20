package com.example.treasure_and_battle.battle.buff;

import android.content.Context;

import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.utils.AttributeUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class PermanentAttributeBuffTest {

    private Context context;
    private Player testPlayer;
    private BuffManager buffManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        testPlayer = new Player("PermanentBuffPlayer", context);
        buffManager = BuffManager.getInstance(context);
    }

    private AttributeSet getCalculatedFinalAttributes() {
        AttributeUtils.calculateFinalAttributes(testPlayer, context);
        return testPlayer.getFinalAttributes();
    }

    @Test
    public void testPermanentTriggerAttributeBuffExpiresAndAttributeFallsBack() {
        AttributeSet base = testPlayer.getBaseAttributes();
        base.strength = 10;
        testPlayer.markAttributeCacheDirty();

        AttributeBuff strengthBuff = new AttributeBuff(
                "buff_test_strength_flat",
                "测试力量",
                "",
                BuffType.BUFF,
                true,
                2,
                1,
                false,
                30f,
                AttributeType.STRENGTH,
                ValueType.FLAT
        );

        buffManager.addBuff(testPlayer, strengthBuff);

        // 生效后：10 + 30 = 40
        assertEquals(40, getCalculatedFinalAttributes().strength);
        assertEquals(1, testPlayer.getActiveBuffList().size());

        // 第1次tick：剩余回合2 -> 1，Buff仍在
        buffManager.tickBuffs(testPlayer);
        assertEquals(40, getCalculatedFinalAttributes().strength);
        assertEquals(1, testPlayer.getActiveBuffList().size());

        // 第2次tick：剩余回合1 -> 0，Buff过期移除
        buffManager.tickBuffs(testPlayer);
        assertEquals(10, getCalculatedFinalAttributes().strength);
        assertEquals(0, testPlayer.getActiveBuffList().size());
    }

    @Test
    public void testPermanentTriggerAttributeBuffWithNegativeDurationWillNotExpireByTick() {
        AttributeSet base = testPlayer.getBaseAttributes();
        base.maxHp = 100;
        testPlayer.markAttributeCacheDirty();

        AttributeBuff maxHpBuff = new AttributeBuff(
                "buff_test_max_hp_flat",
                "测试生命",
                "",
                BuffType.BUFF,
                true,
                -1,
                1,
                false,
                50f,
                AttributeType.MAX_HP,
                ValueType.FLAT
        );

        buffManager.addBuff(testPlayer, maxHpBuff);

        // 生效后：100 + 50 = 150
        assertEquals(150, getCalculatedFinalAttributes().maxHp);
        assertEquals(1, testPlayer.getActiveBuffList().size());

        // -1 持续时间不应被 tick 到过期
        buffManager.tickBuffs(testPlayer);
        buffManager.tickBuffs(testPlayer);
        buffManager.tickBuffs(testPlayer);

        assertEquals(150, getCalculatedFinalAttributes().maxHp);
        assertEquals(1, testPlayer.getActiveBuffList().size());
    }
}
