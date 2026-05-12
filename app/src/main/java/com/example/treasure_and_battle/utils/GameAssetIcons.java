package com.example.treasure_and_battle.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.treasure_and_battle.model.item.Item;
import com.example.treasure_and_battle.model.item.ItemType;

import java.io.IOException;
import java.io.InputStream;

/**
 * 游戏内图标：从 assets 按类型分目录加载 PNG。
 * 路径约定：{@code icons/material/<materialId>.png}，以及
 * {@code icons/consumable/}、{@code icons/gem/}、{@code icons/equip/}、
 * {@code icons/monster/}、{@code icons/skill/} 下同名 id 的 png。
 * <p>
 * 说明：{@code res/drawable} 不支持任意子文件夹，因此素材放在 {@code assets/icons/}。
 */
public final class GameAssetIcons {

    public static final String ROOT = "icons";
    public static final String FOLDER_MATERIAL = "material";
    public static final String FOLDER_CONSUMABLE = "consumable";
    public static final String FOLDER_GEM = "gem";
    public static final String FOLDER_EQUIP = "equip";
    public static final String FOLDER_MONSTER = "monster";
    public static final String FOLDER_SKILL = "skill";

    private GameAssetIcons() {}

    public static boolean isSafeAssetId(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == '/' || c == '\\') {
                return false;
            }
        }
        if (id.startsWith(".") || id.endsWith(".")) {
            return false;
        }
        return true;
    }

    @Nullable
    public static String assetPath(@NonNull String folder, @NonNull String id) {
        if (!isSafeAssetId(id)) {
            return null;
        }
        return ROOT + "/" + folder + "/" + id + ".png";
    }

    /**
     * 按物品类型从 {@code icons/<类型>/<item.getId()>.png} 加载；失败则用 {@link Item#getIconResId()}。
     */
    public static void bindItem(@NonNull Context context, @Nullable ImageView imageView, @Nullable Item item) {
        if (imageView == null || item == null) {
            return;
        }
        String folder = folderForItemType(item.getType());
        if (folder == null) {
            bindFallback(imageView, item.getIconResId());
            return;
        }
        bindPng(context, imageView, folder, item.getId(), item.getIconResId());
    }

    @Nullable
    private static String folderForItemType(@Nullable ItemType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case MATERIAL:
                return FOLDER_MATERIAL;
            case CONSUMABLE:
                return FOLDER_CONSUMABLE;
            case GEM:
                return FOLDER_GEM;
            case EQUIPMENT:
                return FOLDER_EQUIP;
            default:
                return null;
        }
    }

    /** 怪物：{@code icons/monster/<entityId>.png}（与 monster_config 中 entityId 一致） */
    public static void bindMonster(
            @NonNull Context context,
            @Nullable ImageView imageView,
            @Nullable String entityId,
            int fallbackResId
    ) {
        if (imageView == null) {
            return;
        }
        bindPng(context, imageView, FOLDER_MONSTER, entityId, fallbackResId);
    }

    /** 技能：{@code icons/skill/<skillId>.png}（与 skill_config 中 skillId 一致） */
    public static void bindSkill(
            @NonNull Context context,
            @Nullable ImageView imageView,
            @Nullable String skillId,
            int fallbackResId
    ) {
        if (imageView == null) {
            return;
        }
        bindPng(context, imageView, FOLDER_SKILL, skillId, fallbackResId);
    }

    public static void bindPng(
            @NonNull Context context,
            @NonNull ImageView imageView,
            @NonNull String folder,
            @Nullable String id,
            int fallbackResId
    ) {
        String path = assetPath(folder, id != null ? id : "");
        if (path != null) {
            try (InputStream is = context.getAssets().open(path)) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    BitmapDrawable bd = new BitmapDrawable(context.getResources(), bmp);
                    bd.setFilterBitmap(false);
                    imageView.setImageDrawable(bd);
                    tuneDrawableFilter(imageView);
                    return;
                }
            } catch (IOException ignored) {
                // fall through
            }
        }
        bindFallback(imageView, fallbackResId);
    }

    private static void bindFallback(@NonNull ImageView imageView, int resId) {
        int use = resId != 0 ? resId : android.R.drawable.ic_menu_gallery;
        imageView.setImageResource(use);
        tuneDrawableFilter(imageView);
    }

    private static void tuneDrawableFilter(@NonNull ImageView imageView) {
        Drawable d = imageView.getDrawable();
        if (d != null) {
            d.mutate();
            if (d instanceof BitmapDrawable) {
                ((BitmapDrawable) d).setFilterBitmap(false);
            }
        }
    }
}
