package com.example.treasure_and_battle.utils;

import android.content.Context;

public class ConfigUtils {
    private static ConfigUtils instance;
    private Context context;

    private ConfigUtils(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized ConfigUtils getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigUtils(context);
        }
        return instance;
    }

    // TODO: 通用配置校验工具（检查JSON配置完整性、必填字段）
    // TODO: 配置热更新支持
    // TODO: 配置版本兼容性检查
}
