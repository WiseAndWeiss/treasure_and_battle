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
public class MonsterTriggerManaBurnAffixTest {

    private Context context;
    private Monster monster;
    private Player player;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        monster = new Monster("burn_m", "burn_m", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                200, 20,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        player = new Player("burn_p", context);
    }

    @Test
    public void testDescription() {
        MonsterTriggerManaBurnAffix affix = new MonsterTriggerManaBurnAffix(
                6006,
                "法力燃烧",
                "燃烧 %.0f 点",
                Rarity.RARE,
                TriggerType.ON_ROUND_END,
                10.0f
        );

        assertEquals("燃烧 10 点", affix.getDescription());
    }

    @Test
    public void testOnTriggerBurnsPlayerMp() {
        MonsterTriggerManaBurnAffix affix = new MonsterTriggerManaBurnAffix(
                6006,
                "法力燃烧",
                "",
                Rarity.RARE,
                TriggerType.ON_ROUND_END,
                10.0f
        );

        BattleContext ctx = new BattleContext(player, monster, false);
        player.setCurrentMp(50);

        affix.onTrigger(monster, ctx);

        assertTrue(player.getCurrentMp() < 50);
    }

    @Test
    public void testOnTriggerDoesNotReduceBelowZero() {
        MonsterTriggerManaBurnAffix affix = new MonsterTriggerManaBurnAffix(
                6006,
                "法力燃烧",
                "",
                Rarity.RARE,
                TriggerType.ON_ROUND_END,
                100.0f
        );

        BattleContext ctx = new BattleContext(player, monster, false);
        player.setCurrentMp(5);

        affix.onTrigger(monster, ctx);

        assertEquals(0, player.getCurrentMp());
    }
}
