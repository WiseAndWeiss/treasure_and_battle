package com.example.treasure_and_battle.buff.impl.periodic;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 流血状态Debuff
 * 效果：回合结束时触发，每层流失相当于最大生命值1%生命值。
 * 衰减机制：每回合结束时，流失生命后，层数减1。当层数衰减到0时完全解除。
 */
public class BleedingDebuff extends BaseBuff {

    public BleedingDebuff(String buffId, String buffName, String descriptionFormat,
                          BuffType buffType, boolean isDispellable, int maxDuration,
                          int maxStackCount, boolean refreshOnApply, float buffValue) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.ON_ROUND_END,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        // 对于流血，我们可能不需要maxDuration（或者是永久回合，直到层数掉光）。
        // 如果你需要它完全依赖层数衰减，可以在配置文件中将默认持续回合设为 -1（不基于回合移除）。
    }

    @Override
    public boolean tick() {
        // 每回合衰减量：1层
        this.stackCount -= 1;

        // 当层数降为0时，isExpired() 会返回 true，由系统自动脱落
        return this.isExpired();
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 流血直接扣血，不影响基础面板属性
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        if (triggerType == BuffTriggerType.ON_ROUND_END) {
            // 每层流失1%生命值
            int damage = (int) (owner.getFinalAttributes().maxHp * 0.01 * this.stackCount);
            owner.takeDamage(damage);

            // 加入战斗日志
            context.addLogWithMeta(LogType.DAMAGE, owner,
                    "【流血】[%s] 当前层数 %d，损失了 %d 点生命值！剩余生命：(%d/%d)",
                    owner.getClass().getSimpleName(), this.stackCount, damage,
                    owner.getCurrentHp(), owner.getFinalAttributes().maxHp);

        }
    }
}
