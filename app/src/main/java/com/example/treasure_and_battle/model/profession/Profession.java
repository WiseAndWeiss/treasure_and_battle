package com.example.treasure_and_battle.model.profession;

import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.SkillTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Profession {
    private final String professionName;
    private final ProfessionType professionType;
    private final SkillTree activeSkillTree;
    private final SkillTree passiveSkillTree;
    private final SkillTree eventSkillTree;

    public Profession(
            String professionName,
            ProfessionType professionType,
            SkillTree activeSkillTree,
            SkillTree passiveSkillTree,
            SkillTree eventSkillTree) {
        this.professionName = professionName;
        this.professionType = professionType;
        this.activeSkillTree = activeSkillTree;
        this.passiveSkillTree = passiveSkillTree;
        this.eventSkillTree = eventSkillTree;
    }

    public String getProfessionName() {return professionName;}
    public ProfessionType getProfessionType() {return professionType;}
    public SkillTree getActiveSkillTree() {return activeSkillTree;}
    public SkillTree getPassiveSkillTree() {return passiveSkillTree;}
    public SkillTree getEventSkillTree() {return eventSkillTree;}
    public List<String> getProfessionAllActiveSkillIds() {return activeSkillTree.getAllSkillIds();}
    public List<String> getProfessionAllPassiveSkillIds() {return passiveSkillTree.getAllSkillIds();}
    public List<String> getProfessionAllEventSkillIds() {return eventSkillTree.getAllSkillIds();}
    public List<String> getProfessionAllSkillIds() {
        List<String> allSkillIds = new ArrayList<>();
        allSkillIds.addAll(getProfessionAllActiveSkillIds());
        allSkillIds.addAll(getProfessionAllPassiveSkillIds());
        allSkillIds.addAll(getProfessionAllEventSkillIds());
        return allSkillIds;
    }
    public Skill getLearnedSkillById(String skillId) {
        // 先在主动技能树中查找
        Skill skill = activeSkillTree.getAllLearnedSkills().stream()
            .filter(s -> s.getSkillId().equals(skillId))
            .findFirst()
            .orElse(null);

        if (skill != null) return skill;

        // 在被动技能树中查找
        skill = passiveSkillTree.getAllLearnedSkills().stream()
            .filter(s -> s.getSkillId().equals(skillId))
            .findFirst()
            .orElse(null);

        if (skill != null) return skill;

        // 在事件技能树中查找
        return eventSkillTree.getAllLearnedSkills().stream()
            .filter(s -> s.getSkillId().equals(skillId))
            .findFirst()
            .orElse(null);
    }
    public List<Skill> getLearnedActiveSkill() {return activeSkillTree.getAllLearnedSkills();}
    public List<Skill> getLearnedPassiveSkill() {return passiveSkillTree.getAllLearnedSkills();}
    public List<Skill> getLearnedEventSkill() {return eventSkillTree.getAllLearnedSkills();}
    public List<Skill> getLearnedSkill() {
        List<Skill> allSkill = new ArrayList<>();
        allSkill.addAll(getLearnedActiveSkill());
        allSkill.addAll(getLearnedPassiveSkill());
        allSkill.addAll(getLearnedEventSkill());
        return allSkill;
    }
    public boolean isLearnedSkill(String skillId) {
        return activeSkillTree.isLearned(skillId) || passiveSkillTree.isLearned(skillId) || eventSkillTree.isLearned(skillId);
    }
    public boolean isLearnableSkill(String skillId) {
        return activeSkillTree.isLearnable(skillId) || passiveSkillTree.isLearnable(skillId) || eventSkillTree.isLearnable(skillId);
    }
    public boolean canLevelUpSkill(String skillId) {
        return activeSkillTree.canLevelUp(skillId) || passiveSkillTree.canLevelUp(skillId) || eventSkillTree.canLevelUp(skillId);
    }
    public boolean levelUpSkill(String skillId) {
        List<SkillTree> trees = Arrays.asList(activeSkillTree, passiveSkillTree, eventSkillTree);
        for(SkillTree tree : trees) {
            if(tree.hasSkill(skillId)) {
                return tree.levelUpSkill(skillId);
            }
        }
        return false;
    }
}
