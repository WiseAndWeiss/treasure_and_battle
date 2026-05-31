package com.example.treasure_and_battle.character;

import android.content.Context;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.profession.Profession;
import com.example.treasure_and_battle.profession.ProfessionManager;
import com.example.treasure_and_battle.profession.ProfessionType;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.SkillTree;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.utils.AttributeUtils;

import com.example.treasure_and_battle.manager.skill.SkillManager;
import com.example.treasure_and_battle.skill.Skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色类
 * 职责：管理持久数据（等级、经验、金币、天赋、装备、HP/MP），
 * 通过 generatePlayer() 生成战斗用的 Player 快照。
 */
public class Character {
    private final Context mContext;
    private final int characterId;
    private String name;
    private Profession profession;
    private ProfessionType professionType;

    private int level;
    private int currentExp;
    private int expToNextLevel;

    private int talentPoints;
    private int skillPoints;
    private int gold;

    private int baseMaxHp = 20;
    private int baseMaxMp = 10;
    private int currentHp;
    private int currentMp;

    private Map<EquipSlot, EquipItem> equippedItems = new HashMap<>();
    private EquipItem leftRing;
    private EquipItem rightRing;

    private int allocatedStrength;
    private int allocatedAgility;
    private int allocatedIntelligence;
    private int allocatedSpirit;
    private int allocatedPhysique;
    private int allocatedLuck;

    // 基础战斗属性（用于调试）
    private int basePhysicalDef = 0;
    private int baseMagicalDef = 0;

    /** 背包：125 格固定槽位，与 InventoryGridSync.BAG_SLOT_COUNT 一致 */
    private final List<Item> bagItems = new ArrayList<Item>(125);

    public Character(int characterId, String name, ProfessionType professionType, Context context) {
        this.mContext = context;
        this.characterId = characterId;
        this.name = name;
        this.professionType = professionType;
        this.profession = ProfessionManager.getInstance(context).createProfession(professionType);
        this.level = 1;
        this.currentExp = 0;
        this.expToNextLevel = expValueForLevel(1);
        this.talentPoints = 0;
        this.skillPoints = 0;
        this.gold = 0;
        this.currentHp = this.baseMaxHp;
        this.currentMp = this.baseMaxMp;

        for (int i = 0; i < 125; i++) {
            bagItems.add(null);
        }
    }

    // ========== 经验与升级 ==========

    public void gainExp(int expAmount) {
        this.currentExp += expAmount;
        while (this.currentExp >= this.expToNextLevel) {
            levelUp();
        }
    }

    private void levelUp() {
        AttributeSet before = AttributeUtils.calculateCharacterAttributes(this);
        int oldMaxHp = before.maxHp;
        int oldMaxMp = before.maxMp;

        this.currentExp -= this.expToNextLevel;
        this.level++;
        this.baseMaxHp += 8;
        this.baseMaxMp += 4;
        this.expToNextLevel = expValueForLevel(this.level);
        this.talentPoints += 5;
        this.skillPoints += 1;

        // 平衡性调整：每一点六维属性都自动加1
        allocatedStrength++;
        allocatedAgility++;
        allocatedIntelligence++;
        allocatedSpirit++;
        allocatedPhysique++;
        allocatedLuck++;

        growHpMpAfterMaxIncrease(oldMaxHp, oldMaxMp);
    }

    private int expValueForLevel(int level) {
        int BASE_EXP = 100;
        int OFFSET_EXP = 0;
        return BASE_EXP * level ;
    }

    // ========== 天赋分配 ==========

    public boolean allocateTalentPoint(String attributeName) {
        if (talentPoints <= 0) return false;

        String key = attributeName.toUpperCase();
        switch (key) {
            case "STRENGTH":
            case "AGILITY":
            case "INTELLIGENCE":
            case "SPIRIT":
            case "PHYSIQUE":
            case "LUCK":
                break;
            default:
                return false;
        }

        AttributeSet before = AttributeUtils.calculateCharacterAttributes(this);
        int oldMaxHp = before.maxHp;
        int oldMaxMp = before.maxMp;

        switch (key) {
            case "STRENGTH":   allocatedStrength++;   talentPoints--; break;
            case "AGILITY":    allocatedAgility++;    talentPoints--; break;
            case "INTELLIGENCE": allocatedIntelligence++; talentPoints--; break;
            case "SPIRIT":     allocatedSpirit++;     talentPoints--; break;
            case "PHYSIQUE":   allocatedPhysique++;   talentPoints--; break;
            case "LUCK":       allocatedLuck++;       talentPoints--; break;
        }
        growHpMpAfterMaxIncrease(oldMaxHp, oldMaxMp);
        return true;
    }

    public void resetAllTalentPoints() {
        talentPoints += allocatedStrength + allocatedAgility + allocatedIntelligence
                + allocatedSpirit + allocatedPhysique + allocatedLuck;
        allocatedStrength = 0;
        allocatedAgility = 0;
        allocatedIntelligence = 0;
        allocatedSpirit = 0;
        allocatedPhysique = 0;
        allocatedLuck = 0;
        AttributeSet after = AttributeUtils.calculateCharacterAttributes(this);
        currentHp = Math.min(currentHp, after.maxHp);
        currentMp = Math.min(currentMp, after.maxMp);
    }

    public void switchProfession(ProfessionType newProfessionType, Profession newProfession) {
        resetAllTalentPoints();
        resetAllSkills();
        this.professionType = newProfessionType;
        this.profession = newProfession;
    }

    public void resetAllSkills() {
        int skillRefund = 0;
        if (profession != null) {
            skillRefund += profession.getActiveSkillTree().resetAllSkills();
            skillRefund += profession.getPassiveSkillTree().resetAllSkills();
            skillRefund += profession.getEventSkillTree().resetAllSkills();
        }
        skillPoints += skillRefund;
    }

    // ========== 装备 ==========

    public EquipItem equip(EquipItem item) {
        if (item == null) return null;
        if (item.getSlot() == EquipSlot.RING) {
            EquipItem old = leftRing != null ? leftRing : rightRing;
            if (leftRing == null) {
                leftRing = item;
            } else if (rightRing == null) {
                rightRing = item;
            } else {
                leftRing = item;
            }
            return old;
        }
        return equippedItems.put(item.getSlot(), item);
    }

    public void equipRing(EquipItem item, boolean left) {
        if (item == null || item.getSlot() != EquipSlot.RING) return;
        if (left) {
            leftRing = item;
        } else {
            rightRing = item;
        }
    }

    public EquipItem unequip(EquipSlot slot) {
        if (slot == EquipSlot.RING) {
            EquipItem removed = leftRing;
            leftRing = null;
            return removed;
        }
        return equippedItems.remove(slot);
    }

    public EquipItem unequipRing(boolean left) {
        EquipItem removed;
        if (left) {
            removed = leftRing;
            leftRing = null;
        } else {
            removed = rightRing;
            rightRing = null;
        }
        return removed;
    }

    public EquipItem getEquippedItem(EquipSlot slot) {
        if (slot == EquipSlot.RING) return leftRing;
        return equippedItems.get(slot);
    }

    public EquipItem getLeftRing() { return leftRing; }
    public EquipItem getRightRing() { return rightRing; }

    public Collection<EquipItem> getEquippedItems() {
        List<EquipItem> all = new ArrayList<>(equippedItems.values());
        if (leftRing != null) all.add(leftRing);
        if (rightRing != null) all.add(rightRing);
        return all;
    }

    // ========== 生成战斗实体 ==========

    public Player generatePlayer() {
        Player player = new Player(name, mContext);
        player.owner = this;

        injectBaseAttributes(player);
        player.copyEquipmentFrom(this.equippedItems);
        player.copyRingsFrom(leftRing, rightRing);
        player.setLevel(level);
        player.setCurrentHp(Math.min(currentHp, player.getFinalAttributes().maxHp));
        player.setCurrentMp(Math.min(currentMp, player.getFinalAttributes().maxMp));

        injectSkills(player);

        return player;
    }

    private void injectBaseAttributes(Player player) {
        AttributeSet base = player.getBaseAttributes();
        base.strength = allocatedStrength;
        base.agility = allocatedAgility;
        base.intelligence = allocatedIntelligence;
        base.spirit = allocatedSpirit;
        base.physique = allocatedPhysique;
        base.luck = allocatedLuck;
        base.maxHp = this.baseMaxHp;
        base.maxMp = this.baseMaxMp;

        // 添加基础防御属性
        base.physicalDef = basePhysicalDef;
        base.magicalDef = baseMagicalDef;

        Player.computeFullBaseCombatAttributes(base);

        player.markAttributeCacheDirty();
    }

    private void growHpMpAfterMaxIncrease(int oldMaxHp, int oldMaxMp) {
        AttributeSet after = AttributeUtils.calculateCharacterAttributes(this);
        int newMaxHp = after.maxHp;
        int newMaxMp = after.maxMp;
        int dHp = Math.max(0, newMaxHp - oldMaxHp);
        int dMp = Math.max(0, newMaxMp - oldMaxMp);
        currentHp = Math.min(newMaxHp, currentHp + dHp);
        currentMp = Math.min(newMaxMp, currentMp + dMp);
    }

    private void injectSkills(Player player) {
        if (profession == null) return;

        for (Skill skill : profession.getLearnedActiveSkill()) {
            if (skill instanceof ActiveSkill) {
                ((ActiveSkill) skill).clearCooldown();
                player.addActiveSkill((ActiveSkill) skill);
            }
        }
        List<PassiveSkill> passiveSkills = new ArrayList<>();
        for (Skill skill : profession.getLearnedPassiveSkill()) {
            if (skill instanceof PassiveSkill) {
                passiveSkills.add((PassiveSkill) skill);
            }
        }
        player.setPassiveSkillList(passiveSkills);
    }

    // ========== 战斗后同步 ==========

    public void syncFromPlayer(Player player) {
        this.currentHp = player.getCurrentHp();
        this.currentMp = player.getCurrentMp();
    }

    // ========== Getter ==========

    public Context getContext() { return mContext; }
    public int getCharacterId() { return characterId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Profession getProfession() { return profession; }
    public ProfessionType getProfessionType() { return professionType; }
    public int getLevel() { return level; }
    public int getCurrentExp() { return currentExp; }
    public int getExpToNextLevel() { return expToNextLevel; }
    public int getTalentPoints() { return talentPoints; }
    public int getSkillPoints() { return skillPoints; }
    public int getGold() { return gold; }
    public void addGold(int amount) { this.gold += amount; }
    public boolean spendGold(int amount) { if (this.gold < amount) return false; this.gold -= amount; return true; }
    public int getBaseMaxHp() { return baseMaxHp; }
    public void setBaseMaxHp(int maxHp) {
        this.baseMaxHp = maxHp;
        // 确保当前HP不超过新的最大值
        if (currentHp > baseMaxHp) {
            currentHp = baseMaxHp;
        }
    }
    public int getBaseMaxMp() { return baseMaxMp; }

    // 设置基础防御属性的方法（用于调试）
    public void setBasePhysicalDef(int def) { this.basePhysicalDef = def; }
    public void setBaseMagicalDef(int def) { this.baseMagicalDef = def; }
    public int getBasePhysicalDef() { return basePhysicalDef; }
    public int getBaseMagicalDef() { return baseMagicalDef; }

    public void addTalentPoints(int amount) {
        if (amount > 0) {
            this.talentPoints += amount;
        }
    }

    public void addSkillPoints(int amount) {
        if (amount > 0) {
            this.skillPoints += amount;
        }
    }
    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int hp) { this.currentHp = hp; }
    public int getCurrentMp() { return currentMp; }
    public void setCurrentMp(int mp) { this.currentMp = mp; }
    public int getAllocatedStrength() { return allocatedStrength; }
    public int getAllocatedAgility() { return allocatedAgility; }
    public int getAllocatedIntelligence() { return allocatedIntelligence; }
    public int getAllocatedSpirit() { return allocatedSpirit; }
    public int getAllocatedPhysique() { return allocatedPhysique; }
    public int getAllocatedLuck() { return allocatedLuck; }

    /** 背包：125 格固定槽位列表 */
    public List<Item> getBagItems() { return bagItems; }

    void setLevel(int level) { this.level = level; }
    void setCurrentExp(int exp) { this.currentExp = exp; }
    void setExpToNextLevel(int exp) { this.expToNextLevel = exp; }
    void setTalentPoints(int pts) { this.talentPoints = pts; }
    void setSkillPoints(int pts) { this.skillPoints = pts; }
    void setGold(int g) { this.gold = g; }
    void setBaseMaxMp(int mp) { this.baseMaxMp = mp; }
    void setProfession(Profession p) { this.profession = p; }

    void copyEquippedFrom(Map<EquipSlot, EquipItem> src, EquipItem lr, EquipItem rr) {
        this.equippedItems.clear();
        if (src != null) this.equippedItems.putAll(src);
        this.leftRing = lr;
        this.rightRing = rr;
    }

    void setAllocatedStats(int s, int a, int i, int sp, int p, int l) {
        this.allocatedStrength = s; this.allocatedAgility = a;
        this.allocatedIntelligence = i; this.allocatedSpirit = sp;
        this.allocatedPhysique = p; this.allocatedLuck = l;
    }

    // ========== 序列化 ==========

    public static class SkillTreeData {
        public Map<String, Integer> learnedSkillLevels;
        public int usedPoints;
        public int unlockedLayer;
    }

    public static class CharacterData {
        public int characterId;
        public String name;
        public String professionType;
        public int level;
        public int currentExp;
        public int expToNextLevel;
        public int talentPoints;
        public int skillPoints;
        public int gold;
        public int baseMaxHp;
        public int baseMaxMp;
        public int currentHp;
        public int currentMp;
        public int allocatedStrength;
        public int allocatedAgility;
        public int allocatedIntelligence;
        public int allocatedSpirit;
        public int allocatedPhysique;
        public int allocatedLuck;
        public Map<String, EquipItem> equippedItems;
        public EquipItem leftRing;
        public EquipItem rightRing;
        public List<Item> bagItems;
        public SkillTreeData activeSkillTree;
        public SkillTreeData passiveSkillTree;
        public SkillTreeData eventSkillTree;
    }

    public static class SaveData {
        public int version = 1;
        public long timestamp;
        public long playTimeSeconds;
        public CharacterData character;
    }

    public SaveData toSaveData() {
        SaveData data = new SaveData();
        data.character = new CharacterData();
        CharacterData cd = data.character;

        cd.characterId = this.characterId;
        cd.name = this.name;
        cd.professionType = this.professionType.name();
        cd.level = this.level;
        cd.currentExp = this.currentExp;
        cd.expToNextLevel = this.expToNextLevel;
        cd.talentPoints = this.talentPoints;
        cd.skillPoints = this.skillPoints;
        cd.gold = this.gold;
        cd.baseMaxHp = this.baseMaxHp;
        cd.baseMaxMp = this.baseMaxMp;
        cd.currentHp = this.currentHp;
        cd.currentMp = this.currentMp;
        cd.allocatedStrength = this.allocatedStrength;
        cd.allocatedAgility = this.allocatedAgility;
        cd.allocatedIntelligence = this.allocatedIntelligence;
        cd.allocatedSpirit = this.allocatedSpirit;
        cd.allocatedPhysique = this.allocatedPhysique;
        cd.allocatedLuck = this.allocatedLuck;

        cd.equippedItems = new LinkedHashMap<>();
        for (Map.Entry<EquipSlot, EquipItem> e : this.equippedItems.entrySet()) {
            cd.equippedItems.put(e.getKey().name(), e.getValue());
        }
        cd.leftRing = this.leftRing;
        cd.rightRing = this.rightRing;

        cd.bagItems = new ArrayList<>(this.bagItems);

        cd.activeSkillTree = saveSkillTree(this.profession != null ? this.profession.getActiveSkillTree() : null);
        cd.passiveSkillTree = saveSkillTree(this.profession != null ? this.profession.getPassiveSkillTree() : null);
        cd.eventSkillTree = saveSkillTree(this.profession != null ? this.profession.getEventSkillTree() : null);

        return data;
    }

    private static SkillTreeData saveSkillTree(SkillTree tree) {
        if (tree == null) return null;
        SkillTreeData data = new SkillTreeData();
        data.learnedSkillLevels = new LinkedHashMap<>();
        for (Skill skill : tree.getAllLearnedSkills()) {
            data.learnedSkillLevels.put(skill.getSkillId(), skill.getLevel());
        }
        data.usedPoints = tree.getUsedPoints();
        data.unlockedLayer = tree.getUnlockedLayer();
        return data;
    }

    public static Character fromSaveData(CharacterData cd, Context context) {
        if (cd == null) return null;

        ProfessionType pt;
        try {
            pt = ProfessionType.valueOf(cd.professionType);
        } catch (Exception e) {
            pt = ProfessionType.WARRIOR;
        }

        Character ch = new Character(cd.characterId, cd.name, pt, context);
        ch.setLevel(cd.level);
        ch.setCurrentExp(cd.currentExp);
        ch.setExpToNextLevel(cd.expToNextLevel);
        ch.setTalentPoints(cd.talentPoints);
        ch.setSkillPoints(cd.skillPoints);
        ch.setGold(cd.gold);
        ch.setBaseMaxHp(cd.baseMaxHp);
        ch.setBaseMaxMp(cd.baseMaxMp);
        ch.setCurrentHp(cd.currentHp);
        ch.setCurrentMp(cd.currentMp);
        ch.setAllocatedStats(cd.allocatedStrength, cd.allocatedAgility, cd.allocatedIntelligence,
                cd.allocatedSpirit, cd.allocatedPhysique, cd.allocatedLuck);

        Map<EquipSlot, EquipItem> eqMap = new HashMap<>();
        if (cd.equippedItems != null) {
            for (Map.Entry<String, EquipItem> e : cd.equippedItems.entrySet()) {
                try {
                    eqMap.put(EquipSlot.valueOf(e.getKey()), e.getValue());
                } catch (Exception ignored) {}
            }
        }
        ch.copyEquippedFrom(eqMap, cd.leftRing, cd.rightRing);

        if (cd.bagItems != null) {
            for (int i = 0; i < Math.min(cd.bagItems.size(), 125); i++) {
                ch.bagItems.set(i, cd.bagItems.get(i));
            }
        }

        SkillManager sm = SkillManager.getInstance(context);
        restoreSkillTree(sm, ch.profession.getActiveSkillTree(), cd.activeSkillTree);
        restoreSkillTree(sm, ch.profession.getPassiveSkillTree(), cd.passiveSkillTree);
        restoreSkillTree(sm, ch.profession.getEventSkillTree(), cd.eventSkillTree);

        return ch;
    }

    private static void restoreSkillTree(SkillManager sm, SkillTree tree, SkillTreeData data) {
        if (tree == null || data == null || data.learnedSkillLevels == null) return;
        List<Skill> restored = new ArrayList<>();
        for (Map.Entry<String, Integer> e : data.learnedSkillLevels.entrySet()) {
            String skillId = e.getKey();
            int level = e.getValue();
            if (sm.hasSkill(skillId) && level > 0) {
                Skill skill = sm.createSkillBySkillId(skillId, level);
                if (skill != null) {
                    restored.add(skill);
                }
            }
        }
        tree.loadSavedState(restored, data.usedPoints, data.unlockedLayer);
    }
}
