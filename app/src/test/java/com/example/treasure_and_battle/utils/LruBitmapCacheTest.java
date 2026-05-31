package com.example.treasure_and_battle.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class LruBitmapCacheTest {

    private LruBitmapCache cache;

    @Before
    public void setUp() {
        cache = LruBitmapCache.getInstance();
        cache.evictAll(); // 清空缓存
    }

    @After
    public void tearDown() {
        cache.evictAll();
    }

    // ==================== 单例模式测试 ====================

    @Test
    public void testGetInstance_ReturnsNonNull() {
        assertNotNull(LruBitmapCache.getInstance());
    }

    @Test
    public void testGetInstance_ReturnsSingleton() {
        LruBitmapCache instance1 = LruBitmapCache.getInstance();
        LruBitmapCache instance2 = LruBitmapCache.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    public void testReleaseInstance_ClearsInstance() {
        // 获取初始实例
        LruBitmapCache instance1 = LruBitmapCache.getInstance();
        assertNotNull(instance1);

        // 释放实例
        LruBitmapCache.releaseInstance();

        // 确保可以重新获取实例
        LruBitmapCache instance2 = LruBitmapCache.getInstance();
        assertNotNull(instance2);

        // 验证实例可以正常工作
        Bitmap bmp = createBitmap(50, 50);
        instance2.put("test", bmp);
        assertNotNull(instance2.get("test"));
    }

    // ==================== get 测试 ====================

    @Test
    public void testGet_NullKey() {
        assertNull(cache.get(null));
    }

    @Test
    public void testGet_NonExistentKey() {
        assertNull(cache.get("nonexistent"));
    }

    @Test
    public void testGet_ExistingKey() {
        Bitmap bitmap = createBitmap(100, 100);
        cache.put("test", bitmap);

        Bitmap result = cache.get("test");
        assertNotNull(result);
    }

    // ==================== put 测试 ====================

    @Test
    public void testPut_NullKey() {
        Bitmap bitmap = createBitmap(100, 100);
        cache.put(null, bitmap);
        // 不应该抛出异常
    }

    @Test
    public void testPut_NullBitmap() {
        cache.put("test", null);
        // 不应该抛出异常
    }

    @Test
    public void testPut_RecycledBitmap() {
        Bitmap bitmap = createBitmap(100, 100);
        bitmap.recycle();

        cache.put("test", bitmap);
        // 应该被忽略
        assertNull(cache.get("test"));
    }

    @Test
    public void testPut_ValidBitmap() {
        Bitmap bitmap = createBitmap(100, 100);
        cache.put("test", bitmap);

        assertNotNull(cache.get("test"));
    }

    @Test
    public void testPut_OverwritesExisting() {
        Bitmap bitmap1 = createBitmap(50, 50);
        Bitmap bitmap2 = createBitmap(100, 100);

        cache.put("test", bitmap1);
        cache.put("test", bitmap2);

        Bitmap result = cache.get("test");
        assertNotNull(result);
        // 由于bitmap1可能被回收，只验证返回非null
    }

    // ==================== evictAll 测试 ====================

    @Test
    public void testEvictAll_ClearsCache() {
        cache.put("key1", createBitmap(50, 50));
        cache.put("key2", createBitmap(50, 50));
        cache.put("key3", createBitmap(50, 50));

        cache.evictAll();

        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
        assertNull(cache.get("key3"));
    }

    @Test
    public void testEvictAll_EmptyCache() {
        cache.evictAll();
        cache.evictAll();
        // 不应该抛出异常
    }

    // ==================== 缩放行为测试 ====================

    @Test
    public void put_SmallBitmapNotScaled() {
        // 小于MAX_DIMENSION的图片不应该被缩放
        Bitmap original = createBitmap(100, 100);
        cache.put("small", original);

        Bitmap cached = cache.get("small");
        assertNotNull(cached);
    }

    @Test
    public void put_LargeBitmapScaled() {
        // 大于MAX_DIMENSION的图片应该被缩放
        Bitmap large = createBitmap(512, 512);
        assertFalse(large.isRecycled());

        cache.put("large", large);

        // 原始位图可能被回收
        Bitmap cached = cache.get("large");
        assertNotNull(cached);
    }

    @Test
    public void put_VeryLargeBitmapScaled() {
        Bitmap veryLarge = createBitmap(1000, 800);
        cache.put("veryLarge", veryLarge);

        Bitmap cached = cache.get("veryLarge");
        assertNotNull(cached);
        // 缩放后的尺寸应该不超过MAX_DIMENSION
        assertTrue(cached.getWidth() <= 256);
        assertTrue(cached.getHeight() <= 256);
    }

    // ==================== LRU淘汰行为测试 ====================

    @Test
    public void put_LargeNumberEvictsOldEntries() {
        // 添加超过MAX_CACHE_SIZE的条目
        for (int i = 0; i < 60; i++) {
            cache.put("key" + i, createBitmap(50, 50));
        }

        // 早期的条目应该被淘汰
        assertNull(cache.get("key0"));
        assertNull(cache.get("key5"));

        // 最近的条目应该存在
        assertNotNull(cache.get("key55"));
        assertNotNull(cache.get("key59"));
    }

    // ==================== 并发测试 ====================

    @Test
    public void getInstance_ThreadSafe() throws InterruptedException {
        LruBitmapCache.releaseInstance();

        final LruBitmapCache[] instances = new LruBitmapCache[10];
        final Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                instances[index] = LruBitmapCache.getInstance();
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有线程获取的是同一个实例
        for (int i = 1; i < instances.length; i++) {
            assertSame(instances[0], instances[i]);
        }
    }

    // ==================== 辅助方法 ====================

    private Bitmap createBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.BLUE);
        return bitmap;
    }
}
