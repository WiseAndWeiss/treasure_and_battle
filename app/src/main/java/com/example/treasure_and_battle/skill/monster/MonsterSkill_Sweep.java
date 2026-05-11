package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 横扫 (通用) - 怪物主动技能
 * 挥动武器/肢体横扫全场，对全体敌人造成{x}%物理伤害
 */
public class MonsterSkill_Sweep extends MonsterActiveSkill {
    public MonsterSkill_Sweep(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int totalDamage = 0;

        for (BattleEntity target : targets) {
            if (target.isDead()) continue;
            int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);
            int damageDealt = battleManager.dealPhysicalDamage(caster, target, baseDamage, context);
            totalDamage += damageDealt;
        }

        context.addLog(LogType.ACTION,
                "【横扫】[%s] 横扫全场，对 %d 个敌人共造成 %d 点伤害",
                caster.getName(), targets.size(), totalDamage);
    }
}
