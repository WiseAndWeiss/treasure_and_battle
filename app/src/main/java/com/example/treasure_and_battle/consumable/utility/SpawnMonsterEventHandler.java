package com.example.treasure_and_battle.consumable.utility;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.event.EventManager;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;

public class SpawnMonsterEventHandler implements IUtilityHandler {

    @Override
    public boolean execute(Character character, ConsumableItem item, ConsumableItem.Effect effect, Context context) {
        return EventManager.getInstance(context).forceSpawnEvent("BATTLE");
    }

    @Override
    public String getUtilityId() {
        return "SPAWN_MONSTER_EVENT";
    }
}
