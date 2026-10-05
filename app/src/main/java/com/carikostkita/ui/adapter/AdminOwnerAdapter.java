package com.carikostkita.ui.adapter;

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
import com.carikostkita.data.model.User;
import com.carikostkita.data.model.VerificationStatus;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class AdminOwnerAdapter extends RecyclerView.Adapter<AdminOwnerAdapter.OwnerViewHolder> {

    public interface OnOwnerActionListener {
        void onEditOwner(User user, int position);
        void onDeleteOwner(User user, int position);
    }

    private final List<User> ownerList = new ArrayList<>();
    private final OnOwnerActionListener listener;

    public AdminOwnerAdapter(OnOwnerActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<User> list) {
        ownerList.clear();
        if (list != null) {
            ownerList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < ownerList.size()) {
            ownerList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, ownerList.size());
        }
    }

    public void removeItem(User user) {
        if (user == null) return;
        int idx = -1;
        for (int i = 0; i < ownerList.size(); i++) {
            if (user.getUid().equals(ownerList.get(i).getUid())) {
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
    public OwnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_owner, parent, false);
        return new OwnerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OwnerViewHolder holder, int position) {
        User user = ownerList.get(position);
        holder.tvName.setText(user.getNama() != null && !user.getNama().isEmpty() ? user.getNama() : "Tanpa Nama");
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "-");

        String phone = user.getNoHp();
        if (phone != null && !phone.isEmpty()) {
            holder.tvPhone.setText("WA: " + phone);
        } else {
            holder.tvPhone.setText("WA: Belum diatur");
        }

        // Status Verifikasi Badge
        if (user.getVerificationStatus() == VerificationStatus.APPROVED) {
            holder.tvVerifBadge.setText("Terverifikasi");
            holder.tvVerifBadge.setBackgroundResource(R.drawable.bg_badge_tersedia);
            holder.tvVerifBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_tersedia));
        } else if (user.getVerificationStatus() == VerificationStatus.PENDING) {
            holder.tvVerifBadge.setText("Pending");
            holder.tvVerifBadge.setBackgroundResource(R.drawable.bg_badge_campur);
            holder.tvVerifBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_dark));
        } else {
            holder.tvVerifBadge.setText("Non-Aktif");
            holder.tvVerifBadge.setBackgroundResource(R.drawable.bg_badge_penuh);
            holder.tvVerifBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_penuh));
        }

        // Avatar
        String avatar = user.getAvatarUrl();
        if (avatar != null && (avatar.startsWith("http://") || avatar.startsWith("https://") || avatar.startsWith("file://"))) {
            Glide.with(holder.itemView.getContext())
                    .load(avatar)
                    .placeholder(R.drawable.ic_avatar_owner)
                    .error(R.drawable.ic_avatar_owner)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_avatar_owner);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditOwner(user, holder.getAdapterPosition());
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteOwner(user, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return ownerList.size();
    }

    static class OwnerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvEmail, tvPhone, tvRole, tvVerifBadge;
        MaterialButton btnEdit, btnDelete;

        public OwnerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_admin_owner_avatar);
            tvName = itemView.findViewById(R.id.tv_admin_owner_name);
            tvEmail = itemView.findViewById(R.id.tv_admin_owner_email);
            tvPhone = itemView.findViewById(R.id.tv_admin_owner_phone);
            tvRole = itemView.findViewById(R.id.tv_admin_owner_role);
            tvVerifBadge = itemView.findViewById(R.id.tv_admin_owner_verif_badge);
            btnEdit = itemView.findViewById(R.id.btn_admin_edit_owner);
            btnDelete = itemView.findViewById(R.id.btn_admin_delete_owner);
        }
    }
}
