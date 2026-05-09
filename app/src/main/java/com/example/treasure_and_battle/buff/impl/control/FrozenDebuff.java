package com.example.treasure_and_battle.buff.impl.control;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 冻结状态Debuff（硬控）
 * 效果：拥有此状态的实体将无法行动，直接跳过回合。
 */
public class FrozenDebuff extends BaseBuff {

    public FrozenDebuff(String buffId, String buffName, String descriptionFormat,
                        BuffType buffType, boolean isDispellable, int maxDuration,
                        int maxStackCount, boolean refreshOnApply, float buffValue) {
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.ON_ROUND_START,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 冻结只限制行动，不影响基础面板属性
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        if (triggerType == TriggerType.ON_ROUND_START) {
            context.addLogWithMeta(LogType.BUFF, owner,
                    "【冻结】[%s] 被冻结，本回合无法行动！",
                    owner.getClass().getSimpleName());
        }
    }
}
