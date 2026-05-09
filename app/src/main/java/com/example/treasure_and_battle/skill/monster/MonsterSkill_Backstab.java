package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 背刺 (山贼专属) - 怪物主动技能
 * 伺机发动致命偷袭，伤害系数高
 */
public class MonsterSkill_Backstab extends MonsterActiveSkill {
    public MonsterSkill_Backstab(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) return;

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);
        int damageDealt = battleManager.dealPhysicalDamage(caster, target, baseDamage, context);

        context.addLog(LogType.ACTION,
                "【背刺】[%s] 发动致命偷袭命中 [%s]，造成 %d 点伤害",
                caster.getName(), target.getName(), damageDealt);
    }
}
