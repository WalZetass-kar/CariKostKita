package com.carikostkita.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * Micro-interaction helpers — lightweight press & burst animations (150–200ms).
 */
public final class TouchFeedbackUtil {

    private TouchFeedbackUtil() {}

    /**
     * Attaches a subtle press-scale animation to any View.
     * Scale: 1.0 → 0.93 on press, 0.93 → 1.0 on release.
     * Duration: 120ms down / 150ms up.
     *
     * @param v        the view to animate
     * @param listener optional click listener; pass null if click is already set
     */
    public static void attachPress(View v, View.OnClickListener listener) {
        if (listener != null) v.setOnClickListener(listener);
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    animateScale(view, 1f, 0.93f, 120);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    animateScale(view, 0.93f, 1f, 150);
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        view.performClick();
                    }
                    break;
            }
            return true;
        });
    }

    /**
     * Overload that attaches press animation without replacing the click listener.
     */
    public static void attachPress(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    animateScale(view, 1f, 0.93f, 120);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    animateScale(view, 0.93f, 1f, 150);
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        view.performClick();
                    }
                    break;
            }
            return true;
        });
    }

    /**
     * Burst animation for favorite/heart toggle.
     * Scale: 1.0 → 1.28 → 1.0, 200ms total.
     */
    public static void animateFavoriteBurst(ImageView iv) {
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(iv, "scaleX", 1f, 1.28f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(iv, "scaleY", 1f, 1.28f);
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(iv, "scaleX", 1.28f, 1f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(iv, "scaleY", 1.28f, 1f);

        AnimatorSet up = new AnimatorSet();
        up.playTogether(scaleUpX, scaleUpY);
        up.setDuration(110);

        AnimatorSet down = new AnimatorSet();
        down.playTogether(scaleDownX, scaleDownY);
        down.setDuration(90);

        AnimatorSet burst = new AnimatorSet();
        burst.playSequentially(up, down);
        burst.start();
    }

    private static void animateScale(View v, float from, float to, long duration) {
        v.animate()
                .scaleX(to)
                .scaleY(to)
                .setDuration(duration)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                .start();
    }
}
