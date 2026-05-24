package com.example.treasure_and_battle.ui.animation.signal;

import java.util.List;
import java.util.Map;

/**
 * 动画信号 - 业务层向动画系统发送的事件通知
 */
public class AnimationSignal {
    public final String signalId;              // 信号ID，对应配置中的模板key
    public final String activeEntityId;        // 主动发起信号的实体ID
    public final List<String> targetEntityIds; // 目标实体ID列表
    public final Map<String, Object> params;   // 额外参数

    public AnimationSignal(String signalId, String activeEntityId,
                         List<String> targetEntityIds, Map<String, Object> params) {
        this.signalId = signalId;
        this.activeEntityId = activeEntityId;
        this.targetEntityIds = targetEntityIds != null ? targetEntityIds : new java.util.ArrayList<>();
        this.params = params != null ? params : new java.util.HashMap<>();
    }

    /**
     * 便捷构造方法 - 单体目标
     */
    public static AnimationSignal createSingleTarget(String signalId, String activeEntityId,
                                                     String targetEntityId) {
        List<String> targets = new java.util.ArrayList<>();
        targets.add(targetEntityId);
        return new AnimationSignal(signalId, activeEntityId, targets, null);
    }

    /**
     * 便捷构造方法 - 无目标（自身或全局场景）
     * 注意：即使没有目标，也传递空列表而不是null，保持参数完整性
     */
    public static AnimationSignal createNoTarget(String signalId, String activeEntityId) {
        return new AnimationSignal(signalId, activeEntityId, new java.util.ArrayList<>(), null);
    }

    /**
     * 便捷构造方法 - 多目标
     */
    public static AnimationSignal createMultiTarget(String signalId, String activeEntityId,
                                                    List<String> targetEntityIds) {
        return new AnimationSignal(signalId, activeEntityId, targetEntityIds, null);
    }

    @Override
    public String toString() {
        return "AnimationSignal{" +
                "signalId='" + signalId + '\'' +
                ", activeEntityId='" + activeEntityId + '\'' +
                ", targets=" + targetEntityIds.size() +
                ", params=" + params.size() +
                '}';
    }
}