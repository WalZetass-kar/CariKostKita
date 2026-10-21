package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.carikostkita.data.model.VerificationStatus;
import com.carikostkita.data.remote.FastApiAuthService;
import com.carikostkita.data.remote.FastApiProfileService;
import com.carikostkita.data.remote.FastApiUserLookupService;
import com.carikostkita.data.remote.FastApiOwnerVerificationService;
import com.carikostkita.data.remote.FastApiAdminUserService;
import com.carikostkita.data.remote.FastApiAvatarService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.data.remote.dto.AuthRequest;
import com.carikostkita.data.remote.dto.AuthResponse;
import com.carikostkita.data.remote.dto.IdTokenRequest;
import com.carikostkita.data.remote.dto.SignUpRequest;
import com.carikostkita.data.remote.dto.UpdatePasswordRequest;
import com.carikostkita.data.remote.dto.UserDto;
import com.carikostkita.data.remote.dto.UserUpdateDto;
import com.carikostkita.data.remote.dto.ProfileUpdateRequest;
import com.carikostkita.data.remote.dto.UserProfileResponse;
import com.carikostkita.data.remote.dto.OwnerVerificationRequest;
import com.carikostkita.data.remote.dto.OwnerVerificationResponse;
import com.carikostkita.data.remote.dto.AdminUserDto;
import com.carikostkita.data.remote.dto.AdminUserListResponse;
import com.carikostkita.data.remote.dto.AdminUserModerationRequest;
import com.carikostkita.data.remote.dto.AdminUserModerationResponse;
import com.carikostkita.data.remote.dto.AvatarUploadResponse;
import com.carikostkita.data.remote.dto.OwnerVerificationQueueResponse;
import com.carikostkita.data.remote.dto.OwnerVerificationReviewRequest;
import com.carikostkita.data.remote.dto.OwnerVerificationReviewResponse;
import com.carikostkita.util.ErrorMessages;
import com.carikostkita.util.SessionManager;
import android.util.Base64;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.carikostkita.data.remote.SupabaseStorageService;
import com.carikostkita.data.remote.dto.StorageUploadResponse;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class UserRepository {
    /** Dikirim lewat onError saat akun dibuat tapi email harus dikonfirmasi dulu. */
    public static final String EMAIL_CONFIRMATION_REQUIRED =
            "Akun berhasil dibuat. Buka email kamu dan ketuk tautan konfirmasi, lalu masuk dengan email dan kata sandi.";
    public static final String RESET_REDIRECT_URL = "carikostkita://reset-callback";
    private final FastApiAuthService authService;
    private final FastApiProfileService profileService;
    private final FastApiUserLookupService userLookupService;
    private final FastApiOwnerVerificationService ownerVerificationService;
    private final FastApiAdminUserService adminUserService;
    private final FastApiAvatarService avatarService;
    private final SupabaseDbService dbService;
    private final SupabaseStorageService storageService;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Context context;
    private final Map<String, String> pendingOwnerApplicationIds = new ConcurrentHashMap<>();

    public static class UserStats {
        public final int totalUsers;
        public final int totalOwners;
        public final int totalPencari;
        public final int totalPemilik;
        public final int pendingVerifikasi;
        public List<UserDto> userList = new ArrayList<>();

        public UserStats(int totalUsers, int totalOwners, int pendingVerifikasi) {
            this.totalUsers = totalUsers;
            this.totalOwners = totalOwners;
            this.totalPemilik = totalOwners;
            this.totalPencari = Math.max(0, totalUsers - totalOwners);
            this.pendingVerifikasi = pendingVerifikasi;
        }

        public UserStats(int totalUsers, int totalOwners) {
            this(totalUsers, totalOwners, 0);
        }
    }

    public UserRepository(Context context) {
        this.context = context.getApplicationContext();
        this.authService = SupabaseClient.getInstance().createAuthService();
        this.profileService = SupabaseClient.getInstance().createProfileService();
        this.userLookupService = SupabaseClient.getInstance().createUserLookupService();
        this.ownerVerificationService = SupabaseClient.getInstance().createOwnerVerificationService();
        this.adminUserService = SupabaseClient.getInstance().createAdminUserService();
        this.avatarService = SupabaseClient.getInstance().createAvatarService();
        this.dbService = SupabaseClient.getInstance().createService(SupabaseDbService.class);
        this.storageService = SupabaseClient.getInstance().createService(SupabaseStorageService.class);
        this.sessionManager = new SessionManager(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void login(String email, String plainPassword, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (email == null || email.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
                    postError(callback, "Email dan password wajib diisi");
                    return;
                }

                Response<AuthResponse> authRes = authService.signInWithPassword(new AuthRequest(email.trim(), plainPassword)).execute();
                if (!authRes.isSuccessful() || authRes.body() == null) {
                    String errorMsg = parseSupabaseError(authRes, "Login gagal: Email atau password salah.");
                    postError(callback, errorMsg);
                    return;
                }

                AuthResponse auth = authRes.body();
                sessionManager.saveTokens(auth.accessToken, auth.refreshToken, auth.expiresIn);

                // Ambil data profile dari tabel users
                fetchUserProfile(auth.user.id, auth, callback);

            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Kesalahan jaringan login", e));
            }
        });
    }

    public void loginWithGoogle(String idToken, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (idToken == null || idToken.isEmpty()) {
                    postError(callback, "Token Google tidak valid");
                    return;
                }

                Response<AuthResponse> res = authService.signInWithGoogleIdToken(new IdTokenRequest(idToken)).execute();
                if (!res.isSuccessful() || res.body() == null) {
                    String errorMsg = parseSupabaseError(res, "Gagal autentikasi Google dengan Supabase");
                    postError(callback, errorMsg);
                    return;
                }

                AuthResponse auth = res.body();
                sessionManager.saveTokens(auth.accessToken, auth.refreshToken, auth.expiresIn);
                fetchUserProfile(auth.user.id, auth, callback);

            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Kesalahan Google sign-in", e));
            }
        });
    }

    public void handleOAuthCallback(String accessToken, String refreshToken, long expiresIn, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (accessToken == null || accessToken.trim().isEmpty()) {
                    postError(callback, "Token otentikasi tidak valid");
                    return;
                }

                sessionManager.saveTokens(accessToken, refreshToken, expiresIn);

                String uid = getUserIdFromJwt(accessToken);
                String userEmail = null;
                String userName = null;
                try {
                    Response<AuthResponse.AuthUser> authUserRes = authService.getCurrentAuthUser(
                            "Bearer " + accessToken).execute();
                    if (authUserRes.isSuccessful() && authUserRes.body() != null) {
                        AuthResponse.AuthUser authUser = authUserRes.body();
                        if (uid == null) uid = authUser.id;
                        userEmail = authUser.email;
                        if (authUser.userMetadata != null) {
                            if (authUser.userMetadata.containsKey("full_name")) {
                                userName = String.valueOf(authUser.userMetadata.get("full_name"));
                            } else if (authUser.userMetadata.containsKey("name")) {
                                userName = String.valueOf(authUser.userMetadata.get("name"));
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (uid == null || uid.isEmpty()) {
                    postError(callback, "Gagal mendapatkan data akun Google dari server");
                    return;
                }

                if ((userName == null || userName.trim().isEmpty()) && userEmail != null) {
                    userName = userEmail.split("@", 2)[0];
                }
                UserDto profile = loadOrCreateMyProfile(accessToken, userName, "");
                User user = mapDtoToUser(profile);
                sessionManager.createLoginSession(user, accessToken, refreshToken, expiresIn);
                mainHandler.post(() -> callback.onSuccess(user));
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Gagal memproses sesi login", e));
            }
        });
    }

    public void register(String nama, String email, String plainPassword, DataCallback<User> callback) {
        register(nama, email, plainPassword, "", Role.USER, false, "", callback);
    }

    public void register(String nama, String email, String plainPassword, String noHp, Role role, DataCallback<User> callback) {
        register(nama, email, plainPassword, noHp, role, false, "", callback);
    }

    public void register(String nama, String email, String plainPassword, String noHp, Role role, boolean requestOwner, String ownerNote, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (nama == null || nama.trim().isEmpty()) {
                    postError(callback, "Nama lengkap tidak boleh kosong");
                    return;
                }
                if (email == null || !email.contains("@")) {
                    postError(callback, "Format email tidak valid");
                    return;
                }
                if (plainPassword == null || plainPassword.length() < 6) {
                    postError(callback, "Password minimal 6 karakter");
                    return;
                }

                Response<AuthResponse> res = authService.signUp(new SignUpRequest(email.trim().toLowerCase(), plainPassword, nama.trim())).execute();
                if (!res.isSuccessful() || res.body() == null) {
                    String errorMsg = parseSupabaseError(res, "Gagal mendaftar. Silakan coba lagi.");
                    postError(callback, errorMsg);
                    return;
                }

                AuthResponse auth = res.body();
                if (auth.accessToken == null || auth.accessToken.isEmpty()) {
                    postError(callback, EMAIL_CONFIRMATION_REQUIRED);
                    return;
                }

                sessionManager.saveTokens(auth.accessToken, auth.refreshToken, auth.expiresIn);

                String uid = auth.user != null ? auth.user.id : null;
                if (uid != null) {
                    loadOrCreateMyProfile(auth.accessToken, nama.trim(), noHp != null ? noHp.trim() : "");
                    Response<UserProfileResponse> updatedProfile = profileService.updateMyProfile(
                            "Bearer " + auth.accessToken,
                            new ProfileUpdateRequest(nama.trim(), noHp != null ? noHp.trim() : "", "", "avatar_male")
                    ).execute();
                    if (!updatedProfile.isSuccessful() || updatedProfile.body() == null
                            || updatedProfile.body().data == null) {
                        postError(callback, "Akun dibuat, tetapi profil belum tersimpan. Silakan masuk kembali.");
                        return;
                    }

                    if (requestOwner || role == Role.PEMILIK_KOST) {
                        String submissionNote = ownerNote != null && !ownerNote.trim().isEmpty()
                                ? ownerNote.trim() : "Pengajuan saat pendaftaran";
                        Response<OwnerVerificationResponse> ownerRequest = ownerVerificationService.submit(
                                "Bearer " + auth.accessToken,
                                new OwnerVerificationRequest(submissionNote)
                        ).execute();
                        boolean submitted = ownerRequest.isSuccessful() && ownerRequest.body() != null
                                && ownerRequest.body().submitted;
                        if (!submitted && ownerRequest.code() == 404) {
                            // Compatibility for an older API deployment only; RLS/triggers still govern the write.
                            UserUpdateDto legacyRequest = new UserUpdateDto();
                            legacyRequest.verificationStatus = VerificationStatus.PENDING.name();
                            legacyRequest.pengajuanCatatan = submissionNote;
                            Response<List<UserDto>> legacy = dbService.updateUser("eq." + uid, legacyRequest).execute();
                            if (!legacy.isSuccessful()) {
                                postError(callback, "Profil dibuat, tetapi pengajuan pemilik belum tersimpan. Silakan coba lagi.");
                                return;
                            }
                        } else if (!submitted) {
                            postError(callback, "Profil dibuat, tetapi pengajuan pemilik gagal disimpan. "
                                    + ErrorMessages.fromResponse("submitOwnerVerification", ownerRequest));
                            return;
                        }
                    }

                    fetchUserProfile(uid, auth, callback);
                } else {
                    User basic = new User("", nama, email, Role.USER, "");
                    mainHandler.post(() -> callback.onSuccess(basic));
                }

            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Terjadi kesalahan registrasi", e));
            }
        });
    }

    private String parseSupabaseError(Response<?> response, String defaultMsg) {
        if (response == null) return defaultMsg;
        String errorJson = "";
        try {
            if (response.errorBody() != null) errorJson = response.errorBody().string();
        } catch (Exception ignored) {}
        android.util.Log.e("UserRepository", "Auth error HTTP " + response.code() + ": " + errorJson);
        String lower = errorJson != null ? errorJson.toLowerCase() : "";
        if (lower.contains("already registered") || lower.contains("already exists")) {
            return "Email ini sudah terdaftar. Silakan masuk atau gunakan email lain.";
        }
        if (lower.contains("invalid login credentials") || lower.contains("invalid credentials")) {
            return "Email atau kata sandi salah.";
        }
        if (lower.contains("email not confirmed")) {
            return "Email belum dikonfirmasi. Buka email kamu dan ketuk tautan konfirmasi terlebih dahulu.";
        }
        if (lower.contains("password should be at least") || lower.contains("weak_password")) {
            return "Kata sandi minimal 6 karakter.";
        }
        if (lower.contains("rate limit") || response.code() == 429) {
            return "Terlalu banyak percobaan. Tunggu beberapa menit lalu coba lagi.";
        }
        if (lower.contains("same_password") || lower.contains("should be different")) {
            return "Kata sandi baru harus berbeda dari kata sandi lama.";
        }
        if (response.code() >= 500) return ErrorMessages.SERVER;
        return defaultMsg;
    }

    private static String getUserIdFromJwt(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length >= 2) {
                byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
                String json = new String(decoded, StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("sub")) {
                    return obj.get("sub").getAsString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void fetchUserProfile(String uid, AuthResponse auth, DataCallback<User> callback) {
        try {
            String email = auth.user != null && auth.user.email != null ? auth.user.email : "";
            String displayName = email.isEmpty() ? "Pengguna" : email.split("@", 2)[0];
            if (auth.user != null && auth.user.userMetadata != null) {
                Object metadataName = auth.user.userMetadata.containsKey("full_name")
                        ? auth.user.userMetadata.get("full_name")
                        : auth.user.userMetadata.get("name");
                if (metadataName != null && !String.valueOf(metadataName).trim().isEmpty()) {
                    displayName = String.valueOf(metadataName).trim();
                }
            }
            UserDto profile = loadOrCreateMyProfile(auth.accessToken, displayName, "");
            User user = mapDtoToUser(profile);
            if (!user.isActive()) {
                sessionManager.logout();
                postError(callback, "Akun Anda dinonaktifkan oleh administrator.");
                return;
            }
            sessionManager.createLoginSession(user, auth.accessToken, auth.refreshToken, auth.expiresIn);
            mainHandler.post(() -> callback.onSuccess(user));
        } catch (Exception e) {
            postError(callback, ErrorMessages.fromException("fetchUserProfile", e));
        }
    }

    private UserDto loadOrCreateMyProfile(String accessToken, String name, String noHp) throws IOException {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IOException("Authenticated profile session is missing.");
        }
        String bearer = "Bearer " + accessToken;
        Response<UserProfileResponse> existing = profileService.readMyProfile(bearer).execute();
        if (existing.isSuccessful() && existing.body() != null && existing.body().data != null) {
            return existing.body().data;
        }
        if (existing.code() != 404 && existing.code() != 409) {
            throw new IOException("Unable to read the authenticated profile (HTTP " + existing.code() + ").");
        }

        String displayName = name == null || name.trim().isEmpty() ? null : name.trim();
        ProfileUpdateRequest request = new ProfileUpdateRequest(
                displayName, noHp == null ? "" : noHp, "", "avatar_male");
        Response<UserProfileResponse> created = profileService.provisionMyProfile(bearer, request).execute();
        if (created.isSuccessful() && created.body() != null && created.body().data != null) {
            return created.body().data;
        }
        throw new IOException("Unable to provision the authenticated profile (HTTP " + created.code() + ").");
    }

    public void getUserById(String uid, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (uid == null || uid.isEmpty()) {
                    postError(callback, "ID Pengguna tidak valid");
                    return;
                }
                if (uid.equals(sessionManager.getUserUid())) {
                    String token = getActiveAccessToken();
                    if (token == null || token.isEmpty()) {
                        postError(callback, ErrorMessages.SESSION_EXPIRED);
                        return;
                    }
                    Response<UserProfileResponse> profile = profileService
                            .readMyProfile("Bearer " + token).execute();
                    if (profile.isSuccessful() && profile.body() != null && profile.body().data != null) {
                        User user = mapDtoToUser(profile.body().data);
                        mainHandler.post(() -> callback.onSuccess(user));
                    } else {
                        postError(callback, ErrorMessages.fromResponse("getMyProfile", profile));
                    }
                    return;
                }
                long numericUid;
                try {
                    numericUid = Long.parseLong(uid);
                    if (numericUid <= 0) throw new NumberFormatException("non-positive user ID");
                } catch (NumberFormatException invalidId) {
                    postError(callback, "ID pengguna tidak valid");
                    return;
                }

                String token = getActiveAccessToken();
                if (token != null && !token.isEmpty()) {
                    Response<UserProfileResponse> contact = userLookupService
                            .readChatContact("Bearer " + token, numericUid).execute();
                    if (contact.isSuccessful() && contact.body() != null && contact.body().data != null) {
                        User user = mapDtoToUser(contact.body().data);
                        mainHandler.post(() -> callback.onSuccess(user));
                        return;
                    }
                    if (contact.code() != 404) {
                        postError(callback, ErrorMessages.fromResponse("getChatContact", contact));
                        return;
                    }
                }

                Response<UserProfileResponse> owner = userLookupService.readPublicOwner(numericUid).execute();
                if (owner.isSuccessful() && owner.body() != null && owner.body().data != null) {
                    User user = mapDtoToUser(owner.body().data);
                    mainHandler.post(() -> callback.onSuccess(user));
                } else {
                    postError(callback, ErrorMessages.fromResponse("getPublicOwner", owner));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Gagal mengambil data user", e));
            }
        });
    }

    public void getUserById(int legacyId, DataCallback<User> callback) {
        getUserById(sessionManager.getUserUid(), callback);
    }

    public void updateProfile(String uid, String nama, String noHp, String bio, String avatarUrl, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (nama == null || nama.trim().isEmpty()) {
                    postError(callback, "Nama tidak boleh kosong");
                    return;
                }
                String currentUid = sessionManager.getUserUid();
                String token = getActiveAccessToken();
                if (uid == null || !uid.equals(currentUid)) {
                    postError(callback, ErrorMessages.FORBIDDEN);
                    return;
                }
                if (token == null || token.isEmpty()) {
                    postError(callback, ErrorMessages.SESSION_EXPIRED);
                    return;
                }
                ProfileUpdateRequest request = new ProfileUpdateRequest(
                        nama.trim(), noHp != null ? noHp.trim() : "",
                        bio != null ? bio.trim() : "", avatarUrl);
                Response<UserProfileResponse> res = profileService
                        .updateMyProfile("Bearer " + token, request).execute();
                if (res.isSuccessful() && res.body() != null && res.body().data != null) {
                    sessionManager.updateProfile(nama, noHp, bio, avatarUrl);
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal memperbarui profil di server");
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Kesalahan update profil", e));
            }
        });
    }

    public void updateProfile(int legacyId, String nama, String noHp, String bio, String avatarUrl, DataCallback<Boolean> callback) {
        updateProfile(sessionManager.getUserUid(), nama, noHp, bio, avatarUrl, callback);
    }

    private String getActiveAccessToken() {
        String token = sessionManager.getAccessToken();
        if (token == null || token.isEmpty()) return null;
        if (sessionManager.isTokenExpired()) {
            return SupabaseClient.getInstance().refreshTokenBlocking(token);
        }
        return token;
    }

    public void uploadAvatar(Uri imageUri, DataCallback<String> callback) {
        executor.execute(() -> {
            try {
                if (imageUri == null) {
                    postError(callback, "Foto profil tidak ditemukan");
                    return;
                }
                byte[] bytes;
                try (InputStream in = context.getContentResolver().openInputStream(imageUri)) {
                    if (in == null) {
                        postError(callback, "Foto tidak bisa dibaca dari galeri. Coba pilih foto lain.");
                        return;
                    }
                    android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                    opts.inSampleSize = 2;
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in, null, opts);
                    if (bmp == null) {
                        postError(callback, "Format foto tidak didukung. Gunakan JPG atau PNG.");
                        return;
                    }
                    int edge = Math.max(bmp.getWidth(), bmp.getHeight());
                    if (edge > 640) {
                        float scale = 640f / edge;
                        android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(
                                bmp, Math.round(bmp.getWidth() * scale), Math.round(bmp.getHeight() * scale), true);
                        if (scaled != bmp) bmp.recycle();
                        bmp = scaled;
                    }
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out);
                    bmp.recycle();
                    bytes = out.toByteArray();
                }

                String uid = sessionManager.getUserUid();
                RequestBody requestBody = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
                String token = getActiveAccessToken();
                if (token == null || token.isEmpty()) {
                    postError(callback, ErrorMessages.SESSION_EXPIRED);
                    return;
                }
                Response<AvatarUploadResponse> upload = avatarService
                        .uploadMyAvatar("Bearer " + token, requestBody).execute();
                if (upload.isSuccessful() && upload.body() != null && upload.body().data != null
                        && upload.body().data.publicUrl != null) {
                    String publicUrl = upload.body().data.publicUrl;
                    mainHandler.post(() -> callback.onSuccess(publicUrl));
                    return;
                }
                if (upload.code() != 404) {
                    postError(callback, "Foto profil gagal diunggah. "
                            + ErrorMessages.fromResponse("uploadAvatar", upload));
                    return;
                }

                // Compatibility for a FastAPI deployment that does not yet have the avatar route.
                String remoteName = (uid != null ? uid : "anon") + "/avatar_" + UUID.randomUUID() + ".jpg";
                Response<StorageUploadResponse> legacy = storageService
                        .uploadFileBinary("kost-images", remoteName, "image/jpeg", requestBody).execute();
                if (legacy.isSuccessful()) {
                    String publicUrl = SupabaseClient.getStoragePublicUrl("kost-images", remoteName);
                    mainHandler.post(() -> callback.onSuccess(publicUrl));
                } else {
                    postError(callback, "Foto profil gagal diunggah. " + ErrorMessages.fromResponse("uploadAvatar", legacy));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("uploadAvatar", e));
            }
        });
    }

    public void changePassword(String uid, String oldPassword, String newPassword, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (newPassword == null || newPassword.length() < 6) {
                    postError(callback, "Kata sandi baru minimal 6 karakter");
                    return;
                }

                // Akun email wajib membuktikan kata sandi lama sebelum menggantinya
                if (!sessionManager.isGoogleAccount()) {
                    if (oldPassword == null || oldPassword.isEmpty()) {
                        postError(callback, "Masukkan kata sandi lama kamu");
                        return;
                    }
                    Response<AuthResponse> verify = authService.signInWithPassword(
                            new AuthRequest(sessionManager.getUserEmail(), oldPassword)).execute();
                    if (!verify.isSuccessful() || verify.body() == null) {
                        postError(callback, verify.code() == 400 ? "Kata sandi lama salah." : parseSupabaseError(verify, "Kata sandi lama tidak dapat diverifikasi."));
                        return;
                    }
                    AuthResponse fresh = verify.body();
                    sessionManager.saveTokens(fresh.accessToken, fresh.refreshToken, fresh.expiresIn);
                }

                Response<ResponseBody> res = authService.updatePassword(
                        "Bearer " + sessionManager.getAccessToken(),
                        new UpdatePasswordRequest(newPassword)).execute();
                if (res.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, parseSupabaseError(res, "Gagal mengubah kata sandi. Coba lagi."));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("changePassword", e));
            }
        });
    }

    /** Kirim email berisi tautan untuk mengatur ulang kata sandi. */
    public void sendPasswordReset(String email, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> body = new HashMap<>();
                body.put("email", email.trim().toLowerCase());
                Response<ResponseBody> res = authService.recoverPassword(body, RESET_REDIRECT_URL).execute();
                if (res.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, parseSupabaseError(res, "Email reset tidak dapat dikirim. Coba lagi."));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("sendPasswordReset", e));
            }
        });
    }

    /** Atur kata sandi baru memakai sesi pemulihan dari tautan email. */
    public void setPasswordFromRecovery(String accessToken, String refreshToken, long expiresIn,
                                        String newPassword, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (newPassword == null || newPassword.length() < 6) {
                    postError(callback, "Kata sandi baru minimal 6 karakter");
                    return;
                }
                SupabaseClient.getInstance().setAccessToken(accessToken);
                Response<ResponseBody> res = authService.updatePassword(
                        "Bearer " + accessToken,
                        new UpdatePasswordRequest(newPassword)).execute();
                SupabaseClient.getInstance().setAccessToken(sessionManager.getAccessToken());
                if (res.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, parseSupabaseError(res, "Tautan reset sudah kedaluwarsa. Minta tautan baru."));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("setPasswordFromRecovery", e));
            }
        });
    }

    /**
     * Sinkronkan profil dari server saat aplikasi dibuka: memperbarui peran yang mungkin
     * diubah developer dan mengeluarkan akun yang dinonaktifkan.
     */
    public void refreshCurrentUser(DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                String uid = sessionManager.getUserUid();
                if (uid == null || uid.isEmpty()) {
                    postError(callback, "Belum masuk");
                    return;
                }
                String token = getActiveAccessToken();
                if (token == null || token.isEmpty()) {
                    sessionManager.logout();
                    postError(callback, ErrorMessages.SESSION_EXPIRED);
                    return;
                }
                Response<UserProfileResponse> res = profileService
                        .readMyProfile("Bearer " + token).execute();
                User user;
                if (res.isSuccessful() && res.body() != null && res.body().data != null) {
                    user = mapDtoToUser(res.body().data);
                } else if (res.code() == 404) {
                    // Keep the legacy RLS read until profile/identity migration is proven in production.
                    Response<List<UserDto>> legacy = dbService.getUserById("eq." + uid, "*").execute();
                    if (legacy.code() == 401) {
                        sessionManager.logout();
                        postError(callback, ErrorMessages.SESSION_EXPIRED);
                        return;
                    }
                    if (!legacy.isSuccessful()) {
                        postError(callback, ErrorMessages.fromResponse("refreshCurrentUser", legacy));
                        return;
                    }
                    if (legacy.body() == null || legacy.body().isEmpty()) {
                        sessionManager.logout();
                        postError(callback, "Akun kamu sudah tidak terdaftar.");
                        return;
                    }
                    user = mapDtoToUser(legacy.body().get(0));
                } else if (res.code() == 401 || res.code() == 403) {
                    sessionManager.logout();
                    postError(callback, ErrorMessages.SESSION_EXPIRED);
                    return;
                } else {
                    postError(callback, ErrorMessages.fromResponse("refreshCurrentUser", res));
                    return;
                }
                if (!user.isActive()) {
                    sessionManager.logout();
                    postError(callback, "Akun kamu dinonaktifkan oleh tim CariKostKita.");
                    return;
                }
                sessionManager.updateVerificationStatus(user.getVerificationStatus(), user.getRole(), user.getCatatanRevisi());
                sessionManager.updateProfile(user.getNama(), user.getNoHp(), user.getBio(), user.getAvatarUrl());
                User resolvedUser = user;
                mainHandler.post(() -> callback.onSuccess(resolvedUser));
            } catch (Exception e) {
                // Offline: tetap pakai data sesi lokal
                postError(callback, ErrorMessages.fromException("refreshCurrentUser", e));
            }
        });
    }

    public void changePassword(int legacyId, String oldPassword, String newPassword, DataCallback<Boolean> callback) {
        changePassword(sessionManager.getUserUid(), oldPassword, newPassword, callback);
    }

    public void submitOwnerVerification(String uid, String catatan, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String currentUid = sessionManager.getUserUid();
                if (uid == null || !uid.equals(currentUid)) {
                    postError(callback, ErrorMessages.FORBIDDEN);
                    return;
                }
                String token = getActiveAccessToken();
                if (token == null || token.isEmpty()) {
                    postError(callback, ErrorMessages.SESSION_EXPIRED);
                    return;
                }
                Response<OwnerVerificationResponse> res = ownerVerificationService.submit(
                        "Bearer " + token,
                        new OwnerVerificationRequest(catatan == null ? "" : catatan.trim())
                ).execute();
                if (res.isSuccessful() && res.body() != null && res.body().submitted) {
                    sessionManager.updateVerificationStatus(VerificationStatus.PENDING, null, null);
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, ErrorMessages.fromResponse("submitOwnerVerification", res));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Error pengajuan", e));
            }
        });
    }

    public void submitOwnerVerification(int legacyId, String catatan, DataCallback<Boolean> callback) {
        submitOwnerVerification(sessionManager.getUserUid(), catatan, callback);
    }

    public void approveOwnerVerification(String uid, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (reviewOwnerVerificationWithApi(uid, "APPROVED", null, callback)) return;
                UserUpdateDto dto = new UserUpdateDto();
                dto.role = "owner";
                dto.verificationStatus = VerificationStatus.APPROVED.name();
                dto.catatanRevisi = "";

                Response<List<UserDto>> res = dbService.updateUser("eq." + uid, dto).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal menyetujui pengajuan: 0 baris diperbarui di database.");
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Error approval", e));
            }
        });
    }

    public void approveOwnerVerification(int legacyId, DataCallback<Boolean> callback) {
        postError(callback, "Operasi memerlukan UUID user");
    }

    public void rejectOwnerVerification(String uid, String alasan, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (reviewOwnerVerificationWithApi(uid, "REJECTED", alasan, callback)) return;
                UserUpdateDto dto = new UserUpdateDto();
                dto.role = "user";
                dto.verificationStatus = VerificationStatus.REJECTED.name();
                dto.pengajuanCatatan = alasan;

                Response<List<UserDto>> res = dbService.updateUser("eq." + uid, dto).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal menolak pengajuan di server.");
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Error rejection", e));
            }
        });
    }

    public void requireRevisionOwnerVerification(String uid, String catatanRevisi, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (reviewOwnerVerificationWithApi(uid, "REVISION_REQUIRED", catatanRevisi, callback)) return;
                UserUpdateDto dto = new UserUpdateDto();
                dto.role = "user";
                dto.verificationStatus = VerificationStatus.REVISION_REQUIRED.name();
                dto.catatanRevisi = catatanRevisi;

                Response<List<UserDto>> res = dbService.updateUser("eq." + uid, dto).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal meminta revisi di server.");
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Error revision", e));
            }
        });
    }

    public void setRole(String uid, String role, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (moderateUserWithApi(uid, role, null, null, callback)) return;
                UserUpdateDto dto = new UserUpdateDto();
                dto.role = role;
                Response<List<UserDto>> res = dbService.updateUser("eq." + uid, dto).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty() && role.equals(res.body().get(0).role)) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else if (res.isSuccessful()) {
                    postError(callback, ErrorMessages.FORBIDDEN);
                } else {
                    postError(callback, ErrorMessages.fromResponse("setRole", res));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("setRole", e));
            }
        });
    }

    public void setUserActiveStatus(String uid, boolean isActive, DataCallback<Boolean> callback) {
        setUserActiveStatus(uid, isActive, null, callback);
    }

    public void setUserActiveStatus(String uid, boolean isActive, String reason, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (moderateUserWithApi(uid, null, isActive, reason, callback)) return;
                UserUpdateDto dto = new UserUpdateDto();
                dto.isActive = isActive;

                Response<List<UserDto>> res = dbService.updateUser("eq." + uid, dto).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal mengubah status aktif user di server");
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Error set status", e));
            }
        });
    }

    /**
     * Returns false only for HTTP 404, when an older API deployment has not
     * acquired the moderation route yet. Auth, validation, server, and network
     * failures fail closed instead of silently writing outside the audit RPC.
     */
    private boolean moderateUserWithApi(
            String uid, String role, Boolean isActive, String reason, DataCallback<Boolean> callback
    ) {
        String token = getActiveAccessToken();
        if (token == null || token.isEmpty()) {
            postError(callback, ErrorMessages.SESSION_EXPIRED);
            return true;
        }
        try {
            AdminUserModerationRequest request = new AdminUserModerationRequest(role, isActive, reason);
            Response<AdminUserModerationResponse> response = adminUserService
                    .moderateUser("Bearer " + token, uid, request).execute();
            if (response.isSuccessful() && response.body() != null && response.body().updated) {
                mainHandler.post(() -> callback.onSuccess(true));
                return true;
            }
            int code = response.code();
            String errorBody = "";
            try {
                if (response.errorBody() != null) errorBody = response.errorBody().string();
            } catch (IOException ignored) {}
            String message = ErrorMessages.fromCode("moderateAdminUser", code, errorBody);
            if (code == 404 && !errorBody.contains("USER_NOT_FOUND")) return false;
            postError(callback, message);
            return true;
        } catch (Exception error) {
            postError(callback, ErrorMessages.fromException("moderateAdminUser", error));
            return true;
        }
    }

    public void getPendingVerifications(DataCallback<List<User>> callback) {
        executor.execute(() -> {
            try {
                List<OwnerVerificationQueueResponse.Application> applications = fetchPendingOwnerApplications();
                if (applications != null) {
                    pendingOwnerApplicationIds.clear();
                    List<User> users = new ArrayList<>();
                    for (OwnerVerificationQueueResponse.Application application : applications) {
                        if (application.owner == null || application.owner.id == null) continue;
                        pendingOwnerApplicationIds.put(application.owner.id, application.id);
                        User user = mapAdminDtoToUser(application.owner);
                        user.setPengajuanCatatan(application.submissionNote);
                        user.setPengajuanAt(application.createdAt);
                        users.add(user);
                    }
                    mainHandler.post(() -> callback.onSuccess(users));
                    return;
                }
                pendingOwnerApplicationIds.clear();
                List<AdminUserDto> apiUsers = fetchAdminUsers(null, "PENDING");
                if (apiUsers != null) {
                    List<User> users = new ArrayList<>();
                    for (AdminUserDto dto : apiUsers) users.add(mapAdminDtoToUser(dto));
                    mainHandler.post(() -> callback.onSuccess(users));
                    return;
                }
                Map<String, String> filters = new HashMap<>();
                filters.put("verification_status", "eq.PENDING");

                Response<List<UserDto>> res = dbService.getUsers(filters, "*", "created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    List<User> list = new ArrayList<>();
                    for (UserDto dto : res.body()) {
                        list.add(mapDtoToUser(dto));
                    }
                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Gagal memuat daftar pengajuan", e));
            }
        });
    }

    public void getOwners(DataCallback<List<User>> callback) {
        executor.execute(() -> {
            try {
                List<AdminUserDto> apiUsers = fetchAdminUsers("owner", null);
                if (apiUsers != null) {
                    List<User> users = new ArrayList<>();
                    for (AdminUserDto dto : apiUsers) users.add(mapAdminDtoToUser(dto));
                    mainHandler.post(() -> callback.onSuccess(users));
                    return;
                }
                Map<String, String> filters = new HashMap<>();
                filters.put("role", "in.(owner,pemilik,pemilik_kost)");

                Response<List<UserDto>> res = dbService.getUsers(filters, "*", "created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    List<User> list = new ArrayList<>();
                    for (UserDto dto : res.body()) {
                        list.add(mapDtoToUser(dto));
                    }
                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Gagal memuat daftar pemilik", e));
            }
        });
    }

    public void deleteUser(String uid, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("target_id", uid);
                Response<ResponseBody> res = dbService.rpc("admin_delete_user", params).execute();
                if (res.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, ErrorMessages.fromResponse("admin_delete_user", res));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Error hapus pengguna", e));
            }
        });
    }

    public void getAllUsers(DataCallback<List<User>> callback) {
        executor.execute(() -> {
            try {
                List<AdminUserDto> apiUsers = fetchAdminUsers(null, null);
                if (apiUsers != null) {
                    List<User> users = new ArrayList<>();
                    for (AdminUserDto dto : apiUsers) users.add(mapAdminDtoToUser(dto));
                    mainHandler.post(() -> callback.onSuccess(users));
                    return;
                }
                Response<List<UserDto>> res = dbService.getUsers(new HashMap<>(), "*", "created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    List<User> list = new ArrayList<>();
                    for (UserDto dto : res.body()) {
                        list.add(mapDtoToUser(dto));
                    }
                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Gagal memuat pengguna", e));
            }
        });
    }

    public void getUserStats(DataCallback<UserStats> callback) {
        executor.execute(() -> {
            try {
                List<AdminUserDto> apiUsers = fetchAdminUsers(null, null);
                if (apiUsers != null) {
                    int owners = 0;
                    int pending = 0;
                    List<UserDto> legacyUsers = new ArrayList<>();
                    for (AdminUserDto dto : apiUsers) {
                        if ("owner".equalsIgnoreCase(dto.role)) owners++;
                        if ("PENDING".equalsIgnoreCase(dto.verificationStatus)) pending++;
                        legacyUsers.add(mapAdminDtoToLegacy(dto));
                    }
                    UserStats stats = new UserStats(apiUsers.size(), owners, pending);
                    stats.userList.addAll(legacyUsers);
                    UserStats finalStats = stats;
                    mainHandler.post(() -> callback.onSuccess(finalStats));
                    return;
                }
                Response<List<UserDto>> res = dbService.getUsers(new HashMap<>(), "*", null).execute();
                if (res.isSuccessful() && res.body() != null) {
                    int total = res.body().size();
                    int owners = 0;
                    int pending = 0;
                    for (UserDto u : res.body()) {
                        if ("owner".equalsIgnoreCase(u.role) || "pemilik_kost".equalsIgnoreCase(u.role)) {
                            owners++;
                        }
                        if ("PENDING".equalsIgnoreCase(u.verificationStatus)) {
                            pending++;
                        }
                    }
                    UserStats stats = new UserStats(total, owners, pending);
                    stats.userList.addAll(res.body());
                    mainHandler.post(() -> callback.onSuccess(stats));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new UserStats(0, 0, 0)));
                }
            } catch (Exception e) {
                postError(callback, ErrorMessages.fromException("Gagal memuat statistik user", e));
            }
        });
    }

    /**
     * Return null only when FastAPI is not deployed yet/unreachable, allowing
     * the existing caller-JWT/RLS query to remain as a compatibility fallback.
     * Authentication/authorization and other API errors are never downgraded.
     */
    private List<AdminUserDto> fetchAdminUsers(String role, String verificationStatus) {
        String token = getActiveAccessToken();
        if (token == null || token.isEmpty()) return null;
        List<AdminUserDto> users = new ArrayList<>();
        String bearer = "Bearer " + token;
        boolean hasMore = false;
        for (int page = 1; page <= 100; page++) {
            Response<AdminUserListResponse> response;
            try {
                response = adminUserService.listUsers(bearer, page, 100, role, verificationStatus).execute();
            } catch (IOException networkError) {
                if (page == 1) return null;
                throw new IllegalStateException("Koneksi API terputus saat memuat direktori pengguna.", networkError);
            }
            if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                int code = response.code();
                String message = ErrorMessages.fromResponse("listAdminUsers", response);
                if (response.errorBody() != null) response.errorBody().close();
                // A 503 is a live FastAPI route whose data dependency failed;
                // only a missing route may select the legacy RLS read.
                if (page == 1 && code == 404) return null;
                throw new IllegalStateException(message);
            }
            users.addAll(response.body().data);
            hasMore = response.body().pagination != null && response.body().pagination.hasMore;
            if (!hasMore) return users;
        }
        throw new IllegalStateException("Direktori pengguna terlalu besar untuk dimuat sekaligus.");
    }

    private List<OwnerVerificationQueueResponse.Application> fetchPendingOwnerApplications() {
        String token = getActiveAccessToken();
        if (token == null || token.isEmpty()) return null;
        List<OwnerVerificationQueueResponse.Application> applications = new ArrayList<>();
        boolean hasMore = false;
        for (int page = 1; page <= 100; page++) {
            Response<OwnerVerificationQueueResponse> response;
            try {
                response = ownerVerificationService
                        .listAdminQueue("Bearer " + token, "PENDING", page, 100).execute();
            } catch (IOException networkError) {
                if (page == 1) return null;
                throw new IllegalStateException("Koneksi API terputus saat memuat antrean verifikasi.", networkError);
            }
            if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                int code = response.code();
                String message = ErrorMessages.fromResponse("listOwnerVerificationQueue", response);
                if (response.errorBody() != null) response.errorBody().close();
                // Keep the compatibility read for an older deployment only;
                // do not hide a database failure reported by the live API.
                if (page == 1 && code == 404) return null;
                throw new IllegalStateException(message);
            }
            applications.addAll(response.body().data);
            hasMore = response.body().pagination != null && response.body().pagination.hasMore;
            if (!hasMore) return applications;
        }
        throw new IllegalStateException("Antrean verifikasi terlalu besar untuk dimuat sekaligus.");
    }

    /** True means the API handled the decision (success or terminal HTTP failure). */
    private boolean reviewOwnerVerificationWithApi(
            String userId, String status, String note, DataCallback<Boolean> callback
    ) {
        String applicationId = pendingOwnerApplicationIds.get(userId);
        if (applicationId == null) return false;
        String token = getActiveAccessToken();
        if (token == null || token.isEmpty()) {
            postError(callback, ErrorMessages.SESSION_EXPIRED);
            return true;
        }
        try {
            Response<OwnerVerificationReviewResponse> response = ownerVerificationService.review(
                    "Bearer " + token,
                    applicationId,
                    new OwnerVerificationReviewRequest(status, note)
            ).execute();
            if (response.isSuccessful() && response.body() != null && response.body().reviewed) {
                pendingOwnerApplicationIds.remove(userId);
                mainHandler.post(() -> callback.onSuccess(true));
                return true;
            }
            String message = ErrorMessages.fromResponse("reviewOwnerVerification", response);
            if (response.errorBody() != null) response.errorBody().close();
            postError(callback, message);
            return true;
        } catch (IOException networkError) {
            postError(callback, ErrorMessages.fromException("reviewOwnerVerification", networkError));
            return true;
        }
    }

    private User mapAdminDtoToUser(AdminUserDto dto) {
        User user = new User();
        user.setUid(dto.id);
        user.setNama(dto.name);
        user.setEmail(dto.email);
        user.setRole(Role.fromString(dto.role));
        user.setNoHp(dto.noHp);
        user.setAvatarUrl(dto.avatarUrl);
        user.setBio(dto.bio);
        user.setVerificationStatus(VerificationStatus.fromString(dto.verificationStatus));
        user.setCatatanRevisi(dto.catatanRevisi);
        user.setActive(dto.isActive != null ? dto.isActive : true);
        user.setCreatedAt(dto.createdAt);
        return user;
    }

    private UserDto mapAdminDtoToLegacy(AdminUserDto dto) {
        UserDto user = new UserDto();
        user.id = dto.id;
        user.nama = dto.name;
        user.email = dto.email;
        user.role = dto.role;
        user.noHp = dto.noHp;
        user.avatarUrl = dto.avatarUrl;
        user.bio = dto.bio;
        user.isActive = dto.isActive;
        user.verificationStatus = dto.verificationStatus;
        user.catatanRevisi = dto.catatanRevisi;
        user.createdAt = dto.createdAt;
        return user;
    }

    private User mapDtoToUser(UserDto dto) {
        User u = new User();
        u.setUid(dto.id);
        u.setNama(dto.nama);
        u.setEmail(dto.email);
        u.setRole(Role.fromString(dto.role));
        u.setNoHp(dto.noHp);
        u.setAvatarUrl(dto.avatarUrl);
        u.setBio(dto.bio);
        u.setVerificationStatus(VerificationStatus.fromString(dto.verificationStatus));
        u.setPengajuanCatatan(dto.pengajuanCatatan);
        u.setCatatanRevisi(dto.catatanRevisi);
        u.setActive(dto.isActive != null ? dto.isActive : true);
        u.setAuthProvider(dto.authProvider != null ? dto.authProvider : "EMAIL");
        u.setCreatedAt(dto.createdAt);
        u.setPengajuanAt(dto.pengajuanAt);
        return u;
    }

    private <T> void postError(DataCallback<T> callback, String msg) {
        mainHandler.post(() -> callback.onError(msg));
    }
}
