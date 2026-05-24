package com.example.treasure_and_battle.affix.impl.equip.trigger;

import com.example.treasure_and_battle.affix.BaseEquipAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.damage.DamageConfig;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.DamageManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;

import java.util.List;

public class EquipTriggerBattleStartAoeDamageAffix extends BaseEquipAffix {

    public EquipTriggerBattleStartAoeDamageAffix(int affixId, String affixName, String description, Rarity rarity,
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
        int baseDamage = (int) ((attr.physicalAtk + attr.magicalAtk) * affixValue);
        if (baseDamage <= 0) {
            return;
        }

        List<Monster> aliveMonsters = context.getAliveMonsters();
        if (aliveMonsters.isEmpty()) {
            return;
        }

        DamageManager dm = DamageManager.getInstance(owner.getContext());
        DamageConfig config = DamageConfig.buffPhysical();
        int totalDamage = 0;
        for (Monster m : aliveMonsters) {
            int dealt = dm.dealDamage(config, owner, m, baseDamage, context);
            totalDamage += dealt;
        }

        context.addLogWithMeta(
                LogType.AFFIX,
                this,
                "【词缀触发】[%s] 的 [%s] 发动先发制人，对 %d 个敌人造成总计 %d 点伤害。",
                owner.getName(),
                getAffixName(),
                aliveMonsters.size(),
                totalDamage
        );
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100);
    }
}
