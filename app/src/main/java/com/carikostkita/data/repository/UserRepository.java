package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.carikostkita.data.model.VerificationStatus;
import com.carikostkita.data.remote.SupabaseAuthService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.data.remote.dto.AuthRequest;
import com.carikostkita.data.remote.dto.AuthResponse;
import com.carikostkita.data.remote.dto.IdTokenRequest;
import com.carikostkita.data.remote.dto.SignUpRequest;
import com.carikostkita.data.remote.dto.UpdatePasswordRequest;
import com.carikostkita.data.remote.dto.UserDto;
import com.carikostkita.data.remote.dto.UserUpdateDto;
import com.carikostkita.util.SessionManager;
import android.util.Base64;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.carikostkita.data.remote.SupabaseStorageService;
import com.carikostkita.data.remote.dto.StorageUploadResponse;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class UserRepository {
    private final SupabaseAuthService authService;
    private final SupabaseDbService dbService;
    private final SupabaseStorageService storageService;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Context context;

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
        this.authService = SupabaseClient.getInstance().createService(SupabaseAuthService.class);
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
                postError(callback, "Kesalahan jaringan login: " + e.getMessage());
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
                postError(callback, "Kesalahan Google sign-in: " + e.getMessage());
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
                    Response<AuthResponse.AuthUser> authUserRes = authService.getCurrentAuthUser().execute();
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

                // Ambil profil dari public.users
                Response<List<UserDto>> profileRes = dbService.getUserById("eq." + uid, "*").execute();
                if (profileRes.isSuccessful() && profileRes.body() != null && !profileRes.body().isEmpty()) {
                    User user = mapDtoToUser(profileRes.body().get(0));
                    sessionManager.createLoginSession(user, accessToken, refreshToken, expiresIn);
                    mainHandler.post(() -> callback.onSuccess(user));
                } else {
                    // Profile belum ada di public.users, buat baru secara mandiri
                    UserDto newProfile = new UserDto();
                    newProfile.id = uid;
                    newProfile.nama = (userName != null && !userName.isEmpty()) ? userName : (userEmail != null ? userEmail.split("@")[0] : "Pengguna Google");
                    newProfile.email = userEmail != null ? userEmail : "";
                    newProfile.role = "user";
                    newProfile.avatarUrl = "avatar_male";
                    newProfile.authProvider = "GOOGLE";

                    try {
                        dbService.createUser(newProfile).execute();
                    } catch (Exception ignored) {}

                    User user = new User(uid, newProfile.nama, newProfile.email, Role.USER, "");
                    sessionManager.createLoginSession(user, accessToken, refreshToken, expiresIn);
                    mainHandler.post(() -> callback.onSuccess(user));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memproses sesi login: " + e.getMessage());
            }
        });
    }

    public void loginWithGoogle(String email, String name, String avatar, DataCallback<User> callback) {
        login(email, "GoogleAuth123!", new DataCallback<User>() {
            @Override
            public void onSuccess(User data) {
                mainHandler.post(() -> callback.onSuccess(data));
            }

            @Override
            public void onError(String message) {
                registerGoogleUser(name != null ? name : "Pengguna Google", email, avatar, callback);
            }
        });
    }

    private void registerGoogleUser(String nama, String email, String avatar, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                Response<AuthResponse> res = authService.signUp(new SignUpRequest(email.trim().toLowerCase(), "GoogleAuth123!", nama.trim())).execute();
                if (!res.isSuccessful() || res.body() == null) {
                    String errorMsg = parseSupabaseError(res, "Gagal mendaftar akun Google");
                    if (errorMsg.toLowerCase().contains("sudah terdaftar") || errorMsg.toLowerCase().contains("already")) {
                        postError(callback, "Email " + email + " sudah terdaftar dengan kata sandi. Silakan masuk menggunakan form Email & Kata Sandi di atas.");
                    } else {
                        postError(callback, errorMsg);
                    }
                    return;
                }

                AuthResponse auth = res.body();
                if (auth.accessToken != null && !auth.accessToken.isEmpty()) {
                    sessionManager.saveTokens(auth.accessToken, auth.refreshToken, auth.expiresIn);
                }

                String uid = auth.user != null ? auth.user.id : null;
                if (uid != null) {
                    UserDto newProfile = new UserDto();
                    newProfile.id = uid;
                    newProfile.nama = nama.trim();
                    newProfile.email = email.trim().toLowerCase();
                    newProfile.role = "user";
                    newProfile.avatarUrl = avatar != null && !avatar.isEmpty() ? avatar : "avatar_male";
                    newProfile.authProvider = "GOOGLE";

                    try {
                        dbService.createUser(newProfile).execute();
                    } catch (Exception ignored) {}

                    fetchUserProfile(uid, auth, callback);
                } else {
                    User basic = new User("", nama, email, Role.USER, "");
                    mainHandler.post(() -> callback.onSuccess(basic));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memproses pendaftaran Google: " + e.getMessage());
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
                    postError(callback, "Pendaftaran berhasil terkirim! Silakan periksa inbox email Anda untuk konfirmasi, atau nonaktifkan 'Confirm email' di Supabase Dashboard agar bisa langsung masuk.");
                    return;
                }

                sessionManager.saveTokens(auth.accessToken, auth.refreshToken, auth.expiresIn);

                String uid = auth.user != null ? auth.user.id : null;
                if (uid != null) {
                    // Pastikan profil user sudah ada di public.users, jika belum ada buatkan langsung
                    try {
                        Response<List<UserDto>> checkRes = dbService.getUserById("eq." + uid, "*").execute();
                        if (!checkRes.isSuccessful() || checkRes.body() == null || checkRes.body().isEmpty()) {
                            UserDto newProfile = new UserDto();
                            newProfile.id = uid;
                            newProfile.nama = nama.trim();
                            newProfile.email = email.trim().toLowerCase();
                            newProfile.role = role != null ? role.name().toLowerCase() : "user";
                            newProfile.noHp = noHp != null ? noHp.trim() : "";
                            newProfile.avatarUrl = "avatar_male";
                            newProfile.authProvider = "EMAIL";
                            if (requestOwner || role == Role.PEMILIK_KOST) {
                                newProfile.verificationStatus = VerificationStatus.PENDING.name();
                                newProfile.pengajuanCatatan = ownerNote != null && !ownerNote.trim().isEmpty() ? ownerNote.trim() : "Pengajuan saat pendaftaran";
                            }
                            dbService.createUser(newProfile).execute();
                        } else if (requestOwner || role == Role.PEMILIK_KOST || (noHp != null && !noHp.isEmpty())) {
                            UserUpdateDto updateDto = new UserUpdateDto();
                            updateDto.nama = nama.trim();
                            if (noHp != null && !noHp.isEmpty()) updateDto.noHp = noHp.trim();
                            if (requestOwner || role == Role.PEMILIK_KOST) {
                                updateDto.verificationStatus = VerificationStatus.PENDING.name();
                                updateDto.pengajuanCatatan = ownerNote != null && !ownerNote.trim().isEmpty() ? ownerNote.trim() : "Pengajuan saat pendaftaran";
                            }
                            dbService.updateUser("eq." + uid, updateDto).execute();
                        }
                    } catch (Exception ignored) {}

                    fetchUserProfile(uid, auth, callback);
                } else {
                    User basic = new User("", nama, email, Role.USER, "");
                    mainHandler.post(() -> callback.onSuccess(basic));
                }

            } catch (Exception e) {
                postError(callback, "Terjadi kesalahan registrasi: " + e.getMessage());
            }
        });
    }

    private String parseSupabaseError(Response<?> response, String defaultMsg) {
        if (response == null) return defaultMsg;
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                if (errorJson != null && !errorJson.trim().isEmpty()) {
                    JsonObject obj = JsonParser.parseString(errorJson).getAsJsonObject();
                    String rawMsg = null;
                    if (obj.has("msg")) rawMsg = obj.get("msg").getAsString();
                    else if (obj.has("message")) rawMsg = obj.get("message").getAsString();
                    else if (obj.has("error_description")) rawMsg = obj.get("error_description").getAsString();
                    else if (obj.has("error")) rawMsg = obj.get("error").getAsString();

                    if (rawMsg != null && !rawMsg.trim().isEmpty()) {
                        String lower = rawMsg.toLowerCase();
                        if (lower.contains("user already registered") || lower.contains("already exists")) {
                            return "Email ini sudah terdaftar. Silakan login atau gunakan email lain.";
                        }
                        if (lower.contains("invalid login credentials") || lower.contains("invalid credentials")) {
                            return "Email atau password salah.";
                        }
                        if (lower.contains("email not confirmed")) {
                            return "Email belum dikonfirmasi. Silakan periksa inbox email Anda atau nonaktifkan 'Confirm email' di Supabase Dashboard.";
                        }
                        if (lower.contains("database error saving new user")) {
                            return "Gagal menyimpan akun ke database (Database error). Silakan jalankan script SQL perbaikan trigger di Supabase SQL Editor.";
                        }
                        if (lower.contains("password should be at least")) {
                            return "Password minimal harus 6 karakter.";
                        }
                        if (lower.contains("rate limit")) {
                            return "Terlalu banyak percobaan. Silakan tunggu beberapa menit.";
                        }
                        if (lower.contains("bad id token") || lower.contains("nonce")) {
                            return "Gagal validasi token Google di Supabase. Pastikan opsi 'Skip nonce check' dicentang (aktif) di Supabase Dashboard -> Authentication -> Providers -> Google.";
                        }
                        return rawMsg;
                    }
                }
            }
        } catch (Exception ignored) {}
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
            Response<List<UserDto>> profileRes = dbService.getUserById("eq." + uid, "*").execute();
            if (profileRes.isSuccessful() && profileRes.body() != null && !profileRes.body().isEmpty()) {
                User user = mapDtoToUser(profileRes.body().get(0));
                if (!user.isActive()) {
                    sessionManager.logout();
                    postError(callback, "Akun Anda dinonaktifkan oleh administrator.");
                    return;
                }
                sessionManager.createLoginSession(user, auth.accessToken, auth.refreshToken, auth.expiresIn);
                mainHandler.post(() -> callback.onSuccess(user));
            } else {
                // Periksa apakah sudah ada profil berdasarkan email
                String email = (auth.user != null && auth.user.email != null) ? auth.user.email : "";
                if (!email.isEmpty()) {
                    try {
                        Map<String, String> emailFilter = new HashMap<>();
                        emailFilter.put("email", "eq." + email);
                        Response<List<UserDto>> byEmail = dbService.getUsers(emailFilter, "*", null).execute();
                        if (byEmail.isSuccessful() && byEmail.body() != null && !byEmail.body().isEmpty()) {
                            User user = mapDtoToUser(byEmail.body().get(0));
                            sessionManager.createLoginSession(user, auth.accessToken, auth.refreshToken, auth.expiresIn);
                            mainHandler.post(() -> callback.onSuccess(user));
                            return;
                        }
                    } catch (Exception ignored) {}
                }

                // Profile belum ada di public.users, buat record baru
                String displayName = "Pengguna";
                if (auth.user != null && auth.user.userMetadata != null) {
                    if (auth.user.userMetadata.containsKey("full_name")) {
                        displayName = String.valueOf(auth.user.userMetadata.get("full_name"));
                    } else if (auth.user.userMetadata.containsKey("name")) {
                        displayName = String.valueOf(auth.user.userMetadata.get("name"));
                    }
                } else if (!email.isEmpty()) {
                    displayName = email.split("@")[0];
                }

                UserDto newProfile = new UserDto();
                newProfile.id = uid;
                newProfile.nama = displayName;
                newProfile.email = email;
                newProfile.role = "user";
                newProfile.avatarUrl = "avatar_male";
                newProfile.authProvider = "GOOGLE";

                try {
                    dbService.createUser(newProfile).execute();
                } catch (Exception ignored) {}

                User user = new User(uid, displayName, email, Role.USER, "");
                sessionManager.createLoginSession(user, auth.accessToken, auth.refreshToken, auth.expiresIn);
                mainHandler.post(() -> callback.onSuccess(user));
            }
        } catch (Exception e) {
            postError(callback, "Gagal memuat profil pengguna: " + e.getMessage());
        }
    }

    public void getUserById(String uid, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (uid == null || uid.isEmpty()) {
                    postError(callback, "ID Pengguna tidak valid");
                    return;
                }
                Response<List<UserDto>> res = dbService.getUserById("eq." + uid, "*").execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    User u = mapDtoToUser(res.body().get(0));
                    mainHandler.post(() -> callback.onSuccess(u));
                } else {
                    postError(callback, "Data pengguna tidak ditemukan");
                }
            } catch (Exception e) {
                postError(callback, "Gagal mengambil data user: " + e.getMessage());
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
                UserUpdateDto dto = new UserUpdateDto();
                dto.nama = nama.trim();
                dto.noHp = noHp != null ? noHp.trim() : "";
                dto.bio = bio != null ? bio.trim() : "";
                dto.avatarUrl = avatarUrl;

                Response<List<UserDto>> res = dbService.updateUser("eq." + uid, dto).execute();
                if (res.isSuccessful()) {
                    sessionManager.updateProfile(nama, noHp, bio, avatarUrl);
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal memperbarui profil di server");
                }
            } catch (Exception e) {
                postError(callback, "Kesalahan update profil: " + e.getMessage());
            }
        });
    }

    public void updateProfile(int legacyId, String nama, String noHp, String bio, String avatarUrl, DataCallback<Boolean> callback) {
        updateProfile(sessionManager.getUserUid(), nama, noHp, bio, avatarUrl, callback);
    }

    public void uploadAvatar(Uri imageUri, DataCallback<String> callback) {
        executor.execute(() -> {
            try {
                if (imageUri == null) {
                    postError(callback, "Foto profil tidak ditemukan");
                    return;
                }

                File persistentDir = new File(context.getFilesDir(), "avatars");
                if (!persistentDir.exists()) persistentDir.mkdirs();

                String ext = ".jpg";
                String fileName = "avatar_" + UUID.randomUUID().toString() + ext;
                File persistentFile = new File(persistentDir, fileName);

                try (InputStream in = context.getContentResolver().openInputStream(imageUri);
                     OutputStream out = new FileOutputStream(persistentFile)) {
                    if (in == null) {
                        postError(callback, "Gagal membaca foto dari galeri");
                        return;
                    }
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }

                String localUri = persistentFile.toURI().toString();
                String remoteName = "avatars/" + fileName;

                try {
                    RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), persistentFile);
                    MultipartBody.Part part = MultipartBody.Part.createFormData("file", remoteName, requestBody);
                    Response<StorageUploadResponse> uploadRes = storageService.uploadFile("kost-images", remoteName, part).execute();

                    if (uploadRes.isSuccessful() && uploadRes.body() != null) {
                        String publicUrl = SupabaseClient.getStoragePublicUrl("kost-images", remoteName);
                        mainHandler.post(() -> callback.onSuccess(publicUrl));
                        return;
                    }
                } catch (Exception ignored) {
                    // Jika upload Supabase Storage gagal/bucket belum tersedia, gunakan persistent local file URI
                }

                mainHandler.post(() -> callback.onSuccess(localUri));
            } catch (Exception e) {
                postError(callback, "Gagal mengunggah foto profil: " + e.getMessage());
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

                // Supabase Auth updatePassword requires valid active access token
                Response<ResponseBody> res = authService.updatePassword(new UpdatePasswordRequest(newPassword)).execute();
                if (res.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal mengubah kata sandi di Supabase");
                }
            } catch (Exception e) {
                postError(callback, "Kesalahan ganti kata sandi: " + e.getMessage());
            }
        });
    }

    public void changePassword(int legacyId, String oldPassword, String newPassword, DataCallback<Boolean> callback) {
        changePassword(sessionManager.getUserUid(), oldPassword, newPassword, callback);
    }

    public void submitOwnerVerification(String uid, String catatan, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                UserUpdateDto dto = new UserUpdateDto();
                dto.verificationStatus = VerificationStatus.PENDING.name();
                dto.pengajuanCatatan = catatan;

                Response<List<UserDto>> res = dbService.updateUser("eq." + uid, dto).execute();
                if (res.isSuccessful()) {
                    sessionManager.updateVerificationStatus(VerificationStatus.PENDING, null, null);
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal mengajukan verifikasi pemilik kost");
                }
            } catch (Exception e) {
                postError(callback, "Error pengajuan: " + e.getMessage());
            }
        });
    }

    public void submitOwnerVerification(int legacyId, String catatan, DataCallback<Boolean> callback) {
        submitOwnerVerification(sessionManager.getUserUid(), catatan, callback);
    }

    public void approveOwnerVerification(String uid, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
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
                postError(callback, "Error approval: " + e.getMessage());
            }
        });
    }

    public void approveOwnerVerification(int legacyId, DataCallback<Boolean> callback) {
        postError(callback, "Operasi memerlukan UUID user");
    }

    public void rejectOwnerVerification(String uid, String alasan, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
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
                postError(callback, "Error rejection: " + e.getMessage());
            }
        });
    }

    public void requireRevisionOwnerVerification(String uid, String catatanRevisi, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
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
                postError(callback, "Error revision: " + e.getMessage());
            }
        });
    }

    public void setUserActiveStatus(String uid, boolean isActive, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                UserUpdateDto dto = new UserUpdateDto();
                dto.isActive = isActive;

                Response<List<UserDto>> res = dbService.updateUser("eq." + uid, dto).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal mengubah status aktif user di server");
                }
            } catch (Exception e) {
                postError(callback, "Error set status: " + e.getMessage());
            }
        });
    }

    public void getPendingVerifications(DataCallback<List<User>> callback) {
        executor.execute(() -> {
            try {
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
                postError(callback, "Gagal memuat daftar pengajuan: " + e.getMessage());
            }
        });
    }

    public void getOwners(DataCallback<List<User>> callback) {
        executor.execute(() -> {
            try {
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
                postError(callback, "Gagal memuat daftar pemilik: " + e.getMessage());
            }
        });
    }

    public void deleteUser(String uid, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                Response<Void> res = dbService.deleteUser("eq." + uid).execute();
                if (res.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal menghapus pengguna dari database");
                }
            } catch (Exception e) {
                postError(callback, "Error hapus pengguna: " + e.getMessage());
            }
        });
    }

    public void getAllUsers(DataCallback<List<User>> callback) {
        executor.execute(() -> {
            try {
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
                postError(callback, "Gagal memuat pengguna: " + e.getMessage());
            }
        });
    }

    public void getUserStats(DataCallback<UserStats> callback) {
        executor.execute(() -> {
            try {
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
                postError(callback, "Gagal memuat statistik user: " + e.getMessage());
            }
        });
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
        return u;
    }

    private <T> void postError(DataCallback<T> callback, String msg) {
        mainHandler.post(() -> callback.onError(msg));
    }
}
