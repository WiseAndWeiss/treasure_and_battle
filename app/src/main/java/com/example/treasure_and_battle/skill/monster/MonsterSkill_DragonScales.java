package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 龙鳞护体 (龙专属) - 怪物主动技能
 * 龙鳞硬化，生成强力护盾
 */
public class MonsterSkill_DragonScales extends MonsterActiveSkill {
    public MonsterSkill_DragonScales(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int shieldPercent = getEffectParams().x;
        int maxHp = caster.getFinalAttributes().maxHp;
        int shieldValue = (int) (maxHp * shieldPercent / 100.0f);

        ShieldBuff shieldBuff = new ShieldBuff(
                "dragon_scales_shield", "龙鳞护体", "吸收%d点伤害",
                BuffType.BUFF, false, -1, shieldValue, false, 1.0f);
        battleManager.applyBuff(caster, shieldBuff);

        context.addLog(LogType.BUFF,
                "【龙鳞护体】[%s] 龙鳞硬化，生成了 %d 点护盾",
                caster.getName(), shieldValue);
    }
}
