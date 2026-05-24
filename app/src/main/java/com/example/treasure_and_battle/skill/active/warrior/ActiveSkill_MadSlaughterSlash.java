package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.WeaknessDebuff;
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

/**
 * 狂弑千斩 - 战士主动技能
 * 效果：消耗生命，对全体敌方造成x%物理攻击伤害，附加y层流血debuff与z%虚弱减益（持续w回合）
 */
public class ActiveSkill_MadSlaughterSlash extends ActiveSkill {
    public ActiveSkill_MadSlaughterSlash(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        // 发送狂弑千斩动画信号
        List<String> targetEntityIds = new ArrayList<>();
        for (BattleEntity target : targets) {
            String targetId = (target instanceof Monster) ? ((Monster) target).getAnimationId() : target.getEntityId();
            targetEntityIds.add(targetId);
        }

        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_mad_slaughter_slash", casterId, targetEntityIds, null)
        );

        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int damagePercent = getEffectParams().x;  // 伤害百分比
        int bleedingStacks = getEffectParams().y;  // 流血层数
        int weaknessPercent = getEffectParams().z;  // 虚弱百分比
        int weaknessDuration = getEffectParams().w;  // 虚弱持续回合

        int totalDamage = 0;
        int totalHits = 0;

        // 对所有敌人造成伤害并施加debuff
        for (BattleEntity target : targets) {
            if (target.isDead()) {
                continue;
            }

            // 计算伤害
            int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);
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

            // 施加虚弱debuff
            WeaknessDebuff weaknessDebuff = new WeaknessDebuff(
                "weakness",
                "虚弱",
                "物理与法术攻击降低%d%%",
                BuffType.DEBUFF,
                true,  // 可驱散
                weaknessDuration,  // 持续w回合
                1,    // 最多1层
                false,
                1.0f,
                weaknessPercent  // 虚弱百分比（整数）
            );
            battleManager.applyBuff(target, weaknessDebuff);

            totalDamage += damageDealt;
            totalHits++;
        }

        // 记录日志
        context.addLog(LogType.ACTION,
            "【狂弑千斩】[%s] 疯狂斩击了 %d 个敌人，总共造成 %d 点伤害！所有敌人附加了 %d 层流血与 %d%% 虚弱",
            caster.getName(), totalHits, totalDamage, bleedingStacks, weaknessPercent);
    }
}
