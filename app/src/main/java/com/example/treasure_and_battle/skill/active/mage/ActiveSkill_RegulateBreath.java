package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.periodic.RegeneratingBuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 调息技能 - 法师恢复技能
 *
 * 技能效果：
 * - 恢复x点HP和y点MP
 * - 获得再生buff（持续z回合，5%最大生命值/回合）
 */
public class ActiveSkill_RegulateBreath extends ActiveSkill {

    public ActiveSkill_RegulateBreath(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        BattleContext context = battleManager.getContext();

        int healAmount = getEffectParams().x;
        int manaRestoreAmount = getEffectParams().y;
        int regenDuration = getEffectParams().z;

        // 1. 恢复HP
        int currentHp = caster.getCurrentHp();
        int maxHp = caster.getFinalAttributes().maxHp;
        int newHp = Math.min(currentHp + healAmount, maxHp);
        int actualHeal = newHp - currentHp;
        caster.setCurrentHp(newHp);

        // 2. 恢复MP
        int currentMp = caster.getCurrentMp();
        int maxMp = caster.getFinalAttributes().maxMp;
        int newMp = Math.min(currentMp + manaRestoreAmount, maxMp);
        int actualManaRestore = newMp - currentMp;
        caster.setCurrentMp(newMp);

        // 记录恢复日志
        context.addLog(LogType.HEAL,
            "【%s】%s恢复了%d点生命值和%d点魔法值",
            getSkillName(), caster.getName(), actualHeal, actualManaRestore);

        // 3. 如果有持续时间，添加再生buff
        if (regenDuration > 0) {
            // 直接创建再生buff
            RegeneratingBuff regenBuff = new RegeneratingBuff(
                "regulate_bath_regen",
                "再生",
                "回合结束时恢复相当于层数*1%最大生命值的生命值（当前层数：%2$d）",
                BuffType.BUFF,
                true,  // 可驱散
                regenDuration,
                5,     // 5层（每层1%）
                true,  // 刷新
                1.0f,  // buffValue（1%）
                com.example.treasure_and_battle.model.common.ValueType.PERCENTAGE
            );
            caster.getActiveBuffList().add(regenBuff);
            caster.markAttributeCacheDirty();

            context.addLog(LogType.BUFF,
                "【%s】%s获得了%d%%生命值再生（持续%d回合）",
                getSkillName(), caster.getName(), 5, regenDuration);
        }
    }
}
