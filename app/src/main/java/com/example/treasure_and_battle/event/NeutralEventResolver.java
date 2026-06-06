package com.example.treasure_and_battle.event;

import android.content.Context;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.event.EventManager;
import com.example.treasure_and_battle.manager.battle.MonsterManager;
import com.example.treasure_and_battle.manager.item.EquipmentManager;
import com.example.treasure_and_battle.manager.item.InventoryManager;
import com.example.treasure_and_battle.manager.item.ItemManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.gem.GemItem;
import com.example.treasure_and_battle.utils.AttributeUtils;
import com.example.treasure_and_battle.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stateless utility class containing all pure business logic for neutral events.
 * The Activity calls these methods and handles UI rendering (buttons, dialogs, intents).
 * Context is required only for manager singletons (ItemManager, EquipmentManager, etc.).
 */
public class NeutralEventResolver {

    /** Fibonacci-like wishing costs per round */
    public static final int[] WISHING_AMOUNTS = {1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233};

    // ====================== Result data classes ======================

    public static class ScholarResetResult {
        public boolean success;
        public String errorMessage;
        public int goldSpent = 500;
        public int talentRefunded;
        public int skillRefunded;
        public int currentGold;
    }

    public static class CampRestResult {
        public int maxHp;
        public int currentHp;
        public int healAmount;
        public int newHp;
        public boolean alreadyFull;
    }

    public static class CaveTreasureStepResult {
        public int step;
        public int hpLoss;
        /** Generated item, or null if none */
        public Item generatedItem;
        /** Display name for the generated item (may include "背包已满" suffix) */
        public String itemDisplayName;
        /** True if the item was successfully added to bag */
        public boolean itemAdded;

        // Step 3 specific
        public boolean isBattle;
        public boolean isTreasure;
        public int goldFound;
        public Item bonusItem;
        public String bonusItemName;
        public boolean bonusItemAdded;
    }

    public static class ChestRollResult {
        public String chestName;
        public String keyId;
        /** Rarity options for equipment, ordered by weight */
        public Rarity[] equipRarities;
        /** Cumulative weight for each equip rarity (last must be 1.0) */
        public double[] equipWeights;
        /** Rarity options for gems */
        public Rarity[] gemRarities;
        /** Cumulative weight for each gem rarity */
        public double[] gemWeights;
        public int goldMin;
        public int goldMax;
        /** Gold chest special rule: one legendary, one epic */
        public boolean isGoldChest;
        /** Whether this chest requires a key to open */
        public boolean requiresKey = true;
    }

    public static class ChestOpenResult {
        public boolean hasKey;
        public String keyName;
        public boolean keyConsumed;
        public int goldGained;
        public int currentGold;
        public String equipName;
        public String equipRarityDisplay;
        public boolean equipAdded;
        public String gemName;
        public String gemRarityDisplay;
        public boolean gemAdded;
        public boolean bagFull;
    }

    public static class TravelerRequestResult {
        public boolean hasEligibleItems;
        public Item matchedItem;
        public int requiredRarityId;
        public ItemType requiredType;
        public String requestDesc;
    }

    public static class TravelerRewardResult {
        public int goldReward;
        public String itemName;
        public int currentGold;
    }

    public static class CasinoResult {
        /** true=win, false=lose, null=tie */
        public Boolean won;
        public int goldChange;
        public int currentGold;
    }

    public static class MysteryBoxResult {
        public boolean success;
        public Item item;
        public boolean added;
        public boolean bagFull;
        public int currentGold;
        public Rarity rarity;
    }

    public static class WishingResult {
        /** null means nothing happened */
        public Rarity rarity;
        public Item reward;
        public String rewardName;
        public boolean added;
        public String rarityDisplay;
        public int currentGold;
    }

    public static class AlterSacrificeResult {
        public boolean success;
        public String errorMessage;
        public Item reward;
        public String rewardName;
        public String rewardRarityDisplay;
        public boolean rewardAdded;
        public int totalSacrificed;
        /** Rollback info: index -> {count, originalCount} */
        public int[][] rollback;
    }

    public static class GemUpgradeResult {
        public boolean success;
        public String errorMessage;
        public GemItem upgradedGem;
        public String nextRarityDisplay;
    }

    // ====================== Event Methods ======================

    /**
     * Scholar reset: spend 500 gold to reset all talent and skill points.
     */
    public static ScholarResetResult resolveScholarReset(Character ch) {
        ScholarResetResult result = new ScholarResetResult();
        if (!ch.spendGold(500)) {
            result.success = false;
            result.errorMessage = "你的金币不足500，无法支付洗点费用。";
            return result;
        }
        int oldTalent = ch.getTalentPoints();
        int oldSkill = ch.getSkillPoints();
        ch.resetAllTalentPoints();
        int skillRefund = 0;
        if (ch.getProfession() != null) {
            skillRefund += ch.getProfession().getActiveSkillTree().resetAllSkills();
            skillRefund += ch.getProfession().getPassiveSkillTree().resetAllSkills();
            skillRefund += ch.getProfession().getEventSkillTree().resetAllSkills();
        }
        ch.addSkillPoints(skillRefund);
        result.success = true;
        result.talentRefunded = ch.getTalentPoints() - oldTalent;
        result.skillRefunded = ch.getSkillPoints() - oldSkill;
        result.currentGold = ch.getGold();
        return result;
    }

    /**
     * Camp rest: heal 30% of max HP using AttributeUtils for accurate maxHp.
     */
    public static CampRestResult resolveCampRest(Character ch) {
        CampRestResult result = new CampRestResult();
        result.maxHp = AttributeUtils.calculateCharacterAttributes(ch).maxHp;
        result.currentHp = ch.getCurrentHp();
        if (result.currentHp >= result.maxHp) {
            result.alreadyFull = true;
            return result;
        }
        result.healAmount = (int) (result.maxHp * 0.3);
        result.newHp = Math.min(result.currentHp + result.healAmount, result.maxHp);
        ch.setCurrentHp(result.newHp);
        result.healAmount = result.newHp - result.currentHp; // actual heal after clamping
        return result;
    }

    /**
     * Execute one step of cave treasure exploration.
     * Steps 0-2 generate items and calculate HP loss; step 3 is the final encounter.
     */
    public static CaveTreasureStepResult resolveCaveTreasureStep(int step, Character ch, Context ctx) {
        CaveTreasureStepResult result = new CaveTreasureStepResult();
        result.step = step;
        ItemManager im = ItemManager.getInstance(ctx);
        EquipmentManager em = EquipmentManager.getInstance(ctx);

        int equipLevel = ch.getLevel() + RandomUtils.getRandomInt(-3, 3);
        equipLevel = Math.max(1, equipLevel);

        if (step == 0) {
            result.hpLoss = (ch.getCurrentHp() + 9) / 10;
            Rarity r = Math.random() < 0.5 ? Rarity.COMMON : Rarity.UNCOMMON;
            if (Math.random() < 0.4) {
                result.generatedItem = em.generateEquip(em.getRandomTemplateId(), equipLevel, r);
            } else {
                result.generatedItem = im.getRandomConsumableByRarity(r);
                if (result.generatedItem == null) result.generatedItem = im.getRandomGemByRarity(r);
            }
            result.itemDisplayName = result.generatedItem != null ? result.generatedItem.getName() : "一件宝物";
            if (result.generatedItem != null) {
                result.itemAdded = InventoryManager.addItem(ch.getBagItems(), result.generatedItem);
                if (!result.itemAdded) {
                    result.itemDisplayName = result.generatedItem.getName() + "（背包已满，未能获得）";
                    result.generatedItem = null;
                }
            }
        } else if (step == 1) {
            result.hpLoss = (AttributeUtils.calculateCharacterAttributes(ch).maxHp + 7) / 8;
            Rarity r = Math.random() < 0.6 ? Rarity.UNCOMMON : Rarity.RARE;
            if (Math.random() < 0.4) {
                result.generatedItem = em.generateEquip(em.getRandomTemplateId(), equipLevel, r);
            } else {
                result.generatedItem = im.getRandomConsumableByRarity(r);
                if (result.generatedItem == null) result.generatedItem = im.getRandomGemByRarity(r);
            }
            result.itemDisplayName = result.generatedItem != null ? result.generatedItem.getName() : "一件宝物";
            if (result.generatedItem != null) {
                result.itemAdded = InventoryManager.addItem(ch.getBagItems(), result.generatedItem);
                if (!result.itemAdded) {
                    result.itemDisplayName = result.generatedItem.getName() + "（背包已满，未能获得）";
                    result.generatedItem = null;
                }
            }
        } else if (step == 2) {
            result.hpLoss = (AttributeUtils.calculateCharacterAttributes(ch).maxHp + 5) / 6;
        } else {
            int roll = (int) (Math.random() * 2);
            if (roll == 0) {
                result.isBattle = true;
                MonsterManager mm = MonsterManager.getInstance(ctx);
                Monster caveMonster = mm.createRandomMonsterWithConstraints(
                        new int[]{Rarity.RARE.getId(), Rarity.EPIC.getId()},
                        new String[]{"BANDIT", "CULTIST"});
                if (caveMonster != null) {
                    List<Monster> batch = new java.util.ArrayList<>();
                    batch.add(caveMonster);
                    EventManager.getInstance(ctx.getApplicationContext()).setCurrentBattleMonsters(batch);
                }
            } else {
                result.isTreasure = true;
                result.goldFound = 500 + (int) (Math.random() * 1001);
                ch.addGold(result.goldFound);
                Rarity r = Math.random() < 0.5 ? Rarity.RARE : Rarity.EPIC;
                if (Math.random() < 0.4) {
                    result.bonusItem = em.generateEquip(em.getRandomTemplateId(), equipLevel, r);
                } else {
                    result.bonusItem = im.getRandomGemByRarity(r);
                    if (result.bonusItem == null) result.bonusItem = im.getRandomConsumableByRarity(r);
                }
                result.bonusItemName = result.bonusItem != null ? result.bonusItem.getName() : "一份神秘的战利品";
                if (result.bonusItem != null) {
                    result.bonusItemAdded = InventoryManager.addItem(ch.getBagItems(), result.bonusItem);
                    if (!result.bonusItemAdded) {
                        result.bonusItemName = result.bonusItem.getName() + "（背包已满，未能获得）";
                        result.bonusItem = null;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Apply HP loss from cave treasure exploration step.
     */
    public static void applyCaveHpLoss(Character ch, int hpLoss) {
        ch.setCurrentHp(Math.max(1, ch.getCurrentHp() - hpLoss));
    }

    /**
     * Roll a chest type with probability distribution:
     * Wood 40%, Copper 30%, Silver 20%, Gold 10%.
     */
    public static ChestRollResult rollChestType() {
        ChestRollResult result = new ChestRollResult();
        double roll = Math.random();
        if (roll < 0.10) {
            // 金制宝箱 10%
            result.chestName = "金宝箱";
            result.keyId = "key_gold";
            result.goldMin = 501;
            result.goldMax = 999;
            result.isGoldChest = true;
        } else if (roll < 0.30) {
            // 银质宝箱 20%
            result.chestName = "银宝箱";
            result.keyId = "key_silver";
            result.goldMin = 301;
            result.goldMax = 399;
            result.equipRarities = new Rarity[]{Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
            result.equipWeights = new double[]{0.1, 0.5, 1.0};
            result.gemRarities = new Rarity[]{Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
            result.gemWeights = new double[]{0.1, 0.5, 1.0};
        } else if (roll < 0.60) {
            // 铜质宝箱 30%
            result.chestName = "铜宝箱";
            result.keyId = "key_copper";
            result.goldMin = 101;
            result.goldMax = 199;
            result.equipRarities = new Rarity[]{Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE};
            result.equipWeights = new double[]{0.3, 0.6, 1.0};
            result.gemRarities = new Rarity[]{Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE};
            result.gemWeights = new double[]{0.3, 0.6, 1.0};
        } else {
            // 木质宝箱 40%
            result.chestName = "木宝箱";
            result.keyId = "key_wood";
            result.goldMin = 1;
            result.goldMax = 99;
            result.requiresKey = false;
            result.equipRarities = new Rarity[]{Rarity.COMMON, Rarity.UNCOMMON};
            result.equipWeights = new double[]{0.7, 1.0};
            result.gemRarities = new Rarity[]{Rarity.COMMON, Rarity.UNCOMMON};
            result.gemWeights = new double[]{0.7, 1.0};
        }
        return result;
    }

    /**
     * Open a chest: consume key, generate gold and items.
     * Reward rarities follow chest type probability distribution.
     * Gold chest: one Legendary + one Epic guaranteed.
     */
    public static ChestOpenResult openChest(Character ch, Context ctx, ChestRollResult chestInfo) {
        ChestOpenResult result = new ChestOpenResult();
        result.keyName = getKeyName(chestInfo.keyId);
        List<Item> bag = ch.getBagItems();

        if (chestInfo.requiresKey) {
            ConsumableItem keyItem = findConsumableById(bag, chestInfo.keyId);
            if (keyItem == null) {
                result.hasKey = false;
                return result;
            }
            result.hasKey = true;

            keyItem.setCount(keyItem.getCount() - 1);
            if (keyItem.getCount() <= 0) {
                InventoryManager.removeItem(bag, keyItem);
            }
            result.keyConsumed = true;
        } else {
            result.hasKey = true;
        }

        EquipmentManager em = EquipmentManager.getInstance(ctx);
        ItemManager im = ItemManager.getInstance(ctx);
        result.goldGained = chestInfo.goldMin + (int) (Math.random() * (chestInfo.goldMax - chestInfo.goldMin + 1));
        ch.addGold(result.goldGained);
        result.currentGold = ch.getGold();

        int equipLevel = ch.getLevel() + RandomUtils.getRandomInt(-3, 3);
        equipLevel = Math.max(1, equipLevel);

        if (chestInfo.isGoldChest) {
            // Gold chest: one Legendary + one Epic
            boolean equipLegendary = Math.random() < 0.5;
            Rarity equipRarity = equipLegendary ? Rarity.LEGENDARY : Rarity.EPIC;
            Rarity gemRarity = equipLegendary ? Rarity.EPIC : Rarity.LEGENDARY;

            EquipItem equip = em.generateEquip(em.getRandomTemplateId(), equipLevel, equipRarity);
            if (equip != null) {
                result.equipName = equip.getName();
                result.equipRarityDisplay = equip.getRarity().getDisplayName();
                result.equipAdded = InventoryManager.addItem(bag, equip);
                if (!result.equipAdded) result.bagFull = true;
            }

            GemItem gem = im.getRandomGemByRarity(gemRarity);
            if (gem != null) {
                result.gemName = gem.getName();
                result.gemRarityDisplay = gem.getRarity().getDisplayName();
                result.gemAdded = InventoryManager.addItem(bag, gem);
                if (!result.gemAdded) result.bagFull = true;
            }
        } else {
            // Weighted random for equip rarity
            Rarity equipRarity = pickWeightedRarity(chestInfo.equipRarities, chestInfo.equipWeights);
            EquipItem equip = em.generateEquip(em.getRandomTemplateId(), equipLevel, equipRarity);
            if (equip != null) {
                result.equipName = equip.getName();
                result.equipRarityDisplay = equip.getRarity().getDisplayName();
                result.equipAdded = InventoryManager.addItem(bag, equip);
                if (!result.equipAdded) result.bagFull = true;
            }

            // Weighted random for gem rarity
            Rarity gemRarity = pickWeightedRarity(chestInfo.gemRarities, chestInfo.gemWeights);
            GemItem gem = im.getRandomGemByRarity(gemRarity);
            if (gem != null) {
                result.gemName = gem.getName();
                result.gemRarityDisplay = gem.getRarity().getDisplayName();
                result.gemAdded = InventoryManager.addItem(bag, gem);
                if (!result.gemAdded) result.bagFull = true;
            }
        }

        return result;
    }

    private static Rarity pickWeightedRarity(Rarity[] rarities, double[] weights) {
        double roll = Math.random();
        for (int i = 0; i < weights.length; i++) {
            if (roll < weights[i]) return rarities[i];
        }
        return rarities[rarities.length - 1];
    }

    /**
     * Build a traveler request: pick a random eligible item from the bag to ask for.
     */
    public static TravelerRequestResult buildTravelerRequest(Character ch) {
        TravelerRequestResult result = new TravelerRequestResult();
        List<Item> bag = ch.getBagItems();

        List<Item> eligibleItems = new ArrayList<>();
        ItemType[] allowedTypes = {ItemType.EQUIPMENT, ItemType.CONSUMABLE, ItemType.GEM};
        java.util.Set<ItemType> allowedSet = new java.util.HashSet<>(java.util.Arrays.asList(allowedTypes));
        for (Item item : bag) {
            if (item != null && allowedSet.contains(item.getType()) && item.getCount() > 0) {
                eligibleItems.add(item);
            }
        }

        if (eligibleItems.isEmpty()) {
            result.hasEligibleItems = false;
            return result;
        }
        result.hasEligibleItems = true;

        Item match = eligibleItems.get((int) (Math.random() * eligibleItems.size()));
        result.matchedItem = match;
        result.requiredRarityId = match.getRarity().getId();
        result.requiredType = match.getType();

        String typeName;
        switch (result.requiredType) {
            case EQUIPMENT: typeName = "装备"; break;
            case CONSUMABLE: typeName = "药水"; break;
            case GEM: typeName = "宝石"; break;
            default: typeName = "物品"; break;
        }
        result.requestDesc = "一件" + match.getRarity().getDisplayName() + "品质的" + typeName;
        return result;
    }

    /**
     * Resolve traveler reward: remove the matched item and give gold.
     */
    public static TravelerRewardResult resolveTravelerReward(Character ch, Item matchedItem, int requiredRarityId) {
        TravelerRewardResult result = new TravelerRewardResult();
        result.itemName = matchedItem.getName();
        InventoryManager.removeItem(ch.getBagItems(), matchedItem);
        result.goldReward = 50 + (requiredRarityId + 1) * 50
                + (int) (Math.random() * ((requiredRarityId + 1) * 100 + 1));
        ch.addGold(result.goldReward);
        result.currentGold = ch.getGold();
        return result;
    }

    /**
     * Casino wagon: bet 100 gold, 40% win / 30% tie / 30% lose.
     * Character must already have gold spent (the Activity checks and spends).
     */
    public static CasinoResult resolveCasinoWager(Character ch) {
        CasinoResult result = new CasinoResult();
        int roll = (int) (Math.random() * 100);
        if (roll < 40) {
            ch.addGold(200);
            result.won = true;
            result.goldChange = 200;
        } else if (roll < 70) {
            ch.addGold(100);
            result.won = null; // tie
            result.goldChange = 100;
        } else {
            result.won = false;
            result.goldChange = 0;
        }
        result.currentGold = ch.getGold();
        return result;
    }

    /**
     * Mystery box: generate a random item (equip/consumable/gem) of random rarity.
     * Character must already have 200 gold spent (the Activity checks and spends).
     */
    public static MysteryBoxResult resolveMysteryBox(Character ch, Context ctx) {
        MysteryBoxResult result = new MysteryBoxResult();
        result.success = true;
        ItemManager im = ItemManager.getInstance(ctx);
        EquipmentManager em = EquipmentManager.getInstance(ctx);
        Rarity[] rarities = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
        result.rarity = rarities[(int) (Math.random() * rarities.length)];

        int roll = (int) (Math.random() * 3);
        int equipLevel = ch.getLevel() + RandomUtils.getRandomInt(-3, 3);
        equipLevel = Math.max(1, equipLevel);
        if (roll == 0) {
            result.item = em.generateEquip(em.getRandomTemplateId(), equipLevel, result.rarity);
        } else if (roll == 1) {
            result.item = im.getRandomConsumableByRarity(result.rarity);
        } else {
            result.item = im.getRandomGemByRarity(result.rarity);
        }

        if (result.item != null) {
            result.added = InventoryManager.addItem(ch.getBagItems(), result.item);
            if (!result.added) result.bagFull = true;
        }
        result.currentGold = ch.getGold();
        return result;
    }

    /**
     * Wishing well: spend coins and roll for a reward.
     * Character must already have coins spent (the Activity checks and spends).
     */
    public static WishingResult resolveWishingWellWish(Character ch, Context ctx, int wishingRound, int coins) {
        WishingResult result = new WishingResult();
        double cp = Math.min(coins * 0.005, 0.70);
        double up = Math.min(coins * 0.0001, 0.04);
        double rp = Math.min(coins * 0.0001, 0.04);
        double ep = Math.min(coins * 0.000005, 0.002);
        double lp = Math.min(coins * 0.000005, 0.002);

        double roll = Math.random();
        Rarity rr = null;
        double cum = cp; if (roll < cum) rr = Rarity.COMMON;
        cum += up; if (rr == null && roll < cum) rr = Rarity.UNCOMMON;
        cum += rp; if (rr == null && roll < cum) rr = Rarity.RARE;
        cum += ep; if (rr == null && roll < cum) rr = Rarity.EPIC;
        cum += lp; if (rr == null && roll < cum) rr = Rarity.LEGENDARY;

        result.rarity = rr;
        result.currentGold = ch.getGold();

        if (rr == null) return result;

        EquipmentManager em = EquipmentManager.getInstance(ctx);
        ItemManager im = ItemManager.getInstance(ctx);

        int equipLevel = ch.getLevel() + RandomUtils.getRandomInt(-3, 3);
        equipLevel = Math.max(1, equipLevel);
        int tr = (int) (Math.random() * 3);
        if (tr == 0) result.reward = em.generateEquip(em.getRandomTemplateId(), equipLevel, rr);
        else if (tr == 1) result.reward = im.getRandomConsumableByRarity(rr);
        else result.reward = im.getRandomGemByRarity(rr);
        if (result.reward == null) result.reward = em.generateEquip(em.getRandomTemplateId(), equipLevel, rr);

        result.rarityDisplay = rr.getDisplayName();
        if (result.reward != null) {
            result.rewardName = result.reward.getName();
            result.added = InventoryManager.addItem(ch.getBagItems(), result.reward);
        }
        return result;
    }

    /**
     * Get the cost for a given wishing round.
     */
    public static int getWishingCost(int round) {
        return WISHING_AMOUNTS[Math.min(round, WISHING_AMOUNTS.length - 1)];
    }

    /**
     * Mysterious altar sacrifice: validate selection, consume items, generate upgraded reward.
     * On failure (e.g., bag full), rolls back consumption.
     */
    public static AlterSacrificeResult resolveAlterSacrifice(List<Item> bag, List<Item> eligible,
                                                               int[] sacrificeCounts, Context ctx, Character ch) {
        AlterSacrificeResult result = new AlterSacrificeResult();
        int total = 0;
        for (int c : sacrificeCounts) total += c;
        if (total != 3) {
            result.success = false;
            result.errorMessage = "需要恰好3件物品。";
            return result;
        }

        if (!isSacCountsValid(eligible, sacrificeCounts)) {
            result.success = false;
            result.errorMessage = "选中的物品品质不一致！";
            return result;
        }

        Item first = null;
        for (int i = 0; i < sacrificeCounts.length; i++)
            if (sacrificeCounts[i] > 0) { first = eligible.get(i); break; }
        if (first == null) {
            result.success = false;
            result.errorMessage = "未选中物品。";
            return result;
        }

        Rarity fromR = first.getRarity();
        Rarity toR = Rarity.fromId(fromR.getId() + 1);
        if (toR == null) {
            result.success = false;
            result.errorMessage = "已达到最高品质，无法升阶！";
            return result;
        }

        // Consume items (with rollback info)
        result.rollback = new int[sacrificeCounts.length][2];
        for (int i = 0; i < sacrificeCounts.length; i++) {
            int n = sacrificeCounts[i];
            result.rollback[i][0] = n;
            if (n <= 0) continue;
            Item item = eligible.get(i);
            result.rollback[i][1] = item.getCount();
            item.setCount(item.getCount() - n);
            if (item.getCount() <= 0) InventoryManager.removeItem(bag, item);
        }

        ItemType rewardType = pickRewardTypeByWeight(eligible, sacrificeCounts);
        Item reward = generateReward(rewardType, toR, ctx, ch);
        result.reward = reward;
        result.totalSacrificed = total;

        if (reward == null) {
            result.success = true;
            result.rewardAdded = false;
            return result;
        }

        result.rewardName = reward.getName();
        result.rewardRarityDisplay = reward.getRarity().getDisplayName();

        if (!InventoryManager.addItem(bag, reward)) {
            // Rollback consumption
            for (int i = 0; i < result.rollback.length; i++) {
                int n = result.rollback[i][0];
                if (n <= 0) continue;
                Item item = eligible.get(i);
                item.setCount(result.rollback[i][1]);
                if (n == result.rollback[i][1]) InventoryManager.addItem(bag, item);
            }
            result.success = false;
            result.errorMessage = "背包已满，无法获得献祭奖励！物品已归还。";
            return result;
        }

        result.success = true;
        result.rewardAdded = true;
        return result;
    }

    /**
     * Upgrade a gem to the next rarity tier.
     * Returns the upgraded gem or null if upgrade fails.
     */
    public static GemUpgradeResult resolveGemUpgrade(GemItem gem, Character ch, Context ctx) {
        GemUpgradeResult result = new GemUpgradeResult();
        int currentRarityId = gem.getRarity().getId();
        Rarity nextRarity = Rarity.fromId(currentRarityId + 1);
        result.nextRarityDisplay = nextRarity != null ? nextRarity.getDisplayName() : "???";

        GemItem upgraded = ItemManager.getInstance(ctx).createGem(
                gem.getGemType().toLowerCase() + "_" + nextRarity.name().toLowerCase());
        if (upgraded == null) {
            result.success = false;
            result.errorMessage = "宝石升级失败：无法找到对应模板。";
            return result;
        }

        List<Item> bag = ch.getBagItems();
        gem.setCount(gem.getCount() - 1);
        if (gem.getCount() <= 0) InventoryManager.removeItem(bag, gem);

        if (!InventoryManager.addItem(bag, upgraded)) {
            // Rollback
            gem.setCount(gem.getCount() + 1);
            if (gem.getCount() == 1) InventoryManager.addItem(bag, gem);
            result.success = false;
            result.errorMessage = "宝石升级失败：背包已满！";
            return result;
        }

        result.success = true;
        result.upgradedGem = upgraded;
        return result;
    }

    /**
     * Resolve divination: spend 300 gold and reveal an unknown event.
     * Character must already have gold spent (checked by Activity).
     */
    public static String resolveDivination(Character ch, Context ctx) {
        return EventManager.getInstance(ctx.getApplicationContext()).revealUnknownEvent();
    }

    // ====================== Helper Methods ======================

    public static int countItemsByRarity(List<Item> bag, Rarity rarity) {
        int count = 0;
        for (Item item : bag)
            if (item != null && item.getType() != ItemType.MATERIAL && item.getRarity() == rarity)
                count += item.getCount();
        return count;
    }

    public static boolean isSacCountsValid(List<Item> items, int[] counts) {
        Item first = null;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] <= 0) continue;
            if (first == null) { first = items.get(i); continue; }
            Item t = items.get(i);
            if (t.getRarity() != first.getRarity()) return false;
        }
        return first != null;
    }

    public static ItemType pickRewardTypeByWeight(List<Item> items, int[] counts) {
        int equipCount = 0, consumableCount = 0, gemCount = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] <= 0) continue;
            Item item = items.get(i);
            if (item.getType() == ItemType.EQUIPMENT) equipCount += counts[i];
            else if (item.getType() == ItemType.CONSUMABLE) consumableCount += counts[i];
            else if (item.getType() == ItemType.GEM) gemCount += counts[i];
        }
        int totalWeight = equipCount + consumableCount + gemCount;
        if (totalWeight == 0) return ItemType.EQUIPMENT;
        int roll = new Random().nextInt(totalWeight);
        if (roll < equipCount) return ItemType.EQUIPMENT;
        if (roll < equipCount + consumableCount) return ItemType.CONSUMABLE;
        return ItemType.GEM;
    }

    public static Item generateReward(ItemType type, Rarity rarity, Context ctx, Character ch) {
        if (type == ItemType.EQUIPMENT) {
            EquipmentManager em = EquipmentManager.getInstance(ctx);
            int equipLevel = ch.getLevel() + RandomUtils.getRandomInt(-3, 3);
            equipLevel = Math.max(1, equipLevel);
            return em.generateEquip(em.getRandomTemplateId(), equipLevel, rarity);
        }
        if (type == ItemType.CONSUMABLE)
            return ItemManager.getInstance(ctx).getRandomConsumableByRarity(rarity);
        return ItemManager.getInstance(ctx).getRandomGemByRarity(rarity);
    }

    public static ConsumableItem findConsumableById(List<Item> bag, String id) {
        for (Item item : bag) {
            if (item instanceof ConsumableItem && id.equals(item.getId())) {
                return (ConsumableItem) item;
            }
        }
        return null;
    }

    public static String getKeyName(String keyId) {
        if ("key_gold".equals(keyId)) return "金钥匙";
        if ("key_silver".equals(keyId)) return "银钥匙";
        if ("key_copper".equals(keyId)) return "铜钥匙";
        return "木钥匙";
    }
}
