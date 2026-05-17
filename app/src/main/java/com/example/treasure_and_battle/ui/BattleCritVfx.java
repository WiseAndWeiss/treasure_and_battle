package com.example.treasure_and_battle.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * 战斗暴击视觉特效：受击目标短暂放大回弹。
 */
public final class BattleCritVfx {

    private BattleCritVfx() {}

    public static void playHitPulse(@Nullable View target) {
        if (target == null) {
            return;
        }
        float rawScaleX = target.getScaleX();
        float rawScaleY = target.getScaleY();
        final float baseX = rawScaleX == 0f ? 1f : rawScaleX;
        final float baseY = rawScaleY == 0f ? 1f : rawScaleY;
        final float peakX = baseX * 1.14f;
        final float peakY = baseY * 1.14f;
        target.animate().cancel();
        ObjectAnimator expandX = ObjectAnimator.ofFloat(target, View.SCALE_X, baseX, peakX);
        ObjectAnimator expandY = ObjectAnimator.ofFloat(target, View.SCALE_Y, baseY, peakY);
        expandX.setDuration(90);
        expandY.setDuration(90);
        expandX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator shrinkX = ObjectAnimator.ofFloat(target, View.SCALE_X, peakX, baseX);
                ObjectAnimator shrinkY = ObjectAnimator.ofFloat(target, View.SCALE_Y, peakY, baseY);
                shrinkX.setDuration(160);
                shrinkY.setDuration(160);
                shrinkX.start();
                shrinkY.start();
            }
        });
        expandX.start();
        expandY.start();
    }
}
