package com.example.treasure_and_battle.affix.impl.equip.trigger;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.damage.DamageConfig;
import com.example.treasure_and_battle.battle.damage.DamageSource;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.DamageManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;

public class EquipTriggerThornsAffix extends BaseEquipAffix {

    public EquipTriggerThornsAffix(int affixId, String affixName, String description, Rarity rarity,
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

        if (context.damageSource == DamageSource.COUNTER_ATTACK) {
            return;
        }

        BattleEntity attacker = context.currentActor;
        if (attacker == null || attacker.isDead() || attacker == owner) {
            return;
        }

        int reflectDamage = (int) (context.finalDamage * affixValue);
        if (reflectDamage <= 0) {
            return;
        }

        DamageManager dm = DamageManager.getInstance(owner.getContext());
        dm.dealDamage(DamageConfig.piercing(DamageSource.COUNTER_ATTACK),
                owner, attacker, reflectDamage, context);

        context.addLogWithMeta(
                LogType.AFFIX,
                this,
                "【词缀触发】[%s] 的 [%s] 触发，反弹了 %d 点伤害给 [%s]。",
                owner.getName(),
                getAffixName(),
                reflectDamage,
                attacker.getName()
        );
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100);
    }
}
