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
public class ShieldBuffTest {

    private Context context;
    private Player player;
    private BattleContext ctx;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("shield_p", context);
        ctx = new BattleContext(player, null, false);
    }

    @Test
    public void testAbsorbDamagePartiallyAndFully() {
        ShieldBuff shield = new ShieldBuff("shield", "护盾", "", BuffType.BUFF, true, 3, 999, true, 0f);
        shield.setStack(10);

        int left = shield.absorbDamage(6, player, ctx);
        assertEquals(0, left);
        assertEquals(4, shield.getStackCount());

        left = shield.absorbDamage(10, player, ctx);
        assertEquals(6, left);
        assertEquals(0, shield.getStackCount());
    }
}
