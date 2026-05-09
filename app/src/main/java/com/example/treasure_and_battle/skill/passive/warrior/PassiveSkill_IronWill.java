package com.example.treasure_and_battle.skill.passive.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.buff.impl.periodic.BleedingDebuff;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.buff.impl.attribute.WeaknessDebuff;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

import java.util.ArrayList;
import java.util.List;

/**
 * 铁血意志 - 战士被动技能
 * 效果：回合开始时若生命值低于最大生命值的x%，则物理攻击与物理防御提升y%，立即驱散流血/虚弱/中毒效果
 * 注意：属性加成不累积，血量恢复到阈值以上时移除属性加成
 */
public class PassiveSkill_IronWill extends PassiveSkill {

    private AttributeBuff physicalBuff = null; // 追踪物攻物防buff

    public PassiveSkill_IronWill(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onRoundStart(BattleEntity owner, BattleContext context) {
        int hpThresholdPercent = getEffectParams().x; // HP阈值百分比
        int statBoostPercent = getEffectParams().y; // 属性提升百分比

        int currentHp = owner.getCurrentHp();
        int maxHp = owner.getFinalAttributes().maxHp;
        float hpPercent = (float) currentHp / maxHp * 100;

        boolean isBelowThreshold = hpPercent <= hpThresholdPercent;

        if (isBelowThreshold) {
            // HP低于阈值：添加属性buff，驱散debuff
            if (physicalBuff == null || !owner.getActiveBuffList().contains(physicalBuff)) {
                // 创建物攻物防提升buff
                physicalBuff = new AttributeBuff(
                    "iron_will_stat_boost",
                    "铁血意志",
                    "物理攻击与物理防御提升%d%%",
                    BuffType.BUFF,
                    false,  // 不可驱散
                    -1,    // 持续到战斗结束或血量恢复
                    1,     // 最多1层
                    false,
                    statBoostPercent / 100.0f, // 转换为小数
                    AttributeType.PHYSICAL_ATK,
                    ValueType.PERCENTAGE
                );

                owner.getActiveBuffList().add(physicalBuff);
                owner.markAttributeCacheDirty();

                context.addLog(LogType.BUFF,
                    "【铁血意志】[%s] 生命值低于 %d%%，物理攻击与物理防御提升 %d%%",
                    owner.getName(), hpThresholdPercent, statBoostPercent);
            }

            // 驱散流血、虚弱、中毒debuff
            List<BaseBuff> buffsToRemove = new ArrayList<>();
            List<BaseBuff> currentBuffs = owner.getActiveBuffList();

            for (BaseBuff buff : currentBuffs) {
                if (buff.getBuffType() != BuffType.DEBUFF) {
                    continue;
                }

                // 检查是否是需要驱散的debuff类型
                if (buff instanceof BleedingDebuff ||
                    buff instanceof WeaknessDebuff ||
                    buff instanceof PoisoningDebuff) {
                    buffsToRemove.add(buff);
                }
            }

            // 移除找到的debuff
            for (BaseBuff buff : buffsToRemove) {
                BuffManager.getInstance(owner.getContext()).removeBuff(owner, buff.getBuffId());
                context.addLog(LogType.BUFF,
                    "【铁血意志】[%s] 驱散了 [%s] 效果",
                    owner.getName(), buff.getBuffName());
            }

        } else {
            // HP恢复到阈值以上：移除属性buff
            if (physicalBuff != null && owner.getActiveBuffList().contains(physicalBuff)) {
                owner.getActiveBuffList().remove(physicalBuff);
                owner.markAttributeCacheDirty();
                physicalBuff = null;

                context.addLog(LogType.BUFF,
                    "【铁血意志】[%s] 生命值恢复到 %d%% 以上，移除了属性加成",
                    owner.getName(), hpThresholdPercent);
            }
        }
    }
}
