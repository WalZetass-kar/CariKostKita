package com.carikostkita.ui.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import com.carikostkita.R;

/**
 * Floating Expanding Pill Bottom Navigation View.
 * Berdasarkan Design Reference resmi CariKostKita:
 * - Tab aktif melebar menjadi pill berlatar gradien merah marun (#C0182A ke #8E0F1A)
 *   dengan ikon putih + label teks putih.
 * - Tab non-aktif menyusut menjadi icon-only (#667085) dengan touch target >= 48dp.
 * - Transisi halus 250ms dengan FastOutSlowInInterpolator.
 */
public class AppBottomNavView extends FrameLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    private LinearLayout itemsContainer;
    private final FrameLayout[] tabContainers = new FrameLayout[5];
    private final LinearLayout[] tabPills = new LinearLayout[5];
    private final ImageView[] tabIcons = new ImageView[5];
    private final TextView[] tabLabels = new TextView[5];
    private View badgeUnreadChat;

    private int selectedIndex = 0;
    private OnTabSelectedListener listener;
    private int colorSecondary;

    public AppBottomNavView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public AppBottomNavView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AppBottomNavView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_app_bottom_nav, this, true);

        setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        setClipToOutline(true);

        colorSecondary = ContextCompat.getColor(context, R.color.text_secondary);
        itemsContainer = findViewById(R.id.nav_items_container);

        tabContainers[0] = findViewById(R.id.tab_home);
        tabContainers[1] = findViewById(R.id.tab_search);
        tabContainers[2] = findViewById(R.id.tab_favorite);
        tabContainers[3] = findViewById(R.id.tab_chat);
        tabContainers[4] = findViewById(R.id.tab_profile);

        tabPills[0] = findViewById(R.id.pill_home);
        tabPills[1] = findViewById(R.id.pill_search);
        tabPills[2] = findViewById(R.id.pill_favorite);
        tabPills[3] = findViewById(R.id.pill_chat);
        tabPills[4] = findViewById(R.id.pill_profile);

        tabIcons[0] = findViewById(R.id.iv_tab_home);
        tabIcons[1] = findViewById(R.id.iv_tab_search);
        tabIcons[2] = findViewById(R.id.iv_tab_favorite);
        tabIcons[3] = findViewById(R.id.iv_tab_chat);
        tabIcons[4] = findViewById(R.id.iv_tab_profile);

        tabLabels[0] = findViewById(R.id.tv_tab_home);
        tabLabels[1] = findViewById(R.id.tv_tab_search);
        tabLabels[2] = findViewById(R.id.tv_tab_favorite);
        tabLabels[3] = findViewById(R.id.tv_tab_chat);
        tabLabels[4] = findViewById(R.id.tv_tab_profile);

        badgeUnreadChat = findViewById(R.id.badge_unread_chat);

        for (int i = 0; i < 5; i++) {
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

        // Apply initial state without animation
        post(() -> selectTab(0, false));
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setChatUnreadBadge(boolean show) {
        if (badgeUnreadChat != null) {
            badgeUnreadChat.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void selectTab(int index, boolean animate) {
        if (index < 0 || index >= 5) return;
        selectedIndex = index;

        if (animate && itemsContainer != null) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(250);
            transition.setInterpolator(new FastOutSlowInInterpolator());
            TransitionManager.beginDelayedTransition(itemsContainer, transition);
        }

        for (int i = 0; i < 5; i++) {
            FrameLayout container = tabContainers[i];
            LinearLayout pill = tabPills[i];
            ImageView icon = tabIcons[i];
            TextView label = tabLabels[i];

            if (container == null || pill == null || icon == null || label == null) continue;

            if (i == index) {
                // Active Expanding Pill
                pill.setBackgroundResource(R.drawable.bg_nav_pill_active);
                icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                label.setTextColor(Color.WHITE);
                label.setVisibility(View.VISIBLE);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1.85f);
                container.setLayoutParams(params);
            } else {
                // Inactive Icon-Only
                pill.setBackground(null);
                icon.setImageTintList(ColorStateList.valueOf(colorSecondary));
                label.setVisibility(View.GONE);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
                container.setLayoutParams(params);
            }
        }
    }
}
