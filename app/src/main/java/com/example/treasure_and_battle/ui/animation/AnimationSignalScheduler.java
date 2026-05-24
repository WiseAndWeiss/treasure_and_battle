package com.example.treasure_and_battle.ui.animation;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.treasure_and_battle.ui.animation.signal.AnimationSignal;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignalPipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 动画信号调度器 - 用于非阻塞地延迟发送动画信号
 * 解决多次攻击技能中动画重叠显示的问题
 */
public class AnimationSignalScheduler {
    private static final String TAG = "AnimationSignalScheduler";
    private static final int FRAME_DELAY_MS = 167; // 约10帧的时间 (10/60秒 ≈ 0.167秒)

    private static volatile AnimationSignalScheduler instance;
    private final Handler handler;
    private final List<ScheduledSignal> scheduledSignals;

    private AnimationSignalScheduler() {
        this.handler = new Handler(Looper.getMainLooper());
        this.scheduledSignals = new ArrayList<>();
    }

    public static AnimationSignalScheduler getInstance() {
        if (instance == null) {
            synchronized (AnimationSignalScheduler.class) {
                if (instance == null) {
                    instance = new AnimationSignalScheduler();
                }
            }
        }
        return instance;
    }

    /**
     * 调度一系列动画信号，每个信号之间延迟指定帧数
     * @param signals 信号列表
     * @param framesBetweenSignals 信号之间的帧数延迟
     */
    public void scheduleSignals(List<AnimationSignal> signals, int framesBetweenSignals) {
        if (signals == null || signals.isEmpty()) {
            return;
        }

        long delayMs = framesBetweenSignals * FRAME_DELAY_MS;
        Log.d(TAG, "Scheduling " + signals.size() + " signals with " + delayMs + "ms delay between each");

        for (int i = 0; i < signals.size(); i++) {
            long totalDelayMs = i * delayMs;
            AnimationSignal signal = signals.get(i);

            ScheduledSignal scheduled = new ScheduledSignal(signal, totalDelayMs);
            scheduledSignals.add(scheduled);

            handler.postDelayed(() -> {
                AnimationSignalPipeline.getInstance().emitSignal(signal);
                Log.d(TAG, "Emitted delayed signal: " + signal.signalId + " (delay: " + totalDelayMs + "ms)");
            }, totalDelayMs);
        }
    }

    /**
     * 便捷方法：为多个目标调度相同的信号
     * @param signalId 信号ID
     * @param activeEntityId 主动实体ID
     * @param targetEntityIds 目标实体ID列表
     * @param framesBetweenSignals 信号之间的帧数延迟
     */
    public void scheduleSignalForTargets(String signalId, String activeEntityId,
                                         List<String> targetEntityIds, int framesBetweenSignals) {
        List<AnimationSignal> signals = new ArrayList<>();

        for (String targetId : targetEntityIds) {
            List<String> singleTarget = new ArrayList<>();
            singleTarget.add(targetId);

            AnimationSignal signal = new AnimationSignal(signalId, activeEntityId, singleTarget, null);
            signals.add(signal);
        }

        scheduleSignals(signals, framesBetweenSignals);
    }

    /**
     * 清除所有已调度的信号
     */
    public void clear() {
        handler.removeCallbacksAndMessages(null);
        scheduledSignals.clear();
        Log.d(TAG, "Cleared all scheduled signals");
    }

    /**
     * 销毁调度器
     */
    public static void destroy() {
        if (instance != null) {
            instance.clear();
            instance = null;
        }
    }

    private static class ScheduledSignal {
        final AnimationSignal signal;
        final long delayMs;

        ScheduledSignal(AnimationSignal signal, long delayMs) {
            this.signal = signal;
            this.delayMs = delayMs;
        }
    }
}