package com.example.treasure_and_battle.affix.impl.equip.trigger;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
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
public class EquipTriggerBuffAffixTest {

    private Context context;
    private Player player;
    private Monster monster;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("equip_affix_owner", context);
        monster = new Monster("m1", "m1", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                10, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
    }

    @Test
    public void testDescriptionFixedStacks() {
        EquipTriggerBuffAffix affix = new EquipTriggerBuffAffix(
                301,
                "灼附",
                "有%.0f%%概率施加%.0f层",
                Rarity.RARE,
                TriggerType.ON_HIT,
                new EquipCategory[]{EquipCategory.WEAPON},
                1.0f,
                3002,
                AffixBuffApplyTarget.TARGET,
                2,
                0f
        );

        assertEquals("有100%概率施加2层", affix.getDescription());
    }

    @Test
    public void testDescriptionDamageToStackRatio() {
        EquipTriggerBuffAffix affix = new EquipTriggerBuffAffix(
                302,
                "灼附",
                "有%.0f%%概率施加伤害%.0f%%层数",
                Rarity.RARE,
                TriggerType.ON_HIT,
                new EquipCategory[]{EquipCategory.WEAPON},
                1.0f,
                3002,
                AffixBuffApplyTarget.TARGET,
                1,
                0.3f
        );

        assertEquals("有100%概率施加伤害30%层数", affix.getDescription());
    }

    @Test
    public void testOnTriggerSelfShouldApplyBuffWithAtLeastConfiguredStacks() {
        EquipTriggerBuffAffix affix = new EquipTriggerBuffAffix(
                303,
                "灼附",
                "",
                Rarity.RARE,
                TriggerType.ON_HIT,
                new EquipCategory[]{EquipCategory.WEAPON},
                1.0f,
                3002,
                AffixBuffApplyTarget.SELF,
                2,
                0f
        );

        BattleContext ctx = new BattleContext(player, monster, false);
        ctx.currentTarget = monster;

        affix.onTrigger(player, ctx);

        assertTrue(player.getActiveBuffList().size() >= 1);
        assertTrue(player.getActiveBuffList().get(0).getStackCount() >= 2);
    }
}
