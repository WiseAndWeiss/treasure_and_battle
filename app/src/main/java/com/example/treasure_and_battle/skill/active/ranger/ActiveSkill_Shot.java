package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 点射 - 游侠主动技能
 * 效果：对目标造成{x}%物理攻击伤害，该次攻击以{y}%的概率增加一轮额外的暴击判定
 */
public class ActiveSkill_Shot extends ActiveSkill {
    public ActiveSkill_Shot(SkillTemplate template) {
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
        int damagePercent = getEffectParams().x;     // 伤害百分比
        int extraCritChance = getEffectParams().y;   // 额外暴击概率

        // 计算基础伤害
        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);

        // 检查是否触发额外暴击判定
        boolean extraCritTriggered = false;
        if (extraCritChance > 0) {
            float critChance = extraCritChance / 100.0f;
            extraCritTriggered = com.example.treasure_and_battle.utils.RandomUtils.checkProbability(critChance);
        }

        int finalDamage = 0;

        // 第一轮暴击判定
        if (extraCritTriggered) {
            // 强制进行暴击判定
            float critChance = caster.getFinalAttributes().physicalCritRate;
            boolean isCrit = com.example.treasure_and_battle.utils.RandomUtils.checkProbability(critChance);

            if (isCrit) {
                // 暴击伤害
                baseDamage *= caster.getFinalAttributes().physicalCritDmg;
                context.isCriticalHit = true;
            }

            finalDamage = battleManager.dealPhysicalDamage(caster, target, baseDamage, context);

            context.addLog(LogType.DODGE_CRIT,
                "【点射】[%s] 的额外暴击判定%s！造成 %d 点伤害",
                caster.getName(), isCrit ? "成功" : "失败", finalDamage);
        } else {
            // 普通攻击
            finalDamage = battleManager.dealPhysicalDamage(caster, target, baseDamage, context);
        }

        // 记录日志
        context.addLog(LogType.DAMAGE,
            "【点射】[%s] 对 [%s] 造成 %d 点伤害",
            caster.getName(), target.getName(), finalDamage);
    }
}
