package com.example.treasure_and_battle.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.profession.ProfessionType;

/**
 * 在尚未接入全局存档/GameManager 前，由 UI 层持有唯一 {@link Character} 实例，
 * 供技能页等与 {@link com.example.treasure_and_battle.manager.PlayerManager}、职业技能树共用。
 */
public final class PlayerCharacterHolder {

    private static Character instance;

    private PlayerCharacterHolder() {}

    @NonNull
    public static synchronized Character getOrCreate(@NonNull Context context) {
        if (instance == null) {
            Context app = context.getApplicationContext();
            instance = new Character(1, "冒险者", ProfessionType.WARRIOR, app);
        }
        return instance;
    }

    public static synchronized void setForTesting(@NonNull Character character) {
        instance = character;
    }

    public static synchronized void clear() {
        instance = null;
    }
}
