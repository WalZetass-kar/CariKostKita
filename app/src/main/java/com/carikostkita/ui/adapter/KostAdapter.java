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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KostAdapter extends RecyclerView.Adapter<KostAdapter.KostViewHolder> {

    public interface OnKostClickListener {
        void onKostClick(Kost kost);
        void onFavoriteToggle(Kost kost, int position);
    }

    private final List<Kost> kostList;
    private final OnKostClickListener listener;
    private double originLat = 0;
    private double originLng = 0;

    public KostAdapter(OnKostClickListener listener) {
        this.kostList = new ArrayList<>();
        this.listener = listener;
    }

    /** Titik acuan untuk menampilkan jarak di kartu; (0,0) menyembunyikan jarak. */
    public void setDistanceOrigin(double lat, double lng) {
        this.originLat = lat;
        this.originLng = lng;
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
    public KostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kost_card, parent, false);
        return new KostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KostViewHolder holder, int position) {
        Kost kost = kostList.get(position);
        holder.bind(kost, listener, position, originLat, originLng);
    }

    @Override
    public int getItemCount() {
        return kostList.size();
    }

    public static class KostViewHolder extends RecyclerView.ViewHolder {
        private final View cardRoot;
        private final ImageView ivThumbnail;
        private final View layoutPlaceholder;
        private final TextView tvBadgeTipe;
        private final ImageButton btnFavorite;
        private final TextView tvName;
        private final TextView tvLocation;
        private final TextView tvFacilities;
        private final TextView tvRoomSpecs;
        private final TextView tvPrice;
        private final TextView tvStatus;

        public KostViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_kost_root);
            ivThumbnail = itemView.findViewById(R.id.iv_kost_thumbnail);
            layoutPlaceholder = itemView.findViewById(R.id.layout_thumb_placeholder);
            tvBadgeTipe = itemView.findViewById(R.id.tv_badge_tipe);
            btnFavorite = itemView.findViewById(R.id.btn_favorite_toggle);
            tvName = itemView.findViewById(R.id.tv_kost_name);
            tvLocation = itemView.findViewById(R.id.tv_kost_location);
            tvFacilities = itemView.findViewById(R.id.tv_kost_facilities);
            tvRoomSpecs = itemView.findViewById(R.id.tv_kost_room_specs);
            tvPrice = itemView.findViewById(R.id.tv_kost_price);
            tvStatus = itemView.findViewById(R.id.tv_kost_status);
        }

        public void bind(Kost kost, OnKostClickListener listener, int position, double originLat, double originLng) {
            Context context = itemView.getContext();
            tvName.setText(kost.getNamaKost());
            String location = kost.getFullLocation();
            if (originLat != 0 && originLng != 0 && kost.hasCoordinates()) {
                location = com.carikostkita.util.GeoUtil.formatDistance(com.carikostkita.util.GeoUtil.distanceKm(
                        originLat, originLng, kost.getLatitude(), kost.getLongitude())) + " • " + location;
            }
            tvLocation.setText(location);
            String rating = kost.getRatingLabel();
            tvPrice.setText(rating != null ? kost.getFormattedHarga() + "   " + rating : kost.getFormattedHarga());

            // Ukuran kamar hanya tampil bila pemilik mengisinya
            if (tvRoomSpecs != null) {
                String ukuran = kost.getUkuranKamar();
                tvRoomSpecs.setText(ukuran != null ? ukuran : "");
                tvRoomSpecs.setVisibility(ukuran != null ? View.VISIBLE : View.GONE);
            }

            // Facilities
            if (tvFacilities != null) {
                if (kost.getListFasilitas() != null && !kost.getListFasilitas().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int max = Math.min(3, kost.getListFasilitas().size());
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
            boolean isPenuh = !kost.isAvailable();
            if (isPenuh) {
                if (cardRoot != null) cardRoot.setAlpha(0.6f);
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                ivThumbnail.setColorFilter(new ColorMatrixColorFilter(cm));
                tvStatus.setText(kost.getAvailabilityLabel());
                tvStatus.setBackgroundResource(R.drawable.bg_pill_penuh);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_penuh));
            } else {
                if (cardRoot != null) cardRoot.setAlpha(1.0f);
                ivThumbnail.clearColorFilter();
                tvStatus.setText(kost.hasRoomInfo() ? kost.getKamarTersedia() + " kamar" : "Tersedia");
                tvStatus.setBackgroundResource(R.drawable.bg_pill_tersedia);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_tersedia));
            }

            // Thumbnail Loading via Glide
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
                btnFavorite.setContentDescription("Hapus " + kost.getNamaKost() + " dari favorit");
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                btnFavorite.setContentDescription("Simpan " + kost.getNamaKost() + " ke favorit");
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
