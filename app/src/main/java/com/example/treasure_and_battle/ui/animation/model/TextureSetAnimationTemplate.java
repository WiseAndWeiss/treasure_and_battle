package com.example.treasure_and_battle.ui.animation.model;

public class TextureSetAnimationTemplate extends AnimationTemplate {

    public String position;
    public float scale;
    public int num;
    public String[] textureSet;

    public TextureSetAnimationTemplate(AnimationType type, AnimationTargetType target, int beginAt, int duration,
                                      String position, float scale, int num, String[] textureSet) {
        super(type, target, beginAt, duration);
        this.position = position;
        this.scale = scale;
        this.num = num;
        this.textureSet = textureSet;
    }
}
