package com.example.treasure_and_battle.ui;

import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * 背包 5×5 网格格子尺寸：与 {@link BagFragment}、{@link TradeBagBottomController} 共用。
 */
public final class BagGridCellSizer {

    public static final int GRID_COLUMNS = 5;
    public static final int GRID_ROWS = 5;
    public static final int CELL_SPACING_DP = 2;
    public static final int CELL_MAX_DP = 68;
    public static final int CELL_MIN_DP = 42;

    private BagGridCellSizer() {}

    public static int dpToPx(DisplayMetrics dm, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
    }

    /** @return 0 表示可用区域尚未量好 */
    public static int resolveCellSizePx(DisplayMetrics dm, int availableWidthPx, int availableHeightPx) {
        if (availableWidthPx <= 0 || availableHeightPx <= 0) {
            return 0;
        }
        int spacing = dpToPx(dm, CELL_SPACING_DP);
        int horizontalSpace = spacing * 2 * GRID_COLUMNS;
        int verticalSpace = spacing * 2 * GRID_ROWS;
        int cellByWidth = (availableWidthPx - horizontalSpace) / GRID_COLUMNS;
        int cellByHeight = (availableHeightPx - verticalSpace) / GRID_ROWS;
        int maxCell = dpToPx(dm, CELL_MAX_DP);
        int minCell = dpToPx(dm, CELL_MIN_DP);
        int resolved = Math.min(cellByWidth, cellByHeight);
        resolved = Math.min(maxCell, Math.max(minCell, resolved));
        return resolved > 0 ? resolved : 0;
    }

    public static int horizontalPaddingPx(DisplayMetrics dm, int recyclerWidthPx, int cellSizePx) {
        int spacing = dpToPx(dm, CELL_SPACING_DP);
        int totalCellWidth = (cellSizePx + spacing * 2) * GRID_COLUMNS;
        return Math.max((recyclerWidthPx - totalCellWidth) / 2, 0);
    }
}
