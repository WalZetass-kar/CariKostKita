package com.carikostkita.data.model;

import java.io.Serializable;

public class ChatConversation implements Serializable {
    private int idConversation;
    private int idKost;
    private String namaKost;
    private String thumbnailKost;
    private int idPencari;
    private String namaPencari;
    private int idPemilik;
    private String namaPemilik;
    private String lastMessage;
    private String lastMessageTime;
    private int unreadCount;

    public ChatConversation() {}

    public int getIdConversation() {
        return idConversation;
    }

    public void setIdConversation(int idConversation) {
        this.idConversation = idConversation;
    }

    public int getIdKost() {
        return idKost;
    }

    public void setIdKost(int idKost) {
        this.idKost = idKost;
    }

    public String getNamaKost() {
        return namaKost != null ? namaKost : "Kost Pekanbaru";
    }

    public void setNamaKost(String namaKost) {
        this.namaKost = namaKost;
    }

    public String getThumbnailKost() {
        return thumbnailKost;
    }

    public void setThumbnailKost(String thumbnailKost) {
        this.thumbnailKost = thumbnailKost;
    }

    public String getFotoKost() {
        return getThumbnailKost();
    }

    public void setFotoKost(String fotoKost) {
        setThumbnailKost(fotoKost);
    }

    public int getIdPencari() {
        return idPencari;
    }

    public void setIdPencari(int idPencari) {
        this.idPencari = idPencari;
    }

    public String getNamaPencari() {
        return namaPencari != null ? namaPencari : "Pencari Kost";
    }

    public void setNamaPencari(String namaPencari) {
        this.namaPencari = namaPencari;
    }

    public int getIdPemilik() {
        return idPemilik;
    }

    public void setIdPemilik(int idPemilik) {
        this.idPemilik = idPemilik;
    }

    public String getNamaPemilik() {
        return namaPemilik != null ? namaPemilik : "Pemilik Kost";
    }

    public void setNamaPemilik(String namaPemilik) {
        this.namaPemilik = namaPemilik;
    }

    public String getLastMessage() {
        return lastMessage != null ? lastMessage : "";
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime != null ? lastMessageTime : "";
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getUnreadCountPemilik() {
        return unreadCount;
    }

    public void setUnreadCountPemilik(int count) {
        this.unreadCount = count;
    }

    public int getUnreadCountPencari() {
        return unreadCount;
    }

    public void setUnreadCountPencari(int count) {
        this.unreadCount = count;
    }

    public String getTargetName(int currentUserId) {
        if (currentUserId == idPencari) {
            return getNamaPemilik();
        } else {
            return getNamaPencari();
        }
    }
}
