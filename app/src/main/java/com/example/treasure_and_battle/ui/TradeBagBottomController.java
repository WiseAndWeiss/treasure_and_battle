package com.example.treasure_and_battle.ui;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.drawable.TreasureStyleDrawable;
import com.example.treasure_and_battle.utils.GameAssetIcons;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易页背包下半部分：自 {@link BagFragment} 复制的逻辑与布局（{@link R.layout#include_bag_bottom_half}），
 * 独立物品列表，无装备栏、无拖入装备。
 */
public final class TradeBagBottomController {

    private final TradeFragment host;

    private TextView tvPageInfo;
    private TextView tvFilterInfo;
    private TextView btnPrevPage;
    private TextView btnNextPage;
    private TextView btnCompactBag;
    private TextView btnFilterSlot;
    private LinearLayout gridBagContainer;

    private RecyclerView recyclerView;
    private BagAdapter adapter;
    private GridLayoutManager gridLayoutManager;

    private int currentPage = 1;
    private final int totalPages = 5;
    private final int itemsPerPage = 25;
    @Nullable
    private EquipSlot currentFilterSlot = null;
    private static final int BAG_GRID_COLUMNS = 5;
    private static final int BAG_GRID_ROWS = 5;
    private static final int BAG_CELL_MAX_DP = 68;
    private static final int BAG_CELL_MIN_DP = 42;
    private static final int BAG_CELL_SPACING_DP = 2;

    private List<Item> allItems;

    // 拖动翻页控制
    private Handler edgeScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable edgeScrollRunnable;
    private boolean isNavigatingPage = false;

    // 跨页虚位填补视图
    private View holeView = null;
    private BagAdapter.ViewHolder holeHolder = null;

    // 拖拉状态记录
    private RecyclerView.ViewHolder currentDragHolder = null;
    private int dragStartPage = -1;
    private int dragStartUiPosition = -1;
    private int dragToUiPosition = -1;
    private int dragStartCellLeft = 0;
    private int dragStartCellTop = 0;
    private int dragStartCellWidth = 0;
    private int dragStartCellHeight = 0;

    private int bagCellSizePx;
    private int lastDragCenterX = -1;
    private int lastDragCenterY = -1;

    public TradeBagBottomController(TradeFragment host, View sectionRoot) {
        this.host = host;
        if (sectionRoot instanceof ViewGroup) {
            ((ViewGroup) sectionRoot).setClipChildren(false);
            ((ViewGroup) sectionRoot).setClipToPadding(false);
        }

        initDummyData();

        tvPageInfo = sectionRoot.findViewById(R.id.tv_page_info);
        tvFilterInfo = sectionRoot.findViewById(R.id.tv_filter_info);
        btnPrevPage = sectionRoot.findViewById(R.id.btn_prev_page);
        btnNextPage = sectionRoot.findViewById(R.id.btn_next_page);
        btnCompactBag = sectionRoot.findViewById(R.id.btn_compact_bag);
        btnFilterSlot = sectionRoot.findViewById(R.id.btn_filter_slot);
        gridBagContainer = sectionRoot.findViewById(R.id.grid_bag_container);
        gridBagContainer.setClipChildren(false);
        gridBagContainer.setClipToPadding(false);
        bagCellSizePx = dpToPx(68);

        setupRecyclerView();
        setupPagination();
        setupDragAndDrop();
    }

    private void initDummyData() {
        allItems = InventoryGridSync.getSharedBagGrid(host.requireContext());
    }

    /** 从 {@link InventoryManager} 刷新网格显示（与背包页共用数据时调用） */
    public void reloadFromInventory() {
        InventoryGridSync.reloadSharedGridFromManager(host.requireContext());
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /** 商人购入放入背包 */
    public boolean tryPutInFirstEmptySlot(@NonNull Item item) {
        int cap = Math.min(InventoryGridSync.BAG_SLOT_COUNT, allItems.size());
        for (int i = 0; i < cap; i++) {
            if (allItems.get(i) == null) {
                allItems.set(i, item);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                return true;
            }
        }
        return false;
    }

    /** 与 {@link #tryPutInFirstEmptySlot} 使用相同的索引范围，用于估算可放入数量 */
    public int countEmptySlots() {
        int cap = Math.min(InventoryGridSync.BAG_SLOT_COUNT, allItems.size());
        int n = 0;
        for (int i = 0; i < cap; i++) {
            if (allItems.get(i) == null) {
                n++;
            }
        }
        return n;
    }

    public void notifyDataChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void setupRecyclerView() {
        recyclerView = new RecyclerView(host.requireContext());
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setClipChildren(false);
        recyclerView.setClipToPadding(false);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setNestedScrollingEnabled(false);
        gridLayoutManager = new GridLayoutManager(host.requireContext(), BAG_GRID_COLUMNS) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }

            @Override
            public boolean canScrollHorizontally() {
                return false;
            }
        };
        recyclerView.setLayoutManager(gridLayoutManager);

        adapter = new BagAdapter();
        adapter.setCellSizePx(bagCellSizePx);
        recyclerView.setAdapter(adapter);
        applyAdaptiveBagCellSize();
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                applyAdaptiveBagCellSize();
            }
        });
        gridBagContainer.addView(recyclerView);
    }

    /** 当前手指下的背包 UI 格是否应显示「缩小高亮」 */
    private boolean shouldShowBagCellHoverScale(@NonNull RecyclerView.ViewHolder holder, int adapterPosition) {
        if (dragToUiPosition < 0 || adapterPosition != dragToUiPosition) {
            return false;
        }
        if (currentDragHolder == null) {
            return false;
        }
        boolean isSamePageStartTarget =
                currentPage == dragStartPage && adapterPosition == dragStartUiPosition;
        if (isSamePageStartTarget) {
            return false;
        }
        boolean isCrossPageStartTarget =
                currentPage != dragStartPage && adapterPosition == dragStartUiPosition;
        return holder != currentDragHolder || isCrossPageStartTarget;
    }

    private void applyAdaptiveBagCellSize() {
        recyclerView.post(() -> {
            int width = recyclerView.getWidth();
            int height = recyclerView.getHeight();
            if (width <= 0 || height <= 0) return;

            int spacing = dpToPx(BAG_CELL_SPACING_DP);
            int horizontalSpace = spacing * 2 * BAG_GRID_COLUMNS;
            int verticalSpace = spacing * 2 * BAG_GRID_ROWS;

            int cellByWidth = (width - horizontalSpace) / BAG_GRID_COLUMNS;
            int cellByHeight = (height - verticalSpace) / BAG_GRID_ROWS;
            int maxCell = dpToPx(BAG_CELL_MAX_DP);
            int minCell = dpToPx(BAG_CELL_MIN_DP);
            int resolvedCell = Math.min(cellByWidth, cellByHeight);
            resolvedCell = Math.min(maxCell, Math.max(minCell, resolvedCell));
            if (resolvedCell <= 0) return;

            if (resolvedCell != bagCellSizePx) {
                bagCellSizePx = resolvedCell;
                adapter.setCellSizePx(bagCellSizePx);
                adapter.notifyDataSetChanged();
            }

            int totalCellWidth = (bagCellSizePx + spacing * 2) * BAG_GRID_COLUMNS;
            int horizontalPadding = Math.max((width - totalCellWidth) / 2, 0);
            int topBottomPadding = dpToPx(4);
            recyclerView.setPadding(horizontalPadding, topBottomPadding, horizontalPadding, topBottomPadding);
            recyclerView.setClipToPadding(false);
        });
    }

    private void setupPagination() {
        updatePageUI();
        btnPrevPage.setOnClickListener(v -> goPrevPage());
        btnNextPage.setOnClickListener(v -> goNextPage());
        btnCompactBag.setOnClickListener(v -> {
            compactAllItemsForward();
            adapter.notifyDataSetChanged();
            showFloatMsg("已向前整理背包");
        });
        btnFilterSlot.setOnClickListener(this::showFilterMenu);
    }

    private void goPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            animatePageChange(1);
        }
    }

    private void goNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            animatePageChange(-1);
        }
    }

    private void animatePageChange(int direction) {
        if (recyclerView.getWidth() == 0) {
            updatePageUI();
            return;
        }
        boolean isDragging = (currentDragHolder != null);
        int width = recyclerView.getWidth();

        tvPageInfo.setText("第" + currentPage + "页 / 共" + totalPages + "页");

        recyclerView.animate()
                .translationX(direction * width)
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {
                    if (isDragging) {
                        adapter.notifyItemRangeChanged(0, itemsPerPage, "PAGE_TURN");
                    } else {
                        updatePageUI();
                    }
                    recyclerView.setTranslationX(-direction * width);
                    recyclerView.animate()
                            .translationX(0)
                            .alpha(1f)
                            .setDuration(120)
                            .start();
                }).start();
    }

    private void updatePageUI() {
        tvPageInfo.setText("第" + currentPage + "页 / 共" + totalPages + "页");
        updateFilterButtonText();
        adapter.notifyDataSetChanged();
    }

    private void showFilterMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(host.requireContext(), anchor);
        popupMenu.getMenu().add(0, 100, 0, "显示全部");
        popupMenu.getMenu().add(0, 101, 1, "武器");
        popupMenu.getMenu().add(0, 102, 2, "头盔");
        popupMenu.getMenu().add(0, 103, 3, "胸甲");
        popupMenu.getMenu().add(0, 104, 4, "护腿");
        popupMenu.getMenu().add(0, 105, 5, "鞋子");
        popupMenu.getMenu().add(0, 106, 6, "项链");
        popupMenu.getMenu().add(0, 107, 7, "手镯");
        popupMenu.getMenu().add(0, 108, 8, "戒指");
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case 100:
                    applyFilter(null);
                    return true;
                case 101:
                    applyFilter(EquipSlot.WEAPON);
                    return true;
                case 102:
                    applyFilter(EquipSlot.HELMET);
                    return true;
                case 103:
                    applyFilter(EquipSlot.CHEST);
                    return true;
                case 104:
                    applyFilter(EquipSlot.LEGGINGS);
                    return true;
                case 105:
                    applyFilter(EquipSlot.BOOTS);
                    return true;
                case 106:
                    applyFilter(EquipSlot.NECKLACE);
                    return true;
                case 107:
                    applyFilter(EquipSlot.BRACELET);
                    return true;
                case 108:
                    applyFilter(EquipSlot.RING);
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
    }

    @Nullable
    private EquipSlot resolveFilterSlotByViewId(int slotViewId) {
        if (slotViewId == R.id.slot_weapon) return EquipSlot.WEAPON;
        if (slotViewId == R.id.slot_helmet) return EquipSlot.HELMET;
        if (slotViewId == R.id.slot_chest) return EquipSlot.CHEST;
        if (slotViewId == R.id.slot_leggings) return EquipSlot.LEGGINGS;
        if (slotViewId == R.id.slot_boots) return EquipSlot.BOOTS;
        if (slotViewId == R.id.slot_necklace) return EquipSlot.NECKLACE;
        if (slotViewId == R.id.slot_bracelet) return EquipSlot.BRACELET;
        if (slotViewId == R.id.slot_ring_left || slotViewId == R.id.slot_ring_right) return EquipSlot.RING;
        return null;
    }

    private void applyFilter(@Nullable EquipSlot slot) {
        currentFilterSlot = slot;
        if (slot != null) {
            compactItemsByFilter(slot);
            currentPage = 1;
        }
        updateFilterButtonText();
        adapter.notifyDataSetChanged();
        if (slot == null) {
            showFloatMsg("已取消筛选");
        } else {
            showFloatMsg("已筛选: " + getSlotLabel(slot));
        }
    }

    private void compactItemsByFilter(@NonNull EquipSlot slot) {
        int bagCapacity = InventoryGridSync.BAG_SLOT_COUNT;
        List<Item> matches = new ArrayList<>();
        List<Item> others = new ArrayList<>();

        for (int i = 0; i < bagCapacity; i++) {
            Item item = allItems.get(i);
            if (item instanceof EquipItem && ((EquipItem) item).getSlot() == slot) {
                matches.add(item);
            } else {
                others.add(item);
            }
        }

        int writeIndex = 0;
        for (Item item : matches) {
            allItems.set(writeIndex++, item);
        }
        for (Item item : others) {
            allItems.set(writeIndex++, item);
        }
    }

    private void compactAllItemsForward() {
        int bagCapacity = InventoryGridSync.BAG_SLOT_COUNT;
        List<Item> nonEmptyItems = new ArrayList<>();
        int emptyCount = 0;

        for (int i = 0; i < bagCapacity; i++) {
            Item item = allItems.get(i);
            if (item == null) {
                emptyCount++;
            } else {
                nonEmptyItems.add(item);
            }
        }

        int writeIndex = 0;
        for (Item item : nonEmptyItems) {
            allItems.set(writeIndex++, item);
        }
        for (int i = 0; i < emptyCount; i++) {
            allItems.set(writeIndex++, null);
        }
    }

    private void updateFilterButtonText() {
        if (btnFilterSlot == null) return;
        if (currentFilterSlot == null) {
            btnFilterSlot.setText("筛选");
            btnFilterSlot.setBackgroundResource(R.drawable.bg_tab_idle);
            if (tvFilterInfo != null) {
                tvFilterInfo.setVisibility(View.GONE);
            }
        } else {
            btnFilterSlot.setText(getSlotLabel(currentFilterSlot));
            btnFilterSlot.setBackgroundResource(R.drawable.bg_tab_active);
            if (tvFilterInfo != null) {
                tvFilterInfo.setText("当前筛选：" + getSlotLabel(currentFilterSlot));
                tvFilterInfo.setVisibility(View.VISIBLE);
            }
        }
    }

    private String getSlotLabel(@NonNull EquipSlot slot) {
        if (slot == EquipSlot.WEAPON) return "武器";
        if (slot == EquipSlot.HELMET) return "头盔";
        if (slot == EquipSlot.CHEST) return "胸甲";
        if (slot == EquipSlot.LEGGINGS) return "护腿";
        if (slot == EquipSlot.BOOTS) return "鞋子";
        if (slot == EquipSlot.NECKLACE) return "项链";
        if (slot == EquipSlot.BRACELET) return "手镯";
        if (slot == EquipSlot.RING) return "戒指";
        return "筛选";
    }

    private boolean canDisplayByCurrentFilter(@Nullable Item item) {
        if (currentFilterSlot == null) return true;
        if (!(item instanceof EquipItem)) return false;
        EquipSlot itemSlot = ((EquipItem) item).getSlot();
        return itemSlot == currentFilterSlot;
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int uiPos = viewHolder.getAdapterPosition();
                if (uiPos == RecyclerView.NO_POSITION) return makeMovementFlags(0, 0);
                int realPos = (currentPage - 1) * itemsPerPage + uiPos;
                if (realPos < 0 || realPos >= allItems.size()
                        || allItems.get(realPos) == null
                        || !canDisplayByCurrentFilter(allItems.get(realPos))) {
                    return makeMovementFlags(0, 0); // 空格子禁止拖拽
                }
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(dragFlags, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int toPos = target.getAdapterPosition();
                if (toPos == dragToUiPosition) return false;

                int oldHover = dragToUiPosition;
                dragToUiPosition = toPos;

                // 使用 Payload 局部刷新以避免卡顿
                if (oldHover != -1) {
                    adapter.notifyItemChanged(oldHover, "HOVER_CLEAR");
                }
                adapter.notifyItemChanged(dragToUiPosition, "HOVER");

                return false; // 返回 false，阻止系统默认的数据挤占移位
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public boolean isLongPressDragEnabled() { return true; }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    currentDragHolder = viewHolder;
                    dragStartPage = currentPage;
                    dragStartUiPosition = viewHolder.getAdapterPosition();
                    dragToUiPosition = dragStartUiPosition;
                    dragStartCellLeft = viewHolder.itemView.getLeft();
                    dragStartCellTop = viewHolder.itemView.getTop();
                    dragStartCellWidth = viewHolder.itemView.getWidth();
                    dragStartCellHeight = viewHolder.itemView.getHeight();
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    stopEdgeScroll();
                    isNavigatingPage = false;
                    int releasePos = findBagAdapterPositionByGlobalPoint(lastDragCenterX, lastDragCenterY);
                    // 仅在命中有效格子时覆盖目标位置，避免误把可交换目标回退到起点
                    if (releasePos >= 0) {
                        dragToUiPosition = releasePos;
                    } else if (dragToUiPosition < 0) {
                        dragToUiPosition = dragStartUiPosition;
                    }

                    boolean handledEquipDrop = false;
                    if (dragStartPage != -1 && dragStartUiPosition != -1 && dragToUiPosition != -1) {
                        int realFrom = (dragStartPage - 1) * itemsPerPage + dragStartUiPosition;
                        int realTo = (currentPage - 1) * itemsPerPage + dragToUiPosition;

                        if (!handledEquipDrop && realFrom != realTo) {
                            // 纯粹地交换原目标格和新目标格的数据
                            Item temp = allItems.get(realFrom);
                            allItems.set(realFrom, allItems.get(realTo));
                            allItems.set(realTo, temp);
                        }
                    }

                    currentDragHolder = null;
                    dragStartPage = -1;
                    dragStartUiPosition = -1;
                    dragToUiPosition = -1;
                    dragStartCellLeft = 0;
                    dragStartCellTop = 0;
                    dragStartCellWidth = 0;
                    dragStartCellHeight = 0;
                    lastDragCenterX = -1;
                    lastDragCenterY = -1;

                    adapter.notifyDataSetChanged(); // 刷新全局状态，消除残影与Hover
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                    // 拖拽物本体始终保持原尺寸，不参与“目标格子缩小”动画
                    viewHolder.itemView.setScaleX(1.0f);
                    viewHolder.itemView.setScaleY(1.0f);

                    int screenWidth = recyclerView.getWidth();
                    int screenHeight = recyclerView.getHeight();
                    float viewX = viewHolder.itemView.getLeft() + dX;
                    float viewY = viewHolder.itemView.getTop() + dY;
                    int[] recyclerLocation = new int[2];
                    recyclerView.getLocationOnScreen(recyclerLocation);
                    lastDragCenterX = (int) (recyclerLocation[0] + viewX + viewHolder.itemView.getWidth() / 2f);
                    lastDragCenterY = (int) (recyclerLocation[1] + viewHolder.itemView.getTop() + dY + viewHolder.itemView.getHeight() / 2f);

                    // 边缘触发翻页检测
                    float centerYInRecycler = viewY + viewHolder.itemView.getHeight() / 2f;
                    boolean withinBagHeight = centerYInRecycler >= 0 && centerYInRecycler <= screenHeight;
                    if (withinBagHeight && viewX < screenWidth * 0.1f && dX < 0) {
                        checkEdgeScroll(-1);
                    } else if (withinBagHeight && viewX + viewHolder.itemView.getWidth() > screenWidth * 0.9f && dX > 0) {
                        checkEdgeScroll(1);
                    } else {
                        stopEdgeScroll();
                    }

                    // 使用拖拽中心实时命中目标格，确保四个方向都能正确交换
                    float centerXInRecycler = viewX + viewHolder.itemView.getWidth() / 2f;
                    int hoverPos = findBagAdapterPositionByLocalPoint(centerXInRecycler, centerYInRecycler);
                    if (hoverPos >= 0 && hoverPos != dragToUiPosition) {
                        int oldHover = dragToUiPosition;
                        dragToUiPosition = hoverPos;
                        if (oldHover >= 0) {
                            adapter.notifyItemChanged(oldHover, "HOVER_CLEAR");
                        }
                        adapter.notifyItemChanged(dragToUiPosition, "HOVER");
                    }

                    // 仅当目前处于原始页面时，才绘制原始格子的虚影
                    if (currentPage == dragStartPage) {
                        c.save();
                        // 在起始坑位绘制一个“背景格子”虚影；当目标还是起始位时，缩小坑位而不是缩小拖拽物
                        float bgScale = (dragToUiPosition == dragStartUiPosition) ? 0.85f : 1.0f;
                        float pivotX = dragStartCellLeft + dragStartCellWidth / 2f;
                        float pivotY = dragStartCellTop + dragStartCellHeight / 2f;
                        c.translate(pivotX, pivotY);
                        c.scale(bgScale, bgScale);
                        c.translate(-dragStartCellWidth / 2f, -dragStartCellHeight / 2f);
                        // 强制截取绘制层，令背景格子统一附上30% (76/255) 的透明度
                        c.saveLayerAlpha(0, 0, dragStartCellWidth, dragStartCellHeight, 76);
                        viewHolder.itemView.draw(c);
                        c.restore();
                        c.restore();
                    }

                    // 跨页后，填补本来被拖拽物作为 draggedView 占掉坑位而引起的空白
                    if (currentPage != dragStartPage) {
                        if (holeView == null && viewHolder.itemView.getWidth() > 0) {
                            holeView = LayoutInflater.from(host.requireContext()).inflate(R.layout.item_bag_grid, recyclerView, false);
                            holeView.setLayoutParams(new RecyclerView.LayoutParams(viewHolder.itemView.getWidth(), viewHolder.itemView.getHeight()));
                            holeView.measure(
                                    View.MeasureSpec.makeMeasureSpec(viewHolder.itemView.getWidth(), View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(viewHolder.itemView.getHeight(), View.MeasureSpec.EXACTLY)
                            );
                            holeView.layout(0, 0, viewHolder.itemView.getWidth(), viewHolder.itemView.getHeight());
                            holeHolder = adapter.new ViewHolder(holeView);
                        }

                        if (holeView != null) {
                            int realPosition = (currentPage - 1) * itemsPerPage + dragStartUiPosition;
                            Item holeItem = allItems.get(realPosition);

                            if (holeItem == null) {
                                holeHolder.tvItemName.setText("");
                                holeHolder.tvItemLevel.setVisibility(View.GONE);
                                bindBagStackCountBadge(holeHolder.tvBagStackCount, null);
                                holeHolder.ivItemIcon.setVisibility(View.INVISIBLE);
                                holeHolder.bgItemColor.setBackgroundResource(R.drawable.bg_slot_treasure_fill);
                                holeHolder.bgItemColor.setBackgroundTintList(null);
                                bindBagGridCellFrame(holeView, null);
                            } else {
                                holeHolder.tvItemName.setText(holeItem.getName());
                                holeHolder.ivItemIcon.setVisibility(View.VISIBLE);
                                bindBagItemIcon(holeHolder.ivItemIcon, holeItem);
                                holeHolder.bgItemColor.setBackgroundResource(R.drawable.bg_slot_treasure_fill);
                                holeHolder.bgItemColor.setBackgroundTintList(null);
                                bindBagStackCountBadge(holeHolder.tvBagStackCount, holeItem);
                                bindBagGridCellFrame(holeView, holeItem);

                                if (holeItem instanceof EquipItem) {
                                    holeHolder.tvItemLevel.setVisibility(View.VISIBLE);
                                    holeHolder.tvItemLevel.setText("Lv." + ((EquipItem) holeItem).getLevel());
                                } else {
                                    holeHolder.tvItemLevel.setVisibility(View.GONE);
                                }
                            }

                            holeView.measure(
                                    View.MeasureSpec.makeMeasureSpec(viewHolder.itemView.getWidth(), View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(viewHolder.itemView.getHeight(), View.MeasureSpec.EXACTLY)
                            );
                            holeView.layout(0, 0, viewHolder.itemView.getWidth(), viewHolder.itemView.getHeight());

                            c.save();
                            c.translate(viewHolder.itemView.getLeft(), viewHolder.itemView.getTop());
                            holeView.draw(c);
                            c.restore();
                        }
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            private void checkEdgeScroll(int direction) {
                if (isNavigatingPage || currentDragHolder == null) return;
                if (edgeScrollRunnable == null) {
                    edgeScrollRunnable = () -> {
                        if (direction == -1 && currentPage > 1) {
                            isNavigatingPage = true;
                            goPrevPage();
                            edgeScrollHandler.postDelayed(() -> isNavigatingPage = false, 1200);
                        } else if (direction == 1 && currentPage < totalPages) {
                            isNavigatingPage = true;
                            goNextPage();
                            edgeScrollHandler.postDelayed(() -> isNavigatingPage = false, 1200);
                        }
                        edgeScrollRunnable = null;
                    };
                    edgeScrollHandler.postDelayed(edgeScrollRunnable, 800); // 边缘悬停判定时间
                }
            }

            private void stopEdgeScroll() {
                if (edgeScrollRunnable != null) {
                    edgeScrollHandler.removeCallbacks(edgeScrollRunnable);
                    edgeScrollRunnable = null;
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private int findBagAdapterPositionByGlobalPoint(int globalX, int globalY) {
        if (globalX < 0 || globalY < 0 || recyclerView == null) return -1;
        int[] recyclerLocation = new int[2];
        recyclerView.getLocationOnScreen(recyclerLocation);
        float localX = globalX - recyclerLocation[0];
        float localY = globalY - recyclerLocation[1];
        return findBagAdapterPositionByLocalPoint(localX, localY);
    }

    private int findBagAdapterPositionByLocalPoint(float localX, float localY) {
        if (recyclerView == null) return -1;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            if (child == null) continue;
            float left = child.getLeft();
            float top = child.getTop();
            float right = child.getRight();
            float bottom = child.getBottom();
            if (localX >= left && localX <= right && localY >= top && localY <= bottom) {
                return recyclerView.getChildAdapterPosition(child);
            }
        }
        return -1;
    }

    private void bindBagGridCellFrame(@NonNull View cellFrameRoot, @Nullable Item item) {
        Integer borderArgb = null;
        if (item != null && item.getRarity() != null) {
            borderArgb = item.getRarity().getColor();
        }
        cellFrameRoot.setForeground(TreasureStyleDrawable.newSlotStrokeOverlay(host.requireContext(), borderArgb));
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                host.getResources().getDisplayMetrics()
        );
    }

    /** 物品格图标：关闭双线性过滤，像素风图标更清晰。 */
    private void bindBagItemIcon(@Nullable ImageView imageView, @Nullable Item item) {
        if (imageView == null || item == null) {
            return;
        }
        GameAssetIcons.bindItem(host.requireContext(), imageView, item);
    }

    private static void bindBagStackCountBadge(@Nullable TextView tv, @Nullable Item item) {
        if (tv == null) {
            return;
        }
        if (item != null && item.canStack() && item.getCount() > 1) {
            tv.setVisibility(View.VISIBLE);
            tv.setText("×" + item.getCount());
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private class BagAdapter extends RecyclerView.Adapter<BagAdapter.ViewHolder> {
        private int cellSizePx = ViewGroup.LayoutParams.WRAP_CONTENT;

        void setCellSizePx(int cellSizePx) {
            this.cellSizePx = cellSizePx;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bag_grid, parent, false);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    cellSizePx > 0 ? cellSizePx : ViewGroup.LayoutParams.WRAP_CONTENT);
            int spacing = dpToPx(BAG_CELL_SPACING_DP);
            params.setMargins(spacing, spacing, spacing, spacing);
            view.setLayoutParams(params);
            return new ViewHolder(view);
        }

        // 接管 Payload 局部刷新以避免卡顿和闪退
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position);
                return;
            }

            for (Object payload : payloads) {
                if ("HOVER".equals(payload)) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && shouldShowBagCellHoverScale(holder, pos)) {
                        holder.itemView.setScaleX(0.85f);
                        holder.itemView.setScaleY(0.85f);
                    }
                } else if ("HOVER_CLEAR".equals(payload)) {
                    holder.itemView.setScaleX(1.0f);
                    holder.itemView.setScaleY(1.0f);
                } else if ("PAGE_TURN".equals(payload)) {
                    if (holder == currentDragHolder) {
                        // 【核心防闪退点】禁止更新正在被拖拽和追踪的 ViewHolder 视图状态
                        continue;
                    }
                    onBindViewHolder(holder, position);
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int realPosition = (currentPage - 1) * itemsPerPage + position;
            Item sourceItem = allItems.get(realPosition);
            Item item = canDisplayByCurrentFilter(sourceItem) ? sourceItem : null;

            holder.itemView.setScaleX(1.0f);
            holder.itemView.setScaleY(1.0f);

            // 当目标移位时，高亮新坑位（背包内拖 / 装备栏拖入）
            if (shouldShowBagCellHoverScale(holder, position)) {
                holder.itemView.setScaleX(0.85f);
                holder.itemView.setScaleY(0.85f);
            }

            if (item == null) {
                holder.tvItemName.setText("");
                holder.tvItemLevel.setVisibility(View.GONE);
                bindBagStackCountBadge(holder.tvBagStackCount, null);
                holder.ivItemIcon.setVisibility(View.INVISIBLE);
                holder.bgItemColor.setBackgroundResource(R.drawable.bg_slot_treasure_fill);
                holder.bgItemColor.setBackgroundTintList(null);
                bindBagGridCellFrame(holder.itemView, null);
            } else {
                holder.tvItemName.setText(item.getName());
                holder.ivItemIcon.setVisibility(View.VISIBLE);
                bindBagItemIcon(holder.ivItemIcon, item);
                holder.bgItemColor.setBackgroundResource(R.drawable.bg_slot_treasure_fill);
                holder.bgItemColor.setBackgroundTintList(null);
                bindBagStackCountBadge(holder.tvBagStackCount, item);
                bindBagGridCellFrame(holder.itemView, item);

                if (item instanceof EquipItem) {
                    EquipItem eq = (EquipItem) item;
                    holder.tvItemLevel.setVisibility(View.VISIBLE);
                    holder.tvItemLevel.setText("Lv." + eq.getLevel());
                } else {
                    holder.tvItemLevel.setVisibility(View.GONE);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (item != null) {
                    showItemMenu(v, realPosition, item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return itemsPerPage;
        }

        private void showItemMenu(View view, int realPosition, Item item) {
            PopupMenu popupMenu = new PopupMenu(host.requireContext(), view);
            popupMenu.getMenu().add(0, 1, 0, "查看描述");
            popupMenu.getMenu().add(0, 2, 0, "出售");
            popupMenu.getMenu().add(0, 3, 0, "丢弃");

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case 1:
                        ItemDetailDialog.show(host.requireContext(), item);
                        break;
                    case 2:
                        SellItemDialog.show(host.requireActivity(), allItems, realPosition, item,
                                () -> {
                                    InventoryGridSync.flushSharedGridToManager(host.requireContext());
                                    adapter.notifyDataSetChanged();
                                },
                                host::addGoldFromSell);
                        break;
                    case 3:
                        allItems.set(realPosition, null);
                        InventoryGridSync.flushSharedGridToManager(host.requireContext());
                        adapter.notifyDataSetChanged();
                        showFloatMsg("已丢弃" + item.getName());
                        break;
                }
                return true;
            });
            popupMenu.show();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            RelativeLayout bgItemColor;
            ImageView ivItemIcon;
            TextView tvItemLevel;
            TextView tvItemName;
            TextView tvBagStackCount;

            ViewHolder(View itemView) {
                super(itemView);
                bgItemColor = itemView.findViewById(R.id.bg_item_color);
                ivItemIcon = itemView.findViewById(R.id.iv_item_icon);
                tvItemLevel = itemView.findViewById(R.id.tv_item_level);
                tvItemName = itemView.findViewById(R.id.tv_item_name);
                tvBagStackCount = itemView.findViewById(R.id.tv_bag_stack_count);
            }
        }
    }

    private void showFloatMsg(String text) {
        FloatMsgOverlay.showFloatMsg(host.getContext(), text);
    }
}