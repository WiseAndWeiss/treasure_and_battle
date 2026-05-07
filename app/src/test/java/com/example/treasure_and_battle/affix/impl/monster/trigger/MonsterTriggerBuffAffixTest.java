package com.example.treasure_and_battle.affix.impl.monster.trigger;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
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
public class MonsterTriggerBuffAffixTest {

    private Context context;
    private Monster monster;
    private Player player;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        monster = new Monster("m3", "m3", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                10, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
        player = new Player("player_target", context);
    }

    @Test
    public void testDescriptionWithFixedStacks() {
        MonsterTriggerBuffAffix affix = new MonsterTriggerBuffAffix(
                501, "怪物附加", "%.0f%%概率，施加%d层", Rarity.RARE,
                AffixTriggerType.ON_HIT, 1.0f, 3002,
                AffixBuffApplyTarget.TARGET, 3, 0f
        );

        assertEquals("100%概率，施加3层", affix.getDescription());
    }

    @Test
    public void testDescriptionWithDamageRatioStacks() {
        MonsterTriggerBuffAffix affix = new MonsterTriggerBuffAffix(
                502, "怪物附加", "%.0f%%概率，施加伤害%.0f%%层", Rarity.RARE,
                AffixTriggerType.ON_HIT, 1.0f, 3002,
                AffixBuffApplyTarget.TARGET, 1, 0.2f
        );

        assertEquals("100%概率，施加伤害20%层", affix.getDescription());
    }

    @Test
    public void testApplyBuffToSelf() {
        MonsterTriggerBuffAffix affix = new MonsterTriggerBuffAffix(
                503, "怪物附加", "", Rarity.RARE,
                AffixTriggerType.ON_HIT, 1.0f, 3002,
                AffixBuffApplyTarget.SELF, 2, 0f
        );

        BattleContext ctx = new BattleContext(player, monster, false);
        ctx.currentTarget = player;

        affix.onTrigger(monster, ctx);

        assertTrue(monster.getActiveBuffList().size() >= 1);
        assertTrue(monster.getActiveBuffList().get(0).getStackCount() >= 2);
    }
}
