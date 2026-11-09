package com.carikostkita.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.AppNotification;
import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification, int position);
    }

    private final List<AppNotification> list = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AppNotification> items) {
        this.list.clear();
        if (items != null) {
            this.list.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification notif = list.get(position);

        holder.tvTitle.setText(notif.getTitle());
        holder.tvMessage.setText(notif.getMessage());
        holder.tvDate.setText(notif.getCreatedAt().length() >= 10 ? notif.getCreatedAt().substring(0, 10) : notif.getCreatedAt());

        if (notif.isRead()) {
            holder.dotUnread.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.75f);
        } else {
            holder.dotUnread.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);
        }

        // Icon based on type
        String type = notif.getType().toUpperCase();
        if (type.contains("VERIF")) {
            holder.ivIcon.setImageResource(R.drawable.ic_verified);
        } else if (type.contains("LAPOR") || type.contains("REPORT")) {
            holder.ivIcon.setImageResource(R.drawable.ic_warning);
        } else if (type.contains("TAGIH") || type.contains("SEWA")) {
            holder.ivIcon.setImageResource(R.drawable.ic_nav_home);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_bell);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(notif, position);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvMessage;
        TextView tvDate;
        View dotUnread;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_notif_icon);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            tvDate = itemView.findViewById(R.id.tv_notif_date);
            dotUnread = itemView.findViewById(R.id.dot_notif_unread);
        }
    }
}
