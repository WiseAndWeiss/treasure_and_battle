package com.example.treasure_and_battle.buff.impl.skill;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.DamageConfig;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.manager.DamageManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

/**
 * 反击姿态buff
 * 效果：受到的伤害减少x%，每受到一次攻击就反击一次
 */
public class CounterStanceBuff extends BaseBuff {
    private final int damageReductionPercent;
    private int counterAttackCount = 0;

    public CounterStanceBuff(String buffId, String buffName, String descriptionFormat,
                              BuffType buffType, boolean isDispellable, int maxDuration,
                              int maxStackCount, boolean refreshOnApply, float buffValue,
                              int damageReductionPercent) {
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.PERMANENT,
              isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.damageReductionPercent = damageReductionPercent;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 不提供属性加成，效果通过回调实现
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        // 不需要，我们使用事件回调
    }

    /**
     * 受到伤害时触发（减少伤害）
     */
    @Override
    public int onBeforeDamageReceived(BattleEntity owner, BattleEntity attacker, int damage, BattleContext context) {
        // 减少受到的伤害
        int reducedDamage = (int) (damage * (100 - damageReductionPercent) / 100.0f);
        int damageReduced = damage - reducedDamage;

        if (damageReduced > 0) {
            context.addLog(LogType.BUFF,
                "【反击姿态】减少了 %d 点伤害（减免%d%%）",
                damageReduced, damageReductionPercent);
        }

        return reducedDamage;
    }

    /**
     * 被攻击时触发（反击）
     */
    @Override
    public void onAttacked(BattleEntity owner, BattleEntity attacker, BattleContext context) {
        // 执行普通攻击反击
        if (attacker.isDead() || owner.isDead()) {
            return;
        }

        context.addLog(LogType.ACTION,
            "【反击姿态】[%s] 对 [%s] 发动了反击！",
            owner.getName(), attacker.getName());

        AttributeSet ownerAttr = owner.getFinalAttributes();
        int baseDamage = ownerAttr.physicalAtk;

        int finalDamage = DamageManager.getInstance(owner.getContext())
                .dealDamage(DamageConfig.counterAttack(), owner, attacker, baseDamage, context);

        counterAttackCount++;

        context.addLog(LogType.DAMAGE,
            "【反击】造成了 %d 点伤害", finalDamage);
    }

    public int getCounterAttackCount() {
        return counterAttackCount;
    }

    public int getDamageReductionPercent() {
        return damageReductionPercent;
    }
}
