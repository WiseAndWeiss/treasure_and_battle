package com.example.treasure_and_battle.model.item;

public enum EquipSlot {
    WEAPON(EquipCategory.WEAPON),     // 武器
    HELMET(EquipCategory.ARMOR),      // 头盔
    CHEST(EquipCategory.ARMOR),       // 胸甲
    LEGGINGS(EquipCategory.ARMOR),    // 护腿
    BOOTS(EquipCategory.ARMOR),       // 鞋子
    NECKLACE(EquipCategory.ACCESSORY),// 项链
    RING(EquipCategory.ACCESSORY),    // 戒指
    BRACELET(EquipCategory.ACCESSORY);// 手镯

    private final EquipCategory category;

    EquipSlot(EquipCategory category) {
        this.category = category;
    }

    public EquipCategory getCategory() {
        return category;
    }
}
