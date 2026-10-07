package com.carikostkita.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    public interface OnUserActionListener {
        default void onChangeRole(User user, int position) {}
        void onToggleStatus(User user, int position);
        void onDeleteUser(User user, int position);
    }

    private final List<User> userList = new ArrayList<>();
    private final OnUserActionListener listener;

    public AdminUserAdapter(OnUserActionListener listener) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvNama.setText(user.getNama());
        holder.tvEmail.setText(user.getEmail());
        holder.tvPhone.setText(user.getNoHp() != null && !user.getNoHp().isEmpty() ? user.getNoHp() : "No HP tidak diisi");

        // Role Badge
        if (user.getRole() == Role.MODERATOR) {
            holder.tvRole.setText("Moderator");
            holder.tvRole.setBackgroundResource(R.drawable.bg_badge_putri);
            holder.tvRole.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_putri));
        } else if (user.getRole() == Role.ADMIN) {
            holder.tvRole.setText("Super Admin");
            holder.tvRole.setBackgroundResource(R.drawable.bg_badge_putra);
            holder.tvRole.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_putra));
        } else if (user.getRole() == Role.PEMILIK_KOST) {
            holder.tvRole.setText("Pemilik Kost");
            holder.tvRole.setBackgroundResource(R.drawable.bg_badge_tersedia);
            holder.tvRole.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_tersedia));
        } else {
            holder.tvRole.setText("Pencari Kost");
            holder.tvRole.setBackgroundResource(R.drawable.bg_badge_campur);
            holder.tvRole.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_campur));
        }

        // Active Status
        if (user.isActive()) {
            holder.tvStatus.setText("Aktif");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_tersedia);
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_tersedia));
            holder.btnToggleStatus.setText("Tangguhkan");
            holder.btnToggleStatus.setTextColor(0xFFDC2626);
            holder.btnToggleStatus.setStrokeColorResource(R.color.status_penuh);
        } else {
            holder.tvStatus.setText("Ditangguhkan");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_penuh);
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_penuh));
            holder.btnToggleStatus.setText("Aktifkan Kembali");
            holder.btnToggleStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
            holder.btnToggleStatus.setStrokeColorResource(R.color.primary);
        }

        // Peran: ketuk badge untuk mengubah (hanya Super Admin; dicek lagi di server)
        holder.tvRole.setOnClickListener(v -> {
            if (listener != null) listener.onChangeRole(user, position);
        });

        // Akun staf tidak bisa ditangguhkan/dihapus dari daftar ini
        if (user.getRole() != null && user.getRole().isDeveloper()) {
            holder.btnToggleStatus.setVisibility(View.GONE);
            holder.btnDeleteUser.setVisibility(View.GONE);
        } else {
            holder.btnToggleStatus.setVisibility(View.VISIBLE);
            holder.btnDeleteUser.setVisibility(View.VISIBLE);
            holder.btnToggleStatus.setOnClickListener(v -> {
                if (listener != null) listener.onToggleStatus(user, position);
            });
            holder.btnDeleteUser.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteUser(user, position);
            });
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama;
        TextView tvEmail;
        TextView tvPhone;
        TextView tvRole;
        TextView tvStatus;
        MaterialButton btnToggleStatus;
        MaterialButton btnDeleteUser;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNama = itemView.findViewById(R.id.tv_admin_user_name);
            tvEmail = itemView.findViewById(R.id.tv_admin_user_email);
            tvPhone = itemView.findViewById(R.id.tv_admin_user_phone);
            tvRole = itemView.findViewById(R.id.tv_admin_user_role);
            tvStatus = itemView.findViewById(R.id.tv_admin_user_status);
            btnToggleStatus = itemView.findViewById(R.id.btn_admin_toggle_user_status);
            btnDeleteUser = itemView.findViewById(R.id.btn_admin_delete_user);
        }
    }
}
