package com.example.treasure_and_battle.affix.impl.monster.trigger;

import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.damage.DamageConfig;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.DamageManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;

public class MonsterTriggerOnDeathExplodeAffix extends BaseMonsterAffix {

    public MonsterTriggerOnDeathExplodeAffix(int affixId, String affixName, String description, Rarity rarity,
                                             TriggerType triggerType, float value) {
        super(affixId, affixName, description, rarity, triggerType, value);
    }

    @Override
    public void onTrigger(BattleEntity owner, BattleContext context) {
        if (owner == null || context == null || owner.getContext() == null) {
            return;
        }

        int maxHp = owner.getFinalAttributes().maxHp;
        int explodeDamage = (int) (maxHp * affixValue);
        if (explodeDamage <= 0) {
            return;
        }

        BattleEntity killer = context.currentActor;
        if (killer == null || killer.isDead()) {
            return;
        }

        DamageManager dm = DamageManager.getInstance(owner.getContext());
        dm.dealDamage(DamageConfig.skillMagical(), owner, killer, explodeDamage, context);

        context.addLogWithMeta(
                LogType.AFFIX,
                this,
                "【词缀触发】[%s] 的 [%s] 触发，自爆对 [%s] 造成了 %d 点魔法伤害。",
                owner.getName(),
                getAffixName(),
                killer.getName(),
                explodeDamage
        );
    }

    @Override
    public void applyAttributeBonus(AttributeSet attributeSet) {
    }

    @Override
    public String getDescription() {
        return String.format(description, affixValue * 100);
    }
}
