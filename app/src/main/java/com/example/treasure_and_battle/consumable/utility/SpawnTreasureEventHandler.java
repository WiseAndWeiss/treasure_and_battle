package com.example.treasure_and_battle.consumable.utility;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.EventManager;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;

public class SpawnTreasureEventHandler implements IUtilityHandler {

    @Override
    public boolean execute(Character character, ConsumableItem item, ConsumableItem.Effect effect, Context context) {
        return EventManager.getInstance(context).forceSpawnEvent("BENEFIT");
    }

    @Override
    public String getUtilityId() {
        return "SPAWN_TREASURE_EVENT";
    }
}
