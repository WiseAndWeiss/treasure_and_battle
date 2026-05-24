package com.example.treasure_and_battle.affix.impl.monster.trigger;

import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

public class MonsterTriggerManaBurnAffix extends BaseMonsterAffix {

    public MonsterTriggerManaBurnAffix(int affixId, String affixName, String description, Rarity rarity,
                                       TriggerType triggerType, float value) {
        super(affixId, affixName, description, rarity, triggerType, value);
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        if (owner == null || context == null || owner.getContext() == null) {
            return;
        }

        int burnAmount = (int) affixValue;
        if (burnAmount <= 0) {
            return;
        }

        int totalBurned = 0;
        int affectedCount = 0;

        for (BattleEntity target : context.playerParty) {
            if (target == null || target.isDead()) {
                continue;
            }

            int currentMp = target.getCurrentMp();
            if (currentMp <= 0) {
                continue;
            }

            int actualBurn = Math.min(currentMp, burnAmount);
            target.setCurrentMp(currentMp - actualBurn);
            totalBurned += actualBurn;
            affectedCount++;
        }

        if (affectedCount > 0) {
            context.addLogWithMeta(
                    LogType.AFFIX,
                    this,
                    "【词缀触发】[%s] 的 [%s] 触发，使 %d 个敌人共燃烧了 %d 点法力值。",
                    owner.getName(),
                    getAffixName(),
                    affectedCount,
                    totalBurned
            );
        }
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue);
    }
}
