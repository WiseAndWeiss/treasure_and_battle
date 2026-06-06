package com.example.treasure_and_battle.model.profession;

/**
 * 职业模板类
 * 用于从配置文件加载职业数据
 */
public class ProfessionTemplate {
    private int templateId;
    private String professionName;
    private ProfessionType professionType;
    private String activeSkillTreeTemplates;
    private String passiveSkillTreeTemplates;
    private String eventSkillTreeTemplates;

    // 默认构造函数（GSON需要）
    public ProfessionTemplate() {}

    public ProfessionTemplate(
            int templateId,
            String professionName,
            ProfessionType professionType,
            String activeSkillTreeTemplates,
            String passiveSkillTreeTemplates,
            String eventSkillTreeTemplates) {
        this.templateId = templateId;
        this.professionName = professionName;
        this.professionType = professionType;
        this.activeSkillTreeTemplates = activeSkillTreeTemplates;
        this.passiveSkillTreeTemplates = passiveSkillTreeTemplates;
        this.eventSkillTreeTemplates = eventSkillTreeTemplates;
    }

    // Getters
    public int getTemplateId() { return templateId; }
    public String getProfessionName() { return professionName; }
    public ProfessionType getProfessionType() { return professionType; }
    public String getActiveSkillTreeTemplates() { return activeSkillTreeTemplates; }
    public String getPassiveSkillTreeTemplates() { return passiveSkillTreeTemplates; }
    public String getEventSkillTreeTemplates() { return eventSkillTreeTemplates; }

    // Setters (GSON需要)
    public void setTemplateId(int templateId) { this.templateId = templateId; }
    public void setProfessionName(String professionName) { this.professionName = professionName; }
    public void setProfessionType(ProfessionType professionType) { this.professionType = professionType; }
    public void setActiveSkillTreeTemplates(String activeSkillTreeTemplates) { this.activeSkillTreeTemplates = activeSkillTreeTemplates; }
    public void setPassiveSkillTreeTemplates(String passiveSkillTreeTemplates) { this.passiveSkillTreeTemplates = passiveSkillTreeTemplates; }
    public void setEventSkillTreeTemplates(String eventSkillTreeTemplates) { this.eventSkillTreeTemplates = eventSkillTreeTemplates; }
}
