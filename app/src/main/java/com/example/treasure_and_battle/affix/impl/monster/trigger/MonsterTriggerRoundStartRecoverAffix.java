package com.example.treasure_and_battle.affix.impl.monster.trigger;

import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

public class MonsterTriggerRoundStartRecoverAffix extends BaseMonsterAffix {

    public MonsterTriggerRoundStartRecoverAffix(int affixId, String affixName, String description, Rarity rarity,
                                                TriggerType triggerType, float value) {
        super(affixId, affixName, description, rarity, triggerType, value);
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        if (owner == null || context == null || owner.getContext() == null) {
            return;
        }

        AttributeSet attr = owner.getFinalAttributes();
        int recoverAmount = (int) (attr.maxHp * affixValue);
        if (recoverAmount <= 0) {
            return;
        }

        int beforeHp = owner.getCurrentHp();
        owner.healHp(recoverAmount);
        int actualRecover = owner.getCurrentHp() - beforeHp;

        if (actualRecover > 0) {
            context.addLogWithMeta(
                    LogType.AFFIX,
                    this,
                    "【词缀触发】[%s] 的 [%s] 触发，恢复了 %d 点生命值。",
                    owner.getName(),
                    getAffixName(),
                    actualRecover
            );
        }
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100);
    }
}
