package com.example.treasure_and_battle.battle.log;

/**
 * 战斗日志类别枚举
 * 用于区分不同场景下的日志输出，方便后续在UI上按颜色、分类筛选展示
 */
public enum LogType {
    INIT,       // 初始化、遭遇战、先手判定
    ROUND_INFO, // 回合开始/回合结束等流程信息
    ACTION,     // 具体行为（普攻执行、怪物意图执行、逃跑尝试等）
    DAMAGE,     // 伤害计算、命中结算
    HEAL,       // 治疗、回复结算
    DODGE_CRIT, // 暴击、闪避、看破等概率性事件判定
    BUFF,       // Buff状态的触发、施加、移除
    AFFIX,      // 词缀效果的触发
    DEATH,      // 角色或怪物死亡
    RESULT,     // 战斗结果结算（胜利、失败、掉落、经验等）
    SYSTEM      // 系统的异常/占位提示
}