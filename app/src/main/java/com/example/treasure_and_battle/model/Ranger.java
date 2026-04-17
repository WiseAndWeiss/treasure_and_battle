package com.example.treasure_and_battle.model;

/**
 * 游侠职业
 * 核心属性：敏捷、幸运，定位远程输出/机动，高闪避高暴击。
 */
public class Ranger extends Profession {
    public Ranger() {
        super("游侠");
    }

    @Override
    protected void initSkillPool() {
        // ...根据文档配置：被动4，主动6，事件4... 
        passiveSkills.add(new Skill("灵巧", "获得x点敏捷，闪避率提高y%", Skill.SkillType.PASSIVE));
        passiveSkills.add(new Skill("专注", "命中率提高x%，对满血敌人造成额外伤害", Skill.SkillType.PASSIVE));
        passiveSkills.add(new Skill("道具大师", "首用x品质及以下道具不消耗", Skill.SkillType.PASSIVE));
        passiveSkills.add(new Skill("破甲箭", "暴击有概率施加破甲debuff", Skill.SkillType.PASSIVE));

        activeSkills.add(new Skill("两连射", "单体连续射击", Skill.SkillType.ACTIVE).setCosts(1, 0, 0));
        activeSkills.add(new Skill("全神贯注", "暴击率、暴击伤害提高x%", Skill.SkillType.ACTIVE).setCosts(1, 10, 0));
        activeSkills.add(new Skill("箭雨", "对所有敌人造成一次伤害", Skill.SkillType.ACTIVE).setCosts(2, 20, 0));

        eventSkills.add(new Skill("巧手", "开启宝箱有概率不消耗钥匙", Skill.SkillType.EVENT));
        eventSkills.add(new Skill("隐蔽", "怪物对你的判定范围减少x米", Skill.SkillType.EVENT));
    }

    @Override
    public void applyInitialStats(Player player) {
        player.setAgility(player.getAgility() + 8);
        player.setLuck(player.getLuck() + 5);
        player.setSpeed(player.getSpeed() + 5);
        // 初始高暴击、高闪避
        player.setPhysicalCritRate(player.getPhysicalCritRate() + 0.05);
        player.setDodgeRate(player.getDodgeRate() + 0.05);

        player.healHp(9999);
    }

    @Override
    public void applyLevelUpGrowth(Player player) {
        // 游侠专属成长：均衡偏敏捷、暴击率有概率随等级递增
        player.setMaxHp(player.getMaxHp() + 15);
        player.setMaxMp(player.getMaxMp() + 10);
        player.setSpeed(player.getSpeed() + 2);
        player.setPhysicalAttack(player.getPhysicalAttack() + 2);
        // 升级小幅提升暴击/闪避
        player.setPhysicalCritRate(player.getPhysicalCritRate() + 0.005);
    }
}
