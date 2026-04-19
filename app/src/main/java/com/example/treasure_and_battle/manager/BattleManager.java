package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.BattleLogEntry;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.manager.AffixManager;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.entity.MonsterIntent;
import com.example.treasure_and_battle.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗管理器 (BattleManager)
 * 职责：战斗流程的唯一驱动者，封装所有战斗规则
 * 设计模式：状态机 + 流水线
 */
public class BattleManager {
    private static BattleManager instance;
    private Context context;

    private BattleManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized BattleManager getInstance(Context context) {
        if (instance == null) {
            instance = new BattleManager(context);
        }
        return instance;
    }

    // ====================== 【入口】1. 初始化战斗 ======================
    public BattleContext startBattle(Player player, Monster monster, boolean isSurpriseAttack) {
        BattleContext battleContext = new BattleContext(player, monster, isSurpriseAttack);

        // 1.1 初始化战斗实体状态
        player.setDead(false);
        monster.setDead(false);
        player.resetActionPoints();
        monster.resetActionPoints();

        // 1.2 触发战斗开始Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(player, battleContext, BuffTriggerType.ON_BATTLE_START);
        BuffManager.getInstance(context).triggerBuffs(monster, battleContext, BuffTriggerType.ON_BATTLE_START);
        AffixManager.getInstance(context).triggerAffixes(player, battleContext, AffixTriggerType.ON_BATTLE_START);
        AffixManager.getInstance(context).triggerAffixes(monster, battleContext, AffixTriggerType.ON_BATTLE_START);

        // 1.3 判定先手（功能清单第5点）
        determineTurnOrder(battleContext);

        battleContext.addLog(LogType.INIT, "战斗开始：[%s] VS [%s]", 
                player.getName(), monster.getName());

        // 1.4 进入战斗循环
        battleLoop(battleContext);

        return battleContext;
    }

    // ====================== 2. 战斗主循环 ======================
    private void battleLoop(BattleContext ctx) {
        while (!ctx.isBattleEnded) {
            ctx.currentRound++;
            ctx.resetDamageData();

            // 防卡死安全机制：回合数过大时强行平局结束
            if (ctx.currentRound >= 100) {
                ctx.isBattleEnded = true;
                ctx.battleResult = BattleContext.BattleResult.DEFEAT;
                ctx.addLog(LogType.SYSTEM, "【系统】达到回合数上限(100)，战斗强制判定为失败。");
                break;
            }

            ctx.addLog(LogType.ROUND_INFO, "======== 第 %d 回合开始 (行动方: %s) ========", 
                    ctx.currentRound, ctx.isPlayerTurn ? "玩家" : "怪物");

            // 2.1 回合开始阶段
            onRoundStart(ctx);
            if (ctx.isBattleEnded) break;

            // 2.2 行动阶段（功能清单第1、3、4点）
            if (ctx.isPlayerTurn) {
                playerActionPhase(ctx);
            } else {
                monsterActionPhase(ctx);
            }
            if (ctx.isBattleEnded) break;

            // 2.3 回合结束阶段
            onRoundEnd(ctx);
            if (ctx.isBattleEnded) break;

            // 2.4 切换回合
            ctx.isPlayerTurn = !ctx.isPlayerTurn;
        }

        // 2.5 战斗结束结算
        settleBattleResult(ctx);
    }

    // ====================== 3. 回合开始阶段 ======================
    private void onRoundStart(BattleContext ctx) {
        // 3.1 重置行动点
        if (ctx.isPlayerTurn) {
            ctx.player.resetActionPoints();
            ctx.currentActor = ctx.player;
            ctx.currentTarget = ctx.monster;
        } else {
            ctx.monster.resetActionPoints();
            ctx.currentActor = ctx.monster;
            ctx.currentTarget = ctx.player;
        }
        ctx.currentActionPoints = ctx.currentActor.getCurrentActionPoints();

        // 3.2 怪物回合：生成并显示意图（功能清单第4点）
        if (!ctx.isPlayerTurn) {
            generateAndRevealMonsterIntents(ctx);
        }

        // 3.3 触发回合开始Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(ctx.currentActor, ctx, BuffTriggerType.ON_ROUND_START);
        AffixManager.getInstance(context).triggerAffixes(ctx.currentActor, ctx, AffixTriggerType.ON_ROUND_START);
    }

    // ====================== 4. 玩家行动阶段 ======================
    private void playerActionPhase(BattleContext ctx) {
        // 这里是一个循环，直到玩家行动点耗尽或选择结束回合
        // 实际项目中，这里通过UI回调玩家的选择
        // 这里仅展示核心逻辑框架：
        // TODO: 连接UI输入，处理玩家选择的操作（普攻/技能/道具/逃跑等），并调用相应的执行方法

        while (ctx.currentActionPoints > 0 && !ctx.isBattleEnded) {
            // 假设玩家选择了某个操作（普攻/技能/道具/逃跑）
            // 这里仅以「普攻」和「逃跑」为例展示逻辑

            // 示例：玩家选择普攻
            // executePlayerNormalAttack(ctx);
            // ctx.currentActionPoints--;

            // 示例：玩家选择逃跑
            // if (executePlayerEscape(ctx)) {
            //     return;
            // }
        }
    }

    // ====================== 5. 怪物行动阶段 ======================
    private void monsterActionPhase(BattleContext ctx) {
        // 按顺序执行怪物意图（功能清单第4点）
        if (ctx.currentMonsterIntents == null || ctx.currentMonsterIntents.isEmpty()) {
            return;
        }

        for (MonsterIntent intent : ctx.currentMonsterIntents) {
            if (ctx.isBattleEnded) break;

            // 检查行动点和MP是否足够
            if (ctx.monster.getCurrentActionPoints() < intent.getApCost() ||
                    ctx.monster.getCurrentMp() < intent.getMpCost()) {
                continue;
            }

            // 执行意图
            executeMonsterIntent(ctx, intent);
            ctx.monster.consumeActionPoints(intent.getApCost());
            ctx.monster.setCurrentMp(ctx.monster.getCurrentMp() - intent.getMpCost());
        }
    }

    // ====================== 6. 回合结束阶段 ======================
    private void onRoundEnd(BattleContext ctx) {
        // 6.1 触发回合结束Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(ctx.player, ctx, BuffTriggerType.ON_ROUND_END);
        BuffManager.getInstance(context).triggerBuffs(ctx.monster, ctx, BuffTriggerType.ON_ROUND_END);
        AffixManager.getInstance(context).triggerAffixes(ctx.player, ctx, AffixTriggerType.ON_ROUND_END);
        AffixManager.getInstance(context).triggerAffixes(ctx.monster, ctx, AffixTriggerType.ON_ROUND_END);

        // 6.2 Buff Tick（减少持续时间，清理过期）
        BuffManager.getInstance(context).tickBuffs(ctx.player);
        BuffManager.getInstance(context).tickBuffs(ctx.monster);

        // 6.3 检查是否有实体死亡
        checkDeath(ctx);
    }

    // ====================== 【核心规则实现】功能清单具体逻辑 ======================

    // 5. 先手规则判定
    private void determineTurnOrder(BattleContext ctx) {
        if (ctx.isSurpriseAttack) {
            // 偷袭战斗：袭击方先行动（这里假设袭击方是玩家，可根据需求调整）
            ctx.isPlayerTurn = true;
            ctx.addLog(LogType.INIT, "【偷袭】[%s] 发起突袭，获得先手行动权。", ctx.player.getName());
            return;
        }

        // 常规战斗：速度高的先行动
        AttributeSet playerAttr = ctx.player.getFinalAttributes();
        AttributeSet monsterAttr = ctx.monster.getFinalAttributes();
        ctx.isPlayerTurn = playerAttr.speed >= monsterAttr.speed;

        ctx.addLog(LogType.INIT, "【先手判定】玩家速度(%.0f) vs 怪物速度(%.0f) => [%s] 行动。", 
            playerAttr.speed, monsterAttr.speed, ctx.isPlayerTurn ? "玩家" : "怪物");
    }

    // 4. 怪物意图生成与看破
    private void generateAndRevealMonsterIntents(BattleContext ctx) {
        // 4.1 怪物决策意图
        ctx.currentMonsterIntents = ctx.monster.decideNextTurnIntents();
        ctx.intentVisibility = new ArrayList<>();

        // 4.2 计算看破概率（功能清单第4点）
        AttributeSet playerAttr = ctx.player.getFinalAttributes();
        AttributeSet monsterAttr = ctx.monster.getFinalAttributes();

        double seeThroughChance = 0.5 * ((double) playerAttr.spirit / monsterAttr.spirit);
        // 截断在10%-90%之间
        seeThroughChance = Math.max(0.1, Math.min(0.9, seeThroughChance));

        ctx.addLog(LogType.DODGE_CRIT, "【意图看破判定】玩家精神(%.0f) vs 怪物精神(%.0f) => 看破率: %.1f%%", 
                playerAttr.spirit, monsterAttr.spirit, seeThroughChance * 100);

        // 4.3 判定每个意图是否可见
        for (int i = 0; i < ctx.currentMonsterIntents.size(); i++) {
            boolean isVisible = RandomUtils.checkProbability((float) seeThroughChance);
            ctx.intentVisibility.add(isVisible);
            
            MonsterIntent intent = ctx.currentMonsterIntents.get(i);
            if (isVisible) {
                ctx.addLog(LogType.ACTION, "  看破怪物意图：[%s]", intent.getType().name());
            } else {
                ctx.addLog(LogType.ACTION, "  怪物意图未知。");
            }
        }
    }

    // 8. 玩家逃跑逻辑
    public boolean executePlayerEscape(BattleContext ctx) {
        ctx.addLog(LogType.ACTION, "玩家尝试逃跑...");

        // 8.1 消耗1点行动点
        if (!ctx.player.consumeActionPoints(1)) {
            ctx.addLog(LogType.SYSTEM, "行动点不足，逃跑失败。");
            return false;
        }
        ctx.currentActionPoints--;

        // 8.2 计算逃跑成功率（功能清单第8点）
        AttributeSet playerAttr = ctx.player.getFinalAttributes();
        AttributeSet monsterAttr = ctx.monster.getFinalAttributes();

        double escapeChance = 0.2 + ((double) playerAttr.speed / monsterAttr.speed - 1) * 0.5;
        escapeChance = Math.max(0.1, Math.min(0.9, escapeChance));
        
        ctx.addLog(LogType.DODGE_CRIT, "【逃跑判定】玩家速度(%.0f) vs 怪物速度(%.0f) => 成功率: %.1f%%", 
                playerAttr.speed, monsterAttr.speed, escapeChance * 100);

        // 8.3 判定是否成功
        if (RandomUtils.checkProbability((float) escapeChance)) {
            // 逃跑成功
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.ESCAPED;
            ctx.addLog(LogType.ACTION, "逃跑成功。");
            return true;
        } else {
            // 逃跑失败：怪物立即执行一次攻击
            ctx.addLog(LogType.ACTION, "逃跑失败，遭到怪物追击。");
            executeMonsterNormalAttack(ctx);
            return false;
        }
    }

    // 3. 玩家普攻逻辑
    public void executePlayerNormalAttack(BattleContext ctx) {
        ctx.resetDamageData();
        ctx.currentActor = ctx.player;
        ctx.currentTarget = ctx.monster;

        ctx.addLog(LogType.ACTION, "玩家发动普通攻击。");

        // 3.1 计算原始伤害（100%物理攻击，功能清单第3点）
        AttributeSet playerAttr = ctx.player.getFinalAttributes();
        ctx.rawDamage = playerAttr.physicalAtk;
        ctx.addLog(LogType.DAMAGE, "  基础物理伤害：%d", ctx.rawDamage);

        // 3.2 计算命中/闪避/暴击（这里简化，后续可扩展）//TODO
        ctx.isHit = true;
        ctx.isCriticalHit = RandomUtils.checkProbability((float) playerAttr.physicalCritRate);
        if (ctx.isCriticalHit) {
            ctx.rawDamage *= playerAttr.physicalCritDmg;
            ctx.addLog(LogType.DODGE_CRIT, "  触发暴击！伤害提升至 %d", ctx.rawDamage);
        }

        // 3.3 计算最终伤害（减去防御）
        AttributeSet monsterAttr = ctx.monster.getFinalAttributes();
        ctx.finalDamage = Math.max(1, ctx.rawDamage - monsterAttr.physicalDef);
        ctx.addLog(LogType.DAMAGE, "  扣除物理防御(%d)，结算伤害：%d", monsterAttr.physicalDef, ctx.finalDamage);

        // 3.4 触发攻击时的Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(ctx.player, ctx, BuffTriggerType.ON_ATTACK_HIT);
        AffixManager.getInstance(context).triggerAffixes(ctx.player, ctx, AffixTriggerType.ON_ATTACK_HIT);

        // 3.5 造成伤害
        ctx.monster.takeDamage(ctx.finalDamage);
        ctx.addLog(LogType.DAMAGE, "  怪物受到 %d 点伤害。剩余HP：(%d/%d)", ctx.finalDamage, ctx.monster.getCurrentHp(), monsterAttr.maxHp);

        // 3.6 触发受击时的Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(ctx.monster, ctx, BuffTriggerType.ON_DAMAGE_TAKEN);
        AffixManager.getInstance(context).triggerAffixes(ctx.monster, ctx, AffixTriggerType.ON_DAMAGE_TAKEN);

        // 3.7 检查死亡
        checkDeath(ctx);
    }

    // 怪物普攻逻辑
    public void executeMonsterNormalAttack(BattleContext ctx) {
        ctx.resetDamageData();
        ctx.currentActor = ctx.monster;
        ctx.currentTarget = ctx.player;

        ctx.addLog(LogType.ACTION, "怪物发动普通攻击。");

        AttributeSet monsterAttr = ctx.monster.getFinalAttributes();
        ctx.rawDamage = monsterAttr.physicalAtk;
        ctx.addLog(LogType.DAMAGE, "  基础物理伤害：%d", ctx.rawDamage);

        // 简化的伤害计算 (后续应当加上命中、暴击的计算)
        AttributeSet playerAttr = ctx.player.getFinalAttributes();
        ctx.isHit = true;  // 设为必中做占位
        ctx.finalDamage = Math.max(1, ctx.rawDamage - playerAttr.physicalDef);
        ctx.addLog(LogType.DAMAGE, "  扣除物理防御(%d)，结算伤害：%d", playerAttr.physicalDef, ctx.finalDamage);

        // 触发怪物攻击时的Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(ctx.monster, ctx, BuffTriggerType.ON_ATTACK_HIT);
        AffixManager.getInstance(context).triggerAffixes(ctx.monster, ctx, AffixTriggerType.ON_ATTACK_HIT);

        ctx.player.takeDamage(ctx.finalDamage);
        ctx.addLog(LogType.DAMAGE, "  玩家受到 %d 点伤害。剩余HP：(%d/%d)", ctx.finalDamage, ctx.player.getCurrentHp(), playerAttr.maxHp);

        // 触发玩家受击时的Buff和词缀
        BuffManager.getInstance(context).triggerBuffs(ctx.player, ctx, BuffTriggerType.ON_DAMAGE_TAKEN);
        AffixManager.getInstance(context).triggerAffixes(ctx.player, ctx, AffixTriggerType.ON_DAMAGE_TAKEN);

        checkDeath(ctx);
    }

    // 执行怪物意图
    private void executeMonsterIntent(BattleContext ctx, MonsterIntent intent) {
        ctx.addLog(LogType.ACTION, "怪物执行动作：[%s]", intent.getType().name());
        switch (intent.getType()) {
            case ATTACK:
                executeMonsterNormalAttack(ctx);
                break;
            case DEFEND:
                ctx.monster.setDefending(true);
                ctx.addLog(LogType.ACTION, "  怪物进入防御状态。");
                break;
            case BUFF:
                // 给怪物上Buff
                ctx.addLog(LogType.ACTION, "  怪物施放增益技能。");
                break;
            case DEBUFF:
                // 给玩家上Debuff
                ctx.addLog(LogType.ACTION, "  怪物施放减益技能。");
                break;
            case HEAL:
                int healAmount = (int) (intent.getPowerMultiplier() * ctx.monster.getFinalAttributes().maxHp);
                ctx.monster.healHp(healAmount);
                ctx.addLog(LogType.ACTION, "  怪物回复了 %d 点HP。", healAmount);
                break;
        }
    }

    // 检查死亡
    private void checkDeath(BattleContext ctx) {
        if (ctx.player.isDead()) {
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.DEFEAT;
            ctx.addLog(LogType.DEATH, "玩家阵亡。");
        } else if (ctx.monster.isDead()) {
            ctx.isBattleEnded = true;
            ctx.battleResult = BattleContext.BattleResult.VICTORY;
            ctx.addLog(LogType.DEATH, "怪物阵亡，战斗胜利。");
        }
    }

    // 7. 战斗结果结算（功能清单第6、7点）
    private void settleBattleResult(BattleContext ctx) {
        ctx.addLog(LogType.ROUND_INFO, "======== 战斗结算 ========");

        if (ctx.battleResult == BattleContext.BattleResult.VICTORY) {
            // 7.1 计算经验加成（功能清单第7点）
            int baseExp = ctx.monster.getExpReward();
            int playerLevel = ctx.player.getLevel();
            int monsterLevel = ctx.monster.getLevel();
            double expBonus = 1.0;
            if (playerLevel < monsterLevel) {
                expBonus += 0.1 * (monsterLevel - playerLevel);
            }
            int finalExp = (int) (baseExp * expBonus * ctx.player.getFinalAttributes().expBonus);
            ctx.player.gainExp(finalExp);

            // 7.2 计算金币加成
            int baseGold = ctx.monster.getGoldReward();
            int finalGold = (int) (baseGold * ctx.player.getFinalAttributes().goldBonus);
            // TODO: 给玩家加金币

            ctx.addLog(LogType.RESULT, "获得战利品：\n  - 金币：+%d\n  - 经验：+%d", finalGold, finalExp);

            // 7.3 生成掉落物（功能清单第7点）
            // DropManager.getInstance().generateDrops(ctx.monster);

        } else if (ctx.battleResult == BattleContext.BattleResult.DEFEAT) {
            // 6.1 失败惩罚：损失25%金币，保留1点HP（功能清单第6点）
            // TODO: 扣除玩家金币
            ctx.player.setCurrentHp(1);
            ctx.player.setDead(false);
            ctx.addLog(LogType.RESULT, "战斗失败，已扣除部分金币，保留1点生命值。");
        }
    }
}