package com.example.treasure_and_battle.buff.impl.defensive;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 减伤Buff
 * 设计逻辑：计次减伤，表示“接下来受到的N次伤害，每次降低X%”。
 * 使用 stackCount 作为剩余触发次数，由 BattleManager 在受击结算流水线中主动调用。
 */
public class DamageReductionBuff extends BaseBuff {

    public DamageReductionBuff(String buffId, String buffName, String descriptionFormat,
                           BuffType buffType, boolean isDispellable, int maxDuration,
                           int maxStackCount, boolean refreshOnApply, float buffValue) {
        // 计次减伤不依赖标准 trigger 调度，由 BattleManager 作为拦截器主动消费。
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.PERMANENT,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 计次减伤不直接改面板属性
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        // 计次减伤无标准触发逻辑
    }

    /**
     * 对本次受击进行减伤并消费1次次数，返回减伤后的伤害。
     */
    public int reduceDamageForOneHit(int incomingDamage, BattleEntity owner, BattleContext context) {
        if (incomingDamage <= 0 || this.stackCount <= 0) {
            return Math.max(0, incomingDamage);
        }

        float reductionRate = this.buffValue; // 直接使用buffValue作为减伤百分比
        reductionRate = Math.max(0f, Math.min(1f, reductionRate));

        int reducedDamage = Math.max(0, Math.round(incomingDamage * (1f - reductionRate)));
        int reducedAmount = incomingDamage - reducedDamage;

        this.stackCount -= 1;

        context.addLogWithMeta(LogType.BUFF, owner,
                "【计次减伤】[%s] 的 [%s] 生效，减免 %d 点伤害（%.1f%%），剩余触发次数：%d",
                owner.getClass().getSimpleName(), this.buffName, reducedAmount, reductionRate * 100f, this.stackCount);

        return reducedDamage;
    }
}
