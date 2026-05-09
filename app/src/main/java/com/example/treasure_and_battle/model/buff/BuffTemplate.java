package com.example.treasure_and_battle.model.buff;

import com.example.treasure_and_battle.model.common.TriggerType;

// Buff模板类，对应AffixTemplate，用于JSON配置化管理
public class BuffTemplate {
    private int templateId;          // 模板唯一ID
    private String buffId;           // Buff唯一标识
    private String buffName;         // Buff名称
    private String descriptionFormat;// 描述格式（用于UI显示）
    private String buffType;         // Buff类型（对应BuffType枚举）
    private TriggerType triggerType;      // 触发时机
    private boolean isDispellable;   // 是否可驱散
    private int defaultDuration;     // 默认持续回合数
    private int maxStackCount;       // 最大堆叠层数
    private boolean refreshOnApply;  // 重新应用时是否刷新持续时间
    private float minValue;          // 效果数值最小值
    private float maxValue;          // 效果数值最大值
    private String buffClass;        // 对应的Buff实现类全类名（反射用）
    private String attributeType;    // 通用属性Buff的目标属性类型（对应AttributeType）
    private String valueType;        // 通用属性Buff的数值类型（对应ValueType）

    // Getter & Setter
    public int getTemplateId() { return templateId; }
    public void setTemplateId(int templateId) { this.templateId = templateId; }
    public String getBuffId() { return buffId; }
    public void setBuffId(String buffId) { this.buffId = buffId; }
    public String getBuffName() { return buffName; }
    public void setBuffName(String buffName) { this.buffName = buffName; }
    public String getDescriptionFormat() { return descriptionFormat; }
    public void setDescriptionFormat(String descriptionFormat) { this.descriptionFormat = descriptionFormat; }
    public String getBuffType() { return buffType; }
    public void setBuffType(String buffType) { this.buffType = buffType; }
    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }
    public boolean isDispellable() { return isDispellable; }
    public void setDispellable(boolean dispellable) { isDispellable = dispellable; }
    public int getDefaultDuration() { return defaultDuration; }
    public void setDefaultDuration(int defaultDuration) { this.defaultDuration = defaultDuration; }
    public int getMaxStackCount() { return maxStackCount; }
    public void setMaxStackCount(int maxStackCount) { this.maxStackCount = maxStackCount; }
    public boolean isRefreshOnApply() { return refreshOnApply; }
    public void setRefreshOnApply(boolean refreshOnApply) { this.refreshOnApply = refreshOnApply; }
    public float getMinValue() { return minValue; }
    public void setMinValue(float minValue) { this.minValue = minValue; }
    public float getMaxValue() { return maxValue; }
    public void setMaxValue(float maxValue) { this.maxValue = maxValue; }
    public String getBuffClass() { return buffClass; }
    public void setBuffClass(String buffClass) { this.buffClass = buffClass; }
    public String getAttributeType() { return attributeType; }
    public void setAttributeType(String attributeType) { this.attributeType = attributeType; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
}