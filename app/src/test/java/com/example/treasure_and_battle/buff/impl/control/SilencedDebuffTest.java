package com.example.treasure_and_battle.buff.impl.control;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.Player;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class SilencedDebuffTest {

    private Context context;
    private Player player;
    private BattleContext ctx;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("silenced_p", context);
        ctx = new BattleContext(player, (com.example.treasure_and_battle.model.entity.Monster) null, false);
    }

    @Test
    public void testTriggerOnlyOnRoundStart() {
        SilencedDebuff buff = new SilencedDebuff("silenced", "沉默", "", BuffType.DEBUFF, true, 2, 1, true, 0f);

        buff.onTrigger(player, ctx, TriggerType.ON_HIT);
        assertEquals(0, ctx.battleLogs.size());

        buff.onTrigger(player, ctx, TriggerType.ON_ROUND_START);
        assertEquals(1, ctx.battleLogs.size());
    }
}
