package com.example.expensetracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * A custom-drawn ring chart showing how spending is split between categories.
 * The ring "draws itself" with an animation.
 */
public class DonutChartView extends View {

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final float stroke;

    private float[] values = new float[0];
    private int[] colors = new int[0];
    private float progress = 1f;

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        stroke = Ui.dp(context, 22);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(stroke);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(stroke);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.donut_track));
    }

    public void setData(float[] values, int[] colors, boolean animate) {
        this.values = values;
        this.colors = colors;
        if (animate) {
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(1200);
            a.setInterpolator(new DecelerateInterpolator(2f));
            a.addUpdateListener(an -> {
                progress = (float) an.getAnimatedValue();
                invalidate();
            });
            a.start();
        } else {
            progress = 1f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f + stroke / 2f;
        float top = (getHeight() - size) / 2f + stroke / 2f;
        bounds.set(left, top, left + size - stroke, top + size - stroke);
        canvas.drawArc(bounds, 0, 360, false, trackPaint);

        float total = 0;
        int nonZero = 0;
        for (float v : values) {
            total += v;
            if (v > 0) nonZero++;
        }
        if (total <= 0) return;

        float gap = nonZero > 1 ? 3f : 0f;
        float start = -90f;
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0) continue;
            float sweep = values[i] / total * 360f * progress;
            arcPaint.setColor(colors[i]);
            canvas.drawArc(bounds, start + gap / 2f, Math.max(0f, sweep - gap), false, arcPaint);
            start += sweep;
        }
    }
}
