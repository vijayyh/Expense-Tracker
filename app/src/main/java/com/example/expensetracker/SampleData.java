package com.example.expensetracker;

import android.content.Context;

import java.util.Calendar;

/**
 * Fills the app with demo transactions, handy for showing the project.
 */
public final class SampleData {

    private SampleData() {}

    // { days ago, hour, type, category, title, amount }
    private static final Object[][] ROWS = {
            {20, 10, Transaction.INCOME, "pocket", "Monthly pocket money", 6000.0},
            {18, 13, Transaction.EXPENSE, "food", "Canteen lunch", 120.0},
            {17, 9, Transaction.EXPENSE, "travel", "Bus pass", 450.0},
            {16, 18, Transaction.EXPENSE, "clothes", "New T-shirt", 799.0},
            {15, 11, Transaction.EXPENSE, "education", "Notebooks & pens", 260.0},
            {14, 20, Transaction.EXPENSE, "fun", "Movie night", 350.0},
            {12, 17, Transaction.INCOME, "parttime", "Tutoring payment", 2500.0},
            {12, 19, Transaction.EXPENSE, "groceries", "Fruits & snacks", 340.0},
            {10, 8, Transaction.EXPENSE, "food", "Breakfast", 80.0},
            {9, 15, Transaction.EXPENSE, "bills", "Mobile recharge", 299.0},
            {8, 12, Transaction.EXPENSE, "health", "Medicines", 180.0},
            {7, 14, Transaction.INCOME, "scholarship", "Merit scholarship", 3000.0},
            {6, 21, Transaction.EXPENSE, "food", "Pizza with friends", 420.0},
            {5, 10, Transaction.EXPENSE, "travel", "Auto rickshaw", 90.0},
            {4, 16, Transaction.EXPENSE, "education", "Online course", 499.0},
            {3, 13, Transaction.EXPENSE, "food", "Canteen lunch", 110.0},
            {2, 18, Transaction.EXPENSE, "clothes", "Sneakers", 1499.0},
            {2, 20, Transaction.INCOME, "gift", "Birthday gift", 1000.0},
            {1, 9, Transaction.EXPENSE, "travel", "Metro card top-up", 200.0},
            {1, 19, Transaction.EXPENSE, "groceries", "Milk & bread", 95.0},
            {0, 9, Transaction.EXPENSE, "food", "Coffee", 60.0},
            {0, 13, Transaction.EXPENSE, "other", "Photocopies", 40.0},
    };

    public static void load(Context context) {
        TransactionDb db = TransactionDb.get(context);
        for (Object[] row : ROWS) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -(int) row[0]);
            c.set(Calendar.HOUR_OF_DAY, (int) row[1]);
            c.set(Calendar.MINUTE, (int) (Math.random() * 59));
            long time = Math.min(c.getTimeInMillis(), System.currentTimeMillis());
            db.insert(new Transaction(0, (String) row[2], (String) row[4], (double) row[5],
                    (String) row[3], null, time));
        }
    }
}
