package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.attribute.AttributeType;

public class PlayerManager {
    private static PlayerManager instance;
    private Context context;

    private PlayerManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized PlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlayerManager(context);
        }
        return instance;
    }

    public static synchronized void releaseInstance() {
        instance = null;
    }

    // ====================== 天赋点分配（委托 Character） ======================

    public boolean allocateTalentPoint(Character character, String attributeName) {
        if (character == null || attributeName == null) return false;
        return character.allocateTalentPoint(attributeName);
    }

    public boolean allocateTalentPoint(Character character, AttributeType attributeType) {
        return allocateTalentPoint(character, attributeType.name());
    }

    public void resetAllTalentPoints(Character character) {
        if (character == null) return;
        character.resetAllTalentPoints();
    }

    public int getAllocatedStat(Character character, String attributeName) {
        if (character == null) return 0;
        switch (attributeName.toUpperCase()) {
            case "STRENGTH":    return character.getAllocatedStrength();
            case "AGILITY":     return character.getAllocatedAgility();
            case "INTELLIGENCE": return character.getAllocatedIntelligence();
            case "SPIRIT":      return character.getAllocatedSpirit();
            case "PHYSIQUE":    return character.getAllocatedPhysique();
            case "LUCK":        return character.getAllocatedLuck();
            default:            return 0;
        }
    }

    // TODO: 创建玩家角色（选择职业、分配初始属性）
    // TODO: 学习/升级技能（消耗技能点，调用 SkillManager）
    // TODO: 角色信息查询（等级、经验、属性面板、装备一览）
    // TODO: 角色名称与头像自定义
}
