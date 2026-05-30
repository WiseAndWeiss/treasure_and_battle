package com.example.treasure_and_battle.affix;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.affix.impl.monster.attribute.MonsterAttributeAffix;
import com.example.treasure_and_battle.affix.impl.monster.defensive.MonsterDamageCapAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerAoeBuffAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerBuffAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerManaBurnAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerOnDeathExplodeAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerRecoverAffix;
import com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerRoundStartRecoverAffix;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
import com.example.treasure_and_battle.model.affix.AffixRecoverResourceType;

import com.example.treasure_and_battle.model.affix.MonsterAffixTemplate;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;

public class MonsterAffixFactory {

    public static BaseMonsterAffix create(MonsterAffixTemplate template, Rarity rarity,
                                   TriggerType triggerType, float randomValue,
                                   MonsterAffixTemplate.RarityParam param) {
        String affixClass = template.getAffixClass();
        Rarity actualRarity = (param != null) ? Rarity.fromId(param.getRarityId()) : rarity;

        if ("com.example.treasure_and_battle.affix.impl.monster.attribute.MonsterAttributeAffix".equals(affixClass)) {
            AttributeType attributeType = AttributeType.valueOf(template.getAttributeType());
            ValueType valueType = parseValueType(template.getValueType());
            return new MonsterAttributeAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, randomValue, attributeType, valueType);
        }

        if ("com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerBuffAffix".equals(affixClass)) {
            Integer buffTemplateId = param != null ? param.getBuffTemplateId() : null;
            if (buffTemplateId == null) {
                throw new IllegalArgumentException(
                        "MonsterTriggerBuffAffix template missing buffTemplateId: " + template.getTemplateId());
            }
            AffixBuffApplyTarget applyTarget = parseApplyTarget(template.getApplyTarget());
            int applyStacks = (param != null && param.getApplyStacks() != null)
                    ? Math.max(1, param.getApplyStacks()) : 1;
            float damageToStackRatio = (param != null && param.getDamageToStackRatio() != null)
                    ? Math.max(0f, param.getDamageToStackRatio()) : 0f;

            return new MonsterTriggerBuffAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, randomValue,
                    buffTemplateId, applyTarget, applyStacks, damageToStackRatio);
        }

        if ("com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerRecoverAffix".equals(affixClass)) {
            AffixRecoverResourceType recoverResourceType = parseRecoverResourceType(
                    template.getRecoverResourceType());
            ValueType recoverValueType = parseValueType(template.getRecoverValueType());
            int recoverValue = (param != null && param.getRecoverValue() != null)
                    ? Math.max(0, param.getRecoverValue()) : 0;
            float damageToRecoverRatio = (param != null && param.getDamageToRecoverRatio() != null)
                    ? Math.max(0f, param.getDamageToRecoverRatio()) : 0f;

            return new MonsterTriggerRecoverAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, randomValue,
                    recoverResourceType, recoverValueType, recoverValue, damageToRecoverRatio);
        }

        if ("com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerRoundStartRecoverAffix".equals(affixClass)) {
            return new MonsterTriggerRoundStartRecoverAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, randomValue);
        }

        if ("com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerAoeBuffAffix".equals(affixClass)) {
            Integer buffTemplateId = param != null ? param.getBuffTemplateId() : null;
            if (buffTemplateId == null) {
                throw new IllegalArgumentException(
                        "MonsterTriggerAoeBuffAffix template missing buffTemplateId: " + template.getTemplateId());
            }
            int applyStacks = (param != null && param.getApplyStacks() != null)
                    ? Math.max(1, param.getApplyStacks()) : 1;

            return new MonsterTriggerAoeBuffAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, randomValue,
                    buffTemplateId, applyStacks);
        }

        if ("com.example.treasure_and_battle.affix.impl.monster.defensive.MonsterDamageCapAffix".equals(affixClass)) {
            return new MonsterDamageCapAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, randomValue);
        }

        if ("com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerManaBurnAffix".equals(affixClass)) {
            return new MonsterTriggerManaBurnAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, randomValue);
        }

        if ("com.example.treasure_and_battle.affix.impl.monster.trigger.MonsterTriggerOnDeathExplodeAffix".equals(affixClass)) {
            return new MonsterTriggerOnDeathExplodeAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, randomValue);
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
