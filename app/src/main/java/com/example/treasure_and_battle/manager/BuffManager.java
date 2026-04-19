package com.example.treasure_and_battle.manager;

import android.content.Context;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.buff.BuffTemplate;
import com.example.treasure_and_battle.model.buff.BuffTriggerType;
import com.example.treasure_and_battle.model.buff.BuffType;
import com.example.treasure_and_battle.utils.RandomUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Buff统一管理类，和AffixManager设计完全对齐
 * 单例模式，负责Buff的加载、生成、生命周期管理和触发调度
 */
public class BuffManager {
    private static BuffManager instance;
    private Context context;
    private Gson gson;

    // Buff模板库，对应AffixManager的templateMap
    private Map<Integer, BuffTemplate> templateMap = new HashMap<>();

    private BuffManager(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        loadBuffTemplates();
    }

    public static synchronized BuffManager getInstance(Context context) {
        if (instance == null) {
            instance = new BuffManager(context);
        }
        return instance;
    }

    // ====================== 1. 加载Buff模板（对应AffixManager的加载逻辑） ======================
    private void loadBuffTemplates() {
        try {
            InputStream is = context.getAssets().open("buff_config.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            Type type = new TypeToken<BuffConfigWrapper>() {}.getType();
            BuffConfigWrapper wrapper = gson.fromJson(json, type);
            for (BuffTemplate template : wrapper.buff_templates) {
                templateMap.put(template.getTemplateId(), template);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================== 2. 生成Buff实例（对应AffixManager的生成逻辑） ======================
    public BaseBuff createBuffByTemplateId(int templateId) {
        BuffTemplate template = templateMap.get(templateId);
        if (template == null) return null;

        // 随机生成Buff数值
        float randomValue = RandomUtils.getRandomFloat(template.getMinValue(), template.getMaxValue());
        BuffType buffType = BuffType.valueOf(template.getBuffType());
        BuffTriggerType triggerType = BuffTriggerType.valueOf(template.getTriggerType());

        // 反射生成Buff实例，和AffixManager完全一致
        try {
            Class<?> buffClass = Class.forName(template.getBuffClass());
            BaseBuff buff = (BaseBuff) buffClass.getConstructor(
                    String.class, String.class, String.class,
                    BuffType.class, boolean.class, int.class,
                    int.class, boolean.class, float.class
            ).newInstance(
                    template.getBuffId(),
                    template.getBuffName(),
                    template.getDescriptionFormat(),
                    buffType,
                    template.isDispellable(),
                    template.getDefaultDuration(),
                    template.getMaxStackCount(),
                    template.isRefreshOnApply(),
                    randomValue
            );
            return buff;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ====================== 3. Buff添加/移除/堆叠管理 ======================
    public void addBuff(BattleEntity entity, BaseBuff buff) {
        List<BaseBuff> buffList = entity.getActiveBuffList();

        // 相同Buff尝试堆叠
        for (BaseBuff existingBuff : buffList) {
            if (existingBuff.getBuffId().equals(buff.getBuffId())) {
                existingBuff.tryStack(buff);
                entity.markAttributeCacheDirty();
                return;
            }
        }

        // 新增Buff
        buffList.add(buff);
        entity.markAttributeCacheDirty();
    }

    public void removeBuff(BattleEntity entity, String buffId) {
        List<BaseBuff> buffList = entity.getActiveBuffList();
        buffList.removeIf(buff -> buff.getBuffId().equals(buffId));
        entity.markAttributeCacheDirty();
    }

    public void dispelBuffs(BattleEntity entity, boolean dispelBuffs, boolean dispelDebuffs) {
        List<BaseBuff> buffList = entity.getActiveBuffList();
        Iterator<BaseBuff> iterator = buffList.iterator();

        while (iterator.hasNext()) {
            BaseBuff buff = iterator.next();
            if (!buff.isDispellable()) continue;

            if ((dispelBuffs && buff.getBuffType() == BuffType.BUFF) ||
                    (dispelDebuffs && buff.getBuffType() == BuffType.DEBUFF)) {
                iterator.remove();
            }
        }
        entity.markAttributeCacheDirty();
    }

    // ====================== 4. 生命周期管理 ======================
    public void tickBuffs(BattleEntity entity) {
        List<BaseBuff> buffList = entity.getActiveBuffList();
        boolean needInvalidate = false;

        Iterator<BaseBuff> iterator = buffList.iterator();
        while (iterator.hasNext()) {
            BaseBuff buff = iterator.next();
            if (buff.tick()) {
                iterator.remove();
                needInvalidate = true;
            }
        }

        if (needInvalidate) {
            entity.markAttributeCacheDirty();
        }
    }

    // ====================== 5. 属性加成与触发调度 ======================
    public void applyAllBuffAttributeBonus(AttributeSet attributeSet, BattleEntity entity) {
        List<BaseBuff> buffList = entity.getActiveBuffList();
        for (BaseBuff buff : buffList) {
            buff.applyAttributeBonus(attributeSet);
        }
    }

    public void triggerBuffs(BattleEntity entity, BattleContext context, BuffTriggerType triggerType) {
        List<BaseBuff> buffList = entity.getActiveBuffList();
        for (BaseBuff buff : buffList) {
            if (buff.getTriggerType() == triggerType) {
                buff.onTrigger(context, triggerType);
                context.addLogWithMeta(
                        com.example.treasure_and_battle.battle.log.LogType.BUFF,
                        buff,
                        "【Buff触发】实体 [%s] 身上的 [%s] 状态被触发。",
                        entity.getClass().getSimpleName(),
                        buff.getBuffName()
                );
            }
        }
    }

    // ====================== 配置文件包装类 ======================
    private static class BuffConfigWrapper {
        List<BuffTemplate> buff_templates;
    }
}