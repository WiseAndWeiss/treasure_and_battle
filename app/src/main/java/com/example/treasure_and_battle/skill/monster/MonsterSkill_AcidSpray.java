package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 酸液喷射 (史莱姆专属) - 怪物主动技能
 * 喷射腐蚀性黏液，造成魔法伤害并降低敌人防御
 */
public class MonsterSkill_AcidSpray extends MonsterActiveSkill {
    public MonsterSkill_AcidSpray(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) return;

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int defReducePercent = getEffectParams().y;
        int duration = getEffectParams().z;

        int baseDamage = (int) (caster.getFinalAttributes().magicalAtk * damagePercent / 100.0f);
        int damageDealt = battleManager.dealMagicalDamage(caster, target, baseDamage, context);

        AttributeBuff defReduceBuff = new AttributeBuff(
                "acid_spray_def_down", "酸液腐蚀", "物理防御降低%d%%",
                BuffType.DEBUFF, true, duration, 1, false, -defReducePercent,
                AttributeType.PHYSICAL_DEF, ValueType.PERCENTAGE);
        battleManager.applyBuff(target, defReduceBuff);

        context.addLog(LogType.ACTION,
                "【酸液喷射】[%s] 喷射酸液命中 [%s]，造成 %d 点伤害，物防降低 %d%% 持续 %d 回合",
                caster.getName(), target.getName(), damageDealt, defReducePercent, duration);
    }
}
