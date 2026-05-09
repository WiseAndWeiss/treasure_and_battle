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
 * 撒沙 (山贼专属) - 怪物主动技能
 * 卑劣的散沙攻击，遮蔽敌人视线降低命中率
 */
public class MonsterSkill_ThrowSand extends MonsterActiveSkill {
    public MonsterSkill_ThrowSand(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) return;

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int hitReducePercent = getEffectParams().y;
        int duration = getEffectParams().z;

        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);
        int damageDealt = battleManager.dealPhysicalDamage(caster, target, baseDamage, context);

        AttributeBuff hitReduceBuff = new AttributeBuff(
                "throw_sand_hit_down", "视线模糊", "命中率降低%d%%",
                BuffType.DEBUFF, true, duration, 1, false, -hitReducePercent,
                AttributeType.HIT_RATE, ValueType.PERCENTAGE);
        battleManager.applyBuff(target, hitReduceBuff);

        context.addLog(LogType.ACTION,
                "【撒沙】[%s] 向 [%s] 撒沙，造成 %d 点伤害，命中率降低 %d%% 持续 %d 回合",
                caster.getName(), target.getName(), damageDealt, hitReducePercent, duration);
    }
}
