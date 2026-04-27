import com.example.treasure_and_battle.model.attribute.AttributeSet;

public class Character {
    // ================ 基础信息 ================
    private final int characterId;         // 角色唯一标识
    private String name;                   // 角色名称
    private Profession profession;         // 角色职业
    private ProfessionType professionType; // 角色职业类型

    // ================ 成长信息 ================
    private int level;                // 等级
    private int currentExp;           // 当前经验值
    private int expToNextLevel;       // 升级所需经验值
    private int talentPoints;         // 可用天赋点数
    private int skillPoints;          // 可用技能点数
    private int gold;                 // 金币数量

    // ================ 属性信息 ================
    private AttributeSet finalAttributes;          // 角色属性系统
    private Boolean finalAttributesDirtyFlag;      // 脏标记
    private int currentHp;                         // 当前生命值
    private int currentMp;                         // 当前魔法值

    // ================ 装备系统 ================
    private CharacterEquipment equipments;        // 角色装备系统

    // ================ 技能系统 ================
    // private CharacterSkills skills;          // 角色技能系统
    // 这是一个伪属性，技能系统实际由Profession类管理，这里只提供接口

    // ================ 物品仓库 ================
    private CharacterInventory inventory;       // 角色物品仓库系统

    // ================ 统计信息 ================
    private CharacterStats stats;            // 角色统计信息系统

    // ================ 构造函数 ================
    public Character(int characterId, String name, ProfessionType professionType) {
        this.characterId = characterId;
        this.name = name;
        this.professionType = professionType;
        this.profession = Profession.initProfession(professionType);
        this.level = 1;
        this.currentExp = 0;
        this.expToNextLevel = expValueForLevel(level);
        this.talentPoints = 0;
        this.skillPoints = 0;
        this.gold = 0;
        this.equipments = new CharacterEquipment();
        this.inventory = new CharacterInventory();
        updateFinalAttributes();
        int currentHp = this.finalAttributes.getMaxHp();
        int currentMp = this.finalAttributes.getMaxMp();
        this.stats = new CharacterStats();
    }

    // ================ Getter方法 ================
    public int getCharacterId() { return characterId; }
    public String getName() { return name; }
    public String getProfessionType() { return professionType.toString(); }
    public int getLevel() { return level; }
    public int getCurrentExp() { return currentExp; }
    public int getExpToNextLevel() { return expToNextLevel; }
    public int getTalentPoints() { return talentPoints; }
    public int getSkillPoints() { return skillPoints; }
    public int getGold() { return gold; }
    public int getCurrentHp() { return currentHp; }
    public int getCurrentMp() { return currentMp; }
    public AttributeSet getCharacterAttributes() {
        if (finalAttributesDirtyFlag)
            updateFinalAttributes();
        return new AttributeSet(this.finalAttributes);
    }
    public int getMaxHp() { 
        if (finalAttributesDirtyFlag)
            updateFinalAttributes();
        return this.finalAttributes.getMaxHp();
    }
    public int getMaxMp() {
        if (finalAttributesDirtyFlag)
            updateFinalAttributes();
        return this.finalAttributes.getMaxMp();
    }
    // TODO: 添加其他Getter方法：装备系统、技能系统、物品仓库、统计信息等

    // ================ Setter方法 ================
    public void setName(String name) { this.name = name; }
    public boolean addExp(int exp) { // 该方法在经验溢出自动升级时返回true，否则返回false
        this.currentExp += exp;
        if (this.currentExp >= this.expToNextLevel) {
            levelUp();
            return true;
        }
        return false;
    }
    public void levelUp() {
        while(currentExp >= expToNextLevel) {
            currentExp -= expToNextLevel;
            level++;
            expToNextLevel = expValueForLevel(level);
            talentPoints += 1;
            skillPoints += 2;
            profession.applyLevelUpGrowth(level);
            finalAttributesDirtyFlag = true;
            currentHp = getMaxHp();
            currentMp = getMaxMp();
        }
    }
    public int addGold(int gold) { this.gold += gold; return this.gold; }
    public int spendGold(int gold) { this.gold -= gold; return this.gold; }
    public void changeCurrentHp(int offset) { this.currentHp += offset; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
    public void changeCurrentMp(int offset) { this.currentMp += offset; }
    public void setCurrentMp(int currentMp) { this.currentMp = currentMp; }
    // TODO: 添加其他Setter方法：装备系统、技能系统、物品仓库、统计信息等

    // ================ 其他方法 ================
    public Player generatePlayer() {
        // TODO: 生成即时的Player对象，以供战斗系统使用
    }
    public void onBattleFinished(BattleResult result) {
        // TODO: 处理战斗结束后的逻辑，如更新角色状态、处理战斗奖励等
    }
    public boolean saveCharacter() {
        // TODO: 保存角色信息到数据库
        return true;
    }
    public static Character loadCharacter(int characterId) {
        // TODO: 从数据库加载角色信息
        return null;
    }


    // ================ 私有方法 ================
    private int expValueForLevel(int level) {
        int BASE_EXP = 100;
        int OFFSET_EXP = 3; 
        // 经验值计算公式（示例）
        return BASE_EXP * (level * level  + OFFSET_EXP * level);
    }

    private int updateFinalAttributes() {
        this.finalAttributes = new AttributeSet()
            .addAttributes(this.profession.getProfessionAttributes())
            .addAttributes(this.equipments.getEquipmentsAttributes());
        this.finalAttributesDirtyFlag = false;
        return this.finalAttributes;
    }
}