package com.carikostkita.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.MaintenanceTicket;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class OwnerTicketAdapter extends RecyclerView.Adapter<OwnerTicketAdapter.ViewHolder> {

    public interface OnTicketActionListener {
        void onResolveTicket(MaintenanceTicket ticket, int position);
    }

    private final List<MaintenanceTicket> ticketList = new ArrayList<>();
    private final OnTicketActionListener listener;

    public OwnerTicketAdapter(OnTicketActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<MaintenanceTicket> list) {
        this.ticketList.clear();
        if (list != null) {
            this.ticketList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_owner_ticket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaintenanceTicket ticket = ticketList.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvTitle.setText(ticket.getTitle());
        holder.tvMeta.setText("Pelapor: " + ticket.getTenantName() + " | Kategori: " + ticket.getCategory());
        holder.tvDesc.setText(ticket.getDescription().isEmpty() ? "Tidak ada rincian tambahan." : ticket.getDescription());
        holder.tvDate.setText("Tgl: " + (ticket.getCreatedAt().length() >= 10 ? ticket.getCreatedAt().substring(0, 10) : ticket.getCreatedAt()));

        String status = ticket.getStatus().toUpperCase();
        holder.tvStatus.setText(status);

        if ("SELESAI".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_tersedia);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_tersedia));
            holder.btnAction.setVisibility(View.GONE);
        } else if ("PROSES".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_campur);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.badge_campur));
            holder.btnAction.setText("Tandai Selesai");
            holder.btnAction.setVisibility(View.VISIBLE);
        } else {
            // BARU
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_penuh);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_penuh));
            holder.btnAction.setText("Tindaklanjuti");
            holder.btnAction.setVisibility(View.VISIBLE);
        }

        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) listener.onResolveTicket(ticket, position);
        });
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvStatus;
        TextView tvMeta;
        TextView tvDesc;
        TextView tvDate;
        MaterialButton btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_ticket_title);
            tvStatus = itemView.findViewById(R.id.tv_ticket_status);
            tvMeta = itemView.findViewById(R.id.tv_ticket_meta);
            tvDesc = itemView.findViewById(R.id.tv_ticket_desc);
            tvDate = itemView.findViewById(R.id.tv_ticket_date);
            btnAction = itemView.findViewById(R.id.btn_ticket_action);
        }
    }
}
