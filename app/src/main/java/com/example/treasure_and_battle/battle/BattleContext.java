package com.example.treasure_and_battle.battle;

import com.example.treasure_and_battle.battle.log.BattleLogEntry;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.MonsterIntent;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗上下文类 (BattleContext)
 * 职责：纯数据容器，存储战斗过程中的所有临时状态，不包含业务逻辑
 * 类似于一个「全局变量袋」，在 Buff、词缀、伤害计算等系统间传递数据
 */
public class BattleContext {
    // ====================== 战斗核心实体 ======================
    public Player player;
    public Monster monster;

    // ====================== 当前行动状态 ======================
    public BattleEntity currentActor;      // 当前行动者
    public BattleEntity currentTarget;     // 当前目标
    public int currentActionPoints;        // 当前行动者剩余行动点
    public int currentRound;               // 当前回合数

    // ====================== 战斗规则标记 ======================
    public boolean isSurpriseAttack;       // 是否是偷袭/突袭战斗
    public boolean isPlayerTurn;           // 是否是玩家的回合
    public boolean isBattleEnded;          // 战斗是否结束
    public BattleResult battleResult;      // 战斗结果（胜利/失败/逃跑）

    // ====================== 伤害计算临时数据 ======================
    public int rawDamage;                  // 原始伤害（未计算防御、暴击）
    public int finalDamage;                // 最终伤害
    public boolean isCriticalHit;          // 是否暴击
    public boolean isHit;                  // 是否命中
    public boolean isDodged;               // 是否闪避

    // ====================== 怪物意图相关 ======================
    public List<MonsterIntent> currentMonsterIntents; // 怪物本轮意图列表
    public List<Boolean> intentVisibility;            // 意图可见性列表（true=看破，false=问号）

    // ====================== 战斗日志相关 ======================
    public List<BattleLogEntry> battleLogs = new ArrayList<>();
    
    /**
     * 快捷添加一条日志记录（无元数据）
     * TODO: 如果需要为具体某一Buff、词缀添加更详细数值输出，也可以在各自的 onTrigger 方法回调此接口
     */
    public void addLog(LogType type, String template, Object... args) {
        battleLogs.add(new BattleLogEntry(currentRound, type, null, template, args));
    }

    /**
     * 添加包含元数据的日志（通常metaData用于前端展示的高亮关联对象，如怪物引用）
     */
    public void addLogWithMeta(LogType type, Object metaData, String template, Object... args) {
        battleLogs.add(new BattleLogEntry(currentRound, type, metaData, template, args));
    }

    // ====================== 构造函数 ======================
    public BattleContext(Player player, Monster monster, boolean isSurpriseAttack) {
        this.player = player;
        this.monster = monster;
        this.isSurpriseAttack = isSurpriseAttack;
        this.currentRound = 0;
        this.isBattleEnded = false;
        this.isPlayerTurn = true;
    }

    // ====================== 辅助方法（仅用于数据重置，无业务逻辑） ======================
    public void resetDamageData() {
        this.rawDamage = 0;
        this.finalDamage = 0;
        this.isCriticalHit = false;
        this.isHit = true;
        this.isDodged = false;
    }

    // ====================== 战斗结果枚举 ======================
    public enum BattleResult {
        VICTORY,  // 胜利
        DEFEAT,   // 失败
        ESCAPED,   // 逃跑成功
        MONSTER_ESCAPER // 怪物逃跑成功
    }
}