package com.example.treasure_and_battle.buff.impl.attribute;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 【通用】属性修改Buff：一个类覆盖所有属性的修改
 * 核心设计：通过 AttributeType 枚举区分要修改的属性，通过 ValueType 区分固定值/百分比
 * 后续加新属性，只需要在 AttributeType 枚举里加一个，在 switch 里加一个 case，无需新建类
 */
public class AttributeBuff extends BaseBuff {
    private final AttributeType attributeType; // 【核心】要修改的属性类型
    private final ValueType valueType;         // 你已有的：固定值/百分比

    public AttributeBuff(String buffId, String buffName, String descriptionFormat,
                         BuffType buffType, boolean isDispellable, int maxDuration,
                         int maxStackCount, boolean refreshOnApply, float buffValue,
                         AttributeType attributeType, ValueType valueType) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.PERMANENT,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.attributeType = attributeType;
        this.valueType = valueType;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        float finalValue = this.buffValue * this.stackCount;

        // 【核心】通过 switch-case 根据 AttributeType 选择要修改的属性
        // 同时支持 FLAT（固定值）和 PERCENTAGE（百分比）
        switch (attributeType) {
            // ====================== 六维属性 ======================
            case STRENGTH:
                if (valueType == ValueType.FLAT) attributeSet.strength += (int) finalValue;
                else attributeSet.percentStrength += finalValue;
                break;
            case AGILITY:
                if (valueType == ValueType.FLAT) attributeSet.agility += (int) finalValue;
                else attributeSet.percentAgility += finalValue;
                break;
            case INTELLIGENCE:
                if (valueType == ValueType.FLAT) attributeSet.intelligence += (int) finalValue;
                else attributeSet.percentIntelligence += finalValue;
                break;
            case SPIRIT:
                if (valueType == ValueType.FLAT) attributeSet.spirit += (int) finalValue;
                else attributeSet.percentSpirit += finalValue;
                break;
            case PHYSIQUE:
                if (valueType == ValueType.FLAT) attributeSet.physique += (int) finalValue;
                else attributeSet.percentPhysique += finalValue;
                break;
            case LUCK:
                if (valueType == ValueType.FLAT) attributeSet.luck += (int) finalValue;
                else attributeSet.percentLuck += finalValue;
                break;

            // ====================== 核心战斗属性 ======================
            case PHYSICAL_ATK:
                if (valueType == ValueType.FLAT) attributeSet.physicalAtk += (int) finalValue;
                else attributeSet.percentPhysicalAtk += finalValue;
                break;
            case MAGICAL_ATK:
                if (valueType == ValueType.FLAT) attributeSet.magicalAtk += (int) finalValue;
                else attributeSet.percentMagicalAtk += finalValue;
                break;
            case PHYSICAL_DEF:
                if (valueType == ValueType.FLAT) attributeSet.physicalDef += (int) finalValue;
                else attributeSet.percentPhysicalDef += finalValue;
                break;
            case MAGICAL_DEF:
                if (valueType == ValueType.FLAT) attributeSet.magicalDef += (int) finalValue;
                else attributeSet.percentMagicalDef += finalValue;
                break;
            case SPEED:
                if (valueType == ValueType.FLAT) attributeSet.speed += (int) finalValue;
                else attributeSet.percentSpeed += finalValue;
                break;
            case MAX_HP:
                if (valueType == ValueType.FLAT) attributeSet.maxHp += (int) finalValue;
                else attributeSet.percentMaxHp += finalValue;
                break;
            case MAX_MP:
                if (valueType == ValueType.FLAT) attributeSet.maxMp += (int) finalValue;
                else attributeSet.percentMaxMp += finalValue;
                break;
            case MAX_ACTION_POINTS:
                // 行动点通常只有固定值，没有百分比
                if (valueType == ValueType.FLAT) attributeSet.maxActionPoints += (int) finalValue;
                break;

            // ====================== 附加战斗属性（通常只有百分比或固定值，按需选择） ======================
            case PHYSICAL_CRIT_RATE:
                attributeSet.physicalCritRate += finalValue;
                break;
            case MAGICAL_CRIT_RATE:
                attributeSet.magicalCritRate += finalValue;
                break;
            case PHYSICAL_CRIT_DMG:
                attributeSet.physicalCritDmg += finalValue;
                break;
            case MAGICAL_CRIT_DMG:
                attributeSet.magicalCritDmg += finalValue;
                break;
            case HIT_RATE:
                attributeSet.hitRate += finalValue;
                break;
            case DODGE_RATE:
                attributeSet.dodgeRate += finalValue;
                break;
            case DEBUFF_RESIST:
                attributeSet.debuffResist += finalValue;
                break;
            case DAMAGE_REDUCTION_RATE:
                attributeSet.damageReductionRate += finalValue;
                break;
        }
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        // 常驻属性Buff无触发逻辑
    }
}