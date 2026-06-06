package com.example.treasure_and_battle.event.handler;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.event.EventUICallback;
import com.example.treasure_and_battle.event.NeutralEventResolver;

/**
 * 许愿古井 — 递归状态机，每次投入金币许愿，获得随机品质奖励。
 * State: wishingRound, lastWishResult
 */
public class WishingWellHandler extends BaseEventHandler {

    private int wishingRound = 0;
    private String lastWishResult;

    public WishingWellHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.clearButtons();
        Character ch = ui.getCharacter();
        int coins = NeutralEventResolver.getWishingCost(wishingRound);

        if (lastWishResult != null && !lastWishResult.isEmpty()) {
            ui.addInfoText(lastWishResult);
            lastWishResult = null;
        }

        if (ch.getGold() < coins) {
            ui.addButton("金币不足" + coins + "，无法继续许愿", 0xFF888888, v -> {});
            ui.addButton("离开古井", 0xFF888888, v -> {
                ui.showResult("许愿之旅到此结束。\n（当前金币：" + ch.getGold() + "）");
                ui.switchToForwardButton();
            });
        } else {
            ui.addButton("投入" + coins + "金币许愿（第" + (wishingRound + 1) + "次）", 0xFF448AFF, v -> {
                if (!ch.spendGold(coins)) return;
                NeutralEventResolver.WishingResult r = NeutralEventResolver.resolveWishingWellWish(ch, ui.getContext(), wishingRound, coins);
                if (r.rarity == null) {
                    lastWishResult = "水面泛起涟漪，什么都没发生...\n（当前金币：" + r.currentGold + "）";
                } else {
                    lastWishResult = "古井涌出" + r.rarityDisplay + "光芒！\n 获得：" + r.rewardName + "（" + r.rarityDisplay + "）"
                            + (r.added ? "" : "\n 但背包已满！") + "\n（当前金币：" + r.currentGold + "）";
                }
                wishingRound++;
                build();
            });
            ui.addButton("离开古井", 0xFF888888, v -> {
                ui.showResult("你对着古井默默祈祷。\n（当前金币：" + ch.getGold() + "）");
                ui.switchToForwardButton();
            });
        }
    }
}
