package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 旋风斩 - 战士主动技能
 * 效果：消耗自身所有行动点，每消耗1点行动点对所有敌方单位造成一次x%物理攻击伤害
 * 注意：配置中actionPointCost=1用于检查可施放性，实际需要额外扣除
 */
public class ActiveSkill_WhirlwindSlash extends ActiveSkill {
    public ActiveSkill_WhirlwindSlash(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int damagePercent = getEffectParams().x;

        int totalAp = caster.getCurrentActionPoints() + getActionPointCost();
        caster.setCurrentActionPoints(totalAp);

        int totalDamageDealt = 0;
        int hits = 0;
        int attackCount = 0;

        while (caster.getCurrentActionPoints() > 0) {
            caster.setCurrentActionPoints(caster.getCurrentActionPoints() - 1);
            attackCount++;

            // 检查是否所有敌人都死亡
            boolean allEnemiesDead = true;
            for (BattleEntity target : targets) {
                if (!target.isDead()) {
                    allEnemiesDead = false;
                    break;
                }
            }

            if (allEnemiesDead) {
                context.addLog(LogType.ACTION,
                    "【旋风斩】所有敌人已倒下，停止攻击");
                break;
            }

            // 对每个活着的敌人造成伤害
            for (BattleEntity target : targets) {
                if (target.isDead()) {
                    continue;  // 跳过已死亡的敌人
                }

                // 计算伤害
                int baseDamage = (int) (caster.getFinalAttributes().physicalAtk * damagePercent / 100.0f);
                int damageDealt = battleManager.dealPhysicalDamage(caster, target, baseDamage, context);

                totalDamageDealt += damageDealt;
                hits++;
            }
        }

        // 计算总共消耗的AP
        int totalApConsumed = attackCount;

        context.addLog(LogType.ACTION,
            "【旋风斩】[%s] 消耗了 %d 点行动点，发动 %d 次斩击",
            caster.getName(), totalApConsumed, attackCount);

        context.addLog(LogType.DAMAGE,
            "【旋风斩】总共造成 %d 点伤害（%d 次命中）",
            totalDamageDealt, hits);
    }
}
