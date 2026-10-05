package com.carikostkita.data.location;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NominatimGeocodingService {

    private static final String BASE_URL = "https://nominatim.openstreetmap.org/";
    private static final String USER_AGENT = "CariKostKita-App/1.0 (Android; contact@carikostkita.id)";
    private static final String LANGUAGE = "id,en;q=0.7";

    public interface ReverseGeocodeCallback {
        void onSuccess(NominatimPlace place, String street, String kelurahan, String kecamatan, String kota, String provinsi, String fullAddress);
        void onError(String message);
    }

    public interface SearchPlacesCallback {
        void onSuccess(List<NominatimPlace> places);
        void onError(String message);
    }

    private static NominatimGeocodingService instance;
    private final NominatimApiService apiService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private NominatimGeocodingService() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(NominatimApiService.class);
    }

    public static synchronized NominatimGeocodingService getInstance() {
        if (instance == null) {
            instance = new NominatimGeocodingService();
        }
        return instance;
    }

    public void reverseGeocode(double lat, double lng, @NonNull ReverseGeocodeCallback callback) {
        apiService.reverseGeocode(USER_AGENT, lat, lng, LANGUAGE).enqueue(new Callback<NominatimPlace>() {
            @Override
            public void onResponse(@NonNull Call<NominatimPlace> call, @NonNull Response<NominatimPlace> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NominatimPlace place = response.body();
                    NominatimAddress addr = place.getAddress();

                    String street = "";
                    String kelurahan = "";
                    String kecamatan = "";
                    String kota = "";
                    String provinsi = "";
                    String fullAddress = place.getDisplayName();

                    if (addr != null) {
                        street = addr.getStreetName();
                        kelurahan = addr.getKelurahan();
                        kecamatan = addr.getKecamatan();
                        kota = addr.getKota();
                        provinsi = addr.getProvinsi();

                        StringBuilder sb = new StringBuilder();
                        if (!street.isEmpty()) sb.append(street).append(", ");
                        if (!kelurahan.isEmpty()) sb.append("Kel. ").append(kelurahan).append(", ");
                        if (!kecamatan.isEmpty()) sb.append("Kec. ").append(kecamatan).append(", ");
                        if (!kota.isEmpty()) sb.append(kota).append(", ");
                        if (!provinsi.isEmpty()) sb.append(provinsi);

                        String constructed = sb.toString().trim();
                        if (constructed.endsWith(",")) {
                            constructed = constructed.substring(0, constructed.length() - 1).trim();
                        }
                        if (!constructed.isEmpty()) {
                            fullAddress = constructed;
                        }
                    }

                    if (street.isEmpty() && place.getName() != null && !place.getName().isEmpty()) {
                        street = place.getName();
                    }

                    final String finalStreet = street;
                    final String finalKel = kelurahan;
                    final String finalKec = kecamatan;
                    final String finalKota = kota;
                    final String finalProv = provinsi;
                    final String finalFull = fullAddress;

                    mainHandler.post(() -> callback.onSuccess(place, finalStreet, finalKel, finalKec, finalKota, finalProv, finalFull));
                } else {
                    mainHandler.post(() -> callback.onError("Gagal menerjemahkan lokasi (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<NominatimPlace> call, @NonNull Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage() != null ? t.getMessage() : "Koneksi gagal"));
            }
        });
    }

    public void searchPlaces(String query, @NonNull SearchPlacesCallback callback) {
        if (query == null || query.trim().length() < 2) {
            callback.onSuccess(Collections.emptyList());
            return;
        }

        apiService.searchPlaces(USER_AGENT, query.trim(), 7, LANGUAGE).enqueue(new Callback<List<NominatimPlace>>() {
            @Override
            public void onResponse(@NonNull Call<List<NominatimPlace>> call, @NonNull Response<List<NominatimPlace>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NominatimPlace> list = response.body();
                    mainHandler.post(() -> callback.onSuccess(list));
                } else {
                    mainHandler.post(() -> callback.onError("Pencarian lokasi gagal (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<NominatimPlace>> call, @NonNull Throwable t) {
                mainHandler.post(() -> callback.onError(t.getMessage() != null ? t.getMessage() : "Koneksi gagal"));
            }
        });
    }
}
