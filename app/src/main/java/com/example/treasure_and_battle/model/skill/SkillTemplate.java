package com.example.treasure_and_battle.model.skill;

import java.util.Collections;
import java.util.List;

// Skill模板类，用于桥接配置文件和反射创建技能对象
public class SkillTemplate {
    // ================ 基础属性 ================
    private int templateId;          // 模板唯一ID
    private String skillId;          // 技能唯一ID
    private String skillName;        // 技能名称
    private String simpleDesc;       // 技能简要描述
    private String detailedDesc;     // 技能详细描述
    private SkillType skillType;        // 技能类型（主动/被动/事件）
    private SkillRangeType skillRangeType; // 技能范围类型
    private SkillTriggerType skillTriggerType; // 技能触发类型
    private int maxLevel;           // 技能最大等级

    // ================ 技能效果 ================
    private int cooldown;           // 技能冷却回合数
    private List<SkillCostParams> costParamsList;        // 各等级技能消耗参数
    private List<SkillEffectParams> effectParamsList;    // 各等级技能效果参数

    private String skillClassName; // 技能实现类的全限定名，用于反射创建技能对象


    // Getter & Setter
    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }
    public String getSkillId() { return skillId; }
    public void setSkillId(String skillId) { this.skillId = skillId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getSimpleDesc() { return simpleDesc; }
    public void setSimpleDesc(String simpleDesc) { this.simpleDesc = simpleDesc; }
    public String getDetailedDesc() { return detailedDesc; }
    public void setDetailedDesc(String detailedDesc) { this.detailedDesc = detailedDesc; }
    public SkillType getSkillType() { return skillType; }
    public void setSkillType(SkillType skillType) { this.skillType = skillType; }
    public SkillRangeType getSkillRangeType() { return skillRangeType; }
    public void setSkillRangeType(SkillRangeType skillRangeType) { this.skillRangeType = skillRangeType; }
    public SkillTriggerType getSkillTriggerType() { return skillTriggerType; }
    public void setSkillTriggerType(SkillTriggerType skillTriggerType) { this.skillTriggerType = skillTriggerType; }
    public int getMaxLevel() { return maxLevel; }
    public void setMaxLevel(int maxLevel) { this.maxLevel = maxLevel; }
    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }
    public List<SkillCostParams> getCostParamsList() { return Collections.unmodifiableList(costParamsList); }
    public void setCostParamsList(List<SkillCostParams> costParamsList) { this.costParamsList = costParamsList; }
    public List<SkillEffectParams> getEffectParamsList() { return Collections.unmodifiableList(effectParamsList); }
    public void setEffectParamsList(List<SkillEffectParams> effectParamsList) { this.effectParamsList = effectParamsList; }
    public String getSkillClassName() { return skillClassName; }

    // 获取技能消耗参数
    public SkillCostParams getCostParamsWithLevel(int level) {
        if(level < 0 || level > maxLevel) {
            throw new IllegalArgumentException("Invalid skill level: " + level);
        }
        if(level == 0)  return new SkillCostParams();
        if(level > costParamsList.size())
            return getCostParamsWithLevel(costParamsList.size());
        return new SkillCostParams(costParamsList.get(level - 1));
    }

    // 获取技能效果参数
    public SkillEffectParams getEffectParamsWithLevel(int level) {
        if(level < 0 || level > maxLevel) {
            throw new IllegalArgumentException("Invalid skill level: " + level);
        }
        if(level == 0)  return new SkillEffectParams();
        return new SkillEffectParams(effectParamsList.get(level - 1));
    }
}
