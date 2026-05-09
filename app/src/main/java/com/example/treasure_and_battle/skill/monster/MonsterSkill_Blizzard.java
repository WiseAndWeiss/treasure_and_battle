package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;

import java.util.List;

/**
 * 暴风雪 (冰灵专属) - 怪物主动技能
 * 召唤暴风雪冻结全体敌人，附加减速
 */
public class MonsterSkill_Blizzard extends MonsterActiveSkill {
    public MonsterSkill_Blizzard(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int slowPercent = getEffectParams().y;
        int duration = getEffectParams().z;
        int totalDamage = 0;
        int hitCount = 0;

        for (BattleEntity target : targets) {
            if (target.isDead()) continue;

            int baseDamage = (int) (caster.getFinalAttributes().magicalAtk * damagePercent / 100.0f);
            int damageDealt = battleManager.dealMagicalDamage(caster, target, baseDamage, context);
            totalDamage += damageDealt;

            SlowDebuff slowDebuff = new SlowDebuff(
                    "blizzard_slow", "暴风雪减速", "速度降低%d%%",
                    BuffType.DEBUFF, true, duration, 1, false, 1.0f, slowPercent);
            battleManager.applyBuff(target, slowDebuff);

            hitCount++;
        }

        context.addLog(LogType.ACTION,
                "【暴风雪】[%s] 暴风雪席卷全场，对 %d 个敌人共造成 %d 点伤害，速度降低 %d%% 持续 %d 回合",
                caster.getName(), hitCount, totalDamage, slowPercent, duration);
    }
}
