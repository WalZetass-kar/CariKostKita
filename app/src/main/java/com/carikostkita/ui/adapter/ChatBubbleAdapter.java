package com.carikostkita.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    public interface OnMessageRetryListener {
        void onRetry(ChatMessage message);
    }

    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_INCOMING = 2;

    private final String currentUserId;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private OnMessageRetryListener retryListener;

    public ChatBubbleAdapter(String currentUserId) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
    }

    public ChatBubbleAdapter(int legacyCurrentUserId) {
        this(String.valueOf(legacyCurrentUserId));
    }

    public void setOnMessageRetryListener(OnMessageRetryListener listener) {
        this.retryListener = listener;
    }

    public void setMessages(List<ChatMessage> list) {
        this.messageList.clear();
        if (list != null) {
            this.messageList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void removeMessage(ChatMessage message) {
        int index = messageList.indexOf(message);
        if (index >= 0) {
            messageList.remove(index);
            notifyItemRemoved(index);
        }
    }

    public void addMessage(ChatMessage message) {
        this.messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    public void updateMessage(ChatMessage updated) {
        if (updated == null) return;
        for (int i = messageList.size() - 1; i >= 0; i--) {
            ChatMessage item = messageList.get(i);
            if ((item.getId() != null && item.getId().equals(updated.getId())) ||
                (item.getSenderId() != null && item.getSenderId().equals(updated.getSenderId()) &&
                 item.getMessageText() != null && item.getMessageText().equals(updated.getMessageText()))) {
                messageList.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void updateMessageRead(String messageId) {
        if (messageId == null || messageId.isEmpty()) return;
        for (int i = 0; i < messageList.size(); i++) {
            ChatMessage item = messageList.get(i);
            if (messageId.equals(item.getId())) {
                item.setRead(true);
                item.setStatus(ChatMessage.STATUS_READ);
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void updateAllMineRead() {
        boolean changed = false;
        for (int i = 0; i < messageList.size(); i++) {
            ChatMessage item = messageList.get(i);
            if (item.isMine(currentUserId) && !item.isRead()) {
                item.setRead(true);
                item.setStatus(ChatMessage.STATUS_READ);
                changed = true;
            }
        }
        if (changed) {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messageList.get(position);
        if (msg.isMine(currentUserId)) {
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

            if (vh.ivStatus != null) {
                int status = msg.getStatus();
                if (status == ChatMessage.STATUS_SENDING) {
                    vh.ivStatus.setImageResource(R.drawable.ic_clock);
                    vh.ivStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D8D2BC")));
                    vh.itemView.setOnClickListener(null);
                } else if (status == ChatMessage.STATUS_READ || msg.isRead()) {
                    vh.ivStatus.setImageResource(R.drawable.ic_check_double);
                    vh.ivStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
                    vh.itemView.setOnClickListener(null);
                } else if (status == ChatMessage.STATUS_FAILED) {
                    vh.ivStatus.setImageResource(R.drawable.ic_error_circle);
                    vh.ivStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                    vh.itemView.setOnClickListener(v -> {
                        if (retryListener != null) {
                            retryListener.onRetry(msg);
                        }
                    });
                } else { // STATUS_SENT
                    vh.ivStatus.setImageResource(R.drawable.ic_check);
                    vh.ivStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#EEEAD7")));
                    vh.itemView.setOnClickListener(null);
                }
            }
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
        ImageView ivStatus;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_chat_outgoing_message);
            tvTime = itemView.findViewById(R.id.tv_chat_outgoing_time);
            ivStatus = itemView.findViewById(R.id.iv_chat_outgoing_status);
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
