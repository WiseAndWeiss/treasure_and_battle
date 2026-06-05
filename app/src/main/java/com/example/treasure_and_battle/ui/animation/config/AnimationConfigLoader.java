package com.example.treasure_and_battle.ui.animation.config;

import android.content.Context;
import android.util.Log;

import com.example.treasure_and_battle.ui.animation.model.AnimationTargetType;
import com.example.treasure_and_battle.ui.animation.model.AnimationTemplate;
import com.example.treasure_and_battle.ui.animation.model.AnimationType;
import com.example.treasure_and_battle.ui.animation.model.DebugBorderAnimationTemplate;
import com.example.treasure_and_battle.ui.animation.model.TextureSetAnimationTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动画配置加载器 - 支持动画组复用的新配置结构
 */
public class AnimationConfigLoader {
    private static final String SIGNAL_CONFIG_FILE = "configs/signal_to_animation_config.json";
    private static final String GROUP_CONFIG_FILE = "configs/animation_group_config.json";
    private static final String TAG = "AnimationConfigLoader";

    private static volatile Map<String, List<AnimationTemplate>> signalCache;
    private static volatile Map<String, List<AnimationTemplate>> groupCache;

    /**
     * 加载信号到动画的映射配置
     */
    public static Map<String, List<AnimationTemplate>> loadConfigs(Context context) {
        if (signalCache != null) {
            return signalCache;
        }

        // 首先加载动画组
        Map<String, List<AnimationTemplate>> animationGroups = loadAnimationGroups(context);

        // 然后加载信号映射
        Map<String, List<AnimationTemplate>> signalMappings = new HashMap<>();

        try (InputStream is = context.getAssets().open(SIGNAL_CONFIG_FILE)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            String json = new String(buffer);

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(json, JsonObject.class);

            JsonObject signalMappingsObj = root.getAsJsonObject("signal_to_animation_templates");
            if (signalMappingsObj != null) {
                for (Map.Entry<String, JsonElement> entry : signalMappingsObj.entrySet()) {
                    String signalId = entry.getKey();
                    String groupName = entry.getValue().getAsString();

                    // 从动画组中获取具体的动画模板
                    List<AnimationTemplate> templates = animationGroups.get(groupName);
                    if (templates != null) {
                        signalMappings.put(signalId, templates);
                        Log.d(TAG, "Mapped signal '" + signalId + "' to animation group '" + groupName +
                              "' (" + templates.size() + " animations)");
                    } else {
                        Log.w(TAG, "Animation group not found: " + groupName + " for signal: " + signalId);
                    }
                }
            }

            signalCache = signalMappings;
            Log.i(TAG, "Loaded " + signalMappings.size() + " signal configurations");

        } catch (IOException e) {
            Log.e(TAG, "Failed to load signal config", e);
        }

        return signalCache;
    }

    /**
     * 加载动画组配置
     */
    private static Map<String, List<AnimationTemplate>> loadAnimationGroups(Context context) {
        if (groupCache != null) {
            return groupCache;
        }

        Map<String, List<AnimationTemplate>> animationGroups = new HashMap<>();

        try (InputStream is = context.getAssets().open(GROUP_CONFIG_FILE)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            String json = new String(buffer);

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(json, JsonObject.class);

            JsonObject groupsObj = root.getAsJsonObject("animation_groups");
            if (groupsObj != null) {
                for (Map.Entry<String, JsonElement> entry : groupsObj.entrySet()) {
                    String groupName = entry.getKey();
                    List<AnimationTemplate> templates = parseAnimationTemplates(entry.getValue(), context);
                    animationGroups.put(groupName, templates);
                    Log.d(TAG, "Loaded animation group '" + groupName + "' with " + templates.size() + " animations");
                }
            }

            groupCache = animationGroups;
            Log.i(TAG, "Loaded " + animationGroups.size() + " animation groups");

        } catch (IOException e) {
            Log.e(TAG, "Failed to load animation group config", e);
        }

        return animationGroups;
    }

    private static List<AnimationTemplate> parseAnimationTemplates(JsonElement element, Context context) {
        List<AnimationTemplate> templates = new ArrayList<>();

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                AnimationTemplate template = parseSingleTemplate(item, context);
                if (template != null) {
                    templates.add(template);
                }
            }
        }

        return templates;
    }

    private static AnimationTemplate parseSingleTemplate(JsonElement element, Context context) {
        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject obj = element.getAsJsonObject();

        try {
            // 解析通用字段
            String typeStr = obj.get("type").getAsString();
            AnimationType type = AnimationType.valueOf(typeStr);

            String targetStr = obj.get("target").getAsString();
            AnimationTargetType target = AnimationTargetType.valueOf(targetStr);

            int beginAt = obj.get("begin_at").getAsInt();
            int duration = obj.get("duration").getAsInt();

            // 根据类型创建具体模板
            switch (type) {
                case TEXTURE_SET:
                    return parseTextureSetTemplate(obj, type, target, beginAt, duration);
                case DEBUG_BORDER:
                    return parseDebugBorderTemplate(obj, type, target, beginAt, duration);
                // 未来可以在这里添加其他类型的解析
                default:
                    Log.w(TAG, "Unknown animation type: " + type);
                    return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse animation template", e);
            return null;
        }
    }

    private static TextureSetAnimationTemplate parseTextureSetTemplate(
            JsonObject obj, AnimationType type, AnimationTargetType target,
            int beginAt, int duration) {

        String position = obj.get("position").getAsString();
        float scale = obj.get("scale").getAsFloat();
        int num = obj.get("num").getAsInt();

        Log.d(TAG, "解析TextureSet模板: position=" + position + ", scale=" + scale + ", num=" + num);

        JsonArray textureArray = obj.get("texture_set").getAsJsonArray();
        String[] textureSet = new String[textureArray.size()];
        for (int i = 0; i < textureArray.size(); i++) {
            textureSet[i] = textureArray.get(i).getAsString();
        }

        return new TextureSetAnimationTemplate(type, target, beginAt, duration,
                position, scale, num, textureSet);
    }

    private static DebugBorderAnimationTemplate parseDebugBorderTemplate(
            JsonObject obj, AnimationType type, AnimationTargetType target,
            int beginAt, int duration) {

        String color = obj.get("color").getAsString();

        return new DebugBorderAnimationTemplate(type, target, beginAt, duration, color);
    }

    /**
     * 清除配置缓存
     */
    public static void clearCache() {
        signalCache = null;
        groupCache = null;
    }
}