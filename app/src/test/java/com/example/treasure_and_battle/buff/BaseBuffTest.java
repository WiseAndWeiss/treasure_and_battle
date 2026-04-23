package com.example.treasure_and_battle.buff;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaseBuffTest {

    @Test
    public void testTickDecreaseAndExpireAtZeroDuration() {
        DummyBuff buff = new DummyBuff("b1", 2, 3, true, 10f);

        assertFalse(buff.tick());
        assertEquals(1, buff.getRemainingDuration());

        assertTrue(buff.tick());
        assertEquals(0, buff.getRemainingDuration());
        assertTrue(buff.isExpired());
    }

    @Test
    public void testNegativeDurationBuffWillNotExpireByTick() {
        DummyBuff buff = new DummyBuff("b2", -1, 3, true, 10f);

        assertFalse(buff.tick());
        assertFalse(buff.tick());
        assertEquals(-1, buff.getRemainingDuration());
        assertFalse(buff.isExpired());
    }

    @Test
    public void testTryStackRefreshDurationAndUseHigherValue() {
        DummyBuff buff = new DummyBuff("b3", 3, 3, true, 10f);
        DummyBuff stronger = new DummyBuff("b3", 3, 3, true, 25f);

        buff.tick(); // 剩余持续时间 2
        assertEquals(2, buff.getRemainingDuration());

        buff.tryStack(stronger);
        assertEquals(2, buff.getStackCount());
        assertEquals(3, buff.getRemainingDuration()); // refreshOnApply 生效
        assertEquals(25f, buff.getBuffValue(), 0.0001f);
    }

    @Test
    public void testTryStackWithAdditionalStacksAndSetStackClamp() {
        DummyBuff buff = new DummyBuff("b4", 5, 3, false, 10f);
        DummyBuff incoming = new DummyBuff("b4", 5, 3, false, 12f);

        buff.tryStack(incoming, 5); // 1 + 5 -> 上限3
        assertEquals(3, buff.getStackCount());

        buff.setStack(100);
        assertEquals(3, buff.getStackCount());

        buff.setStack(-3);
        assertEquals(0, buff.getStackCount());
        assertTrue(buff.isExpired());
    }

    private static class DummyBuff extends BaseBuff {
        DummyBuff(String id, int maxDuration, int maxStackCount, boolean refreshOnApply, float value) {
            super(
                    id,
                    "测试Buff",
                    "值=%.1f, 层=%d",
                    BuffType.BUFF,
                    BuffTriggerType.PERMANENT,
                    true,
                    maxDuration,
                    maxStackCount,
                    refreshOnApply,
                    value
            );
        }

        @Override
        public void applyAttributeBonus(AttributeSet attributeSet) {
            // no-op
        }

        @Override
        public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
            // no-op
        }
    }
}
