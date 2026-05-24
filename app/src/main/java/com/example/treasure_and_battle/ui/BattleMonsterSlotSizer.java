package com.example.treasure_and_battle.ui;

import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * 战斗界面五怪十字站位：按怪物区可用宽高自适应单槽尺寸（与背包格类似 clamp）。
 */
public final class BattleMonsterSlotSizer {

    /** 阵型为 2 列 × 3 行（中间行仅中间位有怪）。 */
    public static final int FORMATION_COLUMNS = 2;
    public static final int FORMATION_ROWS = 3;

    public static final int SLOT_H_MARGIN_DP = 4;
    public static final int SLOT_V_MARGIN_DP = 2;
    public static final int ROW_GAP_DP = 4;

    public static final int SLOT_MAX_WIDTH_DP = 128;
    public static final int SLOT_MIN_WIDTH_DP = 80;
    public static final float SLOT_ASPECT_HEIGHT_OVER_WIDTH = 144f / 148f;
    /** 立绘区约占槽位总高度比例（对齐原 108/144）。 */
    public static final float SPRITE_HEIGHT_RATIO = 108f / 144f;

    private BattleMonsterSlotSizer() {}

    public static int dpToPx(DisplayMetrics dm, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
    }

    public static final class SlotSize {
        public final int widthPx;
        public final int heightPx;
        public final int spriteHeightPx;

        SlotSize(int widthPx, int heightPx, int spriteHeightPx) {
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            this.spriteHeightPx = spriteHeightPx;
        }
    }

    /** @return null 表示区域尚未量好 */
    public static SlotSize resolve(DisplayMetrics dm, int availableWidthPx, int availableHeightPx) {
        if (availableWidthPx <= 0 || availableHeightPx <= 0) {
            return null;
        }
        int hMargin = dpToPx(dm, SLOT_H_MARGIN_DP);
        int vMargin = dpToPx(dm, SLOT_V_MARGIN_DP);
        int rowGap = dpToPx(dm, ROW_GAP_DP);

        int horizontalMargins = hMargin * 2 * FORMATION_COLUMNS;
        int byWidth = (availableWidthPx - horizontalMargins) / FORMATION_COLUMNS;

        int verticalMargins = vMargin * 2 * FORMATION_ROWS;
        int rowGapsTotal = rowGap * (FORMATION_ROWS - 1);
        int byHeight = (availableHeightPx - verticalMargins - rowGapsTotal) / FORMATION_ROWS;
        int byHeightAsWidth = (int) (byHeight / SLOT_ASPECT_HEIGHT_OVER_WIDTH);

        int resolved = Math.min(byWidth, byHeightAsWidth);
        int maxW = dpToPx(dm, SLOT_MAX_WIDTH_DP);
        int minW = dpToPx(dm, SLOT_MIN_WIDTH_DP);
        resolved = Math.min(maxW, Math.max(minW, resolved));

        int heightPx = Math.max(1, (int) (resolved * SLOT_ASPECT_HEIGHT_OVER_WIDTH));
        return new SlotSize(resolved, heightPx, spriteHeightPx(heightPx));
    }

    public static int spriteHeightPx(int slotHeightPx) {
        return Math.max(1, (int) (slotHeightPx * SPRITE_HEIGHT_RATIO));
    }
}
