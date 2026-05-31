package com.example.treasure_and_battle.model.buff;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * BuffType 单元测试
 * 测试Buff类型枚举的值和类型判断
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class BuffTypeTest {

    // ====================== 枚举值测试 ======================

    @Test
    public void testBuffType_ENUM_Values() {
        BuffType[] types = BuffType.values();

        assertEquals(2, types.length);
    }

    @Test
    public void testBuffType_BUFF_Exists() {
        assertEquals("BUFF", BuffType.BUFF.name());
    }

    @Test
    public void testBuffType_DEBUFF_Exists() {
        assertEquals("DEBUFF", BuffType.DEBUFF.name());
    }

    @Test
    public void testBuffType_Values_AreDistinct() {
        assertNotEquals(BuffType.BUFF, BuffType.DEBUFF);
    }

    // ====================== 字符串转换测试 ======================

    @Test
    public void testBuffType_ValueOf_BUFF() {
        BuffType type = BuffType.valueOf("BUFF");
        assertEquals(BuffType.BUFF, type);
    }

    @Test
    public void testBuffType_ValueOf_DEBUFF() {
        BuffType type = BuffType.valueOf("DEBUFF");
        assertEquals(BuffType.DEBUFF, type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuffType_ValueOf_Invalid_ThrowsException() {
        BuffType.valueOf("INVALID_TYPE");
    }

    // ====================== 枚举顺序测试 ======================

    @Test
    public void testBuffType_Ordinal() {
        assertEquals(0, BuffType.BUFF.ordinal());
        assertEquals(1, BuffType.DEBUFF.ordinal());
    }

    // ====================== 枚举比较测试 ======================

    @Test
    public void testBuffType_Equals() {
        assertEquals(BuffType.BUFF, BuffType.BUFF);
        assertEquals(BuffType.DEBUFF, BuffType.DEBUFF);
    }

    @Test
    public void testBuffType_NotEquals() {
        assertNotEquals(BuffType.BUFF, BuffType.DEBUFF);
    }

    // ====================== 使用场景测试 ======================

    @Test
    public void testBuffType_InSwitchStatement() {
        BuffType type = BuffType.BUFF;
        String result = "";

        switch (type) {
            case BUFF:
                result = "增益";
                break;
            case DEBUFF:
                result = "减益";
                break;
        }

        assertEquals("增益", result);
    }

    @Test
    public void testBuffType_InConditional() {
        assertTrue(isBuff(BuffType.BUFF));
        assertFalse(isBuff(BuffType.DEBUFF));
        assertTrue(isDebuff(BuffType.DEBUFF));
        assertFalse(isDebuff(BuffType.BUFF));
    }

    // 辅助方法
    private boolean isBuff(BuffType type) {
        return type == BuffType.BUFF;
    }

    private boolean isDebuff(BuffType type) {
        return type == BuffType.DEBUFF;
    }

    // ====================== 字符串表示测试 ======================

    @Test
    public void testBuffType_ToString() {
        assertEquals("BUFF", BuffType.BUFF.toString());
        assertEquals("DEBUFF", BuffType.DEBUFF.toString());
    }

    // ====================== 实际使用测试 ======================

    @Test
    public void testBuffType_StringComparison_WithConfig() {
        // 模拟从JSON配置加载的字符串
        String configBuffType = "BUFF";
        String configDebuffType = "DEBUFF";
        String invalidType = "INVALID";

        // 字符串可以匹配枚举名
        assertEquals(BuffType.BUFF.name(), configBuffType);
        assertEquals(BuffType.DEBUFF.name(), configDebuffType);
        assertNotEquals(BuffType.BUFF.name(), invalidType);
    }
}
