package com.example.expensetracker;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows one category (e.g. Food): its total, stats and all its transactions.
 */
public class CategoryDetailActivity extends AppCompatActivity implements AddTransactionSheet.Listener {

    private static final String EXTRA_KEY = "category_key";

    private final List<Animator> loops = new ArrayList<>();
    private final TransactionAdapter adapter = new TransactionAdapter();
    private Category category;
    private TextView textTotal, textStats;
    private RecyclerView recycler;
    private View emptyView, btnAdd;

    /** Opens this screen with a slide-in animation. */
    public static void open(Activity from, String categoryKey) {
        from.startActivity(new Intent(from, CategoryDetailActivity.class).putExtra(EXTRA_KEY, categoryKey));
        from.overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);
        Ui.applySystemBars(this);
        loops.addAll(Ui.startBlobAnimation(findViewById(R.id.background)));

        category = Category.find(getIntent().getStringExtra(EXTRA_KEY));

        View header = findViewById(R.id.headerCard);
        header.setBackground(Ui.clay(this, category.color, 32));
        ((TextView) findViewById(R.id.textToolbar)).setText(category.isIncome() ? "Income source" : "Category");
        ((TextView) findViewById(R.id.textEmoji)).setText(category.emoji);
        ((TextView) findViewById(R.id.textName)).setText(category.label);
        ((TextView) findViewById(R.id.textTotalLabel)).setText(category.isIncome() ? "Total received" : "Total spent");
        textTotal = findViewById(R.id.textTotal);
        textStats = findViewById(R.id.textStats);
        emptyView = findViewById(R.id.emptyDetail);

        View back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());
        Ui.pressEffect(back);

        btnAdd = findViewById(R.id.btnAdd);
        ((TextView) btnAdd).setText("＋  Add to " + category.label);
        btnAdd.setBackgroundResource(category.isIncome() ? R.drawable.btn_gradient_income : R.drawable.btn_gradient);
        btnAdd.setOnClickListener(v -> AddTransactionSheet.newInstance(category.type, category.key)
                .show(getSupportFragmentManager(), "add"));
        Ui.pressEffect(btnAdd);

        recycler = findViewById(R.id.recyclerDetail);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        new ItemTouchHelper(new SwipeDeleteCallback(this, adapter,
                t -> Ui.deleteWithUndo(this, btnAdd, t, () -> refresh(false))))
                .attachToRecyclerView(recycler);

        // Entrance animations
        header.setScaleX(0.9f);
        header.setScaleY(0.9f);
        header.setAlpha(0f);
        header.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(550)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();
        Ui.popIn(findViewById(R.id.emojiBox), 250);
        btnAdd.setTranslationY(Ui.dp(this, 120));
        btnAdd.animate().translationY(0).setStartDelay(300).setDuration(600)
                .setInterpolator(new OvershootInterpolator(1.3f)).start();

        refresh(true);
    }

    private void refresh(boolean animate) {
        List<Transaction> list = TransactionDb.get(this).getByCategory(category.key);
        double total = 0;
        for (Transaction t : list) total += t.amount;
        Ui.countUp(textTotal, total, animate);
        String count = list.size() + (list.size() == 1 ? " transaction" : " transactions");
        textStats.setText(list.isEmpty() ? count : count + "  •  Avg " + Format.money(total / list.size()));

        adapter.setTransactions(list, true);
        emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        if (animate) recycler.scheduleLayoutAnimation();
    }

    @Override
    public void onTransactionSaved(Transaction t) {
        refresh(false);
        recycler.scheduleLayoutAnimation();
        Ui.toast(this, btnAdd, "Added " + Format.money(t.amount) + " to " + category.label);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.hold, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        for (Animator a : loops) a.cancel();
        super.onDestroy();
    }
}
