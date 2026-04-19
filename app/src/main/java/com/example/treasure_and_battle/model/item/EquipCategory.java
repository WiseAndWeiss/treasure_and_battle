package com.example.treasure_and_battle.model.item;

/**
 * 装备的粗分类：用于词条系统池的划分
 * 根据您的要求，将其缩减为三类大池子：武器，防具，饰品
 * 不再细分重甲/轻甲等，以达到复用的最大化，仅作词条分类用。
 */
public enum EquipCategory {
    WEAPON,      // 包含剑、弓、法杖
    ARMOR,       // 包含重甲、轻甲、布甲
    ACCESSORY    // 包含戒指、项链等
}
