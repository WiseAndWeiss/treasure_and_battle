package com.example.treasure_and_battle.consumable.utility;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;

public interface IUtilityHandler {
    boolean execute(Character character, ConsumableItem item, ConsumableItem.Effect effect, Context context);
    String getUtilityId();
}
