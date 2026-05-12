package com.example.treasure_and_battle.ui.menu;

import com.example.treasure_and_battle.model.item.Item;

import java.util.function.Consumer;

public class ItemAction {
    public final String name;
    public final boolean enabled;
    public final Consumer<Item> action;
    private final String disabledHint;

    public ItemAction(String name, boolean enabled, Consumer<Item> action) {
        this(name, enabled, action, null);
    }

    public ItemAction(String name, boolean enabled, Consumer<Item> action, String disabledHint) {
        this.name = name;
        this.enabled = enabled;
        this.action = action;
        this.disabledHint = disabledHint;
    }

    public String getDisplayText() {
        if (enabled || disabledHint == null || disabledHint.isEmpty()) {
            return name;
        }
        return name + disabledHint;
    }
}
