package com.example.treasure_and_battle.skill.active;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.battle.BattleManager;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.manager.skill.SkillManager;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.utils.RandomUtils;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * 主动技能测试基类
 * 提供通用的战斗模拟和测试辅助方法
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public abstract class ActiveSkillTestBase {
    protected Context context;
    protected BattleManager battleManager;
    protected Player testPlayer;
    protected Monster testMonster;
    protected BattleContext battleContext;

    @Before
    public void setUp() {
        // 1. 初始化Robolectric模拟Context
        context = RuntimeEnvironment.application;
        battleManager = BattleManager.getInstance(context);

        // 2. 设置固定随机种子，确保测试结果可复现
        RandomUtils.setSeed(123456L);

        // 3. 创建测试玩家（可控属性）
        testPlayer = new Player("TestPlayer", context);
        AttributeSet playerAttr = testPlayer.getBaseAttributes();
        playerAttr.strength = 10;
        playerAttr.agility = 10;
        playerAttr.intelligence = 10;
        playerAttr.spirit = 10;
        playerAttr.physique = 10;
        playerAttr.luck = 10;
        playerAttr.maxHp = 200;
        playerAttr.maxMp = 100;
        playerAttr.physicalAtk = 50;  // 设置较高的攻击力，便于计算伤害
        playerAttr.physicalDef = 10;
        playerAttr.magicalAtk = 50;   // 设置较高的法术攻击
        playerAttr.magicalDef = 10;
        playerAttr.speed = 15;
        playerAttr.hitRate = 1.0f;    // 100%命中，避免随机性
        playerAttr.dodgeRate = 0f;    // 不闪避
        playerAttr.physicalCritRate = 0.5f;  // 50%暴击，便于测试暴击逻辑
        playerAttr.physicalCritDmg = 1.5f;
        playerAttr.magicalCritRate = 0.5f;
        playerAttr.magicalCritDmg = 1.5f;
        playerAttr.expBonus = 1.0f;
        playerAttr.goldBonus = 1.0f;
        testPlayer.markAttributeCacheDirty();
        testPlayer.setCurrentHp(playerAttr.maxHp);
        testPlayer.setCurrentMp(playerAttr.maxMp);

        // 4. 创建测试怪物
        testMonster = MonsterManager.getInstance(context).createMonsterWithoutAffixes(1001);
        AttributeSet monsterAttr = testMonster.getBaseAttributes();
        monsterAttr.maxHp = 300;
        monsterAttr.maxMp = 50;
        monsterAttr.physicalDef = 20;  // 设置防御力，便于测试破甲效果
        monsterAttr.magicalDef = 20;
        monsterAttr.speed = 10;
        monsterAttr.hitRate = 1.0f;
        monsterAttr.dodgeRate = 0f;
        monsterAttr.physicalCritRate = 0f;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(monsterAttr.maxHp);
        testMonster.setCurrentMp(monsterAttr.maxMp);

        // 5. 创建战斗上下文
        battleContext = new BattleContext(testPlayer, testMonster, false);
    }

    /**
     * 创建指定技能并设置等级
     */
    protected ActiveSkill createSkill(String skillId, int level) {
        Skill skill = SkillManager.getInstance(context).createSkillBySkillId(skillId, level);
        assertNotNull("技能 " + skillId + " 不存在", skill);
        assertTrue("技能应该是ActiveSkill类型", skill instanceof ActiveSkill);
        return (ActiveSkill) skill;
    }

    /**
     * 执行技能并返回造成的伤害
     */
    protected int executeSkillAndDamage(ActiveSkill skill, Player attacker, Monster target) {
        // 记录施法前的HP
        int hpBefore = target.getCurrentHp();

        // 执行技能
        battleManager.executeSkill(attacker, skill, Arrays.asList(target), battleContext);

        // 返回造成的伤害
        return hpBefore - target.getCurrentHp();
    }

    /**
     * 验证战斗日志包含指定的日志类型和消息
     */
    protected void assertLogContains(LogType logType, String keyword) {
        boolean found = battleContext.battleLogs.stream()
            .anyMatch(log -> log.getType() == logType && log.getFormattedMessage().contains(keyword));
        assertTrue("未找到类型为 " + logType + " 且包含 '" + keyword + "' 的日志", found);
    }

    /**
     * 验证战斗日志包含指定的日志类型（用于验证伤害日志）
     */
    protected void assertLogExists(LogType logType) {
        boolean found = battleContext.battleLogs.stream()
            .anyMatch(log -> log.getType() == logType);
        assertTrue("未找到类型为 " + logType + " 的日志", found);
    }

    /**
     * 获取战斗日志中指定类型的最后一条消息
     */
    protected String getLastLogMessage(LogType logType) {
        return battleContext.battleLogs.stream()
            .filter(log -> log.getType() == logType)
            .reduce((first, second) -> second)
            .map(log -> log.getFormattedMessage())
            .orElse(null);
    }

    /**
     * 打印所有战斗日志（用于调试）
     */
    protected void printBattleLogs() {
        System.out.println("=== 战斗日志 ===");
        battleContext.battleLogs.forEach(log -> {
            System.out.println("[" + log.getType() + "] " + log.getFormattedMessage());
        });
        System.out.println("================");
    }
}
