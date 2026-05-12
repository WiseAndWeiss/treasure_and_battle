package com.example.treasure_and_battle.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.graphics.Canvas;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.DragEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.character.Character;
import com.example.treasure_and_battle.manager.item.ConsumableManager;
import com.example.treasure_and_battle.model.entity.Player;
import com.example.treasure_and_battle.model.item.equip.EquipItem;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.consumable.ConsumableItem;
import com.example.treasure_and_battle.model.item.equip.EquipSlot;
import com.example.treasure_and_battle.model.common.Rarity;
import com.example.treasure_and_battle.ui.menu.ItemAction;
import com.example.treasure_and_battle.ui.menu.ItemMenuProviderFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BagFragment extends Fragment {

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

    // 装备栏拖放
    private final Map<Integer, View> equipSlotViews = new HashMap<>();
    private final Map<Integer, EquipItem> equippedItems = new HashMap<>();
    private final Map<Integer, String> equipSlotDefaultTexts = new HashMap<>();
    private int bagCellSizePx;
    private int lastDragCenterX = -1;
    private int lastDragCenterY = -1;
    private static final String DRAG_LABEL_EQUIP_FROM_SLOT = "equip_from_slot";
    private boolean isDragFromEquipSlot = false;

    /** 装备区 + 下半区卡片都有 elevation，整体 Z 序会盖住上移中的格子；拖动时压低装备区并抬高背包半区 */
    private View bagEquipmentPanel;
    private View bagBottomToolbar;
    private ViewGroup bagBottomHalfRoot;

    private boolean bagDragLayeringActive;
    private float bagDragSavedEquipElev;
    private float bagDragSavedToolbarElev;
    private float bagDragSavedBottomHalfElev;
    private float bagDragSavedGridElev;
    private float bagDragSavedRecyclerElev;

    private ItemMenuProviderFactory menuProviderFactory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bag, container, false);

        if (view instanceof ViewGroup) {
            ((ViewGroup) view).setClipChildren(false);
            ((ViewGroup) view).setClipToPadding(false);
        }

        bagEquipmentPanel = view.findViewById(R.id.panel_bag_equipment);
        bagBottomToolbar = view.findViewById(R.id.panel_bag_bottom_toolbar);

        tvPageInfo = view.findViewById(R.id.tv_page_info);
        tvFilterInfo = view.findViewById(R.id.tv_filter_info);
        btnPrevPage = view.findViewById(R.id.btn_prev_page);
        btnNextPage = view.findViewById(R.id.btn_next_page);
        btnCompactBag = view.findViewById(R.id.btn_compact_bag);
        btnFilterSlot = view.findViewById(R.id.btn_filter_slot);
        gridBagContainer = view.findViewById(R.id.grid_bag_container);
        gridBagContainer.setClipChildren(false);
        gridBagContainer.setClipToPadding(false);
        if (gridBagContainer.getParent() instanceof ViewGroup) {
            bagBottomHalfRoot = (ViewGroup) gridBagContainer.getParent();
            bagBottomHalfRoot.setClipChildren(false);
            bagBottomHalfRoot.setClipToPadding(false);
        }
        bagCellSizePx = dpToPx(68);

        bindEquipSlots(view);

        initDummyData();
        initMenuProviderFactory();
        setupRecyclerView();
        setupPagination();
        setupDragAndDrop();

        return view;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // 底栏用 hide/show 切换 Fragment 时，从隐藏变为显示会走这里，保证每次点进背包都拉最新数据
        if (!hidden) {
            refreshBagFromInventory();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshBagFromInventory();
    }

    @Override
    public void onPause() {
        InventoryGridSync.flushSharedGridToManager(requireContext());
        super.onPause();
    }

    /** 从 InventoryManager 拉取列表到共享网格并刷新显示 */
    private void refreshBagFromInventory() {
        InventoryGridSync.reloadSharedGridFromManager(requireContext());
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        loadEquippedFromCharacter();
    }

    private void loadEquippedFromCharacter() {
        Character ch = PlayerCharacterHolder.getOrCreate(getContext());
        if (ch == null) return;
        equippedItems.clear();
        for (EquipSlot slot : EquipSlot.values()) {
            int slotViewId = equipSlotToViewId(slot);
            if (slotViewId != -1) {
                updateEquipSlotView(slotViewId, null);
            }
        }
        for (EquipItem item : ch.getEquippedItems()) {
            if (item == null) continue;
            int slotViewId = equipSlotToViewId(item.getSlot());
            if (slotViewId == -1) continue;
            if (item.getSlot() == EquipSlot.RING && equippedItems.containsKey(slotViewId)) {
                slotViewId = R.id.slot_ring_right;
            }
            equippedItems.put(slotViewId, item);
            updateEquipSlotView(slotViewId, item);
        }
    }

    private int equipSlotToViewId(EquipSlot slot) {
        if (slot == null) return -1;
        switch (slot) {
            case WEAPON:   return R.id.slot_weapon;
            case HELMET:   return R.id.slot_helmet;
            case CHEST:    return R.id.slot_chest;
            case LEGGINGS: return R.id.slot_leggings;
            case BOOTS:    return R.id.slot_boots;
            case NECKLACE: return R.id.slot_necklace;
            case BRACELET: return R.id.slot_bracelet;
            case RING:     return R.id.slot_ring_left;
            default:       return -1;
        }
    }

    /** 将当前网格顺序写回 InventoryManager（交换格子、整理、装备移动等之后调用） */
    private void persistSharedBagGridToInventory() {
        InventoryGridSync.flushSharedGridToManager(requireContext());
    }

    private void applyBagDragLayering(boolean dragging) {
        if (!isAdded()) {
            return;
        }
        if (dragging) {
            if (!bagDragLayeringActive) {
                bagDragLayeringActive = true;
                bagDragSavedEquipElev = bagEquipmentPanel != null ? bagEquipmentPanel.getElevation() : 0f;
                bagDragSavedToolbarElev = bagBottomToolbar != null ? bagBottomToolbar.getElevation() : 0f;
                bagDragSavedBottomHalfElev = bagBottomHalfRoot != null ? bagBottomHalfRoot.getElevation() : 0f;
                bagDragSavedGridElev = gridBagContainer != null ? gridBagContainer.getElevation() : 0f;
                bagDragSavedRecyclerElev = recyclerView != null ? recyclerView.getElevation() : 0f;
            }
            float liftHalf = dpToPx(12);
            float liftGrid = liftHalf + dpToPx(2);
            float liftRv = liftHalf + dpToPx(4);
            if (bagEquipmentPanel != null) {
                bagEquipmentPanel.setElevation(0f);
            }
            if (bagBottomToolbar != null) {
                bagBottomToolbar.setElevation(0f);
            }
            if (bagBottomHalfRoot != null) {
                bagBottomHalfRoot.bringToFront();
                ViewCompat.setElevation(bagBottomHalfRoot, liftHalf);
            }
            if (gridBagContainer != null) {
                ViewCompat.setElevation(gridBagContainer, liftGrid);
            }
            if (recyclerView != null) {
                ViewCompat.setElevation(recyclerView, liftRv);
            }
            View root = getView();
            if (root != null) {
                root.invalidate();
            }
        } else {
            if (!bagDragLayeringActive) {
                return;
            }
            bagDragLayeringActive = false;
            if (bagEquipmentPanel != null) {
                bagEquipmentPanel.setElevation(bagDragSavedEquipElev);
            }
            if (bagBottomToolbar != null) {
                bagBottomToolbar.setElevation(bagDragSavedToolbarElev);
            }
            if (bagBottomHalfRoot != null) {
                ViewCompat.setElevation(bagBottomHalfRoot, bagDragSavedBottomHalfElev);
            }
            if (gridBagContainer != null) {
                ViewCompat.setElevation(gridBagContainer, bagDragSavedGridElev);
            }
            if (recyclerView != null) {
                ViewCompat.setElevation(recyclerView, bagDragSavedRecyclerElev);
            }
            View root = getView();
            if (root != null) {
                root.invalidate();
            }
        }
    }

    private void initDummyData() {
        allItems = InventoryGridSync.getSharedBagGrid(requireContext());
    }

    private int findItemIndex(Item item) {
        if (item == null) return -1;
        for (int i = 0; i < allItems.size(); i++) {
            Item existing = allItems.get(i);
            if (existing != null && existing.getId().equals(item.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void initMenuProviderFactory() {
        menuProviderFactory = new ItemMenuProviderFactory(
                requireContext(),
                item -> ItemDetailDialog.show(requireContext(), item),
                item -> new AlertDialog.Builder(requireContext())
                        .setTitle("确认丢弃")
                        .setMessage("确定要丢弃 " + item.getName() + " 吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            int index = findItemIndex(item);
                            if (index >= 0) {
                                allItems.set(index, null);
                            }
                            adapter.notifyDataSetChanged();
                            persistSharedBagGridToInventory();
                            Toast.makeText(getContext(), "已丢弃: " + item.getName(), Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show()
        );
        menuProviderFactory.registerEquipment(equipItem -> {
            int bagIndex = findItemIndex(equipItem);
            if (bagIndex >= 0) {
                boolean equipped = autoEquipFromBag(bagIndex, equipItem);
                if (equipped) {
                    adapter.notifyDataSetChanged();
                    persistSharedBagGridToInventory();
                }
            }
        });
        menuProviderFactory.registerConsumable(consumableItem -> {
            Player player = PlayerCharacterHolder.getOrCreate(getContext()).generatePlayer();
            boolean success = ConsumableManager.execute(player, null, consumableItem, getContext());
            if (success) {
                if (consumableItem.getCount() > 1) {
                    consumableItem.setCount(consumableItem.getCount() - 1);
                } else {
                    int index = findItemIndex(consumableItem);
                    if (index >= 0) {
                        allItems.set(index, null);
                    }
                }
                adapter.notifyDataSetChanged();
                persistSharedBagGridToInventory();
                Toast.makeText(getContext(), "已使用: " + consumableItem.getName(), Toast.LENGTH_SHORT).show();
            }
        });
        menuProviderFactory.registerGem();
        menuProviderFactory.registerMaterial();
    }

    private void setupRecyclerView() {
        recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setClipChildren(false);
        recyclerView.setClipToPadding(false);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerView.setNestedScrollingEnabled(false);
        gridLayoutManager = new GridLayoutManager(requireContext(), BAG_GRID_COLUMNS) {
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
        setupEquipToBagDropListener();
        applyAdaptiveBagCellSize();
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                applyAdaptiveBagCellSize();
            }
        });
        gridBagContainer.addView(recyclerView);
    }

    private void setupEquipToBagDropListener() {
        recyclerView.setOnDragListener((v, event) -> {
            if (!(event.getLocalState() instanceof Integer)) return false;
            int slotId = (Integer) event.getLocalState();
            EquipItem equipItem = equippedItems.get(slotId);
            if (equipItem == null) return false;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    isDragFromEquipSlot = true;
                    dragStartPage = -1;
                    dragStartUiPosition = -1;
                    dragToUiPosition = -1;
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION: {
                    int hoverPos = findBagAdapterPositionByLocalPoint(event.getX(), event.getY());
                    if (hoverPos != dragToUiPosition) {
                        int oldHover = dragToUiPosition;
                        dragToUiPosition = hoverPos;
                        if (oldHover >= 0 && oldHover < itemsPerPage) {
                            adapter.notifyItemChanged(oldHover, "HOVER_CLEAR");
                        }
                        if (dragToUiPosition >= 0 && dragToUiPosition < itemsPerPage) {
                            adapter.notifyItemChanged(dragToUiPosition, "HOVER");
                        }
                    }
                    return true;
                }

                case DragEvent.ACTION_DRAG_EXITED:
                    if (dragToUiPosition >= 0 && dragToUiPosition < itemsPerPage) {
                        adapter.notifyItemChanged(dragToUiPosition, "HOVER_CLEAR");
                    }
                    dragToUiPosition = -1;
                    return true;

                case DragEvent.ACTION_DROP:
                    try {
                        int targetPos = findBagAdapterPositionByLocalPoint(event.getX(), event.getY());
                        if (targetPos < 0) {
                            Toast.makeText(getContext(), "请拖到背包格子内再松手", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        int realIndex = (currentPage - 1) * itemsPerPage + targetPos;
                        if (realIndex < 0 || realIndex >= allItems.size()) return true;
                        if (allItems.get(realIndex) != null) {
                            Toast.makeText(getContext(), "目标格子已有物品", Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        allItems.set(realIndex, equipItem);
                        equippedItems.remove(slotId);
                        syncCharacterUnequip(slotId);
                        updateEquipSlotView(slotId, null);
                        adapter.notifyDataSetChanged();
                        persistSharedBagGridToInventory();
                        Toast.makeText(getContext(), "已拖拽卸下: " + equipItem.getName(), Toast.LENGTH_SHORT).show();
                        return true;
                    } finally {
                        finishEquipSlotDragSession();
                    }

                case DragEvent.ACTION_DRAG_ENDED:
                    finishEquipSlotDragSession();
                    return false;

                default:
                    return true;
            }
        });
    }

    private void finishEquipSlotDragSession() {
        if (!isDragFromEquipSlot) {
            return;
        }
        isDragFromEquipSlot = false;
        if (dragToUiPosition >= 0 && dragToUiPosition < itemsPerPage) {
            adapter.notifyItemChanged(dragToUiPosition, "HOVER_CLEAR");
        }
        dragToUiPosition = -1;
        dragStartPage = -1;
        dragStartUiPosition = -1;
    }

    private boolean shouldShowBagCellHoverScale(@NonNull RecyclerView.ViewHolder holder, int adapterPosition) {
        if (dragToUiPosition < 0 || adapterPosition != dragToUiPosition) {
            return false;
        }
        boolean internalBagDrag = currentDragHolder != null;
        if (!internalBagDrag && !isDragFromEquipSlot) {
            return false;
        }
        boolean isSamePageStartTarget =
                currentPage == dragStartPage && adapterPosition == dragStartUiPosition;
        if (isSamePageStartTarget) {
            return false;
        }
        if (internalBagDrag) {
            boolean isCrossPageStartTarget =
                    currentPage != dragStartPage && adapterPosition == dragStartUiPosition;
            return holder != currentDragHolder || isCrossPageStartTarget;
        }
        return true;
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

    private void bindEquipSlots(View root) {
        bindSingleEquipSlot(root, R.id.slot_weapon, "武器");
        bindSingleEquipSlot(root, R.id.slot_helmet, "头盔");
        bindSingleEquipSlot(root, R.id.slot_chest, "胸甲");
        bindSingleEquipSlot(root, R.id.slot_leggings, "护腿");
        bindSingleEquipSlot(root, R.id.slot_boots, "鞋子");
        bindSingleEquipSlot(root, R.id.slot_necklace, "项链");
        bindSingleEquipSlot(root, R.id.slot_bracelet, "手镯");
        bindSingleEquipSlot(root, R.id.slot_ring_left, "左戒");
        bindSingleEquipSlot(root, R.id.slot_ring_right, "右戒");
    }

    private void bindSingleEquipSlot(View root, int slotViewId, String defaultText) {
        View slotView = root.findViewById(slotViewId);
        if (slotView != null) {
            equipSlotViews.put(slotViewId, slotView);
            equipSlotDefaultTexts.put(slotViewId, defaultText);
            updateEquipSlotView(slotViewId, null);
            slotView.setOnClickListener(v -> onEquipSlotClicked(v, slotViewId));
            slotView.setOnLongClickListener(v -> startDragFromEquipSlot(v, slotViewId));
        }
    }

    private boolean startDragFromEquipSlot(View slotView, int slotViewId) {
        EquipItem equipped = equippedItems.get(slotViewId);
        if (equipped == null) {
            Toast.makeText(getContext(), "该槽位暂无装备", Toast.LENGTH_SHORT).show();
            return false;
        }
        ClipData dragData = ClipData.newPlainText(DRAG_LABEL_EQUIP_FROM_SLOT, String.valueOf(slotViewId));
        return slotView.startDragAndDrop(
                dragData,
                new View.DragShadowBuilder(slotView),
                Integer.valueOf(slotViewId),
                0
        );
    }

    private void onEquipSlotClicked(View anchor, int slotViewId) {
        EquipItem equipped = equippedItems.get(slotViewId);
        if (equipped == null) {
            EquipSlot slotFilter = resolveFilterSlotByViewId(slotViewId);
            if (slotFilter == null) {
                Toast.makeText(getContext(), "该槽位暂不支持筛选", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentFilterSlot == slotFilter) {
                applyFilter(null);
                return;
            }
            applyFilter(slotFilter);
            return;
        }
        showEquippedItemMenu(anchor, slotViewId, equipped);
    }

    private void setupPagination() {
        updatePageUI();
        btnPrevPage.setOnClickListener(v -> goPrevPage());
        btnNextPage.setOnClickListener(v -> goNextPage());
        btnCompactBag.setOnClickListener(v -> {
            compactAllItemsForward();
            adapter.notifyDataSetChanged();
            persistSharedBagGridToInventory();
            Toast.makeText(getContext(), "已向前整理背包", Toast.LENGTH_SHORT).show();
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
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
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
        if (slot != null) {
            persistSharedBagGridToInventory();
        }
        if (slot == null) {
            Toast.makeText(getContext(), "已取消筛选", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "已筛选: " + getSlotLabel(slot), Toast.LENGTH_SHORT).show();
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
                    return makeMovementFlags(0, 0);
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

                if (oldHover != -1) {
                    adapter.notifyItemChanged(oldHover, "HOVER_CLEAR");
                }
                adapter.notifyItemChanged(dragToUiPosition, "HOVER");

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

            @Override
            public boolean isLongPressDragEnabled() { return true; }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    applyBagDragLayering(true);
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
                    if (releasePos >= 0) {
                        dragToUiPosition = releasePos;
                    } else if (dragToUiPosition < 0) {
                        dragToUiPosition = dragStartUiPosition;
                    }

                    boolean handledEquipDrop = tryHandleDropToEquipSlot();
                    if (dragStartPage != -1 && dragStartUiPosition != -1 && dragToUiPosition != -1) {
                        int realFrom = (dragStartPage - 1) * itemsPerPage + dragStartUiPosition;
                        int realTo = (currentPage - 1) * itemsPerPage + dragToUiPosition;

                        if (!handledEquipDrop && realFrom != realTo) {
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

                    adapter.notifyDataSetChanged();
                    persistSharedBagGridToInventory();
                    applyBagDragLayering(false);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
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

                    float centerYInRecycler = viewY + viewHolder.itemView.getHeight() / 2f;
                    boolean withinBagHeight = centerYInRecycler >= 0 && centerYInRecycler <= screenHeight;
                    if (withinBagHeight && viewX < screenWidth * 0.1f && dX < 0) {
                        checkEdgeScroll(-1);
                    } else if (withinBagHeight && viewX + viewHolder.itemView.getWidth() > screenWidth * 0.9f && dX > 0) {
                        checkEdgeScroll(1);
                    } else {
                        stopEdgeScroll();
                    }

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

                    if (currentPage == dragStartPage) {
                        c.save();
                        float bgScale = (dragToUiPosition == dragStartUiPosition) ? 0.85f : 1.0f;
                        float pivotX = dragStartCellLeft + dragStartCellWidth / 2f;
                        float pivotY = dragStartCellTop + dragStartCellHeight / 2f;
                        c.translate(pivotX, pivotY);
                        c.scale(bgScale, bgScale);
                        c.translate(-dragStartCellWidth / 2f, -dragStartCellHeight / 2f);
                        c.saveLayerAlpha(0, 0, dragStartCellWidth, dragStartCellHeight, 76);
                        viewHolder.itemView.draw(c);
                        c.restore();
                        c.restore();
                    }

                    if (currentPage != dragStartPage) {
                        if (holeView == null && viewHolder.itemView.getWidth() > 0) {
                            holeView = LayoutInflater.from(requireContext()).inflate(R.layout.item_bag_grid, recyclerView, false);
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
                                holeHolder.bgItemColor.setBackgroundTintList(ColorStateList.valueOf(
                                        ContextCompat.getColor(requireContext(), R.color.tb_slot_empty)
                                ));
                            } else {
                                holeHolder.tvItemName.setText(holeItem.getName());
                                holeHolder.ivItemIcon.setVisibility(View.VISIBLE);
                                bindBagItemIcon(holeHolder.ivItemIcon, holeItem.getIconResId());
                                holeHolder.bgItemColor.setBackgroundResource(R.drawable.bg_slot_treasure_fill);
                                holeHolder.bgItemColor.setBackgroundTintList(ColorStateList.valueOf(holeItem.getRarity().getColor()));
                                bindBagStackCountBadge(holeHolder.tvBagStackCount, holeItem);

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
                    edgeScrollHandler.postDelayed(edgeScrollRunnable, 800);
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

    private boolean tryHandleDropToEquipSlot() {
        if (currentDragHolder == null || dragStartPage == -1 || dragStartUiPosition == -1) {
            return false;
        }
        int targetSlotViewId = findEquipSlotByDragPosition(currentDragHolder.itemView);
        if (targetSlotViewId == -1) {
            return false;
        }

        int sourceIndex = (dragStartPage - 1) * itemsPerPage + dragStartUiPosition;
        if (sourceIndex < 0 || sourceIndex >= allItems.size()) {
            return false;
        }

        Item sourceItem = allItems.get(sourceIndex);
        if (!(sourceItem instanceof EquipItem)) {
            Toast.makeText(getContext(), "只能把装备拖入装备栏", Toast.LENGTH_SHORT).show();
            return true;
        }

        EquipItem draggedEquip = (EquipItem) sourceItem;
        if (!canEquipToSlotView(targetSlotViewId, draggedEquip.getSlot())) {
            Toast.makeText(getContext(), "该槽位与装备类型不匹配", Toast.LENGTH_SHORT).show();
            return true;
        }

        EquipItem previousEquip = equippedItems.get(targetSlotViewId);
        equippedItems.put(targetSlotViewId, draggedEquip);
        syncCharacterEquip(draggedEquip);
        allItems.set(sourceIndex, previousEquip);
        updateEquipSlotView(targetSlotViewId, draggedEquip);

        Toast.makeText(getContext(), "已装备: " + draggedEquip.getName(), Toast.LENGTH_SHORT).show();
        return true;
    }

    private int findEquipSlotByDragPosition(View dragView) {
        int centerX;
        int centerY;
        if (lastDragCenterX >= 0 && lastDragCenterY >= 0) {
            centerX = lastDragCenterX;
            centerY = lastDragCenterY;
        } else {
            Rect dragRect = new Rect();
            if (!dragView.getGlobalVisibleRect(dragRect)) {
                return -1;
            }
            centerX = dragRect.centerX();
            centerY = dragRect.centerY();
        }
        Rect slotRect = new Rect();
        for (Map.Entry<Integer, View> entry : equipSlotViews.entrySet()) {
            View slotView = entry.getValue();
            if (slotView != null && slotView.getGlobalVisibleRect(slotRect) && slotRect.contains(centerX, centerY)) {
                return entry.getKey();
            }
        }
        return -1;
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

    private boolean canEquipToSlotView(int slotViewId, EquipSlot equipSlot) {
        if (slotViewId == R.id.slot_weapon) return equipSlot == EquipSlot.WEAPON;
        if (slotViewId == R.id.slot_helmet) return equipSlot == EquipSlot.HELMET;
        if (slotViewId == R.id.slot_chest) return equipSlot == EquipSlot.CHEST;
        if (slotViewId == R.id.slot_leggings) return equipSlot == EquipSlot.LEGGINGS;
        if (slotViewId == R.id.slot_boots) return equipSlot == EquipSlot.BOOTS;
        if (slotViewId == R.id.slot_necklace) return equipSlot == EquipSlot.NECKLACE;
        if (slotViewId == R.id.slot_bracelet) return equipSlot == EquipSlot.BRACELET;
        if (slotViewId == R.id.slot_ring_left || slotViewId == R.id.slot_ring_right) {
            return equipSlot == EquipSlot.RING;
        }
        return false;
    }

    private void updateEquipSlotView(int slotViewId, @Nullable EquipItem equipItem) {
        View slotView = equipSlotViews.get(slotViewId);
        if (!(slotView instanceof LinearLayout)) return;
        if (equipItem == null) {
            applyEquipSlotPlaceholder((LinearLayout) slotView, equipSlotDefaultTexts.getOrDefault(slotViewId, ""));
        } else {
            applyEquipSlotItemView((LinearLayout) slotView, equipItem);
        }
    }

    private void applyEquipSlotPlaceholder(LinearLayout slotLayout, String text) {
        slotLayout.setBackgroundResource(R.drawable.bg_slot_treasure);
        slotLayout.setPadding(0, 0, 0, 0);
        slotLayout.removeAllViews();
        TextView placeholder = new TextView(requireContext());
        placeholder.setText(text);
        placeholder.setTextColor(ContextCompat.getColor(requireContext(), R.color.tb_text_main));
        placeholder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        placeholder.setGravity(Gravity.CENTER);
        slotLayout.setGravity(Gravity.CENTER);
        slotLayout.addView(placeholder, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
    }

    private void applyEquipSlotItemView(LinearLayout slotLayout, EquipItem equipItem) {
        slotLayout.setBackgroundResource(R.drawable.bg_slot_treasure_stroke);
        int inset = dpToPx(1);
        slotLayout.setPadding(inset, inset, inset, inset);
        slotLayout.removeAllViews();
        View inner = LayoutInflater.from(requireContext()).inflate(R.layout.item_bag_grid_inner, slotLayout, false);
        inner.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        RelativeLayout bg = inner.findViewById(R.id.bg_item_color);
        ImageView icon = inner.findViewById(R.id.iv_item_icon);
        TextView level = inner.findViewById(R.id.tv_item_level);
        TextView name = inner.findViewById(R.id.tv_item_name);

        if (bg != null) {
            bg.setBackgroundResource(R.drawable.bg_slot_treasure_fill);
            bg.setBackgroundTintList(ColorStateList.valueOf(equipItem.getRarity().getColor()));
        }
        if (icon != null) {
            icon.setVisibility(View.VISIBLE);
            bindBagItemIcon(icon, equipItem.getIconResId());
        }
        if (level != null) {
            level.setVisibility(View.VISIBLE);
            level.setText("Lv." + equipItem.getLevel());
        }
        if (name != null) {
            name.setText(equipItem.getName());
        }
        slotLayout.addView(inner);
    }

    private void showEquippedItemMenu(View anchor, int slotViewId, EquipItem item) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add(0, 1, 0, "查看详情");
        popupMenu.getMenu().add(0, 2, 0, "卸下");
        popupMenu.getMenu().add(0, 3, 0, "丢弃");

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == 1) {
                ItemDetailDialog.show(requireContext(), item);
                return true;
            }
            if (menuItem.getItemId() == 2) {
                if (tryPutIntoBag(item)) {
                    equippedItems.remove(slotViewId);
                    syncCharacterUnequip(slotViewId);
                    updateEquipSlotView(slotViewId, null);
                    adapter.notifyDataSetChanged();
                    persistSharedBagGridToInventory();
                    Toast.makeText(getContext(), "已卸下: " + item.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "背包已满，无法卸下", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            if (menuItem.getItemId() == 3) {
                equippedItems.remove(slotViewId);
                syncCharacterUnequip(slotViewId);
                updateEquipSlotView(slotViewId, null);
                Toast.makeText(getContext(), "已丢弃: " + item.getName(), Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private boolean tryPutIntoBag(Item item) {
        int bagCapacity = InventoryGridSync.BAG_SLOT_COUNT;
        for (int i = 0; i < bagCapacity; i++) {
            if (allItems.get(i) == null) {
                allItems.set(i, item);
                return true;
            }
        }
        return false;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
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

    private void bindBagItemIcon(@Nullable ImageView imageView, int iconResId) {
        if (imageView == null) return;
        imageView.setImageResource(iconResId);
        Drawable d = imageView.getDrawable();
        if (d != null) {
            d.mutate();
            if (d instanceof BitmapDrawable) {
                ((BitmapDrawable) d).setFilterBitmap(false);
            }
        }
    }

    private void syncCharacterEquip(EquipItem item) {
        Character ch = PlayerCharacterHolder.getOrCreate(getContext());
        if (ch != null && item != null) {
            ch.equip(item);
        }
    }

    private void syncCharacterUnequip(int slotViewId) {
        Character ch = PlayerCharacterHolder.getOrCreate(getContext());
        if (ch == null) return;
        EquipSlot slot = viewIdToEquipSlot(slotViewId);
        if (slot != null) {
            ch.unequip(slot);
        }
    }

    private static EquipSlot viewIdToEquipSlot(int slotViewId) {
        if (slotViewId == R.id.slot_weapon)  return EquipSlot.WEAPON;
        if (slotViewId == R.id.slot_helmet)  return EquipSlot.HELMET;
        if (slotViewId == R.id.slot_chest)   return EquipSlot.CHEST;
        if (slotViewId == R.id.slot_leggings) return EquipSlot.LEGGINGS;
        if (slotViewId == R.id.slot_boots)   return EquipSlot.BOOTS;
        if (slotViewId == R.id.slot_necklace) return EquipSlot.NECKLACE;
        if (slotViewId == R.id.slot_bracelet) return EquipSlot.BRACELET;
        if (slotViewId == R.id.slot_ring_left || slotViewId == R.id.slot_ring_right) return EquipSlot.RING;
        return null;
    }

    private boolean autoEquipFromBag(int bagIndex, EquipItem equipItem) {
        int targetSlotId = resolveAutoEquipSlotId(equipItem);
        if (targetSlotId == -1) {
            Toast.makeText(getContext(), "没有可用的装备槽位", Toast.LENGTH_SHORT).show();
            return false;
        }

        EquipItem previousEquip = equippedItems.get(targetSlotId);
        equippedItems.put(targetSlotId, equipItem);
        syncCharacterEquip(equipItem);
        updateEquipSlotView(targetSlotId, equipItem);
        allItems.set(bagIndex, previousEquip);
        Toast.makeText(getContext(), "已装备: " + equipItem.getName(), Toast.LENGTH_SHORT).show();
        return true;
    }

    private int resolveAutoEquipSlotId(EquipItem equipItem) {
        EquipSlot slot = equipItem.getSlot();
        if (slot == null) return -1;
        if (slot == EquipSlot.WEAPON) return R.id.slot_weapon;
        if (slot == EquipSlot.HELMET) return R.id.slot_helmet;
        if (slot == EquipSlot.CHEST) return R.id.slot_chest;
        if (slot == EquipSlot.LEGGINGS) return R.id.slot_leggings;
        if (slot == EquipSlot.BOOTS) return R.id.slot_boots;
        if (slot == EquipSlot.NECKLACE) return R.id.slot_necklace;
        if (slot == EquipSlot.BRACELET) return R.id.slot_bracelet;
        if (slot == EquipSlot.RING) {
            if (!equippedItems.containsKey(R.id.slot_ring_left)) return R.id.slot_ring_left;
            if (!equippedItems.containsKey(R.id.slot_ring_right)) return R.id.slot_ring_right;
            return R.id.slot_ring_left;
        }
        return -1;
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
                holder.bgItemColor.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.tb_slot_empty)
                ));
            } else {
                holder.tvItemName.setText(item.getName());
                holder.ivItemIcon.setVisibility(View.VISIBLE);
                bindBagItemIcon(holder.ivItemIcon, item.getIconResId());
                holder.bgItemColor.setBackgroundResource(R.drawable.bg_slot_treasure_fill);
                holder.bgItemColor.setBackgroundTintList(ColorStateList.valueOf(item.getRarity().getColor()));
                bindBagStackCountBadge(holder.tvBagStackCount, item);

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
            List<ItemAction> actions = menuProviderFactory.getActions(item);
            if (actions.isEmpty()) return;

            PopupMenu popupMenu = new PopupMenu(requireContext(), view);
            for (int i = 0; i < actions.size(); i++) {
                ItemAction action = actions.get(i);
                popupMenu.getMenu().add(0, i, i, action.getDisplayText());
            }

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int index = menuItem.getItemId();
                if (index >= 0 && index < actions.size()) {
                    ItemAction action = actions.get(index);
                    if (action.enabled && action.action != null) {
                        action.action.accept(item);
                    }
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
}