package com.carikostkita.util;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.carikostkita.R;

public class NotificationBannerHelper {

    private static View currentBannerView = null;
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static Runnable dismissRunnable = null;

    public static void showBanner(Activity activity, String title, String message,
                                  @DrawableRes int iconResId, View.OnClickListener onClickListener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) {
            return;
        }

        // Dismiss any existing banner first
        dismissCurrentBanner(false);

        LayoutInflater inflater = LayoutInflater.from(activity);
        View bannerView = inflater.inflate(R.layout.view_notification_banner, rootView, false);

        TextView tvTitle = bannerView.findViewById(R.id.tv_banner_title);
        TextView tvMessage = bannerView.findViewById(R.id.tv_banner_message);
        ImageView ivIcon = bannerView.findViewById(R.id.iv_banner_icon);
        View btnClose = bannerView.findViewById(R.id.btn_banner_close);

        if (tvTitle != null && title != null) {
            tvTitle.setText(title);
        }
        if (tvMessage != null && message != null) {
            tvMessage.setText(message);
        }
        if (ivIcon != null && iconResId != 0) {
            ivIcon.setImageResource(iconResId);
        }

        currentBannerView = bannerView;

        // Animate slide down
        Animation slideDown = AnimationUtils.loadAnimation(activity, R.anim.banner_slide_down);
        bannerView.startAnimation(slideDown);

        bannerView.setOnClickListener(v -> {
            dismissCurrentBanner(true);
            if (onClickListener != null) {
                onClickListener.onClick(v);
            }
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismissCurrentBanner(true));
        }

        rootView.addView(bannerView);

        // Auto dismiss after 3500ms
        dismissRunnable = () -> dismissCurrentBanner(true);
        handler.postDelayed(dismissRunnable, 3500);
    }

    public static void showMessageBanner(Activity activity, String senderName, String message, View.OnClickListener onClickListener) {
        showBanner(activity, "Pesan dari " + senderName, message, R.drawable.ic_nav_chat, onClickListener);
    }

    public static void dismissCurrentBanner(boolean animated) {
        if (dismissRunnable != null) {
            handler.removeCallbacks(dismissRunnable);
            dismissRunnable = null;
        }

        if (currentBannerView != null) {
            final View viewToRemove = currentBannerView;
            currentBannerView = null;

            if (animated && viewToRemove.getContext() != null) {
                Animation slideUp = AnimationUtils.loadAnimation(viewToRemove.getContext(), R.anim.banner_slide_up);
                slideUp.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        ViewGroup parent = (ViewGroup) viewToRemove.getParent();
                        if (parent != null) {
                            parent.removeView(viewToRemove);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                viewToRemove.startAnimation(slideUp);
            } else {
                ViewGroup parent = (ViewGroup) viewToRemove.getParent();
                if (parent != null) {
                    parent.removeView(viewToRemove);
                }
            }
        }
    }
}
