package com.example.treasure_and_battle.event.handler;

import android.view.View;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.EventManager;
import com.example.treasure_and_battle.event.NeutralEventResolver;

/**
 * 占卜小屋 — 消耗300金币揭示一个未知事件。
 */
public class DivinationHandler extends BaseEventHandler {

    public DivinationHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        if (EventManager.getInstance(ui.getContext()).hasUnknownEvents()) {
            ui.addButton("付300金币占卜", 0xFF7B1FA2, v -> {
                Character ch = ui.getCharacter();
                if (!ch.spendGold(300)) {
                    ui.showResult("你的金币不足300，无法支付占卜费用。");
                    ui.switchToForwardButton();
                    return;
                }
                String revealResult = NeutralEventResolver.resolveDivination(ch, ui.getContext());
                ui.showResult(revealResult + "\n（当前金币：" + ch.getGold() + "）");
                ui.switchToForwardButton();
            });
        } else {
            View btn = ui.addButton("付300金币占卜", 0xFFAAAAAA, v -> {});
            btn.setEnabled(false);
        }
        ui.addButton("不感兴趣", 0xFF888888, v -> {
            ui.showResult("你觉得占卜师只是在故弄玄虚，直接走开了。");
            ui.switchToForwardButton();
        });
        if (!EventManager.getInstance(ui.getContext()).hasUnknownEvents()) {
            ui.showResult("占卜师遗憾地告诉你，当前地图上没有未知的迷雾需要揭示。");
            ui.switchToForwardButton();
        }
    }
}
