package com.example.treasure_and_battle.event.handler;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.event.EventUICallback;
import com.example.treasure_and_battle.event.NeutralEventResolver;

/**
 * 学者 — 消耗500金币重置天赋点和技能点。
 */
public class ScholarHandler extends BaseEventHandler {

    public ScholarHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.addButton("洗点重置（消耗500金币）", 0xFFFF9800, v -> {
            Character ch = ui.getCharacter();
            NeutralEventResolver.ScholarResetResult r = NeutralEventResolver.resolveScholarReset(ch);
            if (!r.success) {
                ui.showResult(r.errorMessage);
                ui.switchToForwardButton();
                return;
            }
            ui.showResult("你消耗了500金币，天赋点和技能点已重置！\n\n返还天赋点：+" + r.talentRefunded
                    + "\n返还技能点：+" + r.skillRefunded + "\n当前金币：" + r.currentGold);
            ui.switchToForwardButton();
        });
        ui.addButton("离开", 0xFF888888, v -> {
            ui.showResult("你离开了学者，保持现有技能配置。");
            ui.switchToForwardButton();
        });
    }
}
