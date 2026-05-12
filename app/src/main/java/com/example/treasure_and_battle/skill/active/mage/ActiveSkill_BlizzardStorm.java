package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.control.FrozenDebuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;
import java.util.Random;

/**
 * 暴风骤雪技能 - 法师AOE控制技能
 *
 * 技能效果：
 * - 对全体敌人造成x%法术伤害
 * - 目标有y%概率被冻结1回合
 */
public class ActiveSkill_BlizzardStorm extends ActiveSkill {

    private final Random random = new Random();

    public ActiveSkill_BlizzardStorm(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        float freezeChance = getEffectParams().y;

        int frozenCount = 0;

        // 对所有目标敌人造成伤害并尝试冻结
        for (BattleEntity enemy : targets) {
            if (enemy.isDead()) continue;

            // 计算法术伤害
            int magicalAtk = caster.getFinalAttributes().magicalAtk;
            int baseDamage = (int) (magicalAtk * damagePercent / 100.0f);

            // 造成魔法伤害
            battleManager.dealMagicalDamage(caster, enemy, baseDamage, context);

            // 如果目标还活着，尝试冻结
            if (!enemy.isDead()) {
                float roll = random.nextFloat() * 100;
                if (roll < freezeChance) {
                    // 添加冻结debuff
                    FrozenDebuff frozenDebuff = new FrozenDebuff(
                        "blizzard_freeze",
                        "暴风冻结",
                        "被冻结，无法行动",
                        BuffType.DEBUFF,
                        false, // 不可驱散（冻结通常无法被驱散）
                        1,     // 持续1回合
                        1,     // 不堆叠
                        false, // 不刷新
                        0      // buffValue（不使用）
                    );
                    enemy.getActiveBuffList().add(frozenDebuff);
                    enemy.markAttributeCacheDirty();
                    frozenCount++;

                    context.addLog(LogType.ACTION,
                        "【%s】%s被寒冰冻结，无法行动！",
                        getSkillName(), enemy.getName());
                }
            }
        }

        // 记录总伤害日志
        context.addLog(LogType.ACTION,
            "【%s】暴风骤雪席卷战场，造成了极大伤害，%d个敌人被冻结",
            getSkillName(), frozenCount);
    }
}
