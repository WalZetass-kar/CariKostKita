package com.carikostkita.ui.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.carikostkita.R;

public class ModernBottomNavBar extends FrameLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    private View activeIndicator;
    private View[] tabContainers = new View[4];
    private ImageView[] tabIcons = new ImageView[4];
    private TextView[] tabLabels = new TextView[4];

    private int selectedIndex = 0;
    private OnTabSelectedListener listener;
    private boolean isFirstLayout = true;

    private int colorPrimary;
    private int colorMuted;
    private int colorTextSecondary;

    public ModernBottomNavBar(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ModernBottomNavBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ModernBottomNavBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_modern_bottom_nav, this, true);

        colorPrimary = ContextCompat.getColor(context, R.color.primary);
        colorMuted = ContextCompat.getColor(context, R.color.text_muted);
        colorTextSecondary = ContextCompat.getColor(context, R.color.text_secondary);

        activeIndicator = findViewById(R.id.nav_active_indicator);

        tabContainers[0] = findViewById(R.id.tab_home);
        tabContainers[1] = findViewById(R.id.tab_search);
        tabContainers[2] = findViewById(R.id.tab_favorite);
        tabContainers[3] = findViewById(R.id.tab_profile);

        tabIcons[0] = findViewById(R.id.iv_tab_home);
        tabIcons[1] = findViewById(R.id.iv_tab_search);
        tabIcons[2] = findViewById(R.id.iv_tab_favorite);
        tabIcons[3] = findViewById(R.id.iv_tab_profile);

        tabLabels[0] = findViewById(R.id.tv_tab_home);
        tabLabels[1] = findViewById(R.id.tv_tab_search);
        tabLabels[2] = findViewById(R.id.tv_tab_favorite);
        tabLabels[3] = findViewById(R.id.tv_tab_profile);

        for (int i = 0; i < 4; i++) {
            final int index = i;
            tabContainers[i].setOnClickListener(v -> {
                if (index != selectedIndex) {
                    selectTab(index, true);
                    if (listener != null) {
                        listener.onTabSelected(index);
                    }
                }
            });
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (isFirstLayout || changed) {
            isFirstLayout = false;
            post(() -> positionIndicator(selectedIndex, false));
        }
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void selectTab(int index, boolean animate) {
        if (index < 0 || index >= 4) return;
        int prevIndex = selectedIndex;
        selectedIndex = index;

        // Position indicator capsule
        positionIndicator(index, animate);

        // Update items styling & animations
        for (int i = 0; i < 4; i++) {
            ImageView icon = tabIcons[i];
            TextView label = tabLabels[i];

            if (i == index) {
                // Active Tab
                icon.setImageTintList(ColorStateList.valueOf(colorPrimary));
                label.setTextColor(colorPrimary);
                label.setTypeface(null, Typeface.BOLD);
                label.setAlpha(1.0f);

                if (animate) {
                    icon.setScaleX(0.92f);
                    icon.setScaleY(0.92f);
                    icon.animate()
                            .scaleX(1.08f)
                            .scaleY(1.08f)
                            .setDuration(110)
                            .setInterpolator(new FastOutSlowInInterpolator())
                            .withEndAction(() -> icon.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(110)
                                    .setInterpolator(new FastOutSlowInInterpolator())
                                    .start())
                            .start();
                } else {
                    icon.setScaleX(1.0f);
                    icon.setScaleY(1.0f);
                }
            } else {
                // Inactive Tab
                icon.setImageTintList(ColorStateList.valueOf(colorMuted));
                label.setTextColor(colorTextSecondary);
                label.setTypeface(null, Typeface.NORMAL);
                label.setAlpha(0.75f);
                icon.setScaleX(1.0f);
                icon.setScaleY(1.0f);
            }
        }
    }

    private void positionIndicator(int index, boolean animate) {
        int width = getWidth();
        if (width <= 0) return;

        float tabWidth = width / 4.0f;
        int indicatorWidth = activeIndicator.getWidth();
        if (indicatorWidth <= 0) {
            indicatorWidth = (int) (60 * getResources().getDisplayMetrics().density);
        }

        float targetX = (index + 0.5f) * tabWidth - (indicatorWidth / 2.0f);

        activeIndicator.setVisibility(View.VISIBLE);
        if (animate) {
            activeIndicator.animate()
                    .translationX(targetX)
                    .setDuration(220)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .start();
        } else {
            activeIndicator.setTranslationX(targetX);
        }
    }
}
