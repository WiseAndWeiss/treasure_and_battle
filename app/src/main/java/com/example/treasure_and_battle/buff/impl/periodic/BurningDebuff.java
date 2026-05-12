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
 * 燃烧状态Debuff
 * 效果：回合结束时触发，每层造成1点魔法伤害。
 * 衰减机制：每回合结束时，伤害后，层数减半。当层数衰减到0时完全解除。
 *
 * 特殊机制：燃烧debuff在实体身上应该是唯一的，不同来源的燃烧应该叠加层数而非创建新实例。
 * 因此在BuffManager中有特殊处理，确保只有唯一的BurningDebuff实例存在。
 */
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

    /**
     * 叠加燃烧层数
     * 与流血debuff类似，燃烧debuff在实体身上应该是唯一的，不同来源的燃烧应该叠加层数
     * @param additionalStacks 要叠加的层数
     */
    public void stackBurning(int additionalStacks) {
        if (additionalStacks > 0) {
            this.stackCount += additionalStacks;
            // 确保不超过最大层数限制
            if (this.maxStackCount > 0 && this.stackCount > this.maxStackCount) {
                this.stackCount = this.maxStackCount;
            }
        }
    }
}
