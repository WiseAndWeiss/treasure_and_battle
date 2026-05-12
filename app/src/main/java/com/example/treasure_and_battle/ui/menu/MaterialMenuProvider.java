package com.example.treasure_and_battle.ui.menu;

import com.example.treasure_and_battle.model.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class MaterialMenuProvider implements ItemMenuProvider {

    private final Consumer<Item> viewCallback;
    private final Consumer<Item> discardCallback;

    MaterialMenuProvider(Consumer<Item> viewCallback, Consumer<Item> discardCallback) {
        this.viewCallback = viewCallback;
        this.discardCallback = discardCallback;
    }

    @Override
    public List<ItemAction> getActions(Item item) {
        List<ItemAction> actions = new ArrayList<>();
        actions.add(new ItemAction("查看", true, viewCallback));
        actions.add(new ItemAction("丢弃", true, discardCallback));
        return actions;
    }
}
