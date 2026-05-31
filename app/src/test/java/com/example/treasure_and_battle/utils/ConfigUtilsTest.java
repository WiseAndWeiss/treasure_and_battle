package com.example.treasure_and_battle.utils;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class ConfigUtilsTest {

    private Context context;

    @Before
    public void setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication();
    }

    @Test
    public void testGetInstance_ReturnsNonNull() {
        ConfigUtils instance = ConfigUtils.getInstance(context);
        assertNotNull(instance);
    }

    @Test
    public void testGetInstance_ReturnsSingleton() {
        ConfigUtils instance1 = ConfigUtils.getInstance(context);
        ConfigUtils instance2 = ConfigUtils.getInstance(context);
        assertSame(instance1, instance2);
    }

    @Test
    public void testGetInstance_ThreadSafe() throws InterruptedException {
        final ConfigUtils[] instances = new ConfigUtils[10];
        final Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                instances[index] = ConfigUtils.getInstance(context);
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

    @Test
    public void testGetInstance_WithApplicationContext() {
        ConfigUtils instance = ConfigUtils.getInstance(context);
        assertNotNull(instance);
    }

    @Test
    public void testGetInstance_MultipleCallsSameInstance() {
        ConfigUtils instance1 = ConfigUtils.getInstance(context);
        ConfigUtils instance2 = ConfigUtils.getInstance(context);
        ConfigUtils instance3 = ConfigUtils.getInstance(context);

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
    }
}
