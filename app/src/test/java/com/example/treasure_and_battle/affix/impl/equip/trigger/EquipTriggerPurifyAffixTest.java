package com.example.treasure_and_battle.affix.impl.equip.trigger;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.periodic.PoisoningDebuff;
import com.example.treasure_and_battle.model.buff.BuffType;
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
public class EquipTriggerPurifyAffixTest {

    private Context context;
    private Player player;
    private Monster dummy;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("purify_owner", context);
        dummy = new Monster("dummy", "dummy", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                100, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
    }

    @Test
    public void testDescription() {
        EquipTriggerPurifyAffix affix = new EquipTriggerPurifyAffix(
                6005,
                "净化",
                "%.0f%%概率净化",
                Rarity.RARE,
                TriggerType.ON_ROUND_END,
                new EquipCategory[]{EquipCategory.ACCESSORY},
                0.5f
        );

        assertEquals("50%概率净化", affix.getDescription());
    }

    @Test
    public void testOnTriggerWith100PercentProbabilityReducesDebuffStack() {
        EquipTriggerPurifyAffix affix = new EquipTriggerPurifyAffix(
                6005,
                "净化",
                "",
                Rarity.RARE,
                TriggerType.ON_ROUND_END,
                new EquipCategory[]{EquipCategory.ACCESSORY},
                1.0f
        );

        PoisoningDebuff poisonDebuff = new PoisoningDebuff(
                "poison_test",
                "中毒",
                "",
                BuffType.DEBUFF,
                true,
                -1,
                10000,
                true,
                1.0f
        );
        poisonDebuff.setStack(4);
        player.getActiveBuffList().add(poisonDebuff);

        BattleContext ctx = new BattleContext(player, dummy, false);

        affix.onTrigger(player, ctx);

        assertTrue(poisonDebuff.getStackCount() < 4);
    }

    @Test
    public void testOnTriggerDoesNothingWhenNoDebuffs() {
        EquipTriggerPurifyAffix affix = new EquipTriggerPurifyAffix(
                6005,
                "净化",
                "",
                Rarity.RARE,
                TriggerType.ON_ROUND_END,
                new EquipCategory[]{EquipCategory.ACCESSORY},
                1.0f
        );

        BattleContext ctx = new BattleContext(player, dummy, false);

        affix.onTrigger(player, ctx);
    }
}
