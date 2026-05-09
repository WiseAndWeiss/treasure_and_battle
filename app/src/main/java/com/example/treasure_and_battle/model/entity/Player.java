package com.example.treasure_and_battle.model.entity;

import android.content.Context;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.EquipItem;
import com.example.treasure_and_battle.model.item.EquipSlot;
import com.example.treasure_and_battle.utils.AttributeUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
// 暂时注释掉职业相关 import，后续实现后再打开
// import com.example.treasure_and_battle.model.profession.Profession;

/**
 * 玩家类 (Player)
 * 完全基于用户提供的 AttributeSet 重新适配
 * 核心改动：
 * 1. 完全适配 AttributeSet 的字段命名（physicalAtk、magicalAtk 等）
 * 2. 移除 Player 类中重复的收益属性（已封装到 AttributeSet）
 * 3. 利用 AttributeSet 自带的 copyFrom() 方法
 * 4. 保持与 BattleEntity 基类的兼容
 */
public class Player extends BattleEntity {
    // ====================== 玩家专属成长属性 ======================
    private int currentExp;
    private int expToNextLevel;

    // 暂时注释掉职业引用，后续实现后再打开
    // private Profession profession;

    // 成长专属点数
    private int talentPoints;         // 天赋点
    private int skillPoints;          // 技能点

    private Map<EquipSlot, EquipItem> equippedItems = new HashMap<>();

    // ====================== 构造函数（完全适配新架构） ======================
    public Player(String name, Context context) {
        // 调用基类构造函数
        super("player_default", name, 1, context);

        // 初始化玩家专属成长属性
        this.currentExp = 0;
        this.expToNextLevel = 100; // 升到2级需要100经验
        this.talentPoints = 0;
        this.skillPoints = 0;

        // 初始化基础属性
        initBaseAttributes();

        // 暂时注释掉职业相关逻辑
        // setProfession(profession);
    }

    // ====================== 实现基类抽象方法 1：初始化基础属性 ======================
    @Override
    public void initBaseAttributes() {
        // 直接操作 baseAttributes（使用用户 AttributeSet 的字段命名）
        AttributeSet base = this.baseAttributes;

        // 1. 六维属性默认值
        base.strength = 0;
        base.agility = 0;
        base.intelligence = 0;
        base.spirit = 0;
        base.physique = 0;
        base.luck = 0;

        // 2. 核心战斗属性默认值（1级白板玩家）
        base.maxHp = 20;
        base.maxMp = 10;
        base.physicalAtk = 2;
        base.physicalDef = 1;
        base.magicalAtk = 2;
        base.magicalDef = 1;
        base.speed = 10;
        base.maxActionPoints = 2;

        // 3. 附加战斗属性默认值（AttributeSet 构造函数已初始化，这里仅作展示）
        base.physicalCritRate = 0.0f;
        base.magicalCritRate = 0.0f;
        base.physicalCritDmg = 2.0f;
        base.magicalCritDmg = 2.0f;
        base.hitRate = 0.9f;
        base.dodgeRate = 0.0f;
        base.debuffResist = 0.0f;
        base.mpCostReduction = 0.0f;

        // 4. 额外收益属性默认值（已封装在 AttributeSet 中！）
        base.lootRarityBonus = 0.0f;
        base.goldBonus = 1.0f; // 注意：这里是倍率，默认1.0
        base.expBonus = 1.0f;

        // 5. 同步战斗资源到最大值
        this.currentHp = base.maxHp;
        this.currentMp = base.maxMp;
        this.currentActionPoints = base.maxActionPoints;

        // 暂时注释掉职业初始属性应用
        // if (this.profession != null) {
        //     this.profession.applyInitialStats(this);
        // }

        // 初始化完成，标记属性缓存失效
        markAttributeCacheDirty();
    }

    // ====================== 实现基类抽象方法 2：重新计算最终属性 ======================
    @Override
    protected void recalculateFinalAttributes() {
        // 1. 利用 AttributeSet 自带的 copyFrom() 方法复制基础属性
        this.finalAttributes.copyFrom(this.baseAttributes);

        // 2. 调用 AttributeUtils 叠加玩家专属加成（装备 + 词缀 + Buff）
        AttributeSet calculatedAttrs = AttributeUtils.calculateFinalAttributes(this, this.getContext());
        this.finalAttributes.copyFrom(calculatedAttrs);

        // 3. 同步战斗资源上限（保持当前 HP/MP 比例）
        int oldMaxHp = this.finalAttributes.maxHp;
        if (oldMaxHp > 0 && this.currentHp > 0) {
            double hpRatio = (double) this.currentHp / oldMaxHp;
            this.currentHp = (int) (this.finalAttributes.maxHp * hpRatio);
        }
        // 确保当前资源不超过新上限
        this.currentHp = Math.min(this.currentHp, this.finalAttributes.maxHp);
        this.currentMp = Math.min(this.currentMp, this.finalAttributes.maxMp);
    }

    // ====================== 玩家专属：经验与升级逻辑 ======================
    /**
     * 获取经验值并处理升级逻辑（注意：现在 expBonus 在 AttributeSet 里！）
     */
    public void gainExp(int expAmount) {
        // 从 finalAttributes 中获取经验加成倍率
        float expBonus = getFinalAttributes().expBonus;
        int finalExp = (int) (expAmount * expBonus);

        this.currentExp += finalExp;
        // 连续升级机制
        while (this.currentExp >= this.expToNextLevel) {
            levelUp();
        }
    }

    /**
     * 玩家升级
     */
    private void levelUp() {
        this.currentExp -= this.expToNextLevel;
        this.level++;

        // 每升1级，固定获得2点天赋点 + 1点技能点
        this.talentPoints += 2;
        this.skillPoints += 1;

        // 简单的经验曲线
        this.expToNextLevel = (int) (this.expToNextLevel * 1.2);

        // 暂时注释掉职业成长逻辑
        // if (this.profession != null) {
        //     this.profession.applyLevelUpGrowth(this);
        // }

        // 升级流程：标记缓存失效 -> 重算属性 -> 回满状态
        markAttributeCacheDirty();
        // 强制触发一次属性重算，确保下面的 getFinalAttributes() 拿到新值
        getFinalAttributes();

        this.currentHp = getFinalAttributes().maxHp;
        this.currentMp = getFinalAttributes().maxMp;
        this.currentActionPoints = getFinalAttributes().maxActionPoints;
    }

    // ====================== 暂时注释掉职业相关方法 ======================
    // /**
    //  * 玩家选择或重置职业
    //  */
    // public void setProfession(Profession p) {
    //     this.profession = p;
    //     if (p == null) return;
    //
    //     // 重新初始化基础属性
    //     initBaseAttributes();
    //
    //     // 应用职业初始属性
    //     p.applyInitialStats(this);
    //
    //     // 标记缓存失效并回满状态
    //     markAttributeCacheDirty();
    //     this.currentHp = getFinalAttributes().maxHp;
    //     this.currentMp = getFinalAttributes().maxMp;
    // }

    // ====================== 简化后的 Getters & Setters ======================
    // 注意：
    // 1. 六维属性、战斗属性、收益属性（goldBonus/expBonus）已全部封装到 AttributeSet
    // 2. 如需读取属性：使用 getFinalAttributes().xxx 或 getBaseAttributes().xxx
    // 3. 如需修改基础属性：修改后请调用 markAttributeCacheDirty()

    public int getCurrentExp() { return currentExp; }
    public void setCurrentExp(int currentExp) { this.currentExp = currentExp; }

    public int getExpToNextLevel() { return expToNextLevel; }
    public void setExpToNextLevel(int expToNextLevel) { this.expToNextLevel = expToNextLevel; }

    // 暂时注释掉职业 Getter/Setter
    // public Profession getProfession() { return profession; }

    public int getTalentPoints() { return talentPoints; }
    public void setTalentPoints(int talentPoints) { this.talentPoints = talentPoints; }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = skillPoints; }

    // ====================== 六维属性手动分配 ======================
    private int allocatedStrength;
    private int allocatedAgility;
    private int allocatedIntelligence;
    private int allocatedSpirit;
    private int allocatedPhysique;
    private int allocatedLuck;

    public boolean allocateTalentPoint(String attributeName) {
        if (talentPoints <= 0) return false;

        switch (attributeName.toUpperCase()) {
            case "STRENGTH":
                baseAttributes.strength++;
                allocatedStrength++;
                talentPoints--;
                break;
            case "AGILITY":
                baseAttributes.agility++;
                allocatedAgility++;
                talentPoints--;
                break;
            case "INTELLIGENCE":
                baseAttributes.intelligence++;
                allocatedIntelligence++;
                talentPoints--;
                break;
            case "SPIRIT":
                baseAttributes.spirit++;
                allocatedSpirit++;
                talentPoints--;
                break;
            case "PHYSIQUE":
                baseAttributes.physique++;
                allocatedPhysique++;
                talentPoints--;
                break;
            case "LUCK":
                baseAttributes.luck++;
                allocatedLuck++;
                talentPoints--;
                break;
            default:
                return false;
        }
        markAttributeCacheDirty();
        return true;
    }

    public void resetAllTalentPoints() {
        baseAttributes.strength -= allocatedStrength;
        baseAttributes.agility -= allocatedAgility;
        baseAttributes.intelligence -= allocatedIntelligence;
        baseAttributes.spirit -= allocatedSpirit;
        baseAttributes.physique -= allocatedPhysique;
        baseAttributes.luck -= allocatedLuck;

        talentPoints += allocatedStrength + allocatedAgility + allocatedIntelligence
                + allocatedSpirit + allocatedPhysique + allocatedLuck;

        allocatedStrength = 0;
        allocatedAgility = 0;
        allocatedIntelligence = 0;
        allocatedSpirit = 0;
        allocatedPhysique = 0;
        allocatedLuck = 0;

        markAttributeCacheDirty();
        getFinalAttributes();
        this.currentHp = getFinalAttributes().maxHp;
        this.currentMp = getFinalAttributes().maxMp;
    }

    public int getAllocatedStrength() { return allocatedStrength; }
    public int getAllocatedAgility() { return allocatedAgility; }
    public int getAllocatedIntelligence() { return allocatedIntelligence; }
    public int getAllocatedSpirit() { return allocatedSpirit; }
    public int getAllocatedPhysique() { return allocatedPhysique; }
    public int getAllocatedLuck() { return allocatedLuck; }

    public int getTotalAllocatedPoints() {
        return allocatedStrength + allocatedAgility + allocatedIntelligence
                + allocatedSpirit + allocatedPhysique + allocatedLuck;
    }

    // ====================== 装备 ======================
    public EquipItem equip(EquipItem item) {
        if (item == null) return null;
        EquipItem old = equippedItems.put(item.getSlot(), item);
        markAttributeCacheDirty();
        return old;
    }

    public EquipItem unequip(EquipSlot slot) {
        EquipItem removed = equippedItems.remove(slot);
        if (removed != null) {
            markAttributeCacheDirty();
        }
        return removed;
    }

    public EquipItem getEquippedItem(EquipSlot slot) {
        return equippedItems.get(slot);
    }

    public Collection<EquipItem> getEquippedItems() {
        return equippedItems.values();
    }
}