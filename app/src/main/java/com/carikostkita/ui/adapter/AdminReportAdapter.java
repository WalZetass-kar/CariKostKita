package com.carikostkita.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.KostReport;
import com.carikostkita.data.model.ReportStatus;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.ViewHolder> {

    public interface OnReportActionListener {
        void onResolve(KostReport report, int position);
        void onDismiss(KostReport report, int position);
    }

    private final List<KostReport> reportList = new ArrayList<>();
    private final OnReportActionListener listener;

    public AdminReportAdapter(OnReportActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<KostReport> list) {
        this.reportList.clear();
        if (list != null) {
            this.reportList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        KostReport report = reportList.get(position);
        holder.tvKostName.setText(report.getNamaKost() != null ? report.getNamaKost() : "Kost #" + report.getIdKost());
        holder.tvCategory.setText("Kategori: " + report.getAlasan());
        holder.tvDesc.setText("Deskripsi: " + report.getDeskripsi());
        holder.tvMeta.setText("Pelapor: " + report.getNamaPelapor() + " | Pemilik: " + report.getNamaPemilik());
        holder.tvDate.setText(report.getCreatedAt() != null ? report.getCreatedAt().substring(0, Math.min(10, report.getCreatedAt().length())) : "");

        ReportStatus status = report.getStatus();
        holder.tvStatus.setText(status.getDisplayName());
        if (status == ReportStatus.BARU) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_penuh);
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_penuh));
            holder.btnResolve.setVisibility(View.VISIBLE);
            holder.btnDismiss.setVisibility(View.VISIBLE);
        } else if (status == ReportStatus.SELESAI) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_tersedia);
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_tersedia));
            holder.btnResolve.setVisibility(View.GONE);
            holder.btnDismiss.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_campur);
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_campur));
            holder.btnResolve.setVisibility(View.GONE);
            holder.btnDismiss.setVisibility(View.GONE);
        }

        holder.btnResolve.setOnClickListener(v -> {
            if (listener != null) listener.onResolve(report, position);
        });

        holder.btnDismiss.setOnClickListener(v -> {
            if (listener != null) listener.onDismiss(report, position);
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvKostName;
        TextView tvCategory;
        TextView tvStatus;
        TextView tvDesc;
        TextView tvMeta;
        TextView tvDate;
        MaterialButton btnDismiss;
        MaterialButton btnResolve;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKostName = itemView.findViewById(R.id.tv_report_kost_name);
            tvCategory = itemView.findViewById(R.id.tv_report_category);
            tvStatus = itemView.findViewById(R.id.tv_report_status);
            tvDesc = itemView.findViewById(R.id.tv_report_desc);
            tvMeta = itemView.findViewById(R.id.tv_report_meta);
            tvDate = itemView.findViewById(R.id.tv_report_date);
            btnDismiss = itemView.findViewById(R.id.btn_report_tolak);
            btnResolve = itemView.findViewById(R.id.btn_report_selesai);
        }
    }
}
