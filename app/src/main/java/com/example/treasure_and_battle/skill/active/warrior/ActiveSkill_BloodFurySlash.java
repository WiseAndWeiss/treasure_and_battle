package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 血怒斩 - 战士主动技能
 * 效果：消耗生命，对单体造成x%物理攻击伤害，为敌人附加y层流血debuff
 */
public class ActiveSkill_BloodFurySlash extends ActiveSkill {
    public ActiveSkill_BloodFurySlash(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int damagePercent = getEffectParams().x;  // 伤害百分比
        int bleedingStacks = getEffectParams().y;  // 流血层数

        // 计算基础伤害
        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);

        // 造成伤害
        int damageDealt = battleManager.dealPhysicalDamage(caster, target, baseDamage, context);

        // 施加流血debuff
        BleedingDebuff bleedingDebuff = new BleedingDebuff(
            "debuff_bleeding",
            "流血",
            "每层损失1%%最大生命值，每回合衰减一层",
            BuffType.DEBUFF,
            true,  // 可驱散
            -1,    // 持续时间由层数决定
            10000, // 最大层数
            true,  // 可叠加
            1.0f
        );
        bleedingDebuff.setStack(bleedingStacks);

        battleManager.applyBuff(target, bleedingDebuff);

        // 记录日志
        context.addLog(LogType.ACTION,
            "【血怒斩】[%s] 狂暴一斩对 [%s] 造成了 %d 点伤害，并附加了 %d 层流血debuff",
            caster.getName(), target.getName(), damageDealt, bleedingStacks);
    }
}
