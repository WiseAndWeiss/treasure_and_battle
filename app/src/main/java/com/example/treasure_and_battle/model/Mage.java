package com.example.treasure_and_battle.model;

/**
 * 法师职业
 * 核心属性：智力、精神，定位法术输出/辅助，高爆发高蓝耗。
 */
public class Mage extends Profession {
    public Mage() {
        super("法师");
    }

    @Override
    protected void initSkillPool() {
        // 法师技能：被动4，主动6，事件4
        passiveSkills.add(new Skill("博学", "获得x点智力，最大mp提高y点", Skill.SkillType.PASSIVE));
        passiveSkills.add(new Skill("宁静", "获得y点精神，蓝耗降低x%", Skill.SkillType.PASSIVE));
        passiveSkills.add(new Skill("魔力光环", "回合结束时，对所有敌人造成基于你智力伤害", Skill.SkillType.PASSIVE));
        passiveSkills.add(new Skill("净化", "回合开始时，有几率移除自身debuff", Skill.SkillType.PASSIVE));

        activeSkills.add(new Skill("魔法箭", "单体法术", Skill.SkillType.ACTIVE).setCosts(1, 10, 0));
        activeSkills.add(new Skill("火球术", "群体法术并且施加燃烧", Skill.SkillType.ACTIVE).setCosts(2, 30, 0));
        activeSkills.add(new Skill("魔法盾", "获得护盾和一次净化", Skill.SkillType.ACTIVE).setCosts(2, 20, 0));

        eventSkills.add(new Skill("透视魔术", "提前查看宝箱内物品", Skill.SkillType.EVENT));
        eventSkills.add(new Skill("魔力涌动", "随时洗点且花费降低x%", Skill.SkillType.EVENT));
    }

    @Override
    public void applyInitialStats(Player player) {
        player.setIntelligence(player.getIntelligence() + 8);
        player.setSpirit(player.getSpirit() + 8);
        player.setMaxMp(player.getMaxMp() + 50);
        player.setMagicAttack(player.getMagicAttack() + 5);

        player.healHp(9999);
        player.healMp(9999);
    }

    @Override
    public void applyLevelUpGrowth(Player player) {
        // 法师专属：更强法术和法力上限
        player.setMaxHp(player.getMaxHp() + 10);
        player.setMaxMp(player.getMaxMp() + 20);
        player.setMagicAttack(player.getMagicAttack() + 3);
        player.setMagicDefense(player.getMagicDefense() + 2);
    } // 升级小幅回蓝在基类处理
}
