package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 重击 (通用) - 怪物主动技能
 * 全力一击造成巨额伤害，需要冷却
 */
public class MonsterSkill_HeavyStrike extends MonsterActiveSkill {
    public MonsterSkill_HeavyStrike(SkillTemplate template) {
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
                "【重击】[%s] 全力一击命中 [%s]，造成 %d 点伤害",
                caster.getName(), target.getName(), damageDealt);
    }
}
