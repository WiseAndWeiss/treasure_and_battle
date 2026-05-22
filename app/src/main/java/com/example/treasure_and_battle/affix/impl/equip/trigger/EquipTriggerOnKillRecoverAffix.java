package com.example.treasure_and_battle.affix.impl.equip.trigger;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;

public class EquipTriggerOnKillRecoverAffix extends BaseEquipAffix {

    public EquipTriggerOnKillRecoverAffix(int affixId, String affixName, String description, Rarity rarity,
                                          TriggerType triggerType, EquipCategory[] allowCategories,
                                          float affixValue) {
        super(affixId, affixName, description, rarity, triggerType, allowCategories, affixValue);
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        if (owner == null || context == null || owner.getContext() == null) {
            return;
        }

        AttributeSet attr = owner.getFinalAttributes();
        int hpRecover = (int) (attr.maxHp * affixValue);
        int mpRecover = (int) (attr.maxMp * affixValue);

        int hpBefore = owner.getCurrentHp();
        owner.healHp(hpRecover);
        int hpActual = owner.getCurrentHp() - hpBefore;

        int mpBefore = owner.getCurrentMp();
        owner.healMp(mpRecover);
        int mpActual = owner.getCurrentMp() - mpBefore;

        context.addLogWithMeta(
                LogType.AFFIX,
                this,
                "【词缀触发】[%s] 的 [%s] 触发，恢复了 %d 点生命和 %d 点魔力。",
                owner.getName(),
                getAffixName(),
                hpActual,
                mpActual
        );
    }

    @Override
    public String getDescription() {
        float pct = affixValue * 100;
        return String.format(description, pct, pct);
    }
}
