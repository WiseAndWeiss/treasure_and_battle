package com.example.treasure_and_battle.skill.monster;

import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.active.ActiveSkill;

import java.util.List;

/**
 * 怪物主动技能基类
 * 与玩家的 ActiveSkill 共享同一套反射创建机制（SkillManager/SkillTemplate），
 * 但在行为上有差异：怪物技能的 canCast/applyCastCost 由 BattleManager 统一管理，
 * 子类只需实现 onCast 即可。
 */
public abstract class MonsterActiveSkill extends ActiveSkill {

    public MonsterActiveSkill(SkillTemplate template) {
        super(template);
    }

    @Override
    public abstract void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager);
}
