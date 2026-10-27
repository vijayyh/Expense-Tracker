package com.example.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows category "clay" cards with each category's own total.
 * Works with two layouts: a small card (home screen) and a big grid card (categories screen).
 */
public class CategoryCardAdapter extends RecyclerView.Adapter<CategoryCardAdapter.Holder> {

    public interface OnCategoryClick {
        void onClick(Category category);
    }

    private final int layoutRes;
    private final OnCategoryClick listener;
    private List<Category> categories = new ArrayList<>();
    private Map<String, Double> amounts = new HashMap<>();
    private Map<String, Integer> counts = new HashMap<>();
    private double total;

    public CategoryCardAdapter(int layoutRes, OnCategoryClick listener) {
        this.layoutRes = layoutRes;
        this.listener = listener;
    }

    public void setData(List<Category> categories, Map<String, Double> amounts,
                        Map<String, Integer> counts, double total) {
        this.categories = categories;
        this.amounts = amounts;
        this.counts = counts;
        this.total = total;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        Ui.pressEffect(v);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Category c = categories.get(position);
        Double value = amounts.get(c.key);
        double amount = value == null ? 0 : value;

        h.itemView.setBackground(Ui.clay(h.itemView.getContext(), c.color, 24));
        h.emoji.setText(c.emoji);
        h.name.setText(c.label);
        h.amount.setText(Format.money(amount));
        h.itemView.setOnClickListener(v -> listener.onClick(c));

        if (h.percent != null) {
            int pct = total > 0 ? (int) Math.round(amount * 100 / total) : 0;
            h.percent.setText(pct + "%");
            h.progress.setIndicatorColor(c.color);
            h.progress.setProgressCompat(0, false);
            h.progress.postDelayed(() -> h.progress.setProgressCompat(pct, true), 250 + position * 60L);
        }
        if (h.count != null) {
            Integer n = counts.get(c.key);
            int count = n == null ? 0 : n;
            h.count.setText(count + (count == 1 ? " transaction" : " transactions"));
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView emoji, name, amount, percent, count;
        final LinearProgressIndicator progress;

        Holder(@NonNull View v) {
            super(v);
            emoji = v.findViewById(R.id.textEmoji);
            name = v.findViewById(R.id.textName);
            amount = v.findViewById(R.id.textAmount);
            percent = v.findViewById(R.id.textPercent);   // only in the grid layout
            count = v.findViewById(R.id.textCount);       // only in the grid layout
            progress = v.findViewById(R.id.progress);     // only in the grid layout
        }
    }
}
