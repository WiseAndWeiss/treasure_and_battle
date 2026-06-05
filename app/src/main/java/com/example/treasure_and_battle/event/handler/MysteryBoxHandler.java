package com.example.treasure_and_battle.event.handler;

import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.event.NeutralEventResolver;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipItem;

/**
 * 神秘盲盒 — 消耗200金币随机获得装备/药水/宝石。
 */
public class MysteryBoxHandler extends BaseEventHandler {

    public MysteryBoxHandler(EventUICallback ui) {
        super(ui);
    }

    @Override
    public void build() {
        ui.addButton("购买盲盒（200金币）", 0xFFFF9800, v -> {
            Character ch = ui.getCharacter();
            if (!ch.spendGold(200)) {
                ui.showResult("你的金币不足200，无法购买盲盒。");
                ui.switchToForwardButton();
                return;
            }
            NeutralEventResolver.MysteryBoxResult r =
                    NeutralEventResolver.resolveMysteryBox(ch, ui.getContext());

            StringBuilder sb = new StringBuilder("打开盲盒！\n\n");
            if (r.item == null) {
                sb.append("盲盒是空的...你被骗了！200金币打水漂。");
            } else {
                String typeLabel;
                if (r.item instanceof EquipItem) typeLabel = "装备";
                else if (r.item instanceof ConsumableItem) typeLabel = "药水";
                else typeLabel = "宝石";
                sb.append("获得").append(typeLabel).append("：").append(r.item.getName())
                        .append("\n品质：").append(r.item.getRarity().getDisplayName());
                if (!r.added) {
                    sb.append("\n⚠ 但背包已满！");
                }
            }
            sb.append("\n（当前金币：" + r.currentGold + "）");
            ui.showResult(sb.toString());
            ui.switchToForwardButton();
        });
        ui.addButton("不相信盲盒", 0xFF888888, v -> {
            ui.showResult("你坚信便宜没好货，头也不回地走了。");
            ui.switchToForwardButton();
        });
    }
}
