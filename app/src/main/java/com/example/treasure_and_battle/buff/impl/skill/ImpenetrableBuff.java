package com.example.treasure_and_battle.buff.impl.skill;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.manager.BuffManager;

/**
 * 固若金汤Buff
 * 效果：本回合受到的所有HP伤害都会以一定比例转化为护盾
 * 注意：只计算HP实际收到的伤害，不包括护盾吸收、减伤、抵挡等
 */
public class ImpenetrableBuff extends BaseBuff {
    private final int conversionPercent; // 转化比例
    private final BuffManager buffManager;
    private int pendingShieldValue = 0; // 待转化的护盾值

    public ImpenetrableBuff(String buffId, String buffName, String descriptionFormat,
                            BuffType buffType, boolean isDispellable, int maxDuration,
                            int maxStackCount, boolean refreshOnApply, float buffValue,
                            int conversionPercent, BuffManager buffManager) {
        super(buffId, buffName, descriptionFormat, buffType, BuffTriggerType.PERMANENT,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.conversionPercent = conversionPercent;
        this.buffManager = buffManager;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 固若金汤不直接增加属性
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, BuffTriggerType triggerType) {
        // 固若金汤不需要触发效果，通过onAfterDamageReceived处理
    }

    /**
     * 受到伤害后的回调（在护盾吸收之后、HP扣除之后调用）
     * 此时已经确认HP减少，计算护盾转化
     */
    @Override
    public void onAfterDamageReceived(BattleEntity owner, BattleEntity attacker, int actualHpDamage, BattleContext context) {
        if (actualHpDamage > 0) {
            // 计算转化的护盾值
            int shieldValue = (int) (actualHpDamage * conversionPercent / 100.0f);
            pendingShieldValue += shieldValue;

            context.addLog(LogType.BUFF,
                "【固若金汤】记录了 %d 点伤害，将转化 %d 点护盾",
                actualHpDamage, shieldValue);
        }
    }

    /**
     * 回合结束时，将待转化的护盾值实际添加到实体身上
     */
    public void convertToShieldOnRoundEnd(BattleEntity owner, BattleContext context) {
        if (pendingShieldValue > 0 && context != null) {
            // 创建护盾buff（永久，直到被击碎）
            ShieldBuff shieldBuff = new ShieldBuff(
                "impenetrable_shield",
                "固若金汤护盾",
                "吸收%d点伤害",
                BuffType.BUFF,
                false,  // 不可驱散
                -1,    // 永久持续（直到护盾值归零）
                pendingShieldValue,  // maxStackCount作为护盾值
                false,
                1.0f
            );

            buffManager.addBuff(owner, shieldBuff);

            context.addLog(LogType.BUFF,
                "【固若金汤】[%s] 将 %d 点护盾值转化为实体护盾",
                owner.getName(), pendingShieldValue);

            pendingShieldValue = 0;
        }
    }

    /**
     * 重写tick方法，在buff过期前转化护盾
     */
    @Override
    public boolean tick() {
        // 注意：这里的context是null，所以无法转化护盾
        // 实际的护盾转化应该在BuffManager.onRoundEnd中处理
        return super.tick();
    }
}
