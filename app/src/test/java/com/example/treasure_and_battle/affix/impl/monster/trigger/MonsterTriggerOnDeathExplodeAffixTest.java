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
public class MonsterTriggerOnDeathExplodeAffixTest {

    private Context context;
    private Monster monster;
    private Player player;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        monster = new Monster("explode_m", "explode_m", 1, Rarity.COMMON,
                10, 10, 10, 10, 10, 10,
                10, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        player = new Player("explode_p", context);
    }

    @Test
    public void testDescription() {
        MonsterTriggerOnDeathExplodeAffix affix = new MonsterTriggerOnDeathExplodeAffix(
                6007,
                "自爆",
                "自爆伤害 %.0f%%",
                Rarity.RARE,
                TriggerType.ON_DEATH,
                0.5f
        );

        assertEquals("自爆伤害 50%", affix.getDescription());
    }

    @Test
    public void testOnTriggerDealsDamageToKiller() {
        MonsterTriggerOnDeathExplodeAffix affix = new MonsterTriggerOnDeathExplodeAffix(
                6007,
                "自爆",
                "",
                Rarity.RARE,
                TriggerType.ON_DEATH,
                0.3f
        );

        BattleContext ctx = new BattleContext(player, monster, false);
        ctx.currentActor = player;

        int hpBefore = player.getCurrentHp();

        affix.onTrigger(monster, ctx);

        int hpAfter = player.getCurrentHp();
        assertTrue("Expected HP to decrease from " + hpBefore + " but got " + hpAfter,
                hpAfter < hpBefore);
    }
}
