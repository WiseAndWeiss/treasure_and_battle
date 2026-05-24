package com.example.treasure_and_battle.affix.impl.monster.defensive;

import android.content.Context;

import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;

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
public class MonsterDamageCapAffixTest {

    private Context context;
    private Monster monster;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        monster = new Monster("cap_m", "cap_m", 1, Rarity.COMMON,
                10, 10, 10, 10, 10, 10,
                10, 10,
                1.0f, 1.0f, 1.0f, 1.0f,
                context);
    }

    @Test
    public void testDescription() {
        MonsterDamageCapAffix affix = new MonsterDamageCapAffix(
                6005,
                "硬化皮肤",
                "上限 %.0f%%",
                Rarity.RARE,
                TriggerType.PERMANENT,
                0.25f
        );

        assertEquals("上限 25%", affix.getDescription());
    }

    @Test
    public void testCapDamageLimitsToMaxRatio() {
        MonsterDamageCapAffix affix = new MonsterDamageCapAffix(
                6005,
                "硬化皮肤",
                "",
                Rarity.RARE,
                TriggerType.PERMANENT,
                0.25f
        );

        int maxHp = monster.getFinalAttributes().maxHp;
        assertTrue(maxHp > 0);
        int largeDamage = maxHp * 10;

        int result = affix.capDamage(largeDamage, maxHp);

        assertTrue(result < largeDamage);
        assertEquals((int) (maxHp * 0.25f), result);
    }

    @Test
    public void testCapDamageDoesNotCapSmallDamage() {
        MonsterDamageCapAffix affix = new MonsterDamageCapAffix(
                6005,
                "硬化皮肤",
                "",
                Rarity.RARE,
                TriggerType.PERMANENT,
                0.25f
        );

        int maxHp = monster.getFinalAttributes().maxHp;
        int smallDamage = 1;

        int result = affix.capDamage(smallDamage, maxHp);

        assertEquals(smallDamage, result);
    }
}
