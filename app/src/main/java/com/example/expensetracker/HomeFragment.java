package com.example.expensetracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Home tab: greeting, total balance (income − expenses), category totals
 * and the 5 most recent transactions.
 */
public class HomeFragment extends Fragment implements Refreshable {

    private TextView textGreeting, textName, textThemeIcon, textBalance, textIncome, textExpense;
    private RecyclerView recyclerCategories, recyclerRecent;
    private View emptyRecent;
    private ViewGroup content;
    private CategoryCardAdapter categoryAdapter;
    private final TransactionAdapter recentAdapter = new TransactionAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        content = view.findViewById(R.id.content);
        textGreeting = view.findViewById(R.id.textGreeting);
        textName = view.findViewById(R.id.textName);
        textThemeIcon = view.findViewById(R.id.textThemeIcon);
        textBalance = view.findViewById(R.id.textBalance);
        textIncome = view.findViewById(R.id.textIncome);
        textExpense = view.findViewById(R.id.textExpense);
        emptyRecent = view.findViewById(R.id.emptyRecent);

        View balanceCard = view.findViewById(R.id.balanceCard);
        balanceCard.setClipToOutline(true);
        Ui.pressEffect(balanceCard);

        recyclerCategories = view.findViewById(R.id.recyclerCategories);
        recyclerCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryCardAdapter(R.layout.item_category_small,
                c -> CategoryDetailActivity.open(requireActivity(), c.key));
        recyclerCategories.setAdapter(categoryAdapter);

        recyclerRecent = view.findViewById(R.id.recyclerRecent);
        recyclerRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRecent.setAdapter(recentAdapter);

        MainActivity main = (MainActivity) requireActivity();
        view.findViewById(R.id.seeAllCategories).setOnClickListener(v -> main.selectTab(MainActivity.TAB_CATEGORIES));
        view.findViewById(R.id.seeAllHistory).setOnClickListener(v -> main.selectTab(MainActivity.TAB_HISTORY));

        // Quick light/dark switch with a spin animation
        boolean night = Ui.isNight(requireContext());
        textThemeIcon.setText(night ? "☀️" : "🌙");
        View btnTheme = view.findViewById(R.id.btnTheme);
        Ui.pressEffect(btnTheme);
        btnTheme.setOnClickListener(v -> textThemeIcon.animate().rotationBy(360f).scaleX(0.3f).scaleY(0.3f)
                .setDuration(380).setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> Prefs.setThemeMode(requireContext(),
                        night ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES))
                .start());
    }

    @Override
    public void refresh(boolean animate) {
        if (getView() == null) return;
        TransactionDb db = TransactionDb.get(requireContext());
        double income = db.sum(Transaction.INCOME);
        double expense = db.sum(Transaction.EXPENSE);

        textGreeting.setText(Format.greeting());
        textName.setText(Prefs.name(requireContext()) + " 👋");
        Ui.countUp(textBalance, income - expense, animate);
        Ui.countUp(textIncome, income, animate);
        Ui.countUp(textExpense, expense, animate);

        categoryAdapter.setData(Category.EXPENSE, db.sumByCategory(Transaction.EXPENSE),
                db.countByCategory(), expense);

        List<Transaction> recent = db.getRecent(5);
        recentAdapter.setTransactions(recent, false);
        emptyRecent.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);

        if (animate) {
            Ui.animateChildren(content);
            recyclerCategories.scheduleLayoutAnimation();
            recyclerRecent.scheduleLayoutAnimation();
            if (recent.isEmpty()) Ui.popIn(emptyRecent.findViewById(R.id.emptyEmoji), 400);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refresh(true);
    }
}
