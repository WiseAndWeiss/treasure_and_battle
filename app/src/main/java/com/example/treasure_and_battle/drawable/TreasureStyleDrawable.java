package com.example.treasure_and_battle.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.treasure_and_battle.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import android.graphics.drawable.Drawable;

/**
 * 宝箱主题 UI：阶梯描边 + 背景在边框内侧再缩进 1·bw（与描边同一 bw）。
 */
public final class TreasureStyleDrawable extends Drawable {

    private enum Variant {
        PANEL,
        SLOT,
        SLOT_STROKE,
        SLOT_FILL,
        TAB_ACTIVE,
        TAB_IDLE,
        SCREEN,
        BOTTOM_NAV,
        PANEL_FILL,
        PANEL_STROKE,
        /** 与 {@link #SCREEN} 同色填充，无阶梯外框（用于主界面根布局等）。 */
        SCREEN_FILL,
        ENTRY_PRIMARY
    }

    private static final float STAIR_BORDER_WIDTH_DP = 3f;

    private final Paint fillPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private float density = 1f;
    private Variant variant = Variant.PANEL;
    @Nullable private Integer stairBorderColorOverride;

    public TreasureStyleDrawable() {
        fillPaint.setAntiAlias(false);
        fillPaint.setDither(false);
        fillPaint.setFilterBitmap(false);
        fillPaint.setStyle(Paint.Style.FILL);

        borderPaint.setAntiAlias(false);
        borderPaint.setDither(false);
        borderPaint.setFilterBitmap(false);
        borderPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void inflate(@NonNull Resources r, @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
                        @Nullable Resources.Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        density = r.getDisplayMetrics().density;
        TypedArray a = theme != null
                ? theme.obtainStyledAttributes(attrs, R.styleable.TreasureStyleDrawable, 0, 0)
                : r.obtainAttributes(attrs, R.styleable.TreasureStyleDrawable);
        try {
            int ord = a.getInt(R.styleable.TreasureStyleDrawable_treasureVariant, 0);
            Variant[] vals = Variant.values();
            variant = vals[Math.max(0, Math.min(ord, vals.length - 1))];
        } finally {
            a.recycle();
        }
    }

    private int dp(float dp) {
        return Math.max(1, Math.round(dp * density));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        int L = b.left;
        int T = b.top;
        int R = b.right;
        int B = b.bottom;

        switch (variant) {
            case PANEL:
                drawFillInsetOneBw(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP),
                        (c, iL, iT, iR, iB) -> drawSolidFill(c, iL, iT, iR, iB, 0xFFD9C29A));
                drawSlotStairFrame(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP), 0xFF5F3A1D);
                break;
            case SLOT:
                drawFillInsetOneBw(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP),
                        (c, iL, iT, iR, iB) -> drawSolidFill(c, iL, iT, iR, iB, 0xFFC9AD84));
                drawSlotStairFrame(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP), 0xFF5F3A1D);
                break;
            case SLOT_STROKE:
                drawSlotStairFrame(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP), 0xFF5F3A1D);
                break;
            case SLOT_FILL:
                drawFillInsetOneBw(canvas, L, T, R, B, dp(2.7f),
                        (c, iL, iT, iR, iB) -> drawSolidFill(c, iL, iT, iR, iB, 0xFFC9AD84));
                break;
            case TAB_ACTIVE:
                drawFillInsetOneBw(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP),
                        (c, iL, iT, iR, iB) -> drawSolidFill(c, iL, iT, iR, iB, 0xFFD3A569));
                drawSlotStairFrame(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP), 0xFF5F3A1D);
                break;
            case TAB_IDLE:
                drawFillInsetOneBw(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP), (c, iL, iT, iR, iB) ->
                        drawSolidFill(c, iL, iT, iR, iB, 0x66E7D4AE));
                drawSlotStairFrame(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP), 0xB37B5C3B);
                break;
            case SCREEN:
                drawFillInsetOneBw(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP),
                        (c, iL, iT, iR, iB) -> drawSolidFill(c, iL, iT, iR, iB, 0xFFC9AD84));
                drawSlotStairFrame(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP), 0x66705134);
                break;
            case BOTTOM_NAV:
                drawFillInsetOneBw(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP),
                        (c, iL, iT, iR, iB) -> drawSolidFill(c, iL, iT, iR, iB, 0xFF996B3F));
                drawSlotStairFrame(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP), 0xFF5F3A1D);
                break;
            case PANEL_FILL:
                drawFillInsetOneBw(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP),
                        (c, iL, iT, iR, iB) -> drawSolidFill(c, iL, iT, iR, iB, 0xFFD9C29A));
                break;
            case PANEL_STROKE:
                drawSlotStairFrame(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP), 0xFF5F3A1D);
                break;
            case SCREEN_FILL:
                drawSolidFill(canvas, L, T, R, B, 0xFFC9AD84);
                break;
            case ENTRY_PRIMARY:
                drawFillInsetOneBw(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP),
                        (c, iL, iT, iR, iB) -> drawSolidFill(c, iL, iT, iR, iB, 0xFF5F3A1D));
                drawSlotStairFrame(canvas, L, T, R, B, dp(STAIR_BORDER_WIDTH_DP), 0xFF5F3A1D);
                break;
            default:
                break;
        }
    }

    @FunctionalInterface
    private interface InsetFillRenderer {
        void draw(@NonNull Canvas canvas, int iL, int iT, int iR, int iB);
    }

    /** 与描边共用的 bw（阶梯线宽），小区域时自动变细。 */
    private static int resolveStairBorderWidth(int w, int h, int requestedBorderPx) {
        int bw = Math.max(1, requestedBorderPx);
        while (bw > 1 && (w < 4 * bw || h < 4 * bw)) {
            bw--;
        }
        return bw;
    }

    /**
     * 背景相对外框四边各缩进 1·bw；描边仍由 {@link #drawSlotStairFrame} 画满 (L,T,R,B)。
     */
    private void drawFillInsetOneBw(
            @NonNull Canvas canvas, int L, int T, int R, int B, int requestedBorderPx, @NonNull InsetFillRenderer renderer) {
        int w = R - L;
        int h = B - T;
        if (w < 2 || h < 2) {
            return;
        }
        int bw = resolveStairBorderWidth(w, h, requestedBorderPx);
        int iL = L + bw;
        int iT = T + bw;
        int iR = R - bw;
        int iB = B - bw;
        if (iL < iR && iT < iB) {
            renderer.draw(canvas, iL, iT, iR, iB);
        }
    }

    private void drawSolidFill(Canvas canvas, int L, int T, int R, int B, int color) {
        fillPaint.setColor(color);
        canvas.drawRect(L, T, R, B, fillPaint);
    }

    /**
     * 格子像素边框（四角阶梯衔接）：
     * 1. 竖条：左边框 (L, T + bw, L + bw, B - bw)
     * 2. 角块：(L + bw, T + bw, L + 2*bw, T + 2*bw)
     * 3. 横条：上边框 (L + 2*bw, T, R - 2*bw, T + bw)
     * 四边对称。若作为 View 的 background，角块可能被子视图盖住，仅描边时宜用 foreground。
     */
    private void drawSlotStairFrame(Canvas canvas, int L, int T, int R, int B, int borderPx, int color) {
        if (stairBorderColorOverride != null) {
            color = stairBorderColorOverride;
        }
        int w = R - L;
        int h = B - T;
        int bw = resolveStairBorderWidth(w, h, borderPx);
        if (w < 2 || h < 2) {
            return;
        }

        borderPaint.setColor(color);

        // 上边框：横条（排除左右各 2*bw 的区域）
        int topLeft = L + 2 * bw;
        int topRight = R - 2 * bw;
        if (topLeft < topRight) {
            canvas.drawRect(topLeft, T, topRight, T + bw, borderPaint);
        }

        // 下边框：横条
        int botLeft = L + 2 * bw;
        int botRight = R - 2 * bw;
        if (botLeft < botRight) {
            canvas.drawRect(botLeft, B - bw, botRight, B, borderPaint);
        }

        // 左边框：竖条（与角块衔接）
        canvas.drawRect(L, T + 2*bw, L + bw, B - 2*bw, borderPaint);

        // 右边框：竖条
        canvas.drawRect(R - bw, T + 2*bw, R, B - 2*bw, borderPaint);

        // 四角 bw×bw 角块
        if (L + 2 * bw <= R && T + 2 * bw <= B) {
            canvas.drawRect(L + bw, T + bw, L + 2 * bw, T + 2 * bw, borderPaint);
            canvas.drawRect(R - 2 * bw, T + bw, R - bw, T + 2 * bw, borderPaint);
            canvas.drawRect(L + bw, B - 2 * bw, L + 2 * bw, B - bw, borderPaint);
            canvas.drawRect(R - 2 * bw, B - 2 * bw, R - bw, B - bw, borderPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        borderPaint.setAlpha(alpha);
    }

    @Override
    public int getAlpha() {
        return fillPaint.getAlpha();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        fillPaint.setColorFilter(colorFilter);
        borderPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @NonNull
    public static TreasureStyleDrawable newSlotStrokeOverlay(@NonNull Context context, @Nullable Integer borderArgb) {
        TreasureStyleDrawable d = new TreasureStyleDrawable();
        d.density = context.getResources().getDisplayMetrics().density;
        d.variant = Variant.SLOT_STROKE;
        d.stairBorderColorOverride = borderArgb;
        return d;
    }
}
