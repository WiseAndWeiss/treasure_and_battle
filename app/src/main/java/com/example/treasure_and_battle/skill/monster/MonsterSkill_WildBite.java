package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 野性撕咬 (通用) - 怪物主动技能
 * 用利齿撕咬敌人，造成撕裂伤持续流血
 */
public class MonsterSkill_WildBite extends MonsterActiveSkill {
    public MonsterSkill_WildBite(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) return;

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int bleedingStacks = getEffectParams().y;

        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);
        int damageDealt = battleManager.dealPhysicalDamage(caster, target, baseDamage, context);

        BleedingDebuff bleedingDebuff = new BleedingDebuff(
                "debuff_bleeding", "流血", "每层损失1%%最大生命值，每回合衰减一层",
                BuffType.DEBUFF, true, -1, 10000, true, 1.0f);
        bleedingDebuff.setStack(bleedingStacks);
        battleManager.applyBuff(target, bleedingDebuff);

        context.addLog(LogType.ACTION,
                "【野性撕咬】[%s] 撕咬 [%s] 造成 %d 点伤害，附加 %d 层流血",
                caster.getName(), target.getName(), damageDealt, bleedingStacks);
    }
}
