package com.example.treasure_and_battle.model.affix;

public class EquipAffixTemplate {
    private int templateId;
    private String affixName;
    private String descriptionFormat;
    private int rarityId;
    private String triggerType;
    private String[] allowCategories;
    private float minValue;
    private float maxValue;
    private String affixClass;

    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }
    public String getAffixName() { return affixName; }
    public void setAffixName(String affixName) { this.affixName = affixName; }
    public String getDescriptionFormat() { return descriptionFormat; }
    public void setDescriptionFormat(String descriptionFormat) { this.descriptionFormat = descriptionFormat; }
    public int getRarityId() { return rarityId; }
    public void setRarityId(int rarityId) { this.rarityId = rarityId; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String[] getAllowCategories() { return allowCategories; }
    public void setAllowCategories(String[] allowCategories) { this.allowCategories = allowCategories; }
    public float getMinValue() { return minValue; }
    public void setMinValue(float minValue) { this.minValue = minValue; }
    public float getMaxValue() { return maxValue; }
    public void setMaxValue(float maxValue) { this.maxValue = maxValue; }
    public String getAffixClass() { return affixClass; }
    public void setAffixClass(String affixClass) { this.affixClass = affixClass; }
}