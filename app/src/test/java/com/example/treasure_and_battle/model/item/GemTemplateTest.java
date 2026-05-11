package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.item.gem.GemTemplate;
import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GemTemplateTest {

    @Test
    public void testFieldMappingBasic() {
        String json = "{"
                + "\"gemId\":\"ruby_普通\","
                + "\"name\":\"普通红宝石\","
                + "\"rarityId\":0,"
                + "\"baseValue\":50,"
                + "\"gemType\":\"RUBY\","
                + "\"accessoryBonuses\":[{\"type\":\"STRENGTH\",\"value\":1}],"
                + "\"weaponBonuses\":[{\"type\":\"PHYSICAL_ATK\",\"value\":2}],"
                + "\"armorBonuses\":[{\"type\":\"PHYSICAL_CRIT_DMG\",\"value\":0.01}]"
                + "}";
        Gson gson = new Gson();
        GemTemplate t = gson.fromJson(json, GemTemplate.class);

        assertNotNull(t);
        assertEquals("ruby_普通", t.getGemId());
        assertEquals("普通红宝石", t.getName());
        assertEquals(0, t.getRarityId());
        assertEquals(50, t.getBaseValue());
        assertEquals("RUBY", t.getGemType());
    }

    @Test
    public void testAccessoryBonusesParsed() {
        String json = "{"
                + "\"gemId\":\"test\","
                + "\"name\":\"测试宝石\","
                + "\"rarityId\":0,"
                + "\"baseValue\":10,"
                + "\"gemType\":\"RUBY\","
                + "\"accessoryBonuses\":[{\"type\":\"STRENGTH\",\"value\":1}],"
                + "\"weaponBonuses\":[],"
                + "\"armorBonuses\":[]"
                + "}";
        Gson gson = new Gson();
        GemTemplate t = gson.fromJson(json, GemTemplate.class);

        assertNotNull(t);
        assertNotNull(t.getAccessoryBonuses());
        assertEquals(1, t.getAccessoryBonuses().size());
        assertEquals("STRENGTH", t.getAccessoryBonuses().get(0).type);
        assertEquals(1f, t.getAccessoryBonuses().get(0).value, 0.001f);
    }

    @Test
    public void testWeaponBonusesParsed() {
        String json = "{"
                + "\"gemId\":\"test\","
                + "\"name\":\"测试宝石\","
                + "\"rarityId\":0,"
                + "\"baseValue\":10,"
                + "\"gemType\":\"RUBY\","
                + "\"accessoryBonuses\":[],"
                + "\"weaponBonuses\":[{\"type\":\"PHYSICAL_ATK\",\"value\":2}],"
                + "\"armorBonuses\":[]"
                + "}";
        Gson gson = new Gson();
        GemTemplate t = gson.fromJson(json, GemTemplate.class);

        assertNotNull(t);
        assertNotNull(t.getWeaponBonuses());
        assertEquals(1, t.getWeaponBonuses().size());
        assertEquals("PHYSICAL_ATK", t.getWeaponBonuses().get(0).type);
        assertEquals(2f, t.getWeaponBonuses().get(0).value, 0.001f);
    }

    @Test
    public void testArmorBonusesParsed() {
        String json = "{"
                + "\"gemId\":\"test\","
                + "\"name\":\"测试宝石\","
                + "\"rarityId\":0,"
                + "\"baseValue\":10,"
                + "\"gemType\":\"RUBY\","
                + "\"accessoryBonuses\":[],"
                + "\"weaponBonuses\":[],"
                + "\"armorBonuses\":[{\"type\":\"PHYSICAL_CRIT_DMG\",\"value\":0.01}]"
                + "}";
        Gson gson = new Gson();
        GemTemplate t = gson.fromJson(json, GemTemplate.class);

        assertNotNull(t);
        assertNotNull(t.getArmorBonuses());
        assertEquals(1, t.getArmorBonuses().size());
        assertEquals("PHYSICAL_CRIT_DMG", t.getArmorBonuses().get(0).type);
        assertEquals(0.01f, t.getArmorBonuses().get(0).value, 0.001f);
    }

    @Test
    public void testEmptyBonusLists() {
        String json = "{"
                + "\"gemId\":\"test\","
                + "\"name\":\"测试\","
                + "\"rarityId\":0,"
                + "\"baseValue\":10,"
                + "\"gemType\":\"TEST\","
                + "\"accessoryBonuses\":[],"
                + "\"weaponBonuses\":[],"
                + "\"armorBonuses\":[]"
                + "}";
        Gson gson = new Gson();
        GemTemplate t = gson.fromJson(json, GemTemplate.class);

        assertNotNull(t);
        assertNotNull(t.getAccessoryBonuses());
        assertNotNull(t.getWeaponBonuses());
        assertNotNull(t.getArmorBonuses());
        assertEquals(0, t.getAccessoryBonuses().size());
    }

    @Test
    public void testMultipleBonuses() {
        String json = "{"
                + "\"gemId\":\"test\","
                + "\"name\":\"测试\","
                + "\"rarityId\":4,"
                + "\"baseValue\":2000,"
                + "\"gemType\":\"AMETHYST\","
                + "\"accessoryBonuses\":["
                + "{\"type\":\"SPIRIT\",\"value\":10},"
                + "{\"type\":\"MAX_MP\",\"value\":40}"
                + "],"
                + "\"weaponBonuses\":[],"
                + "\"armorBonuses\":[]"
                + "}";
        Gson gson = new Gson();
        GemTemplate t = gson.fromJson(json, GemTemplate.class);

        assertNotNull(t);
        assertEquals(2, t.getAccessoryBonuses().size());
        assertEquals("SPIRIT", t.getAccessoryBonuses().get(0).type);
        assertEquals(10f, t.getAccessoryBonuses().get(0).value, 0.001f);
        assertEquals("MAX_MP", t.getAccessoryBonuses().get(1).type);
        assertEquals(40f, t.getAccessoryBonuses().get(1).value, 0.001f);
    }

    @Test
    public void testBonusEntryFields() {
        GemTemplate.BonusEntry entry = new GemTemplate.BonusEntry();
        assertNull(entry.type);
        assertEquals(0f, entry.value, 0.001f);
    }
}
