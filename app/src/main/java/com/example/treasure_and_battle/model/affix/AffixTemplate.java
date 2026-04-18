package com.example.treasure_and_battle.model.affix;

public class AffixTemplate {
    private int templateId;          // 模板ID
    private String affixName;        // 词缀名称
    private String descriptionFormat; // 描述格式（比如"力量+%d"）
    private int rarityId;            // 词缀稀有度ID
    private String triggerType;      // 触发时机字符串（对应枚举）
    private int[] allowSlots;        // 允许的装备部位
    private float minValue;          // 数值最小值
    private float maxValue;          // 数值最大值
    private String affixClass;       // 对应的词缀实现类全类名（反射用）
    private String affixType; // "EQUIPMENT" 或 "MONSTER"

    // Getter & Setter
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
    public int[] getAllowSlots() { return allowSlots; }
    public void setAllowSlots(int[] allowSlots) { this.allowSlots = allowSlots; }
    public float getMinValue() { return minValue; }
    public void setMinValue(float minValue) { this.minValue = minValue; }
    public float getMaxValue() { return maxValue; }
    public void setMaxValue(float maxValue) { this.maxValue = maxValue; }
    public String getAffixClass() { return affixClass; }
    public void setAffixClass(String affixClass) { this.affixClass = affixClass; }
    public String getAffixType() { return affixType; }
    public void setAffixType(String affixType) { this.affixType = affixType; }
}
