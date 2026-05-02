package com.example.treasure_and_battle.model.skill;

// 技能效果的参数，具体含义由技能描述定义，由重写的触发方法实现
public class SkillEffectParams {
    int x, y, z, w;

    public SkillEffectParams(int x, int y, int z, int w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public SkillEffectParams(int[] params) {
        this.x = params[0];
        this.y = params[1];
        this.z = params[2];
        this.w = params[3];
    }

    public SkillEffectParams() {
        this(0, 0, 0, 0);
    }

    public SkillEffectParams(SkillEffectParams params) {
        this(params.x, params.y, params.z, params.w);
    }
}
