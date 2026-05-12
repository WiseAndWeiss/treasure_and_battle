package com.example.treasure_and_battle.ui.menu;

import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EquipmentMenuProvider implements ItemMenuProvider {

    private final Consumer<EquipItem> equipCallback;
    private final Predicate<EquipSlot> isSlotEquipped;
    private final Consumer<Item> viewCallback;
    private final Consumer<Item> discardCallback;

    public EquipmentMenuProvider(Consumer<EquipItem> equipCallback,
                                 Predicate<EquipSlot> isSlotEquipped,
                                 Consumer<Item> viewCallback,
                                 Consumer<Item> discardCallback) {
        this.equipCallback = equipCallback;
        this.isSlotEquipped = isSlotEquipped;
        this.viewCallback = viewCallback;
        this.discardCallback = discardCallback;
    }

    @Override
    public List<ItemAction> getActions(Item item) {
        EquipItem eq = (EquipItem) item;
        List<ItemAction> actions = new ArrayList<>();
        boolean equipped = isSlotEquipped.test(eq.getSlot());
        actions.add(new ItemAction("装备", !equipped, i -> equipCallback.accept((EquipItem) i)));
        actions.add(new ItemAction("查看", true, viewCallback));
        actions.add(new ItemAction("丢弃", true, discardCallback));
        return actions;
    }
}
