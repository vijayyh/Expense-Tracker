package com.example.expensetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Lets the user swipe a transaction left or right to delete it.
 * Draws a red "Delete" background behind the row while swiping.
 */
public class SwipeDeleteCallback extends ItemTouchHelper.SimpleCallback {

    public interface OnSwiped {
        void onSwiped(Transaction transaction);
    }

    private final TransactionAdapter adapter;
    private final OnSwiped listener;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float radius;
    private final float margin;

    public SwipeDeleteCallback(Context context, TransactionAdapter adapter, OnSwiped listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.listener = listener;
        bgPaint.setColor(ContextCompat.getColor(context, R.color.expense));
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(Ui.dp(context, 15));
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        radius = Ui.dp(context, 22);
        margin = Ui.dp(context, 24);
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
        int pos = vh.getBindingAdapterPosition();
        if (pos == RecyclerView.NO_POSITION || adapter.getTransaction(pos) == null) return 0; // headers can't be swiped
        return super.getSwipeDirs(rv, vh);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
        Transaction t = adapter.getTransaction(vh.getBindingAdapterPosition());
        if (t != null) listener.onSwiped(t);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0) {
            View item = vh.itemView;
            float progress = Math.min(1f, Math.abs(dX) / (item.getWidth() * 0.35f));
            bgPaint.setAlpha((int) (255 * progress));
            rect.set(item.getLeft(), item.getTop(), item.getRight(), item.getBottom());
            c.drawRoundRect(rect, radius, radius, bgPaint);

            String label = "🗑  Delete";
            float textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f;
            float textX = dX > 0 ? rect.left + margin : rect.right - margin - textPaint.measureText(label);
            textPaint.setAlpha((int) (255 * progress));
            c.drawText(label, textX, textY, textPaint);
        }
        super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
    }
}
