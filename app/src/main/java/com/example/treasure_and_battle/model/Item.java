package com.example.treasure_and_battle.model;

// 物品基类：所有物品通用，无类型限制，后续加新物品仅需加TYPE常量
public class Item {
    // ====================== 可拓展物品类型常量（后续加新类型仅需在这里加一行） ======================
    public static final int TYPE_CONSUMABLE = 0; // 消耗品（药水等）
    public static final int TYPE_MATERIAL = 1;   // 材料
    public static final int TYPE_GEM = 2;        // 宝石
    public static final int TYPE_EQUIPMENT = 3;  // 装备
    // 后续加任务物品、图纸、卷轴等，直接在这里加常量即可，核心代码不用改

    // ====================== 物品通用属性 ======================
    private int itemId;          // 物品唯一ID
    private String itemName;     // 物品名称
    private int itemType;        // 物品类型（对应上面的常量）
    private Rarity rarity;          // 稀有度（对应Rarity枚举）
    private int iconResId;       // 图标资源ID
    private String description;  // 物品描述
    private int maxStackCount;   // 最大堆叠数量
    private int currentCount;    // 当前堆叠数量

    private int baseValue;        // 基础价值（用于商店买卖价格计算，后续可根据稀有度、属性加成等调整）
    // 构造函数
    public Item(int itemId, String itemName, int itemType, Rarity rarity, int iconResId, String description, int maxStackCount, int baseValue) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemType = itemType;
        this.rarity = rarity;
        this.iconResId = iconResId;
        this.description = description;
        this.maxStackCount = maxStackCount;
        this.currentCount = 1; // 默认数量1
        this.baseValue = baseValue;
    }

    // ====================== Getter & Setter ======================
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public int getItemType() { return itemType; }
    public void setItemType(int itemType) { this.itemType = itemType; }
    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }
    public int getIconResId() { return iconResId; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getMaxStackCount() { return maxStackCount; }
    public void setMaxStackCount(int maxStackCount) { this.maxStackCount = maxStackCount; }
    public int getCurrentCount() { return currentCount; }
    public void setCurrentCount(int currentCount) { this.currentCount = currentCount; }
}