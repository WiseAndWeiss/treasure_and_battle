package com.example.treasure_and_battle.buff.impl.periodic;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

public class BurningDebuff extends BaseBuff {
    public BurningDebuff(String buffId, String buffName, String descriptionFormat,
                         BuffType buffType, boolean isDispellable, int maxDuration,
                         int maxStackCount, boolean refreshOnApply, float buffValue) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.ON_ROUND_END,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
    }

    @Override
    public boolean tick() {
        // 每回合衰减量：50%（每回合结束时，层数减半，向上取整）
        int decayAmount = (int) Math.ceil(this.stackCount / 2.0);
        this.stackCount -= decayAmount;

        // 当层数降为0时，isExpired() 会返回 true，由系统自动脱落
        return this.isExpired();
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 灼烧直接扣血，不影响基础面板属性
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        if (triggerType == BuffTriggerType.ON_ROUND_END) {
            // 每层灼烧造成1点魔法伤害，会受到魔法防御的减免
            int baseDamage = this.stackCount; // 每层1点伤害
            // 计算魔法防御后的实际伤害，确保不为负数
            int damage = Math.max(0, baseDamage - owner.getFinalAttributes().magicalDef);
            owner.takeDamage(damage);
            // 加入战斗日志
            context.addLogWithMeta(LogType.DAMAGE, owner,
                    "【灼烧】[%s] 当前层数 %d，魔法防御为：%d, 损失了 %d 点生命值！剩余生命：(%d/%d)",
                    owner.getClass().getSimpleName(), this.stackCount,
                    owner.getFinalAttributes().magicalDef, damage,
                    owner.getCurrentHp(), owner.getFinalAttributes().maxHp);
        }
    }
}
