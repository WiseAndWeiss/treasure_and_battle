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
 * 狼嚎 (狼专属) - 怪物主动技能
 * 仰天长嚎鼓舞全体友方，提升攻击力和速度
 */
public class MonsterSkill_Howl extends MonsterActiveSkill {
    public MonsterSkill_Howl(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int atkPercent = getEffectParams().x;
        int spdPercent = getEffectParams().y;
        int duration = getEffectParams().z;

        int count = 0;
        for (BattleEntity ally : targets) {
            if (ally.isDead()) continue;

            AttributeBuff atkBuff = new AttributeBuff(
                    "howl_atk", "狼嚎·攻击", "物攻提升%d%%",
                    BuffType.BUFF, false, duration, 1, false, atkPercent,
                    AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE);
            battleManager.applyBuff(ally, atkBuff);

            AttributeBuff spdBuff = new AttributeBuff(
                    "howl_spd", "狼嚎·速度", "速度提升%d%%",
                    BuffType.BUFF, false, duration, 1, false, spdPercent,
                    AttributeType.SPEED, ValueType.PERCENTAGE);
            battleManager.applyBuff(ally, spdBuff);

            count++;
        }

        context.addLog(LogType.BUFF,
                "【狼嚎】[%s] 仰天长嚎，鼓舞了 %d 个友方单位，物攻+%d%% 速度+%d%% 持续 %d 回合",
                caster.getName(), count, atkPercent, spdPercent, duration);
    }
}
