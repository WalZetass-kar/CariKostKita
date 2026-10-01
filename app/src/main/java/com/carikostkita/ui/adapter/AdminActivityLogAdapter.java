package com.carikostkita.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.SystemActivityLog;
import java.util.ArrayList;
import java.util.List;

public class AdminActivityLogAdapter extends RecyclerView.Adapter<AdminActivityLogAdapter.ViewHolder> {

    private final List<SystemActivityLog> logList = new ArrayList<>();

    public void submitList(List<SystemActivityLog> list) {
        this.logList.clear();
        if (list != null) {
            this.logList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_activity_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SystemActivityLog log = logList.get(position);
        holder.tvAction.setText(log.getActionType());
        holder.tvDesc.setText(log.getDescription());
        holder.tvUser.setText("Oleh: " + (log.getUserName() != null ? log.getUserName() : "Sistem / User #" + log.getIdUser()));
        holder.tvTime.setText(log.getCreatedAt() != null ? log.getCreatedAt() : "");
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAction;
        TextView tvDesc;
        TextView tvUser;
        TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAction = itemView.findViewById(R.id.tv_log_action);
            tvDesc = itemView.findViewById(R.id.tv_log_desc);
            tvUser = itemView.findViewById(R.id.tv_log_user);
            tvTime = itemView.findViewById(R.id.tv_log_time);
        }
    }
}
