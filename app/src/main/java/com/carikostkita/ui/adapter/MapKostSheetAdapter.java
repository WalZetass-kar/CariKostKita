package com.carikostkita.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.util.FormatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapKostSheetAdapter extends RecyclerView.Adapter<MapKostSheetAdapter.ViewHolder> {

    public static class MapKostItem {
        public final Kost kost;
        public final double distanceKm;

        public MapKostItem(Kost kost, double distanceKm) {
            this.kost = kost;
            this.distanceKm = distanceKm;
        }
    }

    public interface OnMapKostClickListener {
        void onKostSelected(MapKostItem item);
    }

    private final List<MapKostItem> items = new ArrayList<>();
    private final OnMapKostClickListener listener;

    public MapKostSheetAdapter(OnMapKostClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<MapKostItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kost_map_sheet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MapKostItem entry = items.get(position);
        Kost kost = entry.kost;

        holder.tvName.setText(kost.getNamaKost());
        holder.tvAddress.setText(kost.getFullLocation());
        holder.tvPrice.setText(FormatUtil.formatRupiah(kost.getHarga()) + " / bln");

        if (entry.distanceKm >= 0) {
            holder.tvDistance.setVisibility(View.VISIBLE);
            if (entry.distanceKm < 1.0) {
                int meters = (int) Math.round(entry.distanceKm * 1000);
                holder.tvDistance.setText(meters + " m");
            } else {
                holder.tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", entry.distanceKm));
            }
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        if (kost.getTipeKost() != null) {
            holder.tvTipe.setText(kost.getTipeKost().name());
            Context ctx = holder.itemView.getContext();
            if ("PUTRA".equalsIgnoreCase(kost.getTipeKost().name())) {
                holder.tvTipe.setBackgroundResource(R.drawable.bg_badge_putra);
                holder.tvTipe.setTextColor(ContextCompat.getColor(ctx, R.color.badge_putra));
            } else if ("PUTRI".equalsIgnoreCase(kost.getTipeKost().name())) {
                holder.tvTipe.setBackgroundResource(R.drawable.bg_badge_putri);
                holder.tvTipe.setTextColor(ContextCompat.getColor(ctx, R.color.badge_putri));
            } else {
                holder.tvTipe.setBackgroundResource(R.drawable.bg_badge_campur);
                holder.tvTipe.setTextColor(ContextCompat.getColor(ctx, R.color.badge_campur));
            }
        }

        if (kost.isAvailable()) {
            holder.tvRooms.setText(kost.hasRoomInfo() ? kost.getKamarTersedia() + " Kamar Kosong" : "Tersedia");
        } else {
            holder.tvRooms.setText("Kamar Penuh");
        }

        String thumb = (kost.getFotoUtama() != null && !kost.getFotoUtama().isEmpty()) ? kost.getFotoUtama() : null;
        Glide.with(holder.itemView.getContext())
                .load(thumb)
                .placeholder(R.drawable.bg_thumb_placeholder)
                .error(R.drawable.bg_thumb_placeholder)
                .centerCrop()
                .into(holder.ivThumb);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onKostSelected(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvTipe, tvDistance, tvName, tvAddress, tvPrice, tvRooms;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_sheet_kost_thumb);
            tvTipe = itemView.findViewById(R.id.tv_sheet_kost_tipe);
            tvDistance = itemView.findViewById(R.id.tv_sheet_kost_distance);
            tvName = itemView.findViewById(R.id.tv_sheet_kost_name);
            tvAddress = itemView.findViewById(R.id.tv_sheet_kost_address);
            tvPrice = itemView.findViewById(R.id.tv_sheet_kost_price);
            tvRooms = itemView.findViewById(R.id.tv_sheet_kost_rooms);
        }
    }
}
