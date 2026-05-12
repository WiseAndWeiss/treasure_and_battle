package com.example.treasure_and_battle.manager.battle;

import android.content.Context;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.manager.item.ItemManager;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.utils.RngEngine;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.MonsterTemplate;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;
import com.example.treasure_and_battle.utils.RandomUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DropManager {
    private static DropManager instance;
    private Context context;

    private Map<Integer, LootPool> lootPoolMap = new HashMap<>();

    private DropManager(Context context) {
        this.context = context.getApplicationContext();
        loadLootPools();
    }

    public static synchronized DropManager getInstance(Context context) {
        if (instance == null) {
            instance = new DropManager(context);
        }
        return instance;
    }

    public static synchronized void releaseInstance() {
        instance = null;
    }

    private void loadLootPools() {
        try {
            InputStream is = context.getAssets().open("loot_pool_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Gson gson = new Gson();
            Type type = new TypeToken<LootPoolConfigWrapper>() {}.getType();
            LootPoolConfigWrapper wrapper = gson.fromJson(json, type);
            if (wrapper != null && wrapper.loot_pools != null) {
                for (LootPool pool : wrapper.loot_pools) {
                    lootPoolMap.put(pool.rarityId, pool);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Item> generateDrops(BattleContext ctx) {
        List<Item> drops = new ArrayList<>();
        if (ctx == null || ctx.monsters == null) return drops;

        for (Monster monster : ctx.monsters) {
            if (monster == null) continue;
            drops.addAll(generateDropsForMonster(monster, ctx.player));
        }

        return drops;
    }

    private List<Item> generateDropsForMonster(Monster monster, Player player) {
        List<Item> drops = new ArrayList<>();

        // ========== 第一块：素材掉落（独立，不占通用槽位） ==========
        int templateId = monster.getTemplateId();
        if (templateId > 0) {
            List<MonsterTemplate.DropEntry> dropTable =
                MonsterManager.getInstance(context).getDropTable(templateId);
            if (dropTable != null) {
                for (MonsterTemplate.DropEntry entry : dropTable) {
                    if (RandomUtils.checkProbability(entry.getDropRate())) {
                        Item material = ItemManager.getInstance(context).createItem(entry.getMaterialId());
                        if (material != null) {
                            drops.add(material);
                        }
                    }
                }
            }
        }

        // ========== 第二块：通用池掉落（数量 = 怪物稀有度 + 1） ==========
        Rarity monsterRarity = monster.getRarity();
        if (monsterRarity == null) return drops;

        int dropCount = monsterRarity.getId() + 1;

        float monsterBonus = monsterRarity.getLootRarityBonus();
        float playerBonus = player != null ? player.getFinalAttributes().lootRarityBonus : 0;
        float totalBonus = monsterBonus + playerBonus;

        boolean needPity = monsterRarity.getId() >= Rarity.RARE.getId();
        Rarity pityMinRarity = monsterRarity;
        if (monsterRarity == Rarity.LEGENDARY) {
            pityMinRarity = Rarity.LEGENDARY;
        }

        List<Rarity> itemRarities = RngEngine.generateRaritiesWithPity(
            dropCount, Rarity.COMMON, totalBonus, needPity, pityMinRarity);

        LootPool pool = lootPoolMap.get(monsterRarity.getId());
        if (pool == null || pool.entries == null) return drops;

        int monsterLevel = monster.getLevel();

        for (Rarity itemRarity : itemRarities) {
            ItemType type = pickTypeByWeight(pool.entries);
            if (type == null) continue;

            Item item = null;
            switch (type) {
                case CONSUMABLE:
                    item = ItemManager.getInstance(context).getRandomConsumableByRarity(itemRarity);
                    break;
                case GEM:
                    item = ItemManager.getInstance(context).getRandomGemByRarity(itemRarity);
                    break;
                case EQUIPMENT:
                    int equipLevel = monsterLevel + RandomUtils.getRandomInt(-3, 3);
                    equipLevel = Math.max(1, equipLevel);
                    item = ItemManager.getInstance(context).getRandomEquipByRarity(equipLevel, itemRarity, context);
                    break;
                default:
                    break;
            }

            if (item != null) {
                drops.add(item);
            }
        }

        return drops;
    }

    private ItemType pickTypeByWeight(List<PoolEntry> entries) {
        if (entries == null || entries.isEmpty()) return null;
        int totalWeight = 0;
        for (PoolEntry e : entries) {
            totalWeight += e.weight;
        }
        if (totalWeight <= 0) return null;

        int roll = RandomUtils.getRandomInt(1, totalWeight);
        int cumulative = 0;
        for (PoolEntry e : entries) {
            cumulative += e.weight;
            if (roll <= cumulative) {
                try {
                    return ItemType.valueOf(e.type);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    // ====================== 玩家领取相关 ======================

    public Item claimDrop(BattleContext ctx, int index) {
        if (ctx == null || ctx.pendingLoot == null || index < 0 || index >= ctx.pendingLoot.size())
            return null;
        Item item = ctx.pendingLoot.get(index);
        List<Item> bag = ctx.player.owner != null ? ctx.player.owner.getBagItems() : null;
        if (bag != null && InventoryManager.addItem(bag, item)) {
            ctx.pendingLoot.remove(index);
            return item;
        }
        return null;
    }

    public List<Item> claimAll(BattleContext ctx) {
        List<Item> unclaimed = new ArrayList<>();
        if (ctx == null || ctx.pendingLoot == null) return unclaimed;

        List<Item> bag = ctx.player.owner != null ? ctx.player.owner.getBagItems() : null;
        if (bag == null) {
            unclaimed.addAll(ctx.pendingLoot);
            ctx.pendingLoot.clear();
            return unclaimed;
        }

        Iterator<Item> it = ctx.pendingLoot.iterator();
        while (it.hasNext()) {
            Item item = it.next();
            if (InventoryManager.addItem(bag, item)) {
                it.remove();
            } else {
                unclaimed.add(item);
                it.remove();
                while (it.hasNext()) {
                    unclaimed.add(it.next());
                    it.remove();
                }
                break;
            }
        }
        return unclaimed;
    }

    // ====================== JSON 包装类 ======================

    private static class LootPool {
        int rarityId;
        List<PoolEntry> entries;
    }

    private static class PoolEntry {
        String type;
        int weight;
    }

    private static class LootPoolConfigWrapper {
        List<LootPool> loot_pools;
    }
}
