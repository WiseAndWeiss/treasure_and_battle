package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.model.entity.Player;

public class GameManager {
    private static GameManager instance;
    private Context context;
    private Player currentPlayer;

    private GameManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized GameManager getInstance(Context context) {
        if (instance == null) {
            instance = new GameManager(context);
        }
        return instance;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    // TODO: 创建新游戏（初始化玩家、设置初始位置等）
    // TODO: 加载存档（从 SharedPreferences / 文件反序列化游戏状态）
    // TODO: 保存游戏（关键操作后自动存档）
    // TODO: 前台/后台切换处理（暂停/恢复定位和事件刷新）
    // TODO: 游戏时间管理（日夜循环、事件时效性）
}
