package com.carikostkita.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.User;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class VerificationAdapter extends RecyclerView.Adapter<VerificationAdapter.ViewHolder> {

    public interface OnVerificationActionListener {
        void onApprove(User user, int position);
        void onRequireRevision(User user, int position);
        void onReject(User user, int position);
    }

    private final List<User> userList = new ArrayList<>();
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

        String catatan = user.getPengajuanCatatan();
        if (catatan != null && !catatan.isEmpty()) {
            holder.tvCatatan.setText("Info Pengajuan: " + catatan);
            holder.tvCatatan.setVisibility(View.VISIBLE);
        } else {
            holder.tvCatatan.setText("Info Pengajuan: Ingin mendaftar sebagai Pemilik Kost.");
            holder.tvCatatan.setVisibility(View.VISIBLE);
        }

        holder.btnSetujui.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(user, position);
        });

        holder.btnRevisi.setOnClickListener(v -> {
            if (listener != null) listener.onRequireRevision(user, position);
        });

        holder.btnTolak.setOnClickListener(v -> {
            if (listener != null) listener.onReject(user, position);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama;
        TextView tvEmail;
        TextView tvPhone;
        TextView tvCatatan;
        MaterialButton btnTolak;
        MaterialButton btnRevisi;
        MaterialButton btnSetujui;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNama = itemView.findViewById(R.id.tv_verif_nama);
            tvEmail = itemView.findViewById(R.id.tv_verif_email);
            tvPhone = itemView.findViewById(R.id.tv_verif_phone);
            tvCatatan = itemView.findViewById(R.id.tv_verif_catatan);
            btnTolak = itemView.findViewById(R.id.btn_verif_tolak);
            btnRevisi = itemView.findViewById(R.id.btn_verif_revisi);
            btnSetujui = itemView.findViewById(R.id.btn_verif_setujui);
        }
    }
}
