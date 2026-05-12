package com.example.treasure_and_battle.ui.menu;

import com.example.treasure_and_battle.model.item.Item;

import java.util.List;

public interface ItemMenuProvider {
    List<ItemAction> getActions(Item item);
}
