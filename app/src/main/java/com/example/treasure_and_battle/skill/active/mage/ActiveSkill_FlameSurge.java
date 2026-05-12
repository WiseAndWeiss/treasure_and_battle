package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.skill.FlameSurgeShieldBuff;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 炽能涌动技能 - 法师防御技能
 *
 * 技能效果：
 * - 生成x点护盾
 * - 护盾存在期间受击有y%概率对攻击者附加z层燃烧
 */
public class ActiveSkill_FlameSurge extends ActiveSkill {

    public ActiveSkill_FlameSurge(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int shieldAmount = getEffectParams().x;
        float triggerChance = getEffectParams().y;
        int burningStacks = getEffectParams().z;

        // 创建炽能涌动护盾
        FlameSurgeShieldBuff shieldBuff = new FlameSurgeShieldBuff(
            "flame_surge_shield",
            "炽能涌动",
            "吸收%d点伤害，受击有%.0f%%概率触发燃烧反噬",
            BuffType.BUFF,
            true,  // 可驱散
            1,     // 持续1回合
            (int)shieldAmount, // 护盾值
            false, // 不刷新
            0,     // buffValue（不使用）
            triggerChance,
            burningStacks
        );

        caster.getActiveBuffList().add(shieldBuff);
        caster.markAttributeCacheDirty();

        // 记录战斗日志
        context.addLog(LogType.ACTION,
            "【%s】%s生成了%d点炽能护盾，受击有%.0f%%概率触发%d层燃烧反噬",
            getSkillName(), caster.getName(), shieldAmount, triggerChance, burningStacks);
    }
}
