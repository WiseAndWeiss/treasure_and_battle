package com.example.treasure_and_battle.manager;

import android.content.Context;

import com.example.treasure_and_battle.manager.affix.MonsterAffixManager;
import com.example.treasure_and_battle.manager.skill.MonsterSkillManager;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.battle.action.ActionIntent;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.entity.MonsterTemplate;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.active.ActiveSkill;
import com.google.gson.Gson;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.treasure_and_battle.affix.BaseMonsterAffix;

public class MonsterManager {
    private static MonsterManager instance;
    private Context context;
    private Map<Integer, MonsterTemplate> templateMap = new HashMap<>();

    // 用于生成唯一entityId的计数器
    private static int monsterInstanceCounter = 0;
    private static final Object counterLock = new Object();

    private MonsterManager(Context context) {
        this.context = context.getApplicationContext();
        loadTemplates();
    }

    public static MonsterManager getInstance(Context context) {
        if (instance == null) {
            instance = new MonsterManager(context);
        }
        return instance;
    }

    public static synchronized void releaseInstance() {
        instance = null;
    }

    public MonsterTemplate getTemplate(int templateId) {
        return templateMap.get(templateId);
    }

    private void loadTemplates() {
        try {
            InputStream is = context.getAssets().open("configs/monster_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Gson gson = new Gson();
            MonsterConfigWrapper wrapper = gson.fromJson(json, MonsterConfigWrapper.class);

            if (wrapper != null && wrapper.monster_templates != null) {
                for (MonsterTemplate template : wrapper.monster_templates) {
                    templateMap.put(template.getTemplateId(), template);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Monster createMonsterByTemplateId(int templateId) {
        return createMonsterInternal(templateId, -1, true);
    }

    public Monster createMonsterByTemplateId(int templateId, boolean injectAffixes) {
        return createMonsterInternal(templateId, -1, injectAffixes);
    }

    public Monster createMonsterWithoutAffixes(int templateId) {
        return createMonsterInternal(templateId, -1, false);
    }

    public Monster createMonsterWithLevelScaling(int templateId, int playerLevel) {
        return createMonsterInternal(templateId, playerLevel, true);
    }

    public Monster createRandomMonster() {
        if (templateMap.isEmpty()) return null;
        List<Integer> ids = new ArrayList<>(templateMap.keySet());
        int randomId = ids.get(new Random().nextInt(ids.size()));
        return createMonsterByTemplateId(randomId);
    }

    // ====================== 种族与模板查询 ======================

    public List<String> getAvailableRaces() {
        List<String> races = new ArrayList<>();
        for (MonsterTemplate t : templateMap.values()) {
            String r = t.getRaceId();
            if (r != null && !r.isEmpty() && !races.contains(r)) races.add(r);
        }
        return races;
    }

    public List<String> getAvailableRacesForBattle() {
        List<String> races = new ArrayList<>();
        for (MonsterTemplate t : templateMap.values()) {
            String r = t.getRaceId();
            if (r == null || r.isEmpty() || races.contains(r)) continue;
            races.add(r);
        }
        return races;
    }

    public Map<String, List<MonsterTemplate>> getTemplatesByRace() {
        Map<String, List<MonsterTemplate>> map = new LinkedHashMap<>();
        for (MonsterTemplate t : templateMap.values()) {
            String r = t.getRaceId();
            if (r == null || r.isEmpty()) continue;
            map.computeIfAbsent(r, k -> new ArrayList<>()).add(t);
        }
        return map;
    }

    private List<MonsterTemplate> getTemplatesOfRarity(List<MonsterTemplate> list, int rarityId) {
        List<MonsterTemplate> result = new ArrayList<>();
        for (MonsterTemplate t : list) if (t.getRarityId() == rarityId) result.add(t);
        return result;
    }

    private int getMaxRarityInRace(List<MonsterTemplate> raceTemplates) {
        int max = 0;
        for (MonsterTemplate t : raceTemplates) if (t.getRarityId() > max) max = t.getRarityId();
        return max;
    }

    public int getMaxRarityForRace(String raceId) {
        List<MonsterTemplate> templates = getTemplatesByRace().get(raceId);
        if (templates == null || templates.isEmpty()) return 0;
        return getMaxRarityInRace(templates);
    }

    public List<Monster> createMonstersFromTemplateIds(List<Integer> templateIds, int playerLevel) {
        List<Monster> monsters = new ArrayList<>();
        for (int tid : templateIds) {
            Monster m = playerLevel > 0
                    ? createMonsterWithLevelScaling(tid, playerLevel)
                    : createMonsterByTemplateId(tid);
            if (m != null) monsters.add(m);
        }
        return monsters;
    }

    // ====================== 调试批次生成 ======================

    public List<Integer> generateDebugBatch(String raceId, int maxRarity, int count, Random rng) {
        List<MonsterTemplate> raceTemplates = getTemplatesByRace().get(raceId);
        if (raceTemplates == null || raceTemplates.isEmpty()) return Collections.emptyList();

        int raceMaxRarity = getMaxRarityInRace(raceTemplates);
        int effectiveMax = Math.min(maxRarity, raceMaxRarity);
        double[] fullDist = raceMaxRarity <= 2 ? RARITY_DIST_3 : RARITY_DIST_5;
        double[] cappedDist = new double[effectiveMax + 1];
        double total = 0;
        for (int i = 0; i < cappedDist.length; i++) { cappedDist[i] = fullDist[i]; total += cappedDist[i]; }
        if (total > 0) for (int i = 0; i < cappedDist.length; i++) cappedDist[i] /= total;

        int[] raritySeq = generateRaritySequence(count, cappedDist, rng);
        List<Integer> templateIds = new ArrayList<>();
        for (int rarityId : raritySeq) {
            List<MonsterTemplate> candidates = getTemplatesOfRarity(raceTemplates, rarityId);
            if (candidates.isEmpty()) {
                for (int lowerR = rarityId - 1; lowerR >= 0; lowerR--) {
                    candidates = getTemplatesOfRarity(raceTemplates, lowerR);
                    if (!candidates.isEmpty()) break;
                }
            }
            if (candidates.isEmpty()) candidates = raceTemplates;
            templateIds.add(candidates.get(rng.nextInt(candidates.size())).getTemplateId());
        }
        return templateIds;
    }

    // ====================== 概率分布生成系统 ======================

    private static final double[] COUNT_DISTRIBUTION = {0.10, 0.20, 0.30, 0.25, 0.15};

    private static final double[] RARITY_DIST_5 = {0.45, 0.30, 0.15, 0.07, 0.03};
    private static final double[] RARITY_DIST_3 = {0.50, 0.35, 0.15};

    private int weightedPick(double[] dist, Random rng) {
        double roll = rng.nextDouble();
        double acc = 0;
        for (int i = 0; i < dist.length; i++) {
            acc += dist[i];
            if (roll < acc) return i;
        }
        return dist.length - 1;
    }

    private int[] generateRaritySequence(int count, double[] dist, Random rng) {
        int[] seq = new int[count];
        seq[0] = weightedPick(dist, rng);
        for (int i = 1; i < count; i++) {
            int cap = seq[i - 1];
            double[] sub = new double[cap + 1];
            double total = 0;
            for (int j = 0; j <= cap; j++) { sub[j] = dist[j]; total += dist[j]; }
            if (total > 0) for (int j = 0; j <= cap; j++) sub[j] /= total;
            seq[i] = weightedPick(sub, rng);
        }
        return seq;
    }

    public List<Integer> generateMonsterBatch(int playerLevel, int slotCount, Random rng,
                                              List<String> recentlyUsedRaces) {
        List<String> allRaces = getAvailableRacesForBattle();
        if (allRaces.isEmpty()) return Collections.emptyList();

        List<String> availableRaces = new ArrayList<>(allRaces);
        if (recentlyUsedRaces != null && availableRaces.size() > 3) {
            availableRaces.removeAll(recentlyUsedRaces);
            if (availableRaces.isEmpty()) availableRaces = new ArrayList<>(allRaces);
        }

        String selectedRace = availableRaces.get(rng.nextInt(availableRaces.size()));
        List<MonsterTemplate> raceTemplates = getTemplatesByRace().get(selectedRace);
        if (raceTemplates == null || raceTemplates.isEmpty()) return Collections.emptyList();

        int raceMaxRarity = getMaxRarityInRace(raceTemplates);
        double[] rarityDist = raceMaxRarity <= 2 ? RARITY_DIST_3 : RARITY_DIST_5;
        int count = weightedPick(COUNT_DISTRIBUTION, rng) + 1;
        int[] raritySeq = generateRaritySequence(count, rarityDist, rng);

        List<Integer> templateIds = new ArrayList<>();
        for (int rarityId : raritySeq) {
            List<MonsterTemplate> candidates = getTemplatesOfRarity(raceTemplates, rarityId);
            if (candidates.isEmpty()) {
                for (int lowerR = rarityId - 1; lowerR >= 0; lowerR--) {
                    candidates = getTemplatesOfRarity(raceTemplates, lowerR);
                    if (!candidates.isEmpty()) break;
                }
            }
            if (candidates.isEmpty()) candidates = raceTemplates;
            templateIds.add(candidates.get(rng.nextInt(candidates.size())).getTemplateId());
        }
        return templateIds;
    }

    private double calculateMonsterBasePower(int level) {
        if (level <= 30) {
            return 10.0 + 0.8 * (level - 1);
        } else {
            double power30 = 10.0 + 0.8 * 29;
            return power30 * Math.pow((double) level / 30.0, 1.4);
        }
    }

    private Monster createMonsterInternal(int templateId, int playerLevel, boolean injectAffixes) {
        MonsterTemplate template = templateMap.get(templateId);
        if (template == null) return null;

        Rarity rarity = Rarity.fromId(template.getRarityId());

        int monsterLevel = template.getLevel();
        if (playerLevel > 0) {
            int minLevel = Math.max(1, playerLevel - 5);
            int maxLevel = playerLevel + 5;
            monsterLevel = Math.max(minLevel, Math.min(maxLevel, template.getLevel()));
        }

        double templatePower = calculateMonsterBasePower(template.getLevel());
        double actualPower = calculateMonsterBasePower(monsterLevel);
        float sixDimScale = (float) (actualPower / Math.max(0.1, templatePower));

        int scaledStrength    = Math.max(1, Math.round(template.getStrength()      * sixDimScale));
        int scaledAgility     = Math.max(1, Math.round(template.getAgility()       * sixDimScale));
        int scaledIntelligence = Math.max(1, Math.round(template.getIntelligence() * sixDimScale));
        int scaledSpirit      = Math.max(1, Math.round(template.getSpirit()        * sixDimScale));
        int scaledPhysique    = Math.max(1, Math.round(template.getPhysique()      * sixDimScale));
        int scaledLuck        = Math.max(1, Math.round(template.getLuck()          * sixDimScale));

        int scaledExp  = Math.max(1, Math.round(template.getExpReward() * sixDimScale));
        int scaledGold = Math.max(1, Math.round(template.getGoldReward() * sixDimScale));

        Monster monster = new Monster(
            template.getEntityId(), template.getName(), monsterLevel, rarity,
            scaledStrength, scaledAgility, scaledIntelligence,
            scaledSpirit, scaledPhysique, scaledLuck,
            scaledExp, scaledGold,
            template.getHpMultiplier(), template.getAtkMultiplier(),
            template.getDefMultiplier(), template.getSpdMultiplier(),
            context
        );

        monster.setTemplateId(templateId);

        // 为动画系统设置唯一ID
        String animationUniqueId = generateUniqueAnimationId(template.getEntityId(), templateId);
        monster.setAnimationUniqueId(animationUniqueId);

        addDefaultCombatIntents(monster);
        addSkillPoolIntents(monster, template);

        if (injectAffixes) {
            List<BaseMonsterAffix> affixes =
                MonsterAffixManager.getInstance(context).generateAffixForMonster(monster);
            for (BaseMonsterAffix affix : affixes) {
                monster.addAffix(affix);
            }
        }

        return monster;
    }

    private void addDefaultCombatIntents(Monster monster) {
        monster.addIntent(new ActionIntent(
                "普通攻击", "基础攻击动作",
                ActionIntent.IntentType.ATTACK,
                1, 0, 1.0, 10, 30, -1f, -1f, null));

        monster.addIntent(new ActionIntent(
                "逃跑", "低血时尝试逃跑",
                ActionIntent.IntentType.ESCAPE,
                1, 0, 1.0, 100, 90, -1f, 0.25f, null));
    }

    private void addSkillPoolIntents(Monster monster, MonsterTemplate template) {
        if (template == null || template.getSkillPool() == null) return;

        MonsterSkillManager msm = MonsterSkillManager.getInstance(context);

        for (MonsterTemplate.SkillReference skillRef : template.getSkillPool()) {
            if (skillRef == null || skillRef.getSkillId() == null || skillRef.getSkillId().isEmpty()) continue;

            int skillLevel = skillRef.getLevel();
            Skill skillInstance = msm.createSkillBySkillId(skillRef.getSkillId(), skillLevel);
            if (skillInstance instanceof ActiveSkill) {
                monster.addMonsterSkill(skillRef.getSkillId(), (ActiveSkill) skillInstance);
            }

            String displayName = skillInstance != null ? skillInstance.getSkillName() : skillRef.getSkillId();

            monster.addIntent(new ActionIntent(
                    displayName,
                    "怪物技能",
                    ActionIntent.IntentType.SKILL,
                    Math.max(1, skillRef.getApCost()),
                    Math.max(0, skillRef.getMpCost()),
                    skillRef.getPowerMultiplier() <= 0 ? 1.0 : skillRef.getPowerMultiplier(),
                    Math.max(1, skillRef.getWeight()),
                    Math.max(20, skillRef.getPriority()),
                    -1f, -1f,
                    skillRef.getSkillId()));
        }
    }

    // ====================== 掉落物查询 ======================

    public List<MonsterTemplate.DropEntry> getDropTable(int templateId) {
        MonsterTemplate template = templateMap.get(templateId);
        return template == null ? null : template.getDropTable();
    }

    /**
     * 为每个怪物实例生成唯一的动画ID
     * 这个ID专门用于动画系统，不影响entityId的其他用途
     * @param baseEntityId 模板的基础entityId
     * @param templateId 模板ID
     * @return 唯一的动画ID
     */
    private String generateUniqueAnimationId(String baseEntityId, int templateId) {
        synchronized (counterLock) {
            monsterInstanceCounter++;
            // 格式: 模板ID_实例计数器
            // 例如: slime_1, slime_2, slime_3, slime_4
            String uniqueAnimationId = baseEntityId + "_" + monsterInstanceCounter;

            android.util.Log.d("MonsterManager", "Generated unique animation ID: " + uniqueAnimationId +
                " (template: " + baseEntityId + ", counter: " + monsterInstanceCounter + ")");

            return uniqueAnimationId;
        }
    }

    private static class MonsterConfigWrapper {
        List<MonsterTemplate> monster_templates;
    }
}
