package com.example.treasure_and_battle.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.treasure_and_battle.R;
import com.example.treasure_and_battle.model.item.Equipment;
import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.common.Rarity;

import java.util.ArrayList;
import java.util.List;

public class BagFragment extends Fragment {

    private TextView tvPageInfo;
    private TextView btnPrevPage;
    private TextView btnNextPage;
    private LinearLayout gridBagContainer;

    private RecyclerView recyclerView;
    private BagAdapter adapter;

    private int currentPage = 1;
    private final int totalPages = 5;
    private final int itemsPerPage = 30;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bag, container, false);

        if (view instanceof ViewGroup) {
            ((ViewGroup) view).setClipChildren(false);
            ((ViewGroup) view).setClipToPadding(false);
        }

        tvPageInfo = view.findViewById(R.id.tv_page_info);
        btnPrevPage = view.findViewById(R.id.btn_prev_page);
        btnNextPage = view.findViewById(R.id.btn_next_page);
        gridBagContainer = view.findViewById(R.id.grid_bag_container);
        gridBagContainer.setClipChildren(false);
        gridBagContainer.setClipToPadding(false);

        initDummyData();
        setupRecyclerView();
        setupPagination();
        setupDragAndDrop();

        return view;
    }

    private void initDummyData() {
        allItems = new ArrayList<>(totalPages * itemsPerPage);
        for (int i = 0; i < totalPages * itemsPerPage; i++) {
            if (i == 0) {
                allItems.add(new Equipment(1, "新兵铁剑", Rarity.COMMON, android.R.drawable.ic_menu_gallery, "一把普通的铁剑", 10, Equipment.SLOT_WEAPON, Equipment.SUBTYPE_WEAPON_SWORD, 1, 1, new int[]{2,0,0,0,0,0}));
            } else if (i == 1) {
                allItems.add(new Equipment(2, "古白金戒指", Rarity.LEGENDARY, android.R.drawable.ic_menu_gallery, "古老的传奇戒指", 50, Equipment.SLOT_RING, Equipment.SUBTYPE_ACCESSORY, 15, 15, new int[]{0,0,0,0,0,0}));
            } else if (i == 2) {
                allItems.add(new Item(3, "红药水", Item.TYPE_CONSUMABLE, Rarity.UNCOMMON, android.R.drawable.ic_menu_gallery, "恢复HP", 99, 5));
            } else if (i == 31) {
                allItems.add(new Item(4, "第二页的物品", Item.TYPE_MATERIAL, Rarity.RARE, android.R.drawable.ic_menu_gallery, "测试", 99, 1));
            } else {
                allItems.add(null);
            }
        }
    }

    private void setupRecyclerView() {
        recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setClipChildren(false);
        recyclerView.setClipToPadding(false);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 6));

        adapter = new BagAdapter();
        recyclerView.setAdapter(adapter);
        gridBagContainer.addView(recyclerView);
    }

    private void setupPagination() {
        updatePageUI();
        btnPrevPage.setOnClickListener(v -> goPrevPage());
        btnNextPage.setOnClickListener(v -> goNextPage());
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
        adapter.notifyDataSetChanged();
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (currentPage == totalPages) return makeMovementFlags(0, 0); // 第5页写死锁定，禁止拖拽起步
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
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    stopEdgeScroll();
                    isNavigatingPage = false;

                    if (dragStartPage != -1 && dragStartUiPosition != -1 && dragToUiPosition != -1) {
                        int realFrom = (dragStartPage - 1) * itemsPerPage + dragStartUiPosition;
                        int realTo = (currentPage - 1) * itemsPerPage + dragToUiPosition;

                        if (currentPage == totalPages && realFrom != realTo) {
                            Toast.makeText(getContext(), "该页面未解锁，无法放置", Toast.LENGTH_SHORT).show();
                        } else if (realFrom != realTo) {
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

                    adapter.notifyDataSetChanged(); // 刷新全局状态，消除残影与Hover
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                    int screenWidth = recyclerView.getWidth();
                    float viewX = viewHolder.itemView.getLeft() + dX;

                    // 边缘触发翻页检测
                    if (viewX < screenWidth * 0.15f && dX < 0) {
                        checkEdgeScroll(-1);
                    } else if (viewX + viewHolder.itemView.getWidth() > screenWidth * 0.85f && dX > 0) {
                        checkEdgeScroll(1);
                    } else {
                        stopEdgeScroll();
                    }

                    // 仅当目前处于原始页面时，才绘制原始格子的虚影
                    if (currentPage == dragStartPage) {
                        c.save();
                        c.translate(viewHolder.itemView.getLeft(), viewHolder.itemView.getTop());
                        // 强制截取绘制层，令整个格子统一附上30% (76/255) 的透明度
                        c.saveLayerAlpha(0, 0, viewHolder.itemView.getWidth(), viewHolder.itemView.getHeight(), 76);
                        viewHolder.itemView.draw(c);
                        c.restore();
                        c.restore();
                    }

                    // 跨页后，填补本来被拖拽物作为 draggedView 占掉坑位而引起的空白
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

                            if (currentPage == totalPages) {
                                holeHolder.tvItemName.setText("");
                                holeHolder.tvItemLevel.setVisibility(View.GONE);
                                holeHolder.ivItemIcon.setVisibility(View.VISIBLE);
                                holeHolder.ivItemIcon.setImageResource(android.R.drawable.ic_secure);
                                holeHolder.bgItemColor.setBackgroundColor(Color.parseColor("#333333"));
                            } else if (holeItem == null) {
                                holeHolder.tvItemName.setText("");
                                holeHolder.tvItemLevel.setVisibility(View.GONE);
                                holeHolder.ivItemIcon.setVisibility(View.INVISIBLE);
                                holeHolder.bgItemColor.setBackgroundColor(Color.parseColor("#EAEAEA"));
                            } else {
                                holeHolder.tvItemName.setText(holeItem.getItemName());
                                holeHolder.ivItemIcon.setVisibility(View.VISIBLE);
                                holeHolder.ivItemIcon.setImageResource(holeItem.getIconResId());
                                holeHolder.bgItemColor.setBackgroundColor(holeItem.getRarity().getColor());

                                if (holeItem instanceof Equipment) {
                                    holeHolder.tvItemLevel.setVisibility(View.VISIBLE);
                                    holeHolder.tvItemLevel.setText("Lv." + ((Equipment) holeItem).getEquipmentLevel());
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

    private class BagAdapter extends RecyclerView.Adapter<BagAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bag_grid, parent, false);
            int height = parent.getMeasuredHeight() / 5;
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, height > 0 ? height : ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(2, 2, 2, 2);
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
                    holder.itemView.setScaleX(0.85f);
                    holder.itemView.setScaleY(0.85f);
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
            Item item = allItems.get(realPosition);

            holder.itemView.setScaleX(1.0f);
            holder.itemView.setScaleY(1.0f);

            // 当目标移位时，高亮新坑位
            if (currentDragHolder != null && position == dragToUiPosition) {
                holder.itemView.setScaleX(0.85f);
                holder.itemView.setScaleY(0.85f);
            }

            if (currentPage == totalPages) {
                holder.tvItemName.setText("");
                holder.tvItemLevel.setVisibility(View.GONE);
                holder.ivItemIcon.setVisibility(View.VISIBLE);
                holder.ivItemIcon.setImageResource(android.R.drawable.ic_secure);
                holder.bgItemColor.setBackgroundColor(Color.parseColor("#333333"));
            } else if (item == null) {
                holder.tvItemName.setText("");
                holder.tvItemLevel.setVisibility(View.GONE);
                holder.ivItemIcon.setVisibility(View.INVISIBLE);
                holder.bgItemColor.setBackgroundColor(Color.parseColor("#EAEAEA"));
            } else {
                holder.tvItemName.setText(item.getItemName());
                holder.ivItemIcon.setVisibility(View.VISIBLE);
                holder.ivItemIcon.setImageResource(item.getIconResId());

                int colorColor = item.getRarity().getColor();
                holder.bgItemColor.setBackgroundColor(colorColor);

                if (item instanceof Equipment) {
                    Equipment eq = (Equipment) item;
                    holder.tvItemLevel.setVisibility(View.VISIBLE);
                    holder.tvItemLevel.setText("Lv." + eq.getEquipmentLevel());
                } else {
                    holder.tvItemLevel.setVisibility(View.GONE);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (currentPage == totalPages) {
                    Toast.makeText(getContext(), "该页面为未解锁区域，后续功能开放", Toast.LENGTH_SHORT).show();
                    return;
                }
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
            PopupMenu popupMenu = new PopupMenu(requireContext(), view);
            popupMenu.getMenu().add(0, 1, 0, "查看描述");
            if (item instanceof Equipment) {
                popupMenu.getMenu().add(0, 2, 0, "装备");
            } else {
                popupMenu.getMenu().add(0, 2, 0, "使用");
            }
            popupMenu.getMenu().add(0, 3, 0, "丢弃");

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case 1:
                        Toast.makeText(getContext(), item.getItemName() + ":" + item.getDescription(), Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(getContext(), "正在操作: " + item.getItemName(), Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        allItems.set(realPosition, null);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), "已丢弃" + item.getItemName(), Toast.LENGTH_SHORT).show();
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

            ViewHolder(View itemView) {
                super(itemView);
                bgItemColor = itemView.findViewById(R.id.bg_item_color);
                ivItemIcon = itemView.findViewById(R.id.iv_item_icon);
                tvItemLevel = itemView.findViewById(R.id.tv_item_level);
                tvItemName = itemView.findViewById(R.id.tv_item_name);
            }
        }
    }
}