package com.example.treasure_and_battle.ui.animation;

import android.view.View;
import android.view.ViewGroup;

import com.example.treasure_and_battle.ui.animation.model.AnimationTemplate;

/**
 * 动画基类 - 管理动画的生命周期和状态
 */
public abstract class Animation {
    protected final AnimationTemplate template;
    protected final int createFrame;          // 创建帧号
    protected int currentFrame;               // 当前帧号
    protected boolean isFinished;            // 是否完成
    protected boolean isStarted;             // 是否已开始

    // 引用管理
    protected final View targetView;         // 目标实体贴图View (可能为null)
    protected final ViewGroup animationContainer; // 动画专用容器

    public Animation(AnimationTemplate template, int createFrame,
                    View targetView, ViewGroup animationContainer) {
        this.template = template;
        this.createFrame = createFrame;
        this.currentFrame = 0;
        this.isFinished = false;
        this.isStarted = false;
        this.targetView = targetView;
        this.animationContainer = animationContainer;
    }

    /**
     * 每帧更新
     * @return true表示动画已完成
     */
    public abstract boolean update();

    /**
     * 清理动画资源
     */
    public abstract void cleanup();

    /**
     * 检查动画是否应该开始
     */
    protected boolean shouldStart() {
        return currentFrame >= template.beginAt;
    }

    /**
     * 检查动画是否应该结束
     */
    protected boolean shouldEnd() {
        int lifeFrame = currentFrame - template.beginAt;
        return lifeFrame >= template.duration;
    }

    /**
     * 获取动画生命周期总帧数
     */
    public int getTotalLifecycleFrames() {
        return template.beginAt + template.duration;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public AnimationTemplate getTemplate() {
        return template;
    }
}
