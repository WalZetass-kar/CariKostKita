package com.carikostkita.ui.adapter;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KostCarouselAdapter extends RecyclerView.Adapter<KostCarouselAdapter.CarouselViewHolder> {

    public interface OnCarouselKostClickListener {
        void onKostClick(Kost kost);
        void onFavoriteToggle(Kost kost, int position);
    }

    private final List<Kost> kostList;
    private final OnCarouselKostClickListener listener;

    public KostCarouselAdapter(OnCarouselKostClickListener listener) {
        this.kostList = new ArrayList<>();
        this.listener = listener;
    }

    public void submitList(List<Kost> newList) {
        this.kostList.clear();
        if (newList != null) {
            this.kostList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kost_card_compact, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        Kost kost = kostList.get(position);
        holder.bind(kost, listener, position);
    }

    @Override
    public int getItemCount() {
        return kostList.size();
    }

    public static class CarouselViewHolder extends RecyclerView.ViewHolder {
        private final View cardRoot;
        private final ImageView ivThumbnail;
        private final View layoutPlaceholder;
        private final TextView tvBadgeTipe;
        private final ImageButton btnFavorite;
        private final TextView tvName;
        private final TextView tvLocation;
        private final TextView tvFacilities;
        private final TextView tvPrice;
        private final TextView tvStatus;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_compact_root);
            ivThumbnail = itemView.findViewById(R.id.iv_compact_thumbnail);
            layoutPlaceholder = itemView.findViewById(R.id.layout_compact_placeholder);
            tvBadgeTipe = itemView.findViewById(R.id.tv_compact_badge_tipe);
            btnFavorite = itemView.findViewById(R.id.btn_compact_favorite_toggle);
            tvName = itemView.findViewById(R.id.tv_compact_name);
            tvLocation = itemView.findViewById(R.id.tv_compact_location);
            tvFacilities = itemView.findViewById(R.id.tv_compact_facilities);
            tvPrice = itemView.findViewById(R.id.tv_compact_price);
            tvStatus = itemView.findViewById(R.id.tv_compact_status);
        }

        public void bind(Kost kost, OnCarouselKostClickListener listener, int position) {
            Context context = itemView.getContext();
            tvName.setText(kost.getNamaKost());
            tvLocation.setText(kost.getFullLocation());
            tvPrice.setText(kost.getFormattedHarga());

            // Facilities
            if (tvFacilities != null) {
                if (kost.getListFasilitas() != null && !kost.getListFasilitas().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int max = Math.min(2, kost.getListFasilitas().size());
                    for (int i = 0; i < max; i++) {
                        if (i > 0) sb.append(" • ");
                        sb.append(kost.getListFasilitas().get(i).getNamaFasilitas());
                    }
                    tvFacilities.setText(sb.toString());
                    tvFacilities.setVisibility(View.VISIBLE);
                } else {
                    tvFacilities.setVisibility(View.GONE);
                }
            }

            // Badge Tipe
            if (kost.getTipeKost() == TipeKost.PUTRI) {
                tvBadgeTipe.setText("PUTRI");
                tvBadgeTipe.setBackgroundResource(R.drawable.bg_badge_putri);
                tvBadgeTipe.setTextColor(ContextCompat.getColor(context, R.color.badge_putri));
            } else if (kost.getTipeKost() == TipeKost.PUTRA) {
                tvBadgeTipe.setText("PUTRA");
                tvBadgeTipe.setBackgroundResource(R.drawable.bg_badge_putra);
                tvBadgeTipe.setTextColor(ContextCompat.getColor(context, R.color.badge_putra));
            } else {
                tvBadgeTipe.setText("CAMPUR");
                tvBadgeTipe.setBackgroundResource(R.drawable.bg_badge_campur);
                tvBadgeTipe.setTextColor(ContextCompat.getColor(context, R.color.badge_campur));
            }

            // Ketersediaan & Efek Penuh (Opacity 0.6 + Grayscale)
            boolean isPenuh = (kost.getStatus() == StatusKost.PENUH || kost.getKamarTersedia() <= 0);
            if (isPenuh) {
                if (cardRoot != null) cardRoot.setAlpha(0.6f);
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                ivThumbnail.setColorFilter(new ColorMatrixColorFilter(cm));
                tvStatus.setText("Penuh");
                tvStatus.setBackgroundResource(R.drawable.bg_pill_penuh);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_penuh));
            } else {
                if (cardRoot != null) cardRoot.setAlpha(1.0f);
                ivThumbnail.clearColorFilter();
                tvStatus.setText(kost.getKamarTersedia() + " kamar");
                tvStatus.setBackgroundResource(R.drawable.bg_pill_tersedia);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_tersedia));
            }

            // Thumbnail Loading
            String path = kost.getThumbnailPath();
            if (path != null && !path.trim().isEmpty()) {
                if (layoutPlaceholder != null) layoutPlaceholder.setVisibility(View.GONE);
                ivThumbnail.setVisibility(View.VISIBLE);

                Object loadTarget;
                if (path.startsWith("content://") || path.startsWith("file://")) {
                    loadTarget = Uri.parse(path);
                } else if (path.startsWith("http://") || path.startsWith("https://")) {
                    loadTarget = path;
                } else {
                    File file = new File(path);
                    loadTarget = file.exists() ? file : path;
                }

                Glide.with(context)
                        .load(loadTarget)
                        .centerCrop()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                if (layoutPlaceholder != null) layoutPlaceholder.setVisibility(View.VISIBLE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                if (layoutPlaceholder != null) layoutPlaceholder.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(ivThumbnail);
            } else {
                ivThumbnail.setImageDrawable(null);
                if (layoutPlaceholder != null) layoutPlaceholder.setVisibility(View.VISIBLE);
            }

            // Favorite Icon & Pop Animation + Feedback Snackbar
            if (kost.isFavorite()) {
                btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            }

            btnFavorite.setOnClickListener(v -> {
                btnFavorite.setScaleX(0.8f);
                btnFavorite.setScaleY(0.8f);
                btnFavorite.animate()
                        .scaleX(1.3f)
                        .scaleY(1.3f)
                        .setDuration(120)
                        .withEndAction(() -> btnFavorite.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(120)
                                .start())
                        .start();

                boolean willBeFav = !kost.isFavorite();
                String msg = willBeFav ? "Disimpan ke favorit" : "Dihapus dari favorit";
                Snackbar.make(itemView, msg, Snackbar.LENGTH_SHORT)
                        .setDuration(1500)
                        .show();

                if (listener != null) {
                    listener.onFavoriteToggle(kost, position);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onKostClick(kost);
                }
            });
        }
    }
}
