package com.carikostkita.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Model for Roommate search posting by students in Pekanbaru.
 */
public class RoommatePost implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("campus")
    private String campus;

    @SerializedName("faculty")
    private String faculty;

    @SerializedName("gender")
    private String gender; // "PUTRA" or "PUTRI"

    @SerializedName("budget")
    private int budget;

    @SerializedName("target_area")
    private String targetArea;

    @SerializedName("room_type")
    private String roomType;

    @SerializedName("description")
    private String description;

    @SerializedName("contact_whatsapp")
    private String contactWhatsapp;

    @SerializedName("is_available")
    private boolean isAvailable = true;

    @SerializedName("created_at")
    private String createdAt;

    public RoommatePost() {}

    public RoommatePost(String name, String campus, String faculty, String gender,
                        int budget, String targetArea, String roomType,
                        String description, String contactWhatsapp) {
        this.name = name;
        this.campus = campus;
        this.faculty = faculty;
        this.gender = gender;
        this.budget = budget;
        this.targetArea = targetArea;
        this.roomType = roomType;
        this.description = description;
        this.contactWhatsapp = contactWhatsapp;
        this.isAvailable = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getCampus() { return campus != null ? campus : ""; }
    public void setCampus(String campus) { this.campus = campus; }

    public String getFaculty() { return faculty != null ? faculty : ""; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public String getGender() { return gender != null ? gender : "PUTRA"; }
    public void setGender(String gender) { this.gender = gender; }

    public int getBudget() { return budget; }
    public void setBudget(int budget) { this.budget = budget; }

    public String getTargetArea() { return targetArea != null ? targetArea : ""; }
    public void setTargetArea(String targetArea) { this.targetArea = targetArea; }

    public String getRoomType() { return roomType != null ? roomType : ""; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public String getContactWhatsapp() { return contactWhatsapp != null ? contactWhatsapp : ""; }
    public void setContactWhatsapp(String contactWhatsapp) { this.contactWhatsapp = contactWhatsapp; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /**
     * Formats budget as IDR string, e.g. "Rp 450.000 / bln"
     */
    public String getFormattedBudget() {
        if (budget <= 0) return "Budget Fleksibel";
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        nf.setMaximumFractionDigits(0);
        return nf.format(budget) + " / bln";
    }

    /**
     * Formatted gender label, e.g. "👦 Putra" or "👧 Putri"
     */
    public String getGenderLabel() {
        if ("PUTRI".equalsIgnoreCase(gender)) {
            return "👧 Mahasiswi (Putri)";
        }
        return "👦 Mahasiswa (Putra)";
    }
}
