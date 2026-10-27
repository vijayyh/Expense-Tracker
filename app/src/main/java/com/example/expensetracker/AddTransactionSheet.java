package com.example.expensetracker;

import android.app.Dialog;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Bottom sheet form to add money spent (expense) or money received (income).
 */
public class AddTransactionSheet extends BottomSheetDialogFragment {

    /** The screen that opened the sheet is told when something was saved. */
    public interface Listener {
        void onTransactionSaved(Transaction transaction);
    }

    private static final String ARG_TYPE = "type";
    private static final String ARG_CATEGORY = "category";

    private String type = Transaction.EXPENSE;
    private String selectedCategory;
    private long pickedTime = System.currentTimeMillis();

    private SegmentedControl segType;
    private EditText editAmount, editTitle, editNote;
    private TextView textCurrency, textError, textDate, btnSave;
    private ChipGroup chipGroup;
    private View amountBox;

    public static AddTransactionSheet newInstance(@Nullable String type, @Nullable String categoryKey) {
        AddTransactionSheet sheet = new AddTransactionSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        args.putString(ARG_CATEGORY, categoryKey);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            if (args.getString(ARG_TYPE) != null) type = args.getString(ARG_TYPE);
            selectedCategory = args.getString(ARG_CATEGORY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_add_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        segType = view.findViewById(R.id.segType);
        editAmount = view.findViewById(R.id.editAmount);
        editTitle = view.findViewById(R.id.editTitle);
        editNote = view.findViewById(R.id.editNote);
        textCurrency = view.findViewById(R.id.textCurrency);
        textError = view.findViewById(R.id.textError);
        textDate = view.findViewById(R.id.textDate);
        btnSave = view.findViewById(R.id.btnSave);
        chipGroup = view.findViewById(R.id.chipCategories);
        amountBox = view.findViewById(R.id.amountBox);

        segType.setOptions("💸 Expense", "💰 Income");
        segType.setSelected(Transaction.INCOME.equals(type) ? 1 : 0, false, false);
        segType.setOnChangeListener(index -> {
            type = index == 1 ? Transaction.INCOME : Transaction.EXPENSE;
            selectedCategory = null;
            applyType();
        });

        chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip chip = group.findViewById(ids.get(0));
            selectedCategory = (String) chip.getTag();
            chip.setScaleX(0.85f);
            chip.setScaleY(0.85f);
            chip.animate().scaleX(1f).scaleY(1f).setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(3f)).start();
        });

        // Hide the error as soon as the user types an amount
        editAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                textError.setVisibility(View.GONE);
            }
        });

        textDate.setText(Format.pickerLabel(pickedTime));
        view.findViewById(R.id.rowDate).setOnClickListener(v -> showDatePicker());

        Ui.pressEffect(btnSave);
        btnSave.setOnClickListener(v -> save());

        applyType();
        Ui.animateChildren(view.findViewById(R.id.sheetContent));
    }

    @Override
    public void onStart() {
        super.onStart();
        // Open the sheet fully expanded
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetBehavior<?> behavior = ((BottomSheetDialog) dialog).getBehavior();
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    /** Updates colours, button text and category chips for Expense or Income. */
    private void applyType() {
        boolean income = Transaction.INCOME.equals(type);
        segType.setIndicatorDrawable(income ? R.drawable.segmented_indicator_income : R.drawable.segmented_indicator);
        btnSave.setBackgroundResource(income ? R.drawable.btn_gradient_income : R.drawable.btn_gradient);
        btnSave.setText(income ? "Add Income" : "Add Expense");
        textCurrency.setTextColor(ContextCompat.getColor(requireContext(), income ? R.color.income : R.color.primary));
        buildChips();
    }

    private void buildChips() {
        chipGroup.removeAllViews();
        List<Category> categories = Category.forType(type);
        if (selectedCategory == null || !Category.find(selectedCategory).type.equals(type)) {
            selectedCategory = categories.get(0).key;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            Chip chip = (Chip) inflater.inflate(R.layout.item_chip, chipGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(c.emoji + "  " + c.label);
            chip.setTag(c.key);
            if (c.isIncome()) {
                chip.setChipBackgroundColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_bg_income));
            }
            chipGroup.addView(chip);
            if (c.key.equals(selectedCategory)) chip.setChecked(true);
            Ui.popIn(chip, i * 35L);
        }
    }

    private void showDatePicker() {
        // The date picker works with UTC midnight values
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(pickedTime);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("When did it happen?")
                .setSelection(utc.getTimeInMillis())
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now()).build())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar picked = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            picked.setTimeInMillis(selection);
            Calendar result = Calendar.getInstance(); // keep the current time of day
            result.set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH));
            pickedTime = Math.min(result.getTimeInMillis(), System.currentTimeMillis());
            textDate.setText(Format.pickerLabel(pickedTime));
        });
        picker.show(getChildFragmentManager(), "date");
    }

    private void save() {
        double amount;
        try {
            amount = Double.parseDouble(editAmount.getText().toString().trim());
        } catch (NumberFormatException e) {
            amount = 0;
        }
        if (amount <= 0) {
            textError.setVisibility(View.VISIBLE);
            Ui.shake(amountBox);
            amountBox.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return;
        }

        Category category = Category.find(selectedCategory);
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) title = category.label; // title is optional
        String note = editNote.getText().toString().trim();

        Transaction t = new Transaction(0, type, title, amount, category.key,
                note.isEmpty() ? null : note, pickedTime);
        TransactionDb.get(requireContext()).insert(t);
        btnSave.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        if (getActivity() instanceof Listener) ((Listener) getActivity()).onTransactionSaved(t);
        dismiss();
    }
}
