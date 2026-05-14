package com.example.treasure_and_battle.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Random;

/**
 * 非阻塞伤害数字 / 治疗数字叠加层。
 * 在指定坐标出现红色 "-9 HP" 或绿色 "+5 HP" 文本，向上浮空 + 渐隐。
 */
public final class DamageNumberOverlay {

    private static final float FLOAT_DISTANCE_DP = 60f;
    private static final long DURATION_MS = 900;
    private static final int DAMAGE_COLOR = 0xFFE53935;
    private static final int HEAL_COLOR = 0xFF4CAF50;
    private static final int MP_COLOR = 0xFF42A5F5;
    private static final int AP_COLOR = 0xFFFFCA28;
    private static final int RANDOM_OFFSET_DP = 20;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FrameLayout anchor;
    private final Random random = new Random();

    public DamageNumberOverlay(FrameLayout anchor) {
        this.anchor = anchor;
    }

    public void showDamage(int xCenterPx, int yCenterPx, int amount) {
        show(xCenterPx, yCenterPx, "-" + amount + " HP", DAMAGE_COLOR);
    }

    public void showHeal(int xCenterPx, int yCenterPx, int amount) {
        show(xCenterPx, yCenterPx, "+" + amount + " HP", HEAL_COLOR);
    }

    public void showDamageOffset(int xCenterPx, int yCenterPx, int amount) {
        float density = anchor != null ? anchor.getResources().getDisplayMetrics().density : 3f;
        int ox = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        int oy = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        show(xCenterPx + ox, yCenterPx + oy, "-" + amount + " HP", DAMAGE_COLOR);
    }

    public void showHealOffset(int xCenterPx, int yCenterPx, int amount) {
        float density = anchor != null ? anchor.getResources().getDisplayMetrics().density : 3f;
        int ox = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        int oy = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        show(xCenterPx + ox, yCenterPx + oy, "+" + amount + " HP", HEAL_COLOR);
    }

    public void showMpChange(int xCenterPx, int yCenterPx, int delta) {
        float density = anchor != null ? anchor.getResources().getDisplayMetrics().density : 3f;
        int ox = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        int oy = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        String text = delta >= 0 ? "+" + delta + " MP" : delta + " MP";
        show(xCenterPx + ox, yCenterPx + oy, text, MP_COLOR);
    }

    public void showApChange(int xCenterPx, int yCenterPx, int delta) {
        float density = anchor != null ? anchor.getResources().getDisplayMetrics().density : 3f;
        int ox = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        int oy = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        String text = delta >= 0 ? "+" + delta + " AP" : delta + " AP";
        show(xCenterPx + ox, yCenterPx + oy, text, AP_COLOR);
    }

    public void show(int xCenterPx, int yCenterPx, String text, int color) {
        if (anchor == null) return;

        float density = anchor.getResources().getDisplayMetrics().density;
        float endY = -FLOAT_DISTANCE_DP * density;

        TextView tv = new TextView(anchor.getContext());
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(16);
        tv.setShadowLayer(2f * density, 0f, 1f * density, 0xAA000000);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = xCenterPx - (int) (30 * density);
        params.topMargin = yCenterPx;
        tv.setLayoutParams(params);

        anchor.addView(tv);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0f);
        ObjectAnimator transY = ObjectAnimator.ofFloat(tv, "translationY", 0f, endY);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tv, "scaleX", 1f, 0.6f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tv, "scaleY", 1f, 0.6f);

        alpha.setDuration(DURATION_MS);
        transY.setDuration(DURATION_MS);
        scaleX.setDuration(DURATION_MS);
        scaleY.setDuration(DURATION_MS);

        alpha.start();
        transY.start();
        scaleX.start();
        scaleY.start();

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
                });
            }
        });
    }
}
