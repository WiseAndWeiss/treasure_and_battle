package com.example.treasure_and_battle.buff.impl.attribute;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AttributeBuffTest {

    @Test
    public void testFlatStrengthBonusWithStacks() {
        AttributeBuff buff = new AttributeBuff(
                "attr_strength_flat",
                "力量提升",
                "",
                BuffType.BUFF,
                true,
                5,
                5,
                false,
                10f,
                AttributeType.STRENGTH,
                ValueType.FLAT
        );

        buff.tryStack(buff, 2); // 1 + 2 = 3层

        AttributeSet set = new AttributeSet();
        buff.applyAttributeBonus(set);

        assertEquals(30, set.strength);
    }

    @Test
    public void testPercentagePhysicalAttackBonusWithStacks() {
        AttributeBuff buff = new AttributeBuff(
                "attr_patk_percent",
                "物攻提升",
                "",
                BuffType.BUFF,
                true,
                5,
                5,
                false,
                0.10f,
                AttributeType.PHYSICAL_ATK,
                ValueType.PERCENTAGE
        );

        buff.tryStack(buff); // 2层

        AttributeSet set = new AttributeSet();
        buff.applyAttributeBonus(set);

        assertEquals(0.20f, set.percentPhysicalAtk, 0.0001f);
    }

    @Test
    public void testFlatAndPercentageTargetDifferentFields() {
        AttributeBuff flatBuff = new AttributeBuff(
                "attr_hp_flat",
                "生命提升",
                "",
                BuffType.BUFF,
                true,
                5,
                2,
                false,
                50f,
                AttributeType.MAX_HP,
                ValueType.FLAT
        );

        AttributeBuff percentBuff = new AttributeBuff(
                "attr_hp_percent",
                "生命百分比提升",
                "",
                BuffType.BUFF,
                true,
                5,
                2,
                false,
                0.25f,
                AttributeType.MAX_HP,
                ValueType.PERCENTAGE
        );

        AttributeSet set = new AttributeSet();
        flatBuff.applyAttributeBonus(set);
        percentBuff.applyAttributeBonus(set);

        assertEquals(50, set.maxHp);
        assertEquals(0.25f, set.percentMaxHp, 0.0001f);
    }
}
