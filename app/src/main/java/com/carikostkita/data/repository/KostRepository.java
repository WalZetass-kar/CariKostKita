package com.carikostkita.data.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.FotoKost;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.model.KostVerificationStatus;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.model.Wilayah;
import android.util.Log;
import com.carikostkita.data.remote.SupabaseAuthService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseDbService;
import com.carikostkita.data.remote.SupabaseStorageService;
import com.carikostkita.data.remote.dto.ActivityLogDto;
import com.carikostkita.data.remote.dto.AuthResponse;
import com.carikostkita.data.remote.dto.FavoriteDto;
import com.carikostkita.data.remote.dto.KostDto;
import com.carikostkita.data.remote.dto.KostUpdateDto;
import com.carikostkita.data.remote.dto.RefreshRequest;
import com.carikostkita.data.remote.dto.StorageUploadResponse;
import com.carikostkita.util.SessionManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import retrofit2.Response;

public class KostRepository {
    private static final String TAG = "KostRepository";
    private final Context appContext;
    private final SupabaseDbService dbService;
    private final SupabaseStorageService storageService;
    private final SupabaseAuthService authService;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public static class AdminStats {
        public int totalKost;
        public int totalTersedia;
        public int totalPenuh;
        public int totalPending;
        public int totalFasilitas;
        public int kostAktif;
        public int kostTerverifikasi;
        public List<KostDto> kostList = new ArrayList<>();

        public AdminStats(int totalKost, int totalTersedia, int totalPenuh, int totalPending, int totalFasilitas) {
            this.totalKost = totalKost;
            this.totalTersedia = totalTersedia;
            this.totalPenuh = totalPenuh;
            this.totalPending = totalPending;
            this.totalFasilitas = totalFasilitas;
            this.kostAktif = totalTersedia;
            this.kostTerverifikasi = totalTersedia + totalPenuh;
        }
    }

    public static class PemilikStats {
        public int totalKost;
        public int totalAktif;
        public int totalPending;
        public int totalRevisi;
        public int totalTersedia;
        public int totalTerisi;
        public int totalFavorit = 0;
        public final List<KostDto> kostList = new ArrayList<>();

        public PemilikStats(int totalKost, int totalAktif, int totalPending, int totalRevisi, int totalTersedia, int totalTerisi) {
            this.totalKost = totalKost;
            this.totalAktif = totalAktif;
            this.totalPending = totalPending;
            this.totalRevisi = totalRevisi;
            this.totalTersedia = totalTersedia;
            this.totalTerisi = totalTerisi;
        }
    }

    public KostRepository(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        this.dbService = SupabaseClient.getInstance().createService(SupabaseDbService.class);
        this.storageService = SupabaseClient.getInstance().createService(SupabaseStorageService.class);
        this.authService = SupabaseClient.getInstance().createService(SupabaseAuthService.class);
        this.sessionManager = new SessionManager(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private void ensureValidAuthToken() {
        try {
            String token = sessionManager.getAccessToken();
            if (token != null && !token.isEmpty()) {
                SupabaseClient.getInstance().setAccessToken(token);
            }
            if (sessionManager.isTokenExpired() || token == null || token.isEmpty()) {
                String refreshToken = sessionManager.getRefreshToken();
                if (refreshToken != null && !refreshToken.isEmpty()) {
                    Response<AuthResponse> refreshRes = authService.refreshToken(new RefreshRequest(refreshToken)).execute();
                    if (refreshRes.isSuccessful() && refreshRes.body() != null) {
                        AuthResponse body = refreshRes.body();
                        sessionManager.saveTokens(body.accessToken, body.refreshToken, body.expiresIn);
                        SupabaseClient.getInstance().setAccessToken(body.accessToken);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Gagal auto-refresh token: " + e.getMessage());
        }
    }

    // =========================================================================
    // PUBLIC CATALOG & USER QUERIES
    // =========================================================================

    public void getAllActiveKost(String currentUserId, DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> filters = new HashMap<>();
                filters.put("verification_status", "eq." + KostVerificationStatus.APPROVED.name());
                filters.put("status", "neq." + StatusKost.TIDAK_AKTIF.name());

                Response<List<KostDto>> res = dbService.getKosts(filters, "*", "created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    List<Kost> list = mapDtoListToKost(res.body());
                    populateFavorites(list, currentUserId);
                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memuat katalog kost: " + e.getMessage());
            }
        });
    }

    public void getAllActiveKost(int legacyUserId, DataCallback<List<Kost>> callback) {
        getAllActiveKost(sessionManager.getUserUid(), callback);
    }

    public void searchKost(String keyword, String currentUserId, DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> filters = new HashMap<>();
                filters.put("verification_status", "eq." + KostVerificationStatus.APPROVED.name());
                filters.put("status", "neq." + StatusKost.TIDAK_AKTIF.name());

                if (keyword != null && !keyword.trim().isEmpty()) {
                    String clean = keyword.trim();
                    filters.put("or", "(nama_kost.ilike.*" + clean + "*,alamat.ilike.*" + clean + "*,kecamatan.ilike.*" + clean + "*,kelurahan.ilike.*" + clean + "*)");
                }

                Response<List<KostDto>> res = dbService.getKosts(filters, "*", "created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    List<Kost> list = mapDtoListToKost(res.body());
                    populateFavorites(list, currentUserId);
                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, "Gagal melakukan pencarian: " + e.getMessage());
            }
        });
    }

    public void searchKost(String keyword, int legacyUserId, DataCallback<List<Kost>> callback) {
        searchKost(keyword, sessionManager.getUserUid(), callback);
    }

    public void filterKost(KostFilterCriteria criteria, String currentUserId, DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> filters = new HashMap<>();
                filters.put("verification_status", "eq." + KostVerificationStatus.APPROVED.name());
                filters.put("status", "neq." + StatusKost.TIDAK_AKTIF.name());

                if (criteria != null) {
                    if (criteria.getTipeKost() != null) {
                        filters.put("tipe_kost", "eq." + criteria.getTipeKost().name());
                    }
                    if (criteria.getMaxHarga() != null && criteria.getMaxHarga() > 0) {
                        filters.put("harga", "lte." + criteria.getMaxHarga());
                    }
                    if (criteria.getMinHarga() != null && criteria.getMinHarga() > 0) {
                        filters.put("harga", "gte." + criteria.getMinHarga());
                    }
                    if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
                        String clean = criteria.getKeyword().trim();
                        filters.put("or", "(nama_kost.ilike.*" + clean + "*,alamat.ilike.*" + clean + "*,kecamatan.ilike.*" + clean + "*)");
                    }
                }

                String order = "created_at.desc";
                if (criteria != null && "TERMURAH".equalsIgnoreCase(criteria.getSortBy())) {
                    order = "harga.asc,created_at.desc";
                } else if (criteria != null && "TERMAHAL".equalsIgnoreCase(criteria.getSortBy())) {
                    order = "harga.desc,created_at.desc";
                }

                Response<List<KostDto>> res = dbService.getKosts(filters, "*", order).execute();
                if (res.isSuccessful() && res.body() != null) {
                    List<Kost> list = mapDtoListToKost(res.body());

                    // Filter fasilitas di client jika ada kriteria
                    if (criteria != null && criteria.getFasilitasIds() != null && !criteria.getFasilitasIds().isEmpty()) {
                        List<Kost> filtered = new ArrayList<>();
                        for (Kost k : list) {
                            if (matchFasilitas(k, criteria.getFasilitasIds())) {
                                filtered.add(k);
                            }
                        }
                        list = filtered;
                    }

                    populateFavorites(list, currentUserId);
                    final List<Kost> resultList = list;
                    mainHandler.post(() -> callback.onSuccess(resultList));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memfilter data kost: " + e.getMessage());
            }
        });
    }

    public void filterKost(KostFilterCriteria criteria, int legacyUserId, DataCallback<List<Kost>> callback) {
        filterKost(criteria, sessionManager.getUserUid(), callback);
    }

    private boolean matchFasilitas(Kost kost, List<Integer> reqIds) {
        if (kost.getListFasilitas() == null) return false;
        Set<Integer> existing = new HashSet<>();
        for (Fasilitas f : kost.getListFasilitas()) {
            existing.add(f.getIdFasilitas());
        }
        for (Integer req : reqIds) {
            if (!existing.contains(req)) return false;
        }
        return true;
    }

    public void getKostDetail(String idKost, String currentUserId, DataCallback<Kost> callback) {
        executor.execute(() -> {
            try {
                Response<List<KostDto>> res = dbService.getKostById("eq." + idKost, "*").execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    Kost kost = mapDtoToKost(res.body().get(0));
                    if (currentUserId != null && !currentUserId.isEmpty()) {
                        Map<String, String> favFilter = new HashMap<>();
                        favFilter.put("user_id", "eq." + currentUserId);
                        favFilter.put("kost_id", "eq." + idKost);
                        Response<List<FavoriteDto>> favRes = dbService.getFavorites(favFilter, "*").execute();
                        if (favRes.isSuccessful() && favRes.body() != null && !favRes.body().isEmpty()) {
                            kost.setFavorite(true);
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(kost));
                } else {
                    postError(callback, "Detail kost tidak ditemukan.");
                }
            } catch (Exception e) {
                postError(callback, "Gagal memuat detail kost: " + e.getMessage());
            }
        });
    }

    public void getKostDetail(int legacyId, int legacyUserId, DataCallback<Kost> callback) {
        getKostDetail(String.valueOf(legacyId), sessionManager.getUserUid(), callback);
    }

    // =========================================================================
    // FAVORITES
    // =========================================================================

    public void toggleFavorite(String userId, String idKost, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (userId == null || userId.isEmpty()) {
                    postError(callback, "Silakan login terlebih dahulu untuk menyimpan favorit.");
                    return;
                }

                ensureValidAuthToken();

                Map<String, String> favFilter = new HashMap<>();
                favFilter.put("user_id", "eq." + userId);
                favFilter.put("kost_id", "eq." + idKost);

                Response<List<FavoriteDto>> checkRes = dbService.getFavorites(favFilter, "*").execute();
                if (checkRes.isSuccessful() && checkRes.body() != null && !checkRes.body().isEmpty()) {
                    // Sudah favorit -> Hapus
                    Response<okhttp3.ResponseBody> delRes = dbService.removeFavorite("eq." + userId, "eq." + idKost).execute();
                    if (delRes.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(false));
                    } else {
                        String err = delRes.errorBody() != null ? delRes.errorBody().string() : "";
                        Log.e(TAG, "Gagal hapus favorit: code=" + delRes.code() + ", error=" + err);
                        postError(callback, "Gagal menghapus favorit: " + parseErrorMessage(delRes.code(), err));
                    }
                } else {
                    // Belum favorit -> Tambah
                    FavoriteDto fav = new FavoriteDto(userId, idKost);
                    Response<List<FavoriteDto>> addRes = dbService.addFavorite(fav).execute();
                    if (addRes.isSuccessful() && addRes.body() != null) {
                        mainHandler.post(() -> callback.onSuccess(true));
                    } else {
                        String err = addRes.errorBody() != null ? addRes.errorBody().string() : "";
                        Log.e(TAG, "Gagal simpan favorit: code=" + addRes.code() + ", error=" + err);
                        postError(callback, "Gagal menyimpan favorit: " + parseErrorMessage(addRes.code(), err));
                    }
                }
            } catch (Exception e) {
                postError(callback, "Gagal mengubah status favorit: " + e.getMessage());
            }
        });
    }

    public void toggleFavorite(int legacyUserId, int legacyKostId, DataCallback<Boolean> callback) {
        toggleFavorite(sessionManager.getUserUid(), String.valueOf(legacyKostId), callback);
    }

    public void toggleFavorite(int legacyUserId, String idKost, DataCallback<Boolean> callback) {
        toggleFavorite(sessionManager.getUserUid(), idKost, callback);
    }

    public void getFavorites(String userId, DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                if (userId == null || userId.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                    return;
                }

                Map<String, String> favFilter = new HashMap<>();
                favFilter.put("user_id", "eq." + userId);
                Response<List<FavoriteDto>> favRes = dbService.getFavorites(favFilter, "*").execute();
                if (favRes.isSuccessful() && favRes.body() != null && !favRes.body().isEmpty()) {
                    StringBuilder idsBuilder = new StringBuilder("(");
                    for (int i = 0; i < favRes.body().size(); i++) {
                        idsBuilder.append(favRes.body().get(i).kostId);
                        if (i < favRes.body().size() - 1) idsBuilder.append(",");
                    }
                    idsBuilder.append(")");

                    Map<String, String> kostFilter = new HashMap<>();
                    kostFilter.put("id", "in." + idsBuilder.toString());
                    Response<List<KostDto>> kostRes = dbService.getKosts(kostFilter, "*", "created_at.desc").execute();
                    if (kostRes.isSuccessful() && kostRes.body() != null) {
                        List<Kost> list = mapDtoListToKost(kostRes.body());
                        for (Kost k : list) k.setFavorite(true);
                        mainHandler.post(() -> callback.onSuccess(list));
                    } else {
                        mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                    }
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memuat daftar favorit: " + e.getMessage());
            }
        });
    }

    public void getFavorites(int legacyUserId, DataCallback<List<Kost>> callback) {
        getFavorites(sessionManager.getUserUid(), callback);
    }

    private void populateFavorites(List<Kost> list, String currentUserId) {
        if (currentUserId == null || currentUserId.isEmpty() || list.isEmpty()) return;
        try {
            Map<String, String> favFilter = new HashMap<>();
            favFilter.put("user_id", "eq." + currentUserId);
            Response<List<FavoriteDto>> favRes = dbService.getFavorites(favFilter, "kost_id").execute();
            if (favRes.isSuccessful() && favRes.body() != null) {
                Set<String> favSet = new HashSet<>();
                for (FavoriteDto f : favRes.body()) {
                    favSet.add(f.kostId);
                }
                for (Kost k : list) {
                    k.setFavorite(favSet.contains(k.getId()));
                }
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // PEMILIK & ADMIN OPERATIONS
    // =========================================================================

    public void getAllKostForAdmin(DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                Response<List<KostDto>> res = dbService.getKosts(new HashMap<>(), "*", "created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(mapDtoListToKost(res.body())));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memuat list admin: " + e.getMessage());
            }
        });
    }

    public void getKostByPemilik(String ownerId, DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> filters = new HashMap<>();
                filters.put("owner_id", "eq." + ownerId);

                Response<List<KostDto>> res = dbService.getKosts(filters, "*", "created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(mapDtoListToKost(res.body())));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memuat kost milik Anda: " + e.getMessage());
            }
        });
    }

    public void getKostByPemilik(int legacyId, DataCallback<List<Kost>> callback) {
        getKostByPemilik(sessionManager.getUserUid(), callback);
    }

    public void saveKost(Kost kost, List<Integer> fasilitasIds, DataCallback<Long> callback) {
        saveKostWithFotos(kost, fasilitasIds, null, callback);
    }

    public void saveKostWithFotos(Kost kost, List<Integer> fasilitasIds, List<FotoKost> fotos, DataCallback<Long> callback) {
        executor.execute(() -> {
            try {
                ensureValidAuthToken();

                if (kost.getOwnerId() == null || kost.getOwnerId().isEmpty()) {
                    kost.setOwnerId(sessionManager.getUserUid());
                }

                // Upload foto-foto baru jika ada
                List<String> uploadedUrls = uploadFotos(fotos);
                if (!uploadedUrls.isEmpty()) {
                    kost.setImageUrls(uploadedUrls);
                    String selectedThumb = uploadedUrls.get(0);
                    if (fotos != null) {
                        for (int i = 0; i < fotos.size() && i < uploadedUrls.size(); i++) {
                            if (fotos.get(i).isThumbnail()) {
                                selectedThumb = uploadedUrls.get(i);
                                break;
                            }
                        }
                    }
                    kost.setThumbnailUrl(selectedThumb);
                }

                // Map fasilitas integer IDs ke nama fasilitas
                if (fasilitasIds != null && !fasilitasIds.isEmpty()) {
                    List<String> fNames = new ArrayList<>();
                    for (int id : fasilitasIds) {
                        fNames.add(getFasilitasNameById(id));
                    }
                    kost.setFasilitas(fNames);
                }

                KostDto dto = mapKostToDto(kost);
                if (dto.ownerId == null || dto.ownerId.isEmpty()) {
                    dto.ownerId = sessionManager.getUserUid();
                }

                Response<List<KostDto>> res = dbService.insertKost(dto).execute();
                if (!res.isSuccessful() && (res.code() == 401 || res.code() == 403)) {
                    String refreshToken = sessionManager.getRefreshToken();
                    if (refreshToken != null && !refreshToken.isEmpty()) {
                        try {
                            Response<AuthResponse> refreshRes = authService.refreshToken(new RefreshRequest(refreshToken)).execute();
                            if (refreshRes.isSuccessful() && refreshRes.body() != null) {
                                AuthResponse authBody = refreshRes.body();
                                sessionManager.saveTokens(authBody.accessToken, authBody.refreshToken, authBody.expiresIn);
                                SupabaseClient.getInstance().setAccessToken(authBody.accessToken);
                                res = dbService.insertKost(dto).execute();
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(1L));
                } else {
                    String errDetail = "";
                    if (res.errorBody() != null) {
                        try {
                            errDetail = res.errorBody().string();
                        } catch (Exception ignored) {}
                    }
                    Log.e(TAG, "Gagal simpan kost: code=" + res.code() + ", error=" + errDetail);
                    String userMsg = "Gagal menyimpan data kost di Supabase";
                    if (errDetail.contains("42501")) {
                        userMsg = "Izin database ditolak (42501): Kebijakan Row-Level Security (RLS) di Supabase membatasi penambahan kost. Jalankan script SQL perbaikan di Supabase Dashboard.";
                    } else if (res.code() == 401 || res.code() == 403) {
                        userMsg = "Akses ditolak (HTTP " + res.code() + "). Sesi akun Anda tidak memiliki izin. Silakan coba login ulang.";
                    } else if (!errDetail.isEmpty() && errDetail.contains("\"message\"")) {
                        try {
                            JSONObject obj = new JSONObject(errDetail);
                            userMsg = obj.optString("message", userMsg);
                        } catch (Exception ignored) {}
                    }
                    postError(callback, userMsg);
                }
            } catch (Exception e) {
                postError(callback, "Kesalahan simpan kost: " + e.getMessage());
            }
        });
    }

    public void updateKost(Kost kost, List<Integer> fasilitasIds, DataCallback<Boolean> callback) {
        updateKostWithFotos(kost, fasilitasIds, null, callback);
    }

    public void updateKostWithFotos(Kost kost, List<Integer> fasilitasIds, List<FotoKost> fotos, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                ensureValidAuthToken();

                List<String> uploadedUrls = uploadFotos(fotos);
                if (!uploadedUrls.isEmpty()) {
                    kost.setImageUrls(uploadedUrls);
                    String selectedThumb = uploadedUrls.get(0);
                    if (fotos != null) {
                        for (int i = 0; i < fotos.size() && i < uploadedUrls.size(); i++) {
                            if (fotos.get(i).isThumbnail()) {
                                selectedThumb = uploadedUrls.get(i);
                                break;
                            }
                        }
                    }
                    kost.setThumbnailUrl(selectedThumb);
                }

                if (fasilitasIds != null && !fasilitasIds.isEmpty()) {
                    List<String> fNames = new ArrayList<>();
                    for (int id : fasilitasIds) {
                        fNames.add(getFasilitasNameById(id));
                    }
                    kost.setFasilitas(fNames);
                }

                KostUpdateDto dto = mapKostToUpdateDto(kost);
                Response<List<KostDto>> res = dbService.updateKost("eq." + kost.getId(), dto).execute();
                if (!res.isSuccessful() && (res.code() == 401 || res.code() == 403)) {
                    String refreshToken = sessionManager.getRefreshToken();
                    if (refreshToken != null && !refreshToken.isEmpty()) {
                        try {
                            Response<AuthResponse> refreshRes = authService.refreshToken(new RefreshRequest(refreshToken)).execute();
                            if (refreshRes.isSuccessful() && refreshRes.body() != null) {
                                AuthResponse authBody = refreshRes.body();
                                sessionManager.saveTokens(authBody.accessToken, authBody.refreshToken, authBody.expiresIn);
                                SupabaseClient.getInstance().setAccessToken(authBody.accessToken);
                                res = dbService.updateKost("eq." + kost.getId(), dto).execute();
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (res.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errDetail = "";
                    if (res.errorBody() != null) {
                        try {
                            errDetail = res.errorBody().string();
                        } catch (Exception ignored) {}
                    }
                    Log.e(TAG, "Gagal update kost: code=" + res.code() + ", error=" + errDetail);
                    String userMsg = "Gagal mengupdate data kost di server";
                    if (errDetail.contains("42501")) {
                        userMsg = "Izin database ditolak (42501): Kebijakan Row-Level Security (RLS) di Supabase membatasi perubahan kost. Jalankan script SQL perbaikan di Supabase Dashboard.";
                    } else if (res.code() == 401 || res.code() == 403) {
                        userMsg = "Akses ditolak (HTTP " + res.code() + "). Sesi akun Anda tidak memiliki izin. Silakan coba login ulang.";
                    } else if (!errDetail.isEmpty() && errDetail.contains("\"message\"")) {
                        try {
                            JSONObject obj = new JSONObject(errDetail);
                            userMsg = obj.optString("message", userMsg);
                        } catch (Exception ignored) {}
                    }
                    postError(callback, userMsg);
                }
            } catch (Exception e) {
                postError(callback, "Kesalahan update kost: " + e.getMessage());
            }
        });
    }

    private List<String> uploadFotos(List<FotoKost> fotos) {
        List<String> urls = new ArrayList<>();
        if (fotos == null || fotos.isEmpty()) return urls;

        for (FotoKost f : fotos) {
            String path = f.getPathFile();
            if (path == null || path.trim().isEmpty()) continue;

            if (path.startsWith("http://") || path.startsWith("https://")) {
                urls.add(path);
                continue;
            }

            byte[] fileBytes = null;
            String mimeType = "image/jpeg";
            String ext = ".jpg";

            try {
                if (path.startsWith("content://") && appContext != null) {
                    Uri contentUri = Uri.parse(path);
                    String resolvedType = appContext.getContentResolver().getType(contentUri);
                    if (resolvedType != null) {
                        mimeType = resolvedType;
                        if (mimeType.contains("png")) ext = ".png";
                        else if (mimeType.contains("webp")) ext = ".webp";
                    }
                    try (InputStream is = appContext.getContentResolver().openInputStream(contentUri);
                         ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                        if (is != null) {
                            byte[] data = new byte[8192];
                            int nRead;
                            while ((nRead = is.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, nRead);
                            }
                            buffer.flush();
                            fileBytes = buffer.toByteArray();
                        }
                    }
                } else {
                    File file = new File(path);
                    if (file.exists() && file.isFile()) {
                        if (path.contains(".")) {
                            ext = path.substring(path.lastIndexOf(".")).toLowerCase(Locale.ROOT);
                            if (ext.equals(".png")) mimeType = "image/png";
                            else if (ext.equals(".webp")) mimeType = "image/webp";
                        }
                        try (FileInputStream fis = new FileInputStream(file);
                             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                            byte[] data = new byte[8192];
                            int nRead;
                            while ((nRead = fis.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, nRead);
                            }
                            buffer.flush();
                            fileBytes = buffer.toByteArray();
                        }
                    }
                }

                if (fileBytes != null && fileBytes.length > 0) {
                    String remoteName = UUID.randomUUID().toString() + ext;
                    RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), fileBytes);

                    Response<StorageUploadResponse> uploadRes = storageService.uploadFileBinary("kost-images", remoteName, mimeType, requestBody).execute();
                    if (uploadRes.isSuccessful()) {
                        String publicUrl = SupabaseClient.getStoragePublicUrl("kost-images", remoteName);
                        urls.add(publicUrl);
                        Log.d(TAG, "Berhasil upload foto kost ke Supabase Storage: " + publicUrl);
                    } else {
                        String errStr = uploadRes.errorBody() != null ? uploadRes.errorBody().string() : "";
                        Log.e(TAG, "Gagal upload foto ke Supabase Storage: code=" + uploadRes.code() + ", error=" + errStr);
                    }
                } else {
                    Log.w(TAG, "File foto tidak dapat dibaca dari path: " + path);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception upload foto kost: " + e.getMessage(), e);
            }
        }
        return urls;
    }

    private String parseErrorMessage(int code, String errDetail) {
        if (errDetail != null && errDetail.contains("42501")) {
            return "Izin database ditolak (42501): Kebijakan Row-Level Security (RLS) di Supabase membatasi aksi ini. Jalankan script SQL perbaikan di Supabase Dashboard.";
        } else if (code == 401 || code == 403) {
            return "Akses ditolak (HTTP " + code + "). Sesi akun Anda telah berakhir. Silakan login kembali.";
        } else if (errDetail != null && errDetail.contains("\"message\"")) {
            try {
                JSONObject obj = new JSONObject(errDetail);
                return obj.optString("message", "Terjadi kesalahan server (HTTP " + code + ")");
            } catch (Exception ignored) {}
        }
        return "Terjadi kesalahan server (HTTP " + code + ")";
    }

    public void updateStatus(String idKost, StatusKost status, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                KostUpdateDto dto = new KostUpdateDto();
                dto.status = status.name();

                Response<List<KostDto>> res = dbService.updateKost("eq." + idKost, dto).execute();
                if (res.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal mengubah status kost");
                }
            } catch (Exception e) {
                postError(callback, "Kesalahan update status: " + e.getMessage());
            }
        });
    }

    public void updateStatus(int legacyId, StatusKost status, DataCallback<Boolean> callback) {
        updateStatus(String.valueOf(legacyId), status, callback);
    }

    public void getPendingKosts(DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> filters = new HashMap<>();
                filters.put("verification_status", "in.(" + KostVerificationStatus.PENDING.name() + "," + KostVerificationStatus.REVISION_REQUIRED.name() + ")");

                Response<List<KostDto>> res = dbService.getKosts(filters, "*", "created_at.desc").execute();
                if (res.isSuccessful() && res.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(mapDtoListToKost(res.body())));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memuat antrean verifikasi kost: " + e.getMessage());
            }
        });
    }

    public void updateKostVerification(String idKost, KostVerificationStatus status, String catatanRevisi,
                                      String adminId, String adminName, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                KostUpdateDto dto = new KostUpdateDto();
                dto.verificationStatus = status.name();
                if (catatanRevisi != null) dto.catatanRevisi = catatanRevisi;

                Response<List<KostDto>> res = dbService.updateKost("eq." + idKost, dto).execute();
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    // Log ke activity_logs
                    ActivityLogDto log = new ActivityLogDto(
                            adminId,
                            adminName != null ? adminName : "Admin",
                            "VERIFIKASI_KOST",
                            "Kost #" + idKost + " diubah status verifikasi menjadi " + status.getDisplayName() + (catatanRevisi != null && !catatanRevisi.isEmpty() ? " (" + catatanRevisi + ")" : ""),
                            "KOST",
                            idKost
                    );
                    try {
                        dbService.insertLog(log).execute();
                    } catch (Exception ignored) {}

                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal memperbarui verifikasi kost");
                }
            } catch (Exception e) {
                postError(callback, "Kesalahan verifikasi kost: " + e.getMessage());
            }
        });
    }

    public void updateVerificationStatus(String idKost, KostVerificationStatus status, String catatanRevisi, DataCallback<Boolean> callback) {
        updateKostVerification(idKost, status, catatanRevisi, sessionManager.getUserUid(), sessionManager.getUserName(), callback);
    }

    public void updateVerificationStatus(int legacyId, KostVerificationStatus status, String catatanRevisi, DataCallback<Boolean> callback) {
        updateVerificationStatus(String.valueOf(legacyId), status, catatanRevisi, callback);
    }

    public void getAdminStats(DataCallback<AdminStats> callback) {
        executor.execute(() -> {
            try {
                Response<List<KostDto>> res = dbService.getKosts(new HashMap<>(), "*", null).execute();
                if (res.isSuccessful() && res.body() != null) {
                    int total = res.body().size();
                    int tersedia = 0;
                    int penuh = 0;
                    int pending = 0;
                    for (KostDto k : res.body()) {
                        if ("TERSEDIA".equalsIgnoreCase(k.status)) tersedia++;
                        if ("PENUH".equalsIgnoreCase(k.status)) penuh++;
                        if ("PENDING".equalsIgnoreCase(k.verificationStatus) || "REVISION_REQUIRED".equalsIgnoreCase(k.verificationStatus)) pending++;
                    }
                    AdminStats stats = new AdminStats(total, tersedia, penuh, pending, getMasterFasilitas().size());
                    stats.kostList.addAll(res.body());
                    stats.kostAktif = tersedia;
                    stats.kostTerverifikasi = total - pending;
                    mainHandler.post(() -> callback.onSuccess(stats));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new AdminStats(0, 0, 0, 0, 0)));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memuat statistik admin: " + e.getMessage());
            }
        });
    }

    public void getPemilikStats(String ownerId, DataCallback<PemilikStats> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> filters = new HashMap<>();
                filters.put("owner_id", "eq." + ownerId);

                Response<List<KostDto>> res = dbService.getKosts(filters, "*", null).execute();
                if (res.isSuccessful() && res.body() != null) {
                    int total = res.body().size();
                    int aktif = 0;
                    int pending = 0;
                    int revisi = 0;
                    int tersedia = 0;
                    int terisi = 0;
                    for (KostDto k : res.body()) {
                        if ("APPROVED".equalsIgnoreCase(k.verificationStatus)) {
                            if ("TERSEDIA".equalsIgnoreCase(k.status)) aktif++;
                        } else if ("PENDING".equalsIgnoreCase(k.verificationStatus)) {
                            pending++;
                        } else if ("REVISION_REQUIRED".equalsIgnoreCase(k.verificationStatus)) {
                            revisi++;
                        }
                        if (k.kamarTersedia != null) tersedia += k.kamarTersedia;
                        if (k.totalKamar != null && k.kamarTersedia != null) {
                            terisi += Math.max(0, k.totalKamar - k.kamarTersedia);
                        }
                    }
                    PemilikStats stats = new PemilikStats(total, aktif, pending, revisi, tersedia, terisi);
                    stats.kostList.addAll(res.body());

                    if (!res.body().isEmpty()) {
                        StringBuilder inFilter = new StringBuilder("(");
                        for (int i = 0; i < res.body().size(); i++) {
                            inFilter.append(res.body().get(i).id);
                            if (i < res.body().size() - 1) inFilter.append(",");
                        }
                        inFilter.append(")");
                        try {
                            Map<String, String> favFilter = new HashMap<>();
                            favFilter.put("kost_id", "in." + inFilter.toString());
                            Response<List<FavoriteDto>> favRes = dbService.getFavorites(favFilter, "id").execute();
                            if (favRes.isSuccessful() && favRes.body() != null) {
                                stats.totalFavorit = favRes.body().size();
                            }
                        } catch (Exception ignored) {}
                    }

                    mainHandler.post(() -> callback.onSuccess(stats));
                } else {
                    mainHandler.post(() -> callback.onSuccess(new PemilikStats(0, 0, 0, 0, 0, 0)));
                }
            } catch (Exception e) {
                postError(callback, "Gagal memuat statistik pemilik: " + e.getMessage());
            }
        });
    }

    public void getPemilikStats(int legacyId, DataCallback<PemilikStats> callback) {
        getPemilikStats(sessionManager.getUserUid(), callback);
    }

    // =========================================================================
    // MASTER DATA (FASILITAS & WILAYAH)
    // =========================================================================

    public void getAllFasilitas(DataCallback<List<Fasilitas>> callback) {
        mainHandler.post(() -> callback.onSuccess(getMasterFasilitas()));
    }

    public void getAllWilayah(DataCallback<List<Wilayah>> callback) {
        mainHandler.post(() -> callback.onSuccess(getMasterWilayah()));
    }

    public static List<Fasilitas> getMasterFasilitas() {
        List<Fasilitas> list = new ArrayList<>();
        list.add(new Fasilitas(1, "WiFi Cepat"));
        list.add(new Fasilitas(2, "Parkir Motor"));
        list.add(new Fasilitas(3, "Parkir Mobil"));
        list.add(new Fasilitas(4, "AC Dingin"));
        list.add(new Fasilitas(5, "Kamar Mandi Dalam"));
        list.add(new Fasilitas(6, "Kasur Springbed"));
        list.add(new Fasilitas(7, "Lemari Pakaian"));
        list.add(new Fasilitas(8, "Meja & Kursi Belajar"));
        list.add(new Fasilitas(9, "Dapur Bersama"));
        list.add(new Fasilitas(10, "Listrik Termasuk"));
        list.add(new Fasilitas(11, "Akses 24 Jam"));
        list.add(new Fasilitas(12, "CCTV & Keamanan"));
        return list;
    }

    private static String getFasilitasNameById(int id) {
        for (Fasilitas f : getMasterFasilitas()) {
            if (f.getIdFasilitas() == id) return f.getNamaFasilitas();
        }
        return "Fasilitas #" + id;
    }

    public static List<Wilayah> getMasterWilayah() {
        List<Wilayah> list = new ArrayList<>();
        list.add(new Wilayah(1, "Bukit Raya", "Simpang Tiga", "Pekanbaru"));
        list.add(new Wilayah(2, "Bukit Raya", "Tangerang Selatan", "Pekanbaru"));
        list.add(new Wilayah(3, "Tampan / Binawidya", "Tuah Karya", "Pekanbaru"));
        list.add(new Wilayah(4, "Tampan / Binawidya", "Simpang Baru", "Pekanbaru"));
        list.add(new Wilayah(5, "Marpoyan Damai", "Sidomulyo Timur", "Pekanbaru"));
        list.add(new Wilayah(6, "Marpoyan Damai", "Wonorejo", "Pekanbaru"));
        list.add(new Wilayah(7, "Sukajadi", "Kampung Melayu", "Pekanbaru"));
        list.add(new Wilayah(8, "Payung Sekaki", "Labuh Baru", "Pekanbaru"));
        list.add(new Wilayah(9, "Tenayan Raya", "Rejosari", "Pekanbaru"));
        list.add(new Wilayah(10, "Rumbai", "Limbungan", "Pekanbaru"));
        return list;
    }

    // =========================================================================
    // MAPPERS
    // =========================================================================

    private List<Kost> mapDtoListToKost(List<KostDto> dtos) {
        List<Kost> list = new ArrayList<>();
        if (dtos != null) {
            for (KostDto d : dtos) {
                list.add(mapDtoToKost(d));
            }
        }
        return list;
    }

    private Kost mapDtoToKost(KostDto dto) {
        Kost k = new Kost();
        k.setId(dto.id);
        k.setOwnerId(dto.ownerId);
        k.setNamaKost(dto.namaKost);
        k.setAlamat(dto.alamat);
        k.setPatokan(dto.patokan);
        k.setHarga(dto.harga != null ? dto.harga : 0);
        k.setTipeKost(TipeKost.fromString(dto.tipeKost));
        k.setDeskripsi(dto.deskripsi);
        k.setNoWhatsapp(dto.noWhatsapp);
        k.setLatitude(dto.latitude != null ? dto.latitude : 0);
        k.setLongitude(dto.longitude != null ? dto.longitude : 0);
        k.setStatus(StatusKost.fromString(dto.status));
        k.setVerificationStatus(KostVerificationStatus.fromString(dto.verificationStatus));
        k.setCatatanRevisi(dto.catatanRevisi);
        k.setProvinsi(dto.provinsi);
        k.setKota(dto.kota);
        k.setKecamatan(dto.kecamatan);
        k.setKelurahan(dto.kelurahan);
        k.setUkuranKamar(dto.ukuranKamar);
        k.setTotalKamar(dto.totalKamar != null ? dto.totalKamar : 10);
        k.setKamarTersedia(dto.kamarTersedia != null ? dto.kamarTersedia : 3);
        // Sanitasi thumbnail dan image URLs agar URI lokal content:// atau file:// tidak merusak Glide
        String validThumb = dto.thumbnailUrl;
        if (validThumb != null && (validThumb.startsWith("content://") || validThumb.startsWith("file://"))) {
            validThumb = null;
        }
        List<String> validImages = new ArrayList<>();
        if (dto.imageUrls != null) {
            for (String img : dto.imageUrls) {
                if (img != null && (img.startsWith("http://") || img.startsWith("https://"))) {
                    validImages.add(img);
                }
            }
        }
        if (validThumb == null && !validImages.isEmpty()) {
            validThumb = validImages.get(0);
        }
        k.setThumbnailUrl(validThumb);
        k.setImageUrls(validImages);
        k.setFasilitas(dto.fasilitas);
        k.setCreatedAt(dto.createdAt);
        k.setUpdatedAt(dto.updatedAt);
        return k;
    }

    private KostDto mapKostToDto(Kost kost) {
        KostDto dto = new KostDto();
        dto.ownerId = kost.getOwnerId();
        dto.namaKost = kost.getNamaKost();
        dto.alamat = kost.getAlamat();
        dto.patokan = kost.getPatokan();
        dto.harga = kost.getHarga();
        dto.tipeKost = kost.getTipeKost() != null ? kost.getTipeKost().name() : "CAMPUR";
        dto.deskripsi = kost.getDeskripsi();
        dto.noWhatsapp = kost.getNoWhatsapp();
        dto.latitude = kost.getLatitude();
        dto.longitude = kost.getLongitude();
        dto.status = kost.getStatus() != null ? kost.getStatus().name() : "TERSEDIA";
        dto.verificationStatus = kost.getVerificationStatus() != null ? kost.getVerificationStatus().name() : "PENDING";
        dto.catatanRevisi = kost.getCatatanRevisi();
        dto.provinsi = kost.getProvinsi();
        dto.kota = kost.getKota();
        dto.kecamatan = kost.getKecamatan();
        dto.kelurahan = kost.getKelurahan();
        dto.ukuranKamar = kost.getUkuranKamar();
        dto.totalKamar = kost.getTotalKamar();
        dto.kamarTersedia = kost.getKamarTersedia();
        dto.thumbnailUrl = kost.getThumbnailUrl();
        dto.imageUrls = kost.getImageUrls();
        dto.fasilitas = kost.getFasilitas();
        return dto;
    }

    private KostUpdateDto mapKostToUpdateDto(Kost kost) {
        KostUpdateDto dto = new KostUpdateDto();
        dto.namaKost = kost.getNamaKost();
        dto.alamat = kost.getAlamat();
        dto.patokan = kost.getPatokan();
        dto.harga = kost.getHarga();
        dto.tipeKost = kost.getTipeKost() != null ? kost.getTipeKost().name() : "CAMPUR";
        dto.deskripsi = kost.getDeskripsi();
        dto.noWhatsapp = kost.getNoWhatsapp();
        dto.latitude = kost.getLatitude();
        dto.longitude = kost.getLongitude();
        dto.status = kost.getStatus() != null ? kost.getStatus().name() : "TERSEDIA";
        dto.verificationStatus = kost.getVerificationStatus() != null ? kost.getVerificationStatus().name() : "PENDING";
        dto.catatanRevisi = kost.getCatatanRevisi();
        dto.provinsi = kost.getProvinsi();
        dto.kota = kost.getKota();
        dto.kecamatan = kost.getKecamatan();
        dto.kelurahan = kost.getKelurahan();
        dto.ukuranKamar = kost.getUkuranKamar();
        dto.totalKamar = kost.getTotalKamar();
        dto.kamarTersedia = kost.getKamarTersedia();
        dto.thumbnailUrl = kost.getThumbnailUrl();
        dto.imageUrls = kost.getImageUrls();
        dto.fasilitas = kost.getFasilitas();
        return dto;
    }

    private <T> void postError(DataCallback<T> callback, String msg) {
        mainHandler.post(() -> callback.onError(msg));
    }
}
