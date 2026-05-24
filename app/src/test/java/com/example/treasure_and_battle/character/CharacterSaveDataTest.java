package com.example.treasure_and_battle.character;

import android.content.Context;

import com.example.treasure_and_battle.profession.ProfessionType;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.SkillTree;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class CharacterSaveDataTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
    }

    // ====================== toSaveData basic fields ======================

    @Test
    public void testToSaveData_BasicFields() {
        Character ch = new Character(1, "冒险者", ProfessionType.WARRIOR, context);

        Character.SaveData saveData = ch.toSaveData();
        assertNotNull(saveData);
        assertEquals(1, saveData.version);
        assertNotNull(saveData.character);

        Character.CharacterData cd = saveData.character;
        assertEquals(1, cd.characterId);
        assertEquals("冒险者", cd.name);
        assertEquals("WARRIOR", cd.professionType);
        assertEquals(1, cd.level);
        assertEquals(0, cd.currentExp);
        assertTrue(cd.expToNextLevel > 0);
        assertEquals(0, cd.talentPoints);
        assertEquals(0, cd.skillPoints);
        assertEquals(0, cd.gold);
        assertEquals(20, cd.baseMaxHp);
        assertEquals(10, cd.baseMaxMp);
        assertEquals(20, cd.currentHp);
        assertEquals(10, cd.currentMp);
    }

    @Test
    public void testToSaveData_AllocatedStatsZero() {
        Character ch = new Character(1, "新手", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = ch.toSaveData().character;

        assertEquals(0, cd.allocatedStrength);
        assertEquals(0, cd.allocatedAgility);
        assertEquals(0, cd.allocatedIntelligence);
        assertEquals(0, cd.allocatedSpirit);
        assertEquals(0, cd.allocatedPhysique);
        assertEquals(0, cd.allocatedLuck);
    }

    @Test
    public void testToSaveData_LeveledCharacter() {
        Character ch = new Character(1, "老手", ProfessionType.MAGE, context);
        ch.gainExp(ch.getExpToNextLevel());
        ch.gainExp(ch.getExpToNextLevel());

        Character.CharacterData cd = ch.toSaveData().character;
        assertEquals(3, cd.level);
        assertTrue(cd.talentPoints >= 4);
        assertTrue(cd.skillPoints >= 2);
        assertEquals(36, cd.baseMaxHp);
        assertEquals(18, cd.baseMaxMp);
    }

    @Test
    public void testToSaveData_WithGoldAndExp() {
        Character ch = new Character(1, "财主", ProfessionType.RANGER, context);
        ch.addGold(8888);
        ch.gainExp(120);

        Character.CharacterData cd = ch.toSaveData().character;
        assertEquals(8888, cd.gold);
        assertEquals(120, cd.currentExp);
    }

    @Test
    public void testToSaveData_DamagedHpMp() {
        Character ch = new Character(1, "伤者", ProfessionType.WARRIOR, context);
        ch.setCurrentHp(3);
        ch.setCurrentMp(0);

        Character.CharacterData cd = ch.toSaveData().character;
        assertEquals(3, cd.currentHp);
        assertEquals(0, cd.currentMp);
    }

    @Test
    public void testToSaveData_AllocatedStats() {
        Character ch = new Character(1, "天赋师", ProfessionType.WARRIOR, context);
        ch.gainExp(ch.getExpToNextLevel() * 20);
        ch.allocateTalentPoint("STRENGTH");
        ch.allocateTalentPoint("STRENGTH");
        ch.allocateTalentPoint("AGILITY");
        ch.allocateTalentPoint("LUCK");

        Character.CharacterData cd = ch.toSaveData().character;
        assertEquals(2, cd.allocatedStrength);
        assertEquals(1, cd.allocatedAgility);
        assertEquals(0, cd.allocatedIntelligence);
        assertEquals(0, cd.allocatedSpirit);
        assertEquals(0, cd.allocatedPhysique);
        assertEquals(1, cd.allocatedLuck);
    }

    // ====================== toSaveData null collections ======================

    @Test
    public void testToSaveData_EmptyBag() {
        Character ch = new Character(1, "空包", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = ch.toSaveData().character;

        assertNotNull(cd.bagItems);
        assertEquals(125, cd.bagItems.size());
    }

    @Test
    public void testToSaveData_NoEquippedItems() {
        Character ch = new Character(1, "裸装", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = ch.toSaveData().character;

        assertNotNull(cd.equippedItems);
        assertTrue(cd.equippedItems.isEmpty());
        assertNull(cd.leftRing);
        assertNull(cd.rightRing);
    }

    // ====================== toSaveData skill trees ======================

    @Test
    public void testToSaveData_SkillTrees_InitiallyNull() {
        Character ch = new Character(1, "技能新手", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = ch.toSaveData().character;

        assertNotNull(ch.getProfession());
        assertNotNull(cd.activeSkillTree);
        assertNotNull(cd.passiveSkillTree);
        assertNotNull(cd.eventSkillTree);

        assertTrue(cd.activeSkillTree.learnedSkillLevels.isEmpty());
        assertTrue(cd.passiveSkillTree.learnedSkillLevels.isEmpty());
    }

    // ====================== fromSaveData basic fields ======================

    @Test
    public void testFromSaveData_RestoresBasicFields() {
        Character.CharacterData cd = new Character.CharacterData();
        cd.characterId = 2;
        cd.name = "英雄";
        cd.professionType = "MAGE";
        cd.level = 1;
        cd.currentExp = 0;
        cd.expToNextLevel = 100;
        cd.talentPoints = 0;
        cd.skillPoints = 0;
        cd.gold = 0;
        cd.baseMaxHp = 20;
        cd.baseMaxMp = 10;
        cd.currentHp = 20;
        cd.currentMp = 10;
        cd.equippedItems = new java.util.LinkedHashMap<>();
        cd.bagItems = new java.util.ArrayList<>();

        Character ch = Character.fromSaveData(cd, context);

        assertNotNull(ch);
        assertEquals(2, ch.getCharacterId());
        assertEquals("英雄", ch.getName());
        assertEquals(ProfessionType.MAGE, ch.getProfessionType());
    }

    @Test
    public void testFromSaveData_NullData_ReturnsNull() {
        assertNull(Character.fromSaveData(null, context));
    }

    // ====================== fromSaveData profession types ======================

    @Test
    public void testFromSaveData_Profession_Warrior() {
        Character.CharacterData cd = createMinimalData("战士", "WARRIOR");
        Character ch = Character.fromSaveData(cd, context);
        assertEquals(ProfessionType.WARRIOR, ch.getProfessionType());
    }

    @Test
    public void testFromSaveData_Profession_Mage() {
        Character.CharacterData cd = createMinimalData("法师", "MAGE");
        Character ch = Character.fromSaveData(cd, context);
        assertEquals(ProfessionType.MAGE, ch.getProfessionType());
    }

    @Test
    public void testFromSaveData_Profession_Ranger() {
        Character.CharacterData cd = createMinimalData("游侠", "RANGER");
        Character ch = Character.fromSaveData(cd, context);
        assertEquals(ProfessionType.RANGER, ch.getProfessionType());
    }

    @Test
    public void testFromSaveData_InvalidProfession_FallsBackToWarrior() {
        Character.CharacterData cd = createMinimalData("异类", "INVALID_PROFESSION");
        Character ch = Character.fromSaveData(cd, context);
        assertNotNull(ch);
        assertEquals(ProfessionType.WARRIOR, ch.getProfessionType());
    }

    // ====================== roundtrip ======================

    @Test
    public void testRoundTrip_BasicCharacter() {
        Character original = new Character(1, "往返测试", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = original.toSaveData().character;
        Character restored = Character.fromSaveData(cd, context);

        assertNotNull(restored);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getProfessionType(), restored.getProfessionType());
        assertEquals(original.getLevel(), restored.getLevel());
        assertEquals(original.getCurrentHp(), restored.getCurrentHp());
        assertEquals(original.getCurrentMp(), restored.getCurrentMp());
        assertEquals(original.getGold(), restored.getGold());
    }

    @Test
    public void testRoundTrip_LeveledCharacter() {
        Character original = new Character(1, "练级", ProfessionType.MAGE, context);
        original.gainExp(original.getExpToNextLevel() * 5);
        original.addGold(2000);

        Character.CharacterData cd = original.toSaveData().character;
        Character restored = Character.fromSaveData(cd, context);

        assertEquals(original.getLevel(), restored.getLevel());
        assertEquals(original.getCurrentExp(), restored.getCurrentExp());
        assertEquals(original.getGold(), restored.getGold());
        assertEquals(original.getTalentPoints(), restored.getTalentPoints());
        assertEquals(original.getSkillPoints(), restored.getSkillPoints());
        assertEquals(original.getBaseMaxHp(), restored.getBaseMaxHp());
        assertEquals(original.getBaseMaxMp(), restored.getBaseMaxMp());
    }

    @Test
    public void testRoundTrip_DamagedHpMp() {
        Character original = new Character(1, "伤者", ProfessionType.RANGER, context);
        original.setCurrentHp(7);
        original.setCurrentMp(2);

        Character.CharacterData cd = original.toSaveData().character;
        Character restored = Character.fromSaveData(cd, context);

        assertEquals(7, restored.getCurrentHp());
        assertEquals(2, restored.getCurrentMp());
    }

    @Test
    public void testRoundTrip_AllocatedStats() {
        Character original = new Character(1, "天赋", ProfessionType.WARRIOR, context);
        original.gainExp(original.getExpToNextLevel() * 10);
        original.allocateTalentPoint("STRENGTH");
        original.allocateTalentPoint("AGILITY");
        original.allocateTalentPoint("INTELLIGENCE");
        original.allocateTalentPoint("SPIRIT");
        original.allocateTalentPoint("PHYSIQUE");
        original.allocateTalentPoint("LUCK");
        original.allocateTalentPoint("STRENGTH");

        Character.CharacterData cd = original.toSaveData().character;
        Character restored = Character.fromSaveData(cd, context);

        assertEquals(2, restored.getAllocatedStrength());
        assertEquals(1, restored.getAllocatedAgility());
        assertEquals(1, restored.getAllocatedIntelligence());
        assertEquals(1, restored.getAllocatedSpirit());
        assertEquals(1, restored.getAllocatedPhysique());
        assertEquals(1, restored.getAllocatedLuck());
    }

    @Test
    public void testRoundTrip_HighLevelCharacter() {
        Character original = new Character(1, "满级", ProfessionType.MAGE, context);
        original.gainExp(original.getExpToNextLevel() * 10);
        original.addGold(50000);
        original.setCurrentHp(original.getBaseMaxHp() / 2);

        Character.CharacterData cd = original.toSaveData().character;
        Character restored = Character.fromSaveData(cd, context);

        assertEquals(original.getLevel(), restored.getLevel());
        assertEquals(original.getGold(), restored.getGold());
        assertEquals(original.getCurrentHp(), restored.getCurrentHp());
        assertEquals(original.getTalentPoints(), restored.getTalentPoints());
        assertEquals(original.getSkillPoints(), restored.getSkillPoints());
    }

    // ====================== roundtrip empty bag ======================

    @Test
    public void testRoundTrip_EmptyBag125Slots() {
        Character original = new Character(1, "空包", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = original.toSaveData().character;
        Character restored = Character.fromSaveData(cd, context);

        assertNotNull(restored.getBagItems());
        assertEquals(125, restored.getBagItems().size());
        for (int i = 0; i < 125; i++) {
            assertNull(restored.getBagItems().get(i));
        }
    }

    // ====================== saveData version ======================

    @Test
    public void testSaveData_VersionIsOne() {
        Character ch = new Character(1, "版本", ProfessionType.WARRIOR, context);
        Character.SaveData sd = ch.toSaveData();
        assertEquals(1, sd.version);
    }

    @Test
    public void testSaveData_TimestampInitiallyZero() {
        Character ch = new Character(1, "时间", ProfessionType.WARRIOR, context);
        Character.SaveData sd = ch.toSaveData();
        assertEquals(0, sd.timestamp);
    }

    @Test
    public void testSaveData_PlayTimeInitiallyZero() {
        Character ch = new Character(1, "时长", ProfessionType.WARRIOR, context);
        Character.SaveData sd = ch.toSaveData();
        assertEquals(0, sd.playTimeSeconds);
    }

    // ====================== fromSaveData large bag ======================

    @Test
    public void testRoundTrip_BagWithNullsAtVariousPositions() {
        Character original = new Character(1, "散装", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = original.toSaveData().character;
        cd.bagItems = new java.util.ArrayList<>(125);
        for (int i = 0; i < 125; i++) {
            cd.bagItems.add(null);
        }

        Character restored = Character.fromSaveData(cd, context);
        assertNotNull(restored);
        assertEquals(125, restored.getBagItems().size());
    }

    @Test
    public void testRoundTrip_BagSmallerThan125_FillsNulls() {
        Character original = new Character(1, "小包", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = original.toSaveData().character;
        cd.bagItems = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cd.bagItems.add(null);
        }

        Character restored = Character.fromSaveData(cd, context);
        assertEquals(125, restored.getBagItems().size());
    }

    // ====================== fromSaveData null equipped items ======================

    @Test
    public void testFromSaveData_NullEquippedItems() {
        Character.CharacterData cd = createMinimalData("裸装", "WARRIOR");
        cd.equippedItems = null;

        Character ch = Character.fromSaveData(cd, context);
        assertNotNull(ch);
    }

    // ====================== skill tree roundtrip ======================

    @Test
    public void testRoundTrip_SkillTreeData_NotNull() {
        Character ch = new Character(1, "技能", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = ch.toSaveData().character;

        assertNotNull(cd.activeSkillTree);
        assertNotNull(cd.passiveSkillTree);
        assertNotNull(cd.eventSkillTree);
        assertNotNull(cd.activeSkillTree.learnedSkillLevels);
        assertTrue(cd.activeSkillTree.learnedSkillLevels.isEmpty());
    }

    @Test
    public void testRoundTrip_LearnedSkill_Serialized() {
        Character ch = new Character(1, "技能玩家", ProfessionType.WARRIOR, context);
        SkillTree activeTree = ch.getProfession().getActiveSkillTree();
        if (activeTree.getAllSkillIds().size() > 0) {
            String skillId = activeTree.getAllSkillIds().get(0);
            Skill learned = com.example.treasure_and_battle.manager.skill.SkillManager
                    .getInstance(context).createSkillBySkillId(skillId, 1);
            activeTree.getAllLearnedSkills().add(learned);

            Character.CharacterData cd = ch.toSaveData().character;
            assertNotNull(cd.activeSkillTree);
            assertFalse(cd.activeSkillTree.learnedSkillLevels.isEmpty());
            assertTrue(cd.activeSkillTree.learnedSkillLevels.containsKey(skillId));
            assertEquals(Integer.valueOf(1), cd.activeSkillTree.learnedSkillLevels.get(skillId));
        }
    }

    // ====================== helper ======================

    private Character.CharacterData createMinimalData(String name, String professionType) {
        Character.CharacterData cd = new Character.CharacterData();
        cd.characterId = 1;
        cd.name = name;
        cd.professionType = professionType;
        cd.level = 1;
        cd.expToNextLevel = 100;
        cd.baseMaxHp = 20;
        cd.baseMaxMp = 10;
        cd.currentHp = 20;
        cd.currentMp = 10;
        cd.equippedItems = new java.util.LinkedHashMap<>();
        cd.bagItems = new java.util.ArrayList<>();
        return cd;
    }
}
