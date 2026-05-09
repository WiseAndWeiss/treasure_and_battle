package com.example.treasure_and_battle.affix;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.affix.impl.monster.attribute.MonsterAttributeAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerBuffAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerRecoverAffix;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
import com.example.treasure_and_battle.model.affix.AffixRecoverResourceType;

import com.example.treasure_and_battle.model.affix.MonsterAffixTemplate;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;

public class MonsterAffixFactory {

    public static BaseMonsterAffix create(MonsterAffixTemplate template, Rarity rarity,
                                   TriggerType triggerType, float randomValue) {
        String affixClass = template.getAffixClass();

        if ("com.example.treasure_and_battle.affix.impl.monster.attribute.MonsterAttributeAffix".equals(affixClass)) {
            AttributeType attributeType = AttributeType.valueOf(template.getAttributeType());
            ValueType valueType = parseValueType(template.getValueType());
            return new MonsterAttributeAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    rarity, triggerType, randomValue, attributeType, valueType);
        }

        if ("com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerBuffAffix".equals(affixClass)) {
            Integer buffTemplateId = template.getBuffTemplateId();
            if (buffTemplateId == null) {
                throw new IllegalArgumentException(
                        "MonsterTriggerBuffAffix template missing buffTemplateId: " + template.getTemplateId());
            }
            AffixBuffApplyTarget applyTarget = parseApplyTarget(template.getApplyTarget());
            int applyStacks = template.getApplyStacks() == null ? 1 : Math.max(1, template.getApplyStacks());
            float damageToStackRatio = template.getDamageToStackRatio() == null
                    ? 0f : Math.max(0f, template.getDamageToStackRatio());

            return new MonsterTriggerBuffAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    rarity, triggerType, randomValue,
                    buffTemplateId, applyTarget, applyStacks, damageToStackRatio);
        }

        if ("com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerRecoverAffix".equals(affixClass)) {
            AffixRecoverResourceType recoverResourceType = parseRecoverResourceType(
                    template.getRecoverResourceType());
            ValueType recoverValueType = parseValueType(template.getRecoverValueType());
            int recoverValue = template.getRecoverValue() == null ? 0 : Math.max(0, template.getRecoverValue());
            float damageToRecoverRatio = template.getDamageToRecoverRatio() == null
                    ? 0f : Math.max(0f, template.getDamageToRecoverRatio());

            return new MonsterTriggerRecoverAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    rarity, triggerType, randomValue,
                    recoverResourceType, recoverValueType, recoverValue, damageToRecoverRatio);
        }

        return null;
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

    private static ValueType parseValueType(String rawType) {
        if (rawType == null || rawType.trim().isEmpty()) {
            return ValueType.FLAT;
        }
        return ValueType.valueOf(rawType.trim().toUpperCase());
    }
}
