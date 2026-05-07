package com.example.treasure_and_battle.affix.impl.equip.trigger;

import android.content.Context;

import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.affix.EquipAffixScope;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.EquipCategory;
import com.example.treasure_and_battle.model.item.EquipItem;
import com.example.treasure_and_battle.model.item.EquipSlot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class EquipTriggerBuffAffixIntegrationTest {

    private Context context;
    private BattleManager battleManager;
    private BuffManager buffManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        battleManager = BattleManager.getInstance(context);
        buffManager = BuffManager.getInstance(context);
    }

    @Test
    public void testNoBurningWhenAttackMissesAgainstHighDodgeMonster() {
        Player player = createPlayerWithWeaponAndAffixes();

        // 按需求构造“100%闪避怪”。
        // 注意：当前属性系统存在硬上限时，最终 dodge 可能被夹到上限值。
        Monster dodgeMonster = createMonster("dodge_monster", 4000, 0, 0, 1.0f);

        BattleContext ctx = new BattleContext(player, dodgeMonster, false);
        battleManager.executeNormalAttack(ctx, player, dodgeMonster);

        BaseBuff burning = findBuffById(dodgeMonster, "debuff_burning");

        // 这个断言用于检测“未命中时不应上燃烧”。
        // 若当前命中逻辑尚未实现，此断言会失败，从而暴露实现问题。
        assertFalse("攻击未命中时不应给目标施加燃烧", burning != null);
        assertFalse("高闪避目标应触发未命中", ctx.isHit);
    }

    @Test
    public void testBurningStacksScaleWithFinalDamageAndMagicDefMitigatesDot() {
        Player player = createPlayerWithWeaponAndAffixes();
        Monster tankMonster = createMonster("tank_monster", 5000, 50, 40, 0f);

        BattleContext ctx = new BattleContext(player, tankMonster, false);
        battleManager.executeNormalAttack(ctx, player, tankMonster);

        assertTrue("该场景应命中以验证燃烧叠层", ctx.isHit);

        BaseBuff burning = findBuffById(tankMonster, "debuff_burning");
        assertNotNull("命中后应施加燃烧（由 buff_config 模板 3002 创建）", burning);

        int expectedStacks = (int) Math.floor(ctx.finalDamage * 0.30f);
        assertEquals("燃烧层数应等于 floor(本次最终伤害 * 30%)", expectedStacks, burning.getStackCount());

        int hpBeforeDot = tankMonster.getCurrentHp();
        buffManager.triggerBuffs(tankMonster, ctx, BuffTriggerType.ON_ROUND_END);
        int hpAfterDot = tankMonster.getCurrentHp();

        int expectedDotDamage = Math.max(1, expectedStacks - tankMonster.getFinalAttributes().magicalDef);
        assertEquals("燃烧造成的魔法伤害应被魔法防御减免", expectedDotDamage, hpBeforeDot - hpAfterDot);
    }

    private Player createPlayerWithWeaponAndAffixes() {
        Player player = new Player("affix_test_player", context);

        AttributeSet playerBase = player.getBaseAttributes();
        playerBase.strength = 0;
        playerBase.agility = 0;
        playerBase.intelligence = 0;
        playerBase.spirit = 0;
        playerBase.physique = 0;
        playerBase.luck = 0;

        playerBase.maxHp = 2000;
        playerBase.maxMp = 200;
        playerBase.physicalAtk = 100;
        playerBase.physicalDef = 0;
        playerBase.magicalAtk = 0;
        playerBase.magicalDef = 0;
        playerBase.speed = 10;
        playerBase.maxActionPoints = 2;

        playerBase.physicalCritRate = 0f;
        playerBase.magicalCritRate = 0f;
        playerBase.physicalCritDmg = 2.0f;
        playerBase.magicalCritDmg = 2.0f;
        playerBase.hitRate = 1.0f;
        playerBase.dodgeRate = 0f;

        player.markAttributeCacheDirty();
        player.setCurrentHp(playerBase.maxHp);

        EquipItem weapon = new EquipItem("weapon_100", "Test Weapon", Rarity.RARE, 100, 1, EquipSlot.WEAPON);
        weapon.getBaseAttributes().physicalAtk = 100;

        EquipAttributeAffix playerAtkPercentAffix = new EquipAttributeAffix(
                9101,
                "玩家攻击提升",
                "玩家物攻提高 %.0f%%",
                Rarity.RARE,
                AffixTriggerType.PERMANENT,
                new EquipCategory[]{EquipCategory.WEAPON},
                0.20f,
                AttributeType.PHYSICAL_ATK,
                ValueType.PERCENTAGE,
                EquipAffixScope.GLOBAL
        );

        EquipAttributeAffix weaponAtkPercentAffix = new EquipAttributeAffix(
                9102,
                "武器攻击提升",
                "武器物攻提高 %.0f%%",
                Rarity.RARE,
                AffixTriggerType.PERMANENT,
                new EquipCategory[]{EquipCategory.WEAPON},
                0.50f,
                AttributeType.PHYSICAL_ATK,
                ValueType.PERCENTAGE,
                EquipAffixScope.EQUIPMENT_ONLY
        );

        // 模拟装备生成阶段：将 EQUIPMENT_ONLY 词条写回装备基础属性。
        applyEquipmentOnlyAffixToWeaponBase(weapon, weaponAtkPercentAffix);

        EquipTriggerBuffAffix burnOnHitAffix = new EquipTriggerBuffAffix(
                9103,
                "焚烬",
                "命中后有 %.0f%% 概率施加相当于本次伤害 %.0f%% 层数的燃烧",
                Rarity.RARE,
                AffixTriggerType.ON_HIT,
                new EquipCategory[]{EquipCategory.WEAPON},
                1.0f,
                3002,
                AffixBuffApplyTarget.TARGET,
                1,
                0.30f
        );

        List<BaseAffix> affixes = new ArrayList<>();
        affixes.add(playerAtkPercentAffix);
        affixes.add(weaponAtkPercentAffix);
        affixes.add(burnOnHitAffix);
        weapon.setAffixes(affixes);

        player.equip(weapon);
        player.markAttributeCacheDirty();
        return player;
    }

    private Monster createMonster(String id, int maxHp, int physicalDef, int magicalDef, float dodgeRate) {
        Monster monster = new Monster(
                id, id, 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                10, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context
        );

        monster.getBaseAttributes().maxHp = maxHp;
        monster.getBaseAttributes().physicalDef = physicalDef;
        monster.getBaseAttributes().magicalDef = magicalDef;
        monster.getBaseAttributes().dodgeRate = dodgeRate;
        monster.markAttributeCacheDirty();
        monster.setCurrentHp(maxHp);
        return monster;
    }

    private BaseBuff findBuffById(Monster monster, String buffId) {
        for (BaseBuff buff : monster.getActiveBuffList()) {
            if (buffId.equals(buff.getBuffId())) {
                return buff;
            }
        }
        return null;
    }

    private void applyEquipmentOnlyAffixToWeaponBase(EquipItem weapon, EquipAttributeAffix equipmentOnlyAffix) {
        AttributeSet equipmentOnlyModifiers = new AttributeSet();
        equipmentOnlyAffix.applyToEquipmentAttributeBonus(equipmentOnlyModifiers);

        AttributeSet base = weapon.getBaseAttributes();
        base.physicalAtk = (int) (base.physicalAtk * (1f + equipmentOnlyModifiers.percentPhysicalAtk)) + equipmentOnlyModifiers.physicalAtk;
    }
}
