package com.example.treasure_and_battle.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 非阻塞浮动消息组件，替换 Toast。
 * 使用 WindowManager TYPE_APPLICATION_PANEL 确保显示在所有 Dialog 之上。
 */
public final class FloatMsgOverlay {

    private static final int MAX_VISIBLE = 3;
    private static final long DEFAULT_DURATION_MS = 1200;
    private static final int FLY_UP_PX = 70;
    private static final int BG_COLOR = 0xA6323232;
    private static final int TEXT_COLOR = 0xD9000000;
    private static final int CORNER_RADIUS_DP = 8;
    private static final int PADDING_DP = 12;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Queue<FloatMsgTask> pendingQueue = new ArrayDeque<>();
    private int visibleCount;
    private Context appContext;
    private WindowManager wm;

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

    public static void show(Context context, String text) {
        show(context, text, DEFAULT_DURATION_MS);
    }

    public static void show(Context context, String text, long durationMs) {
        getInstance().enqueue(context, text, durationMs);
    }

    /**
     * 全局便捷方法：与 {@link #show(Context, String)} 完全等价，名称更语义化。
     * 所有替换 Toast 的统一入口。
     */
    public static void showFloatMsg(Context context, String text) {
        show(context, text, DEFAULT_DURATION_MS);
    }

    public static void showFloatMsg(Context context, String text, long durationMs) {
        show(context, text, durationMs);
    }

    private void enqueue(Context context, String text, long durationMs) {
        handler.post(() -> {
            if (context == null || text == null || text.isEmpty()) return;
            if (appContext == null) {
                appContext = context.getApplicationContext();
                wm = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
            }

            if (visibleCount >= MAX_VISIBLE) {
                pendingQueue.add(new FloatMsgTask(text, durationMs));
                return;
            }

            showInternal(text, durationMs);
        });
    }

    private void showInternal(String text, long durationMs) {
        if (wm == null || appContext == null) return;

        float density = appContext.getResources().getDisplayMetrics().density;
        int pad = (int) (PADDING_DP * density);

        TextView tv = new TextView(appContext);
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

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        params.y = (int) (100 * density) + visibleCount * (int) (56 * density);

        visibleCount++;

        try {
            wm.addView(tv, params);
        } catch (Exception e) {
            visibleCount--;
            drainPending();
            return;
        }

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
                    try { wm.removeView(tv); } catch (Exception ignored) { }
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
