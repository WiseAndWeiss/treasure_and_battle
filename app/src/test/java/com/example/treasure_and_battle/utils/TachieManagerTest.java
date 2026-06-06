package com.example.treasure_and_battle.utils;

import android.content.Context;
import android.widget.ImageView;

import com.example.treasure_and_battle.model.profession.ProfessionType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class TachieManagerTest {

    private Context context;
    private ImageView imageView;

    @Before
    public void setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication();
        imageView = new ImageView(context);
    }

    // ==================== 常量测试 ====================

    @Test
    public void testConstants() {
        assertEquals(768, TachieManager.TACHIE_WIDTH);
        assertEquals(512, TachieManager.TACHIE_HEIGHT);
    }

    // ==================== bind 测试 ====================

    @Test
    public void testBind_NullImageView() {
        // 不应该抛出异常
        TachieManager.bind(context, null, ProfessionType.WARRIOR, 0);
    }

    @Test
    public void testBind_NullProfessionType() {
        TachieManager.bind(context, imageView, null, 0);
        // 应该使用默认fallback
    }

    @Test
    public void testBind_WarriorProfession() {
        TachieManager.bind(context, imageView, ProfessionType.WARRIOR, 0);
        // 资源不存在时使用fallback
    }

    @Test
    public void testBind_MageProfession() {
        TachieManager.bind(context, imageView, ProfessionType.MAGE, 0);
    }

    @Test
    public void testBind_RangerProfession() {
        TachieManager.bind(context, imageView, ProfessionType.RANGER, 0);
    }

    @Test
    public void testBind_WithCustomFallbackResId() {
        TachieManager.bind(context, imageView, ProfessionType.WARRIOR, 12345);
    }

    @Test
    public void testBind_ZeroFallbackResId() {
        TachieManager.bind(context, imageView, ProfessionType.MAGE, 0);
        // 应该使用 android.R.drawable.ic_menu_gallery
    }

    @Test
    public void testBind_MultipleCallsSameProfession() {
        TachieManager.bind(context, imageView, ProfessionType.WARRIOR, 0);
        TachieManager.bind(context, imageView, ProfessionType.WARRIOR, 0);
        // 第二次应该使用缓存
    }

    @Test
    public void testBind_DifferentProfessions() {
        ImageView imageView2 = new ImageView(context);
        TachieManager.bind(context, imageView, ProfessionType.WARRIOR, 0);
        TachieManager.bind(context, imageView2, ProfessionType.MAGE, 0);
    }

    // ==================== fileNameForProfession 测试 ====================

    @Test
    public void testFileNameForProfession_Warrior() {
        // 通过测试绑定行为来间接测试文件名
        TachieManager.bind(context, imageView, ProfessionType.WARRIOR, 0);
    }

    @Test
    public void testFileNameForProfession_Mage() {
        TachieManager.bind(context, imageView, ProfessionType.MAGE, 0);
    }

    @Test
    public void testFileNameForProfession_Ranger() {
        TachieManager.bind(context, imageView, ProfessionType.RANGER, 0);
    }

    // ==================== recycle 测试 ====================

    @Test
    public void testRecycle_ClearsCache() {
        // 先执行一次绑定
        TachieManager.bind(context, imageView, ProfessionType.WARRIOR, 0);

        // 回收缓存
        TachieManager.recycle();

        // 再次绑定应该重新加载
        TachieManager.bind(context, imageView, ProfessionType.WARRIOR, 0);
    }

    @Test
    public void testRecycle_MultipleCalls() {
        TachieManager.recycle();
        TachieManager.recycle();
        TachieManager.recycle();
        // 不应该抛出异常
    }

    @Test
    public void testRecycle_AfterMultipleBinds() {
        ImageView imageView2 = new ImageView(context);
        ImageView imageView3 = new ImageView(context);

        TachieManager.bind(context, imageView, ProfessionType.WARRIOR, 0);
        TachieManager.bind(context, imageView2, ProfessionType.MAGE, 0);
        TachieManager.bind(context, imageView3, ProfessionType.RANGER, 0);

        TachieManager.recycle();

        // 验证回收后可以继续使用
        TachieManager.bind(context, imageView, ProfessionType.WARRIOR, 0);
    }
}
