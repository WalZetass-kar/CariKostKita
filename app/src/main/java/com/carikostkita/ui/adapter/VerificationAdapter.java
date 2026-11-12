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
        holder.tvNama.setText(user.getNama() != null && !user.getNama().isEmpty() ? user.getNama() : "Pemohon Pemilik");
        holder.tvEmail.setText(user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "-");

        UserAvatarHelper.loadAvatar(holder.ivAvatar, user.getAvatarUrl());

        String rawNote = user.getPengajuanCatatan();
        if (rawNote == null || rawNote.trim().isEmpty()) {
            rawNote = user.getCatatanRevisi();
        }

        ParsedSubmission parsed = parseSubmissionNote(rawNote);
        String phoneDisplay = (user.getNoHp() != null && !user.getNoHp().trim().isEmpty())
                ? user.getNoHp().trim()
                : (!parsed.whatsapp.isEmpty() ? parsed.whatsapp : "Nomor WhatsApp belum diisi");
        holder.tvPhone.setText(phoneDisplay);

        holder.tvCatatan.setText(parsed.formattedSummary);
        holder.tvCatatan.setVisibility(View.VISIBLE);

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

    public static class ParsedSubmission {
        public String kode = "";
        public String namaKost = "";
        public String alamat = "";
        public String whatsapp = "";
        public String catatan = "";
        public int photoCount = 0;
        public String formattedSummary = "";
    }

    public static ParsedSubmission parseSubmissionNote(String rawNote) {
        ParsedSubmission res = new ParsedSubmission();
        if (rawNote == null || rawNote.trim().isEmpty()) {
            res.formattedSummary = "Permohonan verifikasi akun sebagai Mitra Pemilik Kost.";
            return res;
        }

        String cleaned = rawNote.trim().replaceFirst("(?i)^\\[verifikasi-properti\\]\\s*", "");
        if (cleaned.contains("|") || cleaned.toLowerCase().startsWith("kode:") || cleaned.toLowerCase().contains("nama kost:")) {
            String[] segments = cleaned.split("\\|");
            String buktiStatus = "";
            for (String seg : segments) {
                String part = seg.trim();
                int colon = part.indexOf(':');
                if (colon <= 0) continue;
                String key = part.substring(0, colon).trim().toLowerCase();
                String val = part.substring(colon + 1).trim();
                if (val.isEmpty()) continue;

                switch (key) {
                    case "kode":
                    case "kode verifikasi":
                        res.kode = val;
                        break;
                    case "nama kost":
                    case "nama properti":
                    case "properti":
                        res.namaKost = val;
                        break;
                    case "alamat":
                    case "alamat kost":
                        res.alamat = val;
                        break;
                    case "wa":
                    case "whatsapp":
                    case "no hp":
                        res.whatsapp = val;
                        break;
                    case "catatan":
                    case "keterangan":
                        res.catatan = val;
                        break;
                    case "foto properti":
                    case "foto kode":
                    case "foto kamar":
                        res.photoCount++;
                        break;
                    case "bukti foto":
                        buktiStatus = val;
                        break;
                }
            }

            StringBuilder sb = new StringBuilder();
            if (!res.namaKost.isEmpty()) {
                sb.append("Properti Kost: ").append(res.namaKost);
            }
            if (!res.alamat.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("Alamat Lokasi: ").append(res.alamat);
            }
            if (!res.kode.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("Kode Verifikasi: ").append(res.kode);
            }
            if (res.photoCount > 0) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(res.photoCount).append(" Foto Bukti Terlampir");
            } else if (!buktiStatus.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("Status Dokumen: ").append(buktiStatus);
            }
            if (!res.catatan.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("Catatan Pemohon: ").append(res.catatan);
            }

            res.formattedSummary = sb.length() > 0
                    ? sb.toString()
                    : "Permohonan verifikasi properti kost menunggu peninjauan.";
            return res;
        }

        // Plain note fallback: strip any raw URLs so raw links never leak into text summary
        String plain = cleaned.replaceAll("https?://\\S+", "").replaceAll("\\s{2,}", " ").trim();
        res.catatan = plain;
        res.formattedSummary = plain.isEmpty()
                ? "Permohonan verifikasi akun sebagai Mitra Pemilik Kost."
                : "Catatan Pengajuan: " + plain;
        return res;
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
