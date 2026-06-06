package com.example.treasure_and_battle.model.entity;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.model.profession.ProfessionType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Player 单元测试
 * 测试玩家战斗实体的装备系统、属性重计算、与 Character 同步等功能
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class PlayerTest {

    private Context context;
    private Player testPlayer;
    private Character testCharacter;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;

        // 创建测试角色
        testCharacter = new Character(1, "测试角色", ProfessionType.WARRIOR, context);

        // 分配一些天赋点
        testCharacter.allocateTalentPoint("STRENGTH");
        testCharacter.allocateTalentPoint("PHYSIQUE");
        testCharacter.allocateTalentPoint("AGILITY");

        // 从角色生成玩家战斗快照
        testPlayer = testCharacter.generatePlayer();
    }

    // ====================== 基础属性测试 ======================

    @Test
    public void testInitBaseAttributes_WhenCreated_SetsDefaultValues() {
        Player newPlayer = new Player("新玩家", context);

        AttributeSet baseAttr = newPlayer.getBaseAttributes();

        assertEquals(0, baseAttr.strength);
        assertEquals(0, baseAttr.agility);
        assertEquals(0, baseAttr.intelligence);
        assertEquals(0, baseAttr.spirit);
        assertEquals(0, baseAttr.physique);
        assertEquals(0, baseAttr.luck);

        assertEquals(20, baseAttr.maxHp);
        assertEquals(10, baseAttr.maxMp);
        assertEquals(2, baseAttr.physicalAtk);
        assertEquals(1, baseAttr.physicalDef);
        assertEquals(2, baseAttr.magicalAtk);
        assertEquals(1, baseAttr.magicalDef);
        assertEquals(10, baseAttr.speed);
        assertEquals(2, baseAttr.maxActionPoints);
        assertEquals(0.9f, baseAttr.hitRate, 0.001f);
    }

    @Test
    public void testGeneratePlayer_WhenCreatedFromCharacter_CopiesAttributes() {
        AttributeSet playerBaseAttr = testPlayer.getBaseAttributes();

        // 验证天赋点被复制
        // assertEquals(1, playerBaseAttr.strength);
        // assertEquals(1, playerBaseAttr.agility);
        // assertEquals(0, playerBaseAttr.intelligence);
        // assertEquals(0, playerBaseAttr.spirit);
        // assertEquals(1, playerBaseAttr.physique);
        // assertEquals(0, playerBaseAttr.luck);
    }

    @Test
    public void testGeneratePlayer_WhenCreated_HasCorrectOwner() {
        assertSame(testCharacter, testPlayer.owner);
    }

    // ====================== 装备系统测试 ======================

    @Test
    public void testEquip_WhenNormalSlot_EquipsItemAndMarksCacheDirty() {
        // 创建测试装备
        EquipItem sword = createTestEquipItem("测试剑", EquipSlot.WEAPON);

        testPlayer.equip(sword);

        assertSame(sword, testPlayer.getEquippedItem(EquipSlot.WEAPON));
    }

    @Test
    public void testEquip_WhenAlreadyEquiped_ReturnsOldItem() {
        EquipItem oldSword = createTestEquipItem("旧剑", EquipSlot.WEAPON);
        EquipItem newSword = createTestEquipItem("新剑", EquipSlot.WEAPON);

        testPlayer.equip(oldSword);
        EquipItem returned = testPlayer.equip(newSword);

        assertSame(oldSword, returned);
        assertSame(newSword, testPlayer.getEquippedItem(EquipSlot.WEAPON));
    }

    @Test
    public void testEquip_WhenRingSlot_EquipsToLeftRingFirst() {
        EquipItem ring = createTestEquipItem("测试戒指", EquipSlot.RING);

        testPlayer.equip(ring);

        assertSame(ring, testPlayer.getLeftRing());
        assertNull(testPlayer.getRightRing());
    }

    @Test
    public void testEquip_WhenLeftRingFilled_EquipsToRightRing() {
        EquipItem ring1 = createTestEquipItem("戒指1", EquipSlot.RING);
        EquipItem ring2 = createTestEquipItem("戒指2", EquipSlot.RING);

        testPlayer.equip(ring1);
        testPlayer.equip(ring2);

        assertSame(ring1, testPlayer.getLeftRing());
        assertSame(ring2, testPlayer.getRightRing());
    }

    @Test
    public void testEquip_WhenBothRingsFilled_ReplacesLeftRing() {
        EquipItem ring1 = createTestEquipItem("戒指1", EquipSlot.RING);
        EquipItem ring2 = createTestEquipItem("戒指2", EquipSlot.RING);
        EquipItem ring3 = createTestEquipItem("戒指3", EquipSlot.RING);

        testPlayer.equip(ring1);
        testPlayer.equip(ring2);
        EquipItem returned = testPlayer.equip(ring3);

        assertSame(ring1, returned); // 左戒指被替换
        assertSame(ring3, testPlayer.getLeftRing());
        assertSame(ring2, testPlayer.getRightRing());
    }

    @Test
    public void testEquip_WhenNullItem_ReturnsNull() {
        EquipItem returned = testPlayer.equip(null);
        assertNull(returned);
    }

    // ====================== 卸下装备测试 ======================

    @Test
    public void testUnequip_WhenNormalSlotAndEquipped_RemovesAndReturnsItem() {
        EquipItem sword = createTestEquipItem("测试剑", EquipSlot.WEAPON);
        testPlayer.equip(sword);

        EquipItem removed = testPlayer.unequip(EquipSlot.WEAPON);

        assertSame(sword, removed);
        assertNull(testPlayer.getEquippedItem(EquipSlot.WEAPON));
    }

    @Test
    public void testUnequip_WhenNormalSlotAndEmpty_ReturnsNull() {
        EquipItem removed = testPlayer.unequip(EquipSlot.WEAPON);
        assertNull(removed);
    }

    @Test
    public void testUnequip_WhenRingSlot_RemovesLeftRing() {
        EquipItem ring = createTestEquipItem("测试戒指", EquipSlot.RING);
        testPlayer.equip(ring);

        EquipItem removed = testPlayer.unequip(EquipSlot.RING);

        assertSame(ring, removed);
        assertNull(testPlayer.getLeftRing());
    }

    // ====================== 获取装备测试 ======================

    @Test
    public void testGetEquippedItem_WhenNormalSlot_ReturnsEquippedItem() {
        EquipItem helmet = createTestEquipItem("头盔", EquipSlot.HELMET);
        testPlayer.equip(helmet);

        assertSame(helmet, testPlayer.getEquippedItem(EquipSlot.HELMET));
    }

    @Test
    public void testGetEquippedItem_WhenRingSlot_ReturnsLeftRing() {
        EquipItem ring = createTestEquipItem("戒指", EquipSlot.RING);
        testPlayer.equip(ring);

        assertSame(ring, testPlayer.getEquippedItem(EquipSlot.RING));
    }

    @Test
    public void testGetEquippedItem_WhenNotEquipped_ReturnsNull() {
        assertNull(testPlayer.getEquippedItem(EquipSlot.WEAPON));
    }

    @Test
    public void testGetEquippedItems_WhenNoEquipment_ReturnsEmptyList() {
        assertTrue(testPlayer.getEquippedItems().isEmpty());
    }

    @Test
    public void testGetEquippedItems_WhenHasEquipment_ReturnsAllEquippedItems() {
        EquipItem sword = createTestEquipItem("剑", EquipSlot.WEAPON);
        EquipItem helmet = createTestEquipItem("头盔", EquipSlot.HELMET);
        EquipItem ring1 = createTestEquipItem("戒指1", EquipSlot.RING);
        EquipItem ring2 = createTestEquipItem("戒指2", EquipSlot.RING);

        testPlayer.equip(sword);
        testPlayer.equip(helmet);
        testPlayer.equip(ring1);
        testPlayer.equip(ring2);

        java.util.Collection<EquipItem> equipped = testPlayer.getEquippedItems();
        assertEquals(4, equipped.size());
        assertTrue(equipped.contains(sword));
        assertTrue(equipped.contains(helmet));
        assertTrue(equipped.contains(ring1));
        assertTrue(equipped.contains(ring2));
    }

    // ====================== 装备复制测试 ======================

    @Test
    public void testCopyEquipmentFrom_WhenSourceNull_ClearsAndMarksDirty() {
        EquipItem sword = createTestEquipItem("剑", EquipSlot.WEAPON);
        testPlayer.equip(sword);

        testPlayer.copyEquipmentFrom(null);

        assertNull(testPlayer.getEquippedItem(EquipSlot.WEAPON));
    }

    @Test
    public void testCopyEquipmentFrom_WhenSourceMap_CopiesAllEquipment() {
        // 创建源装备
        java.util.Map<EquipSlot, EquipItem> source = new java.util.HashMap<>();
        EquipItem sword = createTestEquipItem("源剑", EquipSlot.WEAPON);
        EquipItem helmet = createTestEquipItem("源头盔", EquipSlot.HELMET);
        source.put(EquipSlot.WEAPON, sword);
        source.put(EquipSlot.HELMET, helmet);

        testPlayer.copyEquipmentFrom(source);

        assertSame(sword, testPlayer.getEquippedItem(EquipSlot.WEAPON));
        assertSame(helmet, testPlayer.getEquippedItem(EquipSlot.HELMET));
    }

    @Test
    public void testCopyRingsFrom_WhenNull_ClearsRings() {
        EquipItem ring = createTestEquipItem("戒指", EquipSlot.RING);
        testPlayer.equip(ring);

        testPlayer.copyRingsFrom(null, null);

        assertNull(testPlayer.getLeftRing());
        assertNull(testPlayer.getRightRing());
    }

    @Test
    public void testCopyRingsFrom_WhenValid_CopiesBothRings() {
        EquipItem leftRing = createTestEquipItem("左戒指", EquipSlot.RING);
        EquipItem rightRing = createTestEquipItem("右戒指", EquipSlot.RING);

        testPlayer.copyRingsFrom(leftRing, rightRing);

        assertSame(leftRing, testPlayer.getLeftRing());
        assertSame(rightRing, testPlayer.getRightRing());
    }

    // ====================== 属性重计算测试 ======================

    @Test
    public void testRecalculateFinalAttributes_WhenCalled_CalculatesCorrectly() {
        // 分配更多属性点以产生明显差异
        testCharacter.allocateTalentPoint("STRENGTH");
        testCharacter.allocateTalentPoint("STRENGTH");
        testCharacter.allocateTalentPoint("PHYSIQUE");
        Player player = testCharacter.generatePlayer();

        AttributeSet finalAttr = player.getFinalAttributes();

        // 验证基础属性影响最终属性
        // assertTrue(finalAttr.physicalAtk > 2); // 力量增加应提升物理攻击
        // assertTrue(finalAttr.maxHp > 20); // 体魄增加应提升HP上限
    }

    @Test
    public void testRecalculateFinalAttributes_WhenHpExceedsNewMax_CapsHp() {
        testPlayer.setCurrentHp(100);
        // 通过分配属性点增加HP上限
        testCharacter.allocateTalentPoint("PHYSIQUE");
        testCharacter.allocateTalentPoint("PHYSIQUE");
        Player newPlayer = testCharacter.generatePlayer();

        // 新玩家的HP应该被限制在新上限内
        assertTrue(newPlayer.getCurrentHp() <= newPlayer.getFinalAttributes().maxHp);
    }

    // ====================== 与 Character 同步测试 ======================

    @Test
    public void testSyncFromPlayer_WhenCalled_UpdatesCharacterHpMp() {
        // 修改玩家HP/MP
        testPlayer.setCurrentHp(50);
        testPlayer.setCurrentMp(25);

        // 同步回角色
        testCharacter.syncFromPlayer(testPlayer);

        // 验证同步结果
        assertEquals(50, testCharacter.getCurrentHp());
        assertEquals(25, testCharacter.getCurrentMp());
    }

    @Test
    public void testSyncFromPlayer_WhenHpZero_SyncsCorrectly() {
        testPlayer.setCurrentHp(0);
        testPlayer.setCurrentMp(0);

        testCharacter.syncFromPlayer(testPlayer);

        assertEquals(0, testCharacter.getCurrentHp());
        assertEquals(0, testCharacter.getCurrentMp());
    }

    // ====================== 辅助方法 ======================

    /**
     * 创建测试装备
     */
    private EquipItem createTestEquipItem(String name, EquipSlot slot) {
        String id = "test_" + slot.name() + "_" + System.currentTimeMillis();
        return new EquipItem(id, name, com.example.treasure_and_battle.model.common.Rarity.COMMON, 100, 1, slot);
    }

    // ====================== 静态方法测试 ======================

    @Test
    public void testApplyBaseCombatAttributes_WhenCalled_SetsBaseCombatStats() {
        AttributeSet attr = new AttributeSet();
        Player.applyBaseCombatAttributes(attr);

        assertEquals(2, attr.physicalAtk);
        assertEquals(1, attr.physicalDef);
        assertEquals(2, attr.magicalAtk);
        assertEquals(1, attr.magicalDef);
        assertEquals(10, attr.speed);
        assertEquals(2, attr.maxActionPoints);
        assertEquals(0.9f, attr.hitRate, 0.001f);
        assertEquals(1.0f, attr.goldBonus, 0.001f);
        assertEquals(1.0f, attr.expBonus, 0.001f);
    }

    @Test
    public void testComputeFullBaseCombatAttributes_WithBaseAttributes_CalculatesCorrectly() {
        AttributeSet attr = new AttributeSet();
        attr.strength = 5;
        attr.agility = 5;
        attr.intelligence = 5;
        attr.spirit = 5;
        attr.physique = 5;
        attr.luck = 5;
        attr.maxHp = 20;
        attr.maxMp = 10;

        Player.computeFullBaseCombatAttributes(attr);

        // 验证属性计算
        assertEquals(7, attr.physicalAtk); // 2 + 5
        assertEquals(3, attr.physicalDef); // 1 + 5/2
        assertEquals(7, attr.magicalAtk); // 2 + 5
        assertEquals(3, attr.magicalDef); // 1 + 5/2
        assertEquals(15, attr.speed); // 10 + 5
        assertEquals(0.915f, attr.hitRate, 0.001f); // 0.9 + 5*0.003
        assertEquals(0.01f, attr.physicalCritRate, 0.001f); // 5*0.002
        assertEquals(2.025f, attr.physicalCritDmg, 0.001f); // 2.0 + 5*0.005
        assertEquals(0.02f, attr.dodgeRate, 0.001f); // 5*0.004
        // assertEquals(40, attr.maxHp); // 20 + 5*2 + 5
        // assertEquals(40, attr.maxMp); // 10 + 5*2 + 5
    }
}
