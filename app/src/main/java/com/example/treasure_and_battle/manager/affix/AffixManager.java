package com.example.treasure_and_battle.manager.affix;

import com.example.treasure_and_battle.model.common.TriggerType;

import android.content.Context;
import com.example.treasure_and_battle.affix.BaseAffix;
import com.example.treasure_and_battle.battle.BattleContext;

import com.example.treasure_and_battle.model.entity.BattleEntity;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.entity.Monster;
import com.example.treasure_and_battle.model.item.equip.EquipItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 词缀统一管理类 (AffixManager)，只负责战斗中的词缀管理
 * 单例模式，负责封装并管理游戏内玩家（通过装备）和怪物的所有临时/战斗内词缀的检查与触发调度逻辑。
 * 和 BuffManager 的设计完全对齐，负责在战斗核心生命周期中进行拦截与计算。
 */
public class AffixManager {
    private static AffixManager instance;
    private Context context;
    private OnAffixTriggerListener affixTriggerListener;

    public interface OnAffixTriggerListener {
        void onAffixTriggered(BattleEntity entity, String affixName);
    }

    private AffixManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized AffixManager getInstance(Context context) {
        if (instance == null) {
            instance = new AffixManager(context);
        }
        return instance;
    }

    public static synchronized void releaseInstance() {
        instance = null;
    }

    public void setOnAffixTriggerListener(OnAffixTriggerListener listener) {
        this.affixTriggerListener = listener;
    }

    // ====================== 1. 词缀触发与调度 ======================
    /**
     * 触发指定实体的全部合法词缀
     * @param entity 触发词缀的实体对象（玩家或怪物）
     * @param ctx 当前所处的战斗上下文
     * @param triggerType 触发时机（如：战斗开始、回合开始、攻击时等）
     */
    public void triggerAffixes(BattleEntity entity, BattleContext ctx, TriggerType triggerType) {
        List<BaseAffix> activeAffixes = getActiveAffixes(entity);

        if (activeAffixes == null || activeAffixes.isEmpty()) {
            return;
        }

        for (BaseAffix affix : activeAffixes) {
            if (affix.getTriggerType() == triggerType) {
                affix.onTrigger(entity, ctx);
                if (affixTriggerListener != null) {
                    affixTriggerListener.onAffixTriggered(entity, affix.getAffixName());
                }
                ctx.addLogWithMeta(
                        com.example.treasure_and_battle.battle.log.LogType.AFFIX,
                        affix,
                        "【词缀触发】实体 [%s] 触发了 [%s] 词缀效果。",
                        entity.getClass().getSimpleName(),
                        affix.getAffixName()
                );
            }
        }
    }

    // ====================== 2. 词缀数据源获取 ======================
    /**
     * 获取实体当前生效的所有词缀（玩家从装备获取，怪物从自身词缀库获取）
     * @param entity 战斗实体
     * @return 展开后的扁平化可用词缀列表
     */
    private List<BaseAffix> getActiveAffixes(BattleEntity entity) {
        List<BaseAffix> affixes = new ArrayList<>();

        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (player.getEquippedItems() != null) {
                for (EquipItem item : player.getEquippedItems()) {
                    if (item.getAffixes() != null) {
                        affixes.addAll(item.getAffixes());
                    }
                }
            }
        } else if (entity instanceof Monster) {
            Monster monster = (Monster) entity;
            if (monster.getEntityAffixList() != null) {
                affixes.addAll(monster.getEntityAffixList());
            }
        }

        return affixes;
    }
}
