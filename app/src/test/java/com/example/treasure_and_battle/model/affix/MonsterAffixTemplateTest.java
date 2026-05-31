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
 * MonsterAffixTemplate 单元测试
 * 测试怪物词缀模板的效果、属性加成
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class MonsterAffixTemplateTest {

    // ====================== 基础属性测试 ======================

    @Test
    public void testMonsterAffixTemplate_GettersAndSetters() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        template.setTemplateId(2001);
        template.setAffixName("狂暴");
        template.setDescriptionFormat("攻击力+%d%%");
        template.setTriggerType(TriggerType.ON_ROUND_START);
        template.setAffixClass("MonsterAttributeAffix");
        template.setAttributeType("physicalAtk");
        template.setValueType("PERCENTAGE");
        template.setApplyTarget("SELF");
        template.setRecoverResourceType("HP");
        template.setRecoverValueType("PERCENTAGE");

        assertEquals(2001, template.getTemplateId());
        assertEquals("狂暴", template.getAffixName());
        assertEquals("攻击力+%d%%", template.getDescriptionFormat());
        assertEquals(TriggerType.ON_ROUND_START, template.getTriggerType());
        assertEquals("MonsterAttributeAffix", template.getAffixClass());
        assertEquals("physicalAtk", template.getAttributeType());
        assertEquals("PERCENTAGE", template.getValueType());
        assertEquals("SELF", template.getApplyTarget());
        assertEquals("HP", template.getRecoverResourceType());
        assertEquals("PERCENTAGE", template.getRecoverValueType());
    }

    // ====================== 稀有度参数测试 ======================

    @Test
    public void testRarityParam_GettersAndSetters() {
        MonsterAffixTemplate.RarityParam param = new MonsterAffixTemplate.RarityParam();

        param.setRarityId(1);
        param.setMinValue(10.0f);
        param.setMaxValue(20.0f);
        param.setBuffTemplateId(200);
        param.setDamageToStackRatio(0.3f);
        param.setApplyStacks(5);
        param.setRecoverValue(30);
        param.setDamageToRecoverRatio(0.2f);

        assertEquals(1, param.getRarityId());
        assertEquals(10.0f, param.getMinValue(), 0.001f);
        assertEquals(20.0f, param.getMaxValue(), 0.001f);
        assertEquals(Integer.valueOf(200), param.getBuffTemplateId());
        assertEquals(Float.valueOf(0.3f), param.getDamageToStackRatio());
        assertEquals(Integer.valueOf(5), param.getApplyStacks());
        assertEquals(Integer.valueOf(30), param.getRecoverValue());
        assertEquals(Float.valueOf(0.2f), param.getDamageToRecoverRatio());
    }

    @Test
    public void testRarityParam_NullableFields_CanBeNull() {
        MonsterAffixTemplate.RarityParam param = new MonsterAffixTemplate.RarityParam();

        param.setRarityId(1);
        param.setMinValue(5.0f);
        param.setMaxValue(15.0f);

        // 可选字段可以不设置
        assertNull(param.getBuffTemplateId());
        assertNull(param.getDamageToStackRatio());
        assertNull(param.getApplyStacks());
        assertNull(param.getRecoverValue());
        assertNull(param.getDamageToRecoverRatio());
    }

    @Test
    public void testGetRarityParams_WhenSet_ReturnsCorrectList() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        List<MonsterAffixTemplate.RarityParam> params = new ArrayList<>();
        MonsterAffixTemplate.RarityParam param1 = new MonsterAffixTemplate.RarityParam();
        param1.setRarityId(1);
        MonsterAffixTemplate.RarityParam param2 = new MonsterAffixTemplate.RarityParam();
        param2.setRarityId(2);

        params.add(param1);
        params.add(param2);

        template.setRarityParams(params);

        List<MonsterAffixTemplate.RarityParam> result = template.getRarityParams();
        assertEquals(2, result.size());
        assertEquals(param1, result.get(0));
        assertEquals(param2, result.get(1));
    }

    @Test
    public void testGetRarityParam_WhenExists_ReturnsCorrectParam() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        List<MonsterAffixTemplate.RarityParam> params = new ArrayList<>();
        MonsterAffixTemplate.RarityParam param1 = new MonsterAffixTemplate.RarityParam();
        param1.setRarityId(1);
        param1.setMinValue(5.0f);
        MonsterAffixTemplate.RarityParam param3 = new MonsterAffixTemplate.RarityParam();
        param3.setRarityId(3);
        param3.setMinValue(15.0f);

        params.add(param1);
        params.add(param3);

        template.setRarityParams(params);

        MonsterAffixTemplate.RarityParam result = template.getRarityParam(3);
        assertNotNull(result);
        assertEquals(3, result.getRarityId());
        assertEquals(15.0f, result.getMinValue(), 0.001f);
    }

    @Test
    public void testGetRarityParam_WhenNotExists_ReturnsNull() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        List<MonsterAffixTemplate.RarityParam> params = new ArrayList<>();
        MonsterAffixTemplate.RarityParam param1 = new MonsterAffixTemplate.RarityParam();
        param1.setRarityId(1);

        params.add(param1);
        template.setRarityParams(params);

        MonsterAffixTemplate.RarityParam result = template.getRarityParam(5);
        assertNull(result);
    }

    // ====================== 稀有度参数检查测试 ======================

    @Test
    public void testHasRarityParamUpTo_WhenHasParamWithinRange_ReturnsTrue() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        List<MonsterAffixTemplate.RarityParam> params = new ArrayList<>();
        MonsterAffixTemplate.RarityParam param1 = new MonsterAffixTemplate.RarityParam();
        param1.setRarityId(1);
        MonsterAffixTemplate.RarityParam param4 = new MonsterAffixTemplate.RarityParam();
        param4.setRarityId(4);

        params.add(param1);
        params.add(param4);

        template.setRarityParams(params);

        assertTrue(template.hasRarityParamUpTo(1)); // 有rarityId=1
        assertTrue(template.hasRarityParamUpTo(2)); // 有rarityId=1<=2
        assertTrue(template.hasRarityParamUpTo(4)); // 有rarityId=4
        assertTrue(template.hasRarityParamUpTo(5)); // 有rarityId=1,4<=5
    }

    @Test
    public void testHasRarityParamUpTo_WhenNoParamWithinRange_ReturnsFalse() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        List<MonsterAffixTemplate.RarityParam> params = new ArrayList<>();
        MonsterAffixTemplate.RarityParam param3 = new MonsterAffixTemplate.RarityParam();
        param3.setRarityId(3);
        MonsterAffixTemplate.RarityParam param4 = new MonsterAffixTemplate.RarityParam();
        param4.setRarityId(4);

        params.add(param3);
        params.add(param4);

        template.setRarityParams(params);

        assertFalse(template.hasRarityParamUpTo(2)); // 只有3,4>2
    }

    @Test
    public void testHasRarityParamUpTo_WhenParamsEmpty_ReturnsFalse() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();
        template.setRarityParams(new ArrayList<>());

        assertFalse(template.hasRarityParamUpTo(5));
    }

    @Test
    public void testHasRarityParamUpTo_WhenParamsNull_ReturnsFalse() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        assertFalse(template.hasRarityParamUpTo(1));
    }

    // ====================== 怪物词缀特殊场景测试 ======================

    @Test
    public void testMonsterAffixTemplate_CanHaveMultipleRarityParams() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        List<MonsterAffixTemplate.RarityParam> params = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            MonsterAffixTemplate.RarityParam param = new MonsterAffixTemplate.RarityParam();
            param.setRarityId(i);
            param.setMinValue(i * 5.0f);
            param.setMaxValue(i * 10.0f);
            params.add(param);
        }

        template.setRarityParams(params);

        assertEquals(5, template.getRarityParams().size());

        // 验证每个稀有度参数
        for (int i = 1; i <= 5; i++) {
            MonsterAffixTemplate.RarityParam param = template.getRarityParam(i);
            assertNotNull(param);
            assertEquals(i * 5.0f, param.getMinValue(), 0.001f);
            assertEquals(i * 10.0f, param.getMaxValue(), 0.001f);
        }
    }

    @Test
    public void testMonsterAffixTemplate_CanHaveTriggerType() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        template.setTriggerType(TriggerType.ON_ATTACK);

        assertEquals(TriggerType.ON_ATTACK, template.getTriggerType());
    }

    @Test
    public void testMonsterAffixTemplate_CanHaveBuffTemplateId() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        List<MonsterAffixTemplate.RarityParam> params = new ArrayList<>();
        MonsterAffixTemplate.RarityParam param = new MonsterAffixTemplate.RarityParam();
        param.setRarityId(1);
        param.setBuffTemplateId(301); // 关联的Buff模板ID

        params.add(param);
        template.setRarityParams(params);

        MonsterAffixTemplate.RarityParam result = template.getRarityParam(1);
        assertEquals(Integer.valueOf(301), result.getBuffTemplateId());
    }

    @Test
    public void testMonsterAffixTemplate_CanHaveRecoverParameters() {
        MonsterAffixTemplate template = new MonsterAffixTemplate();

        // 设置恢复资源类型（模板级别）
        template.setRecoverResourceType("HP");
        template.setRecoverValueType("FIXED");

        List<MonsterAffixTemplate.RarityParam> params = new ArrayList<>();
        MonsterAffixTemplate.RarityParam param = new MonsterAffixTemplate.RarityParam();
        param.setRarityId(1);
        param.setRecoverValue(50);
        param.setDamageToRecoverRatio(0.1f);

        params.add(param);
        template.setRarityParams(params);

        assertEquals("HP", template.getRecoverResourceType());
        assertEquals("FIXED", template.getRecoverValueType());

        MonsterAffixTemplate.RarityParam result = template.getRarityParam(1);
        assertEquals(Integer.valueOf(50), result.getRecoverValue());
        assertEquals(Float.valueOf(0.1f), result.getDamageToRecoverRatio());
    }
}
