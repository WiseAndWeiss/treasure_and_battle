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
public class EquipTriggerSkillCastRecoverMpAffixTest {

    private Context context;
    private Player player;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("skill_mp_owner", context);
    }

    @Test
    public void testDescription() {
        EquipTriggerSkillCastRecoverMpAffix affix = new EquipTriggerSkillCastRecoverMpAffix(
                6008,
                "魔力源泉",
                "恢复法力 %.0f%%",
                Rarity.RARE,
                TriggerType.ON_SKILL_CAST,
                new EquipCategory[]{EquipCategory.ACCESSORY},
                0.2f
        );

        assertEquals("恢复法力 20%", affix.getDescription());
    }

    @Test
    public void testOnTriggerRecoversMp() {
        EquipTriggerSkillCastRecoverMpAffix affix = new EquipTriggerSkillCastRecoverMpAffix(
                6008,
                "魔力源泉",
                "",
                Rarity.RARE,
                TriggerType.ON_SKILL_CAST,
                new EquipCategory[]{EquipCategory.ACCESSORY},
                0.3f
        );

        Monster dummy = new Monster("dummy", "dummy", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                100, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        BattleContext ctx = new BattleContext(player, dummy, false);

        player.setCurrentMp(5);

        affix.onTrigger(player, ctx);

        assertTrue(player.getCurrentMp() >= 5);
    }
}
