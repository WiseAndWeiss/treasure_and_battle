package com.example.treasure_and_battle;

import android.content.Context;

import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.buff.impl.attribute.PhysicalAttackBuff;
import com.example.treasure_and_battle.buff.impl.attribute.StrengthBuff;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.AttributeModifierType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.entity.Player;
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
    public void testBuffDamageCalculation() {
        // 目标公式验证（需证明力量的百分比加成与基础值计算、复合向物理攻击的加成是否完全对应）：
        // 1. 玩家拥有的最终力量 = 基础力量50 * (1 + 后天百分比加成20%) + 固定力量加成50 = 60 + 50 = 110点
        // 2. 力量变化带来的攻击力额外增量 = (最终力量110) - (基础力量50) = 60点
        // 3. 最终由于六维增量所影响过的基石基础攻击力 = 原有基础100 + 力量转化增量60 = 160点
        // 4. 计算独立乘区的面板物理攻击力 = (基石基础攻击力160) * (1 + 攻击力Buff20% + 攻击力被动50%) + 攻击力固定Buff100
        //                                 = 160 * 1.7 + 100 = 272 + 100 = 372
        
        // ------------------ 挂载：基础力量的百分比与固定值增益 ------------------
        // 为角色加 20% 的力量百分比Buff
        StrengthBuff strengthPercentBuff = new StrengthBuff("str_percent", "力量百分比", "", 
            BuffType.BUFF, true, 99, 1, false, 0.20f, AttributeModifierType.PERCENTAGE);
        com.example.treasure_and_battle.manager.BuffManager.getInstance(context).addBuff(testPlayer, strengthPercentBuff);

        // 为角色加 50 点固定力量
        StrengthBuff strengthFixedBuff = new StrengthBuff("str_fixed", "力量固定", "", 
            BuffType.BUFF, true, 99, 1, false, 50f, AttributeModifierType.FLAT);
        com.example.treasure_and_battle.manager.BuffManager.getInstance(context).addBuff(testPlayer, strengthFixedBuff);

        // ------------------ 挂载：基础物攻的百分比与固定值增益 ------------------
        // 一个buff让我的物理攻击力增加100
        PhysicalAttackBuff atkFixedBuff = new PhysicalAttackBuff("atk_fixed", "物攻固定", "", 
            BuffType.BUFF, true, 99, 1, false, 100f, AttributeModifierType.FLAT);
        com.example.treasure_and_battle.manager.BuffManager.getInstance(context).addBuff(testPlayer, atkFixedBuff);

        // 另一个buff让我的物理攻击力提高了20%
        PhysicalAttackBuff atkPercentBuff1 = new PhysicalAttackBuff("atk_percent_1", "物攻百分比20", "", 
            BuffType.BUFF, true, 99, 1, false, 0.20f, AttributeModifierType.PERCENTAGE);
        com.example.treasure_and_battle.manager.BuffManager.getInstance(context).addBuff(testPlayer, atkPercentBuff1);

        // 被动技能也可以看作一个不会过期的百分比Buff，提高了50%
        PhysicalAttackBuff atkPercentBuff2 = new PhysicalAttackBuff("atk_percent_2", "物攻百分比50", "", 
            BuffType.BUFF, true, 99, 1, false, 0.50f, AttributeModifierType.PERCENTAGE);
        com.example.treasure_and_battle.manager.BuffManager.getInstance(context).addBuff(testPlayer, atkPercentBuff2);

        // 触发核心基石与复合运算计算方法
        AttributeUtils.calculateFinalAttributes(testPlayer, context);
        AttributeSet finalAttr = testPlayer.getFinalAttributes();

        // ------------------ 断言验证 ------------------
        // 验证1：验证六维力量计算是否符合六维自身的同乘区逻辑 [ 50 * 1.2 + 50 = 110 ]
        assertEquals("力量（含自身百分比加成与固定加成）计算推导不正确", 110, finalAttr.strength);
        
        // 验证2：验证力量带动基础物攻，且进入二次攻击力独立乘区运算结果 [ (100 + 60 增量) * 1.7 + 100 = 372 ]
        assertEquals("六维力量转基础面板后，百分比和固定值复合计算机制不符合预期", 372, finalAttr.physicalAtk);
    }
}
