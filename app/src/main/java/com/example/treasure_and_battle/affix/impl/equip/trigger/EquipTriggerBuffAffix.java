package com.example.treasure_and_battle.affix.impl.equip.trigger;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.EquipCategory;
import com.example.treasure_and_battle.utils.RandomUtils;

public class EquipTriggerBuffAffix extends BaseEquipAffix {
    private final int buffTemplateId;
    private final AffixBuffApplyTarget applyTarget;
    private final int applyStacks;
    private final float damageToStackRatio;

    public EquipTriggerBuffAffix(int affixId, String affixName, String description, Rarity rarity,
                                 AffixTriggerType triggerType, EquipCategory[] allowCategories, float affixValue,
                                 int buffTemplateId, AffixBuffApplyTarget applyTarget, int applyStacks,
                                 float damageToStackRatio) {
        super(affixId, affixName, description, rarity, triggerType, allowCategories, affixValue);
        this.buffTemplateId = buffTemplateId;
        this.applyTarget = applyTarget == null ? AffixBuffApplyTarget.TARGET : applyTarget;
        this.applyStacks = Math.max(1, applyStacks);
        this.damageToStackRatio = Math.max(0f, damageToStackRatio);
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
        // 触发型词缀不直接提供常驻属性。
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        if (owner == null || context == null || owner.getContext() == null) {
            return;
        }
        // 触发概率判定
        if (!RandomUtils.checkProbability(affixValue)) {
            return;
        }
        // 根据伤害计算实际应用层数
        int resolvedStacks = resolveApplyStacks(context);
        if (resolvedStacks <= 0) {
            return;
        }

        BattleEntity targetEntity = applyTarget == AffixBuffApplyTarget.SELF ? owner : context.currentTarget;
        if (targetEntity == null) {
            return;
        }

        BuffManager buffManager = BuffManager.getInstance(owner.getContext());
        String buffName = "未知状态";
        for (int i = 0; i < resolvedStacks; i++) {
            BaseBuff buff = buffManager.createBuffByTemplateId(buffTemplateId);
            if (buff == null) {
                return;
            }
            if (i == 0) {
                buffName = buff.getBuffName();
            }
            buffManager.addBuff(targetEntity, buff);
        }

        context.addLogWithMeta(
            com.example.treasure_and_battle.battle.log.LogType.AFFIX,
            this,
            "【词缀触发】[%s] 的 [%s] 触发，给 [%s] 施加了 %d 层 [%s]。",
            owner.getName(),
            getAffixName(),
            targetEntity.getName(),
            resolvedStacks,
            buffName
        );
    }

    private int resolveApplyStacks(BattleContext context) {
        // 如果配置了伤害转层数的比例，则根据当前伤害计算层数，否则使用固定层数
        if (damageToStackRatio > 0f) {
            int stacksByDamage = (int) (context.finalDamage * damageToStackRatio);
            return Math.max(0, stacksByDamage);
        }
        return applyStacks;
    }

    @Override
    public String getDescription() {
        // 如果配置了伤害转层数的比例，则在描述中动态展示当前的伤害转层数效果，否则展示固定层数
        if (damageToStackRatio > 0f) {
            return String.format(description, affixValue * 100, damageToStackRatio * 100);
        }
        return String.format(description, affixValue * 100, applyStacks);
    }
}
