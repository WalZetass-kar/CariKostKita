package com.carikostkita.data.location;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Service terpusat untuk mengambil data wilayah administratif Republik Indonesia secara dinamis.
 * Mengakses data resmi Kemendagri via API publik (provinces, regencies, districts, villages).
 * Dilengkapi dengan in-memory cache berkecepatan tinggi agar tidak membebani kuota atau jaringan.
 */
public class WilayahIndonesiaService {

    private static final String TAG = "WilayahService";
    private static final String BASE_URL = "https://www.emsifa.com/api-wilayah-indonesia/api/";

    private static volatile WilayahIndonesiaService instance;
    private final WilayahApiService api;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // In-memory cache
    private final Map<String, List<WilayahModel>> regenciesCache = new ConcurrentHashMap<>();
    private final Map<String, List<WilayahModel>> districtsCache = new ConcurrentHashMap<>();
    private final Map<String, List<WilayahModel>> villagesCache = new ConcurrentHashMap<>();

    // Pemetaan nama ke ID untuk resolusi cepat tanpa query berulang
    private static final Map<String, String> PROVINSI_ID_MAP = new HashMap<>();
    private final Map<String, String> regencyNameToIdMap = new ConcurrentHashMap<>();
    private final Map<String, String> districtNameToIdMap = new ConcurrentHashMap<>();

    static {
        // Pemetaan standar 38 Provinsi ke ID resmi
        PROVINSI_ID_MAP.put("aceh", "11");
        PROVINSI_ID_MAP.put("sumatera utara", "12");
        PROVINSI_ID_MAP.put("sumatera barat", "13");
        PROVINSI_ID_MAP.put("riau", "14");
        PROVINSI_ID_MAP.put("jambi", "15");
        PROVINSI_ID_MAP.put("sumatera selatan", "16");
        PROVINSI_ID_MAP.put("bengkulu", "17");
        PROVINSI_ID_MAP.put("lampung", "18");
        PROVINSI_ID_MAP.put("kepulauan bangka belitung", "19");
        PROVINSI_ID_MAP.put("bangka belitung", "19");
        PROVINSI_ID_MAP.put("kepulauan riau", "21");
        PROVINSI_ID_MAP.put("dki jakarta", "31");
        PROVINSI_ID_MAP.put("jakarta", "31");
        PROVINSI_ID_MAP.put("jawa barat", "32");
        PROVINSI_ID_MAP.put("jawa tengah", "33");
        PROVINSI_ID_MAP.put("di yogyakarta", "34");
        PROVINSI_ID_MAP.put("daerah istimewa yogyakarta", "34");
        PROVINSI_ID_MAP.put("yogyakarta", "34");
        PROVINSI_ID_MAP.put("jawa timur", "35");
        PROVINSI_ID_MAP.put("banten", "36");
        PROVINSI_ID_MAP.put("bali", "51");
        PROVINSI_ID_MAP.put("nusa tenggara barat", "52");
        PROVINSI_ID_MAP.put("ntb", "52");
        PROVINSI_ID_MAP.put("nusa tenggara timur", "53");
        PROVINSI_ID_MAP.put("ntt", "53");
        PROVINSI_ID_MAP.put("kalimantan barat", "61");
        PROVINSI_ID_MAP.put("kalimantan tengah", "62");
        PROVINSI_ID_MAP.put("kalimantan selatan", "63");
        PROVINSI_ID_MAP.put("kalimantan timur", "64");
        PROVINSI_ID_MAP.put("kalimantan utara", "65");
        PROVINSI_ID_MAP.put("sulawesi utara", "71");
        PROVINSI_ID_MAP.put("sulawesi tengah", "72");
        PROVINSI_ID_MAP.put("sulawesi selatan", "73");
        PROVINSI_ID_MAP.put("sulawesi tenggara", "74");
        PROVINSI_ID_MAP.put("gorontalo", "75");
        PROVINSI_ID_MAP.put("sulawesi barat", "76");
        PROVINSI_ID_MAP.put("maluku", "81");
        PROVINSI_ID_MAP.put("maluku utara", "82");
        PROVINSI_ID_MAP.put("papua barat", "91");
        PROVINSI_ID_MAP.put("papua barat daya", "91");
        PROVINSI_ID_MAP.put("papua", "94");
        PROVINSI_ID_MAP.put("papua tengah", "94");
        PROVINSI_ID_MAP.put("papua pegunungan", "94");
        PROVINSI_ID_MAP.put("papua selatan", "94");
    }

    public interface WilayahCallback<T> {
        void onSuccess(T data);
        void onError(String message, T fallbackData);
    }

    private WilayahIndonesiaService() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(WilayahApiService.class);
    }

    public static WilayahIndonesiaService getInstance() {
        if (instance == null) {
            synchronized (WilayahIndonesiaService.class) {
                if (instance == null) {
                    instance = new WilayahIndonesiaService();
                }
            }
        }
        return instance;
    }

    /**
     * Resolusi ID Provinsi dari teks nama provinsi.
     */
    public String resolveProvinceId(String provinceName) {
        if (provinceName == null) return null;
        String clean = normalize(provinceName);
        String id = PROVINSI_ID_MAP.get(clean);
        if (id != null) return id;

        for (Map.Entry<String, String> entry : PROVINSI_ID_MAP.entrySet()) {
            if (clean.contains(entry.getKey()) || entry.getKey().contains(clean)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Memuat daftar Kecamatan lengkap untuk Kabupaten/Kota tertentu.
     * Menggunakan cache instan jika sudah pernah diakses, atau fetch via API.
     */
    public void getKecamatanList(String kotaOrKabupaten, String provinsi, WilayahCallback<List<String>> callback) {
        if (kotaOrKabupaten == null || kotaOrKabupaten.trim().isEmpty()) {
            callback.onSuccess(Collections.emptyList());
            return;
        }

        String normKota = normalize(kotaOrKabupaten);
        String regencyId = regencyNameToIdMap.get(normKota);

        if (regencyId != null && districtsCache.containsKey(regencyId)) {
            deliverFormattedStrings(districtsCache.get(regencyId), callback);
            return;
        }

        if (regencyId != null) {
            fetchDistrictsByRegencyId(regencyId, kotaOrKabupaten, callback);
            return;
        }

        // Resolusi ID Kabupaten/Kota dari provinsi
        String provId = resolveProvinceId(provinsi);
        if (provId == null) provId = "14"; // Default Riau jika null

        final String finalProvId = provId;
        api.getRegencies(finalProvId).enqueue(new Callback<List<WilayahModel>>() {
            @Override
            public void onResponse(Call<List<WilayahModel>> call, Response<List<WilayahModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WilayahModel> regencies = response.body();
                    regenciesCache.put(finalProvId, regencies);

                    String matchedId = null;
                    for (WilayahModel reg : regencies) {
                        String rName = normalize(reg.getName());
                        String rFormatted = normalize(reg.getFormattedName());
                        regencyNameToIdMap.put(rName, reg.getId());
                        regencyNameToIdMap.put(rFormatted, reg.getId());

                        if (matchedId == null && (rName.contains(normKota) || normKota.contains(rName)
                                || cleanTypePrefix(normKota).equals(cleanTypePrefix(rName)))) {
                            matchedId = reg.getId();
                        }
                    }

                    if (matchedId != null) {
                        fetchDistrictsByRegencyId(matchedId, kotaOrKabupaten, callback);
                    } else {
                        fallbackKecamatan(kotaOrKabupaten, "Kota tidak ditemukan di API", callback);
                    }
                } else {
                    fallbackKecamatan(kotaOrKabupaten, "Gagal memuat kabupaten", callback);
                }
            }

            @Override
            public void onFailure(Call<List<WilayahModel>> call, Throwable t) {
                fallbackKecamatan(kotaOrKabupaten, t.getMessage(), callback);
            }
        });
    }

    private void fetchDistrictsByRegencyId(String regencyId, String kotaOrKabupaten, WilayahCallback<List<String>> callback) {
        api.getDistricts(regencyId).enqueue(new Callback<List<WilayahModel>>() {
            @Override
            public void onResponse(Call<List<WilayahModel>> call, Response<List<WilayahModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WilayahModel> districts = response.body();
                    districtsCache.put(regencyId, districts);

                    for (WilayahModel dist : districts) {
                        districtNameToIdMap.put(normalize(dist.getName()), dist.getId());
                        districtNameToIdMap.put(normalize(dist.getFormattedName()), dist.getId());
                    }

                    deliverFormattedStrings(districts, callback);
                } else {
                    fallbackKecamatan(kotaOrKabupaten, "Gagal memuat kecamatan", callback);
                }
            }

            @Override
            public void onFailure(Call<List<WilayahModel>> call, Throwable t) {
                fallbackKecamatan(kotaOrKabupaten, t.getMessage(), callback);
            }
        });
    }

    private void fallbackKecamatan(String kota, String error, WilayahCallback<List<String>> callback) {
        List<String> localFallback = IndonesiaLocationData.getKecamatanList(kota);
        mainHandler.post(() -> callback.onError(error, localFallback));
    }

    /**
     * Memuat daftar Kelurahan / Desa lengkap untuk Kecamatan tertentu.
     */
    public void getKelurahanList(String kecamatan, String kotaOrKabupaten, String provinsi, WilayahCallback<List<String>> callback) {
        if (kecamatan == null || kecamatan.trim().isEmpty()) {
            callback.onSuccess(Collections.emptyList());
            return;
        }

        String normKec = normalize(kecamatan);
        String districtId = districtNameToIdMap.get(normKec);

        if (districtId != null && villagesCache.containsKey(districtId)) {
            deliverFormattedStrings(villagesCache.get(districtId), callback);
            return;
        }

        if (districtId != null) {
            fetchVillagesByDistrictId(districtId, kecamatan, callback);
            return;
        }

        // Jika ID belum terpetakan, coba refresh kecamatan terlebih dahulu
        getKecamatanList(kotaOrKabupaten, provinsi, new WilayahCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> data) {
                String resolvedId = districtNameToIdMap.get(normKec);
                if (resolvedId != null) {
                    fetchVillagesByDistrictId(resolvedId, kecamatan, callback);
                } else {
                    fallbackKelurahan(kecamatan, "Kecamatan tidak ditemukan", callback);
                }
            }

            @Override
            public void onError(String message, List<String> fallbackData) {
                fallbackKelurahan(kecamatan, message, callback);
            }
        });
    }

    private void fetchVillagesByDistrictId(String districtId, String kecamatan, WilayahCallback<List<String>> callback) {
        api.getVillages(districtId).enqueue(new Callback<List<WilayahModel>>() {
            @Override
            public void onResponse(Call<List<WilayahModel>> call, Response<List<WilayahModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WilayahModel> villages = response.body();
                    villagesCache.put(districtId, villages);
                    deliverFormattedStrings(villages, callback);
                } else {
                    fallbackKelurahan(kecamatan, "Gagal memuat kelurahan", callback);
                }
            }

            @Override
            public void onFailure(Call<List<WilayahModel>> call, Throwable t) {
                fallbackKelurahan(kecamatan, t.getMessage(), callback);
            }
        });
    }

    private void fallbackKelurahan(String kecamatan, String error, WilayahCallback<List<String>> callback) {
        List<String> localFallback = IndonesiaLocationData.getKelurahanList(kecamatan);
        mainHandler.post(() -> callback.onError(error, localFallback));
    }

    private void deliverFormattedStrings(List<WilayahModel> models, WilayahCallback<List<String>> callback) {
        List<String> formatted = new ArrayList<>();
        if (models != null) {
            for (WilayahModel item : models) {
                String fname = item.getFormattedName();
                if (!fname.isEmpty() && !formatted.contains(fname)) {
                    formatted.add(fname);
                }
            }
        }
        mainHandler.post(() -> callback.onSuccess(formatted));
    }

    private static String normalize(String str) {
        if (str == null) return "";
        return str.trim().toLowerCase(Locale.ROOT)
                .replace("kabupaten", "")
                .replace("kab.", "")
                .replace("kota", "")
                .trim();
    }

    private static String cleanTypePrefix(String str) {
        if (str == null) return "";
        return str.replace("kabupaten", "")
                .replace("kota", "")
                .replace("kab", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }
}
