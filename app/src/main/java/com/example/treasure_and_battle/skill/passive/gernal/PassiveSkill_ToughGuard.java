package com.example.treasure_and_battle.skill.passive.gernal;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;

public class PassiveSkill_ToughGuard extends PassiveSkill {
    public PassiveSkill_ToughGuard(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onTigger(BattleEntity owner, BattleContext context) {

    }

}
