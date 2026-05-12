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
 * 以攻为守 - 游侠被动技能
 * 效果：每次造成暴击伤害时，提升自身x%物理防御与法术防御
 */
public class PassiveSkill_AttackToDefend extends PassiveSkill {

    private static final String ATTACK_TO_DEFEND_PHYSICAL_DEF_BUFF_ID = "attack_to_defend_physical_def_boost";
    private static final String ATTACK_TO_DEFEND_MAGICAL_DEF_BUFF_ID = "attack_to_defend_magical_def_boost";

    public PassiveSkill_AttackToDefend(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCrit(BattleEntity entity, BattleContext context) {
        int defenseBonusPercent = getEffectParams().x;

        // 创建物理防御提升buff（持续较长，允许累积）
        AttributeBuff physicalDefBuff = new AttributeBuff(
                ATTACK_TO_DEFEND_PHYSICAL_DEF_BUFF_ID,
                "以攻为守-物防",
                "物理防御提升%d%%",
                BuffType.BUFF,
                false, // 不可驱散
                -1,    // 永久持续
                999,   // 最大层数（允许累积）
                true,  // 刷新
                defenseBonusPercent / 100.0f,  // 转换为小数
                AttributeType.PHYSICAL_DEF,
                ValueType.PERCENTAGE
        );

        // 创建法术防御提升buff
        AttributeBuff magicalDefBuff = new AttributeBuff(
                ATTACK_TO_DEFEND_MAGICAL_DEF_BUFF_ID,
                "以攻为守-法防",
                "法术防御提升%d%%",
                BuffType.BUFF,
                false,
                -1,
                999,
                true,
                defenseBonusPercent / 100.0f,  // 转换为小数
                AttributeType.MAGICAL_DEF,
                ValueType.PERCENTAGE
        );

        BuffManager.getInstance(entity.getContext()).addBuff(entity, physicalDefBuff);
        BuffManager.getInstance(entity.getContext()).addBuff(entity, magicalDefBuff);
        entity.markAttributeCacheDirty();

        context.addLog(LogType.BUFF,
                "【以攻为守】[%s] 暴击后物理防御与法术防御提升%d%%",
                entity.getName(), defenseBonusPercent);
    }
}
