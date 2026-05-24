package com.example.treasure_and_battle.ui.animation.impl;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.treasure_and_battle.ui.animation.Animation;
import com.example.treasure_and_battle.ui.animation.model.DebugBorderAnimationTemplate;

/**
 * 调试边框动画 - 在目标View上显示彩色边框用于调试
 */
public class DebugBorderAnimation extends Animation {

    private final DebugBorderAnimationTemplate debugTemplate;
    private final View debugOverlay;
    private final ViewGroup parent;
    private final int targetIndex;
    private Drawable originalBackground;
    private boolean hasShownBorder = false;

    public DebugBorderAnimation(DebugBorderAnimationTemplate template, int createFrame,
                               View targetView, ViewGroup animationContainer) {
        super(template, createFrame, targetView, animationContainer);

        this.debugTemplate = template;
        this.parent = (ViewGroup) targetView.getParent();
        this.targetIndex = parent.indexOfChild(targetView);

        // 创建调试覆盖层
        this.debugOverlay = createDebugOverlay(targetView);
    }

    private View createDebugOverlay(View targetView) {
        FrameLayout overlay = new FrameLayout(animationContainer.getContext());

        // 复制目标View的尺寸
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
            targetView.getLayoutParams().width,
            targetView.getLayoutParams().height
        );
        overlay.setLayoutParams(params);

        // 设置半透明边框背景
        String borderColor = debugTemplate.color;
        int borderColorInt = parseColor(borderColor);

        // 创建带边框的背景
        ColorDrawable borderDrawable = new ColorDrawable(borderColorInt);
        overlay.setBackground(borderDrawable);

        // 设置透明度使边框不那么显眼
        overlay.setAlpha(0.3f);

        return overlay;
    }

    private int parseColor(String colorString) {
        try {
            return Color.parseColor(colorString);
        } catch (IllegalArgumentException e) {
            return Color.RED;  // 默认红色
        }
    }

    @Override
    public boolean update() {
        if (!isStarted) {
            if (currentFrame >= template.beginAt) {
                startAnimation();
            } else {
                currentFrame++;
                return false;
            }
        }

        // 检查是否完成
        if (currentFrame >= template.beginAt + template.duration) {
            isFinished = true;
            return true;  // 动画完成
        } else {
            currentFrame++;
            return false; // 动画仍在进行
        }
    }

    private void startAnimation() {
        if (!hasShownBorder && debugOverlay != null && targetView != null) {
            // 保存原始背景
            if (targetView.getBackground() != null) {
                originalBackground = targetView.getBackground().getConstantState().newDrawable();
            }

            // 设置调试边框
            String borderColor = debugTemplate.color;
            int borderColorInt = parseColor(borderColor);

            ColorDrawable borderDrawable = new ColorDrawable(borderColorInt);
            targetView.setBackground(borderDrawable);

            hasShownBorder = true;
            isStarted = true;
            android.util.Log.d("DebugBorderAnimation", "Started border animation: " + debugTemplate.color + " on entity: " + targetView.toString());
        }
    }

    @Override
    public void cleanup() {
        if (hasShownBorder && targetView != null) {
            // 恢复原始背景
            if (originalBackground != null) {
                targetView.setBackground(originalBackground);
            } else {
                targetView.setBackground(null);
            }

            android.util.Log.d("DebugBorderAnimation", "Cleaned up border animation");
            hasShownBorder = false;
        }
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }
}