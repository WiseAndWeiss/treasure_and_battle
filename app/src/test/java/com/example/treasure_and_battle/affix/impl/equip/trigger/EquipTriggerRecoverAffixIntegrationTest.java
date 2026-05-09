package com.example.treasure_and_battle.affix.impl.equip.trigger;

import android.content.Context;

import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.manager.BattleManager;
import com.example.treasure_and_battle.model.affix.AffixRecoverResourceType;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
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
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class EquipTriggerRecoverAffixIntegrationTest {

    private Context context;
    private BattleManager battleManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        battleManager = BattleManager.getInstance(context);
    }

    @Test
    public void testRecoverMpOnAttackWithFixedValue() {
        Player player = createPlayer("recover_mp_player", 220, 100, 40, 1.0f, 0f);
        player.setCurrentMp(20);

        EquipTriggerRecoverAffix recoverMpAffix = new EquipTriggerRecoverAffix(
            9201,
            "灵息",
            "攻击时有 %.0f%% 概率恢复 %d 点MP",
            Rarity.RARE,
            TriggerType.ON_ATTACK,
            new EquipCategory[]{EquipCategory.WEAPON},
            1.0f,
            AffixRecoverResourceType.MP,
            ValueType.FLAT,
            5,
            0f
        );
        equipAffix(player, recoverMpAffix);

        Monster monster = createMonster("dummy_attack", 300, 0, 0, 1.0f);
        BattleContext ctx = new BattleContext(player, monster, false);

        battleManager.executeNormalAttack(ctx, player, monster);

        assertEquals("攻击触发后应回复固定5点MP", 25, player.getCurrentMp());
    }

    @Test
    public void testRecoverHpOnDodgeWithFixedValue() {
        Player player = createPlayer("recover_hp_player", 200, 80, 20, 0.0f, 1.0f);
        player.setCurrentHp(100);

        EquipTriggerRecoverAffix recoverHpAffix = new EquipTriggerRecoverAffix(
            9202,
            "闪身回息",
            "闪避时有 %.0f%% 概率恢复 %d 点HP",
            Rarity.RARE,
            TriggerType.ON_DODGE,
            new EquipCategory[]{EquipCategory.ARMOR},
            1.0f,
            AffixRecoverResourceType.HP,
            ValueType.FLAT,
            5,
            0f
        );
        equipAffix(player, recoverHpAffix);

        Monster monster = createMonster("attacker_dodge", 300, 0, 0, 0f);
        monster.getBaseAttributes().hitRate = 1.0f;
        monster.markAttributeCacheDirty();

        BattleContext ctx = new BattleContext(player, monster, false);
        battleManager.executeNormalAttack(ctx, monster, player);

        assertTrue("该场景应触发闪避", ctx.isDodged);
        assertEquals("闪避触发后应回复固定5点HP", 105, player.getCurrentHp());
    }

    @Test
    public void testRecoverHpOnHitByFinalDamageRatio() {
        Player player = createPlayer("recover_on_hit_player", 300, 80, 120, 1.0f, 0f);
        player.setCurrentHp(120);

        EquipTriggerRecoverAffix recoverOnHitAffix = new EquipTriggerRecoverAffix(
            9203,
            "掠夺之刃",
            "攻击命中后有 %.0f%% 概率恢复本次伤害 %.0f%% 的生命值",
            Rarity.EPIC,
            TriggerType.ON_HIT,
            new EquipCategory[]{EquipCategory.WEAPON},
            1.0f,
            AffixRecoverResourceType.HP,
            ValueType.FLAT,
            0,
            0.50f
        );
        equipAffix(player, recoverOnHitAffix);

        Monster monster = createMonster("dummy_hit", 600, 20, 0, 0f);
        BattleContext ctx = new BattleContext(player, monster, false);

        battleManager.executeNormalAttack(ctx, player, monster);

        assertTrue("该场景应命中以验证ON_HIT回复", ctx.isHit);

        int expectedRecover = (int) (ctx.finalDamage * 0.50f);
        assertEquals("命中触发后应按最终伤害比例回复", 120 + expectedRecover, player.getCurrentHp());
    }

    private Player createPlayer(String name, int maxHp, int maxMp, int physicalAtk, float hitRate, float dodgeRate) {
        Player player = new Player(name, context);

        AttributeSet base = player.getBaseAttributes();
        base.maxHp = maxHp;
        base.maxMp = maxMp;
        base.physicalAtk = physicalAtk;
        base.physicalDef = 0;
        base.magicalDef = 0;
        base.physicalCritRate = 0f;
        base.physicalCritDmg = 2.0f;
        base.hitRate = hitRate;
        base.dodgeRate = dodgeRate;

        player.markAttributeCacheDirty();
        player.setCurrentHp(maxHp);
        player.setCurrentMp(maxMp);
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
        monster.getBaseAttributes().speed = 1;
        monster.markAttributeCacheDirty();
        monster.setCurrentHp(maxHp);
        return monster;
    }

    private void equipAffix(Player player, EquipTriggerRecoverAffix affix) {
        EquipItem equipment = new EquipItem("eq_" + affix.getAffixId(), "Recover Equip", Rarity.RARE, 1, 1, EquipSlot.WEAPON);
        List<BaseAffix> affixes = new ArrayList<>();
        affixes.add(affix);
        equipment.setAffixes(affixes);
        player.equip(equipment);
        player.markAttributeCacheDirty();
    }
}

