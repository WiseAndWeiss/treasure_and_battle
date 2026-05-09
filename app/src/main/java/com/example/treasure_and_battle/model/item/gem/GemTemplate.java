package com.example.treasure_and_battle.model.item.gem;

import java.util.List;

public class GemTemplate {
    private String gemId;
    private String name;
    private int rarityId;
    private int baseValue;
    private String gemType;
    private List<BonusEntry> accessoryBonuses;
    private List<BonusEntry> weaponBonuses;
    private List<BonusEntry> armorBonuses;

    public static class BonusEntry {
        public String type;
        public float value;
    }

    public String getGemId() { return gemId; }
    public String getName() { return name; }
    public int getRarityId() { return rarityId; }
    public int getBaseValue() { return baseValue; }
    public String getGemType() { return gemType; }
    public List<BonusEntry> getAccessoryBonuses() { return accessoryBonuses; }
    public List<BonusEntry> getWeaponBonuses() { return weaponBonuses; }
    public List<BonusEntry> getArmorBonuses() { return armorBonuses; }
}
