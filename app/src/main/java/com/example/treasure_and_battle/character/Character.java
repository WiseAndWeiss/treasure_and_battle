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

    // ========== 等级与经验 ==========
    private int level;
    private int currentExp;
    private int expToNextLevel;

    // ========== 资源 ==========
    private int talentPoints;
    private int skillPoints;
    private int gold;

    // ========== HP/MP（跨战斗持久化） ==========
    private int currentHp;
    private int currentMp;

    // ========== 装备 ==========
    private Map<EquipSlot, EquipItem> equippedItems = new HashMap<>();

    // ========== 天赋分配 ==========
    private int allocatedStrength;
    private int allocatedAgility;
    private int allocatedIntelligence;
    private int allocatedSpirit;
    private int allocatedPhysique;
    private int allocatedLuck;

    // ========== 构造 ==========

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
        this.currentHp = 20;
        this.currentMp = 10;
    }

    // ========== 经验与升级 ==========

    public void gainExp(int expAmount) {
        this.currentExp += expAmount;
        while (this.currentExp >= this.expToNextLevel) {
            levelUp();
        }
    }

    private void levelUp() {
        this.currentExp -= this.expToNextLevel;
        this.level++;
        this.expToNextLevel = expValueForLevel(this.level);
        this.talentPoints += 1;
        this.skillPoints += 2;
    }

    private int expValueForLevel(int level) {
        int BASE_EXP = 100;
        int OFFSET_EXP = 3;
        return BASE_EXP * (level * level + OFFSET_EXP * level);
    }

    // ========== 天赋分配 ==========

    public boolean allocateTalentPoint(String attributeName) {
        if (talentPoints <= 0) return false;

        switch (attributeName.toUpperCase()) {
            case "STRENGTH":   allocatedStrength++;   talentPoints--; break;
            case "AGILITY":    allocatedAgility++;    talentPoints--; break;
            case "INTELLIGENCE": allocatedIntelligence++; talentPoints--; break;
            case "SPIRIT":     allocatedSpirit++;     talentPoints--; break;
            case "PHYSIQUE":   allocatedPhysique++;   talentPoints--; break;
            case "LUCK":       allocatedLuck++;       talentPoints--; break;
            default: return false;
        }
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

        player.markAttributeCacheDirty();
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
        // 消耗品变化由 InventoryManager 单例自动反映
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
