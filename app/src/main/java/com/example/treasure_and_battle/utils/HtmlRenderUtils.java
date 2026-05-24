package com.example.treasure_and_battle.utils;

import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

public final class HtmlRenderUtils {

    private HtmlRenderUtils() {}

    public static void setHtmlText(@NonNull TextView textView, @Nullable String htmlContent) {
        if (TextUtils.isEmpty(htmlContent)) {
            textView.setText("");
        } else {
            String normalized = htmlContent.replace("\n", "<br>");
            textView.setText(HtmlCompat.fromHtml(normalized, HtmlCompat.FROM_HTML_MODE_LEGACY));
        }
    }

    @NonNull
    public static String colorToHex(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }
}
