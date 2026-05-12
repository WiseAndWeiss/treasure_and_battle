package com.example.treasure_and_battle.buff.impl.special;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

import java.util.List;

/**
 * 御风护体Buff - 特殊防御buff
 * 效果：下一次受到攻击时，免疫该次攻击全额伤害，并将该次伤害的x%溅射向全场所有敌人
 *
 * 触发机制：在ON_BEFORE_DAMAGE_TAKEN时机触发，免疫伤害后将自己移除
 */
public class WindProtectBuff extends BaseBuff {

    private final BattleEntity caster; // 施法者
    private final float splashDamagePercent; // 溅射伤害百分比
    private final BattleContext battleContext; // 战斗上下文
    private final BattleManager battleManager; // 战斗管理器

    public WindProtectBuff(String buffId, String buffName, String descriptionFormat,
                           BuffType buffType, boolean isDispellable, int maxDuration,
                           int maxStackCount, boolean refreshOnApply, float buffValue,
                           BattleEntity caster, float splashDamagePercent,
                           BattleContext battleContext, BattleManager battleManager) {
        super(buffId, buffName, descriptionFormat, buffType, TriggerType.ON_BEFORE_DAMAGE_TAKEN,
              isDispellable, maxDuration, maxStackCount, refreshOnApply, buffValue);
        this.caster = caster;
        this.splashDamagePercent = splashDamagePercent;
        this.battleContext = battleContext;
        this.battleManager = battleManager;
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 御风护体不影响基础属性
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context, TriggerType triggerType) {
        if (triggerType == TriggerType.ON_BEFORE_DAMAGE_TAKEN) {
            // 获取即将受到的伤害（从上下文或其他方式）
            // 注意：这里需要实际的伤害值，我们通过参数传递

            battleContext.addLog(LogType.BUFF,
                    "【御风护体】[%s] 触发了风之屏障，免疫了本次伤害！",
                    owner.getName());

            // 将自己标记为已触发，等待移除
            this.stackCount = 0; // 设置为0表示已触发，将被移除
        }
    }

    /**
     * 处理溅射伤害（在buff触发后调用）
     * @param originalDamage 原始伤害值
     * @param allEnemies 所有敌人列表
     */
    public void handleSplashDamage(int originalDamage, List<BattleEntity> allEnemies) {
        if (allEnemies == null || allEnemies.isEmpty()) {
            return;
        }

        // 计算溅射总伤害
        int totalSplashDamage = (int) (originalDamage * splashDamagePercent / 100.0f);

        // 将伤害分摊给所有敌人
        int enemyCount = allEnemies.size();
        int splashDamagePerEnemy = totalSplashDamage / enemyCount;

        battleContext.addLog(LogType.DAMAGE,
                "【御风护体】[%s] 将%d点伤害（原始伤害的%d%%）分摊给%d个敌人，每个敌人受到%d点伤害",
                caster.getName(), totalSplashDamage, (int) splashDamagePercent, enemyCount, splashDamagePerEnemy);

        // 对每个敌人造成分摊后的伤害
        for (BattleEntity enemy : allEnemies) {
            if (!enemy.isDead() && enemy.getCurrentHp() > 0) {
                battleManager.dealPhysicalDamage(caster, enemy, splashDamagePerEnemy, battleContext);
            }
        }
    }

    public float getSplashDamagePercent() {
        return splashDamagePercent;
    }

    /**
     * 检查buff是否已触发
     */
    public boolean isTriggered() {
        return this.stackCount == 0;
    }
}
