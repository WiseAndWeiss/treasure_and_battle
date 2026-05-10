package com.example.treasure_and_battle.character;

import android.content.Context;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.profession.Profession;
import com.example.treasure_and_battle.profession.ProfessionManager;
import com.example.treasure_and_battle.profession.ProfessionType;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.example.treasure_and_battle.skill.passive.PassiveSkill;
import com.example.treasure_and_battle.utils.AttributeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    private int allocatedStrength;
    private int allocatedAgility;
    private int allocatedIntelligence;
    private int allocatedSpirit;
    private int allocatedPhysique;
    private int allocatedLuck;

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
        this.talentPoints += 2;
        this.skillPoints += 1;

        growHpMpAfterMaxIncrease(oldMaxHp, oldMaxMp);
    }

    private int expValueForLevel(int level) {
        int BASE_EXP = 100;
        int OFFSET_EXP = 3;
        return BASE_EXP * (level * level + OFFSET_EXP * level);
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

    // ========== 装备 ==========

    public EquipItem equip(EquipItem item) {
        if (item == null) return null;
        return equippedItems.put(item.getSlot(), item);
    }

    public EquipItem unequip(EquipSlot slot) {
        return equippedItems.remove(slot);
    }

    public EquipItem getEquippedItem(EquipSlot slot) {
        return equippedItems.get(slot);
    }

    public Collection<EquipItem> getEquippedItems() {
        return equippedItems.values();
    }

    // ========== 生成战斗实体 ==========

    public Player generatePlayer() {
        Player player = new Player(name, mContext);
        player.owner = this;

        injectBaseAttributes(player);
        player.copyEquipmentFrom(this.equippedItems);
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
    public int getBaseMaxMp() { return baseMaxMp; }

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
}
