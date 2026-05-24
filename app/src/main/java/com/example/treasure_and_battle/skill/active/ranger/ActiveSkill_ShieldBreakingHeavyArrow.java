package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.defensive.ShieldBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
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
import java.util.Optional;

/**
 * 破盾重箭 - 游侠主动技能
 * 效果：对单体敌人造成x%物理攻击伤害，目标拥有护盾时额外造成y%伤害，若本次攻击击碎目标护盾，附加z层流血buff
 */
public class ActiveSkill_ShieldBreakingHeavyArrow extends ActiveSkill {

    public ActiveSkill_ShieldBreakingHeavyArrow(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        // 发送破盾重箭动画信号
        List<String> targetEntityIds = new ArrayList<>();
        for (BattleEntity target : targets) {
            String targetId = (target instanceof Monster) ? ((Monster) target).getAnimationId() : target.getEntityId();
            targetEntityIds.add(targetId);
        }

        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_shield_breaking_heavy_arrow", casterId, targetEntityIds, null)
        );

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int baseDamagePercent = getEffectParams().x;  // 基础伤害百分比
        int shieldBonusDamagePercent = getEffectParams().y; // 护盾额外伤害百分比
        int bleedingStacks = getEffectParams().z;     // 流血层数

        // 检查目标是否有护盾
        boolean hasShieldBefore = target.getActiveBuffList().stream()
                .anyMatch(buff -> buff instanceof ShieldBuff && ((ShieldBuff) buff).getStackCount() > 0);

        // 计算基础伤害
        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * baseDamagePercent / 100.0f);
        int totalDamage = baseDamage;

        // 如果目标有护盾，额外造成伤害
        if (hasShieldBefore) {
            int shieldBonusDamage = (int) (caster.getFinalAttributes().physicalAtk * shieldBonusDamagePercent / 100.0f);
            totalDamage += shieldBonusDamage;

            context.addLog(LogType.DAMAGE,
                    "【破盾重箭】[%s] 发现 [%s] 拥有护盾，额外造成%d点伤害",
                    caster.getName(), target.getName(), shieldBonusDamage);
        }

        // 造成伤害
        int actualDamage = battleManager.dealPhysicalDamage(caster, target, totalDamage, context);

        // 检查护盾是否被击碎
        boolean hasShieldAfter = target.getActiveBuffList().stream()
                .anyMatch(buff -> buff instanceof ShieldBuff && ((ShieldBuff) buff).getStackCount() > 0);

        // 检查是否击碎护盾（有护盾 → 无护盾）
        boolean shieldBroken = hasShieldBefore && !hasShieldAfter;

        if (shieldBroken) {
            // 附加流血buff
            BleedingDebuff bleedingDebuff = new BleedingDebuff(
                    "shield_breaking_bleeding",
                    "破盾重箭-流血",
                    "流血",
                    BuffType.DEBUFF,
                    true,  // 可驱散
                    3,     // 持续3回合
                    bleedingStacks,  // 最大层数
                    true,  // 刷新
                    1.0f   // buff值
            );

            // 设置初始层数（BaseBuff默认stackCount=1，需要通过stackBleeding增加层数）
            if (bleedingStacks > 1) {
                bleedingDebuff.stackBleeding(bleedingStacks - 1);
            }

            battleManager.applyBuff(target, bleedingDebuff);
            target.markAttributeCacheDirty();

            context.addLog(LogType.DAMAGE,
                    "【破盾重箭】[%s] 击碎了 [%s] 的护盾，附加%d层流血效果！",
                    caster.getName(), target.getName(), bleedingStacks);
        }

        // 记录总伤害日志
        context.addLog(LogType.DAMAGE,
                "【破盾重箭】[%s] 对 [%s] 共造成%d点伤害",
                caster.getName(), target.getName(), actualDamage);
    }
}
