package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 龙吼 (龙专属) - 怪物主动技能
 * 远古龙威震慑全场，削弱敌方攻防
 */
public class MonsterSkill_DragonRoar extends MonsterActiveSkill {
    public MonsterSkill_DragonRoar(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int atkReducePercent = getEffectParams().x;
        int defReducePercent = getEffectParams().y;
        int duration = getEffectParams().z;

        int count = 0;
        for (BattleEntity target : targets) {
            if (target.isDead()) continue;

            AttributeBuff atkReduceBuff = new AttributeBuff(
                    "dragon_roar_atk_down", "龙吼·降攻", "物攻降低%d%%",
                    BuffType.DEBUFF, true, duration, 1, false, -atkReducePercent,
                    AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE);
            battleManager.applyBuff(target, atkReduceBuff);

            AttributeBuff defReduceBuff = new AttributeBuff(
                    "dragon_roar_def_down", "龙吼·降防", "物防降低%d%%",
                    BuffType.DEBUFF, true, duration, 1, false, -defReducePercent,
                    AttributeType.PHYSICAL_DEF, ValueType.PERCENTAGE);
            battleManager.applyBuff(target, defReduceBuff);

            count++;
        }

        context.addLog(LogType.ACTION,
                "【龙吼】[%s] 远古龙威震慑了 %d 个敌人，物攻-%d%% 物防-%d%% 持续 %d 回合",
                caster.getName(), count, atkReducePercent, defReducePercent, duration);
    }
}
