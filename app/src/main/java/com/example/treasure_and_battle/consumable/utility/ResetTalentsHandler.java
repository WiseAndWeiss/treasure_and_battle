package com.example.treasure_and_battle.consumable.utility;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;

public class ResetTalentsHandler implements IUtilityHandler {

    @Override
    public boolean execute(Character character, ConsumableItem item, ConsumableItem.Effect effect, Context context) {
        character.resetAllTalentPoints();
        character.resetAllSkills();
        return true;
    }

    @Override
    public String getUtilityId() {
        return "RESET_TALENTS";
    }
}
