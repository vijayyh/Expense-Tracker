package com.example.expensetracker;

/** A screen that can reload its data from the database. */
public interface Refreshable {
    void refresh(boolean animate);
}
