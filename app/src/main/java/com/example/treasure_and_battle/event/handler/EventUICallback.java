package com.example.treasure_and_battle.event.handler;

import android.content.Context;
import android.view.View;

import com.example.treasure_and_battle.character.Character;

/**
 * Handler 与 Activity 之间的 UI 回调桥梁。
 * 所有事件 Handler 通过此接口操作 UI，不直接持有 Activity 引用。
 */
public interface EventUICallback {

    /** 显示结果文本（隐藏按钮区，显示结果区） */
    void showResult(String text);

    /** 将底部按钮切换为"前进"模式 */
    void switchToForwardButton();

    /** 将底部按钮切换为"被迫进入战斗"模式 */
    void switchToBattleButton();

    /** 关闭当前事件 Activity */
    void finishEvent();

    /** 发起战斗（设置结果码并关闭 Activity） */
    void launchBattle();

    /** 添加一个操作按钮到按钮区，返回创建的 View */
    View addButton(String text, int bgColor, View.OnClickListener listener);

    /** 清空按钮区所有 View */
    void clearButtons();

    /** 在按钮区添加一段只读信息文本，返回创建的 View */
    View addInfoText(String text);

    /** 获取 Context（供 Handler 调用各种 Manager） */
    Context getContext();

    /** 获取当前玩家角色 */
    Character getCharacter();
}
