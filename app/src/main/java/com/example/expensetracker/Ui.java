package com.example.expensetracker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared design + animation helpers (claymorphism, motion, theme).
 */
public final class Ui {

    private Ui() {}

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static boolean isNight(Context c) {
        int mode = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Dark status/navigation bar icons in light mode, light icons in dark mode. */
    public static void applySystemBars(Activity activity) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView());
        boolean light = !isNight(activity);
        controller.setAppearanceLightStatusBars(light);
        controller.setAppearanceLightNavigationBars(light);
    }

    // ---------------- Claymorphism ----------------

    /**
     * Builds a "clay" background in the given colour:
     * a soft shadow underneath, a puffy gradient body and a light inner border.
     * Views using it should have about 5dp extra bottom padding.
     */
    public static Drawable clay(Context c, int color, float radiusDp) {
        boolean night = isNight(c);
        int base = night
                ? ColorUtils.blendARGB(color, 0xFF1A1433, 0.62f)
                : ColorUtils.blendARGB(color, Color.WHITE, 0.55f);
        int top = ColorUtils.blendARGB(base, Color.WHITE, night ? 0.08f : 0.5f);
        int shadow = night
                ? 0x73000000
                : ColorUtils.setAlphaComponent(ColorUtils.blendARGB(color, Color.BLACK, 0.15f), 0x5C);
        float r = dp(c, radiusDp);

        GradientDrawable shadowLayer = new GradientDrawable();
        shadowLayer.setCornerRadius(r);
        shadowLayer.setColor(shadow);

        GradientDrawable body = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, base});
        body.setCornerRadius(r);

        GradientDrawable shine = new GradientDrawable();
        shine.setCornerRadius(r);
        shine.setColor(Color.TRANSPARENT);
        shine.setStroke(dp(c, 1.5f), night ? 0x2EFFFFFF : 0xB3FFFFFF);

        LayerDrawable clay = new LayerDrawable(new Drawable[]{shadowLayer, body, shine});
        int offset = dp(c, 5), side = dp(c, 2);
        clay.setLayerInset(0, side, offset, side, 0);
        clay.setLayerInset(1, 0, 0, 0, offset);
        clay.setLayerInset(2, 0, 0, 0, offset);
        return clay;
    }

    // ---------------- Motion ----------------

    /** Squishy "press" effect: shrinks while touched, bounces back on release. */
    @SuppressLint("ClickableViewAccessibility")
    public static void pressEffect(View... views) {
        for (View view : views) {
            view.setOnTouchListener((v, e) -> {
                int action = e.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(110)
                            .setInterpolator(new DecelerateInterpolator()).start();
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(380)
                            .setInterpolator(new OvershootInterpolator(3f)).start();
                }
                return false; // let the normal click still happen
            });
        }
    }

    /** Slide up + fade in, delayed by its position so items appear one after another. */
    public static void animateIn(View v, int index) {
        v.setAlpha(0f);
        v.setTranslationY(dp(v.getContext(), 28));
        v.animate().alpha(1f).translationY(0f)
                .setStartDelay(index * 70L).setDuration(520)
                .setInterpolator(new DecelerateInterpolator(2.2f)).start();
    }

    public static void animateChildren(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) animateIn(group.getChildAt(i), i);
    }

    /** Pop in with a bounce (used for emoji / icons). */
    public static void popIn(View v, long delay) {
        v.setScaleX(0.4f);
        v.setScaleY(0.4f);
        v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(delay).setDuration(500)
                .setInterpolator(new OvershootInterpolator(2.5f)).start();
    }

    /** Shake left-right, used when the input is wrong. */
    public static void shake(View v) {
        float d = dp(v.getContext(), 10);
        ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0, d, -d, d * 0.7f, -d * 0.7f, d * 0.3f, 0)
                .setDuration(450).start();
    }

    /** Animates a money TextView, counting from its previous value (or 0) to the new one. */
    public static void countUp(TextView tv, double target, boolean fromZero) {
        Object previous = tv.getTag(R.id.tag_value);
        double from = (!fromZero && previous instanceof Double) ? (Double) previous : 0;
        tv.setTag(R.id.tag_value, target);
        if (from == target) {
            tv.setText(Format.money(target));
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat((float) from, (float) target);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator(2.5f));
        animator.addUpdateListener(a -> tv.setText(Format.money(Math.round((float) a.getAnimatedValue()))));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tv.setText(Format.money(target));
            }
        });
        animator.start();
    }

    /** Makes the background blobs float slowly forever. Returns the animators so they can be stopped. */
    public static List<Animator> startBlobAnimation(View background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            float blur = dp(background.getContext(), 30);
            background.setRenderEffect(RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.DECAL));
        }
        float d = dp(background.getContext(), 60);
        List<Animator> list = new ArrayList<>();
        list.add(floatForever(background.findViewById(R.id.blob1), d, d * 0.7f, 1.2f, 7000));
        list.add(floatForever(background.findViewById(R.id.blob2), -d, d * 1.1f, 1.25f, 9000));
        list.add(floatForever(background.findViewById(R.id.blob3), d * 0.8f, -d, 1.15f, 8000));
        return list;
    }

    private static Animator floatForever(View v, float dx, float dy, float scale, long duration) {
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0, dx),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0, dy),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, scale),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, scale));
        a.setDuration(duration);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.start();
        return a;
    }

    // ---------------- Shared actions ----------------

    /** Deletes a transaction and shows an "UNDO" bar at the bottom. */
    public static void deleteWithUndo(Activity activity, View anchor, Transaction t, Runnable afterChange) {
        TransactionDb db = TransactionDb.get(activity);
        db.delete(t.id);
        afterChange.run();
        Snackbar bar = Snackbar.make(activity.findViewById(android.R.id.content),
                "Deleted \"" + t.title + "\"", Snackbar.LENGTH_LONG);
        bar.setAction("UNDO", v -> {
            db.insert(t);
            afterChange.run();
        });
        if (anchor != null) bar.setAnchorView(anchor);
        bar.show();
    }

    public static void toast(Activity activity, View anchor, String message) {
        Snackbar bar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        if (anchor != null) bar.setAnchorView(anchor);
        bar.show();
    }
}
