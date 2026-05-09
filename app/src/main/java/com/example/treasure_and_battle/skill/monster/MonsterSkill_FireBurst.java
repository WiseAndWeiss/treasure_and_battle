package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 火焰爆裂 (火灵专属) - 怪物主动技能
 * 凝聚火焰之力引爆单体敌人，附加灼烧
 */
public class MonsterSkill_FireBurst extends MonsterActiveSkill {
    public MonsterSkill_FireBurst(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) return;

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int burnStacks = getEffectParams().y;

        int baseDamage = (int) (caster.getFinalAttributes().magicalAtk * damagePercent / 100.0f);
        int damageDealt = battleManager.dealMagicalDamage(caster, target, baseDamage, context);

        BurningDebuff burningDebuff = new BurningDebuff(
                "debuff_burning", "灼烧", "每层造成1点魔法伤害，每回合减半",
                BuffType.DEBUFF, true, -1, 10000, true, 1.0f);
        burningDebuff.setStack(burnStacks);
        battleManager.applyBuff(target, burningDebuff);

        context.addLog(LogType.ACTION,
                "【火焰爆裂】[%s] 引爆 [%s]，造成 %d 点魔法伤害，附加 %d 层灼烧",
                caster.getName(), target.getName(), damageDealt, burnStacks);
    }
}
