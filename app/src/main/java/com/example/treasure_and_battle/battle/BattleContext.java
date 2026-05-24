package com.example.treasure_and_battle.battle;

import com.example.treasure_and_battle.battle.damage.DamageSource;
import com.example.treasure_and_battle.battle.log.BattleLogEntry;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.battle.action.ActionIntent;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 战斗上下文类 (BattleContext)
 * 职责：纯数据容器，存储战斗过程中的所有临时状态，不包含业务逻辑
 */
public class BattleContext {

    // ====================== 偷袭方向枚举 ======================
    public enum SurpriseDirection {
        NONE,             // 正常战斗，按速度排序
        PLAYER_SURPRISE,  // 玩家方偷袭，玩家方全阵营先行动
        MONSTER_SURPRISE  // 怪物方偷袭，怪物方全阵营先行动
    }

    // ====================== 包装意图+看破+执行状态 ======================
    public static class RevealedIntent {
        public final ActionIntent intent;
        public final boolean seenThrough;
        public boolean executed;

        public RevealedIntent(ActionIntent intent, boolean seenThrough) {
            this.intent = intent;
            this.seenThrough = seenThrough;
            this.executed = false;
        }
    }

    // ====================== 战斗核心实体 ======================
    /** 玩家方阵营（含玩家+未来可能的NPC盟友），player 始终指向第一个元素 */
    public List<BattleEntity> playerParty;
    /** 便利引用：始终等于 playerParty.get(0) */
    public Player player;
    /** 怪物方阵营 */
    public List<Monster> monsters;
    /** 兼容字段：代表当前主要目标怪物 */
    @Deprecated
    public Monster monster;

    // ====================== 当前行动状态 ======================
    public BattleEntity currentActor;
    public BattleEntity currentTarget;
    public int currentActionPoints;
    public int currentRound;

    // ====================== 全局速度队列 ======================
    /** 本轮按速度降序排列的全体行动者（含玩家方+怪物方） */
    public List<BattleEntity> roundActionOrder;
    /** 当前行动者在 roundActionOrder 中的索引 */
    public int actionOrderIndex;

    // ====================== 战斗规则标记 ======================
    public SurpriseDirection surpriseAttacker;
    /** @deprecated 请使用 surpriseAttacker */
    @Deprecated
    public boolean isSurpriseAttack;
    /** @deprecated 请使用 roundActionOrder 判断当前行动方 */
    @Deprecated
    public boolean isPlayerTurn;
    public boolean isBattleEnded;
    public BattleResult battleResult;

    // ====================== 伤害计算临时数据 ======================
    public int rawDamage;
    public int finalDamage;
    public String damageType;
    public boolean isCriticalHit;
    public boolean isHit;
    public boolean isDodged;
    public DamageSource damageSource;

    // ====================== 怪物意图（回合开始统一下达，含看破+执行标记） ======================
    /** entityId → 本轮揭示的意图列表 */
    public Map<String, List<RevealedIntent>> monsterRevealedIntents;
    /** entityId → 当前正在执行的意图索引（-1=未开始） */
    public transient Map<String, Integer> monsterIntentStepIndex = new java.util.HashMap<>();
    /** 延迟执行的怪物动作（UI驱动模式下，先播动画后执行） */
    public transient com.example.treasure_and_battle.battle.action.BattleAction pendingMonsterAction;

    // ====================== 待领取的掉落物 ======================
    /** 战斗胜利后生成的掉落物列表，玩家可选择逐件拿取或全部拿取 */
    public List<Item> pendingLoot = new ArrayList<>();

    // ====================== 战斗奖励结算值 ======================
    /** 本次战斗获得的金币（settleBattleResult 后写入） */
    public int rewardGold;
    /** 本次战斗获得的经验（settleBattleResult 后写入） */
    public int rewardExp;

    // ====================== 战斗日志 ======================
    public List<BattleLogEntry> battleLogs = new ArrayList<>();

    public void addLog(LogType type, String template, Object... args) {
        battleLogs.add(new BattleLogEntry(currentRound, type, null, template, args));
    }

    public void addLogWithMeta(LogType type, Object metaData, String template, Object... args) {
        battleLogs.add(new BattleLogEntry(currentRound, type, metaData, template, args));
    }

    // ====================== 构造函数 ======================
    public BattleContext(Player player, Monster monster, boolean isSurpriseAttack) {
        this(player, monster == null ? new ArrayList<>() : new ArrayList<>(Collections.singletonList(monster)),
                isSurpriseAttack ? SurpriseDirection.PLAYER_SURPRISE : SurpriseDirection.NONE);
    }

    public BattleContext(Player player, Monster monster, SurpriseDirection surpriseAttacker) {
        this(player, monster == null ? new ArrayList<>() : new ArrayList<>(Collections.singletonList(monster)),
                surpriseAttacker);
    }

    public BattleContext(Player player, List<Monster> monsters, boolean isSurpriseAttack) {
        this(player, monsters,
                isSurpriseAttack ? SurpriseDirection.PLAYER_SURPRISE : SurpriseDirection.NONE);
    }

    public BattleContext(Player player, List<Monster> monsters, SurpriseDirection surpriseAttacker) {
        this.player = player;
        this.playerParty = new ArrayList<>();
        this.playerParty.add(player);
        this.monsters = monsters == null ? new ArrayList<>() : monsters;
        this.monster = firstNonNullMonster(this.monsters);
        this.surpriseAttacker = surpriseAttacker;
        this.isSurpriseAttack = (surpriseAttacker != SurpriseDirection.NONE);
        this.currentRound = 0;
        this.isBattleEnded = false;
        this.isPlayerTurn = true;
        this.roundActionOrder = new ArrayList<>();
        this.actionOrderIndex = 0;
        this.monsterRevealedIntents = new LinkedHashMap<>();
    }

    private static Monster firstNonNullMonster(List<Monster> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (Monster m : list) {
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    // ====================== 查询方法 ======================
    public List<Monster> getAliveMonsters() {
        List<Monster> alive = new ArrayList<>();
        if (monsters == null) return alive;
        for (Monster m : monsters) {
            if (m != null && !m.isDead() && !m.isEscaped()) {
                alive.add(m);
            }
        }
        return alive;
    }

    public List<BattleEntity> getAlivePlayerParty() {
        List<BattleEntity> alive = new ArrayList<>();
        if (playerParty == null) return alive;
        for (BattleEntity e : playerParty) {
            if (e != null && !e.isDead()) {
                alive.add(e);
            }
        }
        return alive;
    }

    public Monster getPrimaryMonsterTarget() {
        if (currentTarget instanceof Monster && !currentTarget.isDead()) {
            return (Monster) currentTarget;
        }
        List<Monster> alive = getAliveMonsters();
        return alive.isEmpty() ? null : alive.get(0);
    }

    public void resetDamageData() {
        this.rawDamage = 0;
        this.finalDamage = 0;
        this.damageType = null;
        this.damageSource = null;
        this.isCriticalHit = false;
        this.isHit = true;
        this.isDodged = false;
    }

    // ====================== 战斗结果枚举 ======================
    public enum BattleResult {
        VICTORY,
        DEFEAT,
        ESCAPED,
        MONSTER_ESCAPED
    }
}
