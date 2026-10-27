package com.example.expensetracker;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;

/**
 * The main screen. It holds the 4 tabs (Home, Categories, History, Profile),
 * the floating bottom bar and the centre "+" button.
 */
public class MainActivity extends AppCompatActivity implements AddTransactionSheet.Listener {

    public static final int TAB_HOME = 0, TAB_CATEGORIES = 1, TAB_HISTORY = 2, TAB_PROFILE = 3;
    private static final String[] TAGS = {"home", "categories", "history", "profile"};

    private final Fragment[] fragments = new Fragment[4];
    private final List<Animator> loops = new ArrayList<>();
    private int current = TAB_HOME;
    private boolean firstResume = true;

    private ImageView[] navIcons;
    private TextView[] navLabels;
    private View bottomBar, fabIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Ui.applySystemBars(this);
        loops.addAll(Ui.startBlobAnimation(findViewById(R.id.background)));

        setUpFragments(savedInstanceState);
        setUpBottomBar();

        // Back button: go to Home first, then exit
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (current != TAB_HOME) {
                    selectTab(TAB_HOME);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setUpFragments(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            fragments[TAB_HOME] = new HomeFragment();
            fragments[TAB_CATEGORIES] = new CategoriesFragment();
            fragments[TAB_HISTORY] = new HistoryFragment();
            fragments[TAB_PROFILE] = new ProfileFragment();
            FragmentTransaction tx = fm.beginTransaction();
            for (int i = 0; i < fragments.length; i++) {
                tx.add(R.id.fragmentContainer, fragments[i], TAGS[i]);
                if (i != TAB_HOME) tx.hide(fragments[i]);
            }
            tx.commitNow();
        } else {
            // Screen was recreated (e.g. theme changed): find the existing fragments
            for (int i = 0; i < fragments.length; i++) fragments[i] = fm.findFragmentByTag(TAGS[i]);
            current = savedInstanceState.getInt("tab", TAB_HOME);
        }
    }

    private void setUpBottomBar() {
        bottomBar = findViewById(R.id.bottomBar);
        View[] items = {findViewById(R.id.navHome), findViewById(R.id.navCategories),
                findViewById(R.id.navHistory), findViewById(R.id.navProfile)};
        navIcons = new ImageView[]{findViewById(R.id.navHomeIcon), findViewById(R.id.navCategoriesIcon),
                findViewById(R.id.navHistoryIcon), findViewById(R.id.navProfileIcon)};
        navLabels = new TextView[]{findViewById(R.id.navHomeLabel), findViewById(R.id.navCategoriesLabel),
                findViewById(R.id.navHistoryLabel), findViewById(R.id.navProfileLabel)};
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            items[i].setOnClickListener(v -> selectTab(index));
        }
        updateNav(false);

        View fab = findViewById(R.id.fabAdd);
        fabIcon = findViewById(R.id.fabIcon);
        fab.setOnClickListener(v -> openAddSheet(null, null));
        Ui.pressEffect(fab);

        // Pulsing ring around the + button
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(findViewById(R.id.fabPulse),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.45f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.45f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 0f));
        pulse.setDuration(1800);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();
        loops.add(pulse);

        // Bottom bar slides up when the app opens
        bottomBar.setTranslationY(Ui.dp(this, 160));
        bottomBar.animate().translationY(0).setStartDelay(250).setDuration(700)
                .setInterpolator(new OvershootInterpolator(1.2f)).start();
    }

    /** Switches to another tab with a fade animation. */
    public void selectTab(int index) {
        if (index == current) return;
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.frag_enter, R.anim.frag_exit)
                .hide(fragments[current])
                .show(fragments[index])
                .commit();
        current = index;
        updateNav(true);
    }

    private void updateNav(boolean animate) {
        int active = ContextCompat.getColor(this, R.color.primary);
        int inactive = ContextCompat.getColor(this, R.color.nav_inactive);
        for (int i = 0; i < navIcons.length; i++) {
            boolean selected = i == current;
            ImageViewCompat.setImageTintList(navIcons[i], ColorStateList.valueOf(selected ? active : inactive));
            navIcons[i].setBackgroundResource(selected ? R.drawable.nav_selected_bg : 0);
            navLabels[i].setTextColor(selected ? active : inactive);
            navLabels[i].setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            if (selected && animate) {
                navIcons[i].setScaleX(0.6f);
                navIcons[i].setScaleY(0.6f);
                navIcons[i].animate().scaleX(1f).scaleY(1f).setDuration(450)
                        .setInterpolator(new OvershootInterpolator(3f)).start();
            }
        }
    }

    public void openAddSheet(String type, String categoryKey) {
        fabIcon.animate().rotationBy(180f).setDuration(500)
                .setInterpolator(new OvershootInterpolator(2f)).start();
        AddTransactionSheet.newInstance(type, categoryKey).show(getSupportFragmentManager(), "add");
    }

    public View getBottomBar() {
        return bottomBar;
    }

    /** Called after any data change: every tab reloads (numbers animate to their new values). */
    public void onDataChanged() {
        refreshAll(false);
    }

    /** @param entrance true = also replay the visible tab's entrance animation */
    private void refreshAll(boolean entrance) {
        for (int i = 0; i < fragments.length; i++) {
            if (fragments[i] instanceof Refreshable && fragments[i].isAdded()) {
                ((Refreshable) fragments[i]).refresh(entrance && i == current);
            }
        }
    }

    @Override
    public void onTransactionSaved(Transaction t) {
        onDataChanged();
        Ui.toast(this, bottomBar, (t.isIncome() ? "💰 Income added: " : "✅ Expense added: ") + Format.money(t.amount));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll(firstResume); // full entrance animation only when the screen first opens
        firstResume = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", current);
    }

    @Override
    protected void onDestroy() {
        for (Animator a : loops) a.cancel();
        super.onDestroy();
    }
}
