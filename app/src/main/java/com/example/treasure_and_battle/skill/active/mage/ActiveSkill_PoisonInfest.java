package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 诡毒侵染技能 - 法师主动技能
 *
 * 技能效果：
 * - 对单个敌人造成x点固定伤害
 * - 附加y层中毒效果
 * - 冷却时间：1回合
 */
public class ActiveSkill_PoisonInfest extends ActiveSkill {

    public ActiveSkill_PoisonInfest(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        int fixedDamage = getEffectParams().x;
        int poisonStacks = getEffectParams().y;

        // 1. 造成固定伤害
        battleManager.dealTrueDamage(target, fixedDamage, context);
        int damageDealt = fixedDamage; // 真实伤害固定值

        // 2. 添加中毒debuff
        PoisoningDebuff existingPoison = null;
        for (BaseBuff buff : target.getActiveBuffList()) {
            if (buff instanceof PoisoningDebuff) {
                existingPoison = (PoisoningDebuff) buff;
                break;
            }
        }

        if (existingPoison != null) {
            // 如果已有中毒debuff，叠加层数
            existingPoison.stackPoisoning(poisonStacks);
            context.addLog(LogType.BUFF,
                    "【诡毒侵染】[%s] 的中毒效果加深，增加了%d层，当前层数：%d",
                    target.getName(), poisonStacks, existingPoison.getStackCount());
        } else {
            // 如果没有中毒debuff，创建新的
            PoisoningDebuff newPoison = new PoisoningDebuff(
                    "poison_infest",
                    "中毒",
                    "每层造成1点真实伤害，每回合层数减半",
                    BuffType.DEBUFF,
                    true,  // 可驱散
                    -1,    // 永久（直到层数归零）
                    999,   // 最大层数
                    true,  // 刷新
                    0
            );
            newPoison.setStack(poisonStacks);
            BuffManager.getInstance(caster.getContext()).addBuff(target, newPoison);
            context.addLog(LogType.BUFF,
                    "【诡毒侵染】[%s] 中毒了，层数：%d",
                    target.getName(), poisonStacks);
        }

        // 3. 记录伤害日志
        context.addLog(LogType.ACTION,
                "【诡毒侵染】[%s] 对 [%s] 造成了%d点固定伤害",
                caster.getName(), target.getName(), damageDealt);
    }
}
