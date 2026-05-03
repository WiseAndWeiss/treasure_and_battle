package com.example.treasure_and_battle.skill.active.ranger;

import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

public class ActiveSkill_Shot extends ActiveSkill {
    public ActiveSkill_Shot(SkillTemplate template) {
        super(template);
    }

    @Override
    public void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager) {

    }

}
