package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.attribute.WeaknessDebuff;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignal;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignalPipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 苦口良药技能 - 法师主动技能
 *
 * 技能效果：
 * - 为自身附加2回合10%虚弱buff
 * - 瞬间恢复x%最大血量与y%最大蓝量
 * - 冷却时间：4回合
 */
public class ActiveSkill_BitterMedicine extends ActiveSkill {

    public ActiveSkill_BitterMedicine(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        // 发送苦口良药动画信号
        String casterId = (caster instanceof Monster) ? ((Monster) caster).getAnimationId() : caster.getEntityId();
        AnimationSignalPipeline.getInstance().emitSignal(
            new AnimationSignal("skill_bitter_medicine", casterId, new ArrayList<>(), null)
        );

        BattleContext context = battleManager.getContext();

        int healPercent = getEffectParams().x;
        int manaRestorePercent = getEffectParams().y;

        // 1. 计算恢复量
        int maxHp = caster.getFinalAttributes().maxHp;
        int maxMp = caster.getFinalAttributes().maxMp;
        int healAmount = (int) (maxHp * healPercent / 100.0f);
        int manaRestoreAmount = (int) (maxMp * manaRestorePercent / 100.0f);

        // 2. 恢复HP
        int currentHp = caster.getCurrentHp();
        int newHp = Math.min(currentHp + healAmount, maxHp);
        int actualHeal = newHp - currentHp;
        caster.setCurrentHp(newHp);

        // 3. 恢复MP
        int currentMp = caster.getCurrentMp();
        int newMp = Math.min(currentMp + manaRestoreAmount, maxMp);
        int actualManaRestore = newMp - currentMp;
        caster.setCurrentMp(newMp);

        // 4. 添加虚弱debuff（降低10%攻击力，持续2回合）
        WeaknessDebuff weaknessDebuff = new WeaknessDebuff(
                "bitter_medicine_weakness",
                "苦口良药虚弱",
                "因服药导致身形虚弱，攻击力降低10%%",
                BuffType.DEBUFF,
                true,  // 可驱散
                2,     // 持续2回合
                1,     // 最大层数
                false, // 不刷新
                0,
                10     // 降低10%攻击力
        );

        BuffManager.getInstance(caster.getContext()).addBuff(caster, weaknessDebuff);

        // 5. 记录日志
        context.addLog(LogType.HEAL,
                "【苦口良药】[%s] 服下良药，恢复了%d点HP和%d点MP，但陷入虚弱状态（攻击力降低10%%），持续2回合！",
                caster.getName(), actualHeal, actualManaRestore);

        context.addLog(LogType.BUFF,
                "【苦口良药】[%s] 获得了虚弱效果，攻击力降低10%%，持续2回合",
                caster.getName());
    }
}
