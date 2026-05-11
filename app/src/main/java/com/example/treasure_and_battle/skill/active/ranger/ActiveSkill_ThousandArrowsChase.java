package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 逐敌千箭 - 游侠主动技能
 * 效果：对全体敌人造成n段x%物理攻击伤害，段数n等于场上存活敌人数量，最大段数不超过4段
 */
public class ActiveSkill_ThousandArrowsChase extends ActiveSkill {

    public ActiveSkill_ThousandArrowsChase(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int damagePercent = getEffectParams().x; // 伤害百分比

        // 统计场上存活的敌人数量（只统计还活着的敌人）
        long aliveEnemyCount = targets.stream()
                .filter(enemy -> !enemy.isDead() && enemy.getCurrentHp() > 0)
                .count();

        // 确定伤害段数（最大不超过4段）
        int waveCount = (int) Math.min(aliveEnemyCount, 4);

        context.addLog(LogType.DAMAGE,
                "【逐敌千箭】[%s] 检测到%d个存活敌人，将造成%d段箭雨攻击",
                caster.getName(), aliveEnemyCount, waveCount);

        // 对全体敌人进行多段攻击
        for (int wave = 1; wave <= waveCount; wave++) {
            context.addLog(LogType.DAMAGE,
                    "【逐敌千箭】[%s] 发射第%d波箭雨",
                    caster.getName(), wave);

            // 计算本段伤害
            int waveDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);

            // 对所有存活敌人造成本段伤害
            int waveTotalDamage = 0;
            int waveTargetCount = 0;

            for (BattleEntity target : targets) {
                // 跳过已死亡的目标
                if (target.isDead() || target.getCurrentHp() <= 0) {
                    continue;
                }

                // 造成伤害
                int damage = battleManager.dealPhysicalDamage(caster, target, waveDamage, context);
                waveTotalDamage += damage;
                waveTargetCount++;

                // 如果目标死亡，记录日志
                if (target.isDead() || target.getCurrentHp() <= 0) {
                    context.addLog(LogType.DEATH,
                            "【逐敌千箭】[%s] 的第%d波箭雨击杀了 [%s]",
                            caster.getName(), wave, target.getName());
                }
            }

            // 记录本段伤害统计
            context.addLog(LogType.DAMAGE,
                    "【逐敌千箭】[%s] 第%d波箭雨对%d个目标造成%d点总伤害",
                    caster.getName(), wave, waveTargetCount, waveTotalDamage);
        }

        // 记录总日志
        context.addLog(LogType.DAMAGE,
                "【逐敌千箭】[%s] 完成了%d波箭雨攻击",
                caster.getName(), waveCount);
    }
}
