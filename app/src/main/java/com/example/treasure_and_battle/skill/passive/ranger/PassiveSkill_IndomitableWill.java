package com.example.treasure_and_battle.skill.passive.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.WeaknessDebuff;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.buff.impl.control.BlindnessDebuff;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.BurningDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 不屈之志 - 游侠被动技能
 * 效果：回合开始时，若自身持有减速/燃烧/中毒/流血/致盲/虚弱debuff，本回合提升x%物理攻击与y%暴击伤害
 */
public class PassiveSkill_IndomitableWill extends PassiveSkill {

    private static final String INDOMITABLE_WILL_ATK_BUFF_ID = "indomitable_will_atk_boost";
    private static final String INDOMITABLE_WILL_CRIT_DMG_BUFF_ID = "indomitable_will_crit_dmg_boost";

    public PassiveSkill_IndomitableWill(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onRoundStart(BattleEntity entity, BattleContext context) {
        // 检查是否拥有指定的debuff
        boolean hasRequiredDebuff = entity.getActiveBuffList().stream()
                .anyMatch(buff -> buff instanceof SlowDebuff
                        || buff instanceof BurningDebuff
                        || buff instanceof PoisoningDebuff
                        || buff instanceof BleedingDebuff
                        || buff instanceof BlindnessDebuff
                        || buff instanceof WeaknessDebuff);

        if (!hasRequiredDebuff) {
            return;
        }

        int attackBonusPercent = getEffectParams().x;
        int critDamageBonusPercent = getEffectParams().y;

        // 创建物理攻击提升buff（持续1回合）
        AttributeBuff attackBuff = new AttributeBuff(
                INDOMITABLE_WILL_ATK_BUFF_ID,
                "不屈之志-物攻",
                "物理攻击提升%d%%",
                BuffType.BUFF,
                false,
                1,     // 持续1回合
                1,
                false,
                attackBonusPercent / 100.0f,  // 转换为小数
                AttributeType.PHYSICAL_ATK,
                ValueType.PERCENTAGE
        );

        // 创建暴击伤害提升buff
        AttributeBuff critDmgBuff = new AttributeBuff(
                INDOMITABLE_WILL_CRIT_DMG_BUFF_ID,
                "不屈之志-暴伤",
                "暴击伤害提升%d%%",
                BuffType.BUFF,
                false,
                1,
                1,
                false,
                critDamageBonusPercent / 100.0f,  // 转换为小数
                AttributeType.PHYSICAL_CRIT_DMG,
                ValueType.PERCENTAGE
        );

        BuffManager.getInstance(entity.getContext()).addBuff(entity, attackBuff);
        BuffManager.getInstance(entity.getContext()).addBuff(entity, critDmgBuff);
        entity.markAttributeCacheDirty();

        context.addLog(LogType.BUFF,
                "【不屈之志】[%s] 身处异常状态，物理攻击提升%d%%，暴击伤害提升%d%%",
                entity.getName(), attackBonusPercent, critDamageBonusPercent);
    }
}
