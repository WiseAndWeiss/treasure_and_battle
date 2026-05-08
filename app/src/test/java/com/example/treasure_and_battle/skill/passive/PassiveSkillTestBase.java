package com.example.treasure_and_battle.skill.passive;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.manager.PassiveSkillManager;
import com.example.treasure_and_battle.manager.SkillManager;
import com.example.treasure_and_battle.model.entity.BattleEntity;
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

import static org.junit.Assert.*;

/**
 * 被动技能测试基类
 * 提供通用的战斗模拟环境和测试辅助方法
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public abstract class PassiveSkillTestBase {
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

        // ⚠️ 测试环境特例：直接修改 baseAttributes
        // 说明：在正常游戏流程中，baseAttributes 应该是只读的（由角色创建时设置）
        // 但在测试环境中，我们需要精确控制基础属性值以便测试，
        // 因此直接修改 baseAttributes 的字段。
        //
        // 这不会造成问题，因为：
        // 1. 测试中每个测试方法都会调用 resetEntityStates() 清空所有状态
        // 2. buff/被动技能仍然通过 modifiers 正确地影响 finalAttributes
        // 3. 属性计算流程没有被破坏
        playerAttr.strength = 10;
        playerAttr.agility = 10;
        playerAttr.intelligence = 10;
        playerAttr.spirit = 10;
        playerAttr.physique = 10;
        playerAttr.luck = 10;
        playerAttr.maxHp = 200;
        playerAttr.maxMp = 100;
        playerAttr.physicalAtk = 50;  // 便于计算伤害
        playerAttr.physicalDef = 50;  // 提高基础防御，让3%加成可见（50 * 1.03 = 51.5 → 52）
        playerAttr.magicalAtk = 50;
        playerAttr.magicalDef = 50;
        playerAttr.speed = 15;
        playerAttr.hitRate = 1.0f;    // 100%命中
        playerAttr.dodgeRate = 0f;    // 不闪避
        playerAttr.physicalCritRate = 0f; // 禁用暴击便于测试
        playerAttr.physicalCritDmg = 1.5f;
        playerAttr.magicalCritRate = 0f;
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
        monsterAttr.physicalDef = 20;
        monsterAttr.magicalDef = 20;
        monsterAttr.speed = 10;
        monsterAttr.hitRate = 1.0f;
        monsterAttr.dodgeRate = 0f;
        monsterAttr.physicalCritRate = 0f;
        testMonster.markAttributeCacheDirty();
        testMonster.setCurrentHp(monsterAttr.maxHp);
        testMonster.setCurrentMp(monsterAttr.maxMp);

        // 5. 创建战斗上下文（但不启动战斗，被动技能在测试中手动触发）
        battleContext = new BattleContext(testPlayer, testMonster, false);
    }

    // ====================== 辅助方法 ======================

    /**
     * 创建指定被动技能并设置等级
     */
    protected PassiveSkill createPassiveSkill(String skillId, int level) {
        Skill skill = SkillManager.getInstance(context).createSkillBySkillId(skillId, level);
        assertNotNull("技能 " + skillId + " 不存在", skill);
        assertTrue("技能应该是PassiveSkill类型", skill instanceof PassiveSkill);
        return (PassiveSkill) skill;
    }

    /**
     * 为实体添加被动技能
     */
    protected void addPassiveSkillToEntity(BattleEntity entity, PassiveSkill passiveSkill) {
        entity.addPassiveSkill(passiveSkill);
        entity.markAttributeCacheDirty(); // 被动技能可能影响属性
    }

    /**
     * 模拟攻击并造成伤害（用于触发造成伤害后的被动技能）
     */
    protected int simulateAttackAndDamage(BattleEntity attacker, BattleEntity target, int baseDamage) {
        int hpBefore = target.getCurrentHp();

        // 直接造成伤害（不经过防御计算，便于测试）
        int finalDamage = baseDamage;
        target.takeDamage(finalDamage);

        // 触发造成伤害后的被动技能
        PassiveSkillManager.getInstance().triggerAfterDamageDealtPassiveSkills(attacker, target, finalDamage, battleContext);

        return finalDamage;
    }

    /**
     * 模拟受到伤害（用于触发受到伤害后的被动技能）
     */
    protected int simulateDamageReceived(BattleEntity target, BattleEntity attacker, int damage) {
        int hpBefore = target.getCurrentHp();

        // 触发受到伤害前的被动技能（可能减少伤害）
        int modifiedDamage = PassiveSkillManager.getInstance().triggerBeforeDamageReceivedPassiveSkills(target, attacker, damage, battleContext);

        // 造成伤害
        target.takeDamage(modifiedDamage);

        // 触发受到伤害后的被动技能
        PassiveSkillManager.getInstance().triggerAfterDamageReceivedPassiveSkills(target, attacker, modifiedDamage, battleContext);

        // 触发buff的受击事件
        BuffManager.getInstance(context).triggerAttackedEvent(target, attacker, battleContext);

        return modifiedDamage;
    }

    /**
     * 触发战斗开始事件（用于触发ON_BATTLE_START被动技能）
     */
    protected void triggerBattleStart() {
        PassiveSkillManager.getInstance().triggerPassiveSkills(testPlayer, battleContext);
        PassiveSkillManager.getInstance().triggerPassiveSkills(testMonster, battleContext);
    }

    /**
     * 触发回合开始事件（用于触发ON_ROUND_START被动技能）
     */
    protected void triggerRoundStart() {
        battleContext.currentActor = testPlayer;
        PassiveSkillManager.getInstance().triggerRoundStartPassiveSkills(testPlayer, battleContext);
        PassiveSkillManager.getInstance().triggerRoundStartPassiveSkills(testMonster, battleContext);
    }

    /**
     * 触发回合结束事件（用于触发ON_ROUND_END被动技能）
     */
    protected void triggerRoundEnd() {
        PassiveSkillManager.getInstance().triggerRoundEndPassiveSkills(testPlayer, battleContext);
        PassiveSkillManager.getInstance().triggerRoundEndPassiveSkills(testMonster, battleContext);
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
     * 验证战斗日志包含指定的日志类型
     */
    protected void assertLogExists(LogType logType) {
        boolean found = battleContext.battleLogs.stream()
            .anyMatch(log -> log.getType() == logType);
        assertTrue("未找到类型为 " + logType + " 的日志", found);
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

    /**
     * 重置实体状态（用于每个测试开始前）
     */
    protected void resetEntityStates() {
        // 重置玩家
        AttributeSet playerAttr = testPlayer.getBaseAttributes();
        testPlayer.setCurrentHp(playerAttr.maxHp);
        testPlayer.setCurrentMp(playerAttr.maxMp);
        testPlayer.setDead(false);

        // 重置怪物
        AttributeSet monsterAttr = testMonster.getBaseAttributes();
        testMonster.setCurrentHp(monsterAttr.maxHp);
        testMonster.setCurrentMp(monsterAttr.maxMp);
        testMonster.setDead(false);

        // 清空buff和被动技能列表
        testPlayer.getActiveBuffList().clear();
        testMonster.getActiveBuffList().clear();
        testPlayer.getPassiveSkillList().clear();
        testMonster.getPassiveSkillList().clear();

        // 标记缓存为脏，强制重新计算属性，并触发一次计算以清除旧值
        testPlayer.markAttributeCacheDirty();
        testMonster.markAttributeCacheDirty();

        // 强制触发一次属性计算，确保缓存中没有旧值
        testPlayer.getFinalAttributes();
        testMonster.getFinalAttributes();

        // 清空战斗日志
        battleContext.battleLogs.clear();
    }
}
