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
public class EquipTriggerCritReduceApAffixTest {

    private Context context;
    private Player player;
    private Monster target;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("crit_ap_owner", context);
        target = new Monster("target_m", "target_m", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                100, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
    }

    @Test
    public void testDescription() {
        EquipTriggerCritReduceApAffix affix = new EquipTriggerCritReduceApAffix(
                6006,
                "以暴制暴",
                "%.0f%%概率减少行动点",
                Rarity.RARE,
                TriggerType.ON_CRIT,
                new EquipCategory[]{EquipCategory.WEAPON},
                0.5f
        );

        assertEquals("50%概率减少行动点", affix.getDescription());
    }

    @Test
    public void testOnTriggerReducesActionPoints() {
        EquipTriggerCritReduceApAffix affix = new EquipTriggerCritReduceApAffix(
                6006,
                "以暴制暴",
                "",
                Rarity.RARE,
                TriggerType.ON_CRIT,
                new EquipCategory[]{EquipCategory.WEAPON},
                1.0f
        );

        BattleContext ctx = new BattleContext(player, target, false);
        ctx.currentTarget = target;
        target.setCurrentActionPoints(3);

        affix.onTrigger(player, ctx);

        assertTrue(target.getCurrentActionPoints() <= 3);
    }

    @Test
    public void testOnTriggerDoesNotReduceBelowZero() {
        EquipTriggerCritReduceApAffix affix = new EquipTriggerCritReduceApAffix(
                6006,
                "以暴制暴",
                "",
                Rarity.RARE,
                TriggerType.ON_CRIT,
                new EquipCategory[]{EquipCategory.WEAPON},
                1.0f
        );

        BattleContext ctx = new BattleContext(player, target, false);
        ctx.currentTarget = target;
        target.setCurrentActionPoints(0);

        affix.onTrigger(player, ctx);

        assertEquals(0, target.getCurrentActionPoints());
    }
}
