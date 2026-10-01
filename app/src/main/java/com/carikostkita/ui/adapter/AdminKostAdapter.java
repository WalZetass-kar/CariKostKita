package com.carikostkita.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.StatusKost;
import java.util.ArrayList;
import java.util.List;

public class AdminKostAdapter extends RecyclerView.Adapter<AdminKostAdapter.AdminKostViewHolder> {

    public interface OnAdminKostClickListener {
        void onEditClick(Kost kost);
        void onToggleStatusClick(Kost kost, int position);
    }

    private final List<Kost> kostList;
    private final OnAdminKostClickListener listener;

    public AdminKostAdapter(OnAdminKostClickListener listener) {
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
    public AdminKostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_kost_card, parent, false);
        return new AdminKostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminKostViewHolder holder, int position) {
        Kost kost = kostList.get(position);
        holder.bind(kost, listener, position);
    }

    @Override
    public int getItemCount() {
        return kostList.size();
    }

    public static class AdminKostViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvLocation;
        private final TextView tvPrice;
        private final TextView tvStatus;
        private final Button btnToggleStatus;
        private final Button btnEdit;

        public AdminKostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_admin_kost_name);
            tvLocation = itemView.findViewById(R.id.tv_admin_kost_location);
            tvPrice = itemView.findViewById(R.id.tv_admin_kost_price);
            tvStatus = itemView.findViewById(R.id.tv_admin_kost_status);
            btnToggleStatus = itemView.findViewById(R.id.btn_admin_toggle_status);
            btnEdit = itemView.findViewById(R.id.btn_admin_edit_kost);
        }

        public void bind(Kost kost, OnAdminKostClickListener listener, int position) {
            tvName.setText(kost.getNamaKost());
            tvLocation.setText(kost.getFullLocation());
            tvPrice.setText(kost.getFormattedHarga());

            if (kost.getStatus() == StatusKost.TERSEDIA) {
                tvStatus.setText("Tersedia");
                tvStatus.setBackgroundResource(R.drawable.bg_badge_tersedia);
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.status_tersedia));
            } else if (kost.getStatus() == StatusKost.PENUH) {
                tvStatus.setText("Penuh");
                tvStatus.setBackgroundResource(R.drawable.bg_badge_penuh);
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.status_penuh));
            } else {
                tvStatus.setText("Tidak Aktif");
                tvStatus.setBackgroundResource(R.drawable.bg_badge_penuh);
                tvStatus.setTextColor(itemView.getContext().getColor(R.color.text_muted));
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(kost);
            });

            btnToggleStatus.setOnClickListener(v -> {
                if (listener != null) listener.onToggleStatusClick(kost, position);
            });
        }
    }
}
