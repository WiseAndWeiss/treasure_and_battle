package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * 外描边文字：八方向偏移叠白字 + 中心填色，避免 STROKE 在排版边界被裁切。
 */
public class OutlineTextView extends AppCompatTextView {

    private static final int[][] OUTLINE_DIRS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0},           {1, 0},
            {-1, 1},  {0, 1},  {1, 1},
    };

    private int outlineColor = 0xFFFFFFFF;
    private float outlineWidthPx;
    private int fillColor;
    private int outlineInsetPx;
    /** 描边 pass 临时改色，勿写入 fillColor */
    private boolean strokePass;

    public OutlineTextView(Context context) {
        super(context);
        init(context);
    }

    public OutlineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OutlineTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        fillColor = getCurrentTextColor();
        outlineWidthPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 3f, context.getResources().getDisplayMetrics());
        outlineInsetPx = (int) Math.ceil(outlineWidthPx);
        setPadding(outlineInsetPx, outlineInsetPx, outlineInsetPx, outlineInsetPx);
        getPaint().setAntiAlias(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        fillColor = getCurrentTextColor();
    }

    @Override
    public void setTextColor(int color) {
        if (!strokePass) {
            fillColor = color;
        }
        super.setTextColor(color);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (outlineInsetPx > 0) {
            int extra = outlineInsetPx * 2;
            setMeasuredDimension(getMeasuredWidth() + extra, getMeasuredHeight() + extra);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float expand = outlineWidthPx;
        canvas.saveLayer(-expand, -expand, getWidth() + expand, getHeight() + expand, null);

        Paint paint = getPaint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        float step = outlineWidthPx * 0.5f;
        strokePass = true;
        super.setTextColor(outlineColor);
        strokePass = false;

        for (int[] dir : OUTLINE_DIRS) {
            canvas.save();
            canvas.translate(dir[0] * step, dir[1] * step);
            super.onDraw(canvas);
            canvas.restore();
        }

        super.setTextColor(fillColor);
        super.onDraw(canvas);

        canvas.restore();
    }
}
