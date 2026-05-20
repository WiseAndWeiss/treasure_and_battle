package com.example.treasure_and_battle.affix.impl.monster.trigger;

import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

public class MonsterTriggerAoeBuffAffix extends BaseMonsterAffix {
    private final int buffTemplateId;
    private final int applyStacks;

    public MonsterTriggerAoeBuffAffix(int affixId, String affixName, String description, Rarity rarity,
                                      TriggerType triggerType, float value, int buffTemplateId, int applyStacks) {
        super(affixId, affixName, description, rarity, triggerType, value);
        this.buffTemplateId = buffTemplateId;
        this.applyStacks = Math.max(1, applyStacks);
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        if (owner == null || context == null || owner.getContext() == null) {
            return;
        }

        BuffManager buffManager = BuffManager.getInstance(owner.getContext());
        String buffName = "未知状态";
        int targetCount = 0;

        for (BattleEntity target : context.playerParty) {
            if (target == null || target.isDead()) {
                continue;
            }

            for (int i = 0; i < applyStacks; i++) {
                BaseBuff buff = buffManager.createBuffByTemplateId(buffTemplateId);
                if (buff == null) {
                    continue;
                }
                if (i == 0 && targetCount == 0) {
                    buffName = buff.getBuffName();
                }
                buffManager.addBuff(target, buff);
            }
            targetCount++;
        }

        if (targetCount > 0) {
            context.addLogWithMeta(
                    LogType.AFFIX,
                    this,
                    "【词缀触发】[%s] 的 [%s] 触发，对 %d 个敌人各施加了 %d 层 [%s]。",
                    owner.getName(),
                    getAffixName(),
                    targetCount,
                    applyStacks,
                    buffName
            );
        }
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
    }

    @Override
    public String getDescription() {
        return String.format(description, (float) applyStacks);
    }
}
