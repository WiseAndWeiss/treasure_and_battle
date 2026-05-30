package com.example.treasure_and_battle.affix.impl.monster.trigger;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;

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
public class MonsterTriggerAoeBuffAffixTest {

    private Context context;
    private Monster monster;
    private Player player1;
    private Player player2;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        monster = new Monster("aoe_m", "aoe_m", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                200, 20,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        player1 = new Player("aoe_p1", context);
        player2 = new Player("aoe_p2", context);
    }

    @Test
    public void testDescription() {
        MonsterTriggerAoeBuffAffix affix = new MonsterTriggerAoeBuffAffix(
                6003,
                "毒雾扩散",
                "施加 %.0f 层",
                Rarity.RARE,
                TriggerType.ON_ROUND_END,
                1.0f,
                3001,
                3
        );

        assertEquals("施加 3 层", affix.getDescription());
    }

    @Test
    public void testOnTriggerAppliesBuffToAllPlayers() {
        MonsterTriggerAoeBuffAffix affix = new MonsterTriggerAoeBuffAffix(
                6003,
                "毒雾扩散",
                "",
                Rarity.RARE,
                TriggerType.ON_ROUND_END,
                1.0f,
                3001,
                2
        );

        java.util.List<Monster> monsters = new java.util.ArrayList<>();
        monsters.add(monster);
        BattleContext ctx = new BattleContext(player1, monsters, false);
        ctx.playerParty.add(player2);

        int before1 = player1.getActiveBuffList().size();
        int before2 = player2.getActiveBuffList().size();

        affix.onTrigger(monster, ctx);

        assertTrue(player1.getActiveBuffList().size() > before1);
        assertTrue(player2.getActiveBuffList().size() > before2);
    }
}
