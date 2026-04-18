package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.common.Rarity;

// 宝石子类：继承自物品基类（用于实现对应的孔位和属性）
public class Gem extends Item {
    // 根据MD文档设定
    public static final int GEM_RED = 1;    // 红宝石
    public static final int GEM_GREEN = 2;  // 绿宝石
    public static final int GEM_BLUE = 3;   // 蓝宝石
    public static final int GEM_YELLOW = 4; // 黄宝石
    public static final int GEM_PURPLE = 5; // 紫宝石
    public static final int GEM_PINK = 6;   // 粉宝石

    private int gemType;    // 宝石类型（红宝石等），决定镶嵌在不同位置获得的不同加成

    public Gem(int itemId, String itemName, Rarity rarity, int iconResId, String description, int baseValue, int gemType) {
        super(itemId, itemName, TYPE_GEM, rarity, iconResId, description, 99, baseValue);
        this.gemType = gemType;
    }

    public int getGemType() { return gemType; }
    public void setGemType(int gemType) { this.gemType = gemType; }
}