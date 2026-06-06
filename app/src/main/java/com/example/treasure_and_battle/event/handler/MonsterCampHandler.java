package com.example.treasure_and_battle.event.handler;

import com.example.treasure_and_battle.battle.BattleContext;
import com.example.treasure_and_battle.event.EventUICallback;
import com.example.treasure_and_battle.manager.event.EventManager;
import com.example.treasure_and_battle.manager.battle.MonsterManager;

/**
 * 休息的怪物 — 偷袭怪物，必定先手攻击。
 */
public class MonsterCampHandler extends BaseEventHandler {

    public MonsterCampHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.addButton("偷袭怪物", 0xFFE53935, v -> {
            EventManager em = EventManager.getInstance(ui.getContext());
            if (em.getCurrentBattleMonster() == null) {
                em.setCurrentBattleMonster(MonsterManager.getInstance(ui.getContext()).createRandomMonster());
            }
            em.setCurrentBattleSurprise(BattleContext.SurpriseDirection.PLAYER_SURPRISE);
            ui.launchBattle();
        });
        ui.addButton("悄悄离开", 0xFF888888, v -> {
            ui.showResult("你屏住呼吸，悄悄绕过了正在休息的怪物。");
            ui.switchToForwardButton();
        });
    }
}
