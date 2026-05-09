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
 * 中毒状态Debuff
 * 效果：回合结束时触发，每层流失1点生命值。
 * 衰减机制：每回合结束时，流失生命后，层数减半（向上取整）。当层数衰减到0时完全解除。
 */
public class PoisoningDebuff extends BaseBuff {

    public PoisoningDebuff(String buffId, String buffName, String descriptionFormat,
                           BuffType buffType, boolean isDispellable, int maxDuration,
                           int maxStackCount, boolean refreshOnApply, float buffValue) {
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.ON_ROUND_END,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        // 对于中毒，我们可能不需要maxDuration（或者是永久回合，直到层数掉光）。
        // 如果你需要它完全依赖层数衰减，可以在配置文件中将默认持续回合设为 -1（不基于回合移除）。
    }

    @Override
    public boolean tick() {
        // 每回合衰减量：当前层数的一半（向上取整）。例如：3层会衰减 ceil(1.5)=2层，剩下1层。1层衰减 ceil(0.5)=1层，剩下0层。
        int decayAmount = (int) Math.ceil(this.stackCount / 2.0);
        this.stackCount -= decayAmount;
        
        // 当层数降为0时，isExpired() 会返回 true，由系统自动脱落
        return this.isExpired();
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 中毒直接扣血，不影响基础面板属性
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        if (triggerType == TriggerType.ON_ROUND_END) {
            int damage = this.stackCount;
            DamageManager.getInstance(owner.getContext())
                    .dealDamage(DamageConfig.buffTrue(), null, owner, damage, context);

            context.addLogWithMeta(LogType.DAMAGE, owner,
                    "【中毒】[%s] 当前层数 %d，损失了 %d 点生命值！剩余生命：(%d/%d)",
                    owner.getClass().getSimpleName(), this.stackCount, damage,
                    owner.getCurrentHp(), owner.getFinalAttributes().maxHp);
        }
    }
}
