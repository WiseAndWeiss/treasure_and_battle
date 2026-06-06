package com.example.treasure_and_battle.manager.character;

import android.content.Context;
import android.util.Log;

import com.example.treasure_and_battle.manager.skill.SkillManager;
import com.example.treasure_and_battle.model.profession.Profession;
import com.example.treasure_and_battle.model.profession.ProfessionTemplate;
import com.example.treasure_and_battle.model.profession.ProfessionType;
import com.example.treasure_and_battle.skill.SkillTree;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 职业管理器
 * 单例模式，负责职业模板的加载和Profession实例的创建
 */
public class ProfessionManager {
    private static final String TAG = "ProfessionManager";
    private static ProfessionManager instance;

    private Context context;
    private Gson gson;
    private SkillManager skillManager;

    // 职业模板库
    private Map<Integer, ProfessionTemplate> professionTemplateMap = new HashMap<>();
    // 职业类型到模板ID的映射
    private Map<ProfessionType, Integer> professionTypeToTemplateIdMap = new HashMap<>();

    private ProfessionManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context;
        this.gson = new Gson();
        this.skillManager = SkillManager.getInstance(context);
        loadProfessionTemplates();
    }

    /**
     * 获取ProfessionManager单例实例
     * @param context Android上下文
     * @return ProfessionManager实例
     */
    public static synchronized ProfessionManager getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (instance == null) {
            instance = new ProfessionManager(context);
        }
        return instance;
    }

    /**
     * 从配置文件加载职业模板
     */
    private void loadProfessionTemplates() {
        try {
            InputStream is = context.getAssets().open("configs/profession_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<ProfessionConfigWrapper>() {}.getType();
            ProfessionConfigWrapper wrapper = gson.fromJson(json, type);

            int autoIncrementTemplateId = 1001; // 自动生成模板ID
            for (ProfessionTemplate template : wrapper.profession_templates) {
                // 如果配置文件没有templateId，自动生成
                if (template.getTemplateId() == 0) {
                    template.setTemplateId(autoIncrementTemplateId++);
                }

                professionTemplateMap.put(template.getTemplateId(), template);
                professionTypeToTemplateIdMap.put(template.getProfessionType(), template.getTemplateId());

                Log.d(TAG, "Loaded profession: " + template.getProfessionName() +
                    " (" + template.getProfessionType() + ")");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load profession templates", e);
        }
    }

    /**
     * 通过职业类型获取职业模板
     * @param professionType 职业类型
     * @return 职业模板，如果不存在则返回null
     */
    public ProfessionTemplate getProfessionTemplate(ProfessionType professionType) {
        Integer templateId = professionTypeToTemplateIdMap.get(professionType);
        return templateId != null ? professionTemplateMap.get(templateId) : null;
    }

    /**
     * 通过模板ID获取职业模板
     * @param templateId 模板ID
     * @return 职业模板，如果不存在则返回null
     */
    public ProfessionTemplate getProfessionTemplateByTemplateId(int templateId) {
        return professionTemplateMap.get(templateId);
    }

    /**
     * 检查职业是否存在
     * @param professionType 职业类型
     * @return 如果职业存在返回true，否则返回false
     */
    public boolean hasProfession(ProfessionType professionType) {
        return professionTypeToTemplateIdMap.containsKey(professionType);
    }

    /**
     * 通过职业类型创建Profession实例
     * @param professionType 职业类型
     * @return Profession实例，如果职业不存在则返回null
     */
    public Profession createProfession(ProfessionType professionType) {
        ProfessionTemplate template = getProfessionTemplate(professionType);
        if (template == null) {
            Log.e(TAG, "Profession template not found for type: " + professionType);
            return null;
        }

        return createProfessionFromTemplate(template);
    }

    /**
     * 通过模板ID创建Profession实例
     * @param templateId 模板ID
     * @return Profession实例，如果模板不存在则返回null
     */
    public Profession createProfessionByTemplateId(int templateId) {
        ProfessionTemplate template = professionTemplateMap.get(templateId);
        if (template == null) {
            Log.e(TAG, "Profession template not found for templateId: " + templateId);
            return null;
        }

        return createProfessionFromTemplate(template);
    }

    /**
     * 从模板创建Profession实例
     * @param template 职业模板
     * @return Profession实例
     */
    private Profession createProfessionFromTemplate(ProfessionTemplate template) {
        // 获取三个技能树
        SkillTree activeSkillTree = skillManager.createSkillTreeBySkillTreeId(
            template.getActiveSkillTreeTemplates());
        SkillTree passiveSkillTree = skillManager.createSkillTreeBySkillTreeId(
            template.getPassiveSkillTreeTemplates());
        SkillTree eventSkillTree = skillManager.createSkillTreeBySkillTreeId(
            template.getEventSkillTreeTemplates());

        if (activeSkillTree == null) {
            Log.e(TAG, "Failed to create active skill tree: " + template.getActiveSkillTreeTemplates());
            return null;
        }

        if (passiveSkillTree == null) {
            Log.e(TAG, "Failed to create passive skill tree: " + template.getPassiveSkillTreeTemplates());
            return null;
        }

        if (eventSkillTree == null) {
            Log.e(TAG, "Failed to create event skill tree: " + template.getEventSkillTreeTemplates());
            return null;
        }

        return new Profession(
            template.getProfessionName(),
            template.getProfessionType(),
            activeSkillTree,
            passiveSkillTree,
            eventSkillTree
        );
    }

    /**
     * 获取所有可用的职业类型
     * @return 所有职业类型列表
     */
    public List<ProfessionType> getAllProfessionTypes() {
        return new ArrayList<>(professionTypeToTemplateIdMap.keySet());
    }

    /**
     * 获取所有职业模板
     * @return 所有职业模板列表
     */
    public List<ProfessionTemplate> getAllProfessionTemplates() {
        return new ArrayList<>(professionTemplateMap.values());
    }

    // ====================== 配置文件包装类 ======================
    private static class ProfessionConfigWrapper {
        List<ProfessionTemplate> profession_templates;
    }
}
