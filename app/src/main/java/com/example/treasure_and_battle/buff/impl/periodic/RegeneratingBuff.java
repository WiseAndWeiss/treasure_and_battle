package com.example.treasure_and_battle.buff.impl.periodic;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 血量回复Buff
 * 效果：回合结束时触发，根据其是百分比/固定值回复生命值。
 * 衰减机制：每回合结束时触发，层数不衰减，持续回合数衰减1。当持续回合数为0时，Buff自动脱落。
 */
public class RegeneratingBuff extends BaseBuff {
    private final ValueType valueType;

    public RegeneratingBuff(String buffId, String buffName, String descriptionFormat,
                            BuffType buffType, boolean isDispellable, int maxDuration,
                            int maxStackCount, boolean refreshOnApply, float buffValue, ValueType valueType) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.ON_ROUND_END,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.valueType = valueType;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 这个Buff没有常驻属性加成，所有逻辑都在onTrigger中处理
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        if (triggerType == BuffTriggerType.ON_ROUND_END) {
            if (this.valueType == ValueType.FLAT) {
                int healAmount = (int) (this.buffValue * this.stackCount);
                owner.healHp(healAmount);
                context.addLogWithMeta(LogType.HEAL, owner,
                        "【回复】[%s] 回复了 %d 点生命值！当前生命：(%d/%d)",
                        owner.getClass().getSimpleName(), healAmount,
                        owner.getCurrentHp(), owner.getFinalAttributes().maxHp);
            } else if (this.valueType == ValueType.PERCENTAGE) {
                int healAmount = (int) (owner.getFinalAttributes().maxHp * this.buffValue * this.stackCount);
                owner.healHp(healAmount);
                context.addLogWithMeta(LogType.HEAL, owner,
                        "【回复】[%s] 回复了 %d 点生命值（%d%%）！当前生命：(%d/%d)",
                        owner.getClass().getSimpleName(), healAmount, (int)(this.buffValue * 100),
                        owner.getCurrentHp(), owner.getFinalAttributes().maxHp);
            }
        }
    }
}
