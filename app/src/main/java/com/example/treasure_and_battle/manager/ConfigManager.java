package com.example.treasure_and_battle.manager;

import android.content.Context;

public class ConfigManager {
    private static ConfigManager instance;
    private Context context;

    private ConfigManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized ConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigManager(context);
        }
        return instance;
    }

    // TODO: 加载全局游戏配置（事件生成间隔、距离范围、最大事件数等）
    // TODO: 提供配置查询接口，供事件生成模块调用
    // TODO: 支持热更新配置文件（不需要重启应用）
}
