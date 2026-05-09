package com.example.treasure_and_battle.model.item;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MaterialTemplateTest {

    @Test
    public void testFieldMappingViaGson() {
        String json = "{\"materialId\":\"slime_gel_common\",\"name\":\"史莱姆凝胶\","
                + "\"rarityId\":0,\"baseValue\":5,\"maxStack\":99,\"dropFrom\":\"史莱姆\"}";
        Gson gson = new Gson();
        MaterialTemplate t = gson.fromJson(json, MaterialTemplate.class);

        assertNotNull(t);
        assertEquals("slime_gel_common", t.getMaterialId());
        assertEquals("史莱姆凝胶", t.getName());
        assertEquals(0, t.getRarityId());
        assertEquals(5, t.getBaseValue());
        assertEquals(99, t.getMaxStack());
        assertEquals("史莱姆", t.getDropFrom());
    }

    @Test
    public void testFieldMappingLegendaryMaterial() {
        String json = "{\"materialId\":\"slime_crown_fragment\",\"name\":\"皇家凝胶\","
                + "\"rarityId\":4,\"baseValue\":500,\"maxStack\":10,\"dropFrom\":\"史莱姆国王\"}";
        Gson gson = new Gson();
        MaterialTemplate t = gson.fromJson(json, MaterialTemplate.class);

        assertNotNull(t);
        assertEquals("slime_crown_fragment", t.getMaterialId());
        assertEquals(4, t.getRarityId());
        assertEquals(500, t.getBaseValue());
        assertEquals(10, t.getMaxStack());
    }

    @Test
    public void testEmptyDropFrom() {
        String json = "{\"materialId\":\"test\",\"name\":\"测试\","
                + "\"rarityId\":0,\"baseValue\":1,\"maxStack\":1,\"dropFrom\":\"\"}";
        Gson gson = new Gson();
        MaterialTemplate t = gson.fromJson(json, MaterialTemplate.class);

        assertNotNull(t);
        assertEquals("", t.getDropFrom());
    }
}
