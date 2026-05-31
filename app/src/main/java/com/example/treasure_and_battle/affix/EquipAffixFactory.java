package com.example.treasure_and_battle.affix;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBattleStartAoeDamageAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBuffAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBuffAffixPercentHp;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerCritReduceApAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerOnKillRecoverAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerPurifyAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerRecoverAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerRoundStartRecoverAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerSkillCastRecoverMpAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerSkillCastReduceCdAffix;
import com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerThornsAffix;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
import com.example.treasure_and_battle.model.affix.AffixRecoverResourceType;

import com.example.treasure_and_battle.model.affix.EquipAffixScope;
import com.example.treasure_and_battle.model.affix.EquipAffixTemplate;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;

public class EquipAffixFactory {

    public static BaseEquipAffix create(EquipAffixTemplate template, Rarity targetRarity,
                                        TriggerType triggerType, EquipCategory[] categories,
                                        float randomValue, EquipAffixTemplate.RarityParam param) {
        String affixClass = template.getAffixClass();
        Rarity actualRarity = (param != null) ? Rarity.fromId(param.getRarityId()) : targetRarity;

        if ("com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix".equals(affixClass)) {
            AttributeType attributeType = AttributeType.valueOf(template.getAttributeType());
            ValueType valueType = ValueType.valueOf(template.getValueType());
            EquipAffixScope affixScope = parseAffixScope(template.getAffixScope());

            return new EquipAttributeAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue,
                    attributeType, valueType, affixScope);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBuffAffix".equals(affixClass)) {
            Integer buffTemplateId = param != null ? param.getBuffTemplateId() : null;
            if (buffTemplateId == null) {
                throw new IllegalArgumentException(
                        "EquipTriggerBuffAffix template missing buffTemplateId: " + template.getTemplateId());
            }

            AffixBuffApplyTarget applyTarget = parseApplyTarget(template.getApplyTarget());
            int applyStacks = (param != null && param.getApplyStacks() != null)
                    ? Math.max(1, param.getApplyStacks()) : 1;
            float damageToStackRatio = (param != null && param.getDamageToStackRatio() != null)
                    ? Math.max(0f, param.getDamageToStackRatio()) : 0f;

            return new EquipTriggerBuffAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue,
                    buffTemplateId, applyTarget, applyStacks, damageToStackRatio);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBuffAffixPercentHp".equals(affixClass)) {
            Integer buffTemplateId = param != null ? param.getBuffTemplateId() : null;
            if (buffTemplateId == null) {
                throw new IllegalArgumentException(
                        "EquipTriggerBuffAffixPercentHp template missing buffTemplateId: " + template.getTemplateId());
            }

            AffixBuffApplyTarget applyTarget = parseApplyTarget(template.getApplyTarget());
            int applyStacks = (param != null && param.getApplyStacks() != null)
                    ? Math.max(1, param.getApplyStacks()) : 1;
            float damageToStackRatio = 0f;

            return new EquipTriggerBuffAffixPercentHp(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue,
                    buffTemplateId, applyTarget, applyStacks, damageToStackRatio);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerRecoverAffix".equals(affixClass)) {
            AffixRecoverResourceType recoverResourceType = parseRecoverResourceType(
                    template.getRecoverResourceType());
            ValueType recoverValueType = parseRecoverValueType(template.getRecoverValueType());
            int recoverValue = (param != null && param.getRecoverValue() != null)
                    ? Math.max(0, param.getRecoverValue()) : 0;
            float damageToRecoverRatio = (param != null && param.getDamageToRecoverRatio() != null)
                    ? Math.max(0f, param.getDamageToRecoverRatio()) : 0f;

            return new EquipTriggerRecoverAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue,
                    recoverResourceType, recoverValueType, recoverValue, damageToRecoverRatio);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerBattleStartAoeDamageAffix".equals(affixClass)) {
            return new EquipTriggerBattleStartAoeDamageAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerOnKillRecoverAffix".equals(affixClass)) {
            return new EquipTriggerOnKillRecoverAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerRoundStartRecoverAffix".equals(affixClass)) {
            return new EquipTriggerRoundStartRecoverAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerPurifyAffix".equals(affixClass)) {
            return new EquipTriggerPurifyAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerCritReduceApAffix".equals(affixClass)) {
            return new EquipTriggerCritReduceApAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerThornsAffix".equals(affixClass)) {
            return new EquipTriggerThornsAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerSkillCastRecoverMpAffix".equals(affixClass)) {
            return new EquipTriggerSkillCastRecoverMpAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue);
        }

        if ("com.example.treasure_and_battle.affix.impl.equip.trigger.EquipTriggerSkillCastReduceCdAffix".equals(affixClass)) {
            return new EquipTriggerSkillCastReduceCdAffix(
                    template.getTemplateId(), template.getAffixName(), template.getDescriptionFormat(),
                    actualRarity, triggerType, categories, randomValue);
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
