package com.example.treasure_and_battle.ui.menu;

import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.equip.EquipItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EquipmentMenuProvider implements ItemMenuProvider {

    private final Consumer<EquipItem> equipCallback;
    private final Consumer<Item> viewCallback;
    private final Consumer<Item> discardCallback;

    public EquipmentMenuProvider(Consumer<EquipItem> equipCallback,
                                 Consumer<Item> viewCallback,
                                 Consumer<Item> discardCallback) {
        this.equipCallback = equipCallback;
        this.viewCallback = viewCallback;
        this.discardCallback = discardCallback;
    }

    @Override
    public List<ItemAction> getActions(Item item) {
        List<ItemAction> actions = new ArrayList<>();
        actions.add(new ItemAction("装备", true, i -> equipCallback.accept((EquipItem) i)));
        actions.add(new ItemAction("查看", true, viewCallback));
        actions.add(new ItemAction("丢弃", true, discardCallback));
        return actions;
    }
}
