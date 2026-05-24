package com.example.treasure_and_battle.skill.active.mage;

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
 * 狂风呼啸技能 - 法师AOE控制技能
 *
 * 技能效果：
 * - 对全体敌人造成x%法术伤害
 * - 附加2回合y%减速和z%致盲
 */
public class ActiveSkill_WindHowl extends ActiveSkill {

    public ActiveSkill_WindHowl(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        // 发送狂风呼啸动画信号
        List<String> targetEntityIds = new ArrayList<>();
        for (BattleEntity target : targets) {
            String targetId = (target instanceof Monster) ? ((Monster) target).getAnimationId() : target.getEntityId();
            targetEntityIds.add(targetId);
        }

        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_wind_howl", casterId, targetEntityIds, null)
        );

        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        float slowPercent = getEffectParams().y;
        float blindPercent = getEffectParams().z;
        int debuffDuration = 2;

        // 对所有目标敌人造成伤害并附加控制效果
        for (BattleEntity enemy : targets) {
            if (enemy.isDead()) continue;

            // 计算法术伤害
            int magicalAtk = caster.getFinalAttributes().magicalAtk;
            int baseDamage = (int) (magicalAtk * damagePercent / 100.0f);

            // 造成魔法伤害
            battleManager.dealMagicalDamage(caster, enemy, baseDamage, context);

            // 如果目标还活着，附加减速和致盲debuff
            if (!enemy.isDead()) {
                // 添加减速
                SlowDebuff slowDebuff = new SlowDebuff(
                    "wind_howl_slow",
                    "狂风减速",
                    "速度降低%d%%",
                    BuffType.DEBUFF,
                    true,  // 可驱散
                    debuffDuration,
                    1,     // 不堆叠
                    true,  // 刷新
                    0,     // buffValue（不使用）
                    slowPercent
                );
                enemy.getActiveBuffList().add(slowDebuff);

                // 添加致盲
                BlindnessDebuff blindDebuff = new BlindnessDebuff(
                    "wind_howl_blind",
                    "狂风致盲",
                    "命中率降低%d%%",
                    BuffType.DEBUFF,
                    true,  // 可驱散
                    debuffDuration,
                    1,     // 不堆叠
                    true,  // 刷新
                    0,     // buffValue（不使用）
                    blindPercent
                );
                enemy.getActiveBuffList().add(blindDebuff);
                enemy.markAttributeCacheDirty();

                context.addLog(LogType.ACTION,
                    "【%s】%s被狂风侵袭，速度降低%.0f%%，命中率降低%.0f%%（持续%d回合）",
                    getSkillName(), enemy.getName(), slowPercent, blindPercent, debuffDuration);
            }
        }

        // 记录总伤害日志
        context.addLog(LogType.ACTION,
            "【%s】狂风席卷全场，所有敌人的攻势与视线都被凝滞",
            getSkillName());
    }
}
