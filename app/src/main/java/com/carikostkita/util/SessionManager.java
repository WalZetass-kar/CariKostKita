package com.carikostkita.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.carikostkita.data.model.VerificationStatus;
import com.carikostkita.data.remote.SupabaseClient;

public class SessionManager {
    private static final String PREF_NAME = "carikostkita_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_UID = "user_uid";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_TOKEN_EXPIRES_AT = "token_expires_at";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_AVATAR = "user_avatar";
    private static final String KEY_USER_BIO = "user_bio";
    private static final String KEY_VERIFICATION_STATUS = "verification_status";
    private static final String KEY_CATATAN_REVISI = "catatan_revisi";
    private static final String KEY_AUTH_PROVIDER = "auth_provider";
    private static final String KEY_IS_FIRST_TIME_LAUNCH = "is_first_time_launch";
    public static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        this.pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();

        // Restore Supabase token to client if active
        String token = getAccessToken();
        if (token != null && !token.isEmpty()) {
            SupabaseClient.getInstance().setAccessToken(token);
        }
    }

    public void createLoginSession(User user) {
        createLoginSession(user, null, null, 0);
    }

    public void createLoginSession(User user, String accessToken, String refreshToken, long expiresInSeconds) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        if (user.getUid() != null) {
            editor.putString(KEY_USER_UID, user.getUid());
        }
        editor.putString(KEY_USER_NAME, user.getNama());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_USER_ROLE, user.getRole() != null ? user.getRole().name() : Role.USER.name());
        editor.putString(KEY_USER_PHONE, user.getNoHp());
        editor.putString(KEY_USER_AVATAR, user.getAvatarUrl());
        editor.putString(KEY_USER_BIO, user.getBio());
        editor.putString(KEY_VERIFICATION_STATUS, user.getVerificationStatus() != null ? user.getVerificationStatus().name() : VerificationStatus.NONE.name());
        editor.putString(KEY_CATATAN_REVISI, user.getCatatanRevisi());
        editor.putString(KEY_AUTH_PROVIDER, user.getAuthProvider() != null ? user.getAuthProvider() : "EMAIL");

        if (accessToken != null && !accessToken.isEmpty()) {
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
            SupabaseClient.getInstance().setAccessToken(accessToken);
        }
        if (refreshToken != null && !refreshToken.isEmpty()) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        if (expiresInSeconds > 0) {
            long expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000);
            editor.putLong(KEY_TOKEN_EXPIRES_AT, expiresAt);
        }
        editor.apply();
    }

    public void saveTokens(String accessToken, String refreshToken, long expiresInSeconds) {
        if (accessToken != null) {
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
            SupabaseClient.getInstance().setAccessToken(accessToken);
        }
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        if (expiresInSeconds > 0) {
            long expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000);
            editor.putLong(KEY_TOKEN_EXPIRES_AT, expiresAt);
        }
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
        return pref.getBoolean(KEY_IS_LOGGED_IN, false) && getUserUid() != null && !getUserUid().isEmpty();
    }

    public String getUserUid() {
        return pref.getString(KEY_USER_UID, null);
    }

    /**
     * Legacy integer ID accessor.
     * Computes a deterministic hash from the UUID for legacy UI checks if needed.
     */
    public int getUserId() {
        String uid = getUserUid();
        return uid != null ? Math.abs(uid.hashCode()) : -1;
    }

    public String getAccessToken() {
        return pref.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return pref.getString(KEY_REFRESH_TOKEN, null);
    }

    public boolean isTokenExpired() {
        long expiresAt = pref.getLong(KEY_TOKEN_EXPIRES_AT, 0);
        return expiresAt > 0 && System.currentTimeMillis() >= expiresAt;
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

    public void setUserPhone(String phone) {
        editor.putString(KEY_USER_PHONE, phone);
        editor.apply();
    }

    public String getUserAvatar() {
        return pref.getString(KEY_USER_AVATAR, "");
    }

    public String getAvatarUrl() {
        return getUserAvatar();
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
        return "GOOGLE".equalsIgnoreCase(pref.getString(KEY_AUTH_PROVIDER, "EMAIL"));
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

    public boolean isFirstTimeLaunch() {
        return !isOnboardingCompleted() && pref.getBoolean(KEY_IS_FIRST_TIME_LAUNCH, true);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(KEY_IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.putBoolean(KEY_ONBOARDING_COMPLETED, !isFirstTime);
        editor.apply();
    }

    public boolean isOnboardingCompleted() {
        return pref.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        editor.putBoolean(KEY_ONBOARDING_COMPLETED, completed);
        editor.putBoolean(KEY_IS_FIRST_TIME_LAUNCH, !completed);
        editor.apply();
    }

    // --- Manajemen Lokasi Dinamis Pengguna ---
    public boolean hasAskedLocationPermission() {
        return pref.getBoolean("key_loc_permission_asked", false);
    }

    public void setAskedLocationPermission(boolean asked) {
        editor.putBoolean("key_loc_permission_asked", asked);
        editor.apply();
    }

    public boolean hasLocationSaved() {
        return !getUserSelectedLocationDisplay().isEmpty();
    }

    public String getUserSelectedLocationDisplay() {
        return pref.getString("key_loc_display", "");
    }

    public void setUserSelectedLocation(String city, String district, double lat, double lng, String display) {
        editor.putString("key_loc_city", city);
        editor.putString("key_loc_district", district);
        editor.putString("key_loc_lat", String.valueOf(lat));
        editor.putString("key_loc_lng", String.valueOf(lng));
        editor.putString("key_loc_display", display);
        editor.apply();
    }

    public String getUserSelectedCity() {
        return pref.getString("key_loc_city", "");
    }

    public String getUserSelectedDistrict() {
        return pref.getString("key_loc_district", "");
    }

    public double getUserSelectedLat() {
        String s = pref.getString("key_loc_lat", "0");
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double getUserSelectedLng() {
        String s = pref.getString("key_loc_lng", "0");
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public void clearUserSelectedLocation() {
        editor.remove("key_loc_city");
        editor.remove("key_loc_district");
        editor.remove("key_loc_lat");
        editor.remove("key_loc_lng");
        editor.remove("key_loc_display");
        editor.apply();
    }

    public void logout() {
        boolean onboardingDone = isOnboardingCompleted();
        boolean askedLoc = hasAskedLocationPermission();
        String city = getUserSelectedCity();
        String district = getUserSelectedDistrict();
        double lat = getUserSelectedLat();
        double lng = getUserSelectedLng();
        String locDisplay = getUserSelectedLocationDisplay();

        SupabaseClient.getInstance().clearToken();
        editor.clear();
        
        // Preserve device-level onboarding and location preferences
        editor.putBoolean(KEY_ONBOARDING_COMPLETED, onboardingDone);
        editor.putBoolean(KEY_IS_FIRST_TIME_LAUNCH, !onboardingDone);
        editor.putBoolean("key_loc_permission_asked", askedLoc);
        if (!locDisplay.isEmpty()) {
            editor.putString("key_loc_city", city);
            editor.putString("key_loc_district", district);
            editor.putString("key_loc_lat", String.valueOf(lat));
            editor.putString("key_loc_lng", String.valueOf(lng));
            editor.putString("key_loc_display", locDisplay);
        }
        editor.apply();
    }
}
