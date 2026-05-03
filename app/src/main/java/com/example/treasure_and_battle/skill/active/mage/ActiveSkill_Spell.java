package com.example.treasure_and_battle.skill.active.mage;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.impl.control.SlowDebuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 咏唱 - 法师主动技能
 * 效果：对目标造成{x}%法术攻击伤害，附带{y}%减速，持续一回合
 */
public class ActiveSkill_Spell extends ActiveSkill {
    public ActiveSkill_Spell(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {
        if (targets.isEmpty()) {
            return;
        }

        BattleEntity target = targets.get(0);

        // 获取效果参数
        int damagePercent = getEffectParams().x;  // 伤害百分比
        int slowPercent = getEffectParams().y;    // 减速百分比

        // 计算基础伤害
        int baseDamage = (int) (caster.getFinalAttributes().magicalAtk * damagePercent / 100.0f);

        // 造成法术伤害
        int finalDamage = battleManager.dealMagicalDamage(caster, target, baseDamage, battleManager.getContext());

        // 施加减速buff（如果减速百分比>0）
        if (slowPercent > 0) {
            SlowDebuff slowDebuff = new SlowDebuff(
                "spell_slow",
                "咏唱减速",
                "速度降低%d%%",
                BuffType.DEBUFF,
                true,  // 可驱散
                1,    // 持续1回合
                1,    // 最大1层
                false,
                1.0f,
                slowPercent
            );

            battleManager.applyBuff(target, slowDebuff);
            battleManager.getContext().addLog(LogType.BUFF,
                "【咏唱】[%s] 对 [%s] 施加了 %d%% 减速，持续1回合",
                caster.getName(), target.getName(), slowPercent);
        }

        // 记录日志
        battleManager.getContext().addLog(LogType.DAMAGE,
            "【咏唱】[%s] 对 [%s] 造成 %d 点法术伤害",
            caster.getName(), target.getName(), finalDamage);
    }
}
