package com.carikostkita.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.carikostkita.R;
import com.carikostkita.data.model.ChatConversation;
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
    private final OnConversationClickListener listener;
    private final List<ChatConversation> conversationList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public ChatConversationAdapter(Context context, boolean isOwner, OnConversationClickListener listener) {
        this.context = context;
        this.isOwner = isOwner;
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
        if (isOwner) {
            String pencari = (item.getNamaPencari() != null && !item.getNamaPencari().isEmpty()) ? item.getNamaPencari() : "Pencari Kost";
            title = pencari + " (" + item.getNamaKost() + ")";
        } else {
            String pemilik = (item.getNamaPemilik() != null && !item.getNamaPemilik().isEmpty()) ? item.getNamaPemilik() : "Pemilik";
            title = item.getNamaKost() + " (" + pemilik + ")";
        }
        holder.tvTitle.setText(title);

        String lastMsg = item.getLastMessage();
        if (lastMsg == null || lastMsg.isEmpty()) {
            lastMsg = "Belum ada pesan";
        }
        holder.tvLastMessage.setText(lastMsg);

        String formattedTime = formatTime(item.getLastMessageTime());
        holder.tvTime.setText(formattedTime);

        int unread = isOwner ? item.getUnreadCountPemilik() : item.getUnreadCountPencari();
        if (unread > 0) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(String.valueOf(unread));
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        String foto = item.getFotoKost();
        if (foto != null && !foto.isEmpty()) {
            Glide.with(context).load(foto).placeholder(R.drawable.ic_bed).into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_bed);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(item);
            }
        });
    }

    private String formatTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty()) return "";
        try {
            Date date = parseFormat.parse(rawTime);
            if (date != null) {
                return timeFormat.format(date);
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
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_chat_conv_avatar);
            tvTitle = itemView.findViewById(R.id.tv_chat_conv_title);
            tvLastMessage = itemView.findViewById(R.id.tv_chat_conv_last_message);
            tvTime = itemView.findViewById(R.id.tv_chat_conv_time);
            tvBadge = itemView.findViewById(R.id.tv_chat_conv_badge);
        }
    }
}
