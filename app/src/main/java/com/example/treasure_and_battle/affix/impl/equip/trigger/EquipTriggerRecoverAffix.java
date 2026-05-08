package com.example.treasure_and_battle.affix.impl.equip.trigger;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.affix.AffixRecoverResourceType;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.EquipCategory;
import com.example.treasure_and_battle.utils.RandomUtils;

public class EquipTriggerRecoverAffix extends BaseEquipAffix {
    private final AffixRecoverResourceType recoverResourceType;
    private final ValueType recoverValueType;
    private final int recoverValue;
    private final float damageToRecoverRatio;

    public EquipTriggerRecoverAffix(int affixId, String affixName, String description, Rarity rarity,
                                    TriggerType triggerType, EquipCategory[] allowCategories, float affixValue,
                                    AffixRecoverResourceType recoverResourceType, ValueType recoverValueType,
                                    int recoverValue, float damageToRecoverRatio) {
        super(affixId, affixName, description, rarity, triggerType, allowCategories, affixValue);
        this.recoverResourceType = recoverResourceType == null ? AffixRecoverResourceType.HP : recoverResourceType;
        this.recoverValueType = recoverValueType == null ? ValueType.FLAT : recoverValueType;
        this.recoverValue = Math.max(0, recoverValue);
        this.damageToRecoverRatio = Math.max(0f, damageToRecoverRatio);
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
        if (!RandomUtils.checkProbability(affixValue)) {
            return;
        }

        int resolvedRecoverValue = resolveRecoverValue(owner, context);
        if (resolvedRecoverValue <= 0) {
            return;
        }

        int beforeValue = getCurrentResource(owner);
        if (recoverResourceType == AffixRecoverResourceType.HP) {
            owner.healHp(resolvedRecoverValue);
        } else {
            owner.healMp(resolvedRecoverValue);
        }
        int recovered = Math.max(0, getCurrentResource(owner) - beforeValue);

        context.addLogWithMeta(
            LogType.HEAL,
            this,
            "【词缀触发】[%s] 的 [%s] 触发，恢复了 %d 点%s。",
            owner.getName(),
            getAffixName(),
            recovered,
            recoverResourceType.name()
        );
    }

    private int resolveRecoverValue(BattleEntity owner, BattleContext context) {
        if (damageToRecoverRatio > 0f) {
            return Math.max(0, (int) (context.finalDamage * damageToRecoverRatio));
        }
        if (recoverValueType == ValueType.PERCENTAGE) {
            int maxResource = getMaxResource(owner);
            return Math.max(0, (int) (maxResource * (recoverValue / 100f)));
        }
        return recoverValue;
    }

    private int getCurrentResource(BattleEntity owner) {
        return recoverResourceType == AffixRecoverResourceType.HP
            ? owner.getCurrentHp()
            : owner.getCurrentMp();
    }

    private int getMaxResource(BattleEntity owner) {
        return recoverResourceType == AffixRecoverResourceType.HP
            ? owner.getFinalAttributes().maxHp
            : owner.getFinalAttributes().maxMp;
    }

    @Override
    public String getDescription() {
        if (damageToRecoverRatio > 0f) {
            return String.format(description, affixValue * 100f, damageToRecoverRatio * 100f);
        }
        return String.format(description, affixValue * 100f, recoverValue);
    }
}

