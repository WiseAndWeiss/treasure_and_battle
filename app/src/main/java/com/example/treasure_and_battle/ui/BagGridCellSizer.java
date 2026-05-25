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

    /** 装备栏左右列每列 4 个格子，估算间距用于自适应尺寸。 */
    public static final int EQUIP_SLOTS_PER_COLUMN = 4;
    public static final int EQUIP_COLUMN_GAP_DP = 4;

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

    /**
     * 装备栏单格尺寸：由侧列宽高推算，并限制在 {@link #CELL_MIN_DP}~{@link #CELL_MAX_DP}。
     */
    public static int resolveEquipSlotSizePx(DisplayMetrics dm, int columnWidthPx, int columnHeightPx) {
        if (columnWidthPx <= 0 || columnHeightPx <= 0) {
            return 0;
        }
        int gap = dpToPx(dm, EQUIP_COLUMN_GAP_DP);
        int gapsTotal = gap * Math.max(0, EQUIP_SLOTS_PER_COLUMN - 1);
        int byHeight = (columnHeightPx - gapsTotal) / EQUIP_SLOTS_PER_COLUMN;
        int resolved = Math.min(columnWidthPx, byHeight);
        int maxCell = dpToPx(dm, CELL_MAX_DP);
        int minCell = dpToPx(dm, CELL_MIN_DP);
        resolved = Math.min(maxCell, Math.max(minCell, resolved));
        return resolved > 0 ? resolved : 0;
    }

    /** 与背包 5×5 格对齐：取背包格与装备栏可容纳尺寸的较小值。 */
    public static int resolveEquipSlotSizeAlignedToBag(
            DisplayMetrics dm, int columnWidthPx, int columnHeightPx, int bagCellSizePx) {
        int byColumn = resolveEquipSlotSizePx(dm, columnWidthPx, columnHeightPx);
        if (byColumn <= 0) {
            return 0;
        }
        if (bagCellSizePx <= 0) {
            return byColumn;
        }
        int maxCell = dpToPx(dm, CELL_MAX_DP);
        int minCell = dpToPx(dm, CELL_MIN_DP);
        int aligned = Math.min(bagCellSizePx, byColumn);
        aligned = Math.min(maxCell, Math.max(minCell, aligned));
        return aligned > 0 ? aligned : 0;
    }

    public static int horizontalPaddingPx(DisplayMetrics dm, int recyclerWidthPx, int cellSizePx) {
        int spacing = dpToPx(dm, CELL_SPACING_DP);
        int totalCellWidth = (cellSizePx + spacing * 2) * GRID_COLUMNS;
        return Math.max((recyclerWidthPx - totalCellWidth) / 2, 0);
    }

    /** 商人 4×2 网格：图标格尺寸（不含下方价格行）。 */
    public static final int MERCHANT_COLUMNS = 4;
    public static final int MERCHANT_ROWS = 2;
    public static final int MERCHANT_CELL_SPACING_DP = 1;
    public static final int MERCHANT_CELL_PADDING_DP = 2;
    /** 价格行：marginTop 2dp + 约一行文字。 */
    public static final int MERCHANT_PRICE_ROW_DP = 18;
    public static final int MERCHANT_SQUARE_MAX_DP = 88;
    public static final int MERCHANT_SQUARE_MIN_DP = 36;

    /** @return 0 表示可用区域尚未量好 */
    public static int resolveMerchantSquareSizePx(DisplayMetrics dm, int availableWidthPx, int availableHeightPx) {
        if (availableWidthPx <= 0 || availableHeightPx <= 0) {
            return 0;
        }
        int spacing = dpToPx(dm, MERCHANT_CELL_SPACING_DP);
        int cellPad = dpToPx(dm, MERCHANT_CELL_PADDING_DP);
        int priceRow = dpToPx(dm, MERCHANT_PRICE_ROW_DP);

        int horizontalSpace = spacing * 2 * MERCHANT_COLUMNS + cellPad * 2 * MERCHANT_COLUMNS;
        int squareByWidth = (availableWidthPx - horizontalSpace) / MERCHANT_COLUMNS;

        int verticalNonSquarePerRow = priceRow + cellPad * 2;
        int verticalSpace = spacing * 2 * MERCHANT_ROWS + verticalNonSquarePerRow * MERCHANT_ROWS;
        int squareByHeight = (availableHeightPx - verticalSpace) / MERCHANT_ROWS;

        int maxSquare = dpToPx(dm, MERCHANT_SQUARE_MAX_DP);
        int minSquare = dpToPx(dm, MERCHANT_SQUARE_MIN_DP);
        int resolved = Math.min(squareByWidth, squareByHeight);
        resolved = Math.min(maxSquare, Math.max(minSquare, resolved));
        return resolved > 0 ? resolved : 0;
    }

    public static int merchantItemHeightPx(DisplayMetrics dm, int squareSizePx) {
        int cellPad = dpToPx(dm, MERCHANT_CELL_PADDING_DP);
        int priceRow = dpToPx(dm, MERCHANT_PRICE_ROW_DP);
        return squareSizePx + priceRow + cellPad * 2;
    }

    public static int merchantHorizontalPaddingPx(DisplayMetrics dm, int recyclerWidthPx, int squareSizePx) {
        int spacing = dpToPx(dm, MERCHANT_CELL_SPACING_DP);
        int cellPad = dpToPx(dm, MERCHANT_CELL_PADDING_DP);
        int totalCellWidth = (squareSizePx + cellPad * 2 + spacing * 2) * MERCHANT_COLUMNS;
        return Math.max((recyclerWidthPx - totalCellWidth) / 2, 0);
    }

    /** 事件弹窗内 2 列物品选择格（祝福 / 祭坛 / 重炼）。 */
    public static final int DIALOG_GRID_COLUMNS = 2;

    /**
     * 根据 RecyclerView 宽度计算正方形边长。
     * @return 0 表示宽度尚未量好
     */
    public static int resolveDialogGridSquareSizePx(DisplayMetrics dm, int recyclerWidthPx) {
        if (recyclerWidthPx <= 0) {
            return 0;
        }
        int spacing = dpToPx(dm, CELL_SPACING_DP);
        int horizontalSpace = spacing * 2 * DIALOG_GRID_COLUMNS;
        int squareByWidth = (recyclerWidthPx - horizontalSpace) / DIALOG_GRID_COLUMNS;
        int maxSquare = dpToPx(dm, CELL_MAX_DP);
        int minSquare = dpToPx(dm, CELL_MIN_DP);
        int resolved = Math.min(maxSquare, Math.max(minSquare, squareByWidth));
        return resolved > 0 ? resolved : 0;
    }

    public static int dialogGridColumnWidthPx(int recyclerWidthPx) {
        return recyclerWidthPx / DIALOG_GRID_COLUMNS;
    }

    /** 正方形格子在列内水平居中时的左侧 inset。 */
    public static int dialogGridCellHorizontalInsetPx(int recyclerWidthPx, int squarePx) {
        int columnWidth = dialogGridColumnWidthPx(recyclerWidthPx);
        return Math.max(0, (columnWidth - squarePx) / 2);
    }
}
