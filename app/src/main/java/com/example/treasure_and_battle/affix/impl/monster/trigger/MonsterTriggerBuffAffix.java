package com.example.treasure_and_battle.affix.impl.monster.trigger;

import com.example.treasure_and_battle.model.common.TriggerType;

import com.example.treasure_and_battle.affix.BaseMonsterAffix;
import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.battle.log.LogType;
import com.example.treasure_and_battle.buff.BaseBuff;
import com.example.treasure_and_battle.manager.BuffManager;
import com.example.treasure_and_battle.model.affix.AffixBuffApplyTarget;

import com.example.treasure_and_battle.model.attribute.AttributeSet;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.utils.RandomUtils;

public class MonsterTriggerBuffAffix extends BaseMonsterAffix {
	private final int buffTemplateId;
	private final AffixBuffApplyTarget applyTarget;
	private final int applyStacks;
	private final float damageToStackRatio;

	public MonsterTriggerBuffAffix(int affixId, String affixName, String description, Rarity rarity,
								   TriggerType triggerType, float value, int buffTemplateId,
								   AffixBuffApplyTarget applyTarget, int applyStacks, float damageToStackRatio) {
		super(affixId, affixName, description, rarity, triggerType, value);
		this.buffTemplateId = buffTemplateId;
		this.applyTarget = applyTarget == null ? AffixBuffApplyTarget.TARGET : applyTarget;
		this.applyStacks = Math.max(1, applyStacks);
		this.damageToStackRatio = Math.max(0f, damageToStackRatio);
	}

	@Override
	public void applyAttributeBonus(AttributeSet attributeSet) {
		// 触发型词缀不直接提供常驻属性。
	}

	@Override
	public void onTrigger(BattleEntity owner, BattleContext context) {
		if (owner == null || context == null || owner.getContext() == null) {
			return;
		}
		if (!RandomUtils.checkProbability(affixValue)) {
			return;
		}

		int resolvedStacks = resolveApplyStacks(context);
		if (resolvedStacks <= 0) {
			return;
		}

		BattleEntity targetEntity = applyTarget == AffixBuffApplyTarget.SELF ? owner : context.currentTarget;
		if (targetEntity == null) {
			return;
		}

		BuffManager buffManager = BuffManager.getInstance(owner.getContext());
		String buffName = "未知状态";
		for (int i = 0; i < resolvedStacks; i++) {
			BaseBuff buff = buffManager.createBuffByTemplateId(buffTemplateId);
			if (buff == null) {
				return;
			}
			if (i == 0) {
				buffName = buff.getBuffName();
			}
			buffManager.addBuff(targetEntity, buff);
		}

		context.addLogWithMeta(
			LogType.AFFIX,
			this,
			"【词缀触发】[%s] 的 [%s] 触发，给 [%s] 施加了 %d 层 [%s]。",
			owner.getName(),
			getAffixName(),
			targetEntity.getName(),
			resolvedStacks,
			buffName
		);
	}

	private int resolveApplyStacks(BattleContext context) {
		if (damageToStackRatio > 0f) {
			int stacksByDamage = (int) (context.finalDamage * damageToStackRatio);
			return Math.max(0, stacksByDamage);
		}
		return applyStacks;
	}

	@Override
	public String getDescription() {
		if (damageToStackRatio > 0f) {
			return String.format(description, affixValue * 100f, damageToStackRatio * 100f);
		}
		return String.format(description, affixValue * 100f, applyStacks);
	}
}
