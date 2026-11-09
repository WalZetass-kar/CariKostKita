package com.carikostkita.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Advertisement;
import com.carikostkita.util.ImageLoaderUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.List;

public class AdminAdsAdapter extends RecyclerView.Adapter<AdminAdsAdapter.ViewHolder> {

    public interface OnAdActionListener {
        void onToggleActive(Advertisement ad, boolean isActive, int position);
        void onDeleteAd(Advertisement ad, int position);
    }

    private final List<Advertisement> adList = new ArrayList<>();
    private final OnAdActionListener listener;

    public AdminAdsAdapter(OnAdActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Advertisement> list) {
        this.adList.clear();
        if (list != null) {
            this.adList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_ad, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Advertisement ad = adList.get(position);

        holder.tvTitle.setText(ad.getTitle());
        holder.tvTarget.setText("Link: " + (ad.getTargetUrl().isEmpty() ? "-" : ad.getTargetUrl()));
        holder.tvStats.setText("Tayang: " + ad.getImpressions() + "x | Klik: " + ad.getClicks() + "x");

        if (ad.getImageUrl() != null && !ad.getImageUrl().trim().isEmpty()) {
            holder.ivThumbnail.setVisibility(View.VISIBLE);
            ImageLoaderUtil.loadImage(holder.ivThumbnail, ad.getImageUrl());
        } else {
            holder.ivThumbnail.setVisibility(View.GONE);
        }

        holder.switchActive.setOnCheckedChangeListener(null);
        holder.switchActive.setChecked(ad.isActive());
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onToggleActive(ad, isChecked, position);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteAd(ad, position);
        });
    }

    @Override
    public int getItemCount() {
        return adList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle;
        TextView tvTarget;
        TextView tvStats;
        SwitchMaterial switchActive;
        MaterialButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_ad_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_ad_title);
            tvTarget = itemView.findViewById(R.id.tv_ad_target);
            tvStats = itemView.findViewById(R.id.tv_ad_stats);
            switchActive = itemView.findViewById(R.id.switch_ad_active);
            btnDelete = itemView.findViewById(R.id.btn_ad_delete);
        }
    }
}
