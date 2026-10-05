package com.carikostkita.data.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.carikostkita.util.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import java.util.List;
import java.util.Locale;

/**
 * Manajer deteksi lokasi dinamis untuk CariKostKita.
 * Menggabungkan Google Fused Location API untuk akurasi GPS tinggi (meter-level),
 * serta Nominatim OpenStreetMap API untuk reverse geocoding presisi
 * dengan fallback otomatis ke Geocoder bawaan sistem Android.
 */
public class UserLocationManager {

    public interface LocationCallback {
        void onLocationDetected(String city, String district, double lat, double lng, String display);
        void onLocationFailed(String error);
    }

    private static UserLocationManager instance;
    private final Context appContext;
    private final SessionManager sessionManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final FusedLocationProviderClient fusedLocationClient;

    private UserLocationManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(context);
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.appContext);
    }

    public static synchronized UserLocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserLocationManager(context);
        }
        return instance;
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public String getActiveLocationDisplay() {
        String saved = sessionManager.getUserSelectedLocationDisplay();
        if (saved != null && !saved.trim().isEmpty()) {
            return saved;
        }
        return "Pilih Lokasi";
    }

    public String getActiveCity() {
        return sessionManager.getUserSelectedCity();
    }

    public String getActiveDistrict() {
        return sessionManager.getUserSelectedDistrict();
    }

    public void setManualLocation(String city, String district, double lat, double lng) {
        StringBuilder sb = new StringBuilder();
        if (district != null && !district.trim().isEmpty()) {
            sb.append(district.trim()).append(", ");
        }
        sb.append(city != null ? city.trim() : "Indonesia");
        String display = sb.toString();

        sessionManager.setUserSelectedLocation(city, district, lat, lng, display);
    }

    @SuppressLint("MissingPermission")
    public void detectCurrentLocation(@NonNull LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationFailed("Izin akses lokasi belum diberikan");
            return;
        }

        try {
            CancellationTokenSource cts = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            reverseGeocode(location.getLatitude(), location.getLongitude(), callback);
                        } else {
                            fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                                if (lastLoc != null) {
                                    reverseGeocode(lastLoc.getLatitude(), lastLoc.getLongitude(), callback);
                                } else {
                                    fallbackToStandardLocationManager(callback);
                                }
                            }).addOnFailureListener(e -> fallbackToStandardLocationManager(callback));
                        }
                    })
                    .addOnFailureListener(e -> fallbackToStandardLocationManager(callback));

        } catch (Exception e) {
            fallbackToStandardLocationManager(callback);
        }
    }

    @SuppressLint("MissingPermission")
    private void fallbackToStandardLocationManager(LocationCallback callback) {
        LocationManager locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            callback.onLocationFailed("Layanan lokasi tidak tersedia di perangkat ini");
            return;
        }

        Location bestLocation = null;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                bestLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
        } catch (Exception ignored) {}

        if (bestLocation != null) {
            reverseGeocode(bestLocation.getLatitude(), bestLocation.getLongitude(), callback);
            return;
        }

        final boolean[] received = {false};
        final LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (!received[0]) {
                    received[0] = true;
                    try {
                        locationManager.removeUpdates(this);
                    } catch (Exception ignored) {}
                    reverseGeocode(location.getLatitude(), location.getLongitude(), callback);
                }
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        };

        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper());
            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper());
            } else {
                callback.onLocationFailed("GPS / Layanan lokasi nonaktif di pengaturan HP");
                return;
            }

            mainHandler.postDelayed(() -> {
                if (!received[0]) {
                    received[0] = true;
                    try {
                        locationManager.removeUpdates(listener);
                    } catch (Exception ignored) {}
                    callback.onLocationFailed("Waktu deteksi GPS habis. Silakan pilih lokasi secara manual.");
                }
            }, 5500);

        } catch (Exception e) {
            callback.onLocationFailed("Gagal membaca GPS: " + e.getMessage());
        }
    }

    public void reverseGeocode(double lat, double lng, LocationCallback callback) {
        // 1. Coba Nominatim OpenStreetMap API untuk akurasi alamat & distrik yang presisi
        NominatimGeocodingService.getInstance().reverseGeocode(lat, lng, new NominatimGeocodingService.ReverseGeocodeCallback() {
            @Override
            public void onSuccess(NominatimPlace place, String street, String kelurahan, String kecamatan, String kota, String provinsi, String fullAddress) {
                String finalCity = !kota.isEmpty() ? kota : (!provinsi.isEmpty() ? provinsi : "Indonesia");
                String finalDistrict = !kecamatan.isEmpty() ? kecamatan : kelurahan;

                StringBuilder sb = new StringBuilder();
                if (!finalDistrict.isEmpty()) {
                    sb.append(finalDistrict).append(", ");
                }
                sb.append(finalCity);
                String display = sb.toString();

                sessionManager.setUserSelectedLocation(finalCity, finalDistrict, lat, lng, display);
                if (callback != null) {
                    callback.onLocationDetected(finalCity, finalDistrict, lat, lng, display);
                }
            }

            @Override
            public void onError(String message) {
                // 2. Fallback ke Geocoder bawaan Android jika koneksi API gagal
                reverseGeocodeFallback(lat, lng, callback);
            }
        });
    }

    private void reverseGeocodeFallback(double lat, double lng, LocationCallback callback) {
        new Thread(() -> {
            String city = "";
            String district = "";
            String display = "";

            try {
                if (Geocoder.isPresent()) {
                    Geocoder geocoder = new Geocoder(appContext, new Locale("id", "ID"));
                    List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);

                        district = addr.getSubLocality();
                        if (district == null || district.isEmpty()) {
                            district = addr.getLocality();
                        }

                        city = addr.getSubAdminArea();
                        if (city == null || city.isEmpty()) {
                            city = addr.getAdminArea();
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (city == null || city.isEmpty()) {
                city = "Lokasi Terpilih";
            }
            if (district == null) district = "";

            StringBuilder sb = new StringBuilder();
            if (!district.isEmpty()) {
                sb.append(district).append(", ");
            }
            sb.append(city);
            display = sb.toString();

            final String fCity = city;
            final String fDistrict = district;
            final String fDisplay = display;

            mainHandler.post(() -> {
                sessionManager.setUserSelectedLocation(fCity, fDistrict, lat, lng, fDisplay);
                if (callback != null) {
                    callback.onLocationDetected(fCity, fDistrict, lat, lng, fDisplay);
                }
            });
        }).start();
    }
}
