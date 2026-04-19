package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.common.Rarity;

public abstract class Item {
    protected String id;
    protected String name;
    protected Rarity rarity;
    protected int baseValue;
    protected ItemType type;
    protected int count;
    protected int maxStack;
    protected int iconResId = android.R.drawable.ic_menu_gallery;
    protected String description = "";

    public Item(String id, String name, Rarity rarity, int baseValue, ItemType type, int count, int maxStack) {
        this.id = id;
        this.name = name;
        this.rarity = rarity;
        this.baseValue = baseValue;
        this.type = type;
        this.count = count;
        this.maxStack = maxStack;
    }

    public int getIconResId() { return iconResId; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getId() { return id; }
    public String getName() { return name; }
    public Rarity getRarity() { return rarity; }
    public int getBaseValue() { return baseValue; }
    public ItemType getType() { return type; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public int getMaxStack() { return maxStack; }
    
    public boolean canStack() { return maxStack > 1; }
}
