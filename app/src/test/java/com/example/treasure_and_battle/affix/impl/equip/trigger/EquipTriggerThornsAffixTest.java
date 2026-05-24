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
public class EquipTriggerThornsAffixTest {

    private Context context;
    private Player player;
    private Monster attacker;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("thorns_owner", context);
        attacker = new Monster("attacker_m", "attacker_m", 1, Rarity.COMMON,
                10, 10, 10, 10, 10, 10,
                200, 20,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
    }

    @Test
    public void testDescription() {
        EquipTriggerThornsAffix affix = new EquipTriggerThornsAffix(
                6007,
                "荆棘",
                "反弹 %.0f%%",
                Rarity.RARE,
                TriggerType.ON_AFTER_DAMAGE_TAKEN,
                new EquipCategory[]{EquipCategory.ARMOR},
                0.3f
        );

        assertEquals("反弹 30%", affix.getDescription());
    }

    @Test
    public void testOnTriggerReflectsDamage() {
        EquipTriggerThornsAffix affix = new EquipTriggerThornsAffix(
                6007,
                "荆棘",
                "",
                Rarity.RARE,
                TriggerType.ON_AFTER_DAMAGE_TAKEN,
                new EquipCategory[]{EquipCategory.ARMOR},
                0.5f
        );

        BattleContext ctx = new BattleContext(player, attacker, false);
        ctx.finalDamage = 50;
        ctx.currentActor = attacker;
        ctx.currentTarget = player;

        int hpBefore = attacker.getCurrentHp();

        affix.onTrigger(player, ctx);

        assertTrue(attacker.getCurrentHp() < hpBefore);
    }
}
