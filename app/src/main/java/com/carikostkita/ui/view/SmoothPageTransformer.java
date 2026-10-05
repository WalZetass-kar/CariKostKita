package com.carikostkita.ui.view;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

/**
 * SmoothPageTransformer memberikan transisi halus dan natural antar halaman/tab
 * dengan subtle cross-dissolve dan subtle depth scale (0.97 ke 1.0)
 * sehingga swipe dan switch tab tidak terasa berat atau meloncat.
 */
public class SmoothPageTransformer implements ViewPager2.PageTransformer {

    private static final float MIN_SCALE = 0.97f;
    private static final float MIN_ALPHA = 0.88f;

    @Override
    public void transformPage(@NonNull View page, float position) {
        if (position < -1) {
            page.setAlpha(0f);
        } else if (position <= 1) {
            float absPos = Math.abs(position);
            float scaleFactor = MIN_SCALE + (1.0f - MIN_SCALE) * (1.0f - absPos);
            float alphaFactor = MIN_ALPHA + (1.0f - MIN_ALPHA) * (1.0f - absPos);

            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);
            page.setAlpha(alphaFactor);
        } else {
            page.setAlpha(0f);
        }
    }
}
