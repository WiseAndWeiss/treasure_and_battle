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
public class EquipTriggerOnKillRecoverAffixTest {

    private Context context;
    private Player player;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("kill_recover_owner", context);
    }

    @Test
    public void testDescription() {
        EquipTriggerOnKillRecoverAffix affix = new EquipTriggerOnKillRecoverAffix(
                6003,
                "食尸",
                "击杀恢复 %.0f%%",
                Rarity.RARE,
                TriggerType.ON_KILL,
                new EquipCategory[]{EquipCategory.WEAPON},
                0.15f
        );

        assertEquals("击杀恢复 15%", affix.getDescription());
    }

    @Test
    public void testOnTriggerRecoversHpAndMp() {
        EquipTriggerOnKillRecoverAffix affix = new EquipTriggerOnKillRecoverAffix(
                6003,
                "食尸",
                "",
                Rarity.RARE,
                TriggerType.ON_KILL,
                new EquipCategory[]{EquipCategory.WEAPON},
                0.2f
        );

        Monster dummy = new Monster("dummy", "dummy", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                100, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        BattleContext ctx = new BattleContext(player, dummy, false);

        player.setCurrentHp(10);
        player.setCurrentMp(5);

        affix.onTrigger(player, ctx);

        assertTrue(player.getCurrentHp() >= 10);
        assertTrue(player.getCurrentMp() >= 5);
    }
}
