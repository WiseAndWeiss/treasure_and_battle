package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 战斗姿态 - 战士主动技能
 * 效果：自身物理攻击、物理防御与速度提升x%
 */
public class ActiveSkill_BattleStance extends ActiveSkill {
    public ActiveSkill_BattleStance(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int boostPercent = getEffectParams().x;  // 提升百分比（如5表示5%）

        // 创建3个属性buff：物理攻击、物理防御、速度
        // 注意：ValueType.PERCENTAGE使用小数表示（0.05 = 5%）
        AttributeBuff physicalAtkBuff = new AttributeBuff(
            "battle_stance_atk", "战斗姿态-攻击", "物理攻击提升%d%%",
            BuffType.BUFF, true, 2, 1, false, boostPercent / 100.0f,
            AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE
        );

        AttributeBuff physicalDefBuff = new AttributeBuff(
            "battle_stance_def", "战斗姿态-防御", "物理防御提升%d%%",
            BuffType.BUFF, true, 2, 1, false, boostPercent / 100.0f,
            AttributeType.PHYSICAL_DEF, ValueType.PERCENTAGE
        );

        AttributeBuff speedBuff = new AttributeBuff(
            "battle_stance_spd", "战斗姿态-速度", "速度提升%d%%",
            BuffType.BUFF, true, 2, 1, false, boostPercent / 100.0f,
            AttributeType.SPEED, ValueType.PERCENTAGE
        );

        // 应用所有buff
        battleManager.applyBuff(caster, physicalAtkBuff);
        battleManager.applyBuff(caster, physicalDefBuff);
        battleManager.applyBuff(caster, speedBuff);

        // 记录日志
        context.addLog(LogType.BUFF,
            "【战斗姿态】[%s] 进入战斗状态，物理攻击、防御与速度提升 %d%%，持续2回合",
            caster.getName(), boostPercent);
    }
}
