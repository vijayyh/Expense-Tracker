package com.example.expensetracker;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Small settings saved with SharedPreferences: theme and user name.
 */
public final class Prefs {

    private Prefs() {}

    private static SharedPreferences get(Context context) {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public static int themeMode(Context context) {
        return get(context).getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /** Saves the theme and applies it (the screen is recreated automatically). */
    public static void setThemeMode(Context context, int mode) {
        get(context).edit().putInt("theme", mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static String name(Context context) {
        return get(context).getString("name", "Student");
    }

    public static void setName(Context context, String name) {
        get(context).edit().putString("name", name).apply();
    }
}
