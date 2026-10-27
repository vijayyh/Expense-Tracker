package com.example.expensetracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * History tab: every transaction grouped by day, with an All / Income / Expense filter.
 * Swipe a row to delete it (with UNDO).
 */
public class HistoryFragment extends Fragment implements Refreshable {

    private static final int FILTER_ALL = 0, FILTER_INCOME = 1, FILTER_EXPENSE = 2;

    private int filter = FILTER_ALL;
    private TextView textIn, textOut;
    private RecyclerView recycler;
    private View emptyView;
    private ViewGroup content;
    private final TransactionAdapter adapter = new TransactionAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        content = view.findViewById(R.id.content);
        textIn = view.findViewById(R.id.textIn);
        textOut = view.findViewById(R.id.textOut);
        emptyView = view.findViewById(R.id.emptyHistory);

        SegmentedControl seg = view.findViewById(R.id.segFilter);
        seg.setOptions("All", "Income", "Expense");
        seg.setOnChangeListener(index -> {
            filter = index;
            refresh(false);
            recycler.scheduleLayoutAnimation();
        });

        recycler = view.findViewById(R.id.recyclerHistory);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        MainActivity main = (MainActivity) requireActivity();
        new ItemTouchHelper(new SwipeDeleteCallback(requireContext(), adapter,
                t -> Ui.deleteWithUndo(main, main.getBottomBar(), t, main::onDataChanged)))
                .attachToRecyclerView(recycler);
    }

    @Override
    public void refresh(boolean animate) {
        if (getView() == null) return;
        TransactionDb db = TransactionDb.get(requireContext());
        List<Transaction> list;
        if (filter == FILTER_INCOME) list = db.getByType(Transaction.INCOME);
        else if (filter == FILTER_EXPENSE) list = db.getByType(Transaction.EXPENSE);
        else list = db.getAll();

        double in = 0, out = 0;
        for (Transaction t : list) {
            if (t.isIncome()) in += t.amount;
            else out += t.amount;
        }
        Ui.countUp(textIn, in, animate);
        Ui.countUp(textOut, out, animate);

        adapter.setTransactions(list, true);
        emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

        if (animate) {
            Ui.animateChildren(content);
            recycler.scheduleLayoutAnimation();
        }
        if (list.isEmpty() && animate) Ui.popIn(emptyView.findViewById(R.id.emptyEmoji), 300);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refresh(true);
    }
}
