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
import java.util.HashMap;
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
                1, 0, 1.0, 100, 10, -1f, -1f, null));

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
