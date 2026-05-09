package com.example.treasure_and_battle.model.skill;

import android.content.Context;

import java.util.List;
import java.util.ArrayList;

public class SkillTreeTemplate {
    public class SkillNode {
        public String skillId;
        public List<String> prerequisiteSkillIds;
        public int layer;
    }

    private int templateId;
    private String skillTreeId;
    private SkillType skillTreeType;
    private List<SkillNode> skillNodes;
    private List<Integer> unlockLayerNeededPoints;
    private int maxLayer;

    public SkillTreeTemplate(
            int templateId,
            String skillTreeId,
            SkillType skillTreeType,
            List<SkillNode> skillNodes,
            List<Integer> unlockLayerNeededPoints,
            int maxLayer) {
        this.templateId = templateId;
        this.skillTreeId = skillTreeId;
        this.skillTreeType = skillTreeType;
        this.skillNodes = skillNodes;
        this.unlockLayerNeededPoints = unlockLayerNeededPoints;
        this.maxLayer = maxLayer;
    }

    public SkillTreeTemplate() {}

    public int getTemplateId() { return templateId; }
    public String getSkillTreeId() { return skillTreeId; }
    public SkillType getSkillTreeType() { return skillTreeType; }
    public int getMaxLayer() { return maxLayer; }
    public List<SkillNode> getSkillNodes() { return skillNodes; }
    public List<Integer> getUnlockLayerNeededPoints() { return unlockLayerNeededPoints; }
    public List<String> getAllSkillIds() {
        List<String> skillIds = new ArrayList<>();
        for (SkillNode skillNode : skillNodes)
            skillIds.add(skillNode.skillId);
        return skillIds;
    }
    public List<String> getSkillIdsInLayer(int layer) {
        List<String> skillIds = new ArrayList<>();
        for (SkillNode skillNode : skillNodes)
            if (skillNode.layer == layer)
                skillIds.add(skillNode.skillId);
        return skillIds;
    }
    public int getLayerUnlockNeededPoints(int layer) {
        return unlockLayerNeededPoints.get(layer);
    }
    public int getLayerOfSkill(String skillId) {
        for (SkillNode skillNode : skillNodes)
            if (skillNode.skillId.equals(skillId))
                return skillNode.layer;
        return -1;
    }
    public List<String> getPrerequisiteSkillIds(String skillId) {
        List<String> prerequisiteSkillIds = new ArrayList<>();
        for (SkillNode skillNode : skillNodes)
            if (skillNode.skillId.equals(skillId))
                prerequisiteSkillIds = skillNode.prerequisiteSkillIds;
        return prerequisiteSkillIds;
    }
    public SkillNode getSkillNode(String skillId) {
        for (SkillNode skillNode : skillNodes)
            if (skillNode.skillId.equals(skillId))
                return skillNode;
        return null;
    }
    public boolean hasSkill(String skillId) {
        for (SkillNode skillNode : skillNodes)
            if (skillNode.skillId.equals(skillId))
                return true;
        return false;
    }
}
