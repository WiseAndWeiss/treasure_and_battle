package com.example.treasure_and_battle.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 非阻塞浮动消息组件，替换 Toast。
 * 显示在 Activity decor view 顶部 25% 位置，带向上飘动 + 渐隐动画，支持最多 3 条并发。
 */
public final class FloatMsgOverlay {

    private static final int MAX_VISIBLE = 3;
    private static final int MARGIN_TOP_PERCENT = 25;
    private static final long DEFAULT_DURATION_MS = 1200;
    private static final int FLY_UP_PX = 70;
    private static final int BG_COLOR = 0xA6323232;
    private static final int TEXT_COLOR = 0xD9000000;
    private static final int CORNER_RADIUS_DP = 8;
    private static final int PADDING_DP = 12;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Queue<FloatMsgTask> pendingQueue = new ArrayDeque<>();
    private int visibleCount;
    @Nullable
    private FrameLayout anchor;

    private static final class FloatMsgTask {
        final String text;
        final long durationMs;

        FloatMsgTask(String text, long durationMs) {
            this.text = text;
            this.durationMs = durationMs;
        }
    }

    private static final class Holder {
        static final FloatMsgOverlay INSTANCE = new FloatMsgOverlay();
    }

    public static FloatMsgOverlay getInstance() {
        return Holder.INSTANCE;
    }

    public static void show(Activity activity, String text) {
        show(activity, text, DEFAULT_DURATION_MS);
    }

    public static void show(Activity activity, String text, long durationMs) {
        getInstance().enqueue(activity, text, durationMs);
    }

    private void enqueue(Activity activity, String text, long durationMs) {
        handler.post(() -> {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
            if (text == null || text.isEmpty()) return;

            anchor = ensureAnchor(activity);

            if (visibleCount >= MAX_VISIBLE) {
                pendingQueue.add(new FloatMsgTask(text, durationMs));
                return;
            }

            showInternal(text, durationMs);
        });
    }

    private FrameLayout ensureAnchor(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        if (decor instanceof FrameLayout) {
            return (FrameLayout) decor;
        }
        if (anchor != null && anchor.getParent() != null) {
            return anchor;
        }
        return null;
    }

    private void showInternal(String text, long durationMs) {
        if (anchor == null) return;

        TextView tv = new TextView(anchor.getContext());
        float density = anchor.getResources().getDisplayMetrics().density;
        int pad = (int) (PADDING_DP * density);
        tv.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(CORNER_RADIUS_DP * density);
        bg.setColor(BG_COLOR);
        tv.setBackground(bg);
        tv.setText(text);
        tv.setTextColor(TEXT_COLOR);
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        tv.setMaxLines(2);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        params.topMargin = (int) (anchor.getHeight() * MARGIN_TOP_PERCENT / 100f) + visibleCount * (int) (56 * density);
        if (params.topMargin < (int) (80 * density)) {
            params.topMargin = (int) (80 * density) + visibleCount * (int) (56 * density);
        }
        tv.setLayoutParams(params);

        anchor.addView(tv);
        visibleCount++;

        ObjectAnimator alpha = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0f);
        ObjectAnimator transY = ObjectAnimator.ofFloat(tv, "translationY", 0f, -FLY_UP_PX * density);
        alpha.setDuration(durationMs);
        transY.setDuration(durationMs);

        alpha.start();
        transY.start();

        alpha.addListener(new AnimatorListenerAdapter() {
            private boolean removed;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (removed) return;
                removed = true;
                handler.post(() -> {
                    if (tv.getParent() != null) {
                        ((ViewGroup) tv.getParent()).removeView(tv);
                    }
                    visibleCount--;
                    drainPending();
                });
            }
        });
    }

    private void drainPending() {
        if (pendingQueue.isEmpty()) return;
        FloatMsgTask next = pendingQueue.poll();
        if (next != null) {
            showInternal(next.text, next.durationMs);
        }
    }
}
