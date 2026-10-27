package com.example.expensetracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

/**
 * Categories tab: a ring chart of spending, a card for every expense category
 * (each with its own total) and the income sources.
 */
public class CategoriesFragment extends Fragment implements Refreshable {

    private DonutChartView donut;
    private TextView textDonutTotal;
    private RecyclerView recyclerGrid, recyclerIncome;
    private ViewGroup content;
    private CategoryCardAdapter gridAdapter, incomeAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        content = view.findViewById(R.id.content);
        donut = view.findViewById(R.id.donut);
        textDonutTotal = view.findViewById(R.id.textDonutTotal);

        CategoryCardAdapter.OnCategoryClick open = c -> CategoryDetailActivity.open(requireActivity(), c.key);

        recyclerGrid = view.findViewById(R.id.recyclerGrid);
        recyclerGrid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        gridAdapter = new CategoryCardAdapter(R.layout.item_category_grid, open);
        recyclerGrid.setAdapter(gridAdapter);

        recyclerIncome = view.findViewById(R.id.recyclerIncome);
        recyclerIncome.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        incomeAdapter = new CategoryCardAdapter(R.layout.item_category_small, open);
        recyclerIncome.setAdapter(incomeAdapter);
    }

    @Override
    public void refresh(boolean animate) {
        if (getView() == null) return;
        TransactionDb db = TransactionDb.get(requireContext());
        Map<String, Double> spent = db.sumByCategory(Transaction.EXPENSE);
        Map<String, Integer> counts = db.countByCategory();
        double totalSpent = db.sum(Transaction.EXPENSE);

        // Ring chart data: one slice per expense category
        List<Category> cats = Category.EXPENSE;
        float[] values = new float[cats.size()];
        int[] colors = new int[cats.size()];
        for (int i = 0; i < cats.size(); i++) {
            Double v = spent.get(cats.get(i).key);
            values[i] = v == null ? 0f : v.floatValue();
            colors[i] = cats.get(i).color;
        }
        donut.setData(values, colors, animate);
        Ui.countUp(textDonutTotal, totalSpent, animate);

        gridAdapter.setData(cats, spent, counts, totalSpent);
        incomeAdapter.setData(Category.INCOME, db.sumByCategory(Transaction.INCOME),
                counts, db.sum(Transaction.INCOME));

        if (animate) {
            Ui.animateChildren(content);
            recyclerGrid.scheduleLayoutAnimation();
            recyclerIncome.scheduleLayoutAnimation();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refresh(true);
    }
}
