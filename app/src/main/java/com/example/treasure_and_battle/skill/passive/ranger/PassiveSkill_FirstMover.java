package com.example.treasure_and_battle.skill.passive.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 先机 - 游侠被动技能
 * 效果：每回合开始时，自身速度提升x%
 */
public class PassiveSkill_FirstMover extends PassiveSkill {

    private static final String FIRST_MOVER_BUFF_ID = "first_mover_speed_boost";

    public PassiveSkill_FirstMover(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onRoundStart(BattleEntity entity, BattleContext context) {
        int speedBonusPercent = getEffectParams().x;

        // 创建速度提升buff（永久持续，直到战斗结束）
        AttributeBuff speedBuff = new AttributeBuff(
                FIRST_MOVER_BUFF_ID,
                "先机-速度",
                "速度提升%d%%",
                BuffType.BUFF,
                false, // 不可驱散
                -1,    // 永久持续（直到战斗结束）
                999,   // 最大层数（允许累积）
                true,  // 刷新（每次回合开始刷新层数和持续时间）
                speedBonusPercent / 100.0f,  // 转换为小数
                AttributeType.SPEED,
                ValueType.PERCENTAGE
        );

        int beforeSize = entity.getActiveBuffList().size();
        BuffManager.getInstance(entity.getContext()).addBuff(entity, speedBuff);
        int afterSize = entity.getActiveBuffList().size();

        context.addLog(LogType.BUFF,
                "【先机】[%s] 速度提升%d%%，buff数量: %d -> %d",
                entity.getName(), speedBonusPercent, beforeSize, afterSize);
    }
}
