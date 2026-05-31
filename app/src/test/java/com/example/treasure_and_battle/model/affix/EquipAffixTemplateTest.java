package com.example.treasure_and_battle.model.affix;

import com.example.treasure_and_battle.model.common.TriggerType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * EquipAffixTemplate 单元测试
 * 测试装备词缀模板的属性、应用范围、触发条件
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class EquipAffixTemplateTest {

    // ====================== 基础属性测试 ======================

    @Test
    public void testEquipAffixTemplate_GettersAndSetters() {
        EquipAffixTemplate template = new EquipAffixTemplate();

        template.setTemplateId(1001);
        template.setAffixName("锐利之刃");
        template.setDescriptionFormat("物理攻击力+%d");
        template.setTriggerType(TriggerType.ON_ATTACK);
        template.setAffixClass("EquipAttributeAffix");
        template.setAttributeType("physicalAtk");
        template.setValueType("FIXED");
        template.setAffixScope("GLOBAL");
        template.setApplyTarget("SELF");
        template.setRecoverResourceType("HP");
        template.setRecoverValueType("FIXED");

        assertEquals(1001, template.getTemplateId());
        assertEquals("锐利之刃", template.getAffixName());
        assertEquals("物理攻击力+%d", template.getDescriptionFormat());
        assertEquals(TriggerType.ON_ATTACK, template.getTriggerType());
        assertEquals("EquipAttributeAffix", template.getAffixClass());
        assertEquals("physicalAtk", template.getAttributeType());
        assertEquals("FIXED", template.getValueType());
        assertEquals("GLOBAL", template.getAffixScope());
        assertEquals("SELF", template.getApplyTarget());
        assertEquals("HP", template.getRecoverResourceType());
        assertEquals("FIXED", template.getRecoverValueType());
    }

    @Test
    public void testAllowCategories_WhenSet_GetterReturnsCorrectArray() {
        EquipAffixTemplate template = new EquipAffixTemplate();

        String[] categories = {"WEAPON", "HELMET"};
        template.setAllowCategories(categories);

        assertArrayEquals(categories, template.getAllowCategories());
    }

    @Test
    public void testAllowCategories_WhenNull_ReturnsNull() {
        EquipAffixTemplate template = new EquipAffixTemplate();

        assertNull(template.getAllowCategories());
    }

    // ====================== 稀有度参数测试 ======================

    @Test
    public void testRarityParam_GettersAndSetters() {
        EquipAffixTemplate.RarityParam param = new EquipAffixTemplate.RarityParam();

        param.setRarityId(1);
        param.setMinValue(5.0f);
        param.setMaxValue(10.0f);
        param.setBuffTemplateId(100);
        param.setDamageToStackRatio(0.5f);
        param.setApplyStacks(3);
        param.setRecoverValue(20);
        param.setDamageToRecoverRatio(0.3f);

        assertEquals(1, param.getRarityId());
        assertEquals(5.0f, param.getMinValue(), 0.001f);
        assertEquals(10.0f, param.getMaxValue(), 0.001f);
        assertEquals(Integer.valueOf(100), param.getBuffTemplateId());
        assertEquals(Float.valueOf(0.5f), param.getDamageToStackRatio());
        assertEquals(Integer.valueOf(3), param.getApplyStacks());
        assertEquals(Integer.valueOf(20), param.getRecoverValue());
        assertEquals(Float.valueOf(0.3f), param.getDamageToRecoverRatio());
    }

    @Test
    public void testRarityParam_DefaultValues_AreNull() {
        EquipAffixTemplate.RarityParam param = new EquipAffixTemplate.RarityParam();

        assertEquals(0, param.getRarityId());
        assertEquals(0.0f, param.getMinValue(), 0.001f);
        assertEquals(0.0f, param.getMaxValue(), 0.001f);
        assertNull(param.getBuffTemplateId());
        assertNull(param.getDamageToStackRatio());
        assertNull(param.getApplyStacks());
        assertNull(param.getRecoverValue());
        assertNull(param.getDamageToRecoverRatio());
    }

    @Test
    public void testGetRarityParams_WhenSet_ReturnsCorrectList() {
        EquipAffixTemplate template = new EquipAffixTemplate();

        List<EquipAffixTemplate.RarityParam> params = new ArrayList<>();
        EquipAffixTemplate.RarityParam param1 = new EquipAffixTemplate.RarityParam();
        param1.setRarityId(1);
        EquipAffixTemplate.RarityParam param2 = new EquipAffixTemplate.RarityParam();
        param2.setRarityId(2);

        params.add(param1);
        params.add(param2);

        template.setRarityParams(params);

        List<EquipAffixTemplate.RarityParam> result = template.getRarityParams();
        assertEquals(2, result.size());
        assertEquals(param1, result.get(0));
        assertEquals(param2, result.get(1));
    }

    @Test
    public void testGetRarityParam_WhenExists_ReturnsCorrectParam() {
        EquipAffixTemplate template = new EquipAffixTemplate();

        List<EquipAffixTemplate.RarityParam> params = new ArrayList<>();
        EquipAffixTemplate.RarityParam param1 = new EquipAffixTemplate.RarityParam();
        param1.setRarityId(1);
        EquipAffixTemplate.RarityParam param2 = new EquipAffixTemplate.RarityParam();
        param2.setRarityId(2);

        params.add(param1);
        params.add(param2);

        template.setRarityParams(params);

        EquipAffixTemplate.RarityParam result = template.getRarityParam(2);
        assertNotNull(result);
        assertEquals(2, result.getRarityId());
    }

    @Test
    public void testGetRarityParam_WhenNotExists_ReturnsNull() {
        EquipAffixTemplate template = new EquipAffixTemplate();

        List<EquipAffixTemplate.RarityParam> params = new ArrayList<>();
        EquipAffixTemplate.RarityParam param1 = new EquipAffixTemplate.RarityParam();
        param1.setRarityId(1);

        params.add(param1);
        template.setRarityParams(params);

        EquipAffixTemplate.RarityParam result = template.getRarityParam(99);
        assertNull(result);
    }

    @Test
    public void testGetRarityParam_WhenParamsNull_ReturnsNull() {
        EquipAffixTemplate template = new EquipAffixTemplate();

        EquipAffixTemplate.RarityParam result = template.getRarityParam(1);
        assertNull(result);
    }

    // ====================== 稀有度参数检查测试 ======================

    @Test
    public void testHasRarityParamUpTo_WhenHasMatchingParam_ReturnsTrue() {
        EquipAffixTemplate template = new EquipAffixTemplate();

        List<EquipAffixTemplate.RarityParam> params = new ArrayList<>();
        EquipAffixTemplate.RarityParam param1 = new EquipAffixTemplate.RarityParam();
        param1.setRarityId(1);
        EquipAffixTemplate.RarityParam param3 = new EquipAffixTemplate.RarityParam();
        param3.setRarityId(3);

        params.add(param1);
        params.add(param3);

        template.setRarityParams(params);

        assertTrue(template.hasRarityParamUpTo(2)); // 有rarityId=1<=2
        assertTrue(template.hasRarityParamUpTo(3)); // 有rarityId=3<=3
        assertTrue(template.hasRarityParamUpTo(5)); // 有rarityId=1,3<=5
    }

    @Test
    public void testHasRarityParamUpTo_WhenNoMatchingParam_ReturnsFalse() {
        EquipAffixTemplate template = new EquipAffixTemplate();

        List<EquipAffixTemplate.RarityParam> params = new ArrayList<>();
        EquipAffixTemplate.RarityParam param3 = new EquipAffixTemplate.RarityParam();
        param3.setRarityId(3);

        params.add(param3);
        template.setRarityParams(params);

        assertFalse(template.hasRarityParamUpTo(2)); // 只有rarityId=3>2
    }

    @Test
    public void testHasRarityParamUpTo_WhenParamsNull_ReturnsFalse() {
        EquipAffixTemplate template = new EquipAffixTemplate();

        assertFalse(template.hasRarityParamUpTo(5));
    }

    @Test
    public void testHasRarityParamUpTo_WhenParamsEmpty_ReturnsFalse() {
        EquipAffixTemplate template = new EquipAffixTemplate();
        template.setRarityParams(new ArrayList<>());

        assertFalse(template.hasRarityParamUpTo(1));
    }

    // ====================== 词缀作用范围测试 ======================

    @Test
    public void testAffixScope_GLOBAL_Value() {
        assertEquals("GLOBAL", EquipAffixScope.GLOBAL.name());
    }

    @Test
    public void testAffixScope_EQUIPMENT_ONLY_Value() {
        assertEquals("EQUIPMENT_ONLY", EquipAffixScope.EQUIPMENT_ONLY.name());
    }

    @Test
    public void testAffixScope_Values_AreDistinct() {
        assertNotEquals(EquipAffixScope.GLOBAL, EquipAffixScope.EQUIPMENT_ONLY);
    }

    // ====================== 应用目标测试 ======================

    @Test
    public void testApplyTarget_SELF_Value() {
        assertEquals("SELF", AffixBuffApplyTarget.SELF.name());
    }

    @Test
    public void testApplyTarget_TARGET_Value() {
        assertEquals("TARGET", AffixBuffApplyTarget.TARGET.name());
    }

    @Test
    public void testApplyTarget_Values_AreDistinct() {
        assertNotEquals(AffixBuffApplyTarget.SELF, AffixBuffApplyTarget.TARGET);
    }
}
