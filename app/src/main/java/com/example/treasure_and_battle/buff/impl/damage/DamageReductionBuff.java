package com.example.treasure_and_battle.buff.impl.damage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 伤害降低Buff
 * 效果：受到的所有伤害降低指定百分比
 */
public class DamageReductionBuff extends BaseBuff {
    private final int damageReductionPercent;

    public DamageReductionBuff(String buffId, String buffName, String descriptionFormat,
                               BuffType buffType, boolean isDispellable, int maxDuration,
                               int maxStackCount, boolean refreshOnApply, float buffValue,
                               int damageReductionPercent) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.PERMANENT,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.damageReductionPercent = damageReductionPercent;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 不直接修改属性，伤害降低在onBeforeDamageReceived中处理
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        // 在ON_BEFORE_DAMAGE_TAKEN时通过BuffManager处理
    }

    @Override
    public int onBeforeDamageReceived(BattleEntity owner, BattleEntity attacker, int damage, BattleContext context) {
        if (damageReductionPercent > 0 && damage > 0) {
            int reducedDamage = (int) (damage * (100 - damageReductionPercent) / 100.0f);
            return Math.max(1, reducedDamage);
        }
        return damage;
    }

    public int getDamageReductionPercent() {
        return damageReductionPercent;
    }
}
