package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignal;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignalPipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 全力以赴 - 战士主动技能
 * 效果：消耗魔力与最大生命值，立即获得y点行动点，每场战斗仅限使用1次
 */
public class ActiveSkill_GoAllOut extends ActiveSkill {
    public ActiveSkill_GoAllOut(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        // 发送全力以赴动画信号
        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_go_all_out", casterId, new ArrayList<>(), null)
        );

        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int actionPointsGained = getEffectParams().y;  // 获得的行动点

        // 获取消耗（applyCastCost已经处理了HP和MP消耗）
        int hpCost = getHpCost();
        int mpCost = getMpCost();

        // 增加行动点
        int currentAp = caster.getCurrentActionPoints();
        caster.setCurrentActionPoints(currentAp + actionPointsGained);

        // 记录日志
        context.addLog(LogType.ACTION,
            "【全力以赴】[%s] 燃烧了 %d 点生命值与 %d 点魔力，获得了 %d 点行动点！(当前AP：%d)",
            caster.getName(), hpCost, mpCost, actionPointsGained, caster.getCurrentActionPoints());
    }
}
