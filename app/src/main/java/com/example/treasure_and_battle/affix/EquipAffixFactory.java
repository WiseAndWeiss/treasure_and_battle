package com.example.treasure_and_battle.affix;

import com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBuffAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerRecoverAffix;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
import com.example.treasure_and_battle.model.affix.AffixRecoverResourceType;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.affix.EquipAffixScope;
import com.example.treasure_and_battle.model.affix.EquipAffixTemplate;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.item.EquipCategory;

public class EquipAffixFactory {

    public static BaseEquipAffix create(EquipAffixTemplate template, Rarity targetRarity,
                                        AffixTriggerType triggerType, EquipCategory[] categories,
                                        float randomValue) {
        String affixClass = template.getAffixClass();

        if ("com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix".equals(affixClass)) {
            AttributeType attributeType = AttributeType.valueOf(template.getAttributeType());
            ValueType valueType = ValueType.valueOf(template.getValueType());
            EquipAffixScope affixScope = parseAffixScope(template.getAffixScope());

            return new EquipAttributeAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    targetRarity, triggerType, categories, randomValue,
                    attributeType, valueType, affixScope);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBuffAffix".equals(affixClass)) {
            Integer buffTemplateId = template.getBuffTemplateId();
            if (buffTemplateId == null) {
                throw new IllegalArgumentException(
                        "EquipTriggerBuffAffix template missing buffTemplateId: " + template.getTemplateId());
            }

            AffixBuffApplyTarget applyTarget = parseApplyTarget(template.getApplyTarget());
            int applyStacks = template.getApplyStacks() == null ? 1 : Math.max(1, template.getApplyStacks());
            float damageToStackRatio = template.getDamageToStackRatio() == null
                    ? 0f : Math.max(0f, template.getDamageToStackRatio());

            return new EquipTriggerBuffAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    targetRarity, triggerType, categories, randomValue,
                    buffTemplateId, applyTarget, applyStacks, damageToStackRatio);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerRecoverAffix".equals(affixClass)) {
            AffixRecoverResourceType recoverResourceType = parseRecoverResourceType(
                    template.getRecoverResourceType());
            ValueType recoverValueType = parseRecoverValueType(template.getRecoverValueType());
            int recoverValue = template.getRecoverValue() == null ? 0 : Math.max(0, template.getRecoverValue());
            float damageToRecoverRatio = template.getDamageToRecoverRatio() == null
                    ? 0f : Math.max(0f, template.getDamageToRecoverRatio());

            return new EquipTriggerRecoverAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    targetRarity, triggerType, categories, randomValue,
                    recoverResourceType, recoverValueType, recoverValue, damageToRecoverRatio);
        }

        return null;
    }

    private static EquipAffixScope parseAffixScope(String rawScope) {
        if (rawScope == null || rawScope.trim().isEmpty()) {
            return EquipAffixScope.GLOBAL;
        }
        String normalized = rawScope.trim().toUpperCase();
        if ("EQUIP_ONLY".equals(normalized)) {
            return EquipAffixScope.EQUIPMENT_ONLY;
        }
        return EquipAffixScope.valueOf(normalized);
    }

    private static AffixBuffApplyTarget parseApplyTarget(String rawTarget) {
        if (rawTarget == null || rawTarget.trim().isEmpty()) {
            return AffixBuffApplyTarget.TARGET;
        }
        return AffixBuffApplyTarget.valueOf(rawTarget.trim().toUpperCase());
    }

    private static AffixRecoverResourceType parseRecoverResourceType(String rawType) {
        if (rawType == null || rawType.trim().isEmpty()) {
            return AffixRecoverResourceType.HP;
        }
        return AffixRecoverResourceType.valueOf(rawType.trim().toUpperCase());
    }

    private static ValueType parseRecoverValueType(String rawType) {
        if (rawType == null || rawType.trim().isEmpty()) {
            return ValueType.FLAT;
        }
        return ValueType.valueOf(rawType.trim().toUpperCase());
    }
}
