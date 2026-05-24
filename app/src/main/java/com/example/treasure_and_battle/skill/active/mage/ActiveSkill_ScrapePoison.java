package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignal;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignalPipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 刮骨疗毒技能 - 法师主动技能
 *
 * 技能效果：
 * - 对单个敌人造成x点基础伤害
 * - 若目标存在中毒buff，清空所有中毒层数，每层额外造成y%法术攻击伤害
 * - 类似战士的汲血技能，但是是伤害而不是回血
 * - 冷却时间：1回合
 */
public class ActiveSkill_ScrapePoison extends ActiveSkill {

    public ActiveSkill_ScrapePoison(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        // 发送刮骨疗毒动画信号
        List<String> targetEntityIds = new ArrayList<>();
        for (BattleEntity target : targets) {
            String targetId = (target instanceof Monster) ? ((Monster) target).getAnimationId() : target.getEntityId();
            targetEntityIds.add(targetId);
        }

        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_scrape_poison", casterId, targetEntityIds, null)
        );

        BattleEntity target = targets.get(0);
        BattleContext context = battleManager.getContext();

        int baseDamage = getEffectParams().x;
        int bonusDamagePercentPerStack = getEffectParams().y;

        // 1. 造成基础伤害
        int damageDealt = battleManager.dealMagicalDamage(caster, target, baseDamage, context);

        // 2. 检查目标是否有中毒debuff
        PoisoningDebuff poisoningDebuff = null;
        for (BaseBuff buff : target.getActiveBuffList()) {
            if (buff instanceof PoisoningDebuff) {
                poisoningDebuff = (PoisoningDebuff) buff;
                break;
            }
        }

        int totalBonusDamage = 0;

        if (poisoningDebuff != null && poisoningDebuff.getStackCount() > 0) {
            // 3. 清空所有中毒层数并造成额外伤害
            int poisonStacks = poisoningDebuff.getStackCount();
            int magicalAtk = caster.getFinalAttributes().magicalAtk;
            totalBonusDamage = (int) (magicalAtk * bonusDamagePercentPerStack / 100.0f * poisonStacks);

            // 移除中毒debuff
            BuffManager.getInstance(caster.getContext()).removeBuff(target, poisoningDebuff.getBuffId());

            // 造成额外伤害
            if (totalBonusDamage > 0) {
                battleManager.dealMagicalDamage(caster, target, totalBonusDamage, context);
            }

            // 记录日志
            context.addLog(LogType.ACTION,
                    "【刮骨疗毒】[%s] 引爆了 [%s] 的 %d 层中毒，造成额外%d点伤害！",
                    caster.getName(), target.getName(), poisonStacks, totalBonusDamage);
        }

        // 记录总伤害日志
        int totalDamage = damageDealt + totalBonusDamage;
        context.addLog(LogType.ACTION,
                "【刮骨疗毒】[%s] 对 [%s] 造成了共计 %d 点伤害",
                caster.getName(), target.getName(), totalDamage);
    }
}
