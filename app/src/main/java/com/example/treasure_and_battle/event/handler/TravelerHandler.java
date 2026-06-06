package com.example.treasure_and_battle.event.handler;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.event.EventUICallback;
import com.example.treasure_and_battle.event.NeutralEventResolver;

/**
 * 旅人 — 赠送一件符合条件的物品换取金币。
 */
public class TravelerHandler extends BaseEventHandler {

    private NeutralEventResolver.TravelerRequestResult travelerRequest;

    public TravelerHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.clearButtons();
        Character ch = ui.getCharacter();
        travelerRequest = NeutralEventResolver.buildTravelerRequest(ch);

        if (!travelerRequest.hasEligibleItems) {
            ui.addButton("很可惜，你无法帮助他", 0xFF888888, v -> {
                ui.showResult("你的背包中没有可赠送的物品，旅人失望地离开了。");
                ui.switchToForwardButton();
            });
            return;
        }

        ui.addButton("帮助旅人", 0xFF4CAF50, v -> {
            NeutralEventResolver.TravelerRewardResult reward =
                    NeutralEventResolver.resolveTravelerReward(ch, travelerRequest.matchedItem,
                            travelerRequest.requiredRarityId);
            String resultText = "你慷慨地赠送了" + reward.itemName
                    + "，旅人感激不尽！\n\n获得金币 ×" + reward.goldReward;
            ui.showResult(resultText);
            ui.switchToForwardButton();
        });
        ui.addButton("无视旅人", 0xFF888888, v -> {
            ui.showResult("你匆匆走过，没有理会旅人求助的目光。");
            ui.switchToForwardButton();
        });
    }
}
