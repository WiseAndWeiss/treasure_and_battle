package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.example.treasure_and_battle.model.skill.SkillTreeTemplate;
import com.example.treasure_and_battle.skill.Skill;
import com.example.treasure_and_battle.skill.SkillTree;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill统一管理类
 * 单例模式，负责Skill和SkillTree的加载、生成和管理
 */
public class SkillManager {
    private static SkillManager instance;
    private Context context;
    private Gson gson;

    // 技能模板库
    private Map<Integer, SkillTemplate> templateMap = new HashMap<>();
    // 技能ID到模板ID的映射（用于通过skillId查找）
    private Map<String, Integer> skillIdToTemplateIdMap = new HashMap<>();

    // 技能树模板库
    private Map<Integer, SkillTreeTemplate> skillTreeTemplateMap = new HashMap<>();
    // 技能树ID到模板ID的映射
    private Map<String, Integer> skillTreeIdToTemplateIdMap = new HashMap<>();

    private SkillManager(Context context) {
        // 直接使用传入的Context，避免在测试环境中getApplicationContext()的问题
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context;
        this.gson = new Gson();
        loadSkillTemplates();
        loadSkillTreeTemplates();
    }

    public static synchronized SkillManager getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (instance == null) {
            instance = new SkillManager(context);
        }
        return instance;
    }

    // ====================== 1. 加载技能模板 ======================
    private void loadSkillTemplates() {
        try {
            InputStream is = context.getAssets().open("skill_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<SkillConfigWrapper>() {}.getType();
            SkillConfigWrapper wrapper = gson.fromJson(json, type);
            for (SkillTemplate template : wrapper.skill_templates) {
                templateMap.put(template.getTemplateId(), template);
                skillIdToTemplateIdMap.put(template.getSkillId(), template.getTemplateId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================== 2. 通过模板ID创建技能实例 ======================
    /**
     * 通过模板ID创建技能实例（默认等级为0，未学习状态）
     * @param templateId 技能模板ID
     * @return 技能实例，如果模板不存在则返回null
     */
    public Skill createSkillByTemplateId(int templateId) {
        return createSkillByTemplateId(templateId, 0);
    }

    /**
     * 通过模板ID创建技能实例并指定等级
     * @param templateId 技能模板ID
     * @param level 技能等级（0表示未学习，1-maxLevel表示已学习）
     * @return 技能实例，如果模板不存在或等级无效则返回null
     */
    public Skill createSkillByTemplateId(int templateId, int level) {
        SkillTemplate template = templateMap.get(templateId);
        if (template == null) {
            return null;
        }

        // 验证等级有效性
        if (level < 0 || level > template.getMaxLevel()) {
            throw new IllegalArgumentException(
                String.format("Invalid skill level: %d, valid range is [0, %d]", level, template.getMaxLevel())
            );
        }

        // 通过反射创建技能实例
        try {
            String skillClassName = template.getSkillClassName();
            Class<?> skillClass = Class.forName(skillClassName);

            // 检查是否是Skill的子类
            if (!Skill.class.isAssignableFrom(skillClass)) {
                throw new IllegalArgumentException(
                    "Skill class must extend Skill: " + skillClassName
                );
            }

            // 获取构造函数（只接受SkillTemplate参数）
            java.lang.reflect.Constructor<?> constructor = skillClass.getConstructor(SkillTemplate.class);
            Skill skill = (Skill) constructor.newInstance(template);

            // 设置技能等级（通过反射调用protected的setLevel方法）
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
    /**
     * 通过技能ID创建技能实例（默认等级为0，未学习状态）
     * @param skillId 技能ID（如"slash", "spell"等）
     * @return 技能实例，如果技能不存在则返回null
     */
    public Skill createSkillBySkillId(String skillId) {
        return createSkillBySkillId(skillId, 0);
    }

    /**
     * 通过技能ID创建技能实例并指定等级
     * @param skillId 技能ID（如"slash", "spell"等）
     * @param level 技能等级（0表示未学习，1-maxLevel表示已学习）
     * @return 技能实例，如果技能不存在或等级无效则返回null
     */
    public Skill createSkillBySkillId(String skillId, int level) {
        Integer templateId = skillIdToTemplateIdMap.get(skillId);
        if (templateId == null) {
            return null;
        }
        return createSkillByTemplateId(templateId, level);
    }

    // ====================== 5. 辅助方法 ======================
    /**
     * 获取技能模板
     * @param templateId 模板ID
     * @return 技能模板，如果不存在则返回null
     */
    public SkillTemplate getSkillTemplate(int templateId) {
        return templateMap.get(templateId);
    }

    /**
     * 通过技能ID获取技能模板
     * @param skillId 技能ID
     * @return 技能模板，如果不存在则返回null
     */
    public SkillTemplate getSkillTemplateBySkillId(String skillId) {
        Integer templateId = skillIdToTemplateIdMap.get(skillId);
        return templateId != null ? templateMap.get(templateId) : null;
    }

    /**
     * 检查技能是否存在
     * @param skillId 技能ID
     * @return 如果技能存在返回true，否则返回false
     */
    public boolean hasSkill(String skillId) {
        return skillIdToTemplateIdMap.containsKey(skillId);
    }

    // ====================== 6. 技能树模板加载 ======================
    /**
     * 加载技能树模板
     */
    private void loadSkillTreeTemplates() {
        try {
            InputStream is = context.getAssets().open("skilltree_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<SkillTreeConfigWrapper>() {}.getType();
            SkillTreeConfigWrapper wrapper = gson.fromJson(json, type);

            for (SkillTreeTemplate template : wrapper.skill_tree_templates) {
                skillTreeTemplateMap.put(template.getTemplateId(), template);
                skillTreeIdToTemplateIdMap.put(template.getSkillTreeId(), template.getTemplateId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取技能树模板
     * @param templateId 模板ID
     * @return 技能树模板，如果不存在则返回null
     */
    public SkillTreeTemplate getSkillTreeTemplate(int templateId) {
        return skillTreeTemplateMap.get(templateId);
    }

    /**
     * 通过技能树ID获取技能树模板
     * @param skillTreeId 技能树ID
     * @return 技能树模板，如果不存在则返回null
     */
    public SkillTreeTemplate getSkillTreeTemplateBySkillTreeId(String skillTreeId) {
        Integer templateId = skillTreeIdToTemplateIdMap.get(skillTreeId);
        return templateId != null ? skillTreeTemplateMap.get(templateId) : null;
    }

    /**
     * 检查技能树是否存在
     * @param skillTreeId 技能树ID
     * @return 如果技能树存在返回true，否则返回false
     */
    public boolean hasSkillTree(String skillTreeId) {
        return skillTreeIdToTemplateIdMap.containsKey(skillTreeId);
    }

    /**
     * 通过技能树模板ID创建技能树实例
     * @param templateId 技能树模板ID
     * @return 技能树实例，如果模板不存在则返回null
     */
    public SkillTree createSkillTreeByTemplateId(int templateId) {
        SkillTreeTemplate template = skillTreeTemplateMap.get(templateId);
        if (template == null) {
            return null;
        }
        return new SkillTree(template, this);
    }

    /**
     * 通过技能树ID创建技能树实例
     * @param skillTreeId 技能树ID
     * @return 技能树实例，如果技能树不存在则返回null
     */
    public SkillTree createSkillTreeBySkillTreeId(String skillTreeId) {
        Integer templateId = skillTreeIdToTemplateIdMap.get(skillTreeId);
        if (templateId == null) {
            return null;
        }
        return createSkillTreeByTemplateId(templateId);
    }

    // ====================== 配置文件包装类 ======================
    private static class SkillConfigWrapper {
        List<SkillTemplate> skill_templates;
    }

    private static class SkillTreeConfigWrapper {
        List<SkillTreeTemplate> skill_tree_templates;
    }
}
