package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 凭风引箭 - 游侠主动技能
 * 效果：对敌人造成x%物理攻击伤害；敌方速度低于自身时附加(1-速度比值)%的额外伤害，高于自身时施加2回合y%减速
 */
public class ActiveSkill_WindArrow extends ActiveSkill {

    public ActiveSkill_WindArrow(SkillTemplate template) {
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
        int damagePercent = getEffectParams().x; // 伤害百分比
        int slowPercent = getEffectParams().y;   // 减速百分比

        // 获取速度值
        int casterSpeed = caster.getFinalAttributes().speed;
        int targetSpeed = target.getFinalAttributes().speed;

        // 计算基础伤害
        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);

        int extraDamage = 0;
        String effectDescription = "";

        if (targetSpeed < casterSpeed) {
            // 敌方速度低于自身：附加额外伤害
            float speedRatio = (float) targetSpeed / casterSpeed;
            float extraDamageRatio = 1.0f - speedRatio; // (1 - 速度比值)
            extraDamage = (int) (baseDamage * extraDamageRatio);

            effectDescription = String.format("（敌方速度%d<己方速度%d，附加%.1f%%额外伤害）",
                    targetSpeed, casterSpeed, extraDamageRatio * 100);

            // 记录日志
            context.addLog(LogType.DAMAGE,
                    "【凭风引箭】[%s] 借风势对 [%s] 造成%d点基础伤害，附加%d点额外伤害%s",
                    caster.getName(), target.getName(), baseDamage, extraDamage, effectDescription);
        } else {
            // 敌方速度高于或等于自身：施加减速
            SlowDebuff slowDebuff = new SlowDebuff(
                    "wind_arrow_slow",
                    "凭风引箭-减速",
                    "速度降低%d%%",
                    BuffType.DEBUFF,
                    true,  // 可驱散
                    2,     // 持续2回合
                    1,     // 最大层数
                    false, // 不刷新
                    0,
                    slowPercent
            );

            battleManager.applyBuff(target, slowDebuff);
            target.markAttributeCacheDirty();

            effectDescription = String.format("（敌方速度%d>=己方速度%d，施加%d%%减速）",
                    targetSpeed, casterSpeed, slowPercent);

            // 记录日志
            context.addLog(LogType.DAMAGE,
                    "【凭风引箭】[%s] 对 [%s] 造成%d点伤害，并施加减速%s",
                    caster.getName(), target.getName(), baseDamage, effectDescription);
        }

        // 造成总伤害（基础伤害 + 额外伤害）
        int totalDamage = baseDamage + extraDamage;
        battleManager.dealPhysicalDamage(caster, target, totalDamage, context);

        // 记录总伤害日志
        context.addLog(LogType.DAMAGE,
                "【凭风引箭】[%s] 对 [%s] 共造成%d点伤害",
                caster.getName(), target.getName(), totalDamage);
    }
}
