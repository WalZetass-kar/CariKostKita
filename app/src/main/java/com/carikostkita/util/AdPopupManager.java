package com.carikostkita.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import com.carikostkita.R;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.dto.AdDto;
import com.carikostkita.data.remote.dto.AdListResponse;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utilitas untuk memeriksa dan menampilkan iklan Pop-up promosi resmi
 * yang dikonfigurasi melalui Admin Dashboard platform CariKostKita.
 */
public final class AdPopupManager {

    private static final String TAG = "AdPopupManager";
    private static boolean hasShownThisSession = false;

    private AdPopupManager() {}

    /**
     * Memeriksa iklan aktif dari server dan menampilkan pop-up jika tersedia.
     * Hanya muncul 1 kali per sesi aplikasi agar tidak mengganggu pengalaman pengguna.
     */
    public static void checkAndShowPopupAd(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (hasShownThisSession) {
            return;
        }

        try {
            SupabaseClient.getInstance().createSettingsService().getActiveAds().enqueue(new Callback<AdListResponse>() {
                @Override
                public void onResponse(Call<AdListResponse> call, Response<AdListResponse> response) {
                    if (activity.isFinishing() || activity.isDestroyed()) {
                        return;
                    }

                    if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                        List<AdDto> ads = response.body().data;
                        AdDto targetAd = null;

                        for (AdDto ad : ads) {
                            if (ad == null) continue;

                            // Filter tipe iklan: Pop-up
                            String type = ad.adType != null ? ad.adType.trim().toLowerCase() : "";
                            boolean isPopup = type.contains("pop") || type.contains("popup");

                            // Filter platform target: Android / Semua / Website + Android
                            String plat = ad.platform != null ? ad.platform.trim().toLowerCase() : "";
                            boolean forAndroid = plat.isEmpty()
                                    || plat.contains("android")
                                    || plat.contains("semua")
                                    || plat.contains("all")
                                    || plat.contains("mobile");

                            if (isPopup && forAndroid) {
                                targetAd = ad;
                                break;
                            }
                        }

                        if (targetAd != null) {
                            showAdDialog(activity, targetAd);
                        }
                    }
                }

                @Override
                public void onFailure(Call<AdListResponse> call, Throwable t) {
                    Log.w(TAG, "Gagal mengambil iklan aktif: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Error in checkAndShowPopupAd: " + e.getMessage());
        }
    }

    private static void showAdDialog(Activity activity, AdDto ad) {
        try {
            hasShownThisSession = true;

            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_ad_popup, null);
            dialog.setContentView(view);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                        (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90),
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

            ImageView ivAdImage = view.findViewById(R.id.iv_ad_image);
            TextView tvAdTitle = view.findViewById(R.id.tv_ad_title);
            TextView tvAdDescription = view.findViewById(R.id.tv_ad_description);
            MaterialButton btnAdAction = view.findViewById(R.id.btn_ad_action);
            TextView btnAdDismiss = view.findViewById(R.id.btn_ad_dismiss);
            View btnAdCloseTop = view.findViewById(R.id.btn_ad_close_top);

            // Set Data
            if (tvAdTitle != null && ad.title != null && !ad.title.trim().isEmpty()) {
                tvAdTitle.setText(ad.title.trim());
            }

            if (tvAdDescription != null) {
                if (ad.description != null && !ad.description.trim().isEmpty()) {
                    tvAdDescription.setText(ad.description.trim());
                    tvAdDescription.setVisibility(View.VISIBLE);
                } else {
                    tvAdDescription.setVisibility(View.GONE);
                }
            }

            // Load Image
            if (ivAdImage != null && ad.imageUrl != null && !ad.imageUrl.trim().isEmpty()) {
                ImageLoaderUtil.loadImage(ivAdImage, ad.imageUrl.trim());
            }

            // Action Button
            final String targetUrl = ad.targetUrl != null ? ad.targetUrl.trim() : "";
            if (btnAdAction != null) {
                if (!targetUrl.isEmpty()) {
                    btnAdAction.setVisibility(View.VISIBLE);
                    btnAdAction.setOnClickListener(v -> {
                        dialog.dismiss();
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                            activity.startActivity(intent);
                        } catch (Exception ex) {
                            Log.w(TAG, "Gagal membuka link iklan: " + ex.getMessage());
                        }
                    });
                } else {
                    btnAdAction.setText("Mengerti");
                    btnAdAction.setOnClickListener(v -> dialog.dismiss());
                }
            }

            // Dismiss Actions
            if (btnAdDismiss != null) {
                btnAdDismiss.setOnClickListener(v -> dialog.dismiss());
            }
            if (btnAdCloseTop != null) {
                btnAdCloseTop.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.setCancelable(true);
            dialog.show();

        } catch (Exception e) {
            Log.w(TAG, "Gagal memunculkan dialog iklan: " + e.getMessage());
        }
    }

    /** Reset state sesi jika diperlukan (misal saat logout/login ulang). */
    public static void resetSession() {
        hasShownThisSession = false;
    }
}
