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
 * 冰霜弹 (冰灵专属) - 怪物主动技能
 * 发射极寒冰弹冻结敌人行动，附加减速
 */
public class MonsterSkill_FrostBolt extends MonsterActiveSkill {
    public MonsterSkill_FrostBolt(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) return;

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int slowPercent = getEffectParams().y;
        int duration = getEffectParams().z;

        int baseDamage = (int) (caster.getFinalAttributes().magicalAtk * damagePercent / 100.0f);
        int damageDealt = battleManager.dealMagicalDamage(caster, target, baseDamage, context);

        SlowDebuff slowDebuff = new SlowDebuff(
                "frost_slow", "冰霜减速", "速度降低%d%%",
                BuffType.DEBUFF, true, duration, 1, false, 1.0f, slowPercent);
        battleManager.applyBuff(target, slowDebuff);

        context.addLog(LogType.ACTION,
                "【冰霜弹】[%s] 极寒冰弹命中 [%s]，造成 %d 点魔法伤害，速度降低 %d%% 持续 %d 回合",
                caster.getName(), target.getName(), damageDealt, slowPercent, duration);
    }
}
