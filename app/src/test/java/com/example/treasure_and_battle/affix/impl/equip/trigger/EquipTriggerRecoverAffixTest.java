package com.example.treasure_and_battle.affix.impl.equip.trigger;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.affix.AffixRecoverResourceType;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.EquipCategory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class EquipTriggerRecoverAffixTest {

    private Context context;
    private Player player;
    private Monster monster;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        player = new Player("recover_owner", context);
        player.getBaseAttributes().maxHp = 100;
        player.getBaseAttributes().maxMp = 80;
        player.markAttributeCacheDirty();
        player.setCurrentHp(30);
        player.setCurrentMp(10);

        monster = new Monster("m2", "m2", 1, Rarity.COMMON,
                1, 1, 1, 1, 1, 1,
                10, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
    }

    @Test
    public void testDescriptionDamageRatioMode() {
        EquipTriggerRecoverAffix affix = new EquipTriggerRecoverAffix(
                401, "吸血", "%.0f%%触发，回复伤害%.0f%%", Rarity.RARE,
                AffixTriggerType.ON_HIT, new EquipCategory[]{EquipCategory.WEAPON}, 1.0f,
                AffixRecoverResourceType.HP, ValueType.FLAT, 0, 0.5f
        );

        assertEquals("100%触发，回复伤害50%", affix.getDescription());
    }

    @Test
    public void testRecoverByPercentageOnMp() {
        EquipTriggerRecoverAffix affix = new EquipTriggerRecoverAffix(
                402, "回蓝", "", Rarity.RARE,
                AffixTriggerType.ON_ATTACK, new EquipCategory[]{EquipCategory.WEAPON}, 1.0f,
                AffixRecoverResourceType.MP, ValueType.PERCENTAGE, 25, 0f
        );

        BattleContext ctx = new BattleContext(player, monster, false);
        affix.onTrigger(player, ctx);

        // maxMp=80, 25% -> 20
        assertEquals(30, player.getCurrentMp());
    }

    @Test
    public void testRecoverByDamageRatioOnHp() {
        EquipTriggerRecoverAffix affix = new EquipTriggerRecoverAffix(
                403, "吸血", "", Rarity.RARE,
                AffixTriggerType.ON_HIT, new EquipCategory[]{EquipCategory.WEAPON}, 1.0f,
                AffixRecoverResourceType.HP, ValueType.FLAT, 0, 0.5f
        );

        BattleContext ctx = new BattleContext(player, monster, false);
        ctx.finalDamage = 40;

        affix.onTrigger(player, ctx);

        assertEquals(50, player.getCurrentHp());
    }
}
