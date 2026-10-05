package com.carikostkita.data.model;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    public static final int STATUS_SENDING = 0;
    public static final int STATUS_SENT = 1;
    public static final int STATUS_READ = 2;
    public static final int STATUS_FAILED = 3;

    private String id;
    private String chatId;
    private String senderId;
    private String namaSender;
    private String message;
    private String createdAt;
    private boolean isRead;
    private int status = STATUS_SENT;

    public ChatMessage() {}

    public ChatMessage(String chatId, String senderId, String message) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.message = message;
        this.status = STATUS_SENDING;
    }

    public ChatMessage(String id, String chatId, String senderId, String message, String createdAt, boolean isRead) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.message = message;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.status = isRead ? STATUS_READ : STATUS_SENT;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdMessage() {
        return getId();
    }

    public void setIdMessage(String id) {
        setId(id);
    }

    public String getChatId() {
        return chatId != null ? chatId : "";
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getIdConversation() {
        return getChatId();
    }

    public void setIdConversation(String chatId) {
        setChatId(chatId);
    }

    public String getSenderId() {
        return senderId != null ? senderId : "";
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getIdSender() {
        return getSenderId();
    }

    public void setIdSender(String senderId) {
        setSenderId(senderId);
    }

    public String getNamaSender() {
        return namaSender != null ? namaSender : "";
    }

    public void setNamaSender(String namaSender) {
        this.namaSender = namaSender;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageText() {
        return getMessage();
    }

    public void setMessageText(String messageText) {
        setMessage(messageText);
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getTimestamp() {
        return getCreatedAt();
    }

    public void setTimestamp(String timestamp) {
        setCreatedAt(timestamp);
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
        if (read && status != STATUS_FAILED) {
            status = STATUS_READ;
        }
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isMine(String currentUserId) {
        return currentUserId != null && currentUserId.equals(senderId);
    }
}
