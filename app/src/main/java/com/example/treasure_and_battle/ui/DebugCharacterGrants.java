package com.example.treasure_and_battle.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.treasure_and_battle.character.Character;

/**
 * 设置页等处的角色调试加资源（经验 / 天赋点 / 技能点）。
 */
public final class DebugCharacterGrants {

    private DebugCharacterGrants() {}

    public static void grantExp(@NonNull Context context, int exp) {
        if (exp <= 0) {
            return;
        }
        Character character = PlayerCharacterHolder.getOrCreate(context);
        character.gainExp(exp);
        FloatMsgOverlay.showFloatMsg(context, "已获得 " + exp + " 经验");
    }

    public static void grantTalentPoints(@NonNull Context context, int amount) {
        if (amount <= 0) {
            return;
        }
        Character character = PlayerCharacterHolder.getOrCreate(context);
        character.addTalentPoints(amount);
        FloatMsgOverlay.showFloatMsg(context, "已获得 " + amount + " 天赋点");
    }

    public static void grantSkillPoints(@NonNull Context context, int amount) {
        if (amount <= 0) {
            return;
        }
        Character character = PlayerCharacterHolder.getOrCreate(context);
        character.addSkillPoints(amount);
        FloatMsgOverlay.showFloatMsg(context, "已获得 " + amount + " 技能点");
    }
}
