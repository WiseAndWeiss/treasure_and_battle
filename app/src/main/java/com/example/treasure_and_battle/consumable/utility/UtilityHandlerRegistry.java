package com.example.treasure_and_battle.consumable.utility;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;

import java.util.HashMap;
import java.util.Map;

public final class UtilityHandlerRegistry {

    private static final Map<String, IUtilityHandler> HANDLERS = new HashMap<>();

    static {
        register(new ResetTalentsHandler());
        register(new SpawnRandomEventsHandler());
        register(new SpawnMonsterEventHandler());
        register(new SpawnTreasureEventHandler());
    }

    private UtilityHandlerRegistry() {}

    private static void register(IUtilityHandler handler) {
        HANDLERS.put(handler.getUtilityId(), handler);
    }

    public static boolean execute(String utilityId, Character character, ConsumableItem item,
                                   ConsumableItem.Effect effect, Context context) {
        IUtilityHandler handler = HANDLERS.get(utilityId);
        if (handler == null) {
            return false;
        }
        return handler.execute(character, item, effect, context);
    }
}
