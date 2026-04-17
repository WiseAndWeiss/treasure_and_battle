package com.example.treasure_and_battle.model;

// 词条类：附加在装备上的额外属性（依据MD设计）
public class Affix {
    private String name;        // 词条名称（如：力量增幅）
    private Rarity rarity;      // 词条稀有度（对应"x品质装备至少拥有1条不低于x品质的词条"）
    private String description; // 效果描述，如 "最大生命值 + 10%"
    
    // TODO: 后续这里可能还需要加上如 bonusTarget (修改谁属性) 和 bonusValue (提高多少数值) 的字段
    
    public Affix(String name, Rarity rarity, String description) {
        this.name = name;
        this.rarity = rarity;
        this.description = description;
    }

    public String getName() { return name; }
    public Rarity getRarity() { return rarity; }
    public String getDescription() { return description; }
}