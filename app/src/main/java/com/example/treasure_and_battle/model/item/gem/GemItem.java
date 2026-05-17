package com.example.treasure_and_battle.model.item.gem;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;

public class GemItem extends Item {
    private String gemType;
    private AttributeSet accessoryBonus;
    private AttributeSet weaponBonus;
    private AttributeSet armorBonus;

    public GemItem(String id, String name, Rarity rarity, int baseValue, String gemType) {
        super(id, name, rarity, baseValue, ItemType.GEM, 99, 99);
        this.gemType = gemType;
        this.accessoryBonus = new AttributeSet();
        this.weaponBonus = new AttributeSet();
        this.armorBonus = new AttributeSet();
    }

    public String getGemType() { return gemType; }

    public AttributeSet getAccessoryBonus() { return accessoryBonus; }
    public AttributeSet getWeaponBonus() { return weaponBonus; }
    public AttributeSet getArmorBonus() { return armorBonus; }

    public AttributeSet getBonusForCategory(EquipCategory category) {
        switch (category) {
            case WEAPON: return weaponBonus;
            case ACCESSORY: return accessoryBonus;
            default: return armorBonus;
        }
    }
}
