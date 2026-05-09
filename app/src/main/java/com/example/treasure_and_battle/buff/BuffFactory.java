package com.example.treasure_and_battle.buff;

import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.buff.impl.control.FrozenDebuff;
import com.example.treasure_and_battle.buff.impl.control.SilencedDebuff;
import com.example.treasure_and_battle.buff.impl.defensive.DamageReductionBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.ManaRegeneratingBuff;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.RegeneratingBuff;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffTemplate;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;

public class BuffFactory {

    public static BaseBuff create(BuffTemplate template, float randomValue) {
        BuffType buffType = BuffType.valueOf(template.getBuffType());
        String buffClass = template.getBuffClass();

        if ("com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff".equals(buffClass)) {
            AttributeType attributeType = AttributeType.valueOf(template.getAttributeType());
            ValueType valueType = ValueType.valueOf(template.getValueType());
            return new AttributeBuff(
                    template.getBuffId(), template.getBuffName(), template.getDescriptionFormat(),
                    buffType, template.isDispellable(), template.getDefaultDuration(),
                    template.getMaxStackCount(), template.isRefreshOnApply(), randomValue,
                    attributeType, valueType);
        }

        if ("com.example.treasure_and_battle.buff.impl.control.FrozenDebuff".equals(buffClass)) {
            return new FrozenDebuff(
                    template.getBuffId(), template.getBuffName(), template.getDescriptionFormat(),
                    buffType, template.isDispellable(), template.getDefaultDuration(),
                    template.getMaxStackCount(), template.isRefreshOnApply(), randomValue);
        }

        if ("com.example.treasure_and_battle.buff.impl.control.SilencedDebuff".equals(buffClass)) {
            return new SilencedDebuff(
                    template.getBuffId(), template.getBuffName(), template.getDescriptionFormat(),
                    buffType, template.isDispellable(), template.getDefaultDuration(),
                    template.getMaxStackCount(), template.isRefreshOnApply(), randomValue);
        }

        if ("com.example.treasure_and_battle.buff.impl.defensive.DamageReductionBuff".equals(buffClass)) {
            return new DamageReductionBuff(
                    template.getBuffId(), template.getBuffName(), template.getDescriptionFormat(),
                    buffType, template.isDispellable(), template.getDefaultDuration(),
                    template.getMaxStackCount(), template.isRefreshOnApply(), randomValue);
        }

        if ("com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff".equals(buffClass)) {
            return new ShieldBuff(
                    template.getBuffId(), template.getBuffName(), template.getDescriptionFormat(),
                    buffType, template.isDispellable(), template.getDefaultDuration(),
                    template.getMaxStackCount(), template.isRefreshOnApply(), randomValue);
        }

        if ("com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff".equals(buffClass)) {
            return new BleedingDebuff(
                    template.getBuffId(), template.getBuffName(), template.getDescriptionFormat(),
                    buffType, template.isDispellable(), template.getDefaultDuration(),
                    template.getMaxStackCount(), template.isRefreshOnApply(), randomValue);
        }

        if ("com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff".equals(buffClass)) {
            return new BurningDebuff(
                    template.getBuffId(), template.getBuffName(), template.getDescriptionFormat(),
                    buffType, template.isDispellable(), template.getDefaultDuration(),
                    template.getMaxStackCount(), template.isRefreshOnApply(), randomValue);
        }

        if ("com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff".equals(buffClass)) {
            return new PoisoningDebuff(
                    template.getBuffId(), template.getBuffName(), template.getDescriptionFormat(),
                    buffType, template.isDispellable(), template.getDefaultDuration(),
                    template.getMaxStackCount(), template.isRefreshOnApply(), randomValue);
        }

        if ("com.example.treasure_and_battle.buff.impl.periodic.RegeneratingBuff".equals(buffClass)) {
            ValueType valueType = template.getValueType() != null
                    ? ValueType.valueOf(template.getValueType()) : ValueType.FLAT;
            return new RegeneratingBuff(
                    template.getBuffId(), template.getBuffName(), template.getDescriptionFormat(),
                    buffType, template.isDispellable(), template.getDefaultDuration(),
                    template.getMaxStackCount(), template.isRefreshOnApply(), randomValue, valueType);
        }

        if ("com.example.treasure_and_battle.buff.impl.periodic.ManaRegeneratingBuff".equals(buffClass)) {
            ValueType valueType = template.getValueType() != null
                    ? ValueType.valueOf(template.getValueType()) : ValueType.FLAT;
            return new ManaRegeneratingBuff(
                    template.getBuffId(), template.getBuffName(), template.getDescriptionFormat(),
                    buffType, template.isDispellable(), template.getDefaultDuration(),
                    template.getMaxStackCount(), template.isRefreshOnApply(), randomValue, valueType);
        }

        return null;
    }
}
