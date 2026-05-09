package com.example.treasure_and_battle.model.item;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ConsumableTemplateTest {

    @Test
    public void testFieldMappingViaGson() {
        String json = "{"
                + "\"consumableId\":\"potion_hp_small\","
                + "\"name\":\"小型生命药水\","
                + "\"rarityId\":0,"
                + "\"baseValue\":20,"
                + "\"maxStack\":30,"
                + "\"usableInBattle\":true,"
                + "\"usableOutBattle\":true,"
                + "\"effects\":[{\"type\":\"HEAL_HP\",\"value\":10,\"isPercent\":true}],"
                + "\"description\":\"恢复10%最大生命值\""
                + "}";
        Gson gson = new Gson();
        ConsumableTemplate t = gson.fromJson(json, ConsumableTemplate.class);

        assertNotNull(t);
        assertEquals("potion_hp_small", t.getConsumableId());
        assertEquals("小型生命药水", t.getName());
        assertEquals(0, t.getRarityId());
        assertEquals(20, t.getBaseValue());
        assertEquals(30, t.getMaxStack());
        assertEquals("恢复10%最大生命值", t.getDescription());
        assertNotNull(t.getEffects());
        assertEquals(1, t.getEffects().size());
    }

    @Test
    public void testUsableFlagsDeserialized() {
        String json = "{"
                + "\"consumableId\":\"test\","
                + "\"name\":\"测试\","
                + "\"rarityId\":0,"
                + "\"baseValue\":1,"
                + "\"maxStack\":1,"
                + "\"usableInBattle\":false,"
                + "\"usableOutBattle\":true,"
                + "\"effects\":[],"
                + "\"description\":\"\""
                + "}";
        Gson gson = new Gson();
        ConsumableTemplate t = gson.fromJson(json, ConsumableTemplate.class);

        assertNotNull(t);
        assertFalse(t.isUsableInBattle());
    }

    @Test
    public void testEffectTypeDeserializedCorrectly() {
        String json = "{"
                + "\"consumableId\":\"test\","
                + "\"name\":\"测试\","
                + "\"rarityId\":0,"
                + "\"baseValue\":1,"
                + "\"maxStack\":1,"
                + "\"usableInBattle\":true,"
                + "\"usableOutBattle\":true,"
                + "\"effects\":[{"
                + "\"type\":\"DAMAGE\","
                + "\"target\":\"ALL_ENEMIES\","
                + "\"value\":50,"
                + "\"isPercent\":true,"
                + "\"debuffs\":[{\"buffTemplateId\":2001,\"stackRatio\":0.3}]"
                + "}],"
                + "\"description\":\"\""
                + "}";
        Gson gson = new Gson();
        ConsumableTemplate t = gson.fromJson(json, ConsumableTemplate.class);

        assertNotNull(t);
        assertEquals(1, t.getEffects().size());
        assertEquals(ConsumableItem.EffectType.DAMAGE, t.getEffects().get(0).type);
        assertEquals(ConsumableItem.Target.ALL_ENEMIES, t.getEffects().get(0).target);
        assertEquals(50f, t.getEffects().get(0).value, 0.001f);
        assertEquals(1, t.getEffects().get(0).debuffs.size());
        assertEquals(2001, t.getEffects().get(0).debuffs.get(0).buffTemplateId);
    }

    @Test
    public void testBuffEffectDeserialized() {
        String json = "{"
                + "\"consumableId\":\"test_buff\","
                + "\"name\":\"Buff测试\","
                + "\"rarityId\":1,"
                + "\"baseValue\":50,"
                + "\"maxStack\":10,"
                + "\"usableInBattle\":true,"
                + "\"usableOutBattle\":false,"
                + "\"effects\":[{"
                + "\"type\":\"BUFF\","
                + "\"buffs\":[{"
                + "\"buffTemplateId\":1005,"
                + "\"stacks\":20,"
                + "\"duration\":1,"
                + "\"isPercent\":true"
                + "}]"
                + "}],"
                + "\"description\":\"buff道具\""
                + "}";
        Gson gson = new Gson();
        ConsumableTemplate t = gson.fromJson(json, ConsumableTemplate.class);

        assertNotNull(t);
        assertEquals(ConsumableItem.EffectType.BUFF, t.getEffects().get(0).type);
        assertEquals(1, t.getEffects().get(0).buffs.size());
        assertEquals(1005, t.getEffects().get(0).buffs.get(0).buffTemplateId);
        assertEquals(20, t.getEffects().get(0).buffs.get(0).stacks);
        assertEquals(1, t.getEffects().get(0).buffs.get(0).duration);
    }

    @Test
    public void testUtilityEffectDeserialized() {
        String json = "{"
                + "\"consumableId\":\"key_copper\","
                + "\"name\":\"铜钥匙\","
                + "\"rarityId\":1,"
                + "\"baseValue\":100,"
                + "\"maxStack\":99,"
                + "\"usableInBattle\":false,"
                + "\"usableOutBattle\":false,"
                + "\"effects\":[{\"type\":\"UTILITY\",\"utilityId\":\"KEY_COPPER\"}],"
                + "\"description\":\"开启铜宝箱\""
                + "}";
        Gson gson = new Gson();
        ConsumableTemplate t = gson.fromJson(json, ConsumableTemplate.class);

        assertNotNull(t);
        assertEquals(ConsumableItem.EffectType.UTILITY, t.getEffects().get(0).type);
        assertEquals("KEY_COPPER", t.getEffects().get(0).utilityId);
    }
}
