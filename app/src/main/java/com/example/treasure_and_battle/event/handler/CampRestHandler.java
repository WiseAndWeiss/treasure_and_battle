package com.example.treasure_and_battle.event.handler;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.event.EventUICallback;
import com.example.treasure_and_battle.event.NeutralEventResolver;

/**
 * 营地休息 — 篝火旁恢复 30% 最大生命值。
 */
public class CampRestHandler extends BaseEventHandler {

    public CampRestHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.addButton("在篝火旁休息", 0xFF4CAF50, v -> {
            Character ch = ui.getCharacter();
            NeutralEventResolver.CampRestResult r = NeutralEventResolver.resolveCampRest(ch);
            if (r.alreadyFull) {
                ui.showResult("你靠在篝火旁休息，但你的状态完好，不需要恢复。");
                ui.switchToForwardButton();
                return;
            }
            ui.showResult("你靠在篝火旁休息，伤势恢复了。\n\n生命值 +" + r.healAmount
                    + "（当前：" + r.newHp + "/" + r.maxHp + "）");
            ui.switchToForwardButton();
        });
        ui.addButton("离开", 0xFF888888, v -> {
            ui.showResult("你离开了营地，继续前行。");
            ui.switchToForwardButton();
        });
    }
}
