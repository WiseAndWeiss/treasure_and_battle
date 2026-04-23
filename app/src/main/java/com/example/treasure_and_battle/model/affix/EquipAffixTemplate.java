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
    private String attributeType;
    private String valueType;
    private String affixScope;
    private Integer buffTemplateId;
    private String applyTarget;
    private Integer applyStacks;
    private Float damageToStackRatio;
    private String recoverResourceType;
    private String recoverValueType;
    private Integer recoverValue;
    private Float damageToRecoverRatio;

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
    public String getAttributeType() { return attributeType; }
    public void setAttributeType(String attributeType) { this.attributeType = attributeType; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public String getAffixScope() { return affixScope; }
    public void setAffixScope(String affixScope) { this.affixScope = affixScope; }
    public Integer getBuffTemplateId() { return buffTemplateId; }
    public void setBuffTemplateId(Integer buffTemplateId) { this.buffTemplateId = buffTemplateId; }
    public String getApplyTarget() { return applyTarget; }
    public void setApplyTarget(String applyTarget) { this.applyTarget = applyTarget; }
    public Integer getApplyStacks() { return applyStacks; }
    public void setApplyStacks(Integer applyStacks) { this.applyStacks = applyStacks; }
    public Float getDamageToStackRatio() { return damageToStackRatio; }
    public void setDamageToStackRatio(Float damageToStackRatio) { this.damageToStackRatio = damageToStackRatio; }
    public String getRecoverResourceType() { return recoverResourceType; }
    public void setRecoverResourceType(String recoverResourceType) { this.recoverResourceType = recoverResourceType; }
    public String getRecoverValueType() { return recoverValueType; }
    public void setRecoverValueType(String recoverValueType) { this.recoverValueType = recoverValueType; }
    public Integer getRecoverValue() { return recoverValue; }
    public void setRecoverValue(Integer recoverValue) { this.recoverValue = recoverValue; }
    public Float getDamageToRecoverRatio() { return damageToRecoverRatio; }
    public void setDamageToRecoverRatio(Float damageToRecoverRatio) { this.damageToRecoverRatio = damageToRecoverRatio; }
}