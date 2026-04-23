package com.example.treasure_and_battle.buff.impl.periodic;

import static com.example.treasure_and_battle.battle.DamageType.TRUE;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
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
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.ON_ROUND_END,
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
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        if (triggerType == BuffTriggerType.ON_ROUND_END) {
            // 每层流失1点生命值
            int damage = this.stackCount; 
            owner.takeDamage(damage, TRUE);
            
            // 加入战斗日志
            context.addLogWithMeta(LogType.DAMAGE, owner, 
                    "【中毒】[%s] 当前层数 %d，损失了 %d 点生命值！剩余生命：(%d/%d)", 
                    owner.getClass().getSimpleName(), this.stackCount, damage, 
                    owner.getCurrentHp(), owner.getFinalAttributes().maxHp);
                    
            // 说明：我们把 tick 减半机制放到了这里之后，由框架统一在 onRoundEnd 中调用 tickBuffs 来衰减层数。
        }
    }
}
