package com.example.treasure_and_battle.ui.animation.signal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 动画信号管道 - 业务层和动画系统之间的通信桥梁
 */
public class AnimationSignalPipeline {
    private static volatile AnimationSignalPipeline instance;
    private final BlockingQueue<AnimationSignal> signalQueue;

    private AnimationSignalPipeline() {
        this.signalQueue = new LinkedBlockingQueue<>();
    }

    public static AnimationSignalPipeline getInstance() {
        if (instance == null) {
            synchronized (AnimationSignalPipeline.class) {
                if (instance == null) {
                    instance = new AnimationSignalPipeline();
                }
            }
        }
        return instance;
    }

    /**
     * 发送信号到管道
     */
    public void emitSignal(AnimationSignal signal) {
        if (signal != null) {
            signalQueue.offer(signal); // 非阻塞添加
        }
    }

    /**
     * 获取待处理的信号（非阻塞）
     * @return 待处理的信号，如果没有则返回null
     */
    public AnimationSignal pollSignal() {
        return signalQueue.poll();
    }

    /**
     * 获取待处理的信号（阻塞）
     * @return 待处理的信号
     */
    public AnimationSignal takeSignal() throws InterruptedException {
        return signalQueue.take();
    }

    /**
     * 获取当前队列中的信号数量
     */
    public int getPendingSignalCount() {
        return signalQueue.size();
    }

    /**
     * 清空管道
     */
    public void clear() {
        signalQueue.clear();
    }

    /**
     * 销毁管道
     */
    public static void destroy() {
        instance = null;
    }
}