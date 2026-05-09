package com.example.treasure_and_battle.model.item.consumable;

import java.util.List;

public class ConsumableTemplate {
    private String consumableId;
    private String name;
    private int rarityId;
    private int baseValue;
    private int maxStack;
    private boolean usableInBattle;
    private boolean usableOutBattle;
    private List<ConsumableItem.Effect> effects;
    private String description;

    public String getConsumableId() { return consumableId; }
    public String getName() { return name; }
    public int getRarityId() { return rarityId; }
    public int getBaseValue() { return baseValue; }
    public int getMaxStack() { return maxStack; }
    public boolean isUsableInBattle() { return usableInBattle; }
    public boolean isUsableOutBattle() { return usableOutBattle; }
    public List<ConsumableItem.Effect> getEffects() { return effects; }
    public String getDescription() { return description; }
}
