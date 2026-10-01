package com.carikostkita.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.carikostkita.data.model.VerificationStatus;

public class SessionManager {
    private static final String PREF_NAME = "carikostkita_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_AVATAR = "user_avatar";
    private static final String KEY_USER_BIO = "user_bio";
    private static final String KEY_VERIFICATION_STATUS = "verification_status";
    private static final String KEY_CATATAN_REVISI = "catatan_revisi";
    private static final String KEY_AUTH_PROVIDER = "auth_provider";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        this.pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
    }

    public void createLoginSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, user.getIdUser());
        editor.putString(KEY_USER_NAME, user.getNama());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_USER_ROLE, user.getRole() != null ? user.getRole().name() : Role.USER.name());
        editor.putString(KEY_USER_PHONE, user.getNoHp());
        editor.putString(KEY_USER_AVATAR, user.getAvatarUrl());
        editor.putString(KEY_USER_BIO, user.getBio());
        editor.putString(KEY_VERIFICATION_STATUS, user.getVerificationStatus() != null ? user.getVerificationStatus().name() : VerificationStatus.NONE.name());
        editor.putString(KEY_CATATAN_REVISI, user.getCatatanRevisi());
        editor.putString(KEY_AUTH_PROVIDER, user.getAuthProvider() != null ? user.getAuthProvider() : "LOCAL");
        editor.apply();
    }

    public void updateProfile(String name, String phone, String bio, String avatarUrl) {
        if (name != null) editor.putString(KEY_USER_NAME, name);
        if (phone != null) editor.putString(KEY_USER_PHONE, phone);
        if (bio != null) editor.putString(KEY_USER_BIO, bio);
        if (avatarUrl != null) editor.putString(KEY_USER_AVATAR, avatarUrl);
        editor.apply();
    }

    public void updateProfile(String name, String phone, String avatarUrl) {
        updateProfile(name, phone, getUserBio(), avatarUrl);
    }

    public void updateVerificationStatus(VerificationStatus status, Role newRole) {
        updateVerificationStatus(status, newRole, null);
    }

    public void updateVerificationStatus(VerificationStatus status, Role newRole, String catatanRevisi) {
        if (status != null) {
            editor.putString(KEY_VERIFICATION_STATUS, status.name());
        }
        if (newRole != null) {
            editor.putString(KEY_USER_ROLE, newRole.name());
        }
        if (catatanRevisi != null) {
            editor.putString(KEY_CATATAN_REVISI, catatanRevisi);
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "Pengguna");
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, "");
    }

    public String getUserAvatar() {
        return pref.getString(KEY_USER_AVATAR, "");
    }

    public String getUserBio() {
        return pref.getString(KEY_USER_BIO, "");
    }

    public Role getUserRole() {
        String roleStr = pref.getString(KEY_USER_ROLE, Role.USER.name());
        return Role.fromString(roleStr);
    }

    public VerificationStatus getVerificationStatus() {
        String statusStr = pref.getString(KEY_VERIFICATION_STATUS, VerificationStatus.NONE.name());
        return VerificationStatus.fromString(statusStr);
    }

    public String getCatatanRevisi() {
        return pref.getString(KEY_CATATAN_REVISI, "");
    }

    public boolean isGoogleAccount() {
        return "GOOGLE".equalsIgnoreCase(pref.getString(KEY_AUTH_PROVIDER, "LOCAL"));
    }

    public boolean isPemilikKost() {
        return getUserRole() == Role.PEMILIK_KOST;
    }

    public boolean isDeveloper() {
        return getUserRole() == Role.ADMIN;
    }

    public boolean isAdmin() {
        return getUserRole() == Role.ADMIN;
    }

    public boolean isPencariKost() {
        return getUserRole() == Role.USER;
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
