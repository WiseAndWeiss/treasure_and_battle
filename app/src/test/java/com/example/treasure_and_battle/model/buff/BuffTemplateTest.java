package com.example.treasure_and_battle.model.buff;

import com.example.treasure_and_battle.model.common.TriggerType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * BuffTemplate 单元测试
 * 测试Buff模板的数据验证、Buff创建
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class BuffTemplateTest {

    // ====================== 基础属性测试 ======================

    @Test
    public void testBuffTemplate_GettersAndSetters() {
        BuffTemplate template = new BuffTemplate();

        template.setTemplateId(1001);
        template.setBuffId("burning");
        template.setBuffName("燃烧");
        template.setDescriptionFormat("每回合受到%d点火焰伤害");
        template.setBuffType("DEBUFF");
        template.setTriggerType(TriggerType.ON_ROUND_END);
        template.setDispellable(true);
        template.setDefaultDuration(3);
        template.setMaxStackCount(5);
        template.setRefreshOnApply(true);
        template.setMinValue(5.0f);
        template.setMaxValue(10.0f);
        template.setBuffClass("BurningDebuff");
        template.setAttributeType("physicalAtk");
        template.setValueType("PERCENTAGE");

        assertEquals(1001, template.getTemplateId());
        assertEquals("burning", template.getBuffId());
        assertEquals("燃烧", template.getBuffName());
        assertEquals("每回合受到%d点火焰伤害", template.getDescriptionFormat());
        assertEquals("DEBUFF", template.getBuffType());
        assertEquals(TriggerType.ON_ROUND_END, template.getTriggerType());
        assertTrue(template.isDispellable());
        assertEquals(3, template.getDefaultDuration());
        assertEquals(5, template.getMaxStackCount());
        assertTrue(template.isRefreshOnApply());
        assertEquals(5.0f, template.getMinValue(), 0.001f);
        assertEquals(10.0f, template.getMaxValue(), 0.001f);
        assertEquals("BurningDebuff", template.getBuffClass());
        assertEquals("physicalAtk", template.getAttributeType());
        assertEquals("PERCENTAGE", template.getValueType());
    }

    @Test
    public void testBuffTemplate_DefaultValues() {
        BuffTemplate template = new BuffTemplate();

        assertEquals(0, template.getTemplateId());
        assertNull(template.getBuffId());
        assertNull(template.getBuffName());
        assertNull(template.getDescriptionFormat());
        assertNull(template.getBuffType());
        assertNull(template.getTriggerType());
        assertFalse(template.isDispellable());
        assertEquals(0, template.getDefaultDuration());
        assertEquals(0, template.getMaxStackCount());
        assertFalse(template.isRefreshOnApply());
        assertEquals(0.0f, template.getMinValue(), 0.001f);
        assertEquals(0.0f, template.getMaxValue(), 0.001f);
        assertNull(template.getBuffClass());
        assertNull(template.getAttributeType());
        assertNull(template.getValueType());
    }

    // ====================== 特殊场景测试 ======================

    @Test
    public void testBuffTemplate_CanBePermanent_WhenDurationNegative() {
        BuffTemplate template = new BuffTemplate();
        template.setBuffId("permanent_buff");
        template.setDefaultDuration(-1); // 负数表示永久

        assertEquals(-1, template.getDefaultDuration());
    }

    @Test
    public void testBuffTemplate_CanHaveZeroMaxStack() {
        BuffTemplate template = new BuffTemplate();
        template.setMaxStackCount(0); // 表示不可堆叠

        assertEquals(0, template.getMaxStackCount());
    }

    @Test
    public void testBuffTemplate_CanHaveZeroDuration() {
        BuffTemplate template = new BuffTemplate();
        template.setDefaultDuration(0); // 瞬时效果

        assertEquals(0, template.getDefaultDuration());
    }

    @Test
    public void testBuffTemplate_IndisposableFlag() {
        BuffTemplate template = new BuffTemplate();
        template.setDispellable(false); // 不可驱散

        assertFalse(template.isDispellable());
    }

    @Test
    public void testBuffTemplate_NoRefreshOnApply() {
        BuffTemplate template = new BuffTemplate();
        template.setRefreshOnApply(false); // 重新应用时不刷新

        assertFalse(template.isRefreshOnApply());
    }

    // ====================== 数值范围测试 ======================

    @Test
    public void testBuffTemplate_MinAndMaxValues_CanBeSame() {
        BuffTemplate template = new BuffTemplate();
        template.setMinValue(10.0f);
        template.setMaxValue(10.0f); // 固定值

        assertEquals(10.0f, template.getMinValue(), 0.001f);
        assertEquals(10.0f, template.getMaxValue(), 0.001f);
    }

    @Test
    public void testBuffTemplate_MinAndMaxValues_CanBeRange() {
        BuffTemplate template = new BuffTemplate();
        template.setMinValue(5.0f);
        template.setMaxValue(15.0f); // 随机范围

        assertEquals(5.0f, template.getMinValue(), 0.001f);
        assertEquals(15.0f, template.getMaxValue(), 0.001f);
    }

    @Test
    public void testBuffTemplate_NegativeValueAllowed() {
        BuffTemplate template = new BuffTemplate();
        template.setMinValue(-20.0f);
        template.setMaxValue(-10.0f); // 负值（减益效果）

        assertEquals(-20.0f, template.getMinValue(), 0.001f);
        assertEquals(-10.0f, template.getMaxValue(), 0.001f);
    }

    // ====================== 触发类型测试 ======================

    @Test
    public void testBuffTemplate_CanHaveVariousTriggerTypes() {
        BuffTemplate template1 = new BuffTemplate();
        template1.setTriggerType(TriggerType.ON_ROUND_START);
        assertEquals(TriggerType.ON_ROUND_START, template1.getTriggerType());

        BuffTemplate template2 = new BuffTemplate();
        template2.setTriggerType(TriggerType.ON_ATTACK);
        assertEquals(TriggerType.ON_ATTACK, template2.getTriggerType());

        BuffTemplate template3 = new BuffTemplate();
        template3.setTriggerType(TriggerType.ON_BEFORE_DAMAGE_TAKEN);
        assertEquals(TriggerType.ON_BEFORE_DAMAGE_TAKEN, template3.getTriggerType());
    }

    @Test
    public void testBuffTemplate_TriggerType_CanBeNull() {
        BuffTemplate template = new BuffTemplate();
        // 某些被动Buff可能不需要触发时机

        assertNull(template.getTriggerType());
    }

    // ====================== 类名和属性类型测试 ======================

    @Test
    public void testBuffTemplate_ClassName_ForReflection() {
        BuffTemplate template = new BuffTemplate();
        template.setBuffClass("com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff");

        assertEquals("com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff", template.getBuffClass());
    }

    @Test
    public void testBuffTemplate_AttributeType_ForAttributeBuff() {
        BuffTemplate template = new BuffTemplate();
        template.setAttributeType("strength");
        template.setValueType("FIXED");

        assertEquals("strength", template.getAttributeType());
        assertEquals("FIXED", template.getValueType());
    }

    @Test
    public void testBuffTemplate_AttributeType_CanBeNull() {
        BuffTemplate template = new BuffTemplate();
        // 非属性Buff可能不需要attributeType

        assertNull(template.getAttributeType());
        assertNull(template.getValueType());
    }
}
