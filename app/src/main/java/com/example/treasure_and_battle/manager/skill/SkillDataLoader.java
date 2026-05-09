package com.example.treasure_and_battle.manager.skill;

import android.content.Context;
import com.example.treasure_and_battle.model.skill.SkillTemplate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能数据加载工具 (SkillDataLoader)
 * 负责从 JSON 文件读取技能模板列表，构建 templateId→Template 和 skillId→templateId 的索引。
 *
 * SkillManager 和 MonsterSkillManager 曾各自内嵌 60+ 行重复加载代码，
 * 现统一由此工具类处理，消除了代码重复。
 */
public class SkillDataLoader {

    public static Map<Integer, SkillTemplate> loadTemplates(Context context, String assetFileName) {
        Map<Integer, SkillTemplate> templateMap = new HashMap<>();
        Map<String, Integer> skillIdToTemplateIdMap = new HashMap<>();
        loadInto(context, assetFileName, templateMap, skillIdToTemplateIdMap);
        return templateMap;
    }

    public static void loadInto(Context context, String assetFileName,
                                 Map<Integer, SkillTemplate> templateMap,
                                 Map<String, Integer> skillIdToTemplateIdMap) {
        try {
            InputStream is = context.getAssets().open(assetFileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<SkillConfigWrapper>() {}.getType();
            SkillConfigWrapper wrapper = new Gson().fromJson(json, type);
            if (wrapper == null || wrapper.skill_templates == null) return;

            for (SkillTemplate template : wrapper.skill_templates) {
                templateMap.put(template.getTemplateId(), template);
                skillIdToTemplateIdMap.put(template.getSkillId(), template.getTemplateId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 通用 JSON 包装类 —— SkillManager 和 MonsterSkillManager 的 JSON 结构一致 */
    private static class SkillConfigWrapper {
        List<SkillTemplate> skill_templates;
    }
}
