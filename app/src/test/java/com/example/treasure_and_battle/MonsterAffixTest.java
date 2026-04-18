package com.example.treasure_and_battle;

import android.content.Context;

import com.example.treasure_and_battle.affix.monster.MonsterHpPercentAffix;
import com.example.treasure_and_battle.model.affix.AffixTriggerType;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.utils.AttributeUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 怪物词缀系统的单元测试
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class MonsterAffixTest {

    private Context context;
    private Monster testMonster;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;

        // 初始化测试怪物
        testMonster = new Monster(
                "monster_1",
                "Slime",
                1,
                Rarity.COMMON,
                100, // 初始最大生命值 100
                50,
                10,
                10,
                5,
                5,
                5,
                5,
                5,
                5,
                5,
                5,
                5,
                10,
                10,
                context
        );
    }

    @Test
    public void testMonsterHpPercentAffix() {
        // 验证初始状态：没有词缀时的最大生命值为 100
        AttributeUtils.calculateFinalAttributes(testMonster, context);
        AttributeSet initialFinalAttr = testMonster.getFinalAttributes();
        assertEquals("没有词缀时的最大生命值应为100", 100, initialFinalAttr.maxHp);

        // 利用配置表和 AffixManager 自动化生成的随机模板词条（类似 BuffManager的逻辑）
        com.example.treasure_and_battle.affix.BaseAffix hpAffix = com.example.treasure_and_battle.manager.AffixManager.getInstance(context).createAffixByTemplateId(2001);
        assertNotNull("hpAffix不应为null，请检查配置文件中的templateId是否对应。", hpAffix);
        
        // 挂载词缀到怪物身上 (使用完善后的 AffixManager 全局方法进行挂载和移除)
        com.example.treasure_and_battle.manager.AffixManager.getInstance(context).addAffix(testMonster, hpAffix);

        // 重新计算并获取最终属性
        AttributeUtils.calculateFinalAttributes(testMonster, context);
        AttributeSet newFinalAttr = testMonster.getFinalAttributes();

        // 验证挂载词缀后的状态：最大生命值应该为 100 * (1 + 随机生成的词缀数值)
        int expectedHp = (int) (100 * (1 + hpAffix.getAffixValue()));
        assertEquals("挂载按配置表生成的生命值词缀后，最大生命值应符合增幅期望", expectedHp, newFinalAttr.maxHp);
    }
}
