package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite database that stores every transaction (income and expense).
 * Only one instance exists in the whole app (singleton).
 */
public class TransactionDb extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "money.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE = "transactions";

    private static TransactionDb instance;

    public static synchronized TransactionDb get(Context context) {
        if (instance == null) instance = new TransactionDb(context.getApplicationContext());
        return instance;
    }

    private TransactionDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "type TEXT NOT NULL, "
                + "title TEXT NOT NULL, "
                + "amount REAL NOT NULL, "
                + "category TEXT NOT NULL, "
                + "note TEXT, "
                + "time INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    // ---------- INSERT / DELETE ----------

    public long insert(Transaction t) {
        ContentValues values = new ContentValues();
        values.put("type", t.type);
        values.put("title", t.title);
        values.put("amount", t.amount);
        values.put("category", t.category);
        values.put("note", t.note);
        values.put("time", t.time);
        return getWritableDatabase().insert(TABLE, null, values);
    }

    public void delete(long id) {
        getWritableDatabase().delete(TABLE, "id = ?", new String[]{String.valueOf(id)});
    }

    public void clearAll() {
        getWritableDatabase().delete(TABLE, null, null);
    }

    // ---------- SELECT lists ----------

    public List<Transaction> getAll() {
        return query(null, null, null);
    }

    public List<Transaction> getRecent(int limit) {
        return query(null, null, String.valueOf(limit));
    }

    public List<Transaction> getByType(String type) {
        return query("type = ?", new String[]{type}, null);
    }

    public List<Transaction> getByCategory(String categoryKey) {
        return query("category = ?", new String[]{categoryKey}, null);
    }

    private List<Transaction> query(String where, String[] args, String limit) {
        List<Transaction> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, null, where, args,
                null, null, "time DESC, id DESC", limit)) {
            while (c.moveToNext()) {
                list.add(new Transaction(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("type")),
                        c.getString(c.getColumnIndexOrThrow("title")),
                        c.getDouble(c.getColumnIndexOrThrow("amount")),
                        c.getString(c.getColumnIndexOrThrow("category")),
                        c.getString(c.getColumnIndexOrThrow("note")),
                        c.getLong(c.getColumnIndexOrThrow("time"))));
            }
        }
        return list;
    }

    // ---------- Totals (SUM / COUNT) ----------

    /** Total income or total expense. */
    public double sum(String type) {
        return sumQuery("SELECT SUM(amount) FROM " + TABLE + " WHERE type = ?", type);
    }

    public double sumCategory(String categoryKey) {
        return sumQuery("SELECT SUM(amount) FROM " + TABLE + " WHERE category = ?", categoryKey);
    }

    public double sumBetween(String type, long from, long to) {
        return sumQuery("SELECT SUM(amount) FROM " + TABLE + " WHERE type = ? AND time >= ? AND time < ?",
                type, String.valueOf(from), String.valueOf(to));
    }

    private double sumQuery(String sql, String... args) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getDouble(0) : 0;
        }
    }

    /** Total amount for each category, e.g. {food=1200, travel=450}. */
    public Map<String, Double> sumByCategory(String type) {
        Map<String, Double> map = new HashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT category, SUM(amount) FROM " + TABLE + " WHERE type = ? GROUP BY category",
                new String[]{type})) {
            while (c.moveToNext()) map.put(c.getString(0), c.getDouble(1));
        }
        return map;
    }

    /** Number of transactions in each category. */
    public Map<String, Integer> countByCategory() {
        Map<String, Integer> map = new HashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT category, COUNT(*) FROM " + TABLE + " GROUP BY category", null)) {
            while (c.moveToNext()) map.put(c.getString(0), c.getInt(1));
        }
        return map;
    }

    public int count() {
        return (int) DatabaseUtils.queryNumEntries(getReadableDatabase(), TABLE);
    }
}
