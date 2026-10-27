package com.example.expensetracker;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Runs once when the app starts: applies the saved light/dark theme.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Prefs.themeMode(this));
    }
}
