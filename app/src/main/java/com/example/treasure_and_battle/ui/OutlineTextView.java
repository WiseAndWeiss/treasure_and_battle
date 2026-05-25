package com.example.treasure_and_battle.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * 先描边再填色，用于事件标题等需要外轮廓的文字。
 */
public class OutlineTextView extends AppCompatTextView {

    private int outlineColor = 0xFFFFFFFF;
    private float outlineWidthPx;
    private int fillColor;

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
                TypedValue.COMPLEX_UNIT_DIP, 2f, context.getResources().getDisplayMetrics());
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(outlineWidthPx);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeMiter(10f);
        setTextColor(outlineColor);
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.FILL);
        setTextColor(fillColor);
        super.onDraw(canvas);
    }
}
