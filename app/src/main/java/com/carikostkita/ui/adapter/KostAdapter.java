package com.carikostkita.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import java.util.ArrayList;
import java.util.List;

public class KostAdapter extends RecyclerView.Adapter<KostAdapter.KostViewHolder> {

    public interface OnKostClickListener {
        void onKostClick(Kost kost);
        void onFavoriteToggle(Kost kost, int position);
    }

    private final List<Kost> kostList;
    private final OnKostClickListener listener;

    public KostAdapter(OnKostClickListener listener) {
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
    public KostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kost_card, parent, false);
        return new KostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KostViewHolder holder, int position) {
        Kost kost = kostList.get(position);
        holder.bind(kost, listener, position);
    }

    @Override
    public int getItemCount() {
        return kostList.size();
    }

    public static class KostViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvBadgeTipe;
        private final ImageButton btnFavorite;
        private final TextView tvName;
        private final TextView tvLocation;
        private final TextView tvFacilities;
        private final TextView tvRoomSpecs;
        private final TextView tvKamarTersedia;
        private final TextView tvPrice;
        private final TextView tvStatus;

        public KostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_kost_thumbnail);
            tvBadgeTipe = itemView.findViewById(R.id.tv_badge_tipe);
            btnFavorite = itemView.findViewById(R.id.btn_favorite_toggle);
            tvName = itemView.findViewById(R.id.tv_kost_name);
            tvLocation = itemView.findViewById(R.id.tv_kost_location);
            tvFacilities = itemView.findViewById(R.id.tv_kost_facilities);
            tvRoomSpecs = itemView.findViewById(R.id.tv_kost_room_specs);
            tvKamarTersedia = itemView.findViewById(R.id.tv_kost_kamar_tersedia);
            tvPrice = itemView.findViewById(R.id.tv_kost_price);
            tvStatus = itemView.findViewById(R.id.tv_kost_status);
        }

        public void bind(Kost kost, OnKostClickListener listener, int position) {
            Context context = itemView.getContext();
            tvName.setText(kost.getNamaKost());
            tvLocation.setText(kost.getFullLocation());
            tvPrice.setText(kost.getFormattedHarga());

            // Ukuran Kamar & Ketersediaan
            String ukuran = kost.getUkuranKamar() != null && !kost.getUkuranKamar().isEmpty() ? kost.getUkuranKamar() : "3x4 m";
            tvRoomSpecs.setText(ukuran);
            if (kost.getKamarTersedia() > 0) {
                tvKamarTersedia.setText("Sisa " + kost.getKamarTersedia() + " kamar");
                tvKamarTersedia.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            } else {
                tvKamarTersedia.setText("Kamar habis");
                tvKamarTersedia.setTextColor(ContextCompat.getColor(context, R.color.status_penuh));
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

            // Status Badge
            if (kost.getStatus() == StatusKost.TERSEDIA && kost.getKamarTersedia() > 0) {
                tvStatus.setText("Tersedia");
                tvStatus.setBackgroundResource(R.drawable.bg_badge_tersedia);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_tersedia));
            } else {
                tvStatus.setText("Penuh");
                tvStatus.setBackgroundResource(R.drawable.bg_badge_penuh);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_penuh));
            }

            // Ringkasan Fasilitas dengan separator '·'
            if (kost.getListFasilitas() != null && !kost.getListFasilitas().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int max = Math.min(3, kost.getListFasilitas().size());
                for (int i = 0; i < max; i++) {
                    if (i > 0) sb.append(" · ");
                    sb.append(kost.getListFasilitas().get(i).getNamaFasilitas());
                }
                tvFacilities.setText(sb.toString());
                tvFacilities.setVisibility(View.VISIBLE);
            } else {
                tvFacilities.setVisibility(View.GONE);
            }

            // Favorite Icon & Micro-animation
            if (kost.isFavorite()) {
                btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            }

            btnFavorite.setOnClickListener(v -> {
                // Heart bounce animation: 0.8 -> 1.15 -> 1.0 (200ms)
                btnFavorite.setScaleX(0.8f);
                btnFavorite.setScaleY(0.8f);
                btnFavorite.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(100)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .withEndAction(() -> btnFavorite.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .setInterpolator(new FastOutSlowInInterpolator())
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
