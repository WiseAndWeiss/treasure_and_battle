package com.example.treasure_and_battle.skill;

import com.example.treasure_and_battle.manager.skill.SkillManager;
import com.example.treasure_and_battle.model.skill.SkillTreeTemplate;

import java.util.ArrayList;
import java.util.List;

public class SkillTree {
    private final int templateId;
    private final SkillTreeTemplate template;
    private final SkillManager skillManager;
    private List<Skill> learnedSkills;
    private int usedPoints;
    private int unlockedLayer;

    public SkillTree(SkillTreeTemplate template, SkillManager skillManager) {
        this.templateId = template.getTemplateId();
        this.template = template;
        this.skillManager = skillManager;
        this.learnedSkills = new ArrayList<>();
        this.usedPoints = 0;
        this.unlockedLayer = 1;
    }

    public List<String> getAllSkillIds() { return template.getAllSkillIds(); }
    public boolean hasSkill(String skillId) { return template.hasSkill(skillId); }
    public List<Skill> getAllLearnedSkills() { return learnedSkills; }
    public Skill getLearnedSkill(String skillId) { return learnedSkills.stream().filter(skill -> skill.getSkillId().equals(skillId)).findFirst().orElse(null); }
    public boolean isLearned(String skillId) { return learnedSkills.stream().anyMatch(skill -> skill.getSkillId().equals(skillId)); }

    public boolean isUnlocked(int layer) { return layer <= unlockedLayer; }
    public boolean isLearnable(String skillId) {
        if(isLearned(skillId)) return false;
        SkillTreeTemplate.SkillNode node = template.getSkillNode(skillId);
        if(node == null) return false;
        if(node.layer > unlockedLayer) return false;
        for(String prerequiredSkillId : node.prerequisiteSkillIds) {
            if(!isLearned(prerequiredSkillId)) return false;
        }
        return true;
    }
    public boolean canLevelUp(String skillId) {
        Skill skill = getLearnedSkill(skillId);
        if(skill == null) return isLearnable(skillId);
        return skill.getLevel() < skill.getMaxLevel();
    }
    public boolean levelUpSkill(String skillId) {
        if(!canLevelUp(skillId)) return false;
        Skill skill = getLearnedSkill(skillId);
        if(skill == null) {
            skill = skillManager.createSkillBySkillId(skillId, 1);
            learnedSkills.add(skill);
        }
        else skill.levelUp();
        usedPoints += 1;
        if(unlockedLayer < template.getMaxLayer() && usedPoints >= template.getLayerUnlockNeededPoints(unlockedLayer+1))
            unlockedLayer++;
        return true;
    }

    public int resetAllSkills() {
        int refund = usedPoints;
        learnedSkills.clear();
        usedPoints = 0;
        unlockedLayer = 1;
        return refund;
    }
}
