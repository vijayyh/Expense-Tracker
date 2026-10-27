package com.example.expensetracker;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Helpers to turn numbers and dates into nice text.
 */
public final class Format {

    // Indian number grouping: 1,23,456.50
    private static final NumberFormat NUMBER = NumberFormat.getNumberInstance(new Locale("en", "IN"));

    static {
        NUMBER.setMinimumFractionDigits(0);
        NUMBER.setMaximumFractionDigits(2);
    }

    private Format() {}

    public static String money(double value) {
        String text = "₹" + NUMBER.format(Math.abs(value));
        return value < 0 ? "-" + text : text;
    }

    /** "Today", "Yesterday" or "Mon, 20 Oct 2026". */
    public static String dayLabel(long time) {
        long today = startOfDay(System.currentTimeMillis());
        long day = startOfDay(time);
        if (day == today) return "Today";
        if (day == today - 24L * 60 * 60 * 1000) return "Yesterday";
        return new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(new Date(time));
    }

    public static String pickerLabel(long time) {
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(time));
        String day = dayLabel(time);
        return day.equals("Today") || day.equals("Yesterday") ? day + ", " + date : day;
    }

    public static String time(long time) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(time));
    }

    public static String shortDate(long time) {
        return new SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(new Date(time));
    }

    public static long startOfDay(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long startOfMonth() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(System.currentTimeMillis()));
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning,";
        if (hour < 17) return "Good afternoon,";
        return "Good evening,";
    }
}
