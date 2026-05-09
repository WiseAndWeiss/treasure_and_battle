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
 * 烈焰风暴 (火灵专属) - 怪物主动技能
 * 释放毁灭性的火焰风暴席卷全场，附加灼烧
 */
public class MonsterSkill_Inferno extends MonsterActiveSkill {
    public MonsterSkill_Inferno(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int burnStacks = getEffectParams().y;
        int totalDamage = 0;
        int hitCount = 0;

        for (BattleEntity target : targets) {
            if (target.isDead()) continue;

            int baseDamage = (int) (caster.getFinalAttributes().magicalAtk * damagePercent / 100.0f);
            int damageDealt = battleManager.dealMagicalDamage(caster, target, baseDamage, context);
            totalDamage += damageDealt;

            BurningDebuff burningDebuff = new BurningDebuff(
                    "debuff_burning", "灼烧", "每层造成1点魔法伤害，每回合减半",
                    BuffType.DEBUFF, true, -1, 10000, true, 1.0f);
            burningDebuff.setStack(burnStacks);
            battleManager.applyBuff(target, burningDebuff);

            hitCount++;
        }

        context.addLog(LogType.ACTION,
                "【烈焰风暴】[%s] 烈焰席卷全场，对 %d 个敌人共造成 %d 点伤害，附加 %d 层灼烧",
                caster.getName(), hitCount, totalDamage, burnStacks);
    }
}
