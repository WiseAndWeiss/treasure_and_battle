package com.example.treasure_and_battle.affix.impl.monster.trigger;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixRecoverResourceType;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class MonsterTriggerRecoverAffixTest {

    private Context context;
    private Monster monster;
    private Player player;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        monster = new Monster("m4", "m4", 1, Rarity.COMMON,
                120, 80, 1, 1, 0, 0, 1,
                0, 0, 0, 0, 0, 0,
                1, 1, context);
        monster.getBaseAttributes().maxHp = 120;
        monster.getBaseAttributes().maxMp = 80;
        monster.markAttributeCacheDirty();
        monster.setCurrentHp(40);
        monster.setCurrentMp(20);

        player = new Player("p", context);
    }

    @Test
    public void testPercentageRecoverMp() {
        MonsterTriggerRecoverAffix affix = new MonsterTriggerRecoverAffix(
                601, "怪物回蓝", "", Rarity.RARE,
                AffixTriggerType.ON_ROUND_END, 1.0f,
                AffixRecoverResourceType.MP, ValueType.PERCENTAGE, 25, 0f
        );

        BattleContext ctx = new BattleContext(player, monster, false);
        affix.onTrigger(monster, ctx);

        assertEquals(40, monster.getCurrentMp()); // 80 * 25% = 20, 20+20
    }

    @Test
    public void testDamageRatioRecoverHp() {
        MonsterTriggerRecoverAffix affix = new MonsterTriggerRecoverAffix(
                602, "怪物回血", "", Rarity.RARE,
                AffixTriggerType.ON_HIT, 1.0f,
                AffixRecoverResourceType.HP, ValueType.FLAT, 0, 0.5f
        );

        BattleContext ctx = new BattleContext(player, monster, false);
        ctx.finalDamage = 30;

        affix.onTrigger(monster, ctx);

        assertEquals(55, monster.getCurrentHp());
    }

    @Test
    public void testDescriptionBranches() {
        MonsterTriggerRecoverAffix ratio = new MonsterTriggerRecoverAffix(
                603, "A", "%.0f%%触发，恢复伤害%.0f%%", Rarity.RARE,
                AffixTriggerType.ON_HIT, 1.0f,
                AffixRecoverResourceType.HP, ValueType.FLAT, 0, 0.2f
        );
        MonsterTriggerRecoverAffix fixed = new MonsterTriggerRecoverAffix(
                604, "B", "%.0f%%触发，恢复%d", Rarity.RARE,
                AffixTriggerType.ON_HIT, 1.0f,
                AffixRecoverResourceType.HP, ValueType.FLAT, 10, 0f
        );

        assertEquals("100%触发，恢复伤害20%", ratio.getDescription());
        assertEquals("100%触发，恢复10", fixed.getDescription());
    }
}
