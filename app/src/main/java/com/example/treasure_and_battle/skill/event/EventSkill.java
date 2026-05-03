package com.example.treasure_and_battle.skill.event;

import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.model.skill.SkillType;
import com.example.treasure_and_battle.skill.Skill;

public class EventSkill extends Skill {
    public EventSkill(SkillTemplate skillTemplate) {
        super(skillTemplate);
        assert skillTemplate.getSkillType() == SkillType.EVENT;
    }
}
