package com.example.treasure_and_battle.skill;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.model.skill.SkillType;

public abstract class PassiveSkill extends Skill {
    public PassiveSkill(SkillTemplate skillTemplate) {
        super(skillTemplate);
        assert skillTemplate.getSkillType() == SkillType.PASSIVE;
    }

    public abstract void onTigger(BattleEntity owner, BattleContext context);
}
