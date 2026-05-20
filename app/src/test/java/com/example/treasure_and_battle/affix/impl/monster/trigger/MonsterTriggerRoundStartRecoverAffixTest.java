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
public class MonsterTriggerRoundStartRecoverAffixTest {

    private Context context;
    private Monster monster;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        monster = new Monster("m_rec", "m_rec", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                200, 20,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
    }

    @Test
    public void testDescription() {
        MonsterTriggerRoundStartRecoverAffix affix = new MonsterTriggerRoundStartRecoverAffix(
                6001,
                "再生皮肤",
                "恢复 %.0f%%",
                Rarity.RARE,
                TriggerType.ON_ROUND_START,
                0.05f
        );

        assertEquals("恢复 5%", affix.getDescription());
    }

    @Test
    public void testOnTriggerRecoversHp() {
        MonsterTriggerRoundStartRecoverAffix affix = new MonsterTriggerRoundStartRecoverAffix(
                6001,
                "再生皮肤",
                "",
                Rarity.RARE,
                TriggerType.ON_ROUND_START,
                0.2f
        );

        Player player = new Player("rec_p", context);
        BattleContext ctx = new BattleContext(player, monster, false);

        monster.setCurrentHp(10);

        affix.onTrigger(monster, ctx);

        assertTrue(monster.getCurrentHp() > 10);
    }
}
