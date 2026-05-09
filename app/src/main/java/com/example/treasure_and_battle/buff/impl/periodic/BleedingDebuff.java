package com.example.treasure_and_battle.buff.impl.periodic;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.damage.DamageConfig;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.manager.battle.DamageManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 流血状态Debuff
 * 效果：回合结束时触发，每层流失相当于最大生命值1%生命值。
 * 衰减机制：每回合结束时，流失生命后，层数减1。当层数衰减到0时完全解除。
 *
 * 特殊机制：流血debuff在实体身上应该是唯一的，不同来源的流血应该叠加层数而非创建新实例。
 * 因此在BuffManager中有特殊处理，确保只有唯一的BleedingDebuff实例存在。
 */
public class BleedingDebuff extends BaseBuff {

    public BleedingDebuff(String buffId, String buffName, String descriptionFormat,
                          BuffType buffType, boolean isDispellable, int maxDuration,
                          int maxStackCount, boolean refreshOnApply, float buffValue) {
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.ON_ROUND_END,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        // defaultDuration: -1 表示永久（直到层数归零）
    }

    @Override
    public boolean tick() {
        // 每回合衰减量：1层
        if (this.stackCount > 0) {
            this.stackCount -= 1;
        }

        // 当层数降为0时，isExpired() 会返回 true，由系统自动脱落
        return this.isExpired();
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 流血直接扣血，不影响基础面板属性
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        if (triggerType == TriggerType.ON_ROUND_END && this.stackCount > 0) {
            int damage = (int) (owner.getFinalAttributes().maxHp * 0.01 * this.stackCount);
            DamageManager.getInstance(owner.getContext())
                    .dealDamage(DamageConfig.buffTrue(), null, owner, damage, context);

            context.addLogWithMeta(LogType.DAMAGE, owner,
                    "【流血】[%s] 当前层数 %d，损失了 %d 点生命值！剩余生命：(%d/%d)",
                    owner.getClass().getSimpleName(), this.stackCount, damage,
                    owner.getCurrentHp(), owner.getFinalAttributes().maxHp);
        }
    }

    /**
     * 流血buff的特殊堆叠方法
     * 由于流血是唯一的，新添加的流血应该叠加层数而不是创建新实例
     */
    public void stackBleeding(int additionalStacks) {
        if (additionalStacks > 0) {
            this.stackCount += additionalStacks;
            // 确保不超过最大层数限制
            if (this.maxStackCount > 0 && this.stackCount > this.maxStackCount) {
                this.stackCount = this.maxStackCount;
            }
        }
    }
}
