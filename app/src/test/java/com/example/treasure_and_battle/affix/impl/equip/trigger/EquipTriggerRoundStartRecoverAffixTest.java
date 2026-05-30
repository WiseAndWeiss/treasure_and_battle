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
public class EquipTriggerRoundStartRecoverAffixTest {

    private Context context;
    private Player player;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("round_recover_owner", context);
    }

    @Test
    public void testDescription() {
        EquipTriggerRoundStartRecoverAffix affix = new EquipTriggerRoundStartRecoverAffix(
                6004,
                "回春之力",
                "恢复 %.0f%%",
                Rarity.RARE,
                TriggerType.ON_ROUND_START,
                new EquipCategory[]{EquipCategory.ARMOR},
                0.05f
        );

        assertEquals("恢复 5%", affix.getDescription());
    }

    @Test
    public void testOnTriggerRecoversHp() {
        EquipTriggerRoundStartRecoverAffix affix = new EquipTriggerRoundStartRecoverAffix(
                6004,
                "回春之力",
                "",
                Rarity.RARE,
                TriggerType.ON_ROUND_START,
                new EquipCategory[]{EquipCategory.ARMOR},
                0.2f
        );

        Monster dummy = new Monster("dummy", "dummy", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                100, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        BattleContext ctx = new BattleContext(player, dummy, false);

        player.setCurrentHp(10);

        affix.onTrigger(player, ctx);

        assertTrue(player.getCurrentHp() > 10);
    }

    @Test
    public void testOnTriggerDoesNotExceedMaxHp() {
        EquipTriggerRoundStartRecoverAffix affix = new EquipTriggerRoundStartRecoverAffix(
                6004,
                "回春之力",
                "",
                Rarity.RARE,
                TriggerType.ON_ROUND_START,
                new EquipCategory[]{EquipCategory.ARMOR},
                0.5f
        );

        Monster dummy = new Monster("dummy", "dummy", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                100, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        BattleContext ctx = new BattleContext(player, dummy, false);

        int maxHp = player.getFinalAttributes().maxHp;
        player.setCurrentHp((int)(maxHp * 0.9f));

        affix.onTrigger(player, ctx);

        assertEquals(maxHp, player.getCurrentHp());
    }
}
