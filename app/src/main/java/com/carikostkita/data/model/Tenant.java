package com.carikostkita.data.model;

import java.io.Serializable;

public class Tenant implements Serializable {
    private long id;
    private long kostId;
    private Long roomId;
    private String roomName;
    private String name;
    private String phone;
    private String startDate;
    private int dueDate;
    private double rentPrice;
    private String paymentStatus;
    private String notes;
    private String createdAt;

    public Tenant() {
        this.dueDate = 1;
        this.paymentStatus = "BELUM_BAYAR";
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

    public String getRoomName() {
        return roomName != null ? roomName : (roomId != null ? "Kamar #" + roomId : "-");
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getName() {
        return name != null ? name : "Penghuni";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone != null ? phone : "";
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStartDate() {
        return startDate != null ? startDate : "";
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public int getDueDate() {
        return dueDate > 0 ? dueDate : 1;
    }

    public void setDueDate(int dueDate) {
        this.dueDate = dueDate;
    }

    public double getRentPrice() {
        return rentPrice;
    }

    public void setRentPrice(double rentPrice) {
        this.rentPrice = rentPrice;
    }

    public String getPaymentStatus() {
        return paymentStatus != null ? paymentStatus : "BELUM_BAYAR";
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isLunas() {
        return "LUNAS".equalsIgnoreCase(getPaymentStatus());
    }

    public boolean isJatuhTempo() {
        return "JATUH_TEMPO".equalsIgnoreCase(getPaymentStatus());
    }
}
