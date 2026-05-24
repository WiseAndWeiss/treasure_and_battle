package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
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
 * 看破 - 游侠主动技能
 * 效果：降低目标x%物理防御和y%法术防御
 */
public class ActiveSkill_SeeThrough extends ActiveSkill {

    public ActiveSkill_SeeThrough(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        // 发送看破动画信号
        List<String> targetEntityIds = new ArrayList<>();
        for (BattleEntity target : targets) {
            String targetId = (target instanceof Monster) ? ((Monster) target).getAnimationId() : target.getEntityId();
            targetEntityIds.add(targetId);
        }

        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_see_through", casterId, targetEntityIds, null)
        );

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int physicalDefReductionPercent = getEffectParams().x; // 物理防御降低百分比
        int magicalDefReductionPercent = getEffectParams().y;  // 法术防御降低百分比

        // 创建物理防御降低buff（持续1回合）
        // 注意：physicalDefReductionPercent是百分比形式（如8代表8%），需要除以100转换为小数
        AttributeBuff physicalDefBuff = new AttributeBuff(
                "see_through_physical_def_reduction",
                "看破-物防降低",
                "物理防御降低%d%%",
                BuffType.DEBUFF,
                true,  // 可驱散
                1,     // 持续1回合
                1,     // 最大层数
                false, // 不刷新
                -physicalDefReductionPercent / 100.0f,  // 将百分比转换为小数（8% -> -0.08）
                AttributeType.PHYSICAL_DEF,
                com.example.treasure_and_battle.model.common.ValueType.PERCENTAGE
        );

        // 创建法术防御降低buff（持续1回合）
        AttributeBuff magicalDefBuff = new AttributeBuff(
                "see_through_magical_def_reduction",
                "看破-法防降低",
                "法术防御降低%d%%",
                BuffType.DEBUFF,
                true,  // 可驱散
                1,     // 持续1回合
                1,     // 最大层数
                false, // 不刷新
                -magicalDefReductionPercent / 100.0f,  // 将百分比转换为小数（8% -> -0.08）
                AttributeType.MAGICAL_DEF,
                com.example.treasure_and_battle.model.common.ValueType.PERCENTAGE
        );

        // 应用buff
        battleManager.applyBuff(target, physicalDefBuff);
        battleManager.applyBuff(target, magicalDefBuff);
        target.markAttributeCacheDirty();

        // 记录日志
        context.addLog(LogType.BUFF,
                "【看破】[%s] 看破了 [%s] 的防御弱点，使其物理防御降低%d%%，法术防御降低%d%%",
                caster.getName(), target.getName(), physicalDefReductionPercent, magicalDefReductionPercent);
    }
}
