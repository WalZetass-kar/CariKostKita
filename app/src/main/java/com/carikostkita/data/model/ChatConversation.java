package com.carikostkita.data.model;

import java.io.Serializable;

public class ChatConversation implements Serializable {
    private String id;
    private String kostId;
    private String namaKost;
    private String thumbnailKost;
    private String pencariId;
    private String namaPencari;
    private String ownerId;
    private String namaPemilik;
    private String lastMessage;
    private String lastMessageTime;
    private String lastMessageSenderId;
    private String avatarLawan;
    private String roleLawan;
    private int unreadCount;

    public ChatConversation() {}

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdConversation() {
        return getId();
    }

    public void setIdConversation(String id) {
        setId(id);
    }

    public String getKostId() {
        return kostId != null ? kostId : "";
    }

    public void setKostId(String kostId) {
        this.kostId = kostId;
    }

    public String getIdKost() {
        return getKostId();
    }

    public void setIdKost(String kostId) {
        setKostId(kostId);
    }

    public String getNamaKost() {
        return namaKost != null ? namaKost : "Kost Pekanbaru";
    }

    public void setNamaKost(String namaKost) {
        this.namaKost = namaKost;
    }

    public String getThumbnailKost() {
        return thumbnailKost != null ? thumbnailKost : "";
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

    public String getPencariId() {
        return pencariId != null ? pencariId : "";
    }

    public void setPencariId(String pencariId) {
        this.pencariId = pencariId;
    }

    public String getIdPencari() {
        return getPencariId();
    }

    public void setIdPencari(String idPencari) {
        setPencariId(idPencari);
    }

    public String getNamaPencari() {
        return namaPencari != null ? namaPencari : "Pencari Kost";
    }

    public void setNamaPencari(String namaPencari) {
        this.namaPencari = namaPencari;
    }

    public String getOwnerId() {
        return ownerId != null ? ownerId : "";
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getIdPemilik() {
        return getOwnerId();
    }

    public void setIdPemilik(String ownerId) {
        setOwnerId(ownerId);
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

    public String getLastMessageSenderId() {
        return lastMessageSenderId != null ? lastMessageSenderId : "";
    }

    public void setLastMessageSenderId(String senderId) {
        this.lastMessageSenderId = senderId;
    }

    public String getAvatarLawan() {
        return avatarLawan != null ? avatarLawan : "";
    }

    public void setAvatarLawan(String avatarLawan) {
        this.avatarLawan = avatarLawan;
    }

    public String getRoleLawan() {
        return roleLawan != null ? roleLawan : "";
    }

    public void setRoleLawan(String roleLawan) {
        this.roleLawan = roleLawan;
    }

    public String getTargetName(String currentUserId) {
        if (currentUserId != null && currentUserId.equals(pencariId)) {
            return getNamaPemilik();
        } else {
            return getNamaPencari();
        }
    }
}
