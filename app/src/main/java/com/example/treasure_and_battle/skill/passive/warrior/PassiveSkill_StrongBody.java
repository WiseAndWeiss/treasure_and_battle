package com.example.treasure_and_battle.skill.passive.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.damage.DamageReductionBuff;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 强健体魄 - 战士被动技能
 * 效果：战斗开始时，获得x点体魄，整场战斗受到所有伤害降低y%
 */
public class PassiveSkill_StrongBody extends PassiveSkill {

    // 用于追踪是否已经添加过伤害降低buff
    private boolean hasAppliedBuff = false;

    public PassiveSkill_StrongBody(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onBattleStart(BattleEntity owner, BattleContext context) {
        int physiqueBonus = getEffectParams().x;
        int damageReductionPercent = getEffectParams().y;

        // 增加体魄
        if (physiqueBonus > 0) {
            owner.getBaseAttributes().physique += physiqueBonus;
            owner.markAttributeCacheDirty();

            context.addLog(LogType.BUFF,
                "【强健体魄】[%s] 的体魄增加了 %d 点",
                owner.getName(), physiqueBonus);
        }

        // 添加全局伤害降低buff（永久，不可驱散）
        if (damageReductionPercent > 0) {
            DamageReductionBuff damageReductionBuff = new DamageReductionBuff(
                "strong_body_damage_reduction",
                "强健体魄",
                "受到的所有伤害降低%d%%",
                com.example.treasure_and_battle.model.buff.BuffType.BUFF,
                false,  // 不可驱散
                -1,    // 永久（直到战斗结束）
                1,     // 最多1层
                false,
                1.0f,
                damageReductionPercent
            );

            owner.getActiveBuffList().add(damageReductionBuff);
            owner.markAttributeCacheDirty();
            hasAppliedBuff = true;

            context.addLog(LogType.BUFF,
                "【强健体魄】[%s] 获得了强健体魄，受到的所有伤害降低 %d%%",
                owner.getName(), damageReductionPercent);
        }
    }
}
