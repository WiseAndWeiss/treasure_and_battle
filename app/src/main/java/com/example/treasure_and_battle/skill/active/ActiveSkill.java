package com.example.treasure_and_battle.skill.active;

import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.model.skill.SkillType;
import com.example.treasure_and_battle.skill.Skill;

import java.util.List;


public abstract class ActiveSkill extends Skill {
    private int currentCooldown; // 当前冷却时间
    
    // ========== 构造函数 ========== //
    public ActiveSkill(SkillTemplate template) {
        super(template);
        assert template.getSkillType() == SkillType.ACTIVE;
        currentCooldown = 0;
    }

    // ========== Getter & Setter ========== //
    public int getCurrentCooldown() { return currentCooldown; }
    public void clearCooldown() { currentCooldown = 0; }
    public void resetCooldown() { currentCooldown = getCooldown(); }
    public void decreaseCooldown() { currentCooldown = Math.max(0, currentCooldown - 1);}
    public boolean isCooldownReady() { return currentCooldown <= 0; }

    // ========== 施法方法 ========== //
    public boolean canCast(BattleEntity caster){
        if(!isCooldownReady()) return false;
        if(caster.getCurrentActionPoints() < getActionPointCost()) return false;
        if(caster.getCurrentMp() < getMpCost()) return false;
        if(caster.getCurrentHp() < getHpCost() + 1) return false;
        return true;
    }
    public void applyCastCost(BattleEntity caster){
        caster.setCurrentActionPoints(caster.getCurrentActionPoints() - getActionPointCost());
        caster.setCurrentMp(caster.getCurrentMp() - getMpCost());
        caster.setCurrentHp(caster.getCurrentHp() - getHpCost());
        resetCooldown();
    }
    public abstract void onCast(BattleEntity caster, List<BattleEntity> targets, BattleManager battleManager);
}
