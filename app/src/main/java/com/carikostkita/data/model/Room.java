package com.carikostkita.data.model;

import java.io.Serializable;

public class Room implements Serializable {
    private long id;
    private long kostId;
    private String name;
    private String roomType;
    private double price;
    private String status;
    private String createdAt;

    public Room() {
        this.status = "TERSEDIA";
        this.roomType = "Standar";
    }

    public Room(long kostId, String name, String roomType, double price, String status) {
        this.kostId = kostId;
        this.name = name;
        this.roomType = roomType != null ? roomType : "Standar";
        this.price = price;
        this.status = status != null ? status : "TERSEDIA";
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

    public String getName() {
        return name != null ? name : "Kamar";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoomType() {
        return roomType != null ? roomType : "Standar";
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getStatus() {
        return status != null ? status : "TERSEDIA";
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

    public boolean isTersedia() {
        return "TERSEDIA".equalsIgnoreCase(getStatus());
    }

    public boolean isTerisi() {
        return "TERISI".equalsIgnoreCase(getStatus());
    }

    public boolean isBooking() {
        return "BOOKING".equalsIgnoreCase(getStatus());
    }

    public boolean isMaintenance() {
        return "MAINTENANCE".equalsIgnoreCase(getStatus());
    }
}
