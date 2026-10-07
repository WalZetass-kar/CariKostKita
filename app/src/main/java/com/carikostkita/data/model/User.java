package com.carikostkita.data.model;

import java.io.Serializable;

public class User implements Serializable {
    private String uid;
    private String nama;
    private String email;
    private Role role;
    private String noHp;
    private String avatarUrl;
    private String bio;
    private VerificationStatus verificationStatus;
    private String pengajuanCatatan;
    private String catatanRevisi;
    private boolean isActive;
    private String authProvider; // "EMAIL" or "GOOGLE"
    private String createdAt;
    private String pengajuanAt;

    public String getPengajuanAt() {
        return pengajuanAt;
    }

    public void setPengajuanAt(String pengajuanAt) {
        this.pengajuanAt = pengajuanAt;
    }

    public User() {
        this.role = Role.USER;
        this.verificationStatus = VerificationStatus.NONE;
        this.isActive = true;
        this.authProvider = "EMAIL";
    }

    public User(String uid, String nama, String email, Role role, String createdAt) {
        this.uid = uid;
        this.nama = nama;
        this.email = email;
        this.role = role != null ? role : Role.USER;
        this.verificationStatus = (role == Role.PEMILIK_KOST) ? VerificationStatus.APPROVED : VerificationStatus.NONE;
        this.isActive = true;
        this.authProvider = "EMAIL";
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getId() {
        return uid;
    }

    public void setId(String id) {
        this.uid = id;
    }

    public String getIdUser() {
        return uid;
    }

    public void setIdUser(String idUser) {
        this.uid = idUser;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role != null ? role : Role.USER;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getNoHp() {
        return noHp != null ? noHp : "";
    }

    public void setNoHp(String noHp) {
        this.noHp = noHp;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio != null ? bio : "";
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus != null ? verificationStatus : VerificationStatus.NONE;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getPengajuanCatatan() {
        return pengajuanCatatan != null ? pengajuanCatatan : "";
    }

    public void setPengajuanCatatan(String pengajuanCatatan) {
        this.pengajuanCatatan = pengajuanCatatan;
    }

    public String getAuthProvider() {
        return authProvider != null ? authProvider : "EMAIL";
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public boolean isGoogleAccount() {
        return "GOOGLE".equalsIgnoreCase(this.authProvider);
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCatatanRevisi() {
        return catatanRevisi != null ? catatanRevisi : "";
    }

    public void setCatatanRevisi(String catatanRevisi) {
        this.catatanRevisi = catatanRevisi;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isPemilikKost() {
        return role != null && role.isPemilikKost();
    }

    public boolean isDeveloper() {
        return role != null && role.isDeveloper();
    }

    public boolean isAdmin() {
        return role != null && role.isAdmin();
    }
}
