package com.carikostkita.data.model;

import java.io.Serializable;

public class MaintenanceTicket implements Serializable {
    private long id;
    private long kostId;
    private Long roomId;
    private String tenantName;
    private String category;
    private String title;
    private String description;
    private String status;
    private String createdAt;

    public MaintenanceTicket() {
        this.status = "BARU";
        this.category = "FASILITAS";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getKostId() {
        return kostId;
    }

    public void setKostId(long kostId) {
        this.kostId = kostId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getTenantName() {
        return tenantName != null ? tenantName : "Penghuni";
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getCategory() {
        return category != null ? category : "FASILITAS";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title != null ? title : "Komplain";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status != null ? status : "BARU";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isBaru() {
        return "BARU".equalsIgnoreCase(getStatus());
    }

    public boolean isProses() {
        return "PROSES".equalsIgnoreCase(getStatus());
    }

    public boolean isSelesai() {
        return "SELESAI".equalsIgnoreCase(getStatus());
    }
}
