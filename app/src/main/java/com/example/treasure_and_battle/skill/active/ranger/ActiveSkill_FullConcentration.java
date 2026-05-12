package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 全神贯注 - 游侠主动技能
 * 效果：自身暴击率提升x%，暴击伤害提升y%
 */
public class ActiveSkill_FullConcentration extends ActiveSkill {

    public ActiveSkill_FullConcentration(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        // 获取效果参数
        int critRateBoost = getEffectParams().x;    // 暴击率提升（百分比）
        int critDamageBoost = getEffectParams().y;  // 暴击伤害提升（百分比）

        // 创建暴击率提升buff（持续1回合）
        // 注意：critRateBoost是百分比形式（如2代表2%），需要除以100转换为小数
        AttributeBuff critRateBuff = new AttributeBuff(
                "full_concentration_crit_rate",
                "全神贯注-暴击率",
                "暴击率提升%d%%",
                BuffType.BUFF,
                false, // 不可驱散
                1,     // 持续1回合
                1,     // 最大层数
                false, // 不刷新
                critRateBoost / 100.0f,  // 将百分比转换为小数（2% -> 0.02）
                AttributeType.PHYSICAL_CRIT_RATE,
                com.example.treasure_and_battle.model.common.ValueType.FLAT  // 已经转换为小数，使用FLAT
        );

        // 创建暴击伤害提升buff（持续1回合）
        // 注意：critDamageBoost是百分比形式（如4代表4%），需要除以100转换为小数
        AttributeBuff critDamageBuff = new AttributeBuff(
                "full_concentration_crit_damage",
                "全神贯注-暴击伤害",
                "暴击伤害提升%d%%",
                BuffType.BUFF,
                false, // 不可驱散
                1,     // 持续1回合
                1,     // 最大层数
                false, // 不刷新
                critDamageBoost / 100.0f,  // 将百分比转换为小数（4% -> 0.04）
                AttributeType.PHYSICAL_CRIT_DMG,
                com.example.treasure_and_battle.model.common.ValueType.FLAT  // 已经转换为小数，使用FLAT
        );

        // 应用buff
        battleManager.applyBuff(caster, critRateBuff);
        battleManager.applyBuff(caster, critDamageBuff);
        caster.markAttributeCacheDirty();

        // 记录日志
        context.addLog(LogType.BUFF,
                "【全神贯注】[%s] 凝神聚气，暴击率提升%d%%，暴击伤害提升%d%%",
                caster.getName(), critRateBoost, critDamageBoost);
    }
}
