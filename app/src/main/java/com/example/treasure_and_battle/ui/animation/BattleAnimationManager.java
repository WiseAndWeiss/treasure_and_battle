package com.example.treasure_and_battle.ui.animation;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.treasure_and_battle.ui.animation.config.AnimationConfigLoader;
import com.example.treasure_and_battle.ui.animation.impl.DebugBorderAnimation;
import com.example.treasure_and_battle.ui.animation.impl.TextureSetAnimation;
import com.example.treasure_and_battle.ui.animation.model.AnimationTemplate;
import com.example.treasure_and_battle.ui.animation.model.AnimationTargetType;
import com.example.treasure_and_battle.ui.animation.model.AnimationType;
import com.example.treasure_and_battle.ui.animation.model.DebugBorderAnimationTemplate;
import com.example.treasure_and_battle.ui.animation.model.TextureSetAnimationTemplate;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignal;
import com.example.treasure_and_battle.ui.animation.signal.AnimationSignalPipeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 战斗动画管理器 - 60Hz统一动画更新系统
 */
public class BattleAnimationManager {
    private static final String TAG = "BattleAnimationManager";
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME_MS = 1000 / TARGET_FPS;

    private final Context context;
    private final AnimationSignalPipeline signalPipeline;
    private final Map<String, List<AnimationTemplate>> animationConfigs;

    // 动画实例列表
    private final List<Animation> activeAnimations = new ArrayList<>();

    // 帧更新控制
    private final Handler frameHandler = new Handler(Looper.getMainLooper());
    private final Runnable frameUpdateTask;
    private boolean isRunning = false;
    private int currentFrame = 0;  // 全局帧计数器

    // 实体View映射
    private final EntityViewMapper entityViewMapper;

    public BattleAnimationManager(Context context) {
        this.context = context.getApplicationContext();
        this.signalPipeline = AnimationSignalPipeline.getInstance();
        this.animationConfigs = AnimationConfigLoader.loadConfigs(context);
        this.entityViewMapper = new EntityViewMapper();
        this.frameUpdateTask = this::frameUpdate;
    }

    /**
     * 启动动画管理器
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;
            scheduleNextFrame();
            Log.i(TAG, "Animation manager started at 60fps");
        }
    }

    /**
     * 停止动画管理器
     */
    public void stop() {
        if (isRunning) {
            isRunning = false;
            frameHandler.removeCallbacks(frameUpdateTask);
            cleanupAllAnimations();
            Log.i(TAG, "Animation manager stopped");
        }
    }

    /**
     * 注册实体View映射
     */
    public void registerEntityView(String entityId, View entityView, ViewGroup container) {
        entityViewMapper.registerEntityView(entityId, entityView, container);
    }

    /**
     * 注册全局场景容器
     */
    public void registerSceneContainer(ViewGroup sceneContainer) {
        entityViewMapper.registerSceneContainer(sceneContainer);
    }

    /**
     * 取消实体View映射
     */
    public void unregisterEntityView(String entityId) {
        entityViewMapper.unregisterEntityView(entityId);
    }

    /**
     * 帧更新主循环
     */
    private void frameUpdate() {
        if (!isRunning) return;

        long startTime = System.currentTimeMillis();

        try {
            // 1. 处理信号队列
            processSignals();

            // 2. 更新活跃动画
            updateActiveAnimations();

            // 3. 清理已完成动画
            cleanupFinishedAnimations();

        } catch (Exception e) {
            Log.e(TAG, "Error in frame update", e);
        }

        currentFrame++;

        // 性能监控
        long frameTime = System.currentTimeMillis() - startTime;
        if (frameTime > FRAME_TIME_MS * 2) {
            Log.w(TAG, "Frame time: " + frameTime + "ms (target: " + FRAME_TIME_MS + "ms)");
        }

        scheduleNextFrame();
    }

    private void scheduleNextFrame() {
        if (isRunning) {
            frameHandler.postDelayed(frameUpdateTask, FRAME_TIME_MS);
        }
    }

    /**
     * 处理信号队列
     */
    private void processSignals() {
        AnimationSignal signal;
        while ((signal = signalPipeline.pollSignal()) != null) {
            createAnimationsFromSignal(signal);
        }
    }

    /**
     * 从信号创建动画
     */
    private void createAnimationsFromSignal(AnimationSignal signal) {
        List<AnimationTemplate> templates = animationConfigs.get(signal.signalId);
        if (templates == null || templates.isEmpty()) {
            Log.w(TAG, "No animation template found for signal: " + signal.signalId);
            return;
        }

        Log.d(TAG, "Processing signal: " + signal.signalId + " from " + signal.activeEntityId +
                   " with " + signal.targetEntityIds.size() + " targets");

        for (AnimationTemplate template : templates) {
            switch (template.target) {
                case OWN_ENTITY:
                    createAnimationForActiveEntity(template, signal);
                    break;
                case TARGET_ENTITY:
                    createAnimationsForTargetEntities(template, signal);
                    break;
                case ENTIRE_FIELD:
                    createAnimationForScene(template, signal);
                    break;
            }
        }
    }

    /**
     * 为主动实体创建动画
     */
    private void createAnimationForActiveEntity(AnimationTemplate template, AnimationSignal signal) {
        String entityId = signal.activeEntityId;
        Log.d(TAG, "Creating OWN_ENTITY animation for: " + entityId + " type: " + template.type);

        View entityView = entityViewMapper.getEntityView(entityId);
        ViewGroup container = entityViewMapper.getEntityContainer(entityId);

        if (entityView != null && container != null) {
            Animation animation = createAnimation(template, entityView, container);
            if (animation != null) {
                activeAnimations.add(animation);
                Log.d(TAG, "✅ Created OWN_ENTITY animation for " + entityId);
            } else {
                Log.w(TAG, "❌ Failed to create animation for " + entityId);
            }
        } else {
            Log.w(TAG, "❌ Entity view or container not found for " + entityId +
                       " (view=" + (entityView != null) + ", container=" + (container != null) + ")");
        }
    }

    /**
     * 为目标实体创建动画
     */
    private void createAnimationsForTargetEntities(AnimationTemplate template, AnimationSignal signal) {
        if (signal.targetEntityIds.isEmpty()) {
            Log.d(TAG, "TARGET_ENTITY animation requested but no targets provided, skipping");
            return;
        }

        Log.d(TAG, "Creating TARGET_ENTITY animations for " + signal.targetEntityIds.size() +
                   " targets, type: " + template.type);

        for (String targetId : signal.targetEntityIds) {
            Log.d(TAG, "🎯 Processing target: " + targetId);

            View entityView = entityViewMapper.getEntityView(targetId);
            ViewGroup container = entityViewMapper.getEntityContainer(targetId);

            Log.d(TAG, "   View lookup result: view=" + (entityView != null ? "✅" : "❌") +
                       ", container=" + (container != null ? "✅" : "❌"));

            if (entityView != null && container != null) {
                Animation animation = createAnimation(template, entityView, container);
                if (animation != null) {
                    activeAnimations.add(animation);
                    Log.d(TAG, "✅ Created TARGET_ENTITY animation for " + targetId);
                } else {
                    Log.w(TAG, "❌ Failed to create animation for target " + targetId);
                }
            } else {
                Log.w(TAG, "❌ Target view or container not found for " + targetId +
                           " (view=" + (entityView != null) + ", container=" + (container != null) + ")");
            }
        }
    }

    /**
     * 为全局场景创建动画
     */
    private void createAnimationForScene(AnimationTemplate template, AnimationSignal signal) {
        ViewGroup sceneContainer = entityViewMapper.getSceneContainer();
        if (sceneContainer != null) {
            Animation animation = createAnimation(template, null, sceneContainer);
            if (animation != null) {
                activeAnimations.add(animation);
            }
        }
    }

    /**
     * 创建具体动画实例
     */
    private Animation createAnimation(AnimationTemplate template, View targetView,
                                      ViewGroup animationContainer) {
        switch (template.type) {
            case TEXTURE_SET:
                if (template instanceof TextureSetAnimationTemplate) {
                    return new TextureSetAnimation(
                        (TextureSetAnimationTemplate) template,
                        currentFrame,
                        targetView,
                        animationContainer,
                        context
                    );
                }
                break;
            case DEBUG_BORDER:
                if (template instanceof DebugBorderAnimationTemplate) {
                    return new DebugBorderAnimation(
                        (DebugBorderAnimationTemplate) template,
                        currentFrame,
                        targetView,
                        animationContainer
                    );
                }
                break;
            // 未来可以在这里添加其他动画类型
            default:
                Log.w(TAG, "Unknown animation type: " + template.type);
                return null;
        }
        return null;
    }

    /**
     * 更新所有活跃动画
     */
    private void updateActiveAnimations() {
        for (Animation animation : activeAnimations) {
            if (!animation.isFinished()) {
                animation.update();
            }
        }
    }

    /**
     * 清理已完成的动画
     */
    private void cleanupFinishedAnimations() {
        Iterator<Animation> iterator = activeAnimations.iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next();
            if (animation.isFinished()) {
                animation.cleanup();
                iterator.remove();
            }
        }
    }

    /**
     * 清理所有动画
     */
    private void cleanupAllAnimations() {
        for (Animation animation : activeAnimations) {
            animation.cleanup();
        }
        activeAnimations.clear();
    }

    /**
     * 获取当前活跃动画数量
     */
    public int getActiveAnimationCount() {
        return activeAnimations.size();
    }

    /**
     * 销毁动画管理器
     */
    public void destroy() {
        stop();
        entityViewMapper.clear();
        AnimationConfigLoader.clearCache();
        Log.i(TAG, "Animation manager destroyed");
    }
}