package com.carikostkita.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatBubbleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_INCOMING = 2;

    private final int currentUserId;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public ChatBubbleAdapter(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<ChatMessage> list) {
        this.messageList.clear();
        if (list != null) {
            this.messageList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        this.messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messageList.get(position);
        if (msg.getIdSender() == currentUserId) {
            return TYPE_OUTGOING;
        } else {
            return TYPE_INCOMING;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_OUTGOING) {
            View view = inflater.inflate(R.layout.item_chat_outgoing, parent, false);
            return new OutgoingViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_incoming, parent, false);
            return new IncomingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messageList.get(position);
        String displayTime = formatTime(msg.getTimestamp());

        if (holder instanceof OutgoingViewHolder) {
            OutgoingViewHolder vh = (OutgoingViewHolder) holder;
            vh.tvMessage.setText(msg.getMessageText());
            vh.tvTime.setText(displayTime);
        } else if (holder instanceof IncomingViewHolder) {
            IncomingViewHolder vh = (IncomingViewHolder) holder;
            vh.tvMessage.setText(msg.getMessageText());
            vh.tvTime.setText(displayTime);
        }
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
        return messageList.size();
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_chat_outgoing_message);
            tvTime = itemView.findViewById(R.id.tv_chat_outgoing_time);
        }
    }

    static class IncomingViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;

        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_chat_incoming_message);
            tvTime = itemView.findViewById(R.id.tv_chat_incoming_time);
        }
    }
}
