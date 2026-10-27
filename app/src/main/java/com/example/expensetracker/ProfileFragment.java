package com.example.expensetracker;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;
import java.util.Map;

/**
 * Profile tab: name, light/dark theme, this month's stats and data tools.
 */
public class ProfileFragment extends Fragment implements Refreshable {

    private TextView textAvatar, textProfileName, statMonth, statAvg, statTop, statTopEmoji, statCount;
    private ViewGroup content;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        content = view.findViewById(R.id.content);
        textAvatar = view.findViewById(R.id.textAvatar);
        textProfileName = view.findViewById(R.id.textProfileName);
        statMonth = view.findViewById(R.id.statMonth);
        statAvg = view.findViewById(R.id.statAvg);
        statTop = view.findViewById(R.id.statTop);
        statTopEmoji = view.findViewById(R.id.statTopEmoji);
        statCount = view.findViewById(R.id.statCount);

        textAvatar.setBackground(Ui.clay(requireContext(), 0xFF7C5CFF, 48));
        view.findViewById(R.id.tileMonth).setBackground(Ui.clay(requireContext(), 0xFFA78BFA, 24));
        view.findViewById(R.id.tileAvg).setBackground(Ui.clay(requireContext(), 0xFF60A5FA, 24));
        view.findViewById(R.id.tileTop).setBackground(Ui.clay(requireContext(), 0xFFF472B6, 24));
        view.findViewById(R.id.tileCount).setBackground(Ui.clay(requireContext(), 0xFF34D399, 24));
        Ui.pressEffect(textAvatar, view.findViewById(R.id.tileMonth), view.findViewById(R.id.tileAvg),
                view.findViewById(R.id.tileTop), view.findViewById(R.id.tileCount));

        // Theme: Light / Dark / System
        SegmentedControl segTheme = view.findViewById(R.id.segTheme);
        segTheme.setOptions("☀️ Light", "🌙 Dark", "📱 System");
        int mode = Prefs.themeMode(requireContext());
        segTheme.setSelected(mode == AppCompatDelegate.MODE_NIGHT_NO ? 0
                : mode == AppCompatDelegate.MODE_NIGHT_YES ? 1 : 2, false, false);
        segTheme.setOnChangeListener(index -> {
            int newMode = index == 0 ? AppCompatDelegate.MODE_NIGHT_NO
                    : index == 1 ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            // small delay so the sliding animation is visible before the theme changes
            segTheme.postDelayed(() -> Prefs.setThemeMode(requireContext(), newMode), 350);
        });

        view.findViewById(R.id.btnEditName).setOnClickListener(v -> editName());
        textAvatar.setOnClickListener(v -> editName());

        MainActivity main = (MainActivity) requireActivity();
        view.findViewById(R.id.rowSample).setOnClickListener(v -> {
            SampleData.load(requireContext());
            main.onDataChanged();
            Ui.toast(main, main.getBottomBar(), "🧪 Sample data added — check Home!");
        });
        view.findViewById(R.id.rowClear).setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear all data?")
                .setMessage("This deletes every income and expense. It can't be undone.")
                .setPositiveButton("Delete all", (d, w) -> {
                    TransactionDb.get(requireContext()).clearAll();
                    main.onDataChanged();
                    Ui.toast(main, main.getBottomBar(), "All data cleared");
                })
                .setNegativeButton("Cancel", null)
                .show());
    }

    private void editName() {
        EditText input = new EditText(requireContext());
        input.setText(Prefs.name(requireContext()));
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setSelectAllOnFocus(true);
        FrameLayout box = new FrameLayout(requireContext());
        int pad = Ui.dp(requireContext(), 22);
        box.setPadding(pad, Ui.dp(requireContext(), 8), pad, 0);
        box.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Your name")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Prefs.setName(requireContext(), name);
                        ((MainActivity) requireActivity()).onDataChanged();
                        Ui.popIn(textAvatar, 0);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void refresh(boolean animate) {
        if (getView() == null) return;
        String name = Prefs.name(requireContext());
        textProfileName.setText(name);
        textAvatar.setText(name.substring(0, 1).toUpperCase());

        TransactionDb db = TransactionDb.get(requireContext());
        double month = db.sumBetween(Transaction.EXPENSE, Format.startOfMonth(), Long.MAX_VALUE);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        Ui.countUp(statMonth, month, animate);
        Ui.countUp(statAvg, month / day, animate);
        statCount.setText(String.valueOf(db.count()));

        // Category with the highest spending
        String topKey = null;
        double topValue = 0;
        for (Map.Entry<String, Double> e : db.sumByCategory(Transaction.EXPENSE).entrySet()) {
            if (e.getValue() > topValue) {
                topValue = e.getValue();
                topKey = e.getKey();
            }
        }
        if (topKey == null) {
            statTop.setText("—");
            statTopEmoji.setText("🏆");
        } else {
            Category top = Category.find(topKey);
            statTop.setText(top.label);
            statTopEmoji.setText(top.emoji);
        }

        if (animate) {
            Ui.animateChildren(content);
            Ui.popIn(textAvatar, 150);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) refresh(true);
    }
}
