package com.example.treasure_and_battle.model.event;

import android.graphics.Color;

public class EventConfig {
    private EventItem[] events;
    private GlobalConfig global;

    public EventItem[] getEvents() { return events; }
    public void setEvents(EventItem[] events) { this.events = events; }
    public GlobalConfig getGlobal() { return global; }
    public void setGlobal(GlobalConfig global) { this.global = global; }

    public static class GlobalConfig {
        private int maxCount;
        private int minDistance;
        private int maxDistance;
        private long generateInterval;
        private int generateMinNum;
        private int generateMaxNum;

        public int getMaxCount() { return maxCount; }
        public void setMaxCount(int maxCount) { this.maxCount = maxCount; }
        public int getMinDistance() { return minDistance; }
        public void setMinDistance(int minDistance) { this.minDistance = minDistance; }
        public int getMaxDistance() { return maxDistance; }
        public void setMaxDistance(int maxDistance) { this.maxDistance = maxDistance; }
        public long getGenerateInterval() { return generateInterval; }
        public void setGenerateInterval(long generateInterval) { this.generateInterval = generateInterval; }
        public int getGenerateMinNum() { return generateMinNum; }
        public void setGenerateMinNum(int generateMinNum) { this.generateMinNum = generateMinNum; }
        public int getGenerateMaxNum() { return generateMaxNum; }
        public void setGenerateMaxNum(int generateMaxNum) { this.generateMaxNum = generateMaxNum; }
    }

    public static class EventItem {
        private String type;
        private int weight;
        private int radius;
        private String strokeColor;
        private String fillColor;
        private long expireTime;
        private int triggerDistance;
        private EventSubItem[] subEvents;

        public int getStrokeColorInt() {
            return Color.parseColor(strokeColor);
        }
        public int getFillColorInt() {
            return Color.parseColor(fillColor);
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
        public int getRadius() { return radius; }
        public void setRadius(int radius) { this.radius = radius; }
        public String getStrokeColor() { return strokeColor; }
        public void setStrokeColor(String strokeColor) { this.strokeColor = strokeColor; }
        public String getFillColor() { return fillColor; }
        public void setFillColor(String fillColor) { this.fillColor = fillColor; }
        public long getExpireTime() { return expireTime; }
        public void setExpireTime(long expireTime) { this.expireTime = expireTime; }
        public int getTriggerDistance() { return triggerDistance; }
        public void setTriggerDistance(int triggerDistance) { this.triggerDistance = triggerDistance; }
        public EventSubItem[] getSubEvents() { return subEvents; }
        public void setSubEvents(EventSubItem[] subEvents) { this.subEvents = subEvents; }
    }

    public static class EventSubItem {
        private String key;
        private String name;
        private String desc;
        private String reward;
        private String risk;
        private String label;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDesc() { return desc; }
        public void setDesc(String desc) { this.desc = desc; }
        public String getReward() { return reward; }
        public void setReward(String reward) { this.reward = reward; }
        public String getRisk() { return risk; }
        public void setRisk(String risk) { this.risk = risk; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}
