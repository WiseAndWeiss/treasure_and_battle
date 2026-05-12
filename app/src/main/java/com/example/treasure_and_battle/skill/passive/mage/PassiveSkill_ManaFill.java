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
 * 魔力充盈 - 法师被动技能
 * 效果：回合开始时恢复x%最大MP；若MP已满，则本回合魔法攻击提升y%
 */
public class PassiveSkill_ManaFill extends PassiveSkill {

    // 用于跟踪魔法攻击提升buff的ID
    private static final String MANA_FILL_BUFF_ID = "mana_fill_magical_atk_boost";

    public PassiveSkill_ManaFill(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onRoundStart(BattleEntity owner, BattleContext context) {
        int manaRestorePercent = getEffectParams().x; // MP恢复百分比
        int magicalAtkBoostPercent = getEffectParams().y; // 魔法攻击提升百分比

        int maxMp = owner.getFinalAttributes().maxMp;
        int currentMp = owner.getCurrentMp();

        // 检查MP是否已满
        if (currentMp >= maxMp) {
            // MP已满，添加魔法攻击提升buff
            int magicalAtkBoost = magicalAtkBoostPercent;

            // 检查是否已有该buff
            AttributeBuff existingBuff = null;
            for (com.example.treasure_and_battle.buff.BaseBuff buff : owner.getActiveBuffList()) {
                if (buff instanceof AttributeBuff && buff.getBuffId().equals(MANA_FILL_BUFF_ID)) {
                    existingBuff = (AttributeBuff) buff;
                    break;
                }
            }

            if (existingBuff == null) {
                // 创建新的魔法攻击提升buff（持续1回合）
                AttributeBuff magicalAtkBuff = new AttributeBuff(
                        MANA_FILL_BUFF_ID,
                        "魔力充盈-魔法攻击",
                        "魔法攻击提升%d%%",
                        BuffType.BUFF,
                        false, // 不可驱散
                        1,     // 持续1回合
                        1,     // 最大层数
                        false, // 不刷新
                        magicalAtkBoost,
                        AttributeType.MAGICAL_ATK,
                        com.example.treasure_and_battle.model.common.ValueType.PERCENTAGE
                );

                BuffManager.getInstance(owner.getContext()).addBuff(owner, magicalAtkBuff);
                owner.markAttributeCacheDirty();

                context.addLog(LogType.BUFF,
                        "【魔力充盈】[%s] MP已满，魔法攻击提升%d%%，持续1回合！",
                        owner.getName(), magicalAtkBoost);
            }
        } else {
            // MP未满，恢复MP
            int manaRestore = (int) (maxMp * manaRestorePercent / 100.0f);
            int newMp = Math.min(currentMp + manaRestore, maxMp);
            int actualRestore = newMp - currentMp;
            owner.setCurrentMp(newMp);

            context.addLog(LogType.HEAL,
                    "【魔力充盈】[%s] 回复了%d点MP",
                    owner.getName(), actualRestore);
        }
    }
}
