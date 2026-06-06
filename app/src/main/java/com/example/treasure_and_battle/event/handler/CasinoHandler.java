package com.example.treasure_and_battle.event.handler;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.event.EventUICallback;
import com.example.treasure_and_battle.event.NeutralEventResolver;

/**
 * 赌场马车 — 下注100金币，40%赢、30%平、30%输。
 */
public class CasinoHandler extends BaseEventHandler {

    public CasinoHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.addButton("下注100金币", 0xFFE53935, v -> {
            Character ch = ui.getCharacter();
            if (!ch.spendGold(100)) {
                ui.showResult("你的金币不足100，无法下注。");
                ui.switchToForwardButton();
                return;
            }
            NeutralEventResolver.CasinoResult r = NeutralEventResolver.resolveCasinoWager(ch);
            if (r.won != null && r.won) {
                ui.showResult("恭喜！你赢了！\n\n 获得双倍回报：200金币！（当前金币：" + r.currentGold + "）");
            } else if (r.won == null) {
                ui.showResult("平局！你的100金币退还给你。（当前金币：" + r.currentGold + "）");
            } else {
                ui.showResult("你输了...100金币血本无归。（当前金币：" + r.currentGold + "）");
            }
            ui.switchToForwardButton();
        });
        ui.addButton("不参与赌博", 0xFF888888, v -> {
            ui.showResult("你收起好奇心，继续前行。");
            ui.switchToForwardButton();
        });
    }
}
