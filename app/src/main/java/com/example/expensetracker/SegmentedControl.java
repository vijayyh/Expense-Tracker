package com.example.expensetracker;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom toggle (e.g. Expense | Income) with a gradient pill that slides
 * to the selected option.
 */
public class SegmentedControl extends FrameLayout {

    public interface OnChangeListener {
        void onChange(int index);
    }

    private final View indicator;
    private final LinearLayout row;
    private final List<TextView> labels = new ArrayList<>();
    private int selected = 0;
    private int segmentWidth = 0;
    private OnChangeListener listener;

    public SegmentedControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(R.drawable.segmented_bg);
        int pad = Ui.dp(context, 4);
        setPadding(pad, pad, pad, pad);

        indicator = new View(context);
        indicator.setBackgroundResource(R.drawable.segmented_indicator);
        addView(indicator, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT));

        row = new LinearLayout(context);
        addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void setOptions(String... options) {
        row.removeAllViews();
        labels.clear();
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            TextView tv = new TextView(getContext());
            tv.setText(options[i]);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(14);
            tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            tv.setOnClickListener(v -> setSelected(index, true, true));
            row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            labels.add(tv);
        }
        updateColors();
    }

    public void setOnChangeListener(OnChangeListener listener) {
        this.listener = listener;
    }

    public void setIndicatorDrawable(int drawableRes) {
        indicator.setBackgroundResource(drawableRes);
    }

    public void setSelected(int index, boolean animate, boolean notify) {
        if (index == selected && notify) return;
        selected = index;
        moveIndicator(animate);
        updateColors();
        if (notify && listener != null) listener.onChange(index);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (labels.isEmpty()) return;
        segmentWidth = (w - getPaddingLeft() - getPaddingRight()) / labels.size();
        post(() -> {
            LayoutParams lp = (LayoutParams) indicator.getLayoutParams();
            lp.width = segmentWidth;
            indicator.setLayoutParams(lp);
            moveIndicator(false);
        });
    }

    private void moveIndicator(boolean animate) {
        float x = selected * segmentWidth;
        if (animate) {
            indicator.animate().translationX(x).setDuration(420)
                    .setInterpolator(new OvershootInterpolator(1.1f)).start();
        } else {
            indicator.setTranslationX(x);
        }
    }

    private void updateColors() {
        int on = ContextCompat.getColor(getContext(), R.color.white);
        int off = ContextCompat.getColor(getContext(), R.color.text_secondary);
        for (int i = 0; i < labels.size(); i++) labels.get(i).setTextColor(i == selected ? on : off);
    }
}
