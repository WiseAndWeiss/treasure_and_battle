package com.example.treasure_and_battle.model;

/**
 * 敌对实体类 (Monster)
 * 继承自战斗实体基类，提供掉落物、给予经验奖励等独特的野怪属性
 */
public class Monster extends BattleEntity {
    private int expReward;     // 玩家击杀后获得的经验值
    private int goldReward;    // 玩家击杀后掉落的金币

    // TODO: 后续可以加入 DropTable (掉落池) 或者 LootTable (战利品系统)

    // 完整的状态构造函数
    public Monster(String name, int level, int maxHp, int maxMp, 
                   int patk, int matt, int pdef, int mdef, int speed, 
                   int expReward, int goldReward) {
        // 创建指定名称与等级的怪
        super(name, level);
        
        // 赋值属性
        this.maxHp = maxHp;
        this.hp = maxHp; // 满血
        this.maxMp = maxMp;
        this.mp = maxMp;

        this.physicalAttack = patk;
        this.magicAttack = matt;
        this.physicalDefense = pdef;
        this.magicDefense = mdef;
        this.speed = speed;

        this.expReward = expReward;
        this.goldReward = goldReward;
    }

    // ================= Getters and Setters =================

    public int getExpReward() { return expReward; }
    public void setExpReward(int expReward) { this.expReward = expReward; }

    public int getGoldReward() { return goldReward; }
    public void setGoldReward(int goldReward) { this.goldReward = goldReward; }
}
