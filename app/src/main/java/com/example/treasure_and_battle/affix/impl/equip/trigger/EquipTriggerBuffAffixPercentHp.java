package com.example.treasure_and_battle.affix.impl.equip.trigger;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;

/**
 * 触发型Buff词缀（百分比最大生命值版本）
 * applyStacks 表示最大生命值的百分比（如 10 = 10% 最大生命值护盾）
 */
public class EquipTriggerBuffAffixPercentHp extends EquipTriggerBuffAffix {

    public EquipTriggerBuffAffixPercentHp(int affixId, String affixName, String description, Rarity rarity,
                                          TriggerType triggerType, EquipCategory[] allowCategories, float affixValue,
                                          int buffTemplateId, AffixBuffApplyTarget applyTarget, int applyStacks,
                                          float damageToStackRatio) {
        super(affixId, affixName, description, rarity, triggerType, allowCategories, affixValue,
                buffTemplateId, applyTarget, applyStacks, damageToStackRatio);
    }

    @Override
    protected int resolveApplyStacks(BattleEntity owner, BattleContext context) {
        int maxHp = owner.getFinalAttributes().maxHp;
        return Math.max(1, (int) (maxHp * applyStacks / 100f));
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100, (float) applyStacks);
    }
}
