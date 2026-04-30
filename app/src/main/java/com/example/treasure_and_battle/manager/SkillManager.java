package com.example.treasure_and_battle.manager;

import android.content.Context;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.model.entity.BattleEntity;

public class SkillManager {
	private static SkillManager instance;
	private final Context context;

	private SkillManager(Context context) {
		this.context = context.getApplicationContext();
	}

	public static synchronized SkillManager getInstance(Context context) {
		if (instance == null) {
			instance = new SkillManager(context);
		}
		return instance;
	}

	/**
	 * 技能执行统一入口（占位）。
	 * 由技能模块同学后续在这里对接真正的技能系统实现。
	 */
	public void executeSkill(String skillId, BattleEntity caster, BattleEntity target, BattleContext ctx) {
		// TODO: 对接正式技能系统。当前仅保留统一入口，避免战斗层散落技能调用。
	}
}
