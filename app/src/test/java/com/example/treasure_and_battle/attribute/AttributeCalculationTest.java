package com.example.treasure_and_battle.attribute;

import android.content.Context;

import com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix;
import com.example.treasure_and_battle.buff.impl.attribute.AttributeBuff;
import com.example.treasure_and_battle.manager.battle.BuffManager;
import com.example.treasure_and_battle.model.affix.EquipAffixScope;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.utils.AttributeUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

/**
 * 针对数值计算机制（固定值和百分比同乘区逻辑）的单元测试
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class AttributeCalculationTest {

    private Context context;
    private Player testPlayer;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        
        // 1. 初始化测试玩家
        testPlayer = new Player("TestPlayer", context);
        
        // 2. 初始化目标基础属性
        // 设定基础攻击力为100，本身自带基础力量为50
        AttributeSet baseAttr = testPlayer.getBaseAttributes();
        baseAttr.strength = 50;
        baseAttr.physicalAtk = 100;
        
        testPlayer.markAttributeCacheDirty();
    }
    
    @Test
    public void testEquipmentDamageCalculation() {
        // 创建一件武器
        EquipItem weapon = new EquipItem(
                "1", "Test Sword", com.example.treasure_and_battle.model.common.Rarity.LEGENDARY, 100, 10, EquipSlot.WEAPON);
        
        // 武器自带的基础属性
        weapon.getBaseAttributes().physicalAtk = 20;
        
        java.util.List<com.example.treasure_and_battle.affix.BaseAffix> affixes = new java.util.ArrayList<>();
        
        // 添加固定的力量词条 (+10)
        affixes.add(new EquipAttributeAffix(
                1, "力量+", "", com.example.treasure_and_battle.model.common.Rarity.COMMON, 
            com.example.treasure_and_battle.model.common.TriggerType.PERMANENT, null, 10f,
            AttributeType.STRENGTH, ValueType.FLAT, EquipAffixScope.GLOBAL));
                
        // 添加力量百分比词条 (+20%)
        affixes.add(new EquipAttributeAffix(
                2, "力量%+", "", com.example.treasure_and_battle.model.common.Rarity.UNCOMMON, 
            com.example.treasure_and_battle.model.common.TriggerType.PERMANENT, null, 0.20f,
            AttributeType.STRENGTH, ValueType.PERCENTAGE, EquipAffixScope.GLOBAL));
                
        // 添加固定物理攻击力词条 (+30)
        affixes.add(new EquipAttributeAffix(
                3, "物攻+", "", com.example.treasure_and_battle.model.common.Rarity.RARE, 
            com.example.treasure_and_battle.model.common.TriggerType.PERMANENT, null, 30f,
            AttributeType.PHYSICAL_ATK, ValueType.FLAT, EquipAffixScope.GLOBAL));
                
        // 添加物理攻击力百分比词条 (+15%)
        affixes.add(new EquipAttributeAffix(
                4, "物攻%+", "", com.example.treasure_and_battle.model.common.Rarity.EPIC, 
            com.example.treasure_and_battle.model.common.TriggerType.PERMANENT, null, 0.15f,
            AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE, EquipAffixScope.GLOBAL));
                
        weapon.setAffixes(affixes);
        
        // 装备它！
        testPlayer.equip(weapon);
        
        AttributeUtils.calculateFinalAttributes(testPlayer, context);
        AttributeSet finalAttr = testPlayer.getFinalAttributes();
        
        // 验证阶段一：计算六维
        // 基础力量: 50
        // 词缀加成: 力量百分比 +20%, 固定力量 +10
        // 最终力量 = 50 * 1.2 + 10 = 60 + 10 = 70
        assertEquals("装备带来的力量百分比和固定加成计算错误", 70, finalAttr.strength);
        
        // 验证阶段三：计算最终面板
        // 衍生物攻 = 100(基础设定) + 20(力量差70-50=20) = 120
        // 这里 modifiers.physicalAtk 固定总加成 = 20(武器Base自带) + 30(词条 EquipAttributeAffix: PHYSICAL_ATK + FLAT) = 50
        // modifiers.percentPhysicalAtk 总百分比加成 = 0.15
        
        // 所以: 最终物攻 = 120 * 1.15 + 50 = 138 + 50 = 188
        assertEquals("装备带来的物理攻击力百分比和固定加成计算错误", 188, finalAttr.physicalAtk);
    }

    @Test
    public void testBuffDamageCalculation() {
        // 目标公式验证（需证明力量的百分比加成与基础值计算、复合向物理攻击的加成是否完全对应）：
        // 1. 玩家拥有的最终力量 = 基础力量50 * (1 + 后天百分比加成20%) + 固定力量加成50 = 60 + 50 = 110点
        // 2. 力量变化带来的攻击力额外增量 = (最终力量110) - (基础力量50) = 60点
        // 3. 最终由于六维增量所影响过的基石基础攻击力 = 原有基础100 + 力量转化增量60 = 160点
        // 4. 计算独立乘区的面板物理攻击力 = (基石基础攻击力160) * (1 + 攻击力Buff20% + 攻击力被动50%) + 攻击力固定Buff100
        //                                 = 160 * 1.7 + 100 = 272 + 100 = 372
        
        // ------------------ 挂载：基础力量的百分比与固定值增益 ------------------
        // 为角色加 20% 的力量百分比Buff
        AttributeBuff strengthPercentBuff = new AttributeBuff("str_percent", "力量百分比", "",
            BuffType.BUFF, true, 99, 1, false, 0.20f, AttributeType.STRENGTH, ValueType.PERCENTAGE);
        BuffManager.getInstance(context).addBuff(testPlayer, strengthPercentBuff);

        // 为角色加 50 点固定力量
        AttributeBuff strengthFixedBuff = new AttributeBuff("str_fixed", "力量固定", "",
            BuffType.BUFF, true, 99, 1, false, 50f, AttributeType.STRENGTH, ValueType.FLAT);
        BuffManager.getInstance(context).addBuff(testPlayer, strengthFixedBuff);

        // ------------------ 挂载：基础物攻的百分比与固定值增益 ------------------
        // 一个buff让我的物理攻击力增加100
        AttributeBuff atkFixedBuff = new AttributeBuff("atk_fixed", "物攻固定", "",
            BuffType.BUFF, true, 99, 1, false, 100f, AttributeType.PHYSICAL_ATK, ValueType.FLAT);
        BuffManager.getInstance(context).addBuff(testPlayer, atkFixedBuff);

        // 另一个buff让我的物理攻击力提高了20%
        AttributeBuff atkPercentBuff1 = new AttributeBuff("atk_percent_1", "物攻百分比20", "",
            BuffType.BUFF, true, 99, 1, false, 0.20f, AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE);
        BuffManager.getInstance(context).addBuff(testPlayer, atkPercentBuff1);

        // 被动技能也可以看作一个不会过期的百分比Buff，提高了50%
        AttributeBuff atkPercentBuff2 = new AttributeBuff("atk_percent_2", "物攻百分比50", "",
            BuffType.BUFF, true, 99, 1, false, 0.50f, AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE);
        BuffManager.getInstance(context).addBuff(testPlayer, atkPercentBuff2);

        // 触发核心基石与复合运算计算方法
        AttributeUtils.calculateFinalAttributes(testPlayer, context);
        AttributeSet finalAttr = testPlayer.getFinalAttributes();

        // ------------------ 断言验证 ------------------
        // 验证1：验证六维力量计算是否符合六维自身的同乘区逻辑 [ 50 * 1.2 + 50 = 110 ]
        assertEquals("力量（含自身百分比加成与固定加成）计算推导不正确", 110, finalAttr.strength);
        
        // 验证2：验证力量带动基础物攻，且进入二次攻击力独立乘区运算结果 [ (100 + 60 增量) * 1.7 + 100 = 372 ]
        assertEquals("六维力量转基础面板后，百分比和固定值复合计算机制不符合预期", 372, finalAttr.physicalAtk);
    }

    @Test
    public void testEquipmentAndBuffCombinedCalculation() {
        // 测试词缀（装备）和buff同时存在时的计算结果
        // ------------------ 装备配置 ------------------
        EquipItem weapon = new EquipItem(
                "1", "Test Sword", com.example.treasure_and_battle.model.common.Rarity.LEGENDARY, 100, 10, EquipSlot.WEAPON);
        weapon.getBaseAttributes().physicalAtk = 20; // 武器基础属性，相当于固定物攻+20
        
        java.util.List<com.example.treasure_and_battle.affix.BaseAffix> affixes = new java.util.ArrayList<>();
        affixes.add(new EquipAttributeAffix(
                1, "力量+", "", com.example.treasure_and_battle.model.common.Rarity.COMMON, 
            com.example.treasure_and_battle.model.common.TriggerType.PERMANENT, null, 10f,
            AttributeType.STRENGTH, ValueType.FLAT, EquipAffixScope.GLOBAL)); // 固定力量+10
        affixes.add(new EquipAttributeAffix(
                2, "力量%+", "", com.example.treasure_and_battle.model.common.Rarity.UNCOMMON, 
            com.example.treasure_and_battle.model.common.TriggerType.PERMANENT, null, 0.20f,
            AttributeType.STRENGTH, ValueType.PERCENTAGE, EquipAffixScope.GLOBAL)); // 百分比力量+20%
        affixes.add(new EquipAttributeAffix(
                3, "物攻+", "", com.example.treasure_and_battle.model.common.Rarity.RARE, 
            com.example.treasure_and_battle.model.common.TriggerType.PERMANENT, null, 30f,
            AttributeType.PHYSICAL_ATK, ValueType.FLAT, EquipAffixScope.GLOBAL)); // 固定物攻+30
        affixes.add(new EquipAttributeAffix(
                4, "物攻%+", "", com.example.treasure_and_battle.model.common.Rarity.EPIC, 
            com.example.treasure_and_battle.model.common.TriggerType.PERMANENT, null, 0.15f,
            AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE, EquipAffixScope.GLOBAL)); // 百分比物攻+15%
        weapon.setAffixes(affixes);
        testPlayer.equip(weapon);

        // ------------------ Buff配置 ------------------
        AttributeBuff strengthPercentBuff = new AttributeBuff("str_percent", "力量百分比", "",
            BuffType.BUFF, true, 99, 1, false, 0.20f, AttributeType.STRENGTH, ValueType.PERCENTAGE); // 百分比力量+20%
        BuffManager.getInstance(context).addBuff(testPlayer, strengthPercentBuff);
        
        AttributeBuff strengthFixedBuff = new AttributeBuff("str_fixed", "力量固定", "",
            BuffType.BUFF, true, 99, 1, false, 50f, AttributeType.STRENGTH, ValueType.FLAT); // 固定力量+50
        BuffManager.getInstance(context).addBuff(testPlayer, strengthFixedBuff);

        AttributeBuff atkFixedBuff = new AttributeBuff("atk_fixed", "物攻固定", "",
            BuffType.BUFF, true, 99, 1, false, 100f, AttributeType.PHYSICAL_ATK, ValueType.FLAT); // 固定物攻+100
        BuffManager.getInstance(context).addBuff(testPlayer, atkFixedBuff);

        AttributeBuff atkPercentBuff1 = new AttributeBuff("atk_percent_1", "物攻百分比20", "",
            BuffType.BUFF, true, 99, 1, false, 0.20f, AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE); // 百分比物攻+20%
        BuffManager.getInstance(context).addBuff(testPlayer, atkPercentBuff1);

        AttributeBuff atkPercentBuff2 = new AttributeBuff("atk_percent_2", "物攻百分比50", "",
            BuffType.BUFF, true, 99, 1, false, 0.50f, AttributeType.PHYSICAL_ATK, ValueType.PERCENTAGE); // 百分比物攻+50%
        BuffManager.getInstance(context).addBuff(testPlayer, atkPercentBuff2);

        // ------------------ 最终结算与断言 ------------------
        AttributeUtils.calculateFinalAttributes(testPlayer, context);
        AttributeSet finalAttr = testPlayer.getFinalAttributes();

        // 力量计算逻辑预期:
        // 基础=50
        // 百分比同乘区加成 = 20%(装备) + 20%(Buff) = 40%
        // 固定值同乘区加成 = 10(装备) + 50(Buff) = 60
        // 最终力量 = 50 * (1 + 0.40) + 60 = 50 * 1.4 + 60 = 70 + 60 = 130
        assertEquals("装备与Buff叠加时的力量计算错误", 130, finalAttr.strength);

        // 物攻计算逻辑预期:
        // 新基础物攻（受力量增损影响） = 100(原生) + (最终力量130 - 基础力量50) = 180
        // 百分比同乘区加成 = 15%(装备) + 20%(Buff) + 50%(Buff) = 85%
        // 固定值同乘区加成 = 20(武器Base) + 30(装备词缀) + 100(Buff) = 150
        // 最终物攻 = 180 * (1 + 0.85) + 150 = 180 * 1.85 + 150 = 333 + 150 = 483
        assertEquals("装备与Buff叠加时的物攻计算错误", 483, finalAttr.physicalAtk);
    }
}
