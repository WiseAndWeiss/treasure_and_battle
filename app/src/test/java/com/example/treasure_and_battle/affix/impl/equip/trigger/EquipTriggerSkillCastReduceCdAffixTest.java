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

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class EquipTriggerSkillCastReduceCdAffixTest {

    private Context context;
    private Player player;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("skill_cd_owner", context);
    }

    @Test
    public void testDescription() {
        EquipTriggerSkillCastReduceCdAffix affix = new EquipTriggerSkillCastReduceCdAffix(
                6009,
                "法术狂暴",
                "%.0f%%概率减少冷却",
                Rarity.RARE,
                TriggerType.ON_SKILL_CAST,
                new EquipCategory[]{EquipCategory.WEAPON},
                0.5f
        );

        assertEquals("50%概率减少冷却", affix.getDescription());
    }

    @Test
    public void testOnTriggerDoesNothingWhenNoSkills() {
        EquipTriggerSkillCastReduceCdAffix affix = new EquipTriggerSkillCastReduceCdAffix(
                6009,
                "法术狂暴",
                "",
                Rarity.RARE,
                TriggerType.ON_SKILL_CAST,
                new EquipCategory[]{EquipCategory.WEAPON},
                1.0f
        );

        Monster dummy = new Monster("dummy", "dummy", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                100, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        BattleContext ctx = new BattleContext(player, dummy, false);

        affix.onTrigger(player, ctx);
    }
}
