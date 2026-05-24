package com.example.treasure_and_battle.skill;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.model.skill.SkillType;
import com.example.treasure_and_battle.model.skill.SkillRangeType;

import com.example.treasure_and_battle.model.skill.SkillCostParams;
import com.example.treasure_and_battle.model.skill.SkillEffectParams;

import java.util.List;

public abstract class Skill {
    // ================ 基础信息 ================
    private final int templateId; // 技能模板ID，指向技能模板数据
    private final SkillTemplate template; // 技能模板数据

    // ================ 受等级影响的技能参数 ================
    private int level;      // 技能等级
    private SkillCostParams costParams;    // 技能消耗参数
    private SkillEffectParams effectParams; // 技能效果参数

    // ================ 构造方法 ================
    public Skill(SkillTemplate template) {
        this.template = template;
        this.templateId = template.getTemplateId();
        this.level = 0;
        this.costParams = new SkillCostParams();
        this.effectParams = new SkillEffectParams();
    }

    // ================ Setter & Getter ================
    public int getTemplateId() { return templateId; }
    public SkillTemplate getTemplate() { return template; }
    public String getSkillId() { return template.getSkillId(); }
    public String getSkillName() { return template.getSkillName(); }
    public String getSimpleDesc() { return template.getSimpleDesc(); }
    public String getDetailedDesc() { return template.getDetailedDesc(); }
    public SkillType getSkillType() { return template.getSkillType(); }
    public SkillRangeType getSkillRangeType() { return template.getSkillRangeType(); }

    /**
     * 获取主要触发类型（向后兼容）
     * @return 返回第一个触发类型，如果列表为空则返回 null
     * @deprecated 使用 hasTriggerType() 或 getSkillTriggerTypes() 代替
     */
    @Deprecated
    public TriggerType getSkillTriggerType() {
        List<TriggerType> types = template.getSkillTriggerTypes();
        return types.isEmpty() ? null : types.get(0);
    }

    /**
     * 获取所有触发类型
     * @return 触发类型列表
     */
    public List<TriggerType> getSkillTriggerTypes() {
        return template.getSkillTriggerTypes();
    }

    /**
     * 检查技能是否在指定时机触发
     * @param triggerType 要检查的触发类型
     * @return 如果该技能在这个时机触发则返回true
     */
    public boolean hasTriggerType(TriggerType triggerType) {
        return template.hasTriggerType(triggerType);
    }

    public int getLevel() { return level; }
    public int getMaxLevel() { return template.getMaxLevel(); }
    public SkillCostParams getCostParams() { return costParams; }
    public int getActionPointCost() { return costParams.actionPointCost; }
    public int getHpCost() { return costParams.hpCost; }
    public int getMpCost() { return costParams.mpCost; }
    public SkillEffectParams getEffectParams() { return effectParams; }
    public int getCooldown() { return template.getCooldown(); }

    /** 将详细描述中的 {x}/{y}/{z}/{w} 替换为当前等级的效果参数。 */
    public String getFormattedDetailedDesc() {
        String raw = getDetailedDesc();
        if (raw == null || raw.isEmpty()) {
            raw = getSimpleDesc();
        }
        if (raw == null) {
            return "";
        }
        SkillEffectParams p = effectParams;
        return raw
                .replace("{x}", String.valueOf(p.x))
                .replace("{y}", String.valueOf(p.y))
                .replace("{z}", String.valueOf(p.z))
                .replace("{w}", String.valueOf(p.w));
    }

    // =============== 升级方法 ===============
    public boolean isLearned(){
        return level > 0;
    }
    public void levelUp() {
        if (level < getMaxLevel()) {
            setLevel(level + 1);
        }
    };
    public void setLevel(int level) {
        if (level < 0 || level > getMaxLevel()) {
            throw new IllegalArgumentException("Invalid skill level");
        }
        this.level = level;
        this.costParams = template.getCostParamsWithLevel(level);
        this.effectParams = template.getEffectParamsWithLevel(level);
    };
}
