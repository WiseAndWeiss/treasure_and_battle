package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.ResistanceReductionDebuff;
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
 * 蚀弱穿射 - 游侠主动技能
 * 效果：无视护盾和装备对单个敌人造成x%穿甲伤害，附加1回合y%易伤效果
 */
public class ActiveSkill_PierceWeak extends ActiveSkill {

    public ActiveSkill_PierceWeak(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        // 发送蚀弱穿射动画信号
        List<String> targetEntityIds = new ArrayList<>();
        for (BattleEntity target : targets) {
            String targetId = (target instanceof Monster) ? ((Monster) target).getAnimationId() : target.getEntityId();
            targetEntityIds.add(targetId);
        }

        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_pierce_weak", casterId, targetEntityIds, null)
        );

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int piercingDamagePercent = getEffectParams().x; // 穿甲伤害百分比
        int vulnerabilityPercent = getEffectParams().y;  // 易伤百分比

        // 计算穿甲伤害（无视护盾和装备的防御）
        int piercingDamage = (int) (caster.getFinalAttributes().physicalAtk * piercingDamagePercent / 100.0f);

        // 造成穿甲伤害（使用PIERCING伤害类型）
        int finalDamage = battleManager.dealPiercingDamage(caster, target, piercingDamage, context);

        // 创建易伤debuff（降低物抗和法抗，持续1回合）
        ResistanceReductionDebuff vulnerabilityDebuff = new ResistanceReductionDebuff(
                "pierce_weak_vulnerability",
                "蚀弱穿射-易伤",
                "物抗和法抗降低%d%%",
                BuffType.DEBUFF,
                true,  // 可驱散
                1,     // 持续1回合
                1,     // 最大层数
                false, // 不刷新
                0,
                vulnerabilityPercent
        );

        // 应用易伤debuff
        battleManager.applyBuff(target, vulnerabilityDebuff);
        target.markAttributeCacheDirty();

        // 记录日志
        context.addLog(LogType.DAMAGE,
                "【蚀弱穿射】[%s] 的尖锐箭矢穿透 [%s] 的防御，造成%d点穿甲伤害，并使其物抗和法抗降低%d%%",
                caster.getName(), target.getName(), finalDamage, vulnerabilityPercent);
    }
}
