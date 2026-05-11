package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.skill.CounterStanceBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 反击姿态 - 通用主动技能
 * 效果：消耗2点行动点，本回合受到的伤害减少{x}%，且每受到一次攻击，就对对手发动一次普通攻击反击
 */
public class ActiveSkill_CounterStance extends ActiveSkill {
    public ActiveSkill_CounterStance(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int damageReductionPercent = getEffectParams().x;

        // 创建反击姿态buff（持续1回合）
        CounterStanceBuff counterStanceBuff = new CounterStanceBuff(
            "counter_stance_buff",
            "反击姿态",
            "受到的伤害减少%d%%，每次受击反击",
            BuffType.BUFF,
            true,
            1,
            1,
            false,
            1.0f,
            damageReductionPercent
        );

        // 添加buff到施法者
        battleManager.applyBuff(caster, counterStanceBuff);

        // 记录日志
        context.addLog(LogType.BUFF,
            "【反击姿态】[%s] 摆出了防御反击架势，本回合受到的伤害减少 %d%%，每次受击都会反击",
            caster.getName(), damageReductionPercent);
    }
}
