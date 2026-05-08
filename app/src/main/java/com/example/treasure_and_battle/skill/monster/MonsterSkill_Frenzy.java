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
 * 狂化 (狼专属) - 怪物主动技能
 * 陷入狂暴状态，牺牲防御换取攻击力
 */
public class MonsterSkill_Frenzy extends MonsterActiveSkill {
    public MonsterSkill_Frenzy(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int atkPercent = getEffectParams().x;
        int spdPercent = getEffectParams().y;
        int defReducePercent = getEffectParams().z;

        int currentHp = caster.getCurrentHp();
        int hpCost = (int) (currentHp * 0.15);
        int hpBefore = caster.getCurrentHp();
        caster.setCurrentHp(Math.max(1, currentHp - hpCost));

        AttributeBuff atkBuff = new AttributeBuff(
                "frenzy_atk", "狂化·攻击", "物攻提升%d%%",
                BuffType.BUFF, false, 2, 1, false, atkPercent,
                AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE);
        battleManager.applyBuff(caster, atkBuff);

        AttributeBuff spdBuff = new AttributeBuff(
                "frenzy_spd", "狂化·速度", "速度提升%d%%",
                BuffType.BUFF, false, 2, 1, false, spdPercent,
                AttributeType.SPEED, ValueType.PERCENTAGE);
        battleManager.applyBuff(caster, spdBuff);

        AttributeBuff defReduceBuff = new AttributeBuff(
                "frenzy_def_down", "狂化·防御降低", "物防降低%d%%",
                BuffType.DEBUFF, false, 2, 1, false, -defReducePercent,
                AttributeType.PHYSICAL_DEF, ValueType.PERCENTAGE);
        battleManager.applyBuff(caster, defReduceBuff);

        context.addLog(LogType.ACTION,
                "【狂化】[%s] 陷入狂暴！消耗 %d 点HP，物攻+%d%% 速度+%d%% 物防-%d%% 持续2回合",
                caster.getName(), hpBefore - caster.getCurrentHp(), atkPercent, spdPercent, defReducePercent);
    }
}
