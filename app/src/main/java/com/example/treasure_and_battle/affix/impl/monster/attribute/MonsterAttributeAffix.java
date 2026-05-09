package com.example.treasure_and_battle.affix.impl.monster.attribute;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.battle.BattleContext;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

public class MonsterAttributeAffix extends BaseMonsterAffix {
    private final AttributeType attributeType;
    private final ValueType valueType;

    public MonsterAttributeAffix(int affixId, String affixName, String description, Rarity rarity,
                                 TriggerType triggerType, float value, AttributeType attributeType, ValueType valueType) {
        super(affixId, affixName, description, rarity, triggerType, value);
        this.attributeType = attributeType;
        this.valueType = valueType;
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        // 常驻属性词缀不会在战斗中按特定时机触发行为，仅在属性计算时生效
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        switch (attributeType) {
            case STRENGTH:
                if (valueType == ValueType.FLAT) attributeSet.strength += (int) affixValue;
                else attributeSet.percentStrength += affixValue;
                break;
            case AGILITY:
                if (valueType == ValueType.FLAT) attributeSet.agility += (int) affixValue;
                else attributeSet.percentAgility += affixValue;
                break;
            case INTELLIGENCE:
                if (valueType == ValueType.FLAT) attributeSet.intelligence += (int) affixValue;
                else attributeSet.percentIntelligence += affixValue;
                break;
            case SPIRIT:
                if (valueType == ValueType.FLAT) attributeSet.spirit += (int) affixValue;
                else attributeSet.percentSpirit += affixValue;
                break;
            case PHYSIQUE:
                if (valueType == ValueType.FLAT) attributeSet.physique += (int) affixValue;
                else attributeSet.percentPhysique += affixValue;
                break;
            case LUCK:
                if (valueType == ValueType.FLAT) attributeSet.luck += (int) affixValue;
                else attributeSet.percentLuck += affixValue;
                break;

            case PHYSICAL_ATK:
                if (valueType == ValueType.FLAT) attributeSet.physicalAtk += (int) affixValue;
                else attributeSet.percentPhysicalAtk += affixValue;
                break;
            case MAGICAL_ATK:
                if (valueType == ValueType.FLAT) attributeSet.magicalAtk += (int) affixValue;
                else attributeSet.percentMagicalAtk += affixValue;
                break;
            case PHYSICAL_DEF:
                if (valueType == ValueType.FLAT) attributeSet.physicalDef += (int) affixValue;
                else attributeSet.percentPhysicalDef += affixValue;
                break;
            case MAGICAL_DEF:
                if (valueType == ValueType.FLAT) attributeSet.magicalDef += (int) affixValue;
                else attributeSet.percentMagicalDef += affixValue;
                break;
            case SPEED:
                if (valueType == ValueType.FLAT) attributeSet.speed += (int) affixValue;
                else attributeSet.percentSpeed += affixValue;
                break;
            case MAX_HP:
                if (valueType == ValueType.FLAT) attributeSet.maxHp += (int) affixValue;
                else attributeSet.percentMaxHp += affixValue;
                break;
            case MAX_MP:
                if (valueType == ValueType.FLAT) attributeSet.maxMp += (int) affixValue;
                else attributeSet.percentMaxMp += affixValue;
                break;
            case MAX_ACTION_POINTS:
                if (valueType == ValueType.FLAT) attributeSet.maxActionPoints += (int) affixValue;
                break;

            case PHYSICAL_CRIT_RATE:
                attributeSet.physicalCritRate += affixValue;
                break;
            case MAGICAL_CRIT_RATE:
                attributeSet.magicalCritRate += affixValue;
                break;
            case PHYSICAL_CRIT_DMG:
                attributeSet.physicalCritDmg += affixValue;
                break;
            case MAGICAL_CRIT_DMG:
                attributeSet.magicalCritDmg += affixValue;
                break;
            case HIT_RATE:
                attributeSet.hitRate += affixValue;
                break;
            case DODGE_RATE:
                attributeSet.dodgeRate += affixValue;
                break;
            case DEBUFF_RESIST:
                attributeSet.debuffResist += affixValue;
                break;
            case DAMAGE_REDUCTION_RATE:
                attributeSet.damageReductionRate += affixValue;
                break;
        }
    }

     @Override
     public String getDescription() {
        if (valueType == ValueType.PERCENTAGE) {
            return String.format(description, affixValue * 100);
        }
         return String.format(description, affixValue);
     }
}
