package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.entity.Player;

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

    // ====================== 天赋点分配 ======================

    public boolean allocateTalentPoint(Player player, String attributeName) {
        if (player == null || attributeName == null) return false;
        boolean success = player.allocateTalentPoint(attributeName);
        return success;
    }

    public boolean allocateTalentPoint(Player player, AttributeType attributeType) {
        return allocateTalentPoint(player, attributeType.name());
    }

    public void resetAllTalentPoints(Player player) {
        if (player == null) return;
        player.resetAllTalentPoints();
    }

    public int getAllocatedStat(Player player, String attributeName) {
        if (player == null) return 0;
        switch (attributeName.toUpperCase()) {
            case "STRENGTH":    return player.getAllocatedStrength();
            case "AGILITY":     return player.getAllocatedAgility();
            case "INTELLIGENCE": return player.getAllocatedIntelligence();
            case "SPIRIT":      return player.getAllocatedSpirit();
            case "PHYSIQUE":    return player.getAllocatedPhysique();
            case "LUCK":        return player.getAllocatedLuck();
            default:            return 0;
        }
    }

    // TODO: 创建玩家角色（选择职业、分配初始属性）
    // TODO: 学习/升级技能（消耗技能点，调用 SkillManager）
    // TODO: 角色信息查询（等级、经验、属性面板、装备一览）
    // TODO: 角色名称与头像自定义
}
