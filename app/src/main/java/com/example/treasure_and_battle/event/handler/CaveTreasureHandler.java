package com.example.treasure_and_battle.event.handler;

import android.view.View;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.event.NeutralEventResolver;

/**
 * 洞穴宝藏 — 5步状态机，逐步探索洞穴并获取宝物，最终可能遭遇战斗。
 * State: caveStep (0-3), caveHpLoss[4], caveItemDesc[2]
 */
public class CaveTreasureHandler extends BaseEventHandler {

    private int caveStep = 0;
    private final int[] caveHpLoss = new int[4];
    private final String[] caveItemDesc = new String[2];

    public CaveTreasureHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.clearButtons();
        Character ch = ui.getCharacter();

        String btn1Text;
        View.OnClickListener btn1Listener;
        int btn1Color = 0xFFFF9800;

        if (caveStep < 3) {
            NeutralEventResolver.CaveTreasureStepResult stepResult =
                    NeutralEventResolver.resolveCaveTreasureStep(caveStep, ch, ui.getContext());
            caveHpLoss[caveStep] = stepResult.hpLoss;

            if (caveStep == 0) {
                caveItemDesc[0] = stepResult.itemDisplayName;
                if (ch.getCurrentHp() <= stepResult.hpLoss) {
                    btn1Text = "血量太低，你已经无法继续往前走了";
                    btn1Color = 0xFF888888;
                    btn1Listener = v -> {};
                } else {
                    btn1Text = "你不小心擦破了皮肤，但是你找到了一件宝物（-"
                            + stepResult.hpLoss + "点生命，获得" + stepResult.itemDisplayName + "）";
                    btn1Listener = v -> {
                        NeutralEventResolver.applyCaveHpLoss(ch, stepResult.hpLoss);
                        caveStep = 1;
                        build();
                    };
                }
            } else if (caveStep == 1) {
                caveItemDesc[1] = stepResult.itemDisplayName;
                if (ch.getCurrentHp() <= stepResult.hpLoss) {
                    btn1Text = "血量太低，你已经无法继续往前走了";
                    btn1Color = 0xFF888888;
                    btn1Listener = v -> {};
                } else {
                    btn1Text = "你进一步深入探索，虽然受了点伤，但是你找到了一件宝物（-"
                            + stepResult.hpLoss + "点生命，获得" + stepResult.itemDisplayName + "）";
                    btn1Listener = v -> {
                        NeutralEventResolver.applyCaveHpLoss(ch, stepResult.hpLoss);
                        caveStep = 2;
                        build();
                    };
                }
            } else { // caveStep == 2
                if (ch.getCurrentHp() <= stepResult.hpLoss) {
                    btn1Text = "血量太低，你已经无法继续往前走了";
                    btn1Color = 0xFF888888;
                    btn1Listener = v -> {};
                } else {
                    btn1Text = "你即将走到洞穴最深处，但仍坚持继续探索（-"
                            + stepResult.hpLoss + "点生命）";
                    btn1Listener = v -> {
                        NeutralEventResolver.applyCaveHpLoss(ch, stepResult.hpLoss);
                        caveStep = 3;
                        build();
                    };
                }
            }
        } else {
            NeutralEventResolver.CaveTreasureStepResult stepResult =
                    NeutralEventResolver.resolveCaveTreasureStep(3, ch, ui.getContext());

            if (stepResult.isBattle) {
                btn1Text = "洞穴里有一只猛兽，你被迫与它战斗！";
                btn1Color = 0xFFE53935;
                btn1Listener = v -> ui.launchBattle();
            } else {
                String bonusDisplay = stepResult.bonusItemName;
                btn1Text = "你找到了不知谁遗弃的珠宝，你发财了！（金币+" + stepResult.goldFound
                        + "，获得" + bonusDisplay + "）";
                btn1Color = 0xFF4CAF50;
                final String finalText = btn1Text;
                btn1Listener = v -> {
                    ui.showResult(finalText);
                    ui.switchToForwardButton();
                };
            }
        }

        ui.addButton(btn1Text, btn1Color, btn1Listener);
        if (caveStep < 3) {
            ui.addButton("你感到害怕，选择离开", 0xFF888888, v -> {
                StringBuilder sb = new StringBuilder("你感到害怕，转身离开了洞穴。\n\n");
                if (caveStep >= 1) {
                    sb.append("本次探险获得：\n");
                    sb.append("· ").append(caveItemDesc[0]).append("\n");
                }
                if (caveStep >= 2) {
                    sb.append("· ").append(caveItemDesc[1]).append("\n");
                }
                if (caveStep >= 1) {
                    int totalHp = caveHpLoss[0];
                    if (caveStep >= 2) totalHp += caveHpLoss[1];
                    if (caveStep >= 3) totalHp += caveHpLoss[2];
                    sb.append("\n共损失生命：" + totalHp + "点");
                } else {
                    sb.append("你什么都没得到。");
                }
                ui.showResult(sb.toString());
                ui.switchToForwardButton();
            });
        }
    }
}
