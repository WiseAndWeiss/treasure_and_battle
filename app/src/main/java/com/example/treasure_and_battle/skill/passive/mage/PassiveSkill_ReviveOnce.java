package com.example.treasure_and_battle.skill.passive.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 起死回生 - 法师被动技能
 * 效果：首次受到致命伤害时，将生命值恢复至x%，每场战斗仅限触发一次
 */
public class PassiveSkill_ReviveOnce extends PassiveSkill {

    // 用于跟踪是否已触发（每场战斗只触发一次）
    private boolean hasTriggered = false;

    public PassiveSkill_ReviveOnce(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onBattleStart(BattleEntity owner, BattleContext context) {
        // 战斗开始时重置触发标志
        hasTriggered = false;
    }

    @Override
    public void onDeath(BattleEntity owner, BattleContext context) {
        // 检查是否已经触发过
        if (hasTriggered) {
            return; // 已经触发过，不再触发
        }

        int revivePercent = getEffectParams().x; // 复活血量百分比

        // 检查是否真的死亡了（HP为0）
        if (owner.getCurrentHp() > 0) {
            return; // 没有死亡，不触发
        }

        // 标记为已触发
        hasTriggered = true;

        // 计算复活后的HP
        int maxHp = owner.getFinalAttributes().maxHp;
        int reviveHp = (int) (maxHp * revivePercent / 100.0f);

        // 设置HP（复活）
        owner.setCurrentHp(reviveHp);

        // 清除死亡状态
        owner.setDead(false);

        // 记录日志
        context.addLog(LogType.HEAL,
                "【起死回生】[%s] 触发了起死回生！生命值恢复至%d点（%d%%最大HP），本场战斗不可再次触发！",
                owner.getName(), reviveHp, revivePercent);

        context.addLog(LogType.ACTION,
                "【起死回生】[%s] 在濒死之际重获新生！",
                owner.getName());
    }

    /**
     * 获取是否已触发
     * @return true如果已触发，false否则
     */
    public boolean isHasTriggered() {
        return hasTriggered;
    }

    /**
     * 设置触发状态（用于测试或外部重置）
     * @param hasTriggered 触发状态
     */
    public void setHasTriggered(boolean hasTriggered) {
        this.hasTriggered = hasTriggered;
    }
}
