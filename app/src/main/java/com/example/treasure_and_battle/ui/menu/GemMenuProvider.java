package com.example.treasure_and_battle.ui.menu;

import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.gem.GemItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class GemMenuProvider implements ItemMenuProvider {

    private final Consumer<GemItem> gemSocketCallback;
    private final Consumer<Item> viewCallback;
    private final Consumer<Item> discardCallback;
    private final boolean isInBattle;

    GemMenuProvider(Consumer<GemItem> gemSocketCallback,
                    Consumer<Item> viewCallback,
                    Consumer<Item> discardCallback,
                    boolean isInBattle) {
        this.gemSocketCallback = gemSocketCallback;
        this.viewCallback = viewCallback;
        this.discardCallback = discardCallback;
        this.isInBattle = isInBattle;
    }

    @Override
    public List<ItemAction> getActions(Item item) {
        List<ItemAction> actions = new ArrayList<>();
        if (isInBattle) {
            actions.add(new ItemAction("镶嵌(战斗禁用)", false, null, "(战斗中不可镶嵌)"));
        } else {
            actions.add(new ItemAction("镶嵌", true,
                    i -> gemSocketCallback.accept((GemItem) i)));
        }
        actions.add(new ItemAction("查看详情", true, viewCallback));
        actions.add(new ItemAction("丢弃", true, discardCallback));
        return actions;
    }
}
