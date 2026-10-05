package com.carikostkita.ui.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.carikostkita.R;
import com.carikostkita.data.location.NominatimGeocodingService;
import com.carikostkita.data.location.NominatimPlace;
import com.carikostkita.data.location.UserLocationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import java.util.List;
import java.util.Locale;

public class MapPinPickerActivity extends AppCompatActivity {

    public static final String EXTRA_LAT = "latitude";
    public static final String EXTRA_LNG = "longitude";
    public static final String EXTRA_ALAMAT = "alamat";
    public static final String EXTRA_KOTA = "kota";
    public static final String EXTRA_KECAMATAN = "kecamatan";
    public static final String EXTRA_KELURAHAN = "kelurahan";
    public static final String EXTRA_PROVINSI = "provinsi";

    private static final double DEFAULT_LAT = 0.5071;
    private static final double DEFAULT_LNG = 101.4478;

    private MapView mapView;
    private IMapController mapController;
    private ProgressBar pbLoading;
    private TextView tvCoordinates;
    private TextView tvStreetName;
    private TextView tvFullAddress;
    private MaterialButton btnConfirm;
    private FloatingActionButton fabMyLocation;

    // Search Bar Views
    private EditText etSearch;
    private ProgressBar pbSearchLoading;
    private ImageButton btnClearSearch;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingGeocodeRunnable;
    private FusedLocationProviderClient fusedLocationClient;

    // Current State
    private double currentLat = DEFAULT_LAT;
    private double currentLng = DEFAULT_LNG;
    private String currentAlamat = "";
    private String currentKota = "";
    private String currentKecamatan = "";
    private String currentKelurahan = "";
    private String currentProvinsi = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_pin_picker);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupMap();
        setupSearchBar();
        readInitialIntent();
    }

    private void initViews() {
        mapView = findViewById(R.id.map_pin_view);
        pbLoading = findViewById(R.id.pb_pin_loading);
        tvCoordinates = findViewById(R.id.tv_pin_coordinates);
        tvStreetName = findViewById(R.id.tv_pin_street_name);
        tvFullAddress = findViewById(R.id.tv_pin_full_address);
        btnConfirm = findViewById(R.id.btn_pin_confirm);
        fabMyLocation = findViewById(R.id.fab_pin_my_location);

        etSearch = findViewById(R.id.et_pin_search);
        pbSearchLoading = findViewById(R.id.pb_pin_search_loading);
        btnClearSearch = findViewById(R.id.btn_pin_clear_search);

        ImageButton btnBack = findViewById(R.id.btn_pin_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnConfirm.setOnClickListener(v -> confirmLocation());
        fabMyLocation.setOnClickListener(v -> moveToMyLocation());
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        mapController = mapView.getController();
        mapController.setZoom(17.0);

        // Map movement listener with 450ms debounce
        mapView.addMapListener(new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                onMapMoved();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                onMapMoved();
                return true;
            }
        }, 450));
    }

    private void setupSearchBar() {
        if (etSearch == null) return;

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                performPlaceSearch(etSearch.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                etSearch.setText("");
                btnClearSearch.setVisibility(View.GONE);
            });
        }
    }

    private void performPlaceSearch(String query) {
        if (query == null || query.trim().length() < 2) {
            Toast.makeText(this, "Ketik minimal 2 huruf nama jalan atau area", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.VISIBLE);

        NominatimGeocodingService.getInstance().searchPlaces(query.trim(), new NominatimGeocodingService.SearchPlacesCallback() {
            @Override
            public void onSuccess(List<NominatimPlace> places) {
                if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);

                if (places != null && !places.isEmpty()) {
                    NominatimPlace first = places.get(0);
                    currentLat = first.getLatitude();
                    currentLng = first.getLongitude();

                    GeoPoint targetPoint = new GeoPoint(currentLat, currentLng);
                    mapController.animateTo(targetPoint);
                    mapController.setZoom(17.5);

                    updateCoordinatesUI(currentLat, currentLng);
                    scheduleReverseGeocode(currentLat, currentLng);

                    String name = first.getName();
                    if (name == null || name.isEmpty()) name = first.getDisplayName();
                    Toast.makeText(MapPinPickerActivity.this, "Titik ditemukan: " + name, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MapPinPickerActivity.this, "Lokasi tidak ditemukan. Coba nama yang lebih spesifik.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
                Toast.makeText(MapPinPickerActivity.this, "Gagal mencari: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        if (etSearch != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            }
        }
    }

    private void readInitialIntent() {
        double initLat = getIntent().getDoubleExtra(EXTRA_LAT, 0);
        double initLng = getIntent().getDoubleExtra(EXTRA_LNG, 0);

        boolean hasCustomCoord = (initLat != 0 && initLng != 0 && (initLat != DEFAULT_LAT || initLng != DEFAULT_LNG));

        if (hasCustomCoord) {
            currentLat = initLat;
            currentLng = initLng;
            GeoPoint centerPoint = new GeoPoint(currentLat, currentLng);
            mapController.setCenter(centerPoint);
            updateCoordinatesUI(currentLat, currentLng);
            scheduleReverseGeocode(currentLat, currentLng);
        } else {
            // Prioritaskan lokasi GPS nyata lewat Google Fused Location API
            fetchInitialLocationAndCenter();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchInitialLocationAndCenter() {
        UserLocationManager locManager = UserLocationManager.getInstance(this);
        if (locManager.hasLocationPermission()) {
            try {
                CancellationTokenSource cts = new CancellationTokenSource();
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                        .addOnSuccessListener(loc -> {
                            if (loc != null) {
                                currentLat = loc.getLatitude();
                                currentLng = loc.getLongitude();
                            } else {
                                fallbackToLastKnownOrPeanbaru();
                            }
                            applyMapCenterAndGeocode();
                        })
                        .addOnFailureListener(e -> {
                            fallbackToLastKnownOrPeanbaru();
                            applyMapCenterAndGeocode();
                        });
                return;
            } catch (Exception ignored) {}
        }

        fallbackToLastKnownOrPeanbaru();
        applyMapCenterAndGeocode();
    }

    @SuppressLint("MissingPermission")
    private void fallbackToLastKnownOrPeanbaru() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location realLoc = null;
        if (lm != null) {
            try {
                realLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (realLoc == null) realLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } catch (SecurityException ignored) {}
        }

        if (realLoc != null) {
            currentLat = realLoc.getLatitude();
            currentLng = realLoc.getLongitude();
        } else {
            currentLat = DEFAULT_LAT;
            currentLng = DEFAULT_LNG;
        }
    }

    private void applyMapCenterAndGeocode() {
        GeoPoint centerPoint = new GeoPoint(currentLat, currentLng);
        mapController.setCenter(centerPoint);
        updateCoordinatesUI(currentLat, currentLng);
        scheduleReverseGeocode(currentLat, currentLng);
    }

    private void onMapMoved() {
        IGeoPoint center = mapView.getMapCenter();
        if (center == null) return;

        currentLat = center.getLatitude();
        currentLng = center.getLongitude();

        updateCoordinatesUI(currentLat, currentLng);
        scheduleReverseGeocode(currentLat, currentLng);
    }

    private void updateCoordinatesUI(double lat, double lng) {
        if (tvCoordinates != null) {
            tvCoordinates.setText(String.format(Locale.getDefault(), "%.6f, %.6f", lat, lng));
        }
    }

    private void scheduleReverseGeocode(double lat, double lng) {
        if (pendingGeocodeRunnable != null) {
            mainHandler.removeCallbacks(pendingGeocodeRunnable);
        }

        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);

        pendingGeocodeRunnable = () -> {
            // 1. Prioritaskan Nominatim OpenStreetMap API untuk alamat lengkap dan presisi
            NominatimGeocodingService.getInstance().reverseGeocode(lat, lng, new NominatimGeocodingService.ReverseGeocodeCallback() {
                @Override
                public void onSuccess(NominatimPlace place, String street, String kelurahan, String kecamatan, String kota, String provinsi, String fullAddress) {
                    if (isFinishing() || isDestroyed()) return;
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);

                    currentAlamat = !street.isEmpty() ? street : "Titik di Peta";
                    currentKota = kota;
                    currentKecamatan = kecamatan;
                    currentKelurahan = kelurahan;
                    currentProvinsi = provinsi;

                    if (tvStreetName != null) tvStreetName.setText(currentAlamat);
                    if (tvFullAddress != null) tvFullAddress.setText(fullAddress);
                }

                @Override
                public void onError(String message) {
                    // 2. Fallback ke Geocoder bawaan Android jika jaringan ke API lambat
                    executeFallbackGeocoder(lat, lng);
                }
            });
        };

        mainHandler.postDelayed(pendingGeocodeRunnable, 450);
    }

    private void executeFallbackGeocoder(double lat, double lng) {
        new Thread(() -> {
            String street = "Titik di Peta";
            String full = String.format(Locale.getDefault(), "Koordinat: %.5f, %.5f", lat, lng);
            String kota = "";
            String kecamatan = "";
            String kelurahan = "";
            String provinsi = "";

            try {
                if (Geocoder.isPresent()) {
                    Geocoder geocoder = new Geocoder(MapPinPickerActivity.this, new Locale("id", "ID"));
                    List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);

                        if (addr.getThoroughfare() != null && !addr.getThoroughfare().isEmpty()) {
                            street = addr.getThoroughfare();
                            if (addr.getSubThoroughfare() != null) {
                                street += " No. " + addr.getSubThoroughfare();
                            }
                        } else if (addr.getFeatureName() != null && !addr.getFeatureName().isEmpty()) {
                            street = addr.getFeatureName();
                        }

                        if (addr.getSubLocality() != null) kelurahan = addr.getSubLocality();
                        if (addr.getLocality() != null) kecamatan = addr.getLocality();
                        if (addr.getSubAdminArea() != null) kota = addr.getSubAdminArea();
                        if (addr.getAdminArea() != null) provinsi = addr.getAdminArea();

                        StringBuilder sb = new StringBuilder();
                        if (!kelurahan.isEmpty()) sb.append(kelurahan).append(", ");
                        if (!kecamatan.isEmpty()) sb.append(kecamatan).append(", ");
                        if (!kota.isEmpty()) sb.append(kota).append(", ");
                        if (!provinsi.isEmpty()) sb.append(provinsi);

                        String detail = sb.toString();
                        if (!detail.isEmpty()) full = detail;
                        else if (addr.getAddressLine(0) != null) full = addr.getAddressLine(0);
                    }
                }
            } catch (Exception ignored) {}

            final String finalStreet = street;
            final String finalFull = full;
            final String finalKota = kota;
            final String finalKec = kecamatan;
            final String finalKel = kelurahan;
            final String finalProv = provinsi;

            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);

                currentAlamat = finalStreet;
                currentKota = finalKota;
                currentKecamatan = finalKec;
                currentKelurahan = finalKel;
                currentProvinsi = finalProv;

                if (tvStreetName != null) tvStreetName.setText(finalStreet);
                if (tvFullAddress != null) tvFullAddress.setText(finalFull);
            });
        }).start();
    }

    @SuppressLint("MissingPermission")
    private void moveToMyLocation() {
        UserLocationManager locManager = UserLocationManager.getInstance(this);
        if (!locManager.hasLocationPermission()) {
            Toast.makeText(this, "Izin lokasi perangkat belum diaktifkan", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);

        // Ambil posisi realtime berakurasi tinggi lewat Fused Location
        try {
            CancellationTokenSource cts = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            GeoPoint userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            mapController.animateTo(userPoint);
                            mapController.setZoom(17.5);
                        } else {
                            fallbackMoveToMyLocation();
                        }
                    })
                    .addOnFailureListener(e -> fallbackMoveToMyLocation());
        } catch (Exception e) {
            fallbackMoveToMyLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void fallbackMoveToMyLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            try {
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    GeoPoint userPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                    mapController.animateTo(userPoint);
                    mapController.setZoom(17.5);
                    return;
                }
            } catch (SecurityException ignored) {}
        }

        Toast.makeText(MapPinPickerActivity.this, "Gagal mendeteksi lokasi GPS", Toast.LENGTH_SHORT).show();
    }

    private void confirmLocation() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_LAT, currentLat);
        resultIntent.putExtra(EXTRA_LNG, currentLng);
        resultIntent.putExtra(EXTRA_ALAMAT, currentAlamat);
        resultIntent.putExtra(EXTRA_KOTA, currentKota);
        resultIntent.putExtra(EXTRA_KECAMATAN, currentKecamatan);
        resultIntent.putExtra(EXTRA_KELURAHAN, currentKelurahan);
        resultIntent.putExtra(EXTRA_PROVINSI, currentProvinsi);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (pendingGeocodeRunnable != null) {
            mainHandler.removeCallbacks(pendingGeocodeRunnable);
        }
    }
}
