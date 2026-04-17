package com.example.treasure_and_battle.model;

/**
 * 战士职业
 * 核心属性：力量、体魄，定位近战坦克/物理输出，生存能力强。
 */
public class Warrior extends Profession {
    public Warrior() {
        super("战士");
    }

    @Override
    protected void initSkillPool() {
        // 4个被动技能
        passiveSkills.add(new Skill("强健体魄", "获得x点体魄，受到的伤害减少y%", Skill.SkillType.PASSIVE));
        passiveSkills.add(new Skill("嗜血", "你造成伤害的x%会治疗你自己", Skill.SkillType.PASSIVE));
        passiveSkills.add(new Skill("养精蓄锐", "未用完的行动点至多保留x点到下一回合，战斗开始加y点", Skill.SkillType.PASSIVE));
        passiveSkills.add(new Skill("刀刃见血", "每次物理伤害，x概率施加y层流血debuff", Skill.SkillType.PASSIVE));

        // 3-6个主动技能
        activeSkills.add(new Skill("痛击", "单体打击", Skill.SkillType.ACTIVE).setCosts(1, 0, 0));
        activeSkills.add(new Skill("战斗姿态", "物攻、物防、速度提高x%", Skill.SkillType.ACTIVE).setCosts(2, 0, 0));
        activeSkills.add(new Skill("全力以赴", "耗血耗蓝获行动点", Skill.SkillType.ACTIVE).setCosts(0, 10, 10)); // 作为一次性爆发

        // 事件技能
        eventSkills.add(new Skill("生命源泉", "受到的治疗效果提高x%", Skill.SkillType.EVENT));
        eventSkills.add(new Skill("暴力执法", "越高级宝箱暴力打开成功率越低，失败无奖励", Skill.SkillType.EVENT));
    }

    @Override
    public void applyInitialStats(Player player) {
        player.setStrength(player.getStrength() + 8);
        player.setPhysique(player.getPhysique() + 8);
        player.setMaxHp(player.getMaxHp() + 50);
        // 赋予默认满血
        player.healHp(9999);
    }

    @Override
    public void applyLevelUpGrowth(Player player) {
        // 战士专属成长偏移：更高的血量成长度，物理加成更多
        player.setMaxHp(player.getMaxHp() + 20);
        player.setMaxMp(player.getMaxMp() + 5);
        player.setPhysicalAttack(player.getPhysicalAttack() + 3);
        player.setPhysicalDefense(player.getPhysicalDefense() + 3);
    }
}
