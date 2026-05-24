package com.example.treasure_and_battle.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Random;

/**
 * 非阻塞伤害数字 / 治疗数字叠加层。
 * 在指定坐标出现红色 "-9 HP" 或绿色 "+5 HP" 文本，向上浮空后移除。
 */
public final class DamageNumberOverlay {

    private static final float FLOAT_DISTANCE_DP = 60f;
    private static final long DURATION_MS = 900;
    private static final int DAMAGE_COLOR = 0xFFE53935;
    private static final int HEAL_COLOR = 0xFF4CAF50;
    private static final int MP_COLOR = 0xFF42A5F5;
    private static final int AP_COLOR = 0xFFFFCA28;
    private static final int MISS_COLOR = 0xFFFFFFFF;
    private static final int CRIT_LABEL_COLOR = 0xFFFFD54F;
    private static final int AFFIX_TRIGGER_COLOR = 0xFFFFFFFF;
    private static final int BUFF_TRIGGER_COLOR = 0xFF8BC34A;
    private static final int SKILL_TRIGGER_COLOR = 0xFF42A5F5;
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

    /** 未命中 / 闪避（浮字样式与伤害数字一致，文案为 MISS） */
    public void showMissOffset(int xCenterPx, int yCenterPx) {
        float density = anchor != null ? anchor.getResources().getDisplayMetrics().density : 3f;
        int ox = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        int oy = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        showMiss(xCenterPx + ox, yCenterPx + oy);
    }

    public void showMiss(int xCenterPx, int yCenterPx) {
        show(xCenterPx, yCenterPx, "MISS", MISS_COLOR, 18f);
    }

    /** 暴击伤害：单行「暴击! -x HP」，伤害部分与普通扣血格式一致 */
    public void showCritDamageOffset(int xCenterPx, int yCenterPx, int amount) {
        float density = anchor != null ? anchor.getResources().getDisplayMetrics().density : 3f;
        int ox = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        int oy = random.nextInt((int) (RANDOM_OFFSET_DP * density)) - (int) (RANDOM_OFFSET_DP * density / 2);
        showCritDamage(xCenterPx + ox, yCenterPx + oy, amount);
    }

    public void showCritDamage(int xCenterPx, int yCenterPx, int amount) {
        if (anchor == null) {
            return;
        }
        float density = anchor.getResources().getDisplayMetrics().density;
        float endY = -FLOAT_DISTANCE_DP * density;

        String prefix = "暴击! ";
        String damagePart = "-" + amount + " HP";
        String full = prefix + damagePart;
        SpannableString text = new SpannableString(full);
        text.setSpan(new ForegroundColorSpan(CRIT_LABEL_COLOR), 0, prefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new StyleSpan(Typeface.BOLD), 0, prefix.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new ForegroundColorSpan(DAMAGE_COLOR), prefix.length(), full.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView tv = new TextView(anchor.getContext());
        tv.setText(text);
        tv.setTextSize(16);
        tv.setShadowLayer(2f * density, 0f, 1f * density, 0xAA000000);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = xCenterPx - (int) (30 * density);
        params.topMargin = yCenterPx;
        tv.setLayoutParams(params);

        anchor.addView(tv);
        tv.setScaleX(1.35f);
        tv.setScaleY(1.35f);

        ObjectAnimator popX = ObjectAnimator.ofFloat(tv, "scaleX", 1.35f, 1f);
        ObjectAnimator popY = ObjectAnimator.ofFloat(tv, "scaleY", 1.35f, 1f);
        popX.setDuration(120);
        popY.setDuration(120);
        popX.start();
        popY.start();

        ObjectAnimator transY = ObjectAnimator.ofFloat(tv, "translationY", 0f, endY);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tv, "scaleX", 1f, 0.6f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tv, "scaleY", 1f, 0.6f);
        transY.setDuration(DURATION_MS);
        scaleX.setDuration(DURATION_MS);
        scaleY.setDuration(DURATION_MS);
        transY.setStartDelay(120);
        scaleX.setStartDelay(120);
        scaleY.setStartDelay(120);
        transY.start();
        scaleX.start();
        scaleY.start();
        attachRemoveOnEnd(transY, tv);
    }

    public void show(int xCenterPx, int yCenterPx, String text, int color) {
        show(xCenterPx, yCenterPx, text, color, 16f);
    }

    public void showAffixTrigger(int xCenterPx, int yCenterPx, String affixName) {
        show(xCenterPx, yCenterPx, "触发词缀 " + affixName, AFFIX_TRIGGER_COLOR, 13f);
    }

    public void showBuffTrigger(int xCenterPx, int yCenterPx, String buffName) {
        show(xCenterPx, yCenterPx, "触发Buff " + buffName, BUFF_TRIGGER_COLOR, 13f);
    }

    public void showSkillTrigger(int xCenterPx, int yCenterPx, String skillName) {
        show(xCenterPx, yCenterPx, "触发技能 " + skillName, SKILL_TRIGGER_COLOR, 13f);
    }

    public void showTriggerText(int xCenterPx, int yCenterPx, String fullText, int color) {
        show(xCenterPx, yCenterPx, fullText, color, 13f);
    }

    private void show(int xCenterPx, int yCenterPx, String text, int color, float textSizeSp) {
        if (anchor == null) return;

        float density = anchor.getResources().getDisplayMetrics().density;
        float endY = -FLOAT_DISTANCE_DP * density;

        TextView tv = new TextView(anchor.getContext());
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(textSizeSp);
        tv.setShadowLayer(2f * density, 0f, 1f * density, 0xAA000000);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = xCenterPx - (int) (30 * density);
        params.topMargin = yCenterPx;
        tv.setLayoutParams(params);

        anchor.addView(tv);

        ObjectAnimator transY = ObjectAnimator.ofFloat(tv, "translationY", 0f, endY);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tv, "scaleX", 1f, 0.6f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tv, "scaleY", 1f, 0.6f);

        transY.setDuration(DURATION_MS);
        scaleX.setDuration(DURATION_MS);
        scaleY.setDuration(DURATION_MS);

        transY.start();
        scaleX.start();
        scaleY.start();

        attachRemoveOnEnd(transY, tv);
    }

    private void attachRemoveOnEnd(ObjectAnimator animator, TextView tv) {
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean removed;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (removed) {
                    return;
                }
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
