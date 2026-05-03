package com.example.treasure_and_battle.skill.active.warrior;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

public class ActiveSkill_Slash extends ActiveSkill {
    public ActiveSkill_Slash(SkillTemplate skillTemplate) {
        super(skillTemplate);
    }

    @Override
    public void onCast(BattleEntity owner, List<BattleEntity> targets, BattleManager battleManager) {

    }

}
