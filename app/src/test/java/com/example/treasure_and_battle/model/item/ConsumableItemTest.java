package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConsumableItemTest {

    @Test
    public void testCreationBasic() {
        ConsumableItem.Effect effect = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        effect.value = 10;
        effect.valueType = "PERCENTAGE";

        ConsumableItem item = new ConsumableItem("potion_hp_small", "小生命药水",
                Rarity.COMMON, 20, 30, true, true,
                Arrays.asList(effect), "恢复10%HP");

        assertEquals(Rarity.COMMON, item.getRarity());
        assertEquals(ItemType.CONSUMABLE, item.getType());
        assertEquals(30, item.getMaxStack());
        assertTrue(item.canStack());
        assertTrue(item.isUsableInBattle());
        assertTrue(item.isUsableOutBattle());
        assertEquals(1, item.getEffects().size());
    }

    @Test
    public void testEffectTypeEnumAllSeven() {
        assertEquals(7, ConsumableItem.EffectType.values().length);
        assertNotNull(ConsumableItem.EffectType.valueOf("HEAL_HP"));
        assertNotNull(ConsumableItem.EffectType.valueOf("HEAL_MP"));
        assertNotNull(ConsumableItem.EffectType.valueOf("DAMAGE"));
        assertNotNull(ConsumableItem.EffectType.valueOf("BUFF"));
        assertNotNull(ConsumableItem.EffectType.valueOf("CLEANSE"));
        assertNotNull(ConsumableItem.EffectType.valueOf("ESCAPE"));
        assertNotNull(ConsumableItem.EffectType.valueOf("UTILITY"));
    }

    @Test
    public void testTargetEnumAllThree() {
        assertEquals(3, ConsumableItem.Target.values().length);
        assertNotNull(ConsumableItem.Target.valueOf("SELF"));
        assertNotNull(ConsumableItem.Target.valueOf("SINGLE_ENEMY"));
        assertNotNull(ConsumableItem.Target.valueOf("ALL_ENEMIES"));
    }

    @Test
    public void testEffectConstructorSetsType() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.DAMAGE);
        assertEquals(ConsumableItem.EffectType.DAMAGE, e.type);
    }

    @Test
    public void testEffectDefaultConstructor() {
        ConsumableItem.Effect e = new ConsumableItem.Effect();
        assertEquals(null, e.type);
    }

    @Test
    public void testEffectShieldDurationDefaultZero() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        assertEquals(0, e.shieldDuration);
    }

    @Test
    public void testEffectValueDefaultZero() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        assertEquals(0f, e.value, 0.001f);
    }

    @Test
    public void testEffectIsPercentDefaultFalse() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        assertEquals(null, e.valueType);
    }

    @Test
    public void testBuffEntryDefaults() {
        ConsumableItem.BuffEntry be = new ConsumableItem.BuffEntry();
        assertEquals(0, be.buffTemplateId);
        assertEquals(0, be.stacks);
        assertEquals(0, be.duration);
        assertEquals(null, be.valueType);
    }

    @Test
    public void testBuffEntryFullSet() {
        ConsumableItem.BuffEntry be = new ConsumableItem.BuffEntry();
        be.buffTemplateId = 1001;
        be.stacks = 5;
        be.duration = 3;
        be.valueType = "PERCENTAGE";
        assertEquals(1001, be.buffTemplateId);
        assertEquals(5, be.stacks);
        assertEquals(3, be.duration);
        assertEquals("PERCENTAGE", be.valueType);
    }

    @Test
    public void testDebuffEntryDefaults() {
        ConsumableItem.DebuffEntry de = new ConsumableItem.DebuffEntry();
        assertEquals(0, de.buffTemplateId);
        assertEquals(0f, de.stackRatio, 0.001f);
    }

    @Test
    public void testDebuffEntryFullSet() {
        ConsumableItem.DebuffEntry de = new ConsumableItem.DebuffEntry();
        de.buffTemplateId = 2001;
        de.stackRatio = 0.5f;
        assertEquals(2001, de.buffTemplateId);
        assertEquals(0.5f, de.stackRatio, 0.001f);
    }

    @Test
    public void testEffectsNotNullAfterNullInput() {
        ConsumableItem item = new ConsumableItem("c1", "测试",
                Rarity.COMMON, 10, 10, true, true, null, "");
        assertNotNull(item.getEffects());
        assertTrue(item.getEffects().isEmpty());
    }

    @Test
    public void testEffectsNotNullAfterEmptyList() {
        ConsumableItem item = new ConsumableItem("c2", "测试",
                Rarity.COMMON, 10, 10, true, true, new ArrayList<>(), "");
        assertNotNull(item.getEffects());
        assertTrue(item.getEffects().isEmpty());
    }

    @Test
    public void testUsableOnlyInBattle() {
        ConsumableItem item = new ConsumableItem("c3", "战斗中",
                Rarity.COMMON, 10, 10, true, false, new ArrayList<>(), "");
        assertTrue(item.isUsableInBattle());
        assertFalse(item.isUsableOutBattle());
    }

    @Test
    public void testUsableOnlyOutBattle() {
        ConsumableItem item = new ConsumableItem("c4", "战斗外",
                Rarity.COMMON, 10, 10, false, true, new ArrayList<>(), "");
        assertFalse(item.isUsableInBattle());
        assertTrue(item.isUsableOutBattle());
    }

    @Test
    public void testUsableNeither() {
        ConsumableItem item = new ConsumableItem("c5", "都不可用",
                Rarity.COMMON, 10, 10, false, false, new ArrayList<>(), "");
        assertFalse(item.isUsableInBattle());
        assertFalse(item.isUsableOutBattle());
    }

    @Test
    public void testMultipleEffects() {
        ConsumableItem.Effect e1 = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        e1.value = 10;
        ConsumableItem.Effect e2 = new ConsumableItem.Effect(ConsumableItem.EffectType.BUFF);
        ConsumableItem item = new ConsumableItem("c6", "双重",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e1, e2), "");

        assertEquals(2, item.getEffects().size());
        assertEquals(ConsumableItem.EffectType.HEAL_HP, item.getEffects().get(0).type);
        assertEquals(ConsumableItem.EffectType.BUFF, item.getEffects().get(1).type);
    }

    @Test
    public void testEffectWithUtilityId() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.UTILITY);
        e.utilityId = "KEY_COPPER";

        ConsumableItem item = new ConsumableItem("key", "钥匙",
                Rarity.COMMON, 10, 1, false, false, Arrays.asList(e), "");
        assertEquals("KEY_COPPER", item.getEffects().get(0).utilityId);
    }

    @Test
    public void testEffectWithBuffsList() {
        ConsumableItem.BuffEntry be = new ConsumableItem.BuffEntry();
        be.buffTemplateId = 1005;
        be.stacks = 20;
        be.duration = 1;

        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.BUFF);
        e.buffs = Arrays.asList(be);

        ConsumableItem item = new ConsumableItem("buff_item", "Buff物品",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertNotNull(item.getEffects().get(0).buffs);
        assertEquals(1, item.getEffects().get(0).buffs.size());
        assertEquals(1005, item.getEffects().get(0).buffs.get(0).buffTemplateId);
    }

    @Test
    public void testEffectWithDebuffsList() {
        ConsumableItem.DebuffEntry de = new ConsumableItem.DebuffEntry();
        de.buffTemplateId = 1001;
        de.stackRatio = 0.3f;

        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.DAMAGE);
        e.debuffs = Arrays.asList(de);
        e.target = ConsumableItem.Target.ALL_ENEMIES;

        ConsumableItem item = new ConsumableItem("bomb", "炸弹",
                Rarity.COMMON, 10, 10, true, true, Arrays.asList(e), "");
        assertNotNull(item.getEffects().get(0).debuffs);
        assertEquals(1, item.getEffects().get(0).debuffs.size());
        assertEquals(1001, item.getEffects().get(0).debuffs.get(0).buffTemplateId);
        assertEquals(ConsumableItem.Target.ALL_ENEMIES, item.getEffects().get(0).target);
    }

    @Test
    public void testHealEffectWithShieldDuration() {
        ConsumableItem.Effect e = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        e.value = 100;
        e.valueType = "PERCENTAGE";
        e.shieldDuration = 3;

        ConsumableItem item = new ConsumableItem("divine", "圣愈",
                Rarity.LEGENDARY, 800, 5, true, true, Arrays.asList(e), "");
        assertEquals(3, item.getEffects().get(0).shieldDuration);
    }
}
