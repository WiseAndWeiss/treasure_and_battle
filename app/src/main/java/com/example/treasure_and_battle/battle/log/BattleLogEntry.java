package com.example.treasure_and_battle.battle.log;

/**
 * 战斗单条日志实体类
 * 记录发生的回合数、类别、具体格式化文案对象以及可能附带的元数据。
 */
public class BattleLogEntry {
    private final int round;
    private final LogType type;
    private final String messageTemplate;
    private final Object[] args;
    
    // 元数据，存放触发的来源对象或被操作的对象，便于后续UI层做点击高亮等需求（保留作Debug及扩展）
    private final Object metaData;

    public BattleLogEntry(int round, LogType type, Object metaData, String messageTemplate, Object... args) {
        this.round = round;
        this.type = type;
        this.metaData = metaData;
        this.messageTemplate = messageTemplate;
        this.args = args;
    }

    public int getRound() { 
        return round; 
    }
    
    public LogType getType() { 
        return type; 
    }
    
    public Object getMetaData() { 
        return metaData; 
    }
    
    public String getFormattedMessage() {
        if (args == null || args.length == 0) {
            return messageTemplate;
        }
        return String.format(messageTemplate, args);
    }

    @Override
    public String toString() {
        return String.format("[回合 %d] [%s] %s", round, type.name(), getFormattedMessage());
    }
}