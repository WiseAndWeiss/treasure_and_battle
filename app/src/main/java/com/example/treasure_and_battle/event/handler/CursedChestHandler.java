package com.example.treasure_and_battle.event.handler;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.manager.EventManager;
import com.example.treasure_and_battle.manager.MonsterManager;
import com.example.treasure_and_battle.model.entity.Monster;

/**
 * 诅咒宝箱 — 打开后触发怪物先手偷袭。
 */
public class CursedChestHandler extends BaseEventHandler {

    public CursedChestHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.addButton("打开宝箱", 0xFF9C27B0, v -> {
            EventManager em = EventManager.getInstance(ui.getContext());
            if (em.getCurrentBattleMonster() == null) {
                em.setCurrentBattleMonster(MonsterManager.getInstance(ui.getContext()).createRandomMonster());
            }
            em.setCurrentBattleSurprise(BattleContext.SurpriseDirection.MONSTER_SURPRISE);
            Monster monster = em.getCurrentBattleMonster();
            ui.showResult("紫黑色雾气喷涌而出！\n\n" + monster.getName() + "从暗处扑来，怪物获得了先手攻击！");
            ui.switchToBattleButton();
        });
        ui.addButton("就此离开", 0xFF888888, v -> {
            ui.showResult("你绕过了散发不祥气息的宝箱。");
            ui.switchToForwardButton();
        });
    }
}
