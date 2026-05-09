package com.example.treasure_and_battle.model.item;

import com.example.treasure_and_battle.model.item.equip.EquipTemplate;
import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EquipTemplateTest {

    @Test
    public void testFieldMappingViaGson() {
        String json = "{"
                + "\"templateId\":3001,"
                + "\"equipId\":\"equip_weapon_sword_iron\","
                + "\"name\":\"铁剑\","
                + "\"slot\":\"WEAPON\""
                + "}";
        Gson gson = new Gson();
        EquipTemplate t = gson.fromJson(json, EquipTemplate.class);

        assertNotNull(t);
        assertEquals(3001, t.getTemplateId());
        assertEquals("equip_weapon_sword_iron", t.getEquipId());
        assertEquals("铁剑", t.getName());
        assertEquals("WEAPON", t.getSlot());
    }

    @Test
    public void testArmorSlot() {
        String json = "{"
                + "\"templateId\":3002,"
                + "\"equipId\":\"equip_armor_chest_leather\","
                + "\"name\":\"皮胸甲\","
                + "\"slot\":\"CHEST\""
                + "}";
        Gson gson = new Gson();
        EquipTemplate t = gson.fromJson(json, EquipTemplate.class);

        assertNotNull(t);
        assertEquals("CHEST", t.getSlot());
    }

    @Test
    public void testAccessorySlot() {
        String json = "{"
                + "\"templateId\":3003,"
                + "\"equipId\":\"equip_accessory_ring_iron\","
                + "\"name\":\"铁戒指\","
                + "\"slot\":\"RING\""
                + "}";
        Gson gson = new Gson();
        EquipTemplate t = gson.fromJson(json, EquipTemplate.class);

        assertNotNull(t);
        assertEquals("RING", t.getSlot());
    }
}
