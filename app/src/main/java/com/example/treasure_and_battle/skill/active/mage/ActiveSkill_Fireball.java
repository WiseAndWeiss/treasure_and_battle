package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 火球术技能 - 法师单体攻击技能
 *
 * 技能效果：
 * - 对目标造成x%法术攻击伤害
 * - 附加y层燃烧效果
 */
public class ActiveSkill_Fireball extends ActiveSkill {

    public ActiveSkill_Fireball(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        int damagePercent = getEffectParams().x;
        int burningStacks = getEffectParams().y;

        // 计算法术伤害
        int magicalAtk = caster.getFinalAttributes().magicalAtk;
        int baseDamage = (int) (magicalAtk * damagePercent / 100.0f);

        // 造成魔法伤害
        battleManager.dealMagicalDamage(caster, target, baseDamage, context);

        // 如果目标还活着，附加燃烧debuff
        if (!target.isDead()) {
            // 直接创建燃烧debuff
            BurningDebuff burningDebuff = new BurningDebuff(
                "fireball_burning",
                "燃烧",
                "回合结束时受到相当于层数的魔法伤害（当前层数：%2$d），每回合衰减一半。",
                BuffType.DEBUFF,
                true,  // 可驱散
                -1,    // 持续直到层数为0
                10000, // maxStackCount（设置一个大值）
                true,  // 刷新
                1      // buffValue（每层伤害）
            );
            // 手动设置stackCount
            burningDebuff.setStack(burningStacks);
            target.getActiveBuffList().add(burningDebuff);
            target.markAttributeCacheDirty();

            context.addLog(LogType.ACTION,
                "【%s】%s身附火焰，获得%d层燃烧",
                getSkillName(), target.getName(), burningStacks);
        }
    }
}
