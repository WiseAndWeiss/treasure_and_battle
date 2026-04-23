package com.example.treasure_and_battle.buff.impl.defensive;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
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
@Config(sdk = 28, manifest = Config.NONE)
public class DamageReductionBuffTest {

    private Context context;
    private Player player;
    private BattleContext ctx;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("dr_p", context);
        ctx = new BattleContext(player, null, false);
    }

    @Test
    public void testReduceDamageConsumeOneStack() {
        DamageReductionBuff buff = new DamageReductionBuff("dr", "减伤", "", BuffType.BUFF, true, 3, 3, true, 0.25f);
        buff.setStack(2);

        int d1 = buff.reduceDamageForOneHit(100, player, ctx);
        assertEquals(75, d1);
        assertEquals(1, buff.getStackCount());

        int d2 = buff.reduceDamageForOneHit(40, player, ctx);
        assertEquals(30, d2);
        assertEquals(0, buff.getStackCount());
    }

    @Test
    public void testReductionRateClampAndEdgeDamage() {
        DamageReductionBuff over = new DamageReductionBuff("dr2", "减伤", "", BuffType.BUFF, true, 3, 3, true, 5f);
        over.setStack(1);
        assertEquals(0, over.reduceDamageForOneHit(10, player, ctx));

        DamageReductionBuff under = new DamageReductionBuff("dr3", "减伤", "", BuffType.BUFF, true, 3, 3, true, -1f);
        under.setStack(1);
        assertEquals(10, under.reduceDamageForOneHit(10, player, ctx));

        assertEquals(0, under.reduceDamageForOneHit(-5, player, ctx));
    }
}
