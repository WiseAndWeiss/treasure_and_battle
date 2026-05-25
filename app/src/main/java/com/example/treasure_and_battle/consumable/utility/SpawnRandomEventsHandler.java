package com.example.treasure_and_battle.consumable.utility;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;

public class SpawnRandomEventsHandler implements IUtilityHandler {

    @Override
    public boolean execute(Character character, ConsumableItem item, ConsumableItem.Effect effect, Context context) {
        // TODO: 实现生成随机事件的逻辑
        return true;
    }

    @Override
    public String getUtilityId() {
        return "SPAWN_RANDOM_EVENTS";
    }
}
