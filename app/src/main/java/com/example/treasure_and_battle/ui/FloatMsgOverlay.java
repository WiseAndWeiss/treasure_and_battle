package com.example.treasure_and_battle.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 非阻塞浮动消息组件，替换 Toast。
 * 消息添加到 Activity DecorView 顶部，带向上飘动动画，最多 3 条并发。
 */
public final class FloatMsgOverlay {

    private static final int MAX_VISIBLE = 999; // 不限制数量，改为排队显示
    private static final long DEFAULT_DURATION_MS = 1200;
    private static final int FLY_UP_PX = 70;
    private static final int BG_COLOR = 0xA6323232;
    private static final int TEXT_COLOR = 0xFFFFFFFF; 
    private static final int CORNER_RADIUS_DP = 8;
    private static final int PADDING_DP = 12;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Queue<FloatMsgTask> pendingQueue = new ArrayDeque<>();
    private int visibleCount;
    private FrameLayout currentAnchor;
    private Activity currentActivity;

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

    public static void showFloatMsg(Context context, String text) {
        showFloatMsg(context, text, DEFAULT_DURATION_MS);
    }

    public static void showFloatMsg(Context context, String text, long durationMs) {
        getInstance().enqueue(context, text, durationMs);
    }

    public static void show(Context context, String text) {
        showFloatMsg(context, text, DEFAULT_DURATION_MS);
    }

    public static void show(Context context, String text, long durationMs) {
        showFloatMsg(context, text, durationMs);
    }

    private void enqueue(Context context, String text, long durationMs) {
        handler.post(() -> {
            if (context == null || text == null || text.isEmpty()) return;

            if (visibleCount >= MAX_VISIBLE) {
                pendingQueue.add(new FloatMsgTask(text, durationMs));
                return;
            }

            Activity activity = resolveActivity(context);
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

            FrameLayout anchor = getDecorAnchor(activity);
            if (anchor == null) return;
            currentAnchor = anchor;
            currentActivity = activity;

            showInternal(anchor, text, durationMs);
        });
    }

    private Activity resolveActivity(Context context) {
        if (context instanceof Activity) return (Activity) context;
        if (context instanceof ContextWrapper) {
            Context base = ((ContextWrapper) context).getBaseContext();
            return resolveActivity(base);
        }
        return null;
    }

    private FrameLayout getDecorAnchor(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        if (decor instanceof FrameLayout) return (FrameLayout) decor;
        return null;
    }

    private void showInternal(FrameLayout anchor, String text, long durationMs) {
        float density = anchor.getResources().getDisplayMetrics().density;
        int pad = (int) (PADDING_DP * density);

        TextView tv = new TextView(anchor.getContext());
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
        params.topMargin = (int) (100 * density) + visibleCount * (int) (56 * density);
        tv.setLayoutParams(params);

        anchor.addView(tv);
        tv.bringToFront();
        visibleCount++;

        ObjectAnimator transY = ObjectAnimator.ofFloat(tv, "translationY", 0f, -FLY_UP_PX * density);
        transY.setDuration(durationMs);
        transY.start();

        transY.addListener(new AnimatorListenerAdapter() {
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
                    drainPending(anchor);
                });
            }
        });
    }

    private void drainPending(FrameLayout anchor) {
        if (pendingQueue.isEmpty()) return;
        if (currentActivity == null || currentActivity.isFinishing() || currentActivity.isDestroyed()) {
            pendingQueue.clear();
            return;
        }
        FloatMsgTask next = pendingQueue.poll();
        if (next != null) {
            showInternal(anchor, next.text, next.durationMs);
        }
    }
}
