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
 * 威吓 (山贼专属) - 怪物主动技能
 * 凶恶咆哮震慑全场，降低敌人攻击力
 */
public class MonsterSkill_Intimidate extends MonsterActiveSkill {
    public MonsterSkill_Intimidate(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int atkReducePercent = getEffectParams().x;
        int magReducePercent = getEffectParams().y;
        int duration = getEffectParams().z;

        int count = 0;
        for (BattleEntity target : targets) {
            if (target.isDead()) continue;

            AttributeBuff atkReduceBuff = new AttributeBuff(
                    "intimidate_atk_down", "威吓·降攻", "物攻降低%d%%",
                    BuffType.DEBUFF, true, duration, 1, false, -atkReducePercent,
                    AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE);
            battleManager.applyBuff(target, atkReduceBuff);

            AttributeBuff magReduceBuff = new AttributeBuff(
                    "intimidate_mag_down", "威吓·降法攻", "法攻降低%d%%",
                    BuffType.DEBUFF, true, duration, 1, false, -magReducePercent,
                    AttributeType.MAGICAL_ATK, ValueType.PERCENTAGE);
            battleManager.applyBuff(target, magReduceBuff);

            count++;
        }

        context.addLog(LogType.ACTION,
                "【威吓】[%s] 凶恶咆哮震慑了 %d 个敌人，物攻-%d%% 法攻-%d%% 持续 %d 回合",
                caster.getName(), count, atkReducePercent, magReducePercent, duration);
    }
}
