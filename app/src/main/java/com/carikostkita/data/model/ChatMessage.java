package com.carikostkita.data.model;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private int idMessage;
    private int idConversation;
    private int idSender;
    private String namaSender;
    private String message;
    private String createdAt;
    private boolean isRead;

    public ChatMessage() {}

    public ChatMessage(int idConversation, int idSender, String message) {
        this.idConversation = idConversation;
        this.idSender = idSender;
        this.message = message;
    }

    public ChatMessage(int idMessage, int idConversation, int idSender, String message, String createdAt, boolean isRead) {
        this.idMessage = idMessage;
        this.idConversation = idConversation;
        this.idSender = idSender;
        this.message = message;
        this.createdAt = createdAt;
        this.isRead = isRead;
    }

    public int getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(int idMessage) {
        this.idMessage = idMessage;
    }

    public int getIdConversation() {
        return idConversation;
    }

    public void setIdConversation(int idConversation) {
        this.idConversation = idConversation;
    }

    public int getIdSender() {
        return idSender;
    }

    public void setIdSender(int idSender) {
        this.idSender = idSender;
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
    }

    public boolean isMine(int currentUserId) {
        return idSender == currentUserId;
    }
}
