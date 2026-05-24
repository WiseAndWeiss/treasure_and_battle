package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignal;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignalPipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 伤痕汲血 - 战士主动技能
 * 效果：对单体敌人造成少量x%物理伤害；若敌人带有流血debuff，移除其所有流血层数，每层使自身回复y%最大生命值
 */
public class ActiveSkill_WoundBloodSuck extends ActiveSkill {
    public ActiveSkill_WoundBloodSuck(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        // 发送伤痕汲血动画信号
        List<String> targetEntityIds = new ArrayList<>();
        for (BattleEntity target : targets) {
            String targetId = (target instanceof Monster) ? ((Monster) target).getAnimationId() : target.getEntityId();
            targetEntityIds.add(targetId);
        }

        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_wound_blood_suck", casterId, targetEntityIds, null)
        );

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int damagePercent = getEffectParams().x;  // 伤害百分比
        int healPercentPerStack = getEffectParams().y;  // 每层回血百分比

        // 计算基础伤害（少量伤害）
        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);

        // 造成伤害
        int damageDealt = battleManager.dealPhysicalDamage(caster, target, baseDamage, context);

        // 检查目标是否有流血debuff
        BleedingDebuff bleedingDebuff = null;
        List<BaseBuff> targetBuffs = target.getActiveBuffList();

        for (BaseBuff buff : targetBuffs) {
            if (buff instanceof BleedingDebuff) {
                bleedingDebuff = (BleedingDebuff) buff;
                break;
            }
        }

        int totalHeal = 0;

        if (bleedingDebuff != null && bleedingDebuff.getStackCount() > 0) {
            // 移除所有流血层数并回血
            int bleedingStacks = bleedingDebuff.getStackCount();
            int maxHp = caster.getFinalAttributes().maxHp;
            totalHeal = (int) (maxHp * healPercentPerStack / 100.0f * bleedingStacks);

            // 移除流血debuff
            BuffManager.getInstance(caster.getContext()).removeBuff(target, bleedingDebuff.getBuffId());

            // 为自身回血
            caster.healHp(totalHeal);

            // 记录日志
            context.addLog(LogType.ACTION,
                "【伤痕汲血】[%s] 汲取了 [%s] 的 %d 层流血，回复了 %d 点生命值！",
                caster.getName(), target.getName(), bleedingStacks, totalHeal);
        }

        // 记录伤害日志
        context.addLog(LogType.ACTION,
            "【伤痕汲血】[%s] 对 [%s] 造成了 %d 点伤害",
            caster.getName(), target.getName(), damageDealt);
    }
}
