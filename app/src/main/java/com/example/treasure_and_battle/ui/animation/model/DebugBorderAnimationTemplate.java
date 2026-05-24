package com.example.treasure_and_battle.ui.animation.model;

public class DebugBorderAnimationTemplate extends AnimationTemplate {
    public String color;  // 边框颜色，如 "#FF0000"

    public DebugBorderAnimationTemplate(AnimationType type, AnimationTargetType target, int beginAt, int duration,
                                       String color) {
        super(type, target, beginAt, duration);
        this.color = color;
    }
}
