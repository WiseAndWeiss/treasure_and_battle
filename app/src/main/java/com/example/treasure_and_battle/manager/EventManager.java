package com.example.treasure_and_battle.manager;

import android.content.Context;
import android.util.Log;
import com.amap.api.maps.AMap;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.event.EventConfig;
import com.example.treasure_and_battle.utils.GeoUtils;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EventManager {
    private static final String TAG = "EventManager";
    private static EventManager sInstance;
    private Context mContext;
    private AMap mAMap;
    private EventConfig mEventConfig;
    private List<EventCircle> mEventCircleList = new ArrayList<>();
    private Random mRandom = new Random();
    private LatLng mCurrentLatLng;
    private Monster mCurrentBattleMonster;
    private java.util.List<Monster> mCurrentBattleMonsters;
    private BattleContext.SurpriseDirection mCurrentBattleSurprise = BattleContext.SurpriseDirection.NONE;

    private long mOverrideGenerateInterval = -1;
    private long mBattleExpireOverride = -1;
    private long mBenefitExpireOverride = -1;
    private long mNeutralExpireOverride = -1;
    private boolean mPaused = false;

    public static class EventCircle {
        public Circle circle;
        public LatLng position;
        public long createTime;
        public boolean isTriggered;
        public EventConfig.EventItem config;
        public EventConfig.EventSubItem selectedSubEvent;
        public Monster monster;
        public int[] previewTemplateIds;
        public String[] previewMonsterNames;
        public String[] previewMonsterRarities;

        public EventCircle(Circle circle, LatLng position, EventConfig.EventItem config) {
            this.circle = circle;
            this.position = position;
            this.createTime = System.currentTimeMillis();
            this.isTriggered = false;
            this.config = config;
            this.selectedSubEvent = null;
            this.monster = null;
        }
    }

    public static EventManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new EventManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private EventManager(Context context) {
        mContext = context;
        loadEventConfigFromAssets();
    }

    public void bindAMap(AMap aMap) {
        mAMap = aMap;
    }

    public void updateCurrentLatLng(LatLng latLng) {
        mCurrentLatLng = latLng;
    }

    private void loadEventConfigFromAssets() {
        try {
            InputStream is = mContext.getAssets().open("configs/event_config.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            is.close();

            Gson gson = new Gson();
            mEventConfig = gson.fromJson(sb.toString(), EventConfig.class);
            Log.d(TAG, "✅ 事件配置加载成功");

        } catch (Exception e) {
            e.printStackTrace();
            initDefaultConfig();
        }
    }

    private void initDefaultConfig() {
        mEventConfig = new EventConfig();
        EventConfig.GlobalConfig global = new EventConfig.GlobalConfig();
        global.setMaxCount(12);
        global.setMinDistance(100);
        global.setMaxDistance(500);
        global.setGenerateInterval(10000);
        global.setGenerateMinNum(2);
        global.setGenerateMaxNum(4);
        mEventConfig.setGlobal(global);

        EventConfig.EventItem defaultEvent = new EventConfig.EventItem();
        defaultEvent.setType("BATTLE");
        defaultEvent.setWeight(100);
        defaultEvent.setRadius(30);
        defaultEvent.setStrokeColor("#FFFF0000");
        defaultEvent.setFillColor("#30FF0000");
        defaultEvent.setExpireTime(30000);
        defaultEvent.setTriggerDistance(70);
        defaultEvent.setSubEvents(new EventConfig.EventSubItem[0]);
        mEventConfig.setEvents(new EventConfig.EventItem[]{defaultEvent});
    }

    // ==========================
    //  clean version — 只实现规则 1 和 2
    // ==========================
    public int generateRandomEvents() {
        if (mCurrentLatLng == null || mAMap == null || mEventConfig == null) return 0;
        if (mPaused) return 0;

        EventConfig.GlobalConfig global = mEventConfig.getGlobal();
        int availableCount = global.getMaxCount() - mEventCircleList.size();
        if (availableCount <= 0) return 0;

        int generateCount = mRandom.nextInt(global.getGenerateMaxNum() - global.getGenerateMinNum() + 1)
                + global.getGenerateMinNum();
        generateCount = Math.min(generateCount, availableCount);

        int createdCount = 0;
        EventConfig.EventItem[] eventTypes = mEventConfig.getEvents();

        for (int i = 0; i < generateCount; i++) {
            EventConfig.EventItem eventType = pickEventByWeight(eventTypes);
            LatLng finalPos = null;
            boolean valid = false;

            // 重试最多50次找合法位置
            for (int retry = 0; retry < 50; retry++) {
                // 规则1：所有事件 100-500米
                double dist = 100 + mRandom.nextDouble() * 400;
                double angle = mRandom.nextDouble() * Math.PI * 2;
                LatLng p = calculateLatLng(mCurrentLatLng, dist, angle);

                // 规则2：战斗事件之间至少间隔150米
                boolean tooClose = false;
                for (EventCircle ec : mEventCircleList) {
                    double d = GeoUtils.calculateDistance(p, ec.position);

                    if (eventType.getType().equals("BATTLE") && ec.config.getType().equals("BATTLE")) {
                        if (d < 150) {
                            tooClose = true;
                            break;
                        }
                    }

                    // 所有事件之间至少间隔 60 米，避免重叠
                    if (d < 60) {
                        tooClose = true;
                        break;
                    }
                }

                if (!tooClose) {
                    finalPos = p;
                    valid = true;
                    break;
                }
            }

            if (!valid || finalPos == null) continue;

            Circle circle = mAMap.addCircle(new CircleOptions()
                    .center(finalPos)
                    .radius(eventType.getRadius())
                    .strokeColor(eventType.getStrokeColorInt())
                    .strokeWidth(4)
                    .fillColor(eventType.getFillColorInt()));
            if (circle == null) continue;

            mEventCircleList.add(new EventCircle(circle, finalPos, eventType));
            EventCircle ec = mEventCircleList.get(mEventCircleList.size() - 1);
            ec.selectedSubEvent = pickRandomSubEvent(eventType);
            if ("BATTLE".equals(eventType.getType())) {
                ec.monster = MonsterManager.getInstance(mContext).createRandomMonster();
            }
            if ("NEUTRAL".equals(eventType.getType()) && ec.selectedSubEvent != null) {
                String subKey = ec.selectedSubEvent.getKey();
                if ("monster_camp".equals(subKey) || "cursed_chest".equals(subKey)) {
                    ec.monster = MonsterManager.getInstance(mContext).createRandomMonster();
                }
            }
            createdCount++;
        }

        return createdCount;
    }

    public int checkExpiredEvents() {
        if (mEventCircleList.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        Iterator<EventCircle> it = mEventCircleList.iterator();
        int count = 0;

        while (it.hasNext()) {
            EventCircle e = it.next();
            if (e.circle == null) {
                it.remove();
                count++;
                continue;
            }
            long expire = getEffectiveExpire(e.config.getType(), e.config.getExpireTime());
            if (!e.isTriggered && now - e.createTime >= expire) {
                if (mAMap != null) e.circle.remove();
                it.remove();
                count++;
            }
        }
        return count;
    }

    private long getEffectiveExpire(String type, long original) {
        if ("BATTLE".equals(type) && mBattleExpireOverride > 0) return mBattleExpireOverride;
        if ("BENEFIT".equals(type) && mBenefitExpireOverride > 0) return mBenefitExpireOverride;
        if ("NEUTRAL".equals(type) && mNeutralExpireOverride > 0) return mNeutralExpireOverride;
        if ("UNKNOWN".equals(type) && mNeutralExpireOverride > 0) return mNeutralExpireOverride;
        return original;
    }

    public long getEffectiveGenerateInterval() {
        return mOverrideGenerateInterval > 0 ? mOverrideGenerateInterval : mEventConfig.getGlobal().getGenerateInterval();
    }

    public boolean isPaused() { return mPaused; }
    public void setPaused(boolean paused) { mPaused = paused; }

    public void setOverrideGenerateInterval(long ms) {
        mOverrideGenerateInterval = ms;
    }

    public void setExpireOverrides(long battleMs, long benefitMs, long neutralMs) {
        mBattleExpireOverride = battleMs;
        mBenefitExpireOverride = benefitMs;
        mNeutralExpireOverride = neutralMs;
    }

    public boolean checkEventInTriggerRange() {
        if (mCurrentLatLng == null) return false;
        for (EventCircle e : mEventCircleList) {
            if (e.isTriggered) continue;
            double d = GeoUtils.calculateDistance(mCurrentLatLng, e.position);
            if (d <= e.config.getTriggerDistance()) return true;
        }
        return false;
    }

    /**
     * 获取当前触发范围内的第一个事件（用于判断事件类型）
     */
    public EventConfig.EventItem getTriggeredEvent() {
        if (mCurrentLatLng == null) return null;
        for (EventCircle e : mEventCircleList) {
            if (e.isTriggered) continue;
            double d = GeoUtils.calculateDistance(mCurrentLatLng, e.position);
            if (d <= e.config.getTriggerDistance()) return e.config;
        }
        return null;
    }

    public boolean hasUnknownEvents() {
        for (EventCircle ec : mEventCircleList) {
            if ("UNKNOWN".equals(ec.config.getType())) {
                return true;
            }
        }
        return false;
    }

    public String revealUnknownEvent() {
        List<EventCircle> unknownCircles = new ArrayList<>();
        for (EventCircle ec : mEventCircleList) {
            if ("UNKNOWN".equals(ec.config.getType())) {
                unknownCircles.add(ec);
            }
        }
        if (unknownCircles.isEmpty()) {
            return "当前地图上没有未知事件可揭示。";
        }

        EventCircle target = unknownCircles.get(mRandom.nextInt(unknownCircles.size()));

        EventConfig.EventItem eventTypes = findEventConfigByType(resolveUnknownType());
        if (eventTypes == null) {
            return "占卜出现误差，未能揭示事件。";
        }

        EventConfig.EventSubItem sub = pickRandomSubEvent(eventTypes);

        target.config = eventTypes;
        target.selectedSubEvent = sub;
        target.isTriggered = false;
        target.createTime = System.currentTimeMillis();
        target.monster = null;

        target.circle.setRadius(eventTypes.getRadius());
        target.circle.setStrokeColor(eventTypes.getStrokeColorInt());
        target.circle.setStrokeWidth(4);
        target.circle.setFillColor(eventTypes.getFillColorInt());

        if ("BATTLE".equals(eventTypes.getType())) {
            target.monster = MonsterManager.getInstance(mContext).createRandomMonster();
        }
        if ("NEUTRAL".equals(eventTypes.getType()) && sub != null) {
            String subKey = sub.getKey();
            if ("monster_camp".equals(subKey) || "cursed_chest".equals(subKey)) {
                target.monster = MonsterManager.getInstance(mContext).createRandomMonster();
            }
        }

        return "占卜成功！一个未知事件被揭示为：" + (sub != null ? sub.getName() : eventTypes.getType());
    }

    private EventConfig.EventItem findEventConfigByType(String type) {
        for (EventConfig.EventItem item : mEventConfig.getEvents()) {
            if (item.getType().equals(type)) return item;
        }
        return null;
    }

    private String resolveUnknownType() {
        int roll = mRandom.nextInt(100);
        if (roll < 70) return "BATTLE";
        if (roll < 85) return "NEUTRAL";
        return "BENEFIT";
    }

    /**
     * 获取触发事件的按钮文本
     */
    public String getTriggeredEventActionLabel() {
        if (mCurrentLatLng == null) return "进入";
        for (EventCircle e : mEventCircleList) {
            if (e.isTriggered) continue;
            double d = GeoUtils.calculateDistance(mCurrentLatLng, e.position);
            if (d <= e.config.getTriggerDistance()) {
                if (e.selectedSubEvent != null) return e.selectedSubEvent.getLabel();
                return "探索";
            }
        }
        return "进入";
    }

    /**
     * 获取触发事件的按钮 key
     */
    public String getTriggeredEventActionKey() {
        if (mCurrentLatLng == null) return "";
        for (EventCircle e : mEventCircleList) {
            if (e.isTriggered) continue;
            double d = GeoUtils.calculateDistance(mCurrentLatLng, e.position);
            if (d <= e.config.getTriggerDistance()) {
                if (e.selectedSubEvent != null) return e.selectedSubEvent.getKey();
                return "";
            }
        }
        return "";
    }

    /**
     * 获取触发事件选中的 action 对象
     */
    public EventConfig.EventSubItem getTriggeredSubEvent() {
        if (mCurrentLatLng == null) return null;
        for (EventCircle e : mEventCircleList) {
            if (e.isTriggered) continue;
            double d = GeoUtils.calculateDistance(mCurrentLatLng, e.position);
            if (d <= e.config.getTriggerDistance()) return e.selectedSubEvent;
        }
        return null;
    }

    /**
     * 从事件配置的 actions 中随机选一个
     */
    public EventConfig.EventSubItem pickRandomSubEvent(EventConfig.EventItem eventType) {
        EventConfig.EventSubItem[] subs = eventType.getSubEvents();
        if (subs == null || subs.length == 0) return null;
        return subs[mRandom.nextInt(subs.length)];
    }

    public int removeTriggeredEvents() {
        if (mCurrentLatLng == null) return 0;
        Iterator<EventCircle> it = mEventCircleList.iterator();
        int count = 0;

        while (it.hasNext()) {
            EventCircle e = it.next();
            double d = GeoUtils.calculateDistance(mCurrentLatLng, e.position);
            if (d <= e.config.getTriggerDistance()) {
                e.circle.remove();
                e.isTriggered = true;
                it.remove();
                count++;
                break;
            }
        }
        return count;
    }

    public EventCircle getTriggeredEventCircle() {
        if (mCurrentLatLng == null) return null;
        for (EventCircle e : mEventCircleList) {
            if (e.isTriggered) continue;
            double d = GeoUtils.calculateDistance(mCurrentLatLng, e.position);
            if (d <= e.config.getTriggerDistance()) return e;
        }
        return null;
    }

    public void removeEventCircle(EventCircle ec) {
        if (ec != null && mEventCircleList.remove(ec)) {
            ec.circle.remove();
        }
    }

    public void clearAllEvents() {
        for (EventCircle e : mEventCircleList) e.circle.remove();
        mEventCircleList.clear();
    }

    public LatLng getCurrentLatLng() {
        return mCurrentLatLng;
    }

    public void addDebugEvent(EventCircle ec) {
        mEventCircleList.add(ec);
    }

    public int removeEventsAtPosition(LatLng pos, double radiusMeters) {
        if (pos == null || mEventCircleList.isEmpty()) return 0;
        Iterator<EventCircle> it = mEventCircleList.iterator();
        int count = 0;
        while (it.hasNext()) {
            EventCircle e = it.next();
            if (GeoUtils.calculateDistance(pos, e.position) <= radiusMeters) {
                e.circle.remove();
                it.remove();
                count++;
            }
        }
        return count;
    }

    public void setCurrentBattleMonster(Monster monster) {
        mCurrentBattleMonster = monster;
    }

    public Monster getCurrentBattleMonster() {
        return mCurrentBattleMonster;
    }

    public void setCurrentBattleMonsters(java.util.List<Monster> monsters) {
        mCurrentBattleMonsters = monsters;
    }

    public java.util.List<Monster> getCurrentBattleMonsters() {
        return mCurrentBattleMonsters;
    }

    public void setCurrentBattleSurprise(BattleContext.SurpriseDirection surprise) {
        mCurrentBattleSurprise = surprise;
    }

    public BattleContext.SurpriseDirection getCurrentBattleSurprise() {
        return mCurrentBattleSurprise;
    }

    public List<EventCircle> getEventCircleList() {
        return mEventCircleList;
    }

    public EventConfig.GlobalConfig getGlobalConfig() {
        return mEventConfig.getGlobal();
    }

    public EventConfig.EventItem[] getEventItems() {
        return mEventConfig != null ? mEventConfig.getEvents() : new EventConfig.EventItem[0];
    }

    /**
     * 加权随机选择一个事件类型
     */
    private EventConfig.EventItem pickEventByWeight(EventConfig.EventItem[] eventTypes) {
        int totalWeight = 0;
        for (EventConfig.EventItem item : eventTypes) {
            totalWeight += Math.max(1, item.getWeight());
        }
        int roll = mRandom.nextInt(totalWeight);
        int cumulative = 0;
        for (EventConfig.EventItem item : eventTypes) {
            cumulative += Math.max(1, item.getWeight());
            if (roll < cumulative) {
                return item;
            }
        }
        return eventTypes[eventTypes.length - 1]; // fallback
    }

    /**
     * 解析未知事件：交互时按 70%/15%/15% 概率返回真实事件类型的小事件
     * @return 随机选中的 EventSubItem，如果对应类型无 subEvent 则返回 null
     */
    public EventConfig.EventSubItem resolveUnknownEvent() {
        EventConfig.EventItem[] eventTypes = mEventConfig.getEvents();
        int roll = mRandom.nextInt(100);

        String targetType;
        if (roll < 70) {
            targetType = "BATTLE";
        } else if (roll < 85) {
            targetType = "NEUTRAL";
        } else {
            targetType = "BENEFIT";
        }

        for (EventConfig.EventItem item : eventTypes) {
            if (item.getType().equals(targetType)) {
                return pickRandomSubEvent(item);
            }
        }
        return null;
    }

    private LatLng calculateLatLng(LatLng center, double dist, double angle) {
        double lon = center.longitude + dist * Math.sin(angle) / 111319.9;
        double lat = center.latitude + dist * Math.cos(angle) / 110942.9;
        return new LatLng(lat, lon);
    }
}