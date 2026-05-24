package com.example.treasure_and_battle.ui.animation.model;

public class AnimationTemplate {
    public AnimationType type;
    public AnimationTargetType target;
    public int beginAt;
    public int duration;

    public AnimationTemplate(AnimationType type, AnimationTargetType target, int beginAt, int duration) {
        this.type = type;
        this.target = target;
        this.beginAt = beginAt;
        this.duration = duration;
    }
}
