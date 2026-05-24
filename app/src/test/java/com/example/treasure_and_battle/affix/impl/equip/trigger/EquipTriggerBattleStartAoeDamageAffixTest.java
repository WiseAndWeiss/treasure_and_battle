package com.example.treasure_and_battle.affix.impl.equip.trigger;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.equip.EquipCategory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class EquipTriggerBattleStartAoeDamageAffixTest {

    private Context context;
    private Player player;
    private Monster monster1;
    private Monster monster2;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("aoe_owner", context);
        monster1 = new Monster("m1", "m1", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                100, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        monster2 = new Monster("m2", "m2", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                100, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
    }

    @Test
    public void testDescription() {
        EquipTriggerBattleStartAoeDamageAffix affix = new EquipTriggerBattleStartAoeDamageAffix(
                6001,
                "先发制人",
                "伤害比例 %.0f%%",
                Rarity.RARE,
                TriggerType.ON_BATTLE_START,
                new EquipCategory[]{EquipCategory.WEAPON},
                0.2f
        );

        assertEquals("伤害比例 20%", affix.getDescription());
    }

    @Test
    public void testOnTriggerDealsDamageToAllMonsters() {
        EquipTriggerBattleStartAoeDamageAffix affix = new EquipTriggerBattleStartAoeDamageAffix(
                6001,
                "先发制人",
                "",
                Rarity.RARE,
                TriggerType.ON_BATTLE_START,
                new EquipCategory[]{EquipCategory.WEAPON},
                0.3f
        );

        java.util.List<Monster> monsters = new java.util.ArrayList<>();
        monsters.add(monster1);
        monsters.add(monster2);
        BattleContext ctx = new BattleContext(player, monsters, false);

        int hpBefore1 = monster1.getCurrentHp();
        int hpBefore2 = monster2.getCurrentHp();

        affix.onTrigger(player, ctx);

        assertTrue(monster1.getCurrentHp() < hpBefore1);
        assertTrue(monster2.getCurrentHp() < hpBefore2);
    }

    @Test
    public void testOnTriggerDoesNothingWhenNoMonsters() {
        EquipTriggerBattleStartAoeDamageAffix affix = new EquipTriggerBattleStartAoeDamageAffix(
                6001,
                "先发制人",
                "",
                Rarity.RARE,
                TriggerType.ON_BATTLE_START,
                new EquipCategory[]{EquipCategory.WEAPON},
                0.3f
        );

        BattleContext ctx = new BattleContext(player, new java.util.ArrayList<>(), false);

        int hpBefore = monster1.getCurrentHp();
        affix.onTrigger(player, ctx);

        assertEquals(hpBefore, monster1.getCurrentHp());
    }
}
