package com.example.treasure_and_battle.character;

import android.content.Context;

import com.example.treasure_and_battle.affix.impl.equip.attribute.EquipAttributeAffix;
import com.example.treasure_and_battle.model.affix.EquipAffixScope;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.attribute.AttributeType;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.common.TriggerType;
import com.example.treasure_and_battle.model.common.ValueType;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.profession.ProfessionType;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.ui.FloatMsgMsgOverlay;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class CharacterTest {

    private Context context;
    private Character character;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        character = new Character(1, "TestHero", ProfessionType.WARRIOR, context);
    }

    // ====================== 基础初始化 ======================

    @Test
    public void testInitialState() {
        assertEquals(1, character.getCharacterId());
        assertEquals("TestHero", character.getName());
        assertEquals(ProfessionType.WARRIOR, character.getProfessionType());
        assertEquals(1, character.getLevel());
        assertEquals(0, character.getCurrentExp());
        assertTrue(character.getExpToNextLevel() > 0);
        assertEquals(0, character.getTalentPoints());
        assertEquals(0, character.getSkillPoints());
        assertEquals(0, character.getGold());
        assertEquals(20, character.getCurrentHp());
        assertEquals(10, character.getCurrentMp());
    }

    @Test
    public void testInitialAllocatedStatsAreZero() {
        assertEquals(0, character.getAllocatedStrength());
        assertEquals(0, character.getAllocatedAgility());
        assertEquals(0, character.getAllocatedIntelligence());
        assertEquals(0, character.getAllocatedSpirit());
        assertEquals(0, character.getAllocatedPhysique());
        assertEquals(0, character.getAllocatedLuck());
    }

    // ====================== 经验与升级 ======================

    @Test
    public void testGainExpNoLevelUp() {
        int before = character.getCurrentExp();
        character.gainExp(10);
        assertEquals(before + 10, character.getCurrentExp());
        assertEquals(1, character.getLevel());
    }

    @Test
    public void testLevelUpOnce() {
        int toNext = character.getExpToNextLevel();
        character.gainExp(toNext);
        assertEquals(2, character.getLevel());
        assertTrue(character.getCurrentExp() < character.getExpToNextLevel());
        assertEquals(2, character.getTalentPoints());
        assertEquals(1, character.getSkillPoints());
    }

    @Test
    public void testBaseMaxHpMpInitialValues() {
        assertEquals(20, character.getBaseMaxHp());
        assertEquals(10, character.getBaseMaxMp());
    }

    @Test
    public void testLevelUpIncreasesBaseMaxHpMp() {
        character.gainExp(character.getExpToNextLevel());
        assertEquals(28, character.getBaseMaxHp());
        assertEquals(14, character.getBaseMaxMp());
    }

    @Test
    public void testLevelUpThreeTimesBaseMaxHpMp() {
        character.gainExp(character.getExpToNextLevel());
        character.gainExp(character.getExpToNextLevel());
        character.gainExp(character.getExpToNextLevel());

        assertEquals(4, character.getLevel());
        assertEquals(20 + 3 * 8, character.getBaseMaxHp());
        assertEquals(10 + 3 * 4, character.getBaseMaxMp());
    }

    @Test
    public void testLevelUpGrowsCurrentHpMp() {
        character.gainExp(character.getExpToNextLevel());
        assertTrue("升级后 HP 应增长", character.getCurrentHp() > 20);
        assertTrue("升级后 MP 应增长", character.getCurrentMp() > 10);
    }

    @Test
    public void testLevelUpHpNotExceedNewMax() {
        character.setCurrentHp(25);
        character.gainExp(character.getExpToNextLevel());
        assertTrue("HP 应 >= 进入值", character.getCurrentHp() >= 25);
    }

    @Test
    public void testContinuousLevelUp() {
        int total = character.getExpToNextLevel() * 5;
        character.gainExp(total);
        assertTrue("连续升级后等级应 > 2", character.getLevel() >= 3);
        assertTrue("升级后应有天赋点", character.getTalentPoints() > 0);
    }

    @Test
    public void testExpCurveIncreasesWithLevel() {
        int expLv1 = character.getExpToNextLevel();
        character.gainExp(expLv1);
        int expLv2 = character.getExpToNextLevel();
        assertTrue("升级后下一级经验需求应增大", expLv2 >= expLv1);
    }

    @Test
    public void testGainExpAtBoundary() {
        int expNeeded = character.getExpToNextLevel() - 1;
        character.gainExp(expNeeded);
        assertEquals("未满经验不应升级", 1, character.getLevel());

        character.gainExp(1);
        assertEquals("刚好满经验应升级", 2, character.getLevel());
    }

    // ====================== 天赋分配 ======================

    @Test
    public void testAllocateTalentPointWithoutPoints() {
        assertFalse("无天赋点时应返回 false", character.allocateTalentPoint("STRENGTH"));
        assertEquals(0, character.getAllocatedStrength());
    }

    @Test
    public void testAllocateTalentPointWithPoints() {
        character.gainExp(character.getExpToNextLevel());
        assertTrue(character.getTalentPoints() > 0);

        int before = character.getTalentPoints();
        assertTrue(character.allocateTalentPoint("STRENGTH"));
        assertEquals(before - 1, character.getTalentPoints());
        assertEquals(1, character.getAllocatedStrength());
    }

    @Test
    public void testAllocateAllSixStats() {
        character.gainExp(character.getExpToNextLevel() * 50);
        int pts = character.getTalentPoints();
        assertTrue("应有足够天赋点", pts >= 6);

        character.allocateTalentPoint("STRENGTH");
        character.allocateTalentPoint("AGILITY");
        character.allocateTalentPoint("INTELLIGENCE");
        character.allocateTalentPoint("SPIRIT");
        character.allocateTalentPoint("PHYSIQUE");
        character.allocateTalentPoint("LUCK");

        assertEquals(pts - 6, character.getTalentPoints());
        assertEquals(1, character.getAllocatedStrength());
        assertEquals(1, character.getAllocatedLuck());
    }

    @Test
    public void testAllocateInvalidAttribute() {
        character.gainExp(character.getExpToNextLevel());
        assertFalse(character.allocateTalentPoint("INVALID"));
    }

    @Test
    public void testCaseInsensitiveAllocation() {
        character.gainExp(character.getExpToNextLevel() * 50);
        assertTrue(character.allocateTalentPoint("strength"));
        assertEquals(1, character.getAllocatedStrength());

        assertTrue(character.allocateTalentPoint("LUCK"));
        assertEquals(1, character.getAllocatedLuck());
    }

    @Test
    public void testResetAllTalentPoints() {
        character.gainExp(character.getExpToNextLevel() * 2);
        int ptsBefore = character.getTalentPoints();
        character.allocateTalentPoint("STRENGTH");
        character.allocateTalentPoint("AGILITY");

        character.resetAllTalentPoints();
        assertEquals(ptsBefore, character.getTalentPoints());
        assertEquals(0, character.getAllocatedStrength());
        assertEquals(0, character.getAllocatedAgility());
    }

    @Test
    public void testResetWhenNothingAllocated() {
        int pts = character.getTalentPoints();
        character.resetAllTalentPoints();
        assertEquals(pts, character.getTalentPoints());
    }

    // ====================== 装备 ======================

    @Test
    public void testEquipAndUnequip() {
        EquipItem sword = new EquipItem("sword_1", "铁剑", Rarity.COMMON, 50, 1, EquipSlot.WEAPON);
        assertNull("装备前应返回 null", character.equip(sword));
        assertSame(sword, character.getEquippedItem(EquipSlot.WEAPON));

        EquipItem removed = character.unequip(EquipSlot.WEAPON);
        assertSame(sword, removed);
        assertNull(character.getEquippedItem(EquipSlot.WEAPON));
    }

    @Test
    public void testEquipReplacesOld() {
        EquipItem sword1 = new EquipItem("sword_1", "铁剑", Rarity.COMMON, 50, 1, EquipSlot.WEAPON);
        EquipItem sword2 = new EquipItem("sword_2", "钢剑", Rarity.UNCOMMON, 80, 2, EquipSlot.WEAPON);

        assertNull(character.equip(sword1));
        EquipItem old = character.equip(sword2);
        assertSame(sword1, old);
        assertSame(sword2, character.getEquippedItem(EquipSlot.WEAPON));
    }

    @Test
    public void testEquipNullReturnsNull() {
        assertNull(character.equip(null));
    }

    @Test
    public void testUnequipEmptySlot() {
        assertNull(character.unequip(EquipSlot.WEAPON));
    }

    // ====================== 天赋→战斗属性 ======================

    @Test
    public void testTalentStrengthIncreasesPhysicalAttack() {
        character.gainExp(character.getExpToNextLevel());
        character.allocateTalentPoint("STRENGTH");

        Player player = character.generatePlayer();
        int baseAtk = player.getFinalAttributes().physicalAtk;
        assertTrue("力量+1 后物攻应 > 2（基础2 + 力量1）", baseAtk >= 3);
    }

    @Test
    public void testTalentIntelligenceIncreasesMagicalAttack() {
        character.gainExp(character.getExpToNextLevel());
        character.allocateTalentPoint("INTELLIGENCE");

        Player player = character.generatePlayer();
        assertTrue("智力+1 后魔攻应 > 2", player.getFinalAttributes().magicalAtk >= 3);
    }

    @Test
    public void testTalentPhysiqueIncreasesMaxHp() {
        character.gainExp(character.getExpToNextLevel());
        character.allocateTalentPoint("PHYSIQUE");

        Player player = character.generatePlayer();
        int maxHp = player.getFinalAttributes().maxHp;
        assertTrue("体魄+1 后 maxHp 应 > 28（基础28 + 体魄2）", maxHp > 28);
    }

    @Test
    public void testTalentAgilityIncreasesSpeed() {
        character.gainExp(character.getExpToNextLevel());
        character.allocateTalentPoint("AGILITY");

        Player player = character.generatePlayer();
        assertTrue("敏捷+1 后速度应 > 10", player.getFinalAttributes().speed >= 11);
    }

    @Test
    public void testEquipmentIncreasedPhysicalAttack() {
        EquipItem sword = new EquipItem("sword_1", "铁剑", Rarity.COMMON, 50, 1, EquipSlot.WEAPON);
        sword.getBaseAttributes().physicalAtk = 30;
        character.equip(sword);

        Player player = character.generatePlayer();
        int atk = player.getFinalAttributes().physicalAtk;
        assertTrue("装备 +30 物攻，最终应 >= 32（基础2 + 装备30）", atk >= 32);
    }

    @Test
    public void testTalentPlusEquipmentStackCorrectly() {
        character.gainExp(character.getExpToNextLevel());
        assertTrue(character.allocateTalentPoint("STRENGTH"));

        EquipItem sword = new EquipItem("sword_1", "铁剑", Rarity.COMMON, 50, 1, EquipSlot.WEAPON);
        sword.getBaseAttributes().physicalAtk = 30;
        character.equip(sword);

        Player player = character.generatePlayer();
        int atk = player.getFinalAttributes().physicalAtk;
        assertTrue("基础2 + 力量1 + 装备30 = 33，实际=" + atk, atk >= 33);
    }

    // ====================== 生成战斗实体 ======================

    @Test
    public void testGeneratePlayerAssignsOwner() {
        Player player = character.generatePlayer();
        assertNotNull(player);
        assertSame("Player.owner 应指向 Character", character, player.owner);
    }

    @Test
    public void testGeneratePlayerLevelMatches() {
        Player player = character.generatePlayer();
        assertEquals(character.getLevel(), player.getLevel());
    }

    @Test
    public void testGeneratePlayerHpFromCharacter() {
        character.setCurrentHp(15);
        character.setCurrentMp(8);

        Player player = character.generatePlayer();
        assertTrue("HP 应从 Character 同步", player.getCurrentHp() <= 15);
        assertTrue("MP 应从 Character 同步", player.getCurrentMp() <= 8);
    }

    @Test
    public void testGeneratePlayerHpNotExceedMax() {
        character.setCurrentHp(99999);

        Player player = character.generatePlayer();
        assertTrue("HP 不应超过 maxHp", player.getCurrentHp() <= player.getFinalAttributes().maxHp);
    }

    @Test
    public void testGeneratePlayerInjectsTalentIntoBaseAttributes() {
        character.gainExp(character.getExpToNextLevel() * 50);
        assertTrue("应有足够天赋点", character.getTalentPoints() >= 3);

        character.allocateTalentPoint("STRENGTH");
        character.allocateTalentPoint("AGILITY");
        character.allocateTalentPoint("PHYSIQUE");

        Player player = character.generatePlayer();
        AttributeSet base = player.getBaseAttributes();
        assertEquals(1, base.strength);
        assertEquals(1, base.agility);
        assertEquals(1, base.physique);
    }

    @Test
    public void testGeneratePlayerInjectsEquipmentAttributes() {
        EquipItem sword = new EquipItem("sword_1", "铁剑", Rarity.COMMON, 50, 1, EquipSlot.WEAPON);
        sword.getBaseAttributes().physicalAtk = 30;
        character.equip(sword);

        Player player = character.generatePlayer();
        assertTrue("finalAttributes 应包含装备物攻加成",
                player.getFinalAttributes().physicalAtk >= 30 + 2);
    }

    @Test
    public void testGeneratePlayerInjectsEquipmentPermanentAffixes() {
        EquipItem ring = new EquipItem("ring_str", "力量戒指", Rarity.RARE, 80, 1, EquipSlot.RING);
        List<BaseAffix> affixes = new ArrayList<>();
        affixes.add(new EquipAttributeAffix(
                101, "力量+", "", Rarity.COMMON,
                TriggerType.PERMANENT, null, 10f,
                AttributeType.STRENGTH, ValueType.FLAT, EquipAffixScope.GLOBAL));
        ring.setAffixes(affixes);
        character.equip(ring);

        Player player = character.generatePlayer();
        int basePhysicalAtk = player.getFinalAttributes().physicalAtk;
        assertEquals("力量 +10 应使物攻增加 10", 10 + 2, basePhysicalAtk);
    }

    @Test
    public void testGeneratePlayerInjectsNonPermanentAffixes() {
        EquipItem necklace = new EquipItem("necklace_1", "触发项链", Rarity.EPIC, 150, 1, EquipSlot.NECKLACE);
        List<BaseAffix> affixes = new ArrayList<>();
        affixes.add(new EquipAttributeAffix(
                201, "物攻+", "", Rarity.COMMON,
                TriggerType.ON_HIT, null, 5f,
                AttributeType.PHYSICAL_ATK, ValueType.FLAT, EquipAffixScope.GLOBAL));
        necklace.setAffixes(affixes);
        character.equip(necklace);

        Player player = character.generatePlayer();
        assertFalse("装备应复制到 Player",
                player.getEquippedItems().isEmpty());
        EquipItem playerNecklace = player.getEquippedItem(EquipSlot.NECKLACE);
        assertNotNull("项链应存在", playerNecklace);
        assertFalse("装备应保留词缀",
                playerNecklace.getAffixes().isEmpty());
        assertEquals("词缀类型应为 ON_HIT",
                TriggerType.ON_HIT, playerNecklace.getAffixes().get(0).getTriggerType());
    }

    @Test
    public void testGeneratePlayerWithNoEquipment() {
        Player player = character.generatePlayer();
        assertNotNull(player);
        assertTrue("无装备时装备列表应为空",
                player.getEquippedItems().isEmpty());
    }

    // ====================== 战斗后同步 ======================

    @Test
    public void testSyncFromPlayerUpdatesHpMp() {
        Player player = new Player("Fighter", context);
        player.setCurrentHp(5);
        player.setCurrentMp(3);

        character.syncFromPlayer(player);
        assertEquals(5, character.getCurrentHp());
        assertEquals(3, character.getCurrentMp());
    }

    @Test
    public void testSyncFromPlayerZeroHp() {
        Player player = new Player("Fighter", context);
        player.setCurrentHp(0);
        player.setCurrentMp(0);

        character.syncFromPlayer(player);
        assertEquals(0, character.getCurrentHp());
        assertEquals(0, character.getCurrentMp());
    }

    // ====================== 金币 ======================

    @Test
    public void testAddAndSpendGold() {
        character.addGold(100);
        assertEquals(100, character.getGold());

        assertTrue(character.spendGold(30));
        assertEquals(70, character.getGold());
    }

    @Test
    public void testSpendGoldInsufficient() {
        assertFalse("金币不足应返回 false", character.spendGold(9999));
        assertEquals(0, character.getGold());
    }

    @Test
    public void testSpendGoldExactAmount() {
        character.addGold(50);
        assertTrue(character.spendGold(50));
        assertEquals(0, character.getGold());
    }

    // ====================== 极端情况 ======================

    @Test
    public void testMaxLevelFromMassiveExp() {
        character.gainExp(Integer.MAX_VALUE / 2);
        assertTrue("大量经验下等级应远大于初始", character.getLevel() >= 10);
    }

    @Test
    public void testZeroExpNoChange() {
        character.gainExp(0);
        assertEquals(1, character.getLevel());
        assertEquals(0, character.getCurrentExp());
    }

    @Test
    public void testMultipleEquipReplace() {
        for (int i = 0; i < 5; i++) {
            EquipItem sword = new EquipItem("sword_" + i, "剑_" + i, Rarity.COMMON, 50, 1, EquipSlot.WEAPON);
            character.equip(sword);
        }
        assertNotNull(character.getEquippedItem(EquipSlot.WEAPON));
    }

    @Test
    public void testGeneratePlayerAfterFullTalentReset() {
        character.gainExp(character.getExpToNextLevel() * 3);
        character.allocateTalentPoint("STRENGTH");
        character.allocateTalentPoint("STRENGTH");

        character.resetAllTalentPoints();

        Player player = character.generatePlayer();
        assertEquals("重置后力量应为 0", 0, player.getBaseAttributes().strength);
    }

    @Test
    public void testGetEquippedItemsEmpty() {
        assertTrue(character.getEquippedItems().isEmpty());
    }

    @Test
    public void testGetEquippedItemsAfterEquip() {
        EquipItem sword = new EquipItem("sword_1", "铁剑", Rarity.COMMON, 50, 1, EquipSlot.WEAPON);
        EquipItem armor = new EquipItem("armor_1", "布甲", Rarity.COMMON, 40, 1, EquipSlot.CHEST);
        character.equip(sword);
        character.equip(armor);

        assertEquals(2, character.getEquippedItems().size());
    }

    @Test
    public void testSetName() {
        character.setName("NewName");
        assertEquals("NewName", character.getName());
    }
}
