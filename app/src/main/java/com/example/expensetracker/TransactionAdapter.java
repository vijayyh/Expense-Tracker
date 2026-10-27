package com.example.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a list of transactions. In "grouped" mode it adds a date header
 * (Today / Yesterday / date) above each day's transactions.
 */
public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    /** A date header row with that day's net amount. */
    private static class DayHeader {
        final String label;
        double net;

        DayHeader(String label) {
            this.label = label;
        }
    }

    private final List<Object> rows = new ArrayList<>();
    private boolean grouped;

    public void setTransactions(List<Transaction> list, boolean grouped) {
        this.grouped = grouped;
        rows.clear();
        if (!grouped) {
            rows.addAll(list);
        } else {
            DayHeader header = null;
            for (Transaction t : list) {
                String label = Format.dayLabel(t.time);
                if (header == null || !header.label.equals(label)) {
                    header = new DayHeader(label);
                    rows.add(header);
                }
                header.net += t.isIncome() ? t.amount : -t.amount;
                rows.add(t);
            }
        }
        notifyDataSetChanged();
    }

    /** Returns the transaction at this position, or null if it is a header. */
    public Transaction getTransaction(int position) {
        if (position < 0 || position >= rows.size()) return null;
        Object row = rows.get(position);
        return row instanceof Transaction ? (Transaction) row : null;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof DayHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_date_header, parent, false));
        }
        return new ItemHolder(inflater.inflate(R.layout.item_transaction, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            DayHeader h = (DayHeader) row;
            HeaderHolder hh = (HeaderHolder) holder;
            hh.label.setText(h.label);
            hh.net.setText((h.net >= 0 ? "+ " : "− ") + Format.money(Math.abs(h.net)));
            return;
        }

        Transaction t = (Transaction) row;
        ItemHolder ih = (ItemHolder) holder;
        Category c = Category.find(t.category);
        ih.iconBox.setBackground(Ui.clay(ih.itemView.getContext(), c.color, 16));
        ih.emoji.setText(c.emoji);
        ih.title.setText(t.title);
        String when = grouped ? Format.time(t.time) : Format.shortDate(t.time);
        ih.subtitle.setText(c.label + "  •  " + when);
        ih.amount.setText((t.isIncome() ? "+ " : "− ") + Format.money(t.amount));
        ih.amount.setTextColor(ContextCompat.getColor(ih.itemView.getContext(),
                t.isIncome() ? R.color.income : R.color.expense));
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        final View iconBox;
        final TextView emoji, title, subtitle, amount;

        ItemHolder(@NonNull View v) {
            super(v);
            iconBox = v.findViewById(R.id.iconBox);
            emoji = v.findViewById(R.id.textEmoji);
            title = v.findViewById(R.id.textTitle);
            subtitle = v.findViewById(R.id.textSubtitle);
            amount = v.findViewById(R.id.textAmount);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView label, net;

        HeaderHolder(@NonNull View v) {
            super(v);
            label = v.findViewById(R.id.textDay);
            net = v.findViewById(R.id.textDayNet);
        }
    }
}
