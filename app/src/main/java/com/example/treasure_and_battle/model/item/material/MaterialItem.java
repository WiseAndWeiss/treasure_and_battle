package com.example.treasure_and_battle.model.item.material;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;

public class MaterialItem extends Item {
    private String dropFrom;

    public MaterialItem(String id, String name, Rarity rarity, int baseValue,
                        int maxStack, String dropFrom) {
        super(id, name, rarity, baseValue, ItemType.MATERIAL, 1, maxStack);
        this.dropFrom = dropFrom;
    }

    public String getDropFrom() { return dropFrom; }
    public void setDropFrom(String dropFrom) { this.dropFrom = dropFrom; }
}
