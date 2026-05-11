package com.example.treasure_and_battle.ui;

import androidx.annotation.NonNull;

import com.example.treasure_and_battle.character.Character;

/**
 * {@link Character} 当前未公开技能点消耗接口，而 {@link com.example.treasure_and_battle.skill.SkillTree}
 * 会在升级时累计内部点数。在仅修改 ui 包的前提下，升级成功后通过反射扣减 {@code skillPoints} 以保持与界面一致。
 */
public final class SkillUiBridge {

    private SkillUiBridge() {}

    /** 成功扣减 1 点返回 true；技能点不足或反射失败返回 false（此时不应已调用 levelUp）。 */
    public static boolean trySpendOneSkillPoint(@NonNull Character character) {
        try {
            java.lang.reflect.Field f = Character.class.getDeclaredField("skillPoints");
            f.setAccessible(true);
            int sp = f.getInt(character);
            if (sp <= 0) {
                return false;
            }
            f.setInt(character, sp - 1);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
