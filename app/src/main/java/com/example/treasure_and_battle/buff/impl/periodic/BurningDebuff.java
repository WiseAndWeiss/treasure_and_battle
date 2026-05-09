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

public class BurningDebuff extends BaseBuff {
    public BurningDebuff(String buffId, String buffName, String descriptionFormat,
                         BuffType buffType, boolean isDispellable, int maxDuration,
                         int maxStackCount, boolean refreshOnApply, float buffValue) {
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.ON_ROUND_END,
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
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        if (triggerType == TriggerType.ON_ROUND_END) {
            int damage = this.stackCount;
            DamageManager.getInstance(owner.getContext())
                    .dealDamage(DamageConfig.buffMagical(), null, owner, damage, context);
            context.addLogWithMeta(LogType.DAMAGE, owner,
                    "【灼烧】[%s] 当前层数 %d，魔法防御为：%d, 损失了 %d 点生命值！剩余生命：(%d/%d)",
                    owner.getClass().getSimpleName(), this.stackCount,
                    owner.getFinalAttributes().magicalDef, damage,
                    owner.getCurrentHp(), owner.getFinalAttributes().maxHp);
        }
    }
}
