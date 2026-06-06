package com.example.treasure_and_battle.event.handler;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.event.EventUICallback;
import com.example.treasure_and_battle.event.NeutralEventResolver;

/**
 * 宝箱 — 随机生成铜/银/金宝箱，消耗对应钥匙打开获取奖励。
 */
public class ChestHandler extends BaseEventHandler {

    private NeutralEventResolver.ChestRollResult chestInfo;

    public ChestHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.clearButtons();
        chestInfo = NeutralEventResolver.rollChestType();

        ui.addButton("打开宝箱", 0xFFFF9800, v -> {
            Character ch = ui.getCharacter();
            NeutralEventResolver.ChestOpenResult r = NeutralEventResolver.openChest(ch, ui.getContext(), chestInfo);

            if (!r.hasKey) {
                ui.showResult("营地中有一只" + chestInfo.chestName + "！\n\n你没有" + r.keyName
                        + "，无法打开宝箱。");
                ui.switchToForwardButton();
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("营地中有一只").append(chestInfo.chestName).append("！\n");

            if (r.equipName != null) {
                if (r.equipAdded) {
                    sb.append(" 获得：").append(r.equipName)
                            .append("（").append(r.equipRarityDisplay).append("）\n");
                } else {
                    sb.append(" 获得：").append(r.equipName)
                            .append("（").append(r.equipRarityDisplay).append("）但背包已满！\n");
                }
            }
            sb.append(" 金币 +").append(r.goldGained).append("\n");

            if (r.gemName != null) {
                if (r.gemAdded) {
                    sb.append(" 获得：").append(r.gemName)
                            .append("（").append(r.gemRarityDisplay).append("）");
                } else {
                    sb.append(" 获得：").append(r.gemName)
                            .append("（").append(r.gemRarityDisplay).append("）但背包已满！");
                }
            }

            if (r.bagFull) {
                sb.append("\n\n 背包已满，部分物品无法放入！");
            }

            ui.showResult(sb.toString());
            ui.switchToForwardButton();
        });
        ui.addButton("离开", 0xFF888888, v -> {
            ui.showResult("你放弃了" + chestInfo.chestName + "，继续前行。");
            ui.switchToForwardButton();
        });
    }
}
