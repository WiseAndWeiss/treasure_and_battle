package com.example.treasure_and_battle.event.handler;

import com.example.treasure_and_battle.event.EventUICallback;

/**
 * 所有事件 Handler 的抽象基类。
 *
 * 每个具体事件 Handler 继承此类，实现 {@link #build()} 方法来构建该事件的 UI。
 * 子类通过 {@link #ui} 字段回调 Activity 的 UI 操作。
 *
 * 生命周期：
 *   1. 构造（传入 EventUICallback）
 *   2. build() — Activity 调用，构建按钮和初始展示
 *   3. dispose() — Activity onDestroy 时调用，释放资源
 */
public abstract class BaseEventHandler {

    protected final EventUICallback ui;

    public BaseEventHandler(EventUICallback ui) {
        this.ui = ui;
    }

    /**
     * 构建事件的 UI（按钮和初始展示）。
     * Activity 在确定事件类型后调用此方法。
     */
    public abstract void build();

    /**
     * 释放资源。子类可重写以清理 Dialog、Adapter 等。
     * 默认空实现。
     */
    public void dispose() {
    }
}
