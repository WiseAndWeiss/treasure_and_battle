package com.example.treasure_and_battle.ui.menu;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.model.item.material.MaterialItem;
import com.example.treasure_and_battle.profession.ProfessionType;
import com.example.treasure_and_battle.ui.PlayerCharacterHolder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ItemMenuProviderTest {

    private Context context;
    private Character testCharacter;
    private Consumer<Item> viewCallback;
    private Consumer<Item> discardCallback;
    private Consumer<EquipItem> equipCallback;
    private Consumer<ConsumableItem> useCallback;
    private AtomicReference<Item> lastViewed;
    private AtomicReference<Item> lastDiscarded;
    private AtomicReference<EquipItem> lastEquipped;
    private AtomicReference<ConsumableItem> lastUsed;
    private AtomicInteger discardConfirmCount;
    private AtomicInteger equipCallCount;
    private AtomicInteger useCallCount;

    private EquipItem testEquipItem;
    private ConsumableItem testConsumableOutBattle;
    private ConsumableItem testConsumableBattleOnly;
    private GemItem testGemItem;
    private MaterialItem testMaterialItem;

    @Before
    public void setUp() {
        context = mock(Context.class);
        android.app.Application mockApp = mock(android.app.Application.class);
        when(context.getApplicationContext()).thenReturn(mockApp);
        try {
            testCharacter = new Character(99, "测试角色", ProfessionType.WARRIOR, context);
            PlayerCharacterHolder.setForTesting(testCharacter);
        } catch (Exception e) {
            testCharacter = null;
        }

        lastViewed = new AtomicReference<>();
        lastDiscarded = new AtomicReference<>();
        lastEquipped = new AtomicReference<>();
        lastUsed = new AtomicReference<>();
        discardConfirmCount = new AtomicInteger(0);
        equipCallCount = new AtomicInteger(0);
        useCallCount = new AtomicInteger(0);

        viewCallback = lastViewed::set;
        discardCallback = item -> {
            lastDiscarded.set(item);
            discardConfirmCount.incrementAndGet();
        };
        equipCallback = item -> {
            lastEquipped.set(item);
            equipCallCount.incrementAndGet();
        };
        useCallback = item -> {
            lastUsed.set(item);
            useCallCount.incrementAndGet();
        };

        testEquipItem = new EquipItem("eq_test", "测试剑", Rarity.COMMON, 100, 1, EquipSlot.WEAPON);

        List<ConsumableItem.Effect> effects = new ArrayList<>();
        ConsumableItem.Effect healEffect = new ConsumableItem.Effect(ConsumableItem.EffectType.HEAL_HP);
        healEffect.value = 50;
        healEffect.valueType = "FLAT";
        effects.add(healEffect);

        testConsumableOutBattle = new ConsumableItem("con_out", "万能药",
                Rarity.COMMON, 50, 99, true, true, effects, "可在局内局外使用");
        testConsumableBattleOnly = new ConsumableItem("con_battle", "战斗药",
                Rarity.COMMON, 50, 99, true, false, effects, "仅战斗中使用");

        testGemItem = new GemItem("gem_test", "红宝石", Rarity.COMMON, 200, "RUBY");
        testMaterialItem = new MaterialItem("mat_test", "铁矿石", Rarity.COMMON, 10, 99, "矿洞"); 
    }

    @After
    public void tearDown() {
        PlayerCharacterHolder.clear();
    }

    // ====================== ItemAction 测试 ======================

    @Test
    public void testItemAction_Enabled_NoDisabledHint() {
        ItemAction action = new ItemAction("装备", true, item -> {});
        assertEquals("装备", action.name);
        assertTrue(action.enabled);
        assertEquals("装备", action.getDisplayText());
        assertNotNull(action.action);
    }

    @Test
    public void testItemAction_Disabled_NoDisabledHint() {
        ItemAction action = new ItemAction("装备", false, item -> {});
        assertEquals("装备", action.name);
        assertFalse(action.enabled);
        assertEquals("装备", action.getDisplayText());
    }

    @Test
    public void testItemAction_Disabled_WithDisabledHint() {
        ItemAction action = new ItemAction("使用", false, null, "(局外禁用)");
        assertEquals("使用", action.name);
        assertFalse(action.enabled);
        assertEquals("使用(局外禁用)", action.getDisplayText());
    }

    @Test
    public void testItemAction_Enabled_DisabledHintIgnored() {
        ItemAction action = new ItemAction("使用", true, item -> {}, "(局外禁用)");
        assertTrue(action.enabled);
        assertEquals("使用", action.getDisplayText());
    }

    @Test
    public void testItemAction_Disabled_EmptyDisabledHint() {
        ItemAction action = new ItemAction("测试", false, null, "");
        assertEquals("测试", action.getDisplayText());
    }

    @Test
    public void testItemAction_DefaultConstructor_NoHint() {
        ItemAction action = new ItemAction("查看", true, item -> {});
        assertEquals("查看", action.getDisplayText());
    }

    @Test
    public void testItemAction_ActionExecution() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Item item = testEquipItem;
        ItemAction action = new ItemAction("测试", true, i -> {
            assertEquals(item, i);
            executed.set(true);
        });
        action.action.accept(item);
        assertTrue(executed.get());
    }

    @Test
    public void testItemAction_NullAction() {
        ItemAction action = new ItemAction("镶嵌(TODO)", false, null, "(未开放)");
        assertNull(action.action);
        assertFalse(action.enabled);
        assertEquals("镶嵌(TODO)(未开放)", action.getDisplayText());
    }

    // ====================== EquipmentMenuProvider 测试 ======================

    @Test
    public void testEquipmentMenuProvider_ActionCount() {
        EquipmentMenuProvider provider = new EquipmentMenuProvider(
                equipCallback, null, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testEquipItem);
        assertEquals(3, actions.size());
    }

    @Test
    public void testEquipmentMenuProvider_FirstActionIsEquip() {
        EquipmentMenuProvider provider = new EquipmentMenuProvider(
                equipCallback, null, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testEquipItem);
        assertEquals("装备", actions.get(0).name);
    }

    @Test
    public void testEquipmentMenuProvider_EquipAlwaysEnabled() {
        EquipmentMenuProvider provider = new EquipmentMenuProvider(
                equipCallback, null, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testEquipItem);
        assertTrue("装备按钮应始终可用", actions.get(0).enabled);
    }

    @Test
    public void testEquipmentMenuProvider_EquipActionTriggersCallback() {
        EquipmentMenuProvider provider = new EquipmentMenuProvider(
                equipCallback, null, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testEquipItem);
        actions.get(0).action.accept(testEquipItem);
        assertEquals(testEquipItem, lastEquipped.get());
        assertEquals(1, equipCallCount.get());
    }

    @Test
    public void testEquipmentMenuProvider_ViewAction() {
        EquipmentMenuProvider provider = new EquipmentMenuProvider(
                equipCallback, null, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testEquipItem);
        assertEquals("查看", actions.get(1).name);
        assertTrue(actions.get(1).enabled);
        actions.get(1).action.accept(testEquipItem);
        assertEquals(testEquipItem, lastViewed.get());
    }

    @Test
    public void testEquipmentMenuProvider_DiscardAction() {
        EquipmentMenuProvider provider = new EquipmentMenuProvider(
                equipCallback, null, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testEquipItem);
        assertEquals("丢弃", actions.get(2).name);
        assertTrue(actions.get(2).enabled);
        actions.get(2).action.accept(testEquipItem);
        assertEquals(testEquipItem, lastDiscarded.get());
    }

    @Test
    public void testEquipmentMenuProvider_ActionOrder() {
        EquipmentMenuProvider provider = new EquipmentMenuProvider(
                equipCallback, null, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testEquipItem);
        assertEquals("装备", actions.get(0).name);
        assertEquals("查看", actions.get(1).name);
        assertEquals("丢弃", actions.get(2).name);
    }

    // ====================== ConsumableMenuProvider 测试 ======================

    @Test
    public void testConsumableMenuProvider_ActionCount() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testConsumableOutBattle);
        assertEquals(3, actions.size());
    }

    @Test
    public void testConsumableMenuProvider_FirstActionIsUse() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testConsumableOutBattle);
        assertEquals("使用", actions.get(0).name);
    }

    @Test
    public void testConsumableMenuProvider_UseEnabledWhenOutBattleAllowed() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testConsumableOutBattle);
        assertTrue("局外可用时使用按钮应可用", actions.get(0).enabled);
        assertEquals("使用", actions.get(0).getDisplayText());
    }

    @Test
    public void testConsumableMenuProvider_UseDisabledWhenOutBattleNotAllowed() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testConsumableBattleOnly);
        assertFalse("局外禁用时使用按钮应置灰", actions.get(0).enabled);
        assertEquals("使用(局外禁用)", actions.get(0).getDisplayText());
    }

    @Test
    public void testConsumableMenuProvider_UseActionTriggersCallback() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testConsumableOutBattle);
        actions.get(0).action.accept(testConsumableOutBattle);
        assertEquals(testConsumableOutBattle, lastUsed.get());
        assertEquals(1, useCallCount.get());
    }

    @Test
    public void testConsumableMenuProvider_ViewAction() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testConsumableOutBattle);
        assertEquals("查看", actions.get(1).name);
        assertTrue(actions.get(1).enabled);
        actions.get(1).action.accept(testConsumableOutBattle);
        assertEquals(testConsumableOutBattle, lastViewed.get());
    }

    @Test
    public void testConsumableMenuProvider_DiscardAction() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testConsumableOutBattle);
        assertEquals("丢弃", actions.get(2).name);
        assertTrue(actions.get(2).enabled);
        actions.get(2).action.accept(testConsumableOutBattle);
        assertEquals(testConsumableOutBattle, lastDiscarded.get());
    }

    @Test
    public void testConsumableMenuProvider_ActionOrder() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testConsumableOutBattle);
        assertEquals("使用", actions.get(0).name);
        assertEquals("查看", actions.get(1).name);
        assertEquals("丢弃", actions.get(2).name);
    }

    // ====================== GemMenuProvider 测试 ======================

    @Test
    public void testGemMenuProvider_ActionCount() {
        GemMenuProvider provider = new GemMenuProvider(g -> {}, viewCallback, discardCallback, false);
        List<ItemAction> actions = provider.getActions(testGemItem);
        assertEquals(3, actions.size());
    }

    @Test
    public void testGemMenuProvider_MosaicAlwaysDisabled() {
        GemMenuProvider provider = new GemMenuProvider(g -> {}, viewCallback, discardCallback, true);
        List<ItemAction> actions = provider.getActions(testGemItem);
        assertEquals("镶嵌(战斗禁用)", actions.get(0).name);
        assertFalse("镶嵌功能应默认不可用", actions.get(0).enabled);
        assertEquals("镶嵌(战斗禁用)(战斗中不可镶嵌)", actions.get(0).getDisplayText());
    }

    @Test
    public void testGemMenuProvider_NoUseButton() {
        GemMenuProvider provider = new GemMenuProvider(g -> {}, viewCallback, discardCallback, true);
        List<ItemAction> actions = provider.getActions(testGemItem);
        for (ItemAction action : actions) {
            assertFalse("Gem 不应有使用按钮", "使用".equals(action.name));
        }
    }

    @Test
    public void testGemMenuProvider_ActionOrder() {
        GemMenuProvider provider = new GemMenuProvider(g -> {}, viewCallback, discardCallback, true);
        List<ItemAction> actions = provider.getActions(testGemItem);
        assertEquals("镶嵌(战斗禁用)", actions.get(0).name);
        assertEquals("查看", actions.get(1).name);
        assertEquals("丢弃", actions.get(2).name);
    }

    @Test
    public void testGemMenuProvider_ViewAndDiscardWork() {
        GemMenuProvider provider = new GemMenuProvider(g -> {}, viewCallback, discardCallback, false);
        List<ItemAction> actions = provider.getActions(testGemItem);
        actions.get(1).action.accept(testGemItem);
        assertEquals(testGemItem, lastViewed.get());
        actions.get(2).action.accept(testGemItem);
        assertEquals(testGemItem, lastDiscarded.get());
    }

    // ====================== MaterialMenuProvider 测试 ======================

    @Test
    public void testMaterialMenuProvider_ActionCount() {
        MaterialMenuProvider provider = new MaterialMenuProvider(viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testMaterialItem);
        assertEquals(2, actions.size());
    }

    @Test
    public void testMaterialMenuProvider_NoSpecialAction() {
        MaterialMenuProvider provider = new MaterialMenuProvider(viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testMaterialItem);
        assertFalse(actions.get(0).name.contains("使用"));
        assertFalse(actions.get(0).name.contains("装备"));
        assertFalse(actions.get(0).name.contains("镶嵌"));
    }

    @Test
    public void testMaterialMenuProvider_NoUseButton() {
        MaterialMenuProvider provider = new MaterialMenuProvider(viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testMaterialItem);
        for (ItemAction action : actions) {
            assertFalse("Material 不应有使用按钮", "使用".equals(action.name));
        }
    }

    @Test
    public void testMaterialMenuProvider_ActionOrder() {
        MaterialMenuProvider provider = new MaterialMenuProvider(viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testMaterialItem);
        assertEquals("查看", actions.get(0).name);
        assertEquals("丢弃", actions.get(1).name);
    }

    @Test
    public void testMaterialMenuProvider_ViewAndDiscardWork() {
        MaterialMenuProvider provider = new MaterialMenuProvider(viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testMaterialItem);
        actions.get(0).action.accept(testMaterialItem);
        assertEquals(testMaterialItem, lastViewed.get());
        actions.get(1).action.accept(testMaterialItem);
        assertEquals(testMaterialItem, lastDiscarded.get());
    }

    // ====================== ItemMenuProviderFactory 测试 ======================

    @Test
    public void testFactory_GetActionsForEquipItem() {
        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardCallback, false);
        factory.registerEquipment(equipCallback, null);
        factory.registerConsumable(useCallback);
        factory.registerGem(g -> {});
        factory.registerMaterial();

        List<ItemAction> actions = factory.getActions(testEquipItem);
        assertEquals(3, actions.size());
        assertEquals("装备", actions.get(0).name);
        assertEquals("查看", actions.get(1).name);
        assertEquals("丢弃", actions.get(2).name);
    }

    @Test
    public void testFactory_GetActionsForConsumableItem() {
        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardCallback, false);
        factory.registerEquipment(equipCallback, null);
        factory.registerConsumable(useCallback);
        factory.registerGem(g -> {});
        factory.registerMaterial();

        List<ItemAction> actions = factory.getActions(testConsumableOutBattle);
        assertEquals(3, actions.size());
        assertEquals("使用", actions.get(0).name);
    }

    @Test
    public void testFactory_GetActionsForGemItem() {
        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardCallback, true);
        factory.registerEquipment(equipCallback, null);
        factory.registerConsumable(useCallback);
        factory.registerGem(g -> {});
        factory.registerMaterial();
        List<ItemAction> actions = factory.getActions(testGemItem);
        assertEquals(3, actions.size());
        assertEquals("镶嵌(战斗禁用)", actions.get(0).name);
        assertFalse(actions.get(0).enabled);
    }

    @Test
    public void testFactory_GetActionsForMaterialItem() {
        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardCallback, false);
        factory.registerEquipment(equipCallback, null);
        factory.registerConsumable(useCallback);
        factory.registerGem(g -> {});
        factory.registerMaterial();

        List<ItemAction> actions = factory.getActions(testMaterialItem);
        assertEquals(2, actions.size());
        assertEquals("查看", actions.get(0).name);
        assertEquals("丢弃", actions.get(1).name);
    }

    @Test
    public void testFactory_NullItemReturnsEmpty() {
        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardCallback, false);
        factory.registerEquipment(equipCallback, null);
        factory.registerConsumable(useCallback);
        factory.registerGem(g -> {});
        factory.registerMaterial();

        List<ItemAction> actions = factory.getActions(null);
        assertNotNull(actions);
        assertTrue(actions.isEmpty());
    }

    @Test
    public void testFactory_UnregisteredClassReturnsEmpty() {
        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardCallback, false);

        Item unknownItem = new MaterialItem("unknown", "未知", Rarity.COMMON, 1, 1, "?") {};
        List<ItemAction> actions = factory.getActions(unknownItem);
        assertNotNull(actions);
        assertTrue(actions.isEmpty());
    }

    @Test
    public void testFactory_NoInstanceofCheck() {
        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardCallback, true);
        factory.registerEquipment(equipCallback, null);
        factory.registerConsumable(useCallback);
        factory.registerGem(g -> {});
        factory.registerMaterial();

        List<ItemAction> equipActions = factory.getActions(testEquipItem);
        assertFalse(equipActions.isEmpty());

        List<ItemAction> conActions = factory.getActions(testConsumableOutBattle);
        assertEquals("使用", conActions.get(0).name);

        List<ItemAction> gemActions = factory.getActions(testGemItem);
        assertEquals("镶嵌(战斗禁用)", gemActions.get(0).name);

        List<ItemAction> matActions = factory.getActions(testMaterialItem);
        assertEquals("查看", matActions.get(0).name);
    }

    // ====================== Equipment 动态状态测试 ======================

    @Test
    public void testEquipmentProvider_EquipAlwaysEnabledRegardlessOfSlot() {
        org.junit.Assume.assumeNotNull("Character creation requires real context (Robolectric)", testCharacter);
        Character ch = testCharacter;
        ch.unequip(EquipSlot.WEAPON);

        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardCallback, false);
        factory.registerEquipment(equipCallback, null);
        factory.registerConsumable(useCallback);
        factory.registerGem(g -> {});
        factory.registerMaterial();

        List<ItemAction> actions = factory.getActions(testEquipItem);
        assertTrue("初始槽位为空，装备应可用", actions.get(0).enabled);

        ch.equip(testEquipItem);
        actions = factory.getActions(testEquipItem);
        assertTrue("装备后槽位已占用，装备仍应可用（同部位自动替换）", actions.get(0).enabled);

        ch.unequip(EquipSlot.WEAPON);
        actions = factory.getActions(testEquipItem);
        assertTrue("卸下后装备应可用", actions.get(0).enabled);
    }

    @Test
    public void testEquipmentProvider_EquipActionExecution() {
        org.junit.Assume.assumeNotNull("Character creation requires real context (Robolectric)", testCharacter);
        Character ch = testCharacter;

        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardCallback, false);
        factory.registerEquipment(equipCallback, null);
        factory.registerConsumable(useCallback);
        factory.registerGem(g -> {});
        factory.registerMaterial();

        List<ItemAction> actions = factory.getActions(testEquipItem);
        actions.get(0).action.accept(testEquipItem);
        assertEquals(testEquipItem, lastEquipped.get());
        assertEquals(1, equipCallCount.get());
    }

    // ====================== Consumable 动态状态测试 ======================

    @Test
    public void testConsumableProvider_UseEnabledDependsOnUsableOutBattle() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);

        List<ItemAction> outBattleActions = provider.getActions(testConsumableOutBattle);
        assertTrue("usableOutBattle=true 时使用应可用", outBattleActions.get(0).enabled);

        List<ItemAction> battleOnlyActions = provider.getActions(testConsumableBattleOnly);
        assertFalse("usableOutBattle=false 时使用应不可用", battleOnlyActions.get(0).enabled);
        assertEquals("使用(局外禁用)", battleOnlyActions.get(0).getDisplayText());
    }

    @Test
    public void testConsumableProvider_UseActionExecution() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testConsumableOutBattle);
        actions.get(0).action.accept(testConsumableOutBattle);
        assertEquals(testConsumableOutBattle, lastUsed.get());
        assertEquals(1, useCallCount.get());
    }

    @Test
    public void testConsumableProvider_DisabledUseActionDoesNothing() {
        ConsumableMenuProvider provider = new ConsumableMenuProvider(useCallback, viewCallback, discardCallback);
        List<ItemAction> actions = provider.getActions(testConsumableBattleOnly);
        assertFalse(actions.get(0).enabled);
        actions.get(0).action.accept(testConsumableBattleOnly);
        assertEquals(0, useCallCount.get());
    }

    // ====================== 二次确认流程测试 ======================

    @Test
    public void testDiscardAction_ConfirmFlow() {
        AtomicBoolean confirmCalled = new AtomicBoolean(false);
        Consumer<Item> discardWithConfirm = item -> {
            confirmCalled.set(true);
            lastDiscarded.set(item);
        };

        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardWithConfirm, false);
        factory.registerMaterial();

        List<ItemAction> actions = factory.getActions(testMaterialItem);
        assertEquals("丢弃", actions.get(1).name);

        actions.get(1).action.accept(testMaterialItem);
        assertTrue("丢弃确认流程应被触发", confirmCalled.get());
        assertEquals(testMaterialItem, lastDiscarded.get());
    }

    // ====================== UI 刷新验证 ======================

    @Test
    public void testAllProviders_ViewActionAlwaysEnabled() {
        List<ItemMenuProvider> providers = new ArrayList<>();
        providers.add(new EquipmentMenuProvider(equipCallback, null, viewCallback, discardCallback));
        providers.add(new ConsumableMenuProvider(useCallback, viewCallback, discardCallback));
        providers.add(new GemMenuProvider(g -> {}, viewCallback, discardCallback, false));
        providers.add(new MaterialMenuProvider(viewCallback, discardCallback));

        List<Item> items = new ArrayList<>();
        items.add(testEquipItem);
        items.add(testConsumableOutBattle);
        items.add(testGemItem);
        items.add(testMaterialItem);

        for (int i = 0; i < providers.size(); i++) {
            List<ItemAction> actions = providers.get(i).getActions(items.get(i));
            boolean viewFound = false;
            for (ItemAction action : actions) {
                if ("查看".equals(action.name)) {
                    assertTrue("查看按钮应始终可用", action.enabled);
                    viewFound = true;
                }
            }
            assertTrue("应包含查看按钮: " + providers.get(i).getClass().getSimpleName(), viewFound);
        }
    }

    @Test
    public void testAllProviders_DiscardActionAlwaysEnabled() {
        List<ItemMenuProvider> providers = new ArrayList<>();
        providers.add(new EquipmentMenuProvider(equipCallback, null, viewCallback, discardCallback));
        providers.add(new ConsumableMenuProvider(useCallback, viewCallback, discardCallback));
        providers.add(new GemMenuProvider(g -> {}, viewCallback, discardCallback, false));
        providers.add(new MaterialMenuProvider(viewCallback, discardCallback));

        List<Item> items = new ArrayList<>();
        items.add(testEquipItem);
        items.add(testConsumableOutBattle);
        items.add(testGemItem);
        items.add(testMaterialItem);

        for (int i = 0; i < providers.size(); i++) {
            List<ItemAction> actions = providers.get(i).getActions(items.get(i));
            boolean discardFound = false;
            for (ItemAction action : actions) {
                if ("丢弃".equals(action.name)) {
                    assertTrue("丢弃按钮应始终可用", action.enabled);
                    discardFound = true;
                }
            }
            assertTrue("应包含丢弃按钮: " + providers.get(i).getClass().getSimpleName(), discardFound);
        }
    }

    @Test
    public void testAllProviders_OrderIsSpecialActionThenViewThenDiscard() {
        List<Item> items = new ArrayList<>();
        items.add(testEquipItem);
        items.add(testConsumableOutBattle);
        items.add(testGemItem);
        items.add(testMaterialItem);

        ItemMenuProviderFactory factory = new ItemMenuProviderFactory(context, viewCallback, discardCallback, false);
        factory.registerEquipment(equipCallback, null);
        factory.registerConsumable(useCallback);
        factory.registerGem(g -> {});
        factory.registerMaterial();

        for (Item item : items) {
            List<ItemAction> actions = factory.getActions(item);
            assertFalse("菜单不应为空", actions.isEmpty());
            int lastIndex = actions.size() - 1;
            assertEquals("最后一项应为丢弃", "丢弃", actions.get(lastIndex).name);
            assertEquals("倒数第二项应为查看", "查看", actions.get(lastIndex - 1).name);
        }
    }
}
