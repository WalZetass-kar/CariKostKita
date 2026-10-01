package com.carikostkita.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;

public class SessionManager {
    private static final String PREF_NAME = "carikostkita_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_AVATAR = "user_avatar";

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
        editor.apply();
    }

    public void updateProfile(String name, String phone, String avatarUrl) {
        if (name != null) editor.putString(KEY_USER_NAME, name);
        if (phone != null) editor.putString(KEY_USER_PHONE, phone);
        if (avatarUrl != null) editor.putString(KEY_USER_AVATAR, avatarUrl);
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

    public Role getUserRole() {
        String roleStr = pref.getString(KEY_USER_ROLE, Role.USER.name());
        return Role.fromString(roleStr);
    }

    public boolean isPemilikKost() {
        Role role = getUserRole();
        return role == Role.PEMILIK_KOST || role == Role.ADMIN;
    }

    public boolean isDeveloper() {
        return getUserRole() == Role.ADMIN;
    }

    public boolean isAdmin() {
        return isPemilikKost();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
