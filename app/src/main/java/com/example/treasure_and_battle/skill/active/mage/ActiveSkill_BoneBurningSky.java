package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.damage.VulnerabilityDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 焚骨烬天技能 - 法师AOE攻击技能（消耗HP）
 *
 * 技能效果：
 * - 对全体敌人造成x%法术攻击伤害
 * - 附加y层燃烧和易伤效果（易伤增加20%受到的伤害）
 */
public class ActiveSkill_BoneBurningSky extends ActiveSkill {

    private static final float VULNERABILITY_BONUS = 20.0f; // 易伤增加20%伤害

    public ActiveSkill_BoneBurningSky(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int debuffStacks = getEffectParams().y;

        // 对所有目标敌人造成伤害
        for (BattleEntity enemy : targets) {
            if (enemy.isDead()) continue;

            // 计算法术伤害
            int magicalAtk = caster.getFinalAttributes().magicalAtk;
            int baseDamage = (int) (magicalAtk * damagePercent / 100.0f);

            // 造成魔法伤害
            battleManager.dealMagicalDamage(caster, enemy, baseDamage, context);

            // 如果目标还活着，附加燃烧和易伤debuff
            if (!enemy.isDead()) {
                // 添加燃烧
                BurningDebuff burningDebuff = new BurningDebuff(
                    "bone_burning_fire",
                    "燃烧",
                    "回合结束时受到相当于层数的魔法伤害（当前层数：%2$d），每回合衰减一半。",
                    BuffType.DEBUFF,
                    true,  // 可驱散
                    -1,    // 持续直到层数为0
                    10000, // maxStackCount（设置一个大值）
                    true,  // 刷新
                    1      // buffValue（每层伤害）
                );
                burningDebuff.setStack(debuffStacks);
                enemy.getActiveBuffList().add(burningDebuff);

                // 添加易伤
                VulnerabilityDebuff vulnerabilityDebuff = new VulnerabilityDebuff(
                    "vulnerability",
                    "易伤",
                    "受到的伤害增加%d%%",
                    BuffType.DEBUFF,
                    true,  // 可驱散
                    3,     // 持续3回合
                    debuffStacks,
                    true,  // 刷新
                    0,     // buffValue（不使用）
                    VULNERABILITY_BONUS
                );
                enemy.getActiveBuffList().add(vulnerabilityDebuff);
                enemy.markAttributeCacheDirty();

                context.addLog(LogType.ACTION,
                    "【%s】%s身附烈焰且防御削弱，获得%d层燃烧和%d层易伤",
                    getSkillName(), enemy.getName(), debuffStacks, debuffStacks);
            }
        }

        // 记录总伤害日志
        context.addLog(LogType.ACTION,
            "【%s】焚骨烈焰席卷全场，灼烧了所有敌人",
            getSkillName());
    }
}
