package com.carikostkita.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Tenant;
import com.carikostkita.util.FormatUtil;
import com.google.android.material.button.MaterialButton;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class OwnerTenantAdapter extends RecyclerView.Adapter<OwnerTenantAdapter.ViewHolder> {

    public interface OnTenantActionListener {
        void onToggleStatus(Tenant tenant, int position);
        void onDeleteTenant(Tenant tenant, int position);
    }

    private final List<Tenant> tenantList = new ArrayList<>();
    private final OnTenantActionListener listener;
    private String currentKostName = "Kost Kita";

    public OwnerTenantAdapter(OnTenantActionListener listener) {
        this.listener = listener;
    }

    public void setKostName(String name) {
        if (name != null && !name.isEmpty()) this.currentKostName = name;
    }

    public void submitList(List<Tenant> list) {
        this.tenantList.clear();
        if (list != null) {
            this.tenantList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_owner_tenant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tenant tenant = tenantList.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvName.setText(tenant.getName());
        holder.tvRoom.setText(tenant.getRoomName() + " | Sewa: " + FormatUtil.formatRupiah((long) tenant.getRentPrice()) + " / bln");
        holder.tvDueDate.setText("Jatuh tempo setiap tanggal " + tenant.getDueDate() + " tiap bulan" + (!tenant.getPhone().isEmpty() ? " | " + tenant.getPhone() : ""));

        String status = tenant.getPaymentStatus().toUpperCase();
        holder.tvStatus.setText(status);

        if ("LUNAS".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_tersedia);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_tersedia));
            holder.btnSendWa.setVisibility(View.GONE);
        } else if ("JATUH_TEMPO".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_penuh);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_penuh));
            holder.btnSendWa.setVisibility(View.VISIBLE);
        } else {
            // BELUM_BAYAR
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_campur);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.badge_campur));
            holder.btnSendWa.setVisibility(View.VISIBLE);
        }

        holder.btnSendWa.setOnClickListener(v -> {
            String phone = tenant.getPhone().trim().replaceAll("[^0-9]", "");
            if (phone.isEmpty()) {
                Toast.makeText(ctx, "Nomor HP penghuni belum diisi.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.startsWith("0")) {
                phone = "62" + phone.substring(1);
            }
            try {
                String message = "Halo " + tenant.getName() + ", kami dari pengelola " + currentKostName
                        + " mengingatkan tagihan sewa kamar " + tenant.getRoomName()
                        + " sebesar " + FormatUtil.formatRupiah((long) tenant.getRentPrice())
                        + " jatuh tempo pada tanggal " + tenant.getDueDate() + ". Mohon konfirmasikan jika sudah melakukan transfer. Terima kasih 🙏";
                String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + URLEncoder.encode(message, "UTF-8");
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                ctx.startActivity(i);
            } catch (Exception e) {
                Toast.makeText(ctx, "Gagal membuka WhatsApp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnToggleStatus.setOnClickListener(v -> {
            if (listener != null) listener.onToggleStatus(tenant, position);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteTenant(tenant, position);
        });
    }

    @Override
    public int getItemCount() {
        return tenantList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvStatus;
        TextView tvRoom;
        TextView tvDueDate;
        MaterialButton btnSendWa;
        MaterialButton btnToggleStatus;
        MaterialButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_tenant_name);
            tvStatus = itemView.findViewById(R.id.tv_tenant_status);
            tvRoom = itemView.findViewById(R.id.tv_tenant_room);
            tvDueDate = itemView.findViewById(R.id.tv_tenant_due_date);
            btnSendWa = itemView.findViewById(R.id.btn_tenant_send_wa);
            btnToggleStatus = itemView.findViewById(R.id.btn_tenant_toggle_status);
            btnDelete = itemView.findViewById(R.id.btn_tenant_delete);
        }
    }
}
