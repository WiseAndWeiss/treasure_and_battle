package com.example.treasure_and_battle.skill;

import android.content.Context;
import android.util.Log;

import com.example.treasure_and_battle.model.skill.SkillCostParams;
import com.example.treasure_and_battle.model.skill.SkillEffectParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能配置加载器
 * 纯JSON解析，不依赖Gson、Skill、Template等类
 */
public class ConfigLoader {
    private static final String TAG = "ConfigLoader";
    private static ConfigLoader instance;

    private Context context;
    private JSONObject configRoot;
    private JSONArray skillTemplatesArray;

    // 索引缓存
    private Map<Integer, JSONObject> templateIdMap;
    private Map<String, JSONObject> skillIdMap;

    private ConfigLoader(Context context) {
        // 在测试环境中直接使用传入的Context，避免getApplicationContext()的问题
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context;
        this.templateIdMap = new HashMap<>();
        this.skillIdMap = new HashMap<>();
        loadConfig();
    }

    public static synchronized ConfigLoader getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (instance == null) {
            instance = new ConfigLoader(context);
        }
        return instance;
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try {
            InputStream is = context.getAssets().open("configs/skill_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            configRoot = new JSONObject(json);
            skillTemplatesArray = configRoot.getJSONArray("skill_templates");

            // 构建索引
            buildIndex();

        } catch (Exception e) {
            Log.e(TAG, "加载配置文件失败", e);
        }
    }

    /**
     * 构建索引，加速查找
     */
    private void buildIndex() throws JSONException {
        for (int i = 0; i < skillTemplatesArray.length(); i++) {
            JSONObject template = skillTemplatesArray.getJSONObject(i);
            int templateId = template.getInt("templateId");
            String skillId = template.getString("skillId");

            templateIdMap.put(templateId, template);
            skillIdMap.put(skillId, template);
        }
    }

    /**
     * 通过templateId获取技能对象
     */
    private JSONObject getSkillByTemplateId(int templateId) {
        return templateIdMap.get(templateId);
    }

    /**
     * 通过skillId获取技能对象
     */
    private JSONObject getSkillBySkillId(String skillId) {
        return skillIdMap.get(skillId);
    }

    // ====================== 公开接口 ======================

    /**
     * 1. 通过templateId获取skillId
     */
    public String getSkillIdByTemplateId(int templateId) {
        JSONObject skill = getSkillByTemplateId(templateId);
        if (skill == null) return null;
        try {
            return skill.getString("skillId");
        } catch (JSONException e) {
            Log.e(TAG, "获取skillId失败", e);
            return null;
        }
    }

    /**
     * 2. 通过skillId获取templateId
     */
    public Integer getTemplateIdBySkillId(String skillId) {
        JSONObject skill = getSkillBySkillId(skillId);
        if (skill == null) return null;
        try {
            return skill.getInt("templateId");
        } catch (JSONException e) {
            Log.e(TAG, "获取templateId失败", e);
            return null;
        }
    }

    /**
     * 3. 获取技能名称
     */
    public String getSkillName(int templateId) {
        JSONObject skill = getSkillByTemplateId(templateId);
        if (skill == null) return null;
        try {
            return skill.getString("skillName");
        } catch (JSONException e) {
            Log.e(TAG, "获取skillName失败", e);
            return null;
        }
    }

    /**
     * 4. 获取技能类型
     */
    public String getSkillType(int templateId) {
        JSONObject skill = getSkillByTemplateId(templateId);
        if (skill == null) return null;
        try {
            return skill.getString("skillType");
        } catch (JSONException e) {
            Log.e(TAG, "获取skillType失败", e);
            return null;
        }
    }

    /**
     * 5. 获取技能范围类型
     */
    public String getSkillRangeType(int templateId) {
        JSONObject skill = getSkillByTemplateId(templateId);
        if (skill == null) return null;
        try {
            return skill.getString("skillRangeType");
        } catch (JSONException e) {
            Log.e(TAG, "获取skillRangeType失败", e);
            return null;
        }
    }

    /**
     * 6. 获取技能触发类型
     */
    public String getSkillTriggerType(int templateId) {
        JSONObject skill = getSkillByTemplateId(templateId);
        if (skill == null) return null;
        try {
            return skill.getString("TriggerType");
        } catch (JSONException e) {
            Log.e(TAG, "获取skillTriggerType失败", e);
            return null;
        }
    }

    /**
     * 7. 获取最大等级
     */
    public Integer getMaxLevel(int templateId) {
        JSONObject skill = getSkillByTemplateId(templateId);
        if (skill == null) return null;
        try {
            return skill.getInt("maxLevel");
        } catch (JSONException e) {
            Log.e(TAG, "获取maxLevel失败", e);
            return null;
        }
    }

    /**
     * 8. 获取冷却时间
     */
    public Integer getCoolDown(int templateId) {
        JSONObject skill = getSkillByTemplateId(templateId);
        if (skill == null) return null;
        try {
            return skill.getInt("cooldown");
        } catch (JSONException e) {
            Log.e(TAG, "获取cooldown失败", e);
            return null;
        }
    }

    /**
     * 9. 获取消耗参数列表
     */
    public List<SkillCostParams> getCostParamsList(int templateId) {
        JSONObject skill = getSkillByTemplateId(templateId);
        if (skill == null) return null;
        try {
            JSONArray costArray = skill.getJSONArray("costParamsList");
            List<SkillCostParams> result = new ArrayList<>();
            for (int i = 0; i < costArray.length(); i++) {
                JSONObject costObj = costArray.getJSONObject(i);
                SkillCostParams costParams = new SkillCostParams(
                    costObj.getInt("actionPointCost"),
                    costObj.getInt("mpCost"),
                    costObj.getInt("hpCost")
                );
                result.add(costParams);
            }
            return result;
        } catch (JSONException e) {
            Log.e(TAG, "获取costParamsList失败", e);
            return null;
        }
    }

    /**
     * 10. 获取指定等级的消耗参数
     * 特殊逻辑：
     * - 0级返回全0
     * - 列表为空时，各级都是全0
     * - 列表长度不足时，超出的level都用最后一个参数组
     */
    public SkillCostParams getCostParamsByLevel(int templateId, int level) {
        List<SkillCostParams> costParamsList = getCostParamsList(templateId);
        if (costParamsList == null || costParamsList.isEmpty()) {
            return new SkillCostParams(0, 0, 0);
        }

        if (level <= 0) {
            return new SkillCostParams(0, 0, 0);
        }

        // 如果level超出列表长度，使用最后一个参数组
        int index = Math.min(level - 1, costParamsList.size() - 1);
        return costParamsList.get(index);
    }

    /**
     * 11. 获取效果参数列表
     */
    public List<SkillEffectParams> getEffectParamsList(int templateId) {
        JSONObject skill = getSkillByTemplateId(templateId);
        if (skill == null) return null;
        try {
            JSONArray effectArray = skill.getJSONArray("effectParamsList");
            List<SkillEffectParams> result = new ArrayList<>();
            for (int i = 0; i < effectArray.length(); i++) {
                JSONObject effectObj = effectArray.getJSONObject(i);
                SkillEffectParams effectParams = new SkillEffectParams(
                    effectObj.optInt("x", 0),
                    effectObj.optInt("y", 0),
                    effectObj.optInt("z", 0),
                    effectObj.optInt("w", 0)
                );
                result.add(effectParams);
            }
            return result;
        } catch (JSONException e) {
            Log.e(TAG, "获取effectParamsList失败", e);
            return null;
        }
    }

    /**
     * 12. 获取指定等级的效果参数
     * 特殊逻辑：
     * - 0级或无效等级返回全0
     * - 第i级对应列表第i-1项
     */
    public SkillEffectParams getEffectParamsByLevel(int templateId, int level) {
        JSONObject skill = getSkillByTemplateId(templateId);
        if (skill == null) return new SkillEffectParams(0, 0, 0, 0);

        try {
            Integer maxLevel = getMaxLevel(templateId);
            if (level <= 0 || level > maxLevel) {
                return new SkillEffectParams(0, 0, 0, 0);
            }

            JSONArray effectArray = skill.getJSONArray("effectParamsList");
            JSONObject effectObj = effectArray.getJSONObject(level - 1);

            return new SkillEffectParams(
                effectObj.optInt("x", 0),
                effectObj.optInt("y", 0),
                effectObj.optInt("z", 0),
                effectObj.optInt("w", 0)
            );
        } catch (JSONException e) {
            Log.e(TAG, "获取effectParamsByLevel失败", e);
            return new SkillEffectParams(0, 0, 0, 0);
        }
    }
}

