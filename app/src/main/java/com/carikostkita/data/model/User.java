package com.carikostkita.data.model;

import java.io.Serializable;

public class User implements Serializable {
    private int idUser;
    private String nama;
    private String email;
    private String password;
    private Role role;
    private String noHp;
    private String avatarUrl;
    private String bio;
    private VerificationStatus verificationStatus;
    private String pengajuanCatatan;
    private String catatanRevisi;
    private boolean isActive;
    private String authProvider; // "LOCAL" or "GOOGLE"
    private String createdAt;

    public User() {
        this.role = Role.USER;
        this.verificationStatus = VerificationStatus.NONE;
        this.isActive = true;
        this.authProvider = "LOCAL";
    }

    public User(int idUser, String nama, String email, String password, Role role, String createdAt) {
        this.idUser = idUser;
        this.nama = nama;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : Role.USER;
        this.verificationStatus = (role == Role.PEMILIK_KOST) ? VerificationStatus.APPROVED : VerificationStatus.NONE;
        this.isActive = true;
        this.authProvider = "LOCAL";
        this.createdAt = createdAt;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
        return authProvider != null ? authProvider : "LOCAL";
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
