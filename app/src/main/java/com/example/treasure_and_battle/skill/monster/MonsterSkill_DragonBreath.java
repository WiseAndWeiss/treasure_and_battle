package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 龙息 (龙专属) - 怪物主动技能
 * 喷吐毁灭性龙息，灼烧全体敌人
 */
public class MonsterSkill_DragonBreath extends MonsterActiveSkill {
    public MonsterSkill_DragonBreath(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int totalDamage = 0;

        for (BattleEntity target : targets) {
            if (target.isDead()) continue;
            int baseDamage = (int) (caster.getFinalAttributes().magicalAtk * damagePercent / 100.0f);
            int damageDealt = battleManager.dealMagicalDamage(caster, target, baseDamage, context);
            totalDamage += damageDealt;
        }

        context.addLog(LogType.ACTION,
                "【龙息】[%s] 喷吐毁灭性龙息，对 %d 个敌人共造成 %d 点魔法伤害",
                caster.getName(), targets.size(), totalDamage);
    }
}
