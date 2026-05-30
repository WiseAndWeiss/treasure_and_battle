package com.example.treasure_and_battle.ui.menu;

import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.equip.EquipItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EquipmentMenuProvider implements ItemMenuProvider {

    private final Consumer<EquipItem> equipCallback;
    private final Consumer<EquipItem> unsocketCallback;
    private final Consumer<Item> viewCallback;
    private final Consumer<Item> discardCallback;

    public EquipmentMenuProvider(Consumer<EquipItem> equipCallback,
                                 Consumer<EquipItem> unsocketCallback,
                                 Consumer<Item> viewCallback,
                                 Consumer<Item> discardCallback) {
        this.equipCallback = equipCallback;
        this.unsocketCallback = unsocketCallback;
        this.viewCallback = viewCallback;
        this.discardCallback = discardCallback;
    }

    @Override
    public List<ItemAction> getActions(Item item) {
        EquipItem eq = (EquipItem) item;
        List<ItemAction> actions = new ArrayList<>();
        actions.add(new ItemAction("装备", true, i -> equipCallback.accept((EquipItem) i)));
        actions.add(new ItemAction("查看详情", true, viewCallback));
        if (eq.getSocketedGems() != null && !eq.getSocketedGems().isEmpty() && unsocketCallback != null) {
            actions.add(new ItemAction("拆卸宝石", true, i -> unsocketCallback.accept((EquipItem) i)));
        }
        actions.add(new ItemAction("丢弃", true, discardCallback));
        return actions;
    }
}
