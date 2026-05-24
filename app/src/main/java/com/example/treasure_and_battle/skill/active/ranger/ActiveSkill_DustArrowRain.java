package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.control.BlindnessDebuff;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
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
 * 扬尘箭雨 - 游侠主动技能
 * 效果：对所有敌人造成x%物理攻击伤害，附加2回合y%减速与z%致盲效果
 */
public class ActiveSkill_DustArrowRain extends ActiveSkill {

    public ActiveSkill_DustArrowRain(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        // 发送扬尘箭雨动画信号
        List<String> targetEntityIds = new ArrayList<>();
        for (BattleEntity target : targets) {
            String targetId = (target instanceof Monster) ? ((Monster) target).getAnimationId() : target.getEntityId();
            targetEntityIds.add(targetId);
        }

        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_dust_arrow_rain", casterId, targetEntityIds, null)
        );

        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int damagePercent = getEffectParams().x;     // 伤害百分比
        int slowPercent = getEffectParams().y;       // 减速百分比
        int blindPercent = getEffectParams().z;      // 致盲百分比

        // 计算基础伤害
        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);

        int totalDamage = 0;
        int targetCount = 0;

        // 对所有目标造成伤害并附加debuff
        for (BattleEntity target : targets) {
            // 造成物理伤害
            int damage = battleManager.dealPhysicalDamage(caster, target, baseDamage, context);
            totalDamage += damage;
            targetCount++;

            // 创建减速debuff（持续2回合）
            SlowDebuff slowDebuff = new SlowDebuff(
                    "dust_arrow_rain_slow",
                    "扬尘箭雨-减速",
                    "速度降低%d%%",
                    BuffType.DEBUFF,
                    true,  // 可驱散
                    2,     // 持续2回合
                    1,     // 最大层数
                    false, // 不刷新
                    0,
                    slowPercent
            );

            // 创建致盲debuff（持续2回合）
            BlindnessDebuff blindDebuff = new BlindnessDebuff(
                    "dust_arrow_rain_blind",
                    "扬尘箭雨-致盲",
                    "命中率降低%d%%",
                    BuffType.DEBUFF,
                    true,  // 可驱散
                    2,     // 持续2回合
                    1,     // 最大层数
                    false, // 不刷新
                    0,
                    blindPercent
            );

            // 应用debuff
            battleManager.applyBuff(target, slowDebuff);
            battleManager.applyBuff(target, blindDebuff);
            target.markAttributeCacheDirty();

            // 记录单个目标的日志
            context.addLog(LogType.DAMAGE,
                    "【扬尘箭雨】[%s] 的箭矢击中 [%s]，造成%d点伤害，并附加减速和致盲效果",
                    caster.getName(), target.getName(), damage);
        }

        // 记录总伤害日志
        context.addLog(LogType.DAMAGE,
                "【扬尘箭雨】[%s] 的箭雨共对%d个目标造成%d点总伤害",
                caster.getName(), targetCount, totalDamage);
    }
}
