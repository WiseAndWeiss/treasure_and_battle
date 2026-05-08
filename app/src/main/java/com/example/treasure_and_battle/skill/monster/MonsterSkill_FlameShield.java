package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 火焰护盾 (火灵专属) - 怪物主动技能
 * 火焰缠绕全身，生成护盾并灼伤攻击者
 */
public class MonsterSkill_FlameShield extends MonsterActiveSkill {
    public MonsterSkill_FlameShield(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int shieldPercent = getEffectParams().x;
        int reflectBurnStacks = getEffectParams().y;
        int duration = getEffectParams().z;

        int maxHp = caster.getFinalAttributes().maxHp;
        int shieldValue = (int) (maxHp * shieldPercent / 100.0f);

        ShieldBuff shieldBuff = new ShieldBuff(
                "flame_shield", "火焰护盾", "吸收%d点伤害",
                BuffType.BUFF, false, duration, shieldValue, false, 1.0f);
        battleManager.applyBuff(caster, shieldBuff);

        context.addLog(LogType.BUFF,
                "【火焰护盾】[%s] 火焰缠绕全身，生成了 %d 点护盾，持续 %d 回合，受击时对攻击者附加 %d 层灼烧",
                caster.getName(), shieldValue, duration, reflectBurnStacks);
    }
}
