package com.carikostkita.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.User;
import com.carikostkita.util.UserAvatarHelper;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VerificationAdapter extends RecyclerView.Adapter<VerificationAdapter.ViewHolder> {

    public interface OnVerificationActionListener {
        void onApprove(User user, int position);
        void onRequireRevision(User user, int position);
        void onReject(User user, int position);
        default void onViewDocuments(User user) {}
    }

    private final List<User> userList = new ArrayList<>();
    private final Set<String> processingUserIds = new HashSet<>();
    private final OnVerificationActionListener listener;

    public VerificationAdapter(OnVerificationActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<User> list) {
        this.userList.clear();
        if (list != null) {
            this.userList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void setProcessing(String uid, boolean isProcessing) {
        if (uid == null) return;
        if (isProcessing) {
            processingUserIds.add(uid);
        } else {
            processingUserIds.remove(uid);
        }
        for (int i = 0; i < userList.size(); i++) {
            if (uid.equals(userList.get(i).getUid())) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < userList.size()) {
            User removed = userList.remove(position);
            if (removed != null && removed.getUid() != null) {
                processingUserIds.remove(removed.getUid());
            }
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, userList.size());
        }
    }

    public void removeItem(User user) {
        if (user == null) return;
        int idx = -1;
        for (int i = 0; i < userList.size(); i++) {
            if (user.getUid().equals(userList.get(i).getUid())) {
                idx = i;
                break;
            }
        }
        if (idx != -1) {
            removeItem(idx);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_verification_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvNama.setText(user.getNama());
        holder.tvEmail.setText(user.getEmail());
        holder.tvPhone.setText(user.getNoHp() != null && !user.getNoHp().isEmpty() ? user.getNoHp() : "No HP tidak diisi");

        UserAvatarHelper.loadAvatar(holder.ivAvatar, user.getAvatarUrl());

        String catatan = user.getPengajuanCatatan();
        if (catatan != null && !catatan.isEmpty()) {
            holder.tvCatatan.setText("Info Pengajuan: " + catatan);
            holder.tvCatatan.setVisibility(View.VISIBLE);
        } else {
            holder.tvCatatan.setText("Info Pengajuan: Ingin mendaftar sebagai Pemilik Kost.");
            holder.tvCatatan.setVisibility(View.VISIBLE);
        }

        // Umur antrean untuk SLA moderasi (target maks. 24 jam)
        String since = user.getPengajuanAt() != null ? user.getPengajuanAt() : user.getCreatedAt();
        long hours = hoursSince(since);
        holder.tvTime.setVisibility(View.VISIBLE);
        if (hours < 0) {
            holder.tvTime.setText("Diajukan calon pemilik");
        } else {
            String age = hours < 1 ? "kurang dari 1 jam" : hours < 24 ? hours + " jam" : (hours / 24) + " hari";
            holder.tvTime.setText((hours >= 24 ? "Lewat SLA • menunggu " : "Menunggu ") + age);
            holder.tvTime.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(),
                    hours >= 24 ? R.color.status_penuh : R.color.text_secondary));
        }
        View btnDocs = holder.itemView.findViewById(R.id.btn_verif_docs);
        if (btnDocs != null) btnDocs.setOnClickListener(v -> {
            if (listener != null) listener.onViewDocuments(user);
        });

        boolean isProcessing = user.getUid() != null && processingUserIds.contains(user.getUid());
        if (isProcessing) {
            holder.btnSetujui.setText("Memproses...");
            holder.btnSetujui.setEnabled(false);
            holder.btnRevisi.setEnabled(false);
            holder.btnTolak.setEnabled(false);
        } else {
            holder.btnSetujui.setText("Setujui");
            holder.btnSetujui.setEnabled(true);
            holder.btnRevisi.setEnabled(true);
            holder.btnTolak.setEnabled(true);
        }

        holder.btnSetujui.setOnClickListener(v -> {
            if (!isProcessing && listener != null) {
                listener.onApprove(user, holder.getAdapterPosition());
            }
        });

        holder.btnRevisi.setOnClickListener(v -> {
            if (!isProcessing && listener != null) {
                listener.onRequireRevision(user, holder.getAdapterPosition());
            }
        });

        holder.btnTolak.setOnClickListener(v -> {
            if (!isProcessing && listener != null) {
                listener.onReject(user, holder.getAdapterPosition());
            }
        });
    }

    /** Jam sejak waktu ISO (UTC), atau -1 bila tidak terbaca. */
    static long hoursSince(String iso) {
        if (iso == null || iso.length() < 19) return -1;
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
            f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date d = f.parse(iso.substring(0, 19));
            return d == null ? -1 : Math.max(0, (System.currentTimeMillis() - d.getTime()) / 3600000L);
        } catch (Exception e) {
            return -1;
        }
    }

    private String formatTimestamp(String isoString) {
        if (isoString == null || isoString.length() < 10) return "Baru saja";
        try {
            // contoh format singkat: 2026-10-04T04:28:32... -> 04 Okt 2026
            String datePart = isoString.substring(0, 10);
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                String y = parts[0];
                String m = parts[1];
                String d = parts[2];
                String[] months = {"Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"};
                int mIdx = Integer.parseInt(m) - 1;
                if (mIdx >= 0 && mIdx < 12) {
                    return d + " " + months[mIdx] + " " + y;
                }
            }
            return datePart;
        } catch (Exception e) {
            return "Baru saja";
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvNama;
        TextView tvEmail;
        TextView tvPhone;
        TextView tvTime;
        TextView tvCatatan;
        MaterialButton btnTolak;
        MaterialButton btnRevisi;
        MaterialButton btnSetujui;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_verif_avatar);
            tvNama = itemView.findViewById(R.id.tv_verif_nama);
            tvEmail = itemView.findViewById(R.id.tv_verif_email);
            tvPhone = itemView.findViewById(R.id.tv_verif_phone);
            tvTime = itemView.findViewById(R.id.tv_verif_time);
            tvCatatan = itemView.findViewById(R.id.tv_verif_catatan);
            btnTolak = itemView.findViewById(R.id.btn_verif_tolak);
            btnRevisi = itemView.findViewById(R.id.btn_verif_revisi);
            btnSetujui = itemView.findViewById(R.id.btn_verif_setujui);
        }
    }
}
