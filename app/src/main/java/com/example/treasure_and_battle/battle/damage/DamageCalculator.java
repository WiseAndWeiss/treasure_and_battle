package com.example.treasure_and_battle.battle.damage;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.utils.RandomUtils;

/**
 * 伤害计算工具类
 * 统一物理/魔法伤害的核心计算逻辑（命中、暴击、防御减免）
 */
public class DamageCalculator {

    public static float clampProbability(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static float calculateHitChance(AttributeSet attackerAttr, AttributeSet targetAttr) {
        return clampProbability(attackerAttr.hitRate - targetAttr.dodgeRate);
    }

    /**
     * 计算伤害核心流程：命中判定 → 暴击判定 → 防御减免
     */
    public static DamageResult calculate(AttributeSet attackerAttr, AttributeSet targetAttr,
                                          int baseDamage, boolean isPhysical) {
        DamageResult result = new DamageResult();
        result.rawDamage = baseDamage;

        float hitChance = calculateHitChance(attackerAttr, targetAttr);
        result.isHit = RandomUtils.checkProbability(hitChance);

        if (!result.isHit) {
            result.rawDamage = 0;
            result.finalDamage = 0;
            result.isCriticalHit = false;
            return result;
        }

        float critRate = clampProbability(isPhysical ? attackerAttr.physicalCritRate : attackerAttr.magicalCritRate);
        result.isCriticalHit = RandomUtils.checkProbability(critRate);

        if (result.isCriticalHit) {
            float critDmg = isPhysical ? attackerAttr.physicalCritDmg : attackerAttr.magicalCritDmg;
            result.rawDamage *= critDmg;
        }

        int defense = isPhysical ? targetAttr.physicalDef : targetAttr.magicalDef;
        result.finalDamage = Math.max(1, result.rawDamage - defense);

        return result;
    }

    public static class DamageResult {
        public boolean isHit;
        public boolean isCriticalHit;
        public int rawDamage;
        public int finalDamage;
    }
}
