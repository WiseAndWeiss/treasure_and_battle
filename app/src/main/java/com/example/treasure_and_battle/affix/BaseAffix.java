package com.example.treasure_and_battle.affix;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.BattleEntity;

// 词缀抽象基类：所有具体词缀都必须继承这个类
public abstract class BaseAffix {
    // ====================== 通用基础属性 ======================
    protected final int affixId;          // 词缀唯一ID
    protected final String affixName;     // 词缀名称
    protected final String description;    // 词缀描述（用于UI显示）
    protected final Rarity rarity;        // 词缀稀有度（决定数值上限）
    protected final AffixTriggerType triggerType; // 触发时机
    protected final int[] allowSlots;     // 允许出现的装备部位（空数组=全部位可用）

    // ====================== 词缀实例数值 ======================
    protected float affixValue;           // 词缀当前生效的数值（比如+2力量、10%概率）

    // ====================== 构造函数 ======================
    public BaseAffix(int affixId, String affixName, String description, Rarity rarity,
                     AffixTriggerType triggerType, int[] allowSlots, float affixValue) {
        this.affixId = affixId;
        this.affixName = affixName;
        this.description = description;
        this.rarity = rarity;
        this.triggerType = triggerType;
        this.allowSlots = allowSlots;
        this.affixValue = affixValue;
    }

    // ====================== 核心抽象方法：所有词缀必须实现的执行逻辑 ======================
    /**
     * 词缀生效的核心方法
     * @param context 战斗上下文（包含当前攻击者、目标、伤害数值、战斗状态等所有信息）
     */
    // TODO: 完成BattleContext类的设计，包含必要的战斗信息以供词缀逻辑使用
    public abstract void onTrigger(BattleEntity owner, BattleContext context);

    /**
     * 常驻属性词缀专用：给属性集添加加成
     * @param attributeSet 玩家当前的属性集
     */
    public abstract void applyAttributeBonus(AttributeSet attributeSet);

    // ====================== 通用Getter方法（只读，不允许修改词缀基础属性） ======================
    public int getAffixId() { return affixId; }
    public String getAffixName() { return affixName; }
    public String getDescription() { return String.format(description, affixValue); } // 动态替换数值
    public Rarity getRarity() { return rarity; }
    public AffixTriggerType getTriggerType() { return triggerType; }
    public int[] getAllowSlots() { return allowSlots; }
    public float getAffixValue() { return affixValue; }
}