package com.example.treasure_and_battle.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.profession.ProfessionType;

public final class PlayerCharacterHolder {

    private static Character instance;

    private PlayerCharacterHolder() {}

    @androidx.annotation.Nullable
    public static synchronized Character get(@NonNull Context context) {
        return instance;
    }

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

    public static synchronized void restoreFrom(@NonNull Character character) {
        instance = character;
    }

    public static synchronized void clear() {
        instance = null;
    }
}
