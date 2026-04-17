package com.example.treasure_and_battle.model;

/**
 * 玩家类 (Player)
 * 继承自战斗实体基类，加入了经验值(EXP)、升级机制以及玩家特有的状态（比如装备面板预留）。
 */
public class Player extends BattleEntity {
    private int currentExp;
    private int expToNextLevel;

    // 职业类引用 (取代原有的 enum)
    private Profession profession;

    // 额外收益属性
    private int lootRarityBonus;      // 战利品稀有度加成
    private double goldBonusRate;     // 金币获取倍率 (基础1.0)
    private double expBonusRate;      // 经验获取倍率 (基础1.0)

    // 成长专属点数
    private int talentPoints;         // 天赋点
    private int skillPoints;          // 技能点

    public Player(String name, Profession profession) {
        // 新建玩家默认1级
        super(name, 1);
        this.currentExp = 0;
        this.expToNextLevel = 100; // 升到2级需要100经验
        this.talentPoints = 0;
        this.skillPoints = 0;
        
        // 收益默认倍率
        this.lootRarityBonus = 0;
        this.goldBonusRate = 1.0;
        this.expBonusRate = 1.0;

        // 根据传入的职业对象，初始化面板
        setProfession(profession);
    }

    /**
     * 玩家选择或重置职业，刷新底子六维属性及衍生基础属性面板
     */
    public void setProfession(Profession p) {
        this.profession = p;
        if (p == null) return;

        // 统一设所有职业的基础初始默认底子 (相当于1级白板人类)
        this.maxHp = 20;
        this.maxMp = 10;
        this.physicalAttack = 2;
        this.magicAttack = 2;
        this.physicalDefense = 1;
        this.magicDefense = 1;
        this.speed = 10;

        this.strength = 0;
        this.agility = 0;
        this.intelligence = 0;
        this.spirit = 0;
        this.physique = 0;
        this.luck = 0;

        // 让职业自身去修改玩家相应的核心与附加属性倾向
        p.applyInitialStats(this);
        
        this.hp = this.maxHp;
        this.mp = this.maxMp;
    }

    /**
     * 获取经验值并处理升级逻辑 (支持经验获取加成)
     */
    public void gainExp(int expAmount) {
        int finalExp = (int)(expAmount * this.expBonusRate);
        this.currentExp += finalExp;
        // 如果当前经验大于等于升级所需经验，触发连续升级机制
        while (this.currentExp >= this.expToNextLevel) {
            levelUp();
        }
    }

    /**
     * 玩家升级：提升等级，扣除所需经验，提升基础属性并回满状态
     */
    private void levelUp() {
        this.currentExp -= this.expToNextLevel;
        this.level++;
        
        // 每升1级，固定获得2点天赋点 + 1点技能点
        this.talentPoints += 2;
        this.skillPoints += 1;
        
        // TODO: 简单的经验曲线：每级所需经验为上一级的 1.2 倍 后续需要替换为更合理的曲线（例如指数增长或分段函数）
        this.expToNextLevel = (int) (this.expToNextLevel * 1.2);

        // 调用对应职业特有的成长逻辑，覆盖之前死板的均衡加点
        if (this.profession != null) {
            this.profession.applyLevelUpGrowth(this);
        }

        // 升级时状态回满
        this.hp = this.maxHp;
        this.mp = this.maxMp;
    }

    // ================= Getters and Setters =================

    public int getCurrentExp() { return currentExp; }
    public void setCurrentExp(int currentExp) { this.currentExp = currentExp; }

    public int getExpToNextLevel() { return expToNextLevel; }
    public void setExpToNextLevel(int expToNextLevel) { this.expToNextLevel = expToNextLevel; }
    
    // ====== 新增属性 Getters & Setters ======
    public Profession getProfession() { return profession; }

    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }

    public int getAgility() { return agility; }
    public void setAgility(int agility) { this.agility = agility; }

    public int getIntelligence() { return intelligence; }
    public void setIntelligence(int intelligence) { this.intelligence = intelligence; }

    public int getSpirit() { return spirit; }
    public void setSpirit(int spirit) { this.spirit = spirit; }

    public int getPhysique() { return physique; }
    public void setPhysique(int physique) { this.physique = physique; }

    public int getLuck() { return luck; }
    public void setLuck(int luck) { this.luck = luck; }

    public int getLootRarityBonus() { return lootRarityBonus; }
    public void setLootRarityBonus(int lootRarityBonus) { this.lootRarityBonus = lootRarityBonus; }

    public double getGoldBonusRate() { return goldBonusRate; }
    public void setGoldBonusRate(double goldBonusRate) { this.goldBonusRate = goldBonusRate; }

    public double getExpBonusRate() { return expBonusRate; }
    public void setExpBonusRate(double expBonusRate) { this.expBonusRate = expBonusRate; }

    public int getTalentPoints() { return talentPoints; }
    public void setTalentPoints(int talentPoints) { this.talentPoints = talentPoints; }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = skillPoints; }
}
