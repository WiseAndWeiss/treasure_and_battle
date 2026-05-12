package com.example.treasure_and_battle.buff.impl.skill;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

import java.util.Random;

/**
 * 炽能涌动护盾 - 受击时有概率对攻击者附加燃烧
 *
 * 效果：
 * 1. 吸收伤害（继承自ShieldBuff）
 * 2. 受击时有概率对攻击者附加燃烧debuff
 * 来源：法师技能"炽能涌动"
 */
public class FlameSurgeShieldBuff extends ShieldBuff {

    private final float triggerChance; // 触发概率（百分比，如30表示30%）
    private final int burningStacks; // 附加的燃烧层数
    private final Random random;
    private BattleEntity lastAttacker; // 记录最近一次攻击者

    public FlameSurgeShieldBuff(String buffId, String buffName, String descriptionFormat,
                               BuffType buffType, boolean isDispellable, int maxDuration,
                               int maxStackCount, boolean refreshOnApply, float buffValue,
                               float triggerChance, int burningStacks) {
        super(buffId, buffName, descriptionFormat, buffType, isDispellable, maxDuration,
              maxStackCount, refreshOnApply, buffValue);
        this.triggerChance = triggerChance;
        this.burningStacks = burningStacks;
        this.random = new Random();
    }

    @Override
    public int onBeforeDamageReceived(BattleEntity owner, BattleEntity attacker, int damage, BattleContext context) {
        // 记录攻击者，用于后续反噬
        this.lastAttacker = attacker;
        // 先调用父类的护盾抵扣逻辑
        return super.onBeforeDamageReceived(owner, attacker, damage, context);
    }

    @Override
    public void onAfterDamageReceived(BattleEntity owner, BattleEntity attacker, int actualHpDamage, BattleContext context) {
        // 如果护盾还有效且受到了伤害，尝试反噬
        if (getStackCount() > 0 && actualHpDamage > 0) {
            // 随机判定是否触发
            float roll = random.nextFloat() * 100;
            if (roll < triggerChance && lastAttacker != null && !lastAttacker.isDead()) {
                // 附加燃烧debuff
                applyBurningDebuff(lastAttacker, context);
            }
        }
    }

    /**
     * 对目标附加燃烧debuff
     */
    private void applyBurningDebuff(BattleEntity target, BattleContext context) {
        // 直接创建燃烧debuff
        BurningDebuff burningDebuff = new BurningDebuff(
            "flame_surge_burning",
            "燃烧",
            "回合结束时受到相当于层数的魔法伤害（当前层数：%2$d），每回合衰减一半。",
            BuffType.DEBUFF,
            true,  // 可驱散
            -1,    // 持续直到层数为0
            10000, // maxStackCount（设置一个大值）
            true,  // 刷新
            1      // buffValue（每层伤害）
        );
        burningDebuff.setStack(burningStacks);
        target.getActiveBuffList().add(burningDebuff);
        target.markAttributeCacheDirty();

        // 记录战斗日志
        context.addLog(LogType.BUFF,
            "【炽能反噬】%s受到烈焰灼烧，获得%d层燃烧",
            target.getName(), burningStacks);
    }

    public float getTriggerChance() {
        return triggerChance;
    }

    public int getBurningStacks() {
        return burningStacks;
    }

    public int getStackCount() {
        return stackCount;
    }
}
