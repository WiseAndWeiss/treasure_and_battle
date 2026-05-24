package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.attribute.WeaknessDebuff;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignal;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignalPipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 重盾强击 - 战士主动技能
 * 效果：对单体造成x%物理攻击伤害；若自身拥有护盾，附加y%护盾值额外伤害，并施加z%的虚弱buff，持续2回合
 */
public class ActiveSkill_ShieldStrike extends ActiveSkill {
    public ActiveSkill_ShieldStrike(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        // 发送重盾强击动画信号
        List<String> targetEntityIds = new ArrayList<>();
        for (BattleEntity target : targets) {
            String targetId = (target instanceof Monster) ? ((Monster) target).getAnimationId() : target.getEntityId();
            targetEntityIds.add(targetId);
        }

        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_shield_strike", casterId, targetEntityIds, null)
        );

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int damagePercent = getEffectParams().x;  // 基础伤害百分比
        int shieldBonusPercent = getEffectParams().y;  // 护盾额外伤害百分比
        int weaknessDebuffPercent = getEffectParams().z;  // 虚弱debuff百分比

        // 计算基础伤害
        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);

        // 检查是否有护盾
        int shieldBonusDamage = 0;
        for (BaseBuff buff : caster.getActiveBuffList()) {
            if (buff instanceof ShieldBuff) {
                // 获取当前护盾值
                int currentShieldValue = buff.getStackCount();
                shieldBonusDamage = (int) (currentShieldValue * shieldBonusPercent / 100.0f);

                context.addLog(LogType.BUFF,
                    "【重盾强击】[%s] 护盾值 %d 转化为 %d 点额外伤害",
                    caster.getName(), currentShieldValue, shieldBonusDamage);
                break;
            }
        }

        // 总伤害
        int totalDamage = baseDamage + shieldBonusDamage;

        // 造成物理伤害
        int damageDealt = battleManager.dealPhysicalDamage(caster, target, totalDamage, context);

        // 施加虚弱debuff（持续2回合）
        if (weaknessDebuffPercent > 0) {
            WeaknessDebuff weaknessDebuff = new WeaknessDebuff(
                "shield_strike_weakness",
                "虚弱",
                "物理攻击与法术攻击降低%d%%",
                BuffType.DEBUFF,
                true,  // 可驱散
                2,     // 持续2回合
                1,     // 最多1层
                false,
                weaknessDebuffPercent,
                weaknessDebuffPercent
            );

            battleManager.applyBuff(target, weaknessDebuff);

            context.addLog(LogType.BUFF,
                "【重盾强击】[%s] 对 [%s] 施加了虚弱，物理攻击与法术攻击降低 %d%%，持续2回合",
                caster.getName(), target.getName(), weaknessDebuffPercent);
        }

        // 记录伤害日志
        context.addLog(LogType.DAMAGE,
            "【重盾强击】[%s] 对 [%s] 造成 %d 点伤害（基础%d + 护盾加成%d）",
            caster.getName(), target.getName(), damageDealt, baseDamage, shieldBonusDamage);
    }
}
