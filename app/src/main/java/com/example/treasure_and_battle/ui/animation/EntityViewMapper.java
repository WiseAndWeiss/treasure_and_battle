package com.example.treasure_and_battle.ui.animation;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * 实体View映射管理器 - 管理实体ID到View的映射关系
 */
public class EntityViewMapper {
    private static final String TAG = "EntityViewMapper";

    // 实体ID -> 实体View
    private final Map<String, View> entityViewMap = new HashMap<>();

    // 实体ID -> 动画容器 (为每个实体创建专用动画容器)
    private final Map<String, ViewGroup> entityContainerMap = new HashMap<>();

    // 全局场景容器
    private ViewGroup sceneContainer;

    /**
     * 注册实体View
     */
    public void registerEntityView(String entityId, View entityView, ViewGroup parentContainer) {
        if (entityId == null || entityView == null) {
            return;
        }

        entityViewMap.put(entityId, entityView);

        // 为每个实体创建专用动画容器
        ViewGroup animationContainer = createAnimationContainer(parentContainer);
        entityContainerMap.put(entityId, animationContainer);

        Log.d(TAG, "Registered entity: " + entityId);
    }

    /**
     * 注册全局场景容器
     */
    public void registerSceneContainer(ViewGroup container) {
        this.sceneContainer = container;
        Log.d(TAG, "Registered scene container");
    }

    /**
     * 取消实体View注册
     */
    public void unregisterEntityView(String entityId) {
        entityViewMap.remove(entityId);

        ViewGroup container = entityContainerMap.remove(entityId);
        if (container != null && container.getParent() != null) {
            ((ViewGroup) container.getParent()).removeView(container);
        }

        Log.d(TAG, "Unregistered entity: " + entityId);
    }

    /**
     * 获取实体View
     */
    public View getEntityView(String entityId) {
        return entityViewMap.get(entityId);
    }

    /**
     * 获取实体动画容器
     */
    public ViewGroup getEntityContainer(String entityId) {
        return entityContainerMap.get(entityId);
    }

    /**
     * 获取场景容器
     */
    public ViewGroup getSceneContainer() {
        return sceneContainer;
    }

    /**
     * 创建动画容器
     */
    private ViewGroup createAnimationContainer(ViewGroup parentContainer) {
        FrameLayout container = new FrameLayout(parentContainer.getContext());

        // 设置为充满父容器
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        container.setLayoutParams(params);

        // 关键：禁用裁剪，允许动画超出边界
        container.setClipChildren(false);
        container.setClipToPadding(false);

        // 添加到父容器（不覆盖现有内容）
        if (parentContainer instanceof FrameLayout) {
            parentContainer.addView(container);
        } else {
            // 如果不是FrameLayout，添加到末尾
            parentContainer.addView(container, 0);
        }

        return container;
    }

    /**
     * 清理所有映射
     */
    public void clear() {
        entityViewMap.clear();

        for (ViewGroup container : entityContainerMap.values()) {
            if (container != null && container.getParent() != null) {
                ((ViewGroup) container.getParent()).removeView(container);
            }
        }
        entityContainerMap.clear();

        sceneContainer = null;

        Log.d(TAG, "Cleared all mappings");
    }
}