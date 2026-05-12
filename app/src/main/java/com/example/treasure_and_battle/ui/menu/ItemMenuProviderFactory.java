package com.example.treasure_and_battle.ui.menu;

import android.content.Context;

import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.item.material.MaterialItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ItemMenuProviderFactory {

    private final Map<Class<? extends Item>, ItemMenuProvider> providers = new HashMap<>();
    private final Context context;
    private final Consumer<Item> viewCallback;
    private final Consumer<Item> discardCallback;

    public ItemMenuProviderFactory(Context context,
                                   Consumer<Item> viewCallback,
                                   Consumer<Item> discardCallback) {
        this.context = context;
        this.viewCallback = viewCallback;
        this.discardCallback = discardCallback;
    }

    public void registerEquipment(Consumer<EquipItem> equipCallback) {
        providers.put(EquipItem.class,
                new EquipmentMenuProvider(equipCallback, viewCallback, discardCallback));
    }

    public void registerConsumable(Consumer<ConsumableItem> useCallback) {
        providers.put(ConsumableItem.class,
                new ConsumableMenuProvider(useCallback, viewCallback, discardCallback));
    }

    public void registerGem() {
        providers.put(GemItem.class,
                new GemMenuProvider(viewCallback, discardCallback));
    }

    public void registerMaterial() {
        providers.put(MaterialItem.class,
                new MaterialMenuProvider(viewCallback, discardCallback));
    }

    public List<ItemAction> getActions(Item item) {
        if (item == null) return Collections.emptyList();
        ItemMenuProvider provider = providers.get(item.getClass());
        if (provider == null) return Collections.emptyList();
        return provider.getActions(item);
    }
}
