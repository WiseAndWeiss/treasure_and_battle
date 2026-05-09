package com.example.treasure_and_battle.buff.impl.control;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 沉默状态Debuff（软控）
 * 效果：拥有此状态的实体将无法使用技能；怪物则是只能进行普通攻击。
 * 设计思路：和冻结类似，但触发时机不同（技能使用时触发），并且只限制技能使用，不完全限制行动。
 * 实现细节：
 * 1. 触发时机：TriggerType.ON_SKILL_USE（需要在技能使用逻辑中加入触发Buff的调用）
 * 2. 效果实现：在技能使用时检查是否有沉默状态，如果有则阻止技能使用，并记录日志；对于怪物则允许普通攻击但禁止技能。
 * 3. 属性加成：沉默不影响基础属性，只限制技能使用，所以applyAttributeBonus方法可以留空。
 * 4. 持续时间和层数：可以根据设计需求设置持续回合和层数机制，例如每回合结束时减少持续时间，或者每次被触发时减少层数，直到完全解除。
 * 5. 日志记录：当触发沉默效果时，记录日志提示玩家该实体被沉默了，无法使用技能    
 */
public class SilencedDebuff extends BaseBuff {

    public SilencedDebuff(String buffId, String buffName, String descriptionFormat,
                          BuffType buffType, boolean isDispellable, int maxDuration,
                          int maxStackCount, boolean refreshOnApply, float buffValue) {
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.ON_ROUND_START,
                isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 沉默限制魔法施放，不影响基础面板属性
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        if (triggerType == TriggerType.ON_ROUND_START) {
            context.addLogWithMeta(LogType.BUFF, owner,
                    "【沉默】[%s] 被沉默，本回合无法释放技能！",
                    owner.getClass().getSimpleName());
        }
    }
}
