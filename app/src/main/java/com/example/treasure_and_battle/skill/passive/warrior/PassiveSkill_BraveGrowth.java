package com.example.treasure_and_battle.skill.passive.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

/**
 * 越战越勇 - 战士被动技能
 * 效果：每经过一个回合，物理攻击与物理防御额外提升x%（叠加）
 */
public class PassiveSkill_BraveGrowth extends PassiveSkill {

    public PassiveSkill_BraveGrowth(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onRoundEnd(BattleEntity owner, BattleContext context) {
        int statBoostPercent = getEffectParams().x; // 每回合提升百分比

        if (statBoostPercent <= 0) {
            return;
        }

        // 直接修改百分比修饰池（这样会叠加）
        AttributeSet baseAttr = owner.getBaseAttributes();
        baseAttr.percentPhysicalAtk += statBoostPercent / 100.0f;
        baseAttr.percentPhysicalDef += statBoostPercent / 100.0f;

        owner.markAttributeCacheDirty();

        context.addLog(LogType.BUFF,
            "【越战越勇】[%s] 的战斗经验增长，物理攻击与物理防御额外提升 %d%%",
            owner.getName(), statBoostPercent);
    }
}
