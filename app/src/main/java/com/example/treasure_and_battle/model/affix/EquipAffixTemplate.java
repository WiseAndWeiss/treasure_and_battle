package com.example.treasure_and_battle.model.affix;

import com.example.treasure_and_battle.model.common.TriggerType;

import java.util.List;

public class EquipAffixTemplate {
    private int templateId;
    private String affixName;
    private String descriptionFormat;
    private TriggerType triggerType;
    private String[] allowCategories;
    private String affixClass;
    private String attributeType;
    private String valueType;
    private String affixScope;
    private String applyTarget;
    private String recoverResourceType;
    private String recoverValueType;
    private List<RarityParam> rarityParams;

    public static class RarityParam {
        private int rarityId;
        private float minValue;
        private float maxValue;
        private Integer buffTemplateId;
        private Float damageToStackRatio;
        private Integer applyStacks;
        private Integer recoverValue;
        private Float damageToRecoverRatio;

        public int getRarityId() { return rarityId; }
        public void setRarityId(int rarityId) { this.rarityId = rarityId; }
        public float getMinValue() { return minValue; }
        public void setMinValue(float minValue) { this.minValue = minValue; }
        public float getMaxValue() { return maxValue; }
        public void setMaxValue(float maxValue) { this.maxValue = maxValue; }
        public Integer getBuffTemplateId() { return buffTemplateId; }
        public void setBuffTemplateId(Integer buffTemplateId) { this.buffTemplateId = buffTemplateId; }
        public Float getDamageToStackRatio() { return damageToStackRatio; }
        public void setDamageToStackRatio(Float damageToStackRatio) { this.damageToStackRatio = damageToStackRatio; }
        public Integer getApplyStacks() { return applyStacks; }
        public void setApplyStacks(Integer applyStacks) { this.applyStacks = applyStacks; }
        public Integer getRecoverValue() { return recoverValue; }
        public void setRecoverValue(Integer recoverValue) { this.recoverValue = recoverValue; }
        public Float getDamageToRecoverRatio() { return damageToRecoverRatio; }
        public void setDamageToRecoverRatio(Float damageToRecoverRatio) { this.damageToRecoverRatio = damageToRecoverRatio; }
    }

    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }
    public String getAffixName() { return affixName; }
    public void setAffixName(String affixName) { this.affixName = affixName; }
    public String getDescriptionFormat() { return descriptionFormat; }
    public void setDescriptionFormat(String descriptionFormat) { this.descriptionFormat = descriptionFormat; }
    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }
    public String[] getAllowCategories() { return allowCategories; }
    public void setAllowCategories(String[] allowCategories) { this.allowCategories = allowCategories; }
    public String getAffixClass() { return affixClass; }
    public void setAffixClass(String affixClass) { this.affixClass = affixClass; }
    public String getAttributeType() { return attributeType; }
    public void setAttributeType(String attributeType) { this.attributeType = attributeType; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public String getAffixScope() { return affixScope; }
    public void setAffixScope(String affixScope) { this.affixScope = affixScope; }
    public String getApplyTarget() { return applyTarget; }
    public void setApplyTarget(String applyTarget) { this.applyTarget = applyTarget; }
    public String getRecoverResourceType() { return recoverResourceType; }
    public void setRecoverResourceType(String recoverResourceType) { this.recoverResourceType = recoverResourceType; }
    public String getRecoverValueType() { return recoverValueType; }
    public void setRecoverValueType(String recoverValueType) { this.recoverValueType = recoverValueType; }
    public List<RarityParam> getRarityParams() { return rarityParams; }
    public void setRarityParams(List<RarityParam> rarityParams) { this.rarityParams = rarityParams; }

    public RarityParam getRarityParam(int rarityOrdinal) {
        if (rarityParams == null) return null;
        for (RarityParam p : rarityParams) {
            if (p.getRarityId() == rarityOrdinal) return p;
        }
        return null;
    }

    public boolean hasRarityParamUpTo(int maxRarityOrdinal) {
        if (rarityParams == null) return false;
        for (RarityParam p : rarityParams) {
            if (p.getRarityId() <= maxRarityOrdinal) return true;
        }
        return false;
    }
}
