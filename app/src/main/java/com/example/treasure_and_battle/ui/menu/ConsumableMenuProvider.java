package com.example.treasure_and_battle.ui.menu;

import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ConsumableMenuProvider implements ItemMenuProvider {

    private final Consumer<ConsumableItem> useCallback;
    private final Consumer<Item> viewCallback;
    private final Consumer<Item> discardCallback;

    public ConsumableMenuProvider(Consumer<ConsumableItem> useCallback,
                                  Consumer<Item> viewCallback,
                                  Consumer<Item> discardCallback) {
        this.useCallback = useCallback;
        this.viewCallback = viewCallback;
        this.discardCallback = discardCallback;
    }

    @Override
    public List<ItemAction> getActions(Item item) {
        ConsumableItem c = (ConsumableItem) item;
        List<ItemAction> actions = new ArrayList<>();
        boolean canUseOutBattle = c.isUsableOutBattle();
        actions.add(new ItemAction("使用", canUseOutBattle,
                canUseOutBattle ? i -> useCallback.accept((ConsumableItem) i) : i -> {},
                canUseOutBattle ? null : "(局外禁用)"));
        actions.add(new ItemAction("查看详情", true, viewCallback));
        actions.add(new ItemAction("丢弃", true, discardCallback));
        return actions;
    }
}
