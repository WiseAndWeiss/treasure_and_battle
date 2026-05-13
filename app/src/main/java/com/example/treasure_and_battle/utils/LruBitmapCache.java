package com.example.treasure_and_battle.utils;

import android.graphics.Bitmap;
import android.util.LruCache;

import androidx.annotation.Nullable;

public final class LruBitmapCache {

    private static final int MAX_CACHE_SIZE = 50;
    private static final int MAX_DIMENSION = 256;

    private static volatile LruBitmapCache instance;
    private final LruCache<String, Bitmap> cache;

    private LruBitmapCache() {
        cache = new LruCache<String, Bitmap>(MAX_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return 1;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (oldValue != null && !oldValue.isRecycled()) {
                    oldValue.recycle();
                }
            }
        };
    }

    public static LruBitmapCache getInstance() {
        if (instance == null) {
            synchronized (LruBitmapCache.class) {
                if (instance == null) {
                    instance = new LruBitmapCache();
                }
            }
        }
        return instance;
    }

    public static void releaseInstance() {
        synchronized (LruBitmapCache.class) {
            if (instance != null) {
                instance.evictAll();
                instance = null;
            }
        }
    }

    @Nullable
    public Bitmap get(String key) {
        if (key == null) return null;
        return cache.get(key);
    }

    public void put(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) return;
        if (bitmap.isRecycled()) return;

        Bitmap cached = scaleDownIfNeeded(bitmap);
        if (cached != bitmap && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        cache.put(key, cached);
    }

    public void evictAll() {
        cache.evictAll();
    }

    private Bitmap scaleDownIfNeeded(Bitmap source) {
        int w = source.getWidth();
        int h = source.getHeight();
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) {
            return source;
        }
        float scale = Math.min(
                (float) MAX_DIMENSION / w,
                (float) MAX_DIMENSION / h
        );
        int newW = Math.round(w * scale);
        int newH = Math.round(h * scale);
        return Bitmap.createScaledBitmap(source, newW, newH, true);
    }
}
