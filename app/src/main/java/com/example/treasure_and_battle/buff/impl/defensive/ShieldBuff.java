package com.example.treasure_and_battle.buff.impl.defensive;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 护盾Buff
 * 特殊机制：层数即为护盾值（抵挡具体伤害）。受击时被特殊的独立逻辑拦截消费伤害。
 * 如果层数归零，护盾碎裂（isExpired会由于stackCount <= 0返回true被自动清除）
 */
public class ShieldBuff extends BaseBuff {

    public ShieldBuff(String buffId, String buffName, String descriptionFormat,
                      BuffType buffType, boolean isDispellable, int maxDuration,
                      int maxStackCount, boolean refreshOnApply, float buffValue) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.ON_BEFORE_DAMAGE,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 护盾不增加基础面板
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        // ON_DAMAGE_TAKEN 时机通常用于触发特殊的受击反伤/回血，
        // 而真正的护盾挡伤消耗，必须在战斗引擎计算最终伤害时通过提供专用拦截器进行层数抵扣，而不走这个标准 trigger()
    }

    // ========= 新增护盾专用的抵扣伤害逻辑 =========
    /**
     * 消耗层数抵扣伤害，返回未能抵扣掉的剩余伤害。
     */
    public int absorbDamage(int incomingDamage, BattleEntity owner, BattleContext context) {
        int absorbed = Math.min(this.stackCount, incomingDamage);
        this.stackCount -= absorbed;
        
        context.addLogWithMeta(LogType.BUFF, owner,
                "【护盾】[%s] 身上的 [%s] 吸收了 %d 点伤害！护盾剩余值：%d",
                owner.getClass().getSimpleName(), this.buffName, absorbed, this.stackCount);
                
        return incomingDamage - absorbed;
    }
}
