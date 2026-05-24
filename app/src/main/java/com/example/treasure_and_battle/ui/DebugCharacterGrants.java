package com.example.treasure_and_battle.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.profession.Profession;

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

    /**
     * 设置测试角色属性
     * 血量1000/1000，物理防御50，魔法防御50，所有技能升级到1级
     */
    public static void setupTestCharacter(@NonNull Context context) {
        Character character = PlayerCharacterHolder.getOrCreate(context);

        // 设置血量为1000
        character.setCurrentHp(1000);
        character.setBaseMaxHp(1000);

        // 设置防御力为50
        character.setBasePhysicalDef(50);
        character.setBaseMagicalDef(50);

        // 将所有技能升级到1级
        Profession profession = character.getProfession();
        upgradeAllSkillsToLevel1(profession);

        FloatMsgOverlay.showFloatMsg(context, "✅ 测试角色已设置\nHP:1000/1000 防御:50/50 技能:1级");
    }

    /**
     * 将所有技能升级到1级
     */
    private static void upgradeAllSkillsToLevel1(Profession profession) {
        // 升级主动技能到1级
        for (String skillId : profession.getProfessionAllActiveSkillIds()) {
            if (!profession.getActiveSkillTree().isLearned(skillId)) {
                // 如果未学习，调用levelUpSkill会自动学习并升级到1级
                profession.getActiveSkillTree().levelUpSkill(skillId);
            }
        }

        // 升级被动技能到1级
        for (String skillId : profession.getProfessionAllPassiveSkillIds()) {
            if (!profession.getPassiveSkillTree().isLearned(skillId)) {
                profession.getPassiveSkillTree().levelUpSkill(skillId);
            }
        }

        // 升级事件技能到1级
        for (String skillId : profession.getProfessionAllEventSkillIds()) {
            if (!profession.getEventSkillTree().isLearned(skillId)) {
                profession.getEventSkillTree().levelUpSkill(skillId);
            }
        }
    }
}
