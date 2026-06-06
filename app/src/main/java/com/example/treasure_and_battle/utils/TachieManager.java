package com.example.treasure_and_battle.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.treasure_and_battle.model.profession.ProfessionType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class TachieManager {

    public static final int TACHIE_WIDTH = 768;
    public static final int TACHIE_HEIGHT = 512;
    private static final String FOLDER = "icons/tachie";

    private static final Map<String, Bitmap> cache = new HashMap<>();

    private TachieManager() {}

    @Nullable
    private static String fileNameForProfession(@NonNull ProfessionType type) {
        switch (type) {
            case WARRIOR: return "warrior.png";
            case MAGE:    return "mage.png";
            case RANGER:  return "ranger.png";
            default:      return null;
        }
    }

    public static void bind(@NonNull Context context,
                            @NonNull ImageView imageView,
                            @Nullable ProfessionType professionType,
                            int fallbackResId) {
        if (imageView == null) return;
        if (professionType == null) {
            bindFallback(imageView, fallbackResId);
            return;
        }
        String fileName = fileNameForProfession(professionType);
        if (fileName == null) {
            bindFallback(imageView, fallbackResId);
            return;
        }

        Bitmap cached = cache.get(fileName);
        if (cached != null && !cached.isRecycled()) {
            applyBitmap(imageView, cached, context);
            return;
        }

        String path = FOLDER + "/" + fileName;
        try (InputStream is = context.getAssets().open(path)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp != null) {
                cache.put(fileName, bmp);
                applyBitmap(imageView, bmp, context);
                return;
            }
        } catch (IOException ignored) {
        }

        bindFallback(imageView, fallbackResId);
    }

    private static void applyBitmap(@NonNull ImageView imageView,
                                     @NonNull Bitmap bmp,
                                     @NonNull Context context) {
        BitmapDrawable bd = new BitmapDrawable(context.getResources(), bmp);
        bd.setFilterBitmap(false);
        imageView.setImageDrawable(bd);
    }

    private static void bindFallback(@NonNull ImageView imageView, int fallbackResId) {
        int use = fallbackResId != 0 ? fallbackResId : android.R.drawable.ic_menu_gallery;
        imageView.setImageResource(use);
    }

    public static void recycle() {
        for (Bitmap bmp : cache.values()) {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        cache.clear();
    }
}
