package com.example.treasure_and_battle.model.item;

public class MaterialTemplate {
    private String materialId;
    private String name;
    private int rarityId;
    private int baseValue;
    private int maxStack;
    private String dropFrom;

    public String getMaterialId() { return materialId; }
    public String getName() { return name; }
    public int getRarityId() { return rarityId; }
    public int getBaseValue() { return baseValue; }
    public int getMaxStack() { return maxStack; }
    public String getDropFrom() { return dropFrom; }
}
