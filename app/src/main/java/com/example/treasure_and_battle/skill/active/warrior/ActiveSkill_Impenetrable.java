package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.skill.ImpenetrableBuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 固若金汤 - 战士主动技能
 * 效果：本回合受到的所有生命伤害都会以x%比例转化为自身护盾
 * 注意：只计算HP实际收到的伤害，不包括护盾吸收、减伤、抵挡等
 */
public class ActiveSkill_Impenetrable extends ActiveSkill {
    public ActiveSkill_Impenetrable(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int conversionPercent = getEffectParams().x;  // 转化比例

        // 创建固若金汤buff
        ImpenetrableBuff imprenetrableBuff = new ImpenetrableBuff(
            "impenetrable",
            "固若金汤",
            "将受到伤害的%d%%转化为护盾",
            BuffType.BUFF,
            true,  // 可驱散
            1,    // 持续1回合（本回合）
            1,    // 最多1层
            false,
            1.0f,
            conversionPercent,
            BuffManager.getInstance(caster.getContext())
        );

        // 应用buff
        battleManager.applyBuff(caster, imprenetrableBuff);

        // 记录日志
        context.addLog(LogType.BUFF,
            "【固若金汤】[%s] 将受到伤害的 %d%% 转化为护盾",
            caster.getName(), conversionPercent);
    }
}
