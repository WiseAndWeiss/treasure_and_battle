package com.example.treasure_and_battle.model.event;

import android.graphics.Color;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * EventConfig 单元测试
 * 测试事件配置类的数据访问和颜色转换功能
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class EventConfigTest {

    // ==================== EventConfig 主类测试 ====================

    @Test
    public void testEventConfig_GettersAndSetters() {
        EventConfig config = new EventConfig();

        EventConfig.EventItem[] events = new EventConfig.EventItem[]{new EventConfig.EventItem()};
        config.setEvents(events);
        assertSame("应返回设置的events数组", events, config.getEvents());

        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();
        config.setGlobal(global);
        assertSame("应返回设置的global配置", global, config.getGlobal());
    }

    @Test
    public void testEventConfig_DefaultValues() {
        EventConfig config = new EventConfig();

        assertNull("默认events应为null", config.getEvents());
        assertNull("默认global应为null", config.getGlobal());
    }

    @Test
    public void testEventConfig_NullEvents() {
        EventConfig config = new EventConfig();
        config.setEvents(null);

        assertNull("允许设置为null", config.getEvents());
    }

    @Test
    public void testEventConfig_NullGlobal() {
        EventConfig config = new EventConfig();
        config.setGlobal(null);

        assertNull("允许设置为null", config.getGlobal());
    }

    @Test
    public void testEventConfig_EmptyEventsArray() {
        EventConfig config = new EventConfig();
        EventConfig.EventItem[] emptyArray = new EventConfig.EventItem[0];
        config.setEvents(emptyArray);

        assertEquals("空数组长度应为0", 0, config.getEvents().length);
    }

    @Test
    public void testEventConfig_MultipleEvents() {
        EventConfig config = new EventConfig();
        EventConfig.EventItem[] events = new EventConfig.EventItem[]{
                new EventConfig.EventItem(),
                new EventConfig.EventItem(),
                new EventConfig.EventItem()
        };
        config.setEvents(events);

        assertEquals("应返回3个事件", 3, config.getEvents().length);
    }

    // ==================== GlobalConfig 测试 ====================

    @Test
    public void testGlobalConfig_AllGettersAndSetters() {
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();

        global.setMaxCount(100);
        assertEquals("maxCount", 100, global.getMaxCount());

        global.setMinDistance(50);
        assertEquals("minDistance", 50, global.getMinDistance());

        global.setMaxDistance(200);
        assertEquals("maxDistance", 200, global.getMaxDistance());

        global.setGenerateInterval(5000L);
        assertEquals("generateInterval", 5000L, global.getGenerateInterval());

        global.setGenerateMinNum(2);
        assertEquals("generateMinNum", 2, global.getGenerateMinNum());

        global.setGenerateMaxNum(8);
        assertEquals("generateMaxNum", 8, global.getGenerateMaxNum());
    }

    @Test
    public void testGlobalConfig_DefaultValues() {
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();

        assertEquals("默认maxCount应为0", 0, global.getMaxCount());
        assertEquals("默认minDistance应为0", 0, global.getMinDistance());
        assertEquals("默认maxDistance应为0", 0, global.getMaxDistance());
        assertEquals("默认generateInterval应为0", 0L, global.getGenerateInterval());
        assertEquals("默认generateMinNum应为0", 0, global.getGenerateMinNum());
        assertEquals("默认generateMaxNum应为0", 0, global.getGenerateMaxNum());
    }

    @Test
    public void testGlobalConfig_MaxCount_CanBeZero() {
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();
        global.setMaxCount(0);

        assertEquals(0, global.getMaxCount());
    }

    @Test
    public void testGlobalConfig_MaxCount_CanBeNegative() {
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();
        global.setMaxCount(-1);

        assertEquals(-1, global.getMaxCount());
    }

    @Test
    public void testGlobalConfig_MinDistance_CanBeZero() {
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();
        global.setMinDistance(0);

        assertEquals(0, global.getMinDistance());
    }

    @Test
    public void testGlobalConfig_GenerateInterval_CanBeVeryLarge() {
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();
        long veryLargeValue = Long.MAX_VALUE;
        global.setGenerateInterval(veryLargeValue);

        assertEquals(veryLargeValue, global.getGenerateInterval());
    }

    @Test
    public void testGlobalConfig_GenerateInterval_CanBeNegative() {
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();
        global.setGenerateInterval(-1000L);

        assertEquals(-1000L, global.getGenerateInterval());
    }

    @Test
    public void testGlobalConfig_GenerateMinNum_CanBeGreaterThanMaxNum() {
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();
        // 这种情况在实际使用中可能是配置错误，但setter不验证
        global.setGenerateMinNum(10);
        global.setGenerateMaxNum(5);

        assertEquals(10, global.getGenerateMinNum());
        assertEquals(5, global.getGenerateMaxNum());
    }

    @Test
    public void testGlobalConfig_DistanceValues_MakeSense() {
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();
        global.setMinDistance(10);
        global.setMaxDistance(100);

        assertTrue("maxDistance应大于minDistance",
                global.getMaxDistance() > global.getMinDistance());
    }

    // ==================== EventItem 测试 ====================

    @Test
    public void testEventItem_AllGettersAndSetters() {
        EventConfig.EventItem item = new EventConfig.EventItem();

        item.setType("treasure_chest");
        assertEquals("type", "treasure_chest", item.getType());

        item.setWeight(50);
        assertEquals("weight", 50, item.getWeight());

        item.setRadius(100);
        assertEquals("radius", 100, item.getRadius());

        item.setStrokeColor("#FF0000");
        assertEquals("strokeColor", "#FF0000", item.getStrokeColor());

        item.setFillColor("#00FF00");
        assertEquals("fillColor", "#00FF00", item.getFillColor());

        item.setExpireTime(30000L);
        assertEquals("expireTime", 30000L, item.getExpireTime());

        item.setTriggerDistance(150);
        assertEquals("triggerDistance", 150, item.getTriggerDistance());

        EventConfig.EventSubItem[] subEvents = new EventConfig.EventSubItem[]{
                new EventConfig.EventSubItem(),
                new EventConfig.EventSubItem()
        };
        item.setSubEvents(subEvents);
        assertSame("subEvents", subEvents, item.getSubEvents());
    }

    @Test
    public void testEventItem_DefaultValues() {
        EventConfig.EventItem item = new EventConfig.EventItem();

        assertNull("默认type应为null", item.getType());
        assertEquals("默认weight应为0", 0, item.getWeight());
        assertEquals("默认radius应为0", 0, item.getRadius());
        assertNull("默认strokeColor应为null", item.getStrokeColor());
        assertNull("默认fillColor应为null", item.getFillColor());
        assertEquals("默认expireTime应为0", 0L, item.getExpireTime());
        assertEquals("默认triggerDistance应为0", 0, item.getTriggerDistance());
        assertNull("默认subEvents应为null", item.getSubEvents());
    }

    @Test
    public void testEventItem_Weight_CanBeZero() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setWeight(0);

        assertEquals(0, item.getWeight());
    }

    @Test
    public void testEventItem_Weight_CanBeNegative() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setWeight(-10);

        assertEquals(-10, item.getWeight());
    }

    @Test
    public void testEventItem_Radius_CanBeZero() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setRadius(0);

        assertEquals(0, item.getRadius());
    }

    @Test
    public void testEventItem_Radius_CanBeNegative() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setRadius(-50);

        assertEquals(-50, item.getRadius());
    }

    @Test
    public void testEventItem_ExpireTime_CanBeZero() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setExpireTime(0L);

        assertEquals(0L, item.getExpireTime());
    }

    @Test
    public void testEventItem_ExpireTime_CanBeNegative() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setExpireTime(-5000L);

        assertEquals(-5000L, item.getExpireTime());
    }

    @Test
    public void testEventItem_TriggerDistance_CanBeZero() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setTriggerDistance(0);

        assertEquals(0, item.getTriggerDistance());
    }

    @Test
    public void testEventItem_TriggerDistance_CanBeNegative() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setTriggerDistance(-20);

        assertEquals(-20, item.getTriggerDistance());
    }

    @Test
    public void testEventItem_SubEvents_CanBeNull() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setSubEvents(null);

        assertNull(item.getSubEvents());
    }

    @Test
    public void testEventItem_SubEvents_CanBeEmpty() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        EventConfig.EventSubItem[] empty = new EventConfig.EventSubItem[0];
        item.setSubEvents(empty);

        assertEquals(0, item.getSubEvents().length);
    }

    // ==================== EventItem 颜色转换测试 ====================

    @Test
    public void testEventItem_GetStrokeColorInt_Red() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#FF0000");

        int color = item.getStrokeColorInt();
        assertEquals("红色RGB值应为-65536或0xFFFF0000",
                Color.RED, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_Green() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#00FF00");

        int color = item.getStrokeColorInt();
        assertEquals("绿色RGB值", Color.GREEN, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_Blue() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#0000FF");

        int color = item.getStrokeColorInt();
        assertEquals("蓝色RGB值", Color.BLUE, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_White() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#FFFFFF");

        int color = item.getStrokeColorInt();
        assertEquals("白色RGB值", Color.WHITE, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_Black() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#000000");

        int color = item.getStrokeColorInt();
        assertEquals("黑色RGB值", Color.BLACK, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_Yellow() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#FFFF00");

        int color = item.getStrokeColorInt();
        assertEquals("黄色RGB值", Color.YELLOW, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_Cyan() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#00FFFF");

        int color = item.getStrokeColorInt();
        assertEquals("青色RGB值", Color.CYAN, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_Magenta() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#FF00FF");

        int color = item.getStrokeColorInt();
        assertEquals("品红RGB值", Color.MAGENTA, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_Gray() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#808080");

        int color = item.getStrokeColorInt();
        // Color.GRAY 实际上是 0xFF808080
        assertEquals("灰色RGB值应为0xFF808080", 0xFF808080, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_DarkGray() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#404040");

        int color = item.getStrokeColorInt();
        assertEquals("深灰色RGB值应为-12303292", 0xFF404040, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_LightGray() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#C0C0C0");

        int color = item.getStrokeColorInt();
        assertEquals("浅灰色RGB值应为-4034593", 0xFFC0C0C0, color);
    }

    @Test
    public void testEventItem_GetStrokeColorInt_Transparent() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("#00000000");

        int color = item.getStrokeColorInt();
        assertEquals("透明色RGB值应为0", 0x00000000, color);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEventItem_GetStrokeColorInt_InvalidFormat() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("not_a_color");

        item.getStrokeColorInt(); // 应抛出IllegalArgumentException
    }

    @Test(expected = NullPointerException.class)
    public void testEventItem_GetStrokeColorInt_Null() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor(null);

        item.getStrokeColorInt(); // Color.parseColor(null) 抛出NullPointerException
    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void testEventItem_GetStrokeColorInt_EmptyString() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setStrokeColor("");

        item.getStrokeColorInt(); // Color.parseColor("") 抛出StringIndexOutOfBoundsException
    }

    @Test
    public void testEventItem_GetFillColorInt_Red() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setFillColor("#FF0000");

        int color = item.getFillColorInt();
        assertEquals("红色RGB值", Color.RED, color);
    }

    @Test
    public void testEventItem_GetFillColorInt_Green() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setFillColor("#00FF00");

        int color = item.getFillColorInt();
        assertEquals("绿色RGB值", Color.GREEN, color);
    }

    @Test
    public void testEventItem_GetFillColorInt_Blue() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setFillColor("#0000FF");

        int color = item.getFillColorInt();
        assertEquals("蓝色RGB值", Color.BLUE, color);
    }

    @Test
    public void testEventItem_GetFillColorInt_White() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setFillColor("#FFFFFF");

        int color = item.getFillColorInt();
        assertEquals("白色RGB值", Color.WHITE, color);
    }

    @Test
    public void testEventItem_GetFillColorInt_Black() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setFillColor("#000000");

        int color = item.getFillColorInt();
        assertEquals("黑色RGB值", Color.BLACK, color);
    }

    @Test
    public void testEventItem_GetFillColorInt_CustomColor() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setFillColor("#FF5733");

        int color = item.getFillColorInt();
        assertEquals("自定义颜色RGB值应为-41834", 0xFFFF5733, color);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEventItem_GetFillColorInt_InvalidFormat() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setFillColor("invalid");

        item.getFillColorInt(); // 应抛出IllegalArgumentException
    }

    @Test(expected = NullPointerException.class)
    public void testEventItem_GetFillColorInt_Null() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        item.setFillColor(null);

        item.getFillColorInt(); // Color.parseColor(null) 抛出NullPointerException
    }

    @Test
    public void testEventItem_ColorFormats_SixDigitHex() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        // 测试6位十六进制格式 #RRGGBB
        item.setStrokeColor("#AABBCC");
        int color = item.getStrokeColorInt();

        assertEquals("6位十六进制格式应为0xFFAABBCC", 0xFFAABBCC, color);
    }

    @Test
    public void testEventItem_ColorFormats_EightDigitHexWithAlpha() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        // 测试8位十六进制格式 #AARRGGBB（带透明度）
        item.setStrokeColor("#80AABBCC");
        int color = item.getStrokeColorInt();

        assertEquals("8位十六进制格式应为0x80AABBCC", 0x80AABBCC, color);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEventItem_ColorFormats_ShortFormatNotSupported() {
        EventConfig.EventItem item = new EventConfig.EventItem();
        // Android Color 不支持短格式 #RGB
        item.setStrokeColor("#ABC");

        item.getStrokeColorInt(); // 应抛出IllegalArgumentException
    }

    // ==================== EventSubItem 测试 ====================

    @Test
    public void testEventSubItem_AllGettersAndSetters() {
        EventConfig.EventSubItem subItem = new EventConfig.EventSubItem();

        subItem.setKey("event_001");
        assertEquals("key", "event_001", subItem.getKey());

        subItem.setName("神秘宝箱");
        assertEquals("name", "神秘宝箱", subItem.getName());

        subItem.setDesc("一个散发着神秘光芒的宝箱");
        assertEquals("desc", "一个散发着神秘光芒的宝箱", subItem.getDesc());

        subItem.setReward("金币x100,经验x200");
        assertEquals("reward", "金币x100,经验x200", subItem.getReward());

        subItem.setRisk("可能遇到敌人");
        assertEquals("risk", "可能遇到敌人", subItem.getRisk());

        subItem.setLabel("宝藏");
        assertEquals("label", "宝藏", subItem.getLabel());
    }

    @Test
    public void testEventSubItem_DefaultValues() {
        EventConfig.EventSubItem subItem = new EventConfig.EventSubItem();

        assertNull("默认key应为null", subItem.getKey());
        assertNull("默认name应为null", subItem.getName());
        assertNull("默认desc应为null", subItem.getDesc());
        assertNull("默认reward应为null", subItem.getReward());
        assertNull("默认risk应为null", subItem.getRisk());
        assertNull("默认label应为null", subItem.getLabel());
    }

    @Test
    public void testEventSubItem_Key_EmptyString() {
        EventConfig.EventSubItem subItem = new EventConfig.EventSubItem();
        subItem.setKey("");

        assertEquals("", subItem.getKey());
    }

    @Test
    public void testEventSubItem_Name_EmptyString() {
        EventConfig.EventSubItem subItem = new EventConfig.EventSubItem();
        subItem.setName("");

        assertEquals("", subItem.getName());
    }

    @Test
    public void testEventSubItem_Desc_EmptyString() {
        EventConfig.EventSubItem subItem = new EventConfig.EventSubItem();
        subItem.setDesc("");

        assertEquals("", subItem.getDesc());
    }

    @Test
    public void testEventSubItem_Reward_EmptyString() {
        EventConfig.EventSubItem subItem = new EventConfig.EventSubItem();
        subItem.setReward("");

        assertEquals("", subItem.getReward());
    }

    @Test
    public void testEventSubItem_Risk_EmptyString() {
        EventConfig.EventSubItem subItem = new EventConfig.EventSubItem();
        subItem.setRisk("");

        assertEquals("", subItem.getRisk());
    }

    @Test
    public void testEventSubItem_Label_EmptyString() {
        EventConfig.EventSubItem subItem = new EventConfig.EventSubItem();
        subItem.setLabel("");

        assertEquals("", subItem.getLabel());
    }

    @Test
    public void testEventSubItem_AllFieldsCanBeNull() {
        EventConfig.EventSubItem subItem = new EventConfig.EventSubItem();

        subItem.setKey(null);
        subItem.setName(null);
        subItem.setDesc(null);
        subItem.setReward(null);
        subItem.setRisk(null);
        subItem.setLabel(null);

        assertNull(subItem.getKey());
        assertNull(subItem.getName());
        assertNull(subItem.getDesc());
        assertNull(subItem.getReward());
        assertNull(subItem.getRisk());
        assertNull(subItem.getLabel());
    }

    @Test
    public void testEventSubItem_SpecialCharactersInStrings() {
        EventConfig.EventSubItem subItem = new EventConfig.EventSubItem();

        subItem.setKey("event_@#$%");
        assertEquals("特殊字符key", "event_@#$%", subItem.getKey());

        subItem.setName("测试名称《》");
        assertEquals("特殊字符name", "测试名称《》", subItem.getName());

        subItem.setDesc("描述\n换行\t制表符");
        assertEquals("特殊字符desc", "描述\n换行\t制表符", subItem.getDesc());
    }

    @Test
    public void testEventSubItem_AllFieldsIndependent() {
        EventConfig.EventSubItem item1 = new EventConfig.EventSubItem();
        EventConfig.EventSubItem item2 = new EventConfig.EventSubItem();

        item1.setKey("key1");
        item1.setName("name1");
        item1.setDesc("desc1");
        item1.setReward("reward1");
        item1.setRisk("risk1");
        item1.setLabel("label1");

        item2.setKey("key2");
        item2.setName("name2");
        item2.setDesc("desc2");
        item2.setReward("reward2");
        item2.setRisk("risk2");
        item2.setLabel("label2");

        assertNotEquals("不同实例的key应独立", item1.getKey(), item2.getKey());
        assertNotEquals("不同实例的name应独立", item1.getName(), item2.getName());
        assertNotEquals("不同实例的desc应独立", item1.getDesc(), item2.getDesc());
        assertNotEquals("不同实例的reward应独立", item1.getReward(), item2.getReward());
        assertNotEquals("不同实例的risk应独立", item1.getRisk(), item2.getRisk());
        assertNotEquals("不同实例的label应独立", item1.getLabel(), item2.getLabel());
    }

    // ==================== 组合测试 ====================

    @Test
    public void testFullEventConfigStructure() {
        EventConfig config = new EventConfig();

        // 设置GlobalConfig
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();
        global.setMaxCount(50);
        global.setMinDistance(100);
        global.setMaxDistance(500);
        global.setGenerateInterval(10000L);
        global.setGenerateMinNum(1);
        global.setGenerateMaxNum(3);
        config.setGlobal(global);

        // 设置EventItem
        EventConfig.EventItem[] events = new EventConfig.EventItem[2];
        for (int i = 0; i < events.length; i++) {
            events[i] = new EventConfig.EventItem();
            events[i].setType("event_" + i);
            events[i].setWeight(10 + i * 10);
            events[i].setRadius(100 + i * 50);
            events[i].setStrokeColor("#FF0000");
            events[i].setFillColor("#00FF00");
            events[i].setExpireTime(30000L + i * 10000);
            events[i].setTriggerDistance(150 + i * 25);

            // 设置SubEvents
            EventConfig.EventSubItem[] subEvents = new EventConfig.EventSubItem[3];
            for (int j = 0; j < subEvents.length; j++) {
                subEvents[j] = new EventConfig.EventSubItem();
                subEvents[j].setKey("sub_" + i + "_" + j);
                subEvents[j].setName("子事件" + j);
                subEvents[j].setDesc("这是子事件" + j);
                subEvents[j].setReward("奖励" + j);
                subEvents[j].setRisk("风险" + j);
                subEvents[j].setLabel("标签" + j);
            }
            events[i].setSubEvents(subEvents);
        }
        config.setEvents(events);

        // 验证完整结构
        assertNotNull("global不应为null", config.getGlobal());
        assertEquals("maxCount", 50, config.getGlobal().getMaxCount());
        assertEquals("events长度", 2, config.getEvents().length);

        EventConfig.EventItem firstEvent = config.getEvents()[0];
        assertEquals("第一个事件type", "event_0", firstEvent.getType());
        assertEquals("第一个事件weight", 10, firstEvent.getWeight());
        assertEquals("第一个事件radius", 100, firstEvent.getRadius());
        assertEquals("第一个事件颜色", Color.RED, firstEvent.getStrokeColorInt());
        assertEquals("第一个事件填充色", Color.GREEN, firstEvent.getFillColorInt());

        assertNotNull("第一个事件subEvents不应为null", firstEvent.getSubEvents());
        assertEquals("第一个事件subEvents长度", 3, firstEvent.getSubEvents().length);
        assertEquals("第一个subEvent的key", "sub_0_0", firstEvent.getSubEvents()[0].getKey());
    }

    @Test
    public void testEventItem_ArrayIndependence() {
        EventConfig config1 = new EventConfig();
        EventConfig config2 = new EventConfig();

        EventConfig.EventItem[] events1 = new EventConfig.EventItem[]{new EventConfig.EventItem()};
        events1[0].setType("type1");

        EventConfig.EventItem[] events2 = new EventConfig.EventItem[]{new EventConfig.EventItem()};
        events2[0].setType("type2");

        config1.setEvents(events1);
        config2.setEvents(events2);

        assertEquals("config1的第一个事件type", "type1", config1.getEvents()[0].getType());
        assertEquals("config2的第一个事件type", "type2", config2.getEvents()[0].getType());
    }
}
