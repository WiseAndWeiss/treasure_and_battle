package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignal;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignalPipeline;
import com.example.treasure_and_battle.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 致命连射 - 游侠主动技能
 * 效果：对单体敌人造成x%物理攻击伤害，若本次攻击暴击则立刻再次释放该技能，最多触发5轮，直至敌人死亡或暴击判定失败
 */
public class ActiveSkill_LethalContinuousShot extends ActiveSkill {

    private int chainCount = 0; // 连射计数器

    public ActiveSkill_LethalContinuousShot(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        // 发送致命连射动画信号
        List<String> targetEntityIds = new ArrayList<>();
        for (BattleEntity target : targets) {
            String targetId = (target instanceof Monster) ? ((Monster) target).getAnimationId() : target.getEntityId();
            targetEntityIds.add(targetId);
        }

        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_lethal_continuous_shot", casterId, targetEntityIds, null)
        );

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        // 重置连射计数器
        chainCount = 0;

        // 开始连射
        performLethalShot(caster, target, battleManager, context);
    }

    /**
     * 执行致命连射（递归方法）
     */
    private void performLethalShot(BattleEntity caster, BattleEntity target, BattleManager battleManager, BattleContext context) {
        // 检查是否达到最大连射次数
        if (chainCount >= 5) {
            context.addLog(LogType.DAMAGE,
                    "【致命连射】[%s] 达到最大连射次数（5次），连射结束",
                    caster.getName());
            return;
        }

        // 检查目标是否死亡
        if (target.isDead() || target.getCurrentHp() <= 0) {
            context.addLog(LogType.DAMAGE,
                    "【致命连射】[%s] 的目标已死亡，连射结束",
                    caster.getName());
            return;
        }

        chainCount++;

        // 获取效果参数
        int damagePercent = getEffectParams().x; // 伤害百分比

        // 计算基础伤害
        int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);

        // 检查是否暴击
        float critRate = caster.getFinalAttributes().physicalCritRate;
        boolean isCrit = RandomUtils.checkProbability(critRate);

        int finalDamage = baseDamage;
        if (isCrit) {
            // 暴击伤害
            float critDmg = caster.getFinalAttributes().physicalCritDmg;
            finalDamage = (int) (baseDamage * critDmg);
            context.isCriticalHit = true;
        }

        // 造成伤害
        int actualDamage = battleManager.dealPhysicalDamage(caster, target, finalDamage, context);

        // 记录本次射击日志
        context.addLog(LogType.DAMAGE,
                "【致命连射】[%s] 第%d次射击对 [%s] 造成%d点伤害%s",
                caster.getName(), chainCount, target.getName(), actualDamage, isCrit ? "（暴击！）" : "");

        // 检查目标是否在本次攻击后死亡
        if (target.isDead() || target.getCurrentHp() <= 0) {
            context.addLog(LogType.DEATH,
                    "【致命连射】[%s] 的目标被击杀，连射结束（共%d次）",
                    caster.getName(), chainCount);
            return;
        }

        // 如果暴击，再次触发连射
        if (isCrit && chainCount < 5) {
            context.addLog(LogType.DODGE_CRIT,
                    "【致命连射】[%s] 暴击触发连射！准备第%d次射击",
                    caster.getName(), chainCount + 1);

            // 递归调用，再次释放技能
            performLethalShot(caster, target, battleManager, context);
        } else {
            context.addLog(LogType.DAMAGE,
                    "【致命连射】[%s] 连射结束（共%d次，%s）",
                    caster.getName(), chainCount, isCrit ? "达到最大次数" : "未暴击");
        }
    }

    /**
     * 获取当前连射次数（用于测试）
     */
    public int getChainCount() {
        return chainCount;
    }

    /**
     * 重置连射计数器（用于测试）
     */
    public void resetChainCount() {
        this.chainCount = 0;
    }
}
