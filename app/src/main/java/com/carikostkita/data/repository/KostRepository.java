package com.carikostkita.data.repository;



import android.content.Context;

import android.graphics.Bitmap;

import android.graphics.BitmapFactory;

import android.graphics.Matrix;

import android.media.ExifInterface;

import android.net.Uri;

import android.os.Handler;

import android.os.Looper;

import android.util.Log;

import com.carikostkita.data.model.Fasilitas;

import com.carikostkita.data.model.FotoKost;

import com.carikostkita.data.model.Kost;

import com.carikostkita.data.model.KostFilterCriteria;

import com.carikostkita.data.model.KostVerificationStatus;

import com.carikostkita.data.model.StatusKost;

import com.carikostkita.data.model.TipeKost;

import com.carikostkita.data.model.Wilayah;

import com.carikostkita.data.remote.SupabaseClient;

import com.carikostkita.data.remote.SupabaseDbService;

import com.carikostkita.data.remote.SupabaseStorageService;

import com.carikostkita.data.remote.FastApiKostService;

import com.carikostkita.data.remote.FastApiFavoriteService;

import com.carikostkita.data.remote.FastApiAdminKostService;

import com.carikostkita.data.remote.FastApiOwnerKostService;

import com.carikostkita.data.remote.FastApiOwnerStatsService;

import com.carikostkita.data.remote.dto.FavoriteDto;

import com.carikostkita.data.remote.dto.FavoriteListResponse;

import com.carikostkita.data.remote.dto.KostDto;

import com.carikostkita.data.remote.dto.KostUpdateDto;

import com.carikostkita.data.remote.dto.StorageUploadResponse;

import com.carikostkita.util.ErrorMessages;

import com.carikostkita.util.SessionManager;

import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;

import java.io.File;

import java.io.FileInputStream;

import java.io.InputStream;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.HashSet;

import java.util.List;

import java.util.Map;

import java.util.Set;

import java.util.UUID;

import java.util.concurrent.ExecutorService;

import java.util.concurrent.Executors;

import okhttp3.MediaType;

import okhttp3.RequestBody;

import okhttp3.ResponseBody;

import retrofit2.Response;



public class KostRepository {

    private static final String TAG = "KostRepository";

    private static final String BUCKET = "kost-images";

    private static final int MAX_PHOTO_EDGE_PX = 1600;

    private static final int PHOTO_JPEG_QUALITY = 82;



    private final Context appContext;

    private final SupabaseDbService dbService;

    private final FastApiKostService kostService;

    private final FastApiFavoriteService favoriteService;

    private final FastApiOwnerStatsService ownerStatsService;

    private final FastApiOwnerKostService ownerKostService;

    private final FastApiAdminKostService adminKostService;

    private final ActivityLogRepository activityLogRepository;

    private final SupabaseStorageService storageService;

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

        /** Jumlah kost yang belum mengisi data kamar (tidak ikut dihitung di okupansi). */

        public int kostTanpaDataKamar = 0;

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



    /** Hasil simpan/ubah kost, termasuk jumlah foto yang gagal diunggah. */

    public static class SaveResult {

        public final String kostId;

        public final int failedUploads;

        public final boolean sentToReview;



        public SaveResult(String kostId, int failedUploads, boolean sentToReview) {

            this.kostId = kostId;

            this.failedUploads = failedUploads;

            this.sentToReview = sentToReview;

        }

    }



    public KostRepository(Context context) {

        this.appContext = context != null ? context.getApplicationContext() : null;

        this.dbService = SupabaseClient.getInstance().createService(SupabaseDbService.class);

        this.kostService = SupabaseClient.getInstance().createKostService();

        this.favoriteService = SupabaseClient.getInstance().createFavoriteService();

        this.ownerStatsService = SupabaseClient.getInstance().createOwnerStatsService();

        this.ownerKostService = SupabaseClient.getInstance().createOwnerKostService();

        this.adminKostService = SupabaseClient.getInstance().createAdminKostService();

        this.activityLogRepository = new ActivityLogRepository(context);

        this.storageService = SupabaseClient.getInstance().createService(SupabaseStorageService.class);

        this.sessionManager = new SessionManager(context);

        this.executor = Executors.newSingleThreadExecutor();

        this.mainHandler = new Handler(Looper.getMainLooper());

    }



    // =========================================================================

    // PUBLIC CATALOG & USER QUERIES

    // =========================================================================



    private Map<String, String> publicCatalogFilters() {

        Map<String, String> filters = new HashMap<>();

        filters.put("verification_status", "eq." + KostVerificationStatus.APPROVED.name());

        filters.put("status", "neq." + StatusKost.TIDAK_AKTIF.name());

        return filters;

    }



    /** Hapus karakter yang punya arti khusus di filter PostgREST agar query tidak rusak. */

    static String sanitizeKeyword(String keyword) {

        if (keyword == null) return "";

        return keyword.replaceAll("[^\\p{L}\\p{N}\\s\\-']", " ").replaceAll("\\s+", " ").trim();

    }



    public void getAllActiveKost(String currentUserId, DataCallback<List<Kost>> callback) {

        executor.execute(() -> {

            try {

                try {

                    Response<com.carikostkita.data.remote.dto.KostListResponse> apiRes =

                            kostService.listPublicKosts(null, null, null, null, null, null, null, null, null, "TERBARU", 1, 50).execute();

                    if (apiRes.isSuccessful() && apiRes.body() != null && apiRes.body().data != null) {

                        List<Kost> list = mapDtoListToKost(apiRes.body().data);

                        boolean allZero = !list.isEmpty() && list.stream().allMatch(k -> k.getHarga() <= 0);

                        if (!allZero) {

                            populateFavorites(list, currentUserId);

                            mainHandler.post(() -> callback.onSuccess(list));

                            return;

                        }

                        Log.w(TAG, "FastAPI listPublicKosts returned zero-price items, falling back to PostgREST");

                    }

                } catch (Exception apiEx) {

                    Log.w(TAG, "FastAPI listPublicKosts fallback to PostgREST: " + apiEx.getMessage());

                }



                Response<List<KostDto>> res = dbService.getKosts(publicCatalogFilters(), "*", "created_at.desc").execute();

                if (res.isSuccessful() && res.body() != null) {

                    List<Kost> list = mapDtoListToKost(res.body());

                    populateFavorites(list, currentUserId);

                    mainHandler.post(() -> callback.onSuccess(list));

                } else {

                    postError(callback, ErrorMessages.fromResponse("getAllActiveKost", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("getAllActiveKost", e));

            }

        });

    }



    public void getAllActiveKost(int legacyUserId, DataCallback<List<Kost>> callback) {

        getAllActiveKost(sessionManager.getUserUid(), callback);

    }



    public void searchKost(String keyword, String currentUserId, DataCallback<List<Kost>> callback) {

        KostFilterCriteria criteria = new KostFilterCriteria();

        criteria.setKeyword(keyword);

        filterKost(criteria, currentUserId, callback);

    }



    public void searchKost(String keyword, int legacyUserId, DataCallback<List<Kost>> callback) {

        searchKost(keyword, sessionManager.getUserUid(), callback);

    }



    public void filterKost(KostFilterCriteria criteria, String currentUserId, DataCallback<List<Kost>> callback) {

        executor.execute(() -> {

            try {

                // 1. Coba kueri filter komprehensif melalui FastAPI

                try {

                    String queryParam = criteria != null ? sanitizeKeyword(criteria.getKeyword()) : null;

                    if (queryParam != null && queryParam.isEmpty()) queryParam = null;



                    String kecamatanParam = criteria != null && criteria.getKecamatan() != null && !criteria.getKecamatan().trim().isEmpty()

                            ? criteria.getKecamatan().trim() : null;



                    String tipeKostParam = criteria != null && criteria.getTipeKost() != null

                            ? criteria.getTipeKost().name() : null;



                    Double hargaMinParam = criteria != null && criteria.getMinHarga() != null && criteria.getMinHarga() > 0

                            ? criteria.getMinHarga() : null;



                    Double hargaMaxParam = criteria != null && criteria.getMaxHarga() != null && criteria.getMaxHarga() > 0

                            ? criteria.getMaxHarga() : null;



                    Boolean hanyaTersediaParam = criteria != null && criteria.getStatus() == StatusKost.TERSEDIA

                            ? true : null;



                    String sortParam = criteria != null && criteria.getSortBy() != null

                            ? criteria.getSortBy() : "TERBARU";



                    String fasilitasParam = null;

                    if (criteria != null && criteria.getFasilitasIds() != null && !criteria.getFasilitasIds().isEmpty()) {

                        List<String> names = new ArrayList<>();

                        for (Integer fid : criteria.getFasilitasIds()) {

                            String fname = Fasilitas.nameForId(fid);

                            if (fname != null && !fname.isEmpty()) {

                                names.add(fname);

                            }

                        }

                        if (!names.isEmpty()) {

                            fasilitasParam = android.text.TextUtils.join(",", names);

                        }

                    }



                    Response<com.carikostkita.data.remote.dto.KostListResponse> apiRes =

                            kostService.listPublicKosts(

                                    queryParam,

                                    null,

                                    null,

                                    kecamatanParam,

                                    tipeKostParam,

                                    hargaMinParam,

                                    hargaMaxParam,

                                    fasilitasParam,

                                    hanyaTersediaParam,

                                    sortParam,

                                    1,

                                    100

                            ).execute();



                    if (apiRes.isSuccessful() && apiRes.body() != null && apiRes.body().data != null) {

                        List<Kost> list = mapDtoListToKost(apiRes.body().data);

                        boolean allZero = !list.isEmpty() && list.stream().allMatch(k -> k.getHarga() <= 0);

                        if (!allZero) {

                            list = applyDistance(list, criteria);

                            populateFavorites(list, currentUserId);

                            final List<Kost> resultList = list;

                            mainHandler.post(() -> callback.onSuccess(resultList));

                            return;

                        }

                        Log.w(TAG, "FastAPI filterKost returned zero-price items, falling back to PostgREST");

                    }

                } catch (Exception apiEx) {

                    Log.w(TAG, "FastAPI filterKost fallback to PostgREST: " + apiEx.getMessage());

                }



                // 2. Fallback aman ke PostgREST jika FastAPI mengalami kendala jaringan

                Map<String, String> filters = publicCatalogFilters();

                List<String> andConditions = new ArrayList<>();



                if (criteria != null) {

                    if (criteria.getTipeKost() != null) {

                        filters.put("gender_type", "eq." + criteria.getTipeKost().name());

                    }

                    if (criteria.getMinHarga() != null && criteria.getMinHarga() > 0) {

                        andConditions.add("price_min.gte." + criteria.getMinHarga().longValue());

                    }

                    if (criteria.getMaxHarga() != null && criteria.getMaxHarga() > 0) {

                        andConditions.add("price_min.lte." + criteria.getMaxHarga().longValue());

                    }

                    if (criteria.getStatus() == StatusKost.TERSEDIA) {

                        filters.put("status", "eq." + StatusKost.TERSEDIA.name());

                        andConditions.add("or(kamar_tersedia.is.null,kamar_tersedia.gt.0)");

                    }

                    if (criteria.getKecamatan() != null && !criteria.getKecamatan().trim().isEmpty()) {

                        filters.put("district", "ilike." + sanitizeKeyword(criteria.getKecamatan()));

                    }

                    String clean = sanitizeKeyword(criteria.getKeyword());

                    if (!clean.isEmpty()) {

                        filters.put("or", "(name.ilike.*" + clean + "*,nama_kost.ilike.*" + clean

                                + "*,address.ilike.*" + clean + "*,district.ilike.*" + clean

                                + "*,village.ilike.*" + clean + "*,city.ilike.*" + clean + "*)");

                    }

                }

                if (!andConditions.isEmpty()) {

                    filters.put("and", "(" + android.text.TextUtils.join(",", andConditions) + ")");

                }



                String order = "created_at.desc";

                if (criteria != null && "TERMURAH".equalsIgnoreCase(criteria.getSortBy())) {

                    order = "price_min.asc,created_at.desc";

                } else if (criteria != null && "TERMAHAL".equalsIgnoreCase(criteria.getSortBy())) {

                    order = "price_min.desc,created_at.desc";

                }



                Response<List<KostDto>> res = dbService.getKosts(filters, "*", order).execute();

                if (res.isSuccessful() && res.body() != null) {

                    List<Kost> list = mapDtoListToKost(res.body());



                    if (criteria != null && criteria.getFasilitasIds() != null && !criteria.getFasilitasIds().isEmpty()) {

                        List<Kost> filtered = new ArrayList<>();

                        for (Kost k : list) {

                            if (matchFasilitas(k, criteria.getFasilitasIds())) {

                                filtered.add(k);

                            }

                        }

                        list = filtered;

                    }



                    list = applyDistance(list, criteria);

                    populateFavorites(list, currentUserId);

                    final List<Kost> resultList = list;

                    mainHandler.post(() -> callback.onSuccess(resultList));

                } else {

                    postError(callback, ErrorMessages.fromResponse("filterKost", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("filterKost", e));

            }

        });

    }



    public void filterKost(KostFilterCriteria criteria, int legacyUserId, DataCallback<List<Kost>> callback) {

        filterKost(criteria, sessionManager.getUserUid(), callback);

    }



    /** Filter radius & urutan "Terdekat" dihitung di perangkat dari lokasi yang dipilih pengguna. */

    private List<Kost> applyDistance(List<Kost> list, KostFilterCriteria criteria) {

        if (criteria == null) return list;

        boolean byDistance = "TERDEKAT".equalsIgnoreCase(criteria.getSortBy());

        Double radius = criteria.getMaxDistanceKm();

        if (!byDistance && (radius == null || radius <= 0)) return list;

        double lat = criteria.getOriginLat() != null ? criteria.getOriginLat() : sessionManager.getUserSelectedLat();

        double lng = criteria.getOriginLng() != null ? criteria.getOriginLng() : sessionManager.getUserSelectedLng();

        if (lat == 0 || lng == 0) return list;

        List<Kost> result = new ArrayList<>();

        for (Kost k : list) {

            if (radius != null && radius > 0) {

                if (!k.hasCoordinates()) continue;

                if (com.carikostkita.util.GeoUtil.distanceKm(lat, lng, k.getLatitude(), k.getLongitude()) > radius) continue;

            }

            result.add(k);

        }

        if (byDistance) {

            java.util.Collections.sort(result, (a, b) -> Double.compare(

                    a.hasCoordinates() ? com.carikostkita.util.GeoUtil.distanceKm(lat, lng, a.getLatitude(), a.getLongitude()) : Double.MAX_VALUE,

                    b.hasCoordinates() ? com.carikostkita.util.GeoUtil.distanceKm(lat, lng, b.getLatitude(), b.getLongitude()) : Double.MAX_VALUE));

        }

        return result;

    }



    /** Cocokkan berdasarkan nama fasilitas master yang tersimpan di kolom kosts.fasilitas. */

    private boolean matchFasilitas(Kost kost, List<Integer> reqIds) {

        Set<String> existing = new HashSet<>();

        for (String name : kost.getFasilitas()) {

            if (name != null) existing.add(name.trim().toLowerCase());

        }

        for (Integer req : reqIds) {

            String name = Fasilitas.nameForId(req);

            if (name == null || !existing.contains(name.toLowerCase())) return false;

        }

        return true;

    }



    public void getKostDetail(String idKost, String currentUserId, DataCallback<Kost> callback) {

        executor.execute(() -> {

            try {

                try {

                    Response<com.carikostkita.data.remote.dto.KostDetailResponse> apiRes =

                            kostService.readPublicKost(idKost).execute();

                    if (apiRes.isSuccessful() && apiRes.body() != null && apiRes.body().data != null) {

                        Kost kost = mapDtoToKost(apiRes.body().data);

                        if (kost.getHarga() > 0) {

                            if (currentUserId != null && currentUserId.equals(sessionManager.getUserUid())) {

                                try {

                                    String token = activeAccessToken();

                                    if (token != null) {

                                        Response<com.carikostkita.data.remote.dto.FavoriteResponse> favRes =

                                                favoriteService.readFavorite("Bearer " + token, idKost).execute();

                                        kost.setFavorite(favRes.isSuccessful() && favRes.body() != null

                                                && favRes.body().data != null);

                                    }

                                } catch (Exception ignored) {}

                            }

                            mainHandler.post(() -> callback.onSuccess(kost));

                            return;

                        }

                        Log.w(TAG, "FastAPI readPublicKost returned zero-price item, falling back to PostgREST");

                    }

                } catch (Exception apiEx) {

                    Log.w(TAG, "FastAPI readPublicKost fallback to PostgREST: " + apiEx.getMessage());

                }



                Response<List<KostDto>> res = dbService.getKostById("eq." + idKost, "*").execute();

                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {

                    Kost kost = mapDtoToKost(res.body().get(0));

                    if (currentUserId != null && currentUserId.equals(sessionManager.getUserUid())) {

                        try {

                            String token = activeAccessToken();

                            if (token != null) {

                                Response<com.carikostkita.data.remote.dto.FavoriteResponse> favRes =

                                        favoriteService.readFavorite("Bearer " + token, idKost).execute();

                                kost.setFavorite(favRes.isSuccessful() && favRes.body() != null

                                        && favRes.body().data != null);

                            }

                        } catch (Exception ignored) {}

                    }

                    mainHandler.post(() -> callback.onSuccess(kost));

                } else if (res.isSuccessful()) {

                    postError(callback, "Kost ini sudah tidak tersedia atau telah dihapus pemiliknya.");

                } else {

                    postError(callback, ErrorMessages.fromResponse("getKostDetail", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("getKostDetail", e));

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

                    postError(callback, "Masuk terlebih dahulu untuk menyimpan favorit.");

                    return;

                }

                if (!userId.equals(sessionManager.getUserUid())) {

                    postError(callback, ErrorMessages.FORBIDDEN);

                    return;

                }

                String token = activeAccessToken();

                if (token == null) {

                    postError(callback, ErrorMessages.SESSION_EXPIRED);

                    return;

                }

                String bearer = "Bearer " + token;

                Response<com.carikostkita.data.remote.dto.FavoriteResponse> checkRes =

                        favoriteService.readFavorite(bearer, idKost).execute();

                if (!checkRes.isSuccessful() && checkRes.code() != 404) {

                    postError(callback, ErrorMessages.fromResponse("checkFavorite", checkRes));

                    return;

                }

                boolean exists = checkRes.isSuccessful() && checkRes.body() != null

                        && checkRes.body().data != null;

                if (exists) {

                    Response<ResponseBody> delRes = favoriteService.removeFavorite(bearer, idKost).execute();

                    if (delRes.isSuccessful()) {

                        mainHandler.post(() -> callback.onSuccess(false));

                    } else {

                        postError(callback, ErrorMessages.fromResponse("removeFavorite", delRes));

                    }

                } else {

                    Response<com.carikostkita.data.remote.dto.FavoriteResponse> addRes =

                            favoriteService.addFavorite(bearer, idKost).execute();

                    if (addRes.isSuccessful() && addRes.body() != null && addRes.body().data != null) {

                        mainHandler.post(() -> callback.onSuccess(true));

                    } else {

                        postError(callback, ErrorMessages.fromResponse("addFavorite", addRes));

                    }

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("toggleFavorite", e));

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

                if (!userId.equals(sessionManager.getUserUid())) {

                    postError(callback, ErrorMessages.FORBIDDEN);

                    return;

                }

                String token = activeAccessToken();

                if (token == null) {

                    postError(callback, ErrorMessages.SESSION_EXPIRED);

                    return;

                }

                Response<FavoriteListResponse> favRes = favoriteService

                        .listFavorites("Bearer " + token).execute();

                if (!favRes.isSuccessful()) {

                    postError(callback, ErrorMessages.fromResponse("getFavorites", favRes));

                    return;

                }

                if (favRes.body() == null || favRes.body().data == null || favRes.body().data.isEmpty()) {

                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));

                    return;

                }



                List<String> ids = new ArrayList<>();

                for (FavoriteDto f : favRes.body().data) {

                    if (f.kostId != null) ids.add(f.kostId);

                }

                Map<String, String> kostFilter = new HashMap<>();

                kostFilter.put("id", "in.(" + android.text.TextUtils.join(",", ids) + ")");

                Response<List<KostDto>> kostRes = dbService.getKosts(kostFilter, "*", "created_at.desc").execute();

                if (kostRes.isSuccessful() && kostRes.body() != null) {

                    List<Kost> list = mapDtoListToKost(kostRes.body());

                    for (Kost k : list) {

                        k.setFavorite(true);

                        // Kost yang sudah tidak lolos moderasi tetap tampil, tapi ditandai nonaktif

                        if (k.getVerificationStatus() != KostVerificationStatus.APPROVED) {

                            k.setStatus(StatusKost.TIDAK_AKTIF);

                        }

                    }

                    mainHandler.post(() -> callback.onSuccess(list));

                } else {

                    postError(callback, ErrorMessages.fromResponse("getFavoriteKosts", kostRes));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("getFavorites", e));

            }

        });

    }



    public void getFavorites(int legacyUserId, DataCallback<List<Kost>> callback) {

        getFavorites(sessionManager.getUserUid(), callback);

    }



    private void populateFavorites(List<Kost> list, String currentUserId) {

        if (currentUserId == null || !currentUserId.equals(sessionManager.getUserUid()) || list.isEmpty()) return;

        try {

            String token = activeAccessToken();

            if (token == null) return;

            Response<FavoriteListResponse> favRes = favoriteService

                    .listFavorites("Bearer " + token).execute();

            if (favRes.isSuccessful() && favRes.body() != null && favRes.body().data != null) {

                Set<String> favSet = new HashSet<>();

                for (FavoriteDto f : favRes.body().data) {

                    if (f.kostId != null) favSet.add(f.kostId);

                }

                for (Kost k : list) {

                    k.setFavorite(favSet.contains(k.getId()));

                }

            }

        } catch (Exception ignored) {}

    }



    private String activeAccessToken() {

        String token = sessionManager.getAccessToken();

        if (token == null || token.isEmpty()) return null;

        if (sessionManager.isTokenExpired()) {

            return SupabaseClient.getInstance().refreshTokenBlocking(token);

        }

        return token;

    }



    // =========================================================================

    // PEMILIK & ADMIN OPERATIONS

    // =========================================================================



    public void getAllKostForAdmin(DataCallback<List<Kost>> callback) {

        executor.execute(() -> {

            try {

                Response<List<KostDto>> res = dbService.getKosts(new HashMap<>(), "*", "created_at.desc").execute();

                if (res.isSuccessful() && res.body() != null) {

                    List<Kost> list = mapDtoListToKost(res.body());

                    mainHandler.post(() -> callback.onSuccess(list));

                } else {

                    postError(callback, ErrorMessages.fromResponse("getAllKostForAdmin", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("getAllKostForAdmin", e));

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

                    List<Kost> list = mapDtoListToKost(res.body());

                    mainHandler.post(() -> callback.onSuccess(list));

                } else {

                    postError(callback, ErrorMessages.fromResponse("getKostByPemilik", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("getKostByPemilik", e));

            }

        });

    }



    public void getKostByPemilik(int legacyId, DataCallback<List<Kost>> callback) {

        getKostByPemilik(sessionManager.getUserUid(), callback);

    }



    public void saveKostWithFotos(Kost kost, List<Integer> fasilitasIds, List<FotoKost> fotos, DataCallback<SaveResult> callback) {

        executor.execute(() -> {

            try {

                if (kost.getOwnerId() == null || kost.getOwnerId().isEmpty()) {

                    kost.setOwnerId(sessionManager.getUserUid());

                }



                UploadResult upload = uploadFotos(fotos);

                if (fotos != null && !fotos.isEmpty() && upload.urls.isEmpty()) {

                    postError(callback, "Semua foto gagal diunggah. Periksa koneksi lalu coba lagi.");

                    return;

                }

                applyUploadedFotos(kost, fotos, upload);

                applyFasilitas(kost, fasilitasIds);



                KostDto dto = mapKostToDto(kost);

                Response<List<KostDto>> res = dbService.insertKost(dto).execute();

                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {

                    KostDto created = res.body().get(0);

                    boolean review = !KostVerificationStatus.APPROVED.name().equalsIgnoreCase(created.verificationStatus);

                    SaveResult result = new SaveResult(created.id, upload.failed, review);

                    mainHandler.post(() -> callback.onSuccess(result));

                } else {

                    postError(callback, ErrorMessages.fromResponse("insertKost", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("saveKost", e));

            }

        });

    }



    public void updateKostWithFotos(Kost kost, List<Integer> fasilitasIds, List<FotoKost> fotos, DataCallback<SaveResult> callback) {

        executor.execute(() -> {

            try {

                UploadResult upload = uploadFotos(fotos);

                if (fotos != null && !fotos.isEmpty() && upload.urls.isEmpty()) {

                    postError(callback, "Semua foto gagal diunggah. Periksa koneksi lalu coba lagi.");

                    return;

                }

                applyUploadedFotos(kost, fotos, upload);

                applyFasilitas(kost, fasilitasIds);



                KostUpdateDto dto = mapKostToUpdateDto(kost);

                Response<List<KostDto>> res = dbService.updateKost("eq." + kost.getId(), dto).execute();

                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {

                    KostDto updated = res.body().get(0);

                    boolean review = !KostVerificationStatus.APPROVED.name().equalsIgnoreCase(updated.verificationStatus);

                    SaveResult result = new SaveResult(kost.getId(), upload.failed, review);

                    mainHandler.post(() -> callback.onSuccess(result));

                } else if (res.isSuccessful()) {

                    postError(callback, ErrorMessages.FORBIDDEN);

                } else {

                    postError(callback, ErrorMessages.fromResponse("updateKost", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("updateKost", e));

            }

        });

    }



    private void applyFasilitas(Kost kost, List<Integer> fasilitasIds) {

        if (fasilitasIds == null) return;

        List<String> names = new ArrayList<>();

        for (int id : fasilitasIds) {

            String name = Fasilitas.nameForId(id);

            if (name != null) names.add(name);

        }

        kost.setFasilitas(names);

    }



    private void applyUploadedFotos(Kost kost, List<FotoKost> fotos, UploadResult upload) {

        if (upload.urls.isEmpty()) return;

        kost.setImageUrls(new ArrayList<>(upload.urls));

        String thumb = upload.urls.get(0);

        if (fotos != null) {

            for (int i = 0; i < fotos.size(); i++) {

                String url = upload.urlByIndex.get(i);

                if (url != null && fotos.get(i).isThumbnail()) {

                    thumb = url;

                    break;

                }

            }

        }

        kost.setThumbnailUrl(thumb);

    }



    private static class UploadResult {

        final List<String> urls = new ArrayList<>();

        final Map<Integer, String> urlByIndex = new HashMap<>();

        int failed = 0;

    }



    /**

     * Unggah foto baru (URI lokal) dan pertahankan foto lama (URL http).

     * Foto dikecilkan ke sisi terpanjang {@link #MAX_PHOTO_EDGE_PX} dan disimpan di

     * folder milik user agar sesuai kebijakan storage per pengguna.

     */

    private UploadResult uploadFotos(List<FotoKost> fotos) {

        UploadResult result = new UploadResult();

        if (fotos == null || fotos.isEmpty()) return result;



        String folder = sessionManager.getUserUid();

        if (folder == null || folder.isEmpty()) folder = "anon";



        for (int i = 0; i < fotos.size(); i++) {

            String path = fotos.get(i).getPathFile();

            if (path == null || path.trim().isEmpty()) continue;



            if (path.startsWith("http://") || path.startsWith("https://")) {

                result.urls.add(path);

                result.urlByIndex.put(i, path);

                continue;

            }



            try {

                byte[] bytes = readCompressedJpeg(path);

                if (bytes == null) {

                    result.failed++;

                    continue;

                }

                String remoteName = folder + "/" + UUID.randomUUID() + ".jpg";

                RequestBody body = RequestBody.create(bytes, MediaType.parse("image/jpeg"));

                Response<StorageUploadResponse> uploadRes = storageService.uploadFileBinary(BUCKET, remoteName, "image/jpeg", body).execute();

                if (uploadRes.isSuccessful()) {

                    String url = SupabaseClient.getStoragePublicUrl(BUCKET, remoteName);

                    result.urls.add(url);

                    result.urlByIndex.put(i, url);

                } else {

                    ErrorMessages.fromResponse("uploadFoto", uploadRes);

                    result.failed++;

                }

            } catch (Exception e) {

                ErrorMessages.fromException("uploadFoto", e);

                result.failed++;

            }

        }

        return result;

    }



    /** Baca foto dari URI/file, perbaiki rotasi EXIF, perkecil, lalu kompres ke JPEG. */

    private byte[] readCompressedJpeg(String path) {

        if (appContext == null) return null;

        try {

            BitmapFactory.Options bounds = new BitmapFactory.Options();

            bounds.inJustDecodeBounds = true;

            try (InputStream is = openStream(path)) {

                if (is == null) return null;

                BitmapFactory.decodeStream(is, null, bounds);

            }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;



            int sample = 1;

            int longest = Math.max(bounds.outWidth, bounds.outHeight);

            while (longest / (sample * 2) >= MAX_PHOTO_EDGE_PX) sample *= 2;



            BitmapFactory.Options opts = new BitmapFactory.Options();

            opts.inSampleSize = sample;

            Bitmap bitmap;

            try (InputStream is = openStream(path)) {

                bitmap = BitmapFactory.decodeStream(is, null, opts);

            }

            if (bitmap == null) return null;



            int rotation = 0;

            try (InputStream is = openStream(path)) {

                if (is != null) {

                    int orientation = new ExifInterface(is).getAttributeInt(

                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;

                    else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;

                    else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;

                }

            } catch (Exception ignored) {}



            float scale = Math.min(1f, (float) MAX_PHOTO_EDGE_PX / Math.max(bitmap.getWidth(), bitmap.getHeight()));

            if (scale < 1f || rotation != 0) {

                Matrix m = new Matrix();

                m.postScale(scale, scale);

                m.postRotate(rotation);

                Bitmap transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

                if (transformed != bitmap) bitmap.recycle();

                bitmap = transformed;

            }



            ByteArrayOutputStream out = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_JPEG_QUALITY, out);

            bitmap.recycle();

            return out.toByteArray();

        } catch (OutOfMemoryError | Exception e) {

            Log.e(TAG, "Gagal memproses foto: " + path, e);

            return null;

        }

    }



    private InputStream openStream(String path) throws Exception {

        if (path.startsWith("content://")) {

            return appContext.getContentResolver().openInputStream(Uri.parse(path));

        }

        String filePath = path.startsWith("file://") ? Uri.parse(path).getPath() : path;

        File file = new File(filePath);

        return file.exists() ? new FileInputStream(file) : null;

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

                    postError(callback, ErrorMessages.fromResponse("updateStatus", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("updateStatus", e));

            }

        });

    }



    /** Update only this owner's listing availability; other roles keep their separate admin path. */

    public void updateOwnerStatus(String idKost, StatusKost status, DataCallback<Boolean> callback) {

        executor.execute(() -> {

            try {

                if (status != StatusKost.TERSEDIA && status != StatusKost.PENUH) {

                    postError(callback, "Status ketersediaan tidak valid.");

                    return;

                }

                String token = sessionManager.getAccessToken();

                if (token == null || token.isEmpty()) {

                    postError(callback, ErrorMessages.SESSION_EXPIRED);

                    return;

                }

                JsonObject body = new JsonObject();

                body.addProperty("status", status.name());

                Response<JsonObject> api = ownerKostService

                        .updateMyKostStatus("Bearer " + token, idKost, body).execute();

                if (api.isSuccessful()) {

                    mainHandler.post(() -> callback.onSuccess(true));

                    return;

                }

                if (api.code() != 404) {

                    postError(callback, ErrorMessages.fromResponse("updateOwnerStatus", api));

                    return;

                }



                // Compatibility only for an older FastAPI deployment without the owner route.

                KostUpdateDto dto = new KostUpdateDto();

                dto.status = status.name();

                Response<List<KostDto>> legacy = dbService.updateKost("eq." + idKost, dto).execute();

                if (legacy.isSuccessful()) {

                    mainHandler.post(() -> callback.onSuccess(true));

                } else {

                    postError(callback, ErrorMessages.fromResponse("updateOwnerStatus", legacy));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("updateOwnerStatus", e));

            }

        });

    }



    /** Update a listing's availability for the authenticated admin/developer only. */

    public void updateAdminStatus(String idKost, StatusKost status, DataCallback<Boolean> callback) {

        executor.execute(() -> {

            try {

                if (status != StatusKost.TERSEDIA && status != StatusKost.PENUH) {

                    postError(callback, "Status ketersediaan tidak valid.");

                    return;

                }

                String token = sessionManager.getAccessToken();

                if (token == null || token.isEmpty()) {

                    postError(callback, ErrorMessages.SESSION_EXPIRED);

                    return;

                }

                JsonObject body = new JsonObject();

                body.addProperty("status", status.name());

                Response<JsonObject> api = adminKostService

                        .updateKostStatus("Bearer " + token, idKost, body).execute();

                if (api.isSuccessful()) {

                    mainHandler.post(() -> callback.onSuccess(true));

                    return;

                }

                if (api.code() != 404) {

                    postError(callback, ErrorMessages.fromResponse("updateAdminStatus", api));

                    return;

                }



                // Compatibility for an API deployment that predates this route.

                KostUpdateDto dto = new KostUpdateDto();

                dto.status = status.name();

                Response<List<KostDto>> legacy = dbService.updateKost("eq." + idKost, dto).execute();

                if (legacy.isSuccessful()) {

                    mainHandler.post(() -> callback.onSuccess(true));

                } else {

                    postError(callback, ErrorMessages.fromResponse("updateAdminStatus", legacy));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("updateAdminStatus", e));

            }

        });

    }



    /**

     * Ubah jumlah kamar kosong (dan status Tersedia/Penuh) tanpa memicu review ulang:

     * trigger server hanya me-review ulang perubahan isi listing.

     */

    public void updateRoomAvailability(String idKost, int kamarTersedia, DataCallback<Boolean> callback) {

        executor.execute(() -> {

            try {

                KostUpdateDto dto = new KostUpdateDto();

                dto.kamarTersedia = Math.max(0, kamarTersedia);

                dto.status = kamarTersedia > 0 ? StatusKost.TERSEDIA.name() : StatusKost.PENUH.name();

                Response<List<KostDto>> res = dbService.updateKost("eq." + idKost, dto).execute();

                if (res.isSuccessful()) {

                    mainHandler.post(() -> callback.onSuccess(true));

                } else {

                    postError(callback, ErrorMessages.fromResponse("updateRoomAvailability", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("updateRoomAvailability", e));

            }

        });

    }



    /** Ajukan ulang kost REJECTED/REVISION_REQUIRED melalui API pemilik. */

    public void resubmit(String idKost, DataCallback<Boolean> callback) {

        executor.execute(() -> {

            try {

                String token = sessionManager.getAccessToken();

                if (token == null || token.isEmpty()) {

                    postError(callback, ErrorMessages.SESSION_EXPIRED);

                    return;

                }



                Response<JsonObject> api = ownerKostService

                        .resubmitMyKost("Bearer " + token, idKost).execute();

                if (api.isSuccessful()) {

                    mainHandler.post(() -> callback.onSuccess(true));

                    return;

                }

                if (api.code() != 404) {

                    postError(callback, ErrorMessages.fromResponse("resubmit", api));

                    return;

                }



                // Compatibility for API deployments that predate this route.

                Map<String, Object> p = new HashMap<>();

                p.put("target_id", idKost);

                Response<ResponseBody> res = dbService.rpc("resubmit_kost", p).execute();

                if (res.isSuccessful()) {

                    mainHandler.post(() -> callback.onSuccess(true));

                } else {

                    postError(callback, ErrorMessages.fromResponse("resubmit", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("resubmit", e));

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

                    List<Kost> list = mapDtoListToKost(res.body());

                    mainHandler.post(() -> callback.onSuccess(list));

                } else {

                    postError(callback, ErrorMessages.fromResponse("getPendingKosts", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("getPendingKosts", e));

            }

        });

    }



    public void updateKostVerification(String idKost, KostVerificationStatus status, String catatanRevisi,

                                      String adminId, String adminName, DataCallback<Boolean> callback) {

        executor.execute(() -> {

            try {

                KostUpdateDto dto = new KostUpdateDto();

                dto.verificationStatus = status.name();

                dto.catatanRevisi = catatanRevisi != null ? catatanRevisi : "";



                Response<List<KostDto>> res = dbService.updateKost("eq." + idKost, dto).execute();

                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {

                    activityLogRepository.logActivity(

                            sessionManager.getUserUid(), adminName != null ? adminName : "Developer",

                            "VERIFIKASI_KOST",

                            "Kost \"" + res.body().get(0).namaKost + "\" diubah menjadi " + status.getDisplayName()

                                    + (catatanRevisi != null && !catatanRevisi.isEmpty() ? " (" + catatanRevisi + ")" : ""),

                            "KOST", idKost);



                    mainHandler.post(() -> callback.onSuccess(true));

                } else if (res.isSuccessful()) {

                    postError(callback, ErrorMessages.FORBIDDEN);

                } else {

                    postError(callback, ErrorMessages.fromResponse("updateKostVerification", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("updateKostVerification", e));

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

                    int approved = 0;

                    for (KostDto k : res.body()) {

                        if ("TERSEDIA".equalsIgnoreCase(k.status)) tersedia++;

                        if ("PENUH".equalsIgnoreCase(k.status)) penuh++;

                        if ("PENDING".equalsIgnoreCase(k.verificationStatus) || "REVISION_REQUIRED".equalsIgnoreCase(k.verificationStatus)) pending++;

                        if ("APPROVED".equalsIgnoreCase(k.verificationStatus)) approved++;

                    }

                    AdminStats stats = new AdminStats(total, tersedia, penuh, pending, Fasilitas.getMaster().size());

                    stats.kostList.addAll(res.body());

                    stats.kostAktif = tersedia;

                    stats.kostTerverifikasi = approved;

                    mainHandler.post(() -> callback.onSuccess(stats));

                } else {

                    postError(callback, ErrorMessages.fromResponse("getAdminStats", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("getAdminStats", e));

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

                    int tanpaData = 0;

                    for (KostDto k : res.body()) {

                        if ("APPROVED".equalsIgnoreCase(k.verificationStatus)) {

                            if (!"TIDAK_AKTIF".equalsIgnoreCase(k.status)) aktif++;

                        } else if ("PENDING".equalsIgnoreCase(k.verificationStatus)) {

                            pending++;

                        } else if ("REVISION_REQUIRED".equalsIgnoreCase(k.verificationStatus)) {

                            revisi++;

                        }

                        if (k.totalKamar != null && k.kamarTersedia != null && k.totalKamar > 0) {

                            tersedia += k.kamarTersedia;

                            terisi += Math.max(0, k.totalKamar - k.kamarTersedia);

                        } else {

                            tanpaData++;

                        }

                    }

                    PemilikStats stats = new PemilikStats(total, aktif, pending, revisi, tersedia, terisi);

                    stats.kostTanpaDataKamar = tanpaData;

                    stats.kostList.addAll(res.body());

                    stats.totalFavorit = fetchOwnerFavoriteCount();



                    mainHandler.post(() -> callback.onSuccess(stats));

                } else {

                    postError(callback, ErrorMessages.fromResponse("getPemilikStats", res));

                }

            } catch (Exception e) {

                postError(callback, ErrorMessages.fromException("getPemilikStats", e));

            }

        });

    }



    /**

     * Jumlah pencari yang memfavoritkan kost milik pemilik ini. Tabel favorites hanya bisa

     * dibaca pemiliknya sendiri, jadi angka ini diambil lewat fungsi SQL owner_favorite_count.

     */

    private int fetchOwnerFavoriteCount() {

        try {

            String token = sessionManager.getAccessToken();

            if (token == null || token.isEmpty()) return 0;



            Response<JsonObject> api = ownerStatsService.getMyFavoriteCount("Bearer " + token).execute();

            if (api.isSuccessful() && api.body() != null

                    && api.body().has("data") && api.body().getAsJsonObject("data").has("favorite_count")) {

                return Math.max(0, api.body().getAsJsonObject("data").get("favorite_count").getAsInt());

            }

            if (api.code() != 404) {

                Log.w(TAG, "FastAPI gagal membaca statistik favorit (HTTP " + api.code() + ")");

                return 0;

            }



            // Older API deployments may not have this route yet; the migration

            // also repairs the legacy RPC to use the bigint identity bridge.

            Response<ResponseBody> legacy = dbService.rpc("owner_favorite_count", new HashMap<>()).execute();

            if (legacy.isSuccessful() && legacy.body() != null) {

                String raw = legacy.body().string().trim();

                return Integer.parseInt(raw.replaceAll("[^0-9]", "").isEmpty() ? "0" : raw.replaceAll("[^0-9]", ""));

            }

        } catch (Exception e) {

            Log.w(TAG, "owner_favorite_count belum tersedia: " + e.getMessage());

        }

        return 0;

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

        return Fasilitas.getMaster();

    }



    public static List<Wilayah> getMasterWilayah() {

        List<Wilayah> list = new ArrayList<>();

        list.add(new Wilayah(1, "Bukit Raya", "Simpang Tiga", "Pekanbaru"));

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

        k.setPriceMin(dto.harga != null ? dto.harga : 0);

        k.setPriceMax(dto.priceMax != null && dto.priceMax > 0 ? dto.priceMax : (dto.harga != null ? dto.harga : 0));

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

        // Jangan mengarang jumlah kamar: null berarti pemilik belum mengisi

        k.setTotalKamar(dto.totalKamar != null ? dto.totalKamar : 0);

        k.setKamarTersedia(dto.kamarTersedia != null ? dto.kamarTersedia : Kost.ROOMS_UNKNOWN);

        // Normalisasi foto: dukung URL web, Base64 Data URI (Excel import), dan path relatif server
        String validThumb = com.carikostkita.util.ImageLoaderUtil.normalizeUrl(dto.thumbnailUrl);
        List<String> validImages = new ArrayList<>();
        if (dto.imageUrls != null) {
            for (String img : dto.imageUrls) {
                String norm = com.carikostkita.util.ImageLoaderUtil.normalizeUrl(img);
                if (norm != null && !norm.isEmpty()) {
                    validImages.add(norm);
                }
            }
        }
        if (validThumb == null && !validImages.isEmpty()) {
            validThumb = validImages.get(0);
        }
        k.setThumbnailUrl(validThumb);
        k.setImageUrls(validImages);

        k.setFasilitas(dto.fasilitas);

        k.setDeposit(dto.deposit);

        k.setMinimalSewaBulan(dto.minimalSewaBulan);

        k.setBiayaTambahan(dto.biayaTambahan);

        k.setAturan(dto.aturan);

        k.setRatingAvg(dto.ratingAvg != null ? dto.ratingAvg : 0);

        k.setRatingCount(dto.ratingCount != null ? dto.ratingCount : 0);

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

        dto.priceMax = kost.getPriceMax();

        dto.tipeKost = kost.getTipeKost() != null ? kost.getTipeKost().name() : "CAMPUR";

        dto.deskripsi = kost.getDeskripsi();

        dto.noWhatsapp = kost.getNoWhatsapp();

        dto.latitude = kost.getLatitude();

        dto.longitude = kost.getLongitude();

        dto.status = kost.getStatus() != null ? kost.getStatus().name() : "TERSEDIA";

        // Status verifikasi final ditentukan trigger di server; nilai ini hanya permintaan

        dto.verificationStatus = kost.getVerificationStatus() != null ? kost.getVerificationStatus().name() : "PENDING";

        dto.catatanRevisi = kost.getCatatanRevisi();

        dto.provinsi = kost.getProvinsi();

        dto.kota = kost.getKota();

        dto.kecamatan = kost.getKecamatan();

        dto.kelurahan = kost.getKelurahan();

        dto.ukuranKamar = kost.getUkuranKamar();

        dto.totalKamar = kost.getTotalKamar();

        dto.kamarTersedia = kost.hasRoomInfo() ? kost.getKamarTersedia() : null;

        dto.thumbnailUrl = kost.getThumbnailUrl();

        dto.imageUrls = kost.getImageUrls();

        dto.fasilitas = kost.getFasilitas();

        dto.deposit = kost.getDeposit();

        dto.minimalSewaBulan = kost.getMinimalSewaBulan();

        dto.biayaTambahan = kost.getBiayaTambahan();

        dto.aturan = kost.getAturan().isEmpty() ? null : kost.getAturan();

        return dto;

    }



    private KostUpdateDto mapKostToUpdateDto(Kost kost) {

        KostUpdateDto dto = new KostUpdateDto();

        dto.namaKost = kost.getNamaKost();

        dto.alamat = kost.getAlamat();

        dto.patokan = kost.getPatokan();

        dto.harga = kost.getHarga();

        dto.priceMax = kost.getPriceMax();

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

        dto.kamarTersedia = kost.hasRoomInfo() ? kost.getKamarTersedia() : null;

        dto.thumbnailUrl = kost.getThumbnailUrl();

        dto.imageUrls = kost.getImageUrls();

        dto.fasilitas = kost.getFasilitas();

        dto.deposit = kost.getDeposit();

        dto.minimalSewaBulan = kost.getMinimalSewaBulan();

        dto.biayaTambahan = kost.getBiayaTambahan();

        dto.aturan = kost.getAturan(); // daftar kosong = hapus semua aturan

        return dto;

    }



    private <T> void postError(DataCallback<T> callback, String msg) {

        mainHandler.post(() -> callback.onError(msg));

    }

}

