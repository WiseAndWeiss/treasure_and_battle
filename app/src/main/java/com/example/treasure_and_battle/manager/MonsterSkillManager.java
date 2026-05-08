package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.skill.Skill;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 怪物技能管理器 (MonsterSkillManager)
 * 单例模式，负责从 monster_skill_config.json 加载怪物专属技能模板，
 * 并通过反射创建技能实例。
 *
 * 设计上与 SkillManager 完全对齐，但职责分离：
 *   - SkillManager      → 加载玩家技能 (skill_config.json)
 *   - MonsterSkillManager → 加载怪物技能 (monster_skill_config.json)
 */
public class MonsterSkillManager {
    private static MonsterSkillManager instance;
    private Context context;
    private Gson gson;

    private Map<Integer, SkillTemplate> templateMap = new HashMap<>();
    private Map<String, Integer> skillIdToTemplateIdMap = new HashMap<>();

    private MonsterSkillManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadMonsterSkillTemplates();
    }

    public static synchronized MonsterSkillManager getInstance(Context context) {
        if (instance == null) {
            instance = new MonsterSkillManager(context);
        }
        return instance;
    }

    public static synchronized void releaseInstance() {
        instance = null;
    }

    // ====================== 1. 加载怪物技能模板 ======================

    private void loadMonsterSkillTemplates() {
        try {
            InputStream is = context.getAssets().open("monster_skill_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<SkillConfigWrapper>() {}.getType();
            SkillConfigWrapper wrapper = gson.fromJson(json, type);
            if (wrapper != null && wrapper.skill_templates != null) {
                for (SkillTemplate template : wrapper.skill_templates) {
                    templateMap.put(template.getTemplateId(), template);
                    skillIdToTemplateIdMap.put(template.getSkillId(), template.getTemplateId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================== 2. 通过模板ID创建技能实例 ======================

    public Skill createSkillByTemplateId(int templateId, int level) {
        SkillTemplate template = templateMap.get(templateId);
        if (template == null) return null;

        level = Math.max(0, Math.min(level, template.getMaxLevel()));

        try {
            String skillClassName = template.getSkillClassName();
            Class<?> skillClass = Class.forName(skillClassName);

            if (!Skill.class.isAssignableFrom(skillClass)) {
                throw new IllegalArgumentException("Skill class must extend Skill: " + skillClassName);
            }

            java.lang.reflect.Constructor<?> constructor = skillClass.getConstructor(SkillTemplate.class);
            Skill skill = (Skill) constructor.newInstance(template);

            if (level > 0) {
                skill.setLevel(level);
            }

            return skill;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ====================== 3. 通过技能ID创建技能实例 ======================

    public Skill createSkillBySkillId(String skillId, int level) {
        Integer templateId = skillIdToTemplateIdMap.get(skillId);
        if (templateId == null) return null;
        return createSkillByTemplateId(templateId, level);
    }

    public Skill createSkillBySkillId(String skillId) {
        return createSkillBySkillId(skillId, 1);
    }

    // ====================== 4. 查询方法 ======================

    public SkillTemplate getSkillTemplate(int templateId) {
        return templateMap.get(templateId);
    }

    public SkillTemplate getSkillTemplateBySkillId(String skillId) {
        Integer templateId = skillIdToTemplateIdMap.get(skillId);
        return templateId != null ? templateMap.get(templateId) : null;
    }

    public boolean hasSkill(String skillId) {
        return skillIdToTemplateIdMap.containsKey(skillId);
    }

    // ====================== 内部类 ======================

    private static class SkillConfigWrapper {
        List<SkillTemplate> skill_templates;
    }
}
