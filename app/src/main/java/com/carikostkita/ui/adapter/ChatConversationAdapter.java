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
import com.carikostkita.data.model.ChatConversation;
import com.carikostkita.util.SessionManager;
import com.carikostkita.util.UserAvatarHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatConversationAdapter extends RecyclerView.Adapter<ChatConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(ChatConversation conversation);
    }

    private final Context context;
    private final boolean isOwner;
    private final String currentUserId;
    private final OnConversationClickListener listener;
    private final List<ChatConversation> conversationList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public ChatConversationAdapter(Context context, boolean isOwner, OnConversationClickListener listener) {
        this.context = context;
        this.isOwner = isOwner;
        this.currentUserId = new SessionManager(context).getUserUid();
        this.listener = listener;
    }

    public void setConversations(List<ChatConversation> list) {
        this.conversationList.clear();
        if (list != null) {
            this.conversationList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatConversation item = conversationList.get(position);

        String title;
        String roleTag;
        if (isOwner) {
            String pencari = (item.getNamaPencari() != null && !item.getNamaPencari().isEmpty())
                    ? item.getNamaPencari() : "Calon Penyewa";
            title = pencari;
            roleTag = "Calon Penyewa";
        } else {
            String pemilik = (item.getNamaPemilik() != null && !item.getNamaPemilik().isEmpty())
                    ? item.getNamaPemilik() : "Pemilik Kost";
            title = pemilik;
            roleTag = "Pemilik Kost";
        }
        holder.tvTitle.setText(title);
        holder.tvRoleTag.setText(roleTag);

        // Property Context
        String namaKost = item.getNamaKost();
        if (namaKost == null || namaKost.isEmpty()) namaKost = "Informasi Kost";
        holder.tvKostName.setText(namaKost);

        // Last Message & Sender Prefix
        String lastMsg = item.getLastMessage();
        if (lastMsg == null || lastMsg.isEmpty()) {
            lastMsg = "Belum ada pesan";
            holder.ivCheck.setVisibility(View.GONE);
            holder.tvLastMessage.setText(lastMsg);
        } else {
            boolean isMine = item.getLastMessageSenderId() != null && item.getLastMessageSenderId().equals(currentUserId);
            if (isMine) {
                holder.ivCheck.setVisibility(View.VISIBLE);
                holder.tvLastMessage.setText("Kamu: " + lastMsg);
            } else {
                holder.ivCheck.setVisibility(View.GONE);
                holder.tvLastMessage.setText(lastMsg);
            }
        }

        // Relative Human-Friendly Time
        String formattedTime = formatHumanTime(item.getLastMessageTime());
        holder.tvTime.setText(formattedTime);

        // Unread Badge
        int unread = item.getUnreadCount();
        if (unread > 0) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        // Dynamic Avatar Counterpart
        String avatarUrl = item.getAvatarLawan();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            UserAvatarHelper.loadAvatar(holder.ivAvatar, avatarUrl);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_nav_profile);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(item);
            }
        });
    }

    private String formatHumanTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty()) return "";
        try {
            Date date = null;
            if (rawTime.contains("T")) {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                String cleanIso = rawTime.length() >= 19 ? rawTime.substring(0, 19) : rawTime;
                date = isoFormat.parse(cleanIso);
            } else {
                date = parseFormat.parse(rawTime);
            }
            if (date != null) {
                long now = System.currentTimeMillis();
                long diff = now - date.getTime();
                if (diff < 60 * 1000) {
                    return "Baru saja";
                } else if (diff < 60 * 60 * 1000) {
                    long minutes = diff / (60 * 1000);
                    return minutes + " mnt lalu";
                } else if (diff < 24 * 60 * 60 * 1000) {
                    return timeFormat.format(date);
                } else if (diff < 48 * 60 * 60 * 1000) {
                    return "Kemarin";
                } else {
                    return new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date);
                }
            }
        } catch (Exception ignored) {
            if (rawTime.length() >= 16) {
                return rawTime.substring(11, 16);
            }
        }
        return rawTime;
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvTitle;
        TextView tvRoleTag;
        TextView tvKostName;
        ImageView ivCheck;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_chat_conv_avatar);
            tvTitle = itemView.findViewById(R.id.tv_chat_conv_title);
            tvRoleTag = itemView.findViewById(R.id.tv_chat_conv_role_tag);
            tvKostName = itemView.findViewById(R.id.tv_chat_conv_kost_name);
            ivCheck = itemView.findViewById(R.id.iv_chat_conv_check);
            tvLastMessage = itemView.findViewById(R.id.tv_chat_conv_last_message);
            tvTime = itemView.findViewById(R.id.tv_chat_conv_time);
            tvBadge = itemView.findViewById(R.id.tv_chat_conv_badge);
        }
    }
}
