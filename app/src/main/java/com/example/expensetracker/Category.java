package com.example.expensetracker;

import java.util.Arrays;
import java.util.List;

/**
 * A category (division) of money: Food, Travel, Clothes... for expenses,
 * and Pocket Money, Part-time job... for income.
 */
public class Category {
    public final String key;
    public final String label;
    public final String emoji;
    public final int color;
    public final String type;

    public Category(String key, String label, String emoji, int color, String type) {
        this.key = key;
        this.label = label;
        this.emoji = emoji;
        this.color = color;
        this.type = type;
    }

    public boolean isIncome() {
        return Transaction.INCOME.equals(type);
    }

    /** Where money goes. */
    public static final List<Category> EXPENSE = Arrays.asList(
            new Category("food", "Food", "🍔", 0xFFFF8A65, Transaction.EXPENSE),
            new Category("travel", "Travel", "🚌", 0xFF4FC3F7, Transaction.EXPENSE),
            new Category("clothes", "Clothes", "👕", 0xFFBA68C8, Transaction.EXPENSE),
            new Category("groceries", "Groceries", "🛒", 0xFF81C784, Transaction.EXPENSE),
            new Category("education", "Education", "📚", 0xFFFFD54F, Transaction.EXPENSE),
            new Category("health", "Health", "💊", 0xFFF06292, Transaction.EXPENSE),
            new Category("bills", "Bills", "💡", 0xFF4DB6AC, Transaction.EXPENSE),
            new Category("fun", "Fun", "🎬", 0xFF9575CD, Transaction.EXPENSE),
            new Category("other", "Other", "📦", 0xFF90A4AE, Transaction.EXPENSE)
    );

    /** Where money comes from. */
    public static final List<Category> INCOME = Arrays.asList(
            new Category("pocket", "Pocket Money", "💵", 0xFF66BB6A, Transaction.INCOME),
            new Category("parttime", "Part-time Job", "💼", 0xFF26A69A, Transaction.INCOME),
            new Category("scholarship", "Scholarship", "🎓", 0xFF5C6BC0, Transaction.INCOME),
            new Category("gift", "Gift", "🎁", 0xFFEC407A, Transaction.INCOME),
            new Category("other_income", "Other", "💰", 0xFF8D6E63, Transaction.INCOME)
    );

    public static List<Category> forType(String type) {
        return Transaction.INCOME.equals(type) ? INCOME : EXPENSE;
    }

    public static Category find(String key) {
        for (Category c : EXPENSE) if (c.key.equals(key)) return c;
        for (Category c : INCOME) if (c.key.equals(key)) return c;
        return EXPENSE.get(EXPENSE.size() - 1);
    }
}
