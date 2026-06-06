package com.example.treasure_and_battle;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.amap.api.location.AMapLocationClient;

/**
 * 自定义 Application，在 AMap SDK 初始化之前注入用户配置的 API Key。
 */
public class TreasureApp extends Application {

    private static final String PREFS_NAME = "GameSettings";
    private static final String KEY_AMAP_API_KEY = "amap_api_key";

    @Override
    public void onCreate() {
        super.onCreate();
        applyApiKey(this);
    }

    /**
     * 从 SharedPreferences 读取用户自定义 Key 并注入高德 SDK。
     * 必须在任何 MapView / AMapLocationClient 创建之前调用。
     */
    public static void applyApiKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userKey = prefs.getString(KEY_AMAP_API_KEY, "");
        if (!TextUtils.isEmpty(userKey)) {
            try {
                AMapLocationClient.setApiKey(userKey);
            } catch (Exception e) {
                // 忽略设置失败，回退到 manifest 默认 Key
            }
        }
    }

    /** 查询用户是否已配置过自定义 Key */
    public static boolean isKeyConfigured(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return !TextUtils.isEmpty(prefs.getString(KEY_AMAP_API_KEY, ""));
    }

    /** 保存用户自定义 Key */
    public static void saveApiKey(Context context, String key) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_AMAP_API_KEY, key)
                .apply();
    }

    /** 获取已保存的 Key（可能为空） */
    public static String getSavedApiKey(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_AMAP_API_KEY, "");
    }
}
