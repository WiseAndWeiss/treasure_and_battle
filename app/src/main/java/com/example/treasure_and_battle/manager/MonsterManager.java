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
            if ("BANDIT".equals(r) || "CULTIST".equals(r)) continue;
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

    // ====================== 怪物池（30种预设阵容） ======================

    public static class MonsterPool {
        public final String description;
        public final int[] raritySlots;

        public final int monsterCount;
        public final int maxRarity;

        public MonsterPool(int[] raritySlots) {
            this.raritySlots = raritySlots;
            int cnt = 0, mx = 0;
            for (int r : raritySlots) {
                if (r >= 0) { cnt++; if (r > mx) mx = r; }
            }
            this.monsterCount = cnt;
            this.maxRarity = mx;

            StringBuilder sb = new StringBuilder("×").append(cnt).append("  [");
            for (int i = 0; i < raritySlots.length; i++) {
                if (raritySlots[i] == -1) break;
                if (i > 0) sb.append(",");
                sb.append(raritySlots[i]);
            }
            sb.append("]");
            this.description = sb.toString();
        }
    }

    private static final int E = -1;

    private static final List<MonsterPool> MONSTER_POOLS = new ArrayList<>();
    static {
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,E,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,E,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,E,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{3,E,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{4,E,E,E,E}));

        MONSTER_POOLS.add(new MonsterPool(new int[]{0,0,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,1,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,1,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,2,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,2,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,3,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{3,3,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{3,4,E,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{4,4,E,E,E}));

        MONSTER_POOLS.add(new MonsterPool(new int[]{0,0,0,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,0,1,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,1,1,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,1,1,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,1,2,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,1,2,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,2,2,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,2,2,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,2,3,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,2,3,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,3,3,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{3,3,3,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,3,4,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{3,3,4,E,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{4,4,4,E,E}));

        MONSTER_POOLS.add(new MonsterPool(new int[]{0,0,0,0,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,0,0,1,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,0,1,1,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,1,1,1,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,1,1,1,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,1,1,2,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,1,2,2,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,2,2,2,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,2,2,3,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,2,3,3,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{3,3,3,3,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,3,3,4,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{3,3,4,4,E}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{4,4,4,4,E}));

        MONSTER_POOLS.add(new MonsterPool(new int[]{0,0,0,0,0}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,0,0,0,1}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,0,0,1,1}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,0,1,1,1}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,1,1,1,1}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{0,1,1,1,2}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,1,1,2,2}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,2,2,2,2}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{1,1,2,2,3}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,2,2,2,3}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{3,3,3,3,3}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{2,2,3,3,4}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{3,3,3,3,4}));
        MONSTER_POOLS.add(new MonsterPool(new int[]{4,4,4,4,4}));
    }

    public static int getPoolCount() { return MONSTER_POOLS.size(); }
    public static String getPoolDescription(int index) { return MONSTER_POOLS.get(index).description; }
    public static String getPoolDifficultyCategory(int index) {
        MonsterPool pool = MONSTER_POOLS.get(index);
        int m = pool.monsterCount;
        int r = pool.maxRarity;
        if (m == 1 && r <= 1) return "简单";
        if (m <= 2 && r <= 1) return "一般";
        if ((m <= 3 && r <= 2) || (m == 5 && r == 0)) return "挑战";
        if (r == 3) return "困难";
        return "灾难";
    }

    public List<Integer> generateMonstersFromPool(int poolIndex, String raceId, Random rng) {
        Map<String, List<MonsterTemplate>> raceMap = getTemplatesByRace();
        List<MonsterTemplate> raceTemplates = raceMap.get(raceId);
        if (raceTemplates == null || raceTemplates.isEmpty()) return Collections.emptyList();

        MonsterPool pool = MONSTER_POOLS.get(poolIndex);
        List<Integer> templateIds = new ArrayList<>();
        for (int rarityId : pool.raritySlots) {
            if (rarityId == E) continue;
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
        int poolIndex = pickPoolByWeight(rng);
        return generateMonstersFromPool(poolIndex, selectedRace, rng);
    }

    private int pickPoolByWeight(Random rng) {
        int size = MONSTER_POOLS.size();
        int roll = rng.nextInt(100);
        int maxR;
        if (roll < 25) maxR = 0;
        else if (roll < 50) maxR = 1;
        else if (roll < 75) maxR = 2;
        else if (roll < 90) maxR = 3;
        else maxR = 4;

        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (MONSTER_POOLS.get(i).maxRarity == maxR) candidates.add(i);
        }
        if (candidates.isEmpty()) {
            for (int i = 0; i < size; i++) candidates.add(i);
        }
        return candidates.get(rng.nextInt(candidates.size()));
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
                1, 0, 1.0, 10, 20, -1f, -1f, null));

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

    private static class MonsterConfigWrapper {
        List<MonsterTemplate> monster_templates;
    }
}
