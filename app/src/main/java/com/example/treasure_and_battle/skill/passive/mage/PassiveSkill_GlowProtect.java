package com.example.treasure_and_battle.skill.passive.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 辉光庇护 - 法师被动技能
 * 效果：战斗开始时，提升自身精神属性千分之{x}的闪避率与异常抵抗率
 */
public class PassiveSkill_GlowProtect extends PassiveSkill {

    private static final String GLOW_PROTECT_DODGE_BUFF_ID = "glow_protect_dodge";
    private static final String GLOW_PROTECT_RESIST_BUFF_ID = "glow_protect_resist";

    public PassiveSkill_GlowProtect(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onBattleStart(BattleEntity owner, BattleContext context) {
        int spiritMultiplier = getEffectParams().x; // 精神属性千分比

        // 获取精神属性
        int spirit = owner.getFinalAttributes().spirit;
        int dodgeBoost = (int) (spirit * spiritMultiplier / 1000.0f);
        int resistBoost = (int) (spirit * spiritMultiplier / 1000.0f);

        // 创建闪避率提升buff（永久，直到战斗结束）
        AttributeBuff dodgeBuff = new AttributeBuff(
                GLOW_PROTECT_DODGE_BUFF_ID,
                "辉光庇护-闪避",
                "闪避率提升%d%%",
                BuffType.BUFF,
                false, // 不可驱散
                -1,    // 永久（直到战斗结束）
                1,     // 最大层数
                false, // 不刷新
                dodgeBoost,
                AttributeType.DODGE_RATE,
                com.example.treasure_and_battle.model.common.ValueType.FLAT
        );

        // 创建异常抵抗率提升buff（永久，直到战斗结束）
        AttributeBuff resistBuff = new AttributeBuff(
                GLOW_PROTECT_RESIST_BUFF_ID,
                "辉光庇护-抵抗",
                "异常抵抗率提升%d%%",
                BuffType.BUFF,
                false, // 不可驱散
                -1,    // 永久（直到战斗结束）
                1,     // 最大层数
                false, // 不刷新
                resistBoost,
                AttributeType.DEBUFF_RESIST,
                com.example.treasure_and_battle.model.common.ValueType.FLAT
        );

        BuffManager.getInstance(owner.getContext()).addBuff(owner, dodgeBuff);
        BuffManager.getInstance(owner.getContext()).addBuff(owner, resistBuff);
        owner.markAttributeCacheDirty();

        context.addLog(LogType.BUFF,
                "【辉光庇护】[%s] 精神力场护体，闪避率与异常抵抗率各提升%d点（基于精神%d x 千分之%d）",
                owner.getName(), dodgeBoost, spirit, spiritMultiplier);
    }
}
