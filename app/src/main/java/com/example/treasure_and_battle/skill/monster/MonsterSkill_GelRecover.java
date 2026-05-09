package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 凝胶再生 (史莱姆专属) - 怪物主动技能
 * 利用凝胶体质快速再生，恢复生命值
 */
public class MonsterSkill_GelRecover extends MonsterActiveSkill {
    public MonsterSkill_GelRecover(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int healPercent = getEffectParams().x;
        int maxHp = caster.getFinalAttributes().maxHp;
        int healAmount = (int) (maxHp * healPercent / 100.0f);
        int actualHeal = Math.min(healAmount, maxHp - caster.getCurrentHp());

        caster.healHp(actualHeal);

        context.addLog(LogType.HEAL,
                "【凝胶再生】[%s] 快速再生恢复了 %d 点生命值",
                caster.getName(), actualHeal);
    }
}
