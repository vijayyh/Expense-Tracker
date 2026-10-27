package com.example.expensetracker;

/**
 * Model class: ONE money movement, either an expense (money spent)
 * or an income (money received).
 */
public class Transaction {
    public static final String EXPENSE = "expense";
    public static final String INCOME = "income";

    public final long id;
    public final String type;      // EXPENSE or INCOME
    public final String title;
    public final double amount;
    public final String category;  // key of a Category, e.g. "food"
    public final String note;
    public final long time;        // when it happened, in milliseconds

    public Transaction(long id, String type, String title, double amount,
                       String category, String note, long time) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.time = time;
    }

    public boolean isIncome() {
        return INCOME.equals(type);
    }
}
