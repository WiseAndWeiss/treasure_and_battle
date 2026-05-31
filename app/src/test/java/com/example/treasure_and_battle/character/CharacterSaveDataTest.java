package com.example.treasure_and_battle.character;

import android.content.Context;

import com.example.treasure_and_battle.profession.ProfessionType;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.SkillTree;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class CharacterSaveDataTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
    }

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
        assertEquals(0, cd.gold);
        assertEquals(20, cd.currentHp);
        assertEquals(10, cd.currentMp);
    }

    @Test
    public void testToSaveData_LeveledCharacter() {
        Character ch = new Character(1, "老手", ProfessionType.MAGE, context);
        ch.gainExp(ch.getExpToNextLevel());
        ch.gainExp(ch.getExpToNextLevel());
        Character.CharacterData cd = ch.toSaveData().character;
        assertEquals(3, cd.level);
        assertTrue(cd.talentPoints >= 10);
        assertTrue(cd.skillPoints >= 2);
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
    public void testToSaveData_EmptyBag() {
        Character ch = new Character(1, "空包", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = ch.toSaveData().character;
        assertNotNull(cd.bagItems);
        assertEquals(125, cd.bagItems.size());
    }

    @Test
    public void testToSaveData_SkillTrees_InitiallyEmpty() {
        Character ch = new Character(1, "技能新手", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = ch.toSaveData().character;
        assertNotNull(cd.activeSkillTree);
        assertNotNull(cd.passiveSkillTree);
        assertNotNull(cd.eventSkillTree);
        assertTrue(cd.activeSkillTree.learnedSkillLevels.isEmpty());
    }

    @Test
    public void testFromSaveData_RestoresBasicFields() {
        Character.CharacterData cd = createMinimalData("英雄", "MAGE");
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

    @Test
    public void testFromSaveData_InvalidProfession_FallsBackToWarrior() {
        Character.CharacterData cd = createMinimalData("异类", "INVALID_PROFESSION");
        Character ch = Character.fromSaveData(cd, context);
        assertNotNull(ch);
        assertEquals(ProfessionType.WARRIOR, ch.getProfessionType());
    }

    @Test
    public void testRoundTrip_BasicCharacter() {
        Character original = new Character(1, "往返测试", ProfessionType.WARRIOR, context);
        Character.CharacterData cd = original.toSaveData().character;
        Character restored = Character.fromSaveData(cd, context);
        assertNotNull(restored);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getProfessionType(), restored.getProfessionType());
        assertEquals(original.getLevel(), restored.getLevel());
    }

    @Test
    public void testRoundTrip_LeveledCharacter() {
        Character original = new Character(1, "练级", ProfessionType.MAGE, context);
        original.gainExp(original.getExpToNextLevel() * 5);
        original.addGold(2000);
        Character.CharacterData cd = original.toSaveData().character;
        Character restored = Character.fromSaveData(cd, context);
        assertEquals(original.getLevel(), restored.getLevel());
        assertEquals(original.getGold(), restored.getGold());
        assertEquals(original.getTalentPoints(), restored.getTalentPoints());
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
        Character original = new Character(1, "天赋师", ProfessionType.WARRIOR, context);
        original.gainExp(original.getExpToNextLevel() * 50);
        assertTrue("应有足够天赋点", original.getTalentPoints() >= 7);
        assertTrue(original.allocateTalentPoint("STRENGTH"));
        assertTrue(original.allocateTalentPoint("AGILITY"));
        assertTrue(original.allocateTalentPoint("INTELLIGENCE"));
        assertTrue(original.allocateTalentPoint("SPIRIT"));
        assertTrue(original.allocateTalentPoint("PHYSIQUE"));
        assertTrue(original.allocateTalentPoint("LUCK"));
        assertTrue(original.allocateTalentPoint("STRENGTH"));

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
    public void testSaveData_VersionIsOne() {
        Character ch = new Character(1, "版本", ProfessionType.WARRIOR, context);
        assertEquals(1, ch.toSaveData().version);
    }

    private Character.CharacterData createMinimalData(String name, String professionType) {
        Character.CharacterData cd = new Character.CharacterData();
        cd.characterId = 2;
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
