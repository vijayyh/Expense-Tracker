# Student Expense Tracker (Android mini project, v2)

A modern money-tracking app for students. It records **money received (income)** and
**money spent (expenses)**, splits spending into **categories** with their own totals,
and keeps a full **history**. The UI uses **glassmorphism + claymorphism**, smooth
animations, and **light & dark themes**.

Built with **Java**, **XML layouts**, **SQLite** and **Material Design 3** in **Android Studio**.

- **Android:** the Android Studio project in this repo (or the APK).
- **iPhone (and any phone):** web version, live at **https://expense-tracker-alpha-sandy-47.vercel.app**

---

## Use it on iPhone

1. Open **https://expense-tracker-alpha-sandy-47.vercel.app** in **Safari**.
2. Tap **Share** (square with an arrow) → **Add to Home Screen** → **Add**.
3. Open **Expenses** from the home screen. It runs full-screen like an app, works offline, and saves data on the phone.

On Android, the same link works too: in Chrome, tap **⋮ → Install app**.

The web version lives in [`web/`](web) (HTML, CSS, JavaScript, no build step). It has the same screens and features as the
Android app and stores data in the browser's `localStorage`. Vercel publishes it from the repo root using
[`vercel.json`](vercel.json), and every push to `main` redeploys it automatically.

---

## Features

| Screen | What it shows |
|---|---|
| **Home** | Greeting, **Total Balance** (income − expenses) with animated counting numbers, total **Income** and **Expenses**, a scrollable strip of category cards, and the 5 most recent transactions |
| **Categories** | Animated **ring (donut) chart** of spending, a card for **each category** (Food, Travel, Clothes, Groceries, Education, Health, Bills, Fun, Other) with its own total, % share, number of transactions and a progress bar, plus **income sources** |
| **Category detail** | Tap any category: big clay header with its total, average and count, plus all its transactions. An "Add to Food" button adds directly to that category |
| **History** | Every transaction grouped by day (Today / Yesterday / date) with each day's net. Filter **All / Income / Expense**, see Money in and Money out. **Swipe to delete with UNDO** |
| **Profile** | Edit your name, choose **Light / Dark / System** theme, this month's stats (spent, average per day, top category, count), load sample data, clear all data |
| **Centre + button** | Glowing clay button with a pulsing ring. It opens a bottom sheet to add an **Expense or Income**: amount, title, category chips, date picker and note, with validation |

### Design and animation
- **Glassmorphism**: see-through frosted cards with bright thin borders, floating over soft colour blobs.
- **Claymorphism**: puffy, rounded category cards and buttons with a soft shadow underneath and an inner highlight (see `Ui.clay()`).
- **Motion**: floating blurred background blobs, numbers that count up, the ring chart drawing itself, cards sliding in one after another, squishy press effects, a sliding toggle indicator, a bouncing nav icon, a pulsing + button, and slide transitions between screens.
- **Light and dark themes**: every colour has a light version (`res/values/colors.xml`) and a dark version (`res/values-night/colors.xml`). Android picks the right one automatically.

---

## How to run in Android Studio

1. Open **Android Studio**, click **File → Open**, and select the **`ExpenseTracker`** folder (the one with `settings.gradle` inside).
2. Wait for **Gradle Sync** to finish (bottom progress bar). The first time can take a few minutes and needs internet.
   - If asked *"Trust Gradle project?"*, click **Trust Project**.
   - If it suggests **upgrading the Android Gradle Plugin**, click **ignore / don't ask again**. Don't upgrade.
3. Pick the emulator in the device dropdown at the top and press the green **▶ Run** button.

**Demo tip:** open **Profile → Load sample data** to fill the app instantly, then show Home, Categories, the ring chart, History and dark mode.

---

## Project structure

```
app/src/main/java/com/example/expensetracker/
│
├── App.java                   Runs at app start, applies the saved theme
├── MainActivity.java          Holds the 4 tabs, glass bottom bar and centre + button
├── HomeFragment.java          Home tab
├── CategoriesFragment.java    Categories tab (ring chart + category cards)
├── HistoryFragment.java       History tab (grouped list, filter, swipe-delete)
├── ProfileFragment.java       Profile tab (name, theme, stats, data tools)
├── CategoryDetailActivity.java  One category's details
├── AddTransactionSheet.java   Bottom sheet form to add income or expense
│
├── Transaction.java           Model: one income/expense
├── Category.java              Model + list of all categories (emoji, colour)
├── TransactionDb.java         SQLite database (insert, delete, queries, totals)
│
├── TransactionAdapter.java    RecyclerView adapter for transaction lists (+ date headers)
├── CategoryCardAdapter.java   RecyclerView adapter for category cards
├── SwipeDeleteCallback.java   Swipe-to-delete with red background
├── SegmentedControl.java      Custom sliding toggle (Expense | Income)
├── DonutChartView.java        Custom-drawn animated ring chart (Canvas)
│
├── Ui.java                    Clay drawable, animations, theme helpers
├── Format.java                Money (₹1,23,456) and date formatting
├── Prefs.java                 SharedPreferences: theme + name
├── SampleData.java            Demo data loader
└── Refreshable.java           Interface: "this screen can reload its data"

app/src/main/res/
├── layout/       Screen designs (XML)
├── drawable/     Glass/clay shapes, gradients, icons
├── anim/         Animation files (slide, fade, list entrance)
├── values/       Light colours, strings, themes, styles
└── values-night/ Dark colours
```

## Database table: `transactions`

| Column   | Type    | Meaning                          |
|----------|---------|----------------------------------|
| id       | INTEGER | Primary key, auto-increment      |
| type     | TEXT    | `income` or `expense`            |
| title    | TEXT    | What it was for                  |
| amount   | REAL    | Amount in ₹                      |
| category | TEXT    | Category key, e.g. `food`        |
| note     | TEXT    | Optional note                    |
| time     | INTEGER | Date/time in milliseconds        |

Main queries: `SUM(amount) WHERE type=?` (total income/expense), `GROUP BY category` (per-category totals), `ORDER BY time DESC` (history).

---

## Likely viva questions and answers

**Q: What is the aim of the app?**
To help students track money received and spent, see where money goes by category, and know their balance.

**Q: Which components did you use?**
Activities, Fragments, RecyclerView with Adapters, SQLite (SQLiteOpenHelper), SharedPreferences, BottomSheetDialogFragment, MaterialDatePicker, Snackbar, ItemTouchHelper, and custom Views.

**Q: Activity vs Fragment?**
An Activity is a full screen. A Fragment is a reusable part of a screen with its own lifecycle. `MainActivity` hosts 4 fragments (tabs) and switches them with `show()`/`hide()`.

**Q: How is the balance calculated?**
`Total income − Total expenses`, using `SELECT SUM(amount) FROM transactions WHERE type = ?`.

**Q: How do category totals work?**
`SELECT category, SUM(amount) ... GROUP BY category` returns a Map like `{food=790, travel=740}`.

**Q: How did you implement dark mode?**
The theme extends `Theme.Material3.DayNight`. Colours are defined twice: `values/colors.xml` (light) and `values-night/colors.xml` (dark). `AppCompatDelegate.setDefaultNightMode()` switches the mode, and the choice is saved in SharedPreferences.

**Q: What is glassmorphism / claymorphism?**
Glassmorphism makes semi-transparent "frosted glass" cards with thin light borders over colourful backgrounds. Claymorphism makes soft, puffy 3D-looking shapes using big rounded corners, a soft shadow below and an inner highlight. Here they are XML `shape` drawables and a `LayerDrawable` built in `Ui.clay()`.

**Q: How are the animations made?**
- `ValueAnimator` makes numbers count up and the ring chart draw itself.
- `ObjectAnimator` moves the background blobs and makes the + button pulse.
- `View.animate()` handles press effects, sliding cards and the toggle.
- `layoutAnimation` makes list items fall in one after another.
- `overridePendingTransition` slides between screens.

**Q: How does swipe-to-delete work?**
`ItemTouchHelper.SimpleCallback` detects the swipe, deletes the row from SQLite, and shows a Snackbar with **UNDO**, which inserts the row back.

**Q: What is a custom View?**
`DonutChartView` extends `View` and draws arcs on a `Canvas` in `onDraw()`. `SegmentedControl` extends `FrameLayout` and builds its own children.

**Q: How do you validate input?**
The amount must be a number greater than 0. Otherwise the field shakes and an error shows. The title is optional and defaults to the category name.

**Q: Future improvements?**
Monthly budgets with alerts, export to PDF/Excel, cloud backup with Firebase, a login with PIN/fingerprint, and recurring transactions.

---

## Tech details
- Language: Java 17. Min SDK 26 (Android 8.0), Target SDK 34, Compile SDK 36.
- Android Gradle Plugin 8.5.0, Gradle 8.7.
- Libraries: AppCompat, Material Components (Material 3), RecyclerView.
