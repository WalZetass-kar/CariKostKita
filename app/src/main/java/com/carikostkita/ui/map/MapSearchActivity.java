package com.carikostkita.ui.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.carikostkita.R;
import com.carikostkita.data.location.NominatimGeocodingService;
import com.carikostkita.data.location.NominatimPlace;
import com.carikostkita.data.location.UserLocationManager;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.MapKostSheetAdapter;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MapSearchActivity extends AppCompatActivity implements MapKostSheetAdapter.OnMapKostClickListener {

    // Center default (Pekanbaru City Center)
    private static final double DEFAULT_LAT = 0.5071;
    private static final double DEFAULT_LNG = 101.4478;

    private MapView mapView;
    private IMapController mapController;

    // Header & Search
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private ImageButton btnFilterDialog;
    private TextView chipAll, chipPutra, chipPutri, chipCampur, chipUnder1m, chipDistance5km;

    // Floating Preview Card
    private View cardPreview;
    private ImageView ivPreviewThumb;
    private TextView tvPreviewTipe, tvPreviewDistance, tvPreviewTitle, tvPreviewAddress, tvPreviewPrice;
    private MaterialButton btnPreviewDetail;
    private ImageButton btnPreviewClose;
    private Kost selectedPreviewKost;

    // Bottom Sheet
    private LinearLayout bottomSheetLayout;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;
    private FloatingActionButton fabToggleSheet;
    private FloatingActionButton fabMyLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvSheetSubtitle, tvSheetCountBadge;
    private ProgressBar pbLoading;
    private View layoutSheetEmpty;
    private RecyclerView rvNearestKost;
    private MapKostSheetAdapter sheetAdapter;

    // Data
    private KostRepository kostRepository;
    private SessionManager sessionManager;
    private final List<Kost> rawKosts = new ArrayList<>();
    private final List<MapKostSheetAdapter.MapKostItem> displayedKostItems = new ArrayList<>();

    // Active Filters
    private String filterQuery = "";
    private String filterTipe = "ALL"; // ALL, PUTRA, PUTRI, CAMPUR
    private double filterMaxPrice = 0; // 0 = unlimited
    private double filterMinPrice = 0;
    private double filterMaxDistanceKm = 0; // 0 = unlimited
    private final List<String> filterFacilities = new ArrayList<>();

    // Center Reference Location
    private double currentAnchorLat = DEFAULT_LAT;
    private double currentAnchorLng = DEFAULT_LNG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_search);

        kostRepository = new KostRepository(this);
        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Ambil anchor dari lokasi pengguna yang aktif jika ada
        double userLat = sessionManager.getUserSelectedLat();
        double userLng = sessionManager.getUserSelectedLng();
        if (userLat != 0.0 && userLng != 0.0) {
            currentAnchorLat = userLat;
            currentAnchorLng = userLng;
        }

        initViews();
        setupMap();
        setupBottomSheet();
        setupFilters();

        // Read intent extras from discovery cards or search
        String initialQuery = getIntent().getStringExtra("search_query");
        if (initialQuery != null && !initialQuery.isEmpty()) {
            filterQuery = initialQuery;
            if (etSearch != null) etSearch.setText(initialQuery);
        }
        String initialTipe = getIntent().getStringExtra("filter_tipe");
        if (initialTipe != null && !initialTipe.isEmpty()) {
            filterTipe = initialTipe.toUpperCase(Locale.getDefault());
            updateQuickChipStyles();
        }

        loadRealKostData();
    }

    private void initViews() {
        mapView = findViewById(R.id.map_view);

        ImageButton btnBack = findViewById(R.id.btn_map_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        etSearch = findViewById(R.id.et_map_search);
        btnClearSearch = findViewById(R.id.btn_map_clear_search);
        btnFilterDialog = findViewById(R.id.btn_map_filter_dialog);

        chipAll = findViewById(R.id.chip_map_all);
        chipPutra = findViewById(R.id.chip_map_putra);
        chipPutri = findViewById(R.id.chip_map_putri);
        chipCampur = findViewById(R.id.chip_map_campur);
        chipUnder1m = findViewById(R.id.chip_map_under_1m);
        chipDistance5km = findViewById(R.id.chip_map_distance_5km);

        cardPreview = findViewById(R.id.card_map_preview);
        ivPreviewThumb = findViewById(R.id.iv_preview_thumb);
        tvPreviewTipe = findViewById(R.id.tv_preview_tipe);
        tvPreviewDistance = findViewById(R.id.tv_preview_distance);
        tvPreviewTitle = findViewById(R.id.tv_preview_title);
        tvPreviewAddress = findViewById(R.id.tv_preview_address);
        tvPreviewPrice = findViewById(R.id.tv_preview_price);
        btnPreviewDetail = findViewById(R.id.btn_preview_detail);
        btnPreviewClose = findViewById(R.id.btn_preview_close);

        fabToggleSheet = findViewById(R.id.fab_map_toggle_sheet);
        fabMyLocation = findViewById(R.id.fab_map_my_location);
        bottomSheetLayout = findViewById(R.id.bottom_sheet_map);
        tvSheetSubtitle = findViewById(R.id.tv_sheet_subtitle);
        tvSheetCountBadge = findViewById(R.id.tv_sheet_count_badge);
        pbLoading = findViewById(R.id.pb_map_loading);
        layoutSheetEmpty = findViewById(R.id.layout_sheet_empty);
        rvNearestKost = findViewById(R.id.rv_map_nearest_kost);

        if (fabMyLocation != null) {
            fabMyLocation.setOnClickListener(v -> moveToMyLocation());
        }

        sheetAdapter = new MapKostSheetAdapter(this);
        rvNearestKost.setLayoutManager(new LinearLayoutManager(this));
        rvNearestKost.setAdapter(sheetAdapter);

        View btnExpandArea = findViewById(R.id.btn_map_expand_area);
        if (btnExpandArea != null) {
            btnExpandArea.setOnClickListener(v -> expandSearchArea());
        }

        btnPreviewClose.setOnClickListener(v -> hidePreviewCard());
        btnPreviewDetail.setOnClickListener(v -> {
            if (selectedPreviewKost != null) {
                Intent intent = new Intent(this, DetailKostActivity.class);
                intent.putExtra("kost_id", selectedPreviewKost.getIdKost());
                startActivity(intent);
            }
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                etSearch.setText("");
                filterQuery = "";
                applyAllFilters();
            });
        }

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                searchPlaceOnMap(etSearch.getText().toString());
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterQuery = s.toString().trim();
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(filterQuery.isEmpty() ? View.GONE : View.VISIBLE);
                }
                applyAllFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFilterDialog.setOnClickListener(v -> showDetailedFilterDialog());
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);

        mapController = mapView.getController();
        mapController.setZoom(13.5);
        final GeoPoint startPoint = new GeoPoint(DEFAULT_LAT, DEFAULT_LNG);
        mapController.setCenter(startPoint);

        mapView.addOnFirstLayoutListener((v, left, top, right, bottom) -> {
            if (mapController != null) {
                mapController.setZoom(13.5);
                mapController.setCenter(new GeoPoint(currentAnchorLat, currentAnchorLng));
            }
        });

        mapView.post(() -> {
            if (mapController != null) {
                mapController.setCenter(new GeoPoint(currentAnchorLat, currentAnchorLng));
            }
        });
    }

    private void setupBottomSheet() {
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        sheetBehavior.setHideable(false);
        sheetBehavior.setFitToContents(true);
        sheetBehavior.setPeekHeight((int) (80 * getResources().getDisplayMetrics().density));

        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    fabToggleSheet.setImageResource(R.drawable.ic_close);
                    if (cardPreview != null) cardPreview.setVisibility(View.GONE);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    fabToggleSheet.setImageResource(R.drawable.ic_arrow_up);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

        fabToggleSheet.setOnClickListener(v -> {
            if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        View peekHeader = findViewById(R.id.layout_sheet_peek_header);
        if (peekHeader != null) {
            peekHeader.setOnClickListener(v -> {
                if (sheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            });
        }
    }

    private void setupFilters() {
        chipAll.setOnClickListener(v -> {
            filterTipe = "ALL";
            filterMaxPrice = 0;
            filterMaxDistanceKm = 0;
            updateQuickChipStyles();
            applyAllFilters();
        });

        chipPutra.setOnClickListener(v -> {
            filterTipe = "PUTRA".equalsIgnoreCase(filterTipe) ? "ALL" : "PUTRA";
            updateQuickChipStyles();
            applyAllFilters();
        });

        chipPutri.setOnClickListener(v -> {
            filterTipe = "PUTRI".equalsIgnoreCase(filterTipe) ? "ALL" : "PUTRI";
            updateQuickChipStyles();
            applyAllFilters();
        });

        chipCampur.setOnClickListener(v -> {
            filterTipe = "CAMPUR".equalsIgnoreCase(filterTipe) ? "ALL" : "CAMPUR";
            updateQuickChipStyles();
            applyAllFilters();
        });

        chipUnder1m.setOnClickListener(v -> {
            if (filterMaxPrice == 1000000) {
                filterMaxPrice = 0;
            } else {
                filterMaxPrice = 1000000;
                filterMinPrice = 0;
            }
            updateQuickChipStyles();
            applyAllFilters();
        });

        chipDistance5km.setOnClickListener(v -> {
            if (filterMaxDistanceKm == 5.0) {
                filterMaxDistanceKm = 0;
            } else {
                filterMaxDistanceKm = 5.0;
            }
            updateQuickChipStyles();
            applyAllFilters();
        });
    }

    private void updateQuickChipStyles() {
        int activeBg = R.drawable.bg_chip_filter_active;
        int inactiveBg = R.drawable.bg_chip_filter_inactive;
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        boolean isAll = "ALL".equalsIgnoreCase(filterTipe) && filterMaxPrice == 0 && filterMaxDistanceKm == 0;
        chipAll.setBackgroundResource(isAll ? activeBg : inactiveBg);
        chipAll.setTextColor(isAll ? activeColor : inactiveColor);

        chipPutra.setBackgroundResource("PUTRA".equalsIgnoreCase(filterTipe) ? activeBg : inactiveBg);
        chipPutra.setTextColor("PUTRA".equalsIgnoreCase(filterTipe) ? activeColor : inactiveColor);

        chipPutri.setBackgroundResource("PUTRI".equalsIgnoreCase(filterTipe) ? activeBg : inactiveBg);
        chipPutri.setTextColor("PUTRI".equalsIgnoreCase(filterTipe) ? activeColor : inactiveColor);

        chipCampur.setBackgroundResource("CAMPUR".equalsIgnoreCase(filterTipe) ? activeBg : inactiveBg);
        chipCampur.setTextColor("CAMPUR".equalsIgnoreCase(filterTipe) ? activeColor : inactiveColor);

        chipUnder1m.setBackgroundResource(filterMaxPrice == 1000000 ? activeBg : inactiveBg);
        chipUnder1m.setTextColor(filterMaxPrice == 1000000 ? activeColor : inactiveColor);

        chipDistance5km.setBackgroundResource(filterMaxDistanceKm == 5.0 ? activeBg : inactiveBg);
        chipDistance5km.setTextColor(filterMaxDistanceKm == 5.0 ? activeColor : inactiveColor);
    }

    private void expandSearchArea() {
        filterMaxDistanceKm = 0;
        filterMaxPrice = 0;
        filterMinPrice = 0;
        filterQuery = "";
        filterTipe = "ALL";
        if (etSearch != null) etSearch.setText("");
        filterFacilities.clear();
        updateQuickChipStyles();
        if (mapController != null) {
            mapController.setZoom(11.5);
            mapController.animateTo(new GeoPoint(DEFAULT_LAT, DEFAULT_LNG));
        }
        applyAllFilters();
    }

    private void loadRealKostData() {
        pbLoading.setVisibility(View.VISIBLE);
        layoutSheetEmpty.setVisibility(View.GONE);
        rvNearestKost.setVisibility(View.GONE);

        kostRepository.getAllActiveKost(sessionManager.getUserId(), new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> list) {
                pbLoading.setVisibility(View.GONE);
                rawKosts.clear();
                if (list != null) {
                    rawKosts.addAll(list);
                }

                // Prioritaskan anchor lokasi pengguna yang aktif jika ada
                double savedLat = sessionManager.getUserSelectedLat();
                double savedLng = sessionManager.getUserSelectedLng();
                if (savedLat != 0.0 && savedLng != 0.0) {
                    currentAnchorLat = savedLat;
                    currentAnchorLng = savedLng;
                } else {
                    for (Kost k : rawKosts) {
                        if (k.getLatitude() != 0.0 && k.getLongitude() != 0.0) {
                            currentAnchorLat = k.getLatitude();
                            currentAnchorLng = k.getLongitude();
                            break;
                        }
                    }
                }
                if (mapController != null) {
                    mapController.setCenter(new GeoPoint(currentAnchorLat, currentAnchorLng));
                }

                applyAllFilters();
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(MapSearchActivity.this, "Gagal memuat data kost: " + message, Toast.LENGTH_SHORT).show();
                applyAllFilters();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void moveToMyLocation() {
        UserLocationManager locManager = UserLocationManager.getInstance(this);
        if (!locManager.hasLocationPermission()) {
            Toast.makeText(this, "Izin lokasi perangkat belum diaktifkan", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            CancellationTokenSource cts = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            currentAnchorLat = location.getLatitude();
                            currentAnchorLng = location.getLongitude();
                            GeoPoint userPoint = new GeoPoint(currentAnchorLat, currentAnchorLng);
                            if (mapController != null) {
                                mapController.animateTo(userPoint);
                                mapController.setZoom(15.5);
                            }
                            applyAllFilters();
                            Toast.makeText(MapSearchActivity.this, "Peta dipusatkan ke lokasi Anda", Toast.LENGTH_SHORT).show();
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
                    currentAnchorLat = loc.getLatitude();
                    currentAnchorLng = loc.getLongitude();
                    GeoPoint userPoint = new GeoPoint(currentAnchorLat, currentAnchorLng);
                    if (mapController != null) {
                        mapController.animateTo(userPoint);
                        mapController.setZoom(15.5);
                    }
                    applyAllFilters();
                    Toast.makeText(MapSearchActivity.this, "Peta dipusatkan ke lokasi Anda", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (SecurityException ignored) {}
        }
        Toast.makeText(MapSearchActivity.this, "Gagal mendeteksi lokasi GPS", Toast.LENGTH_SHORT).show();
    }

    private void searchPlaceOnMap(String query) {
        if (query == null || query.trim().length() < 2) return;

        NominatimGeocodingService.getInstance().searchPlaces(query.trim(), new NominatimGeocodingService.SearchPlacesCallback() {
            @Override
            public void onSuccess(List<NominatimPlace> places) {
                if (places != null && !places.isEmpty()) {
                    NominatimPlace first = places.get(0);
                    currentAnchorLat = first.getLatitude();
                    currentAnchorLng = first.getLongitude();

                    GeoPoint targetPoint = new GeoPoint(currentAnchorLat, currentAnchorLng);
                    if (mapController != null) {
                        mapController.animateTo(targetPoint);
                        mapController.setZoom(15.0);
                    }
                    applyAllFilters();

                    String name = first.getName();
                    if (name == null || name.isEmpty()) name = first.getDisplayName();
                    Toast.makeText(MapSearchActivity.this, "Menuju area: " + name, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                // Biarkan filter teks kost lokal tetap berjalan
            }
        });
    }

    private void applyAllFilters() {
        displayedKostItems.clear();
        mapView.getOverlays().clear();

        Drawable pinDrawable = ContextCompat.getDrawable(this, R.drawable.ic_map_pin_primary);

        for (Kost k : rawKosts) {
            // Filter query
            if (!filterQuery.isEmpty()) {
                String fullSearch = (k.getNamaKost() + " " + k.getFullLocation() + " " + k.getDeskripsi()).toLowerCase();
                if (!fullSearch.contains(filterQuery.toLowerCase())) {
                    continue;
                }
            }

            // Filter Tipe
            if (!"ALL".equalsIgnoreCase(filterTipe)) {
                if (k.getTipeKost() == null || !filterTipe.equalsIgnoreCase(k.getTipeKost().name())) {
                    continue;
                }
            }

            // Filter Harga
            if (filterMaxPrice > 0 && k.getHarga() > filterMaxPrice) {
                continue;
            }
            if (filterMinPrice > 0 && k.getHarga() < filterMinPrice) {
                continue;
            }

            // Hitung jarak Haversine dari anchor peta
            double dist = -1.0;
            if (k.getLatitude() != 0.0 && k.getLongitude() != 0.0) {
                dist = calculateHaversineKm(currentAnchorLat, currentAnchorLng, k.getLatitude(), k.getLongitude());
            }

            // Filter Jarak
            if (filterMaxDistanceKm > 0) {
                if (dist < 0 || dist > filterMaxDistanceKm) {
                    continue;
                }
            }

            // Filter Fasilitas
            if (!filterFacilities.isEmpty()) {
                boolean hasAll = true;
                for (String fac : filterFacilities) {
                    if (k.getFasilitas() == null || !k.getFasilitas().contains(fac)) {
                        hasAll = false;
                        break;
                    }
                }
                if (!hasAll) continue;
            }

            displayedKostItems.add(new MapKostSheetAdapter.MapKostItem(k, dist));

            // Tambahkan marker ke peta jika koordinat valid
            if (k.getLatitude() != 0.0 && k.getLongitude() != 0.0) {
                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(k.getLatitude(), k.getLongitude()));
                marker.setIcon(pinDrawable);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle(k.getNamaKost());
                marker.setSnippet(FormatUtil.formatRupiah(k.getHarga()));

                marker.setOnMarkerClickListener((m, mapView1) -> {
                    showPreviewCard(k, calculateHaversineKm(currentAnchorLat, currentAnchorLng, k.getLatitude(), k.getLongitude()));
                    mapController.animateTo(marker.getPosition());
                    return true;
                });

                mapView.getOverlays().add(marker);
            }
        }

        // Urutkan list berdasarkan jarak terdekat
        Collections.sort(displayedKostItems, (a, b) -> {
            if (a.distanceKm < 0 && b.distanceKm < 0) return 0;
            if (a.distanceKm < 0) return 1;
            if (b.distanceKm < 0) return -1;
            return Double.compare(a.distanceKm, b.distanceKm);
        });

        // Update Bottom Sheet
        sheetAdapter.submitList(displayedKostItems);
        tvSheetCountBadge.setText(displayedKostItems.size() + " Kost");
        tvSheetSubtitle.setText("Menampilkan " + displayedKostItems.size() + " kost terdekat dari lokasi");

        if (displayedKostItems.isEmpty()) {
            layoutSheetEmpty.setVisibility(View.VISIBLE);
            rvNearestKost.setVisibility(View.GONE);
            tvSheetCountBadge.setText("0 Kost");
            tvSheetSubtitle.setText("Belum ada kost di sekitar area ini");
            if (sheetBehavior != null && sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        } else {
            layoutSheetEmpty.setVisibility(View.GONE);
            rvNearestKost.setVisibility(View.VISIBLE);
        }

        mapView.invalidate();
    }

    private void showPreviewCard(Kost kost, double distanceKm) {
        selectedPreviewKost = kost;
        if (cardPreview == null) return;

        cardPreview.setVisibility(View.VISIBLE);
        tvPreviewTitle.setText(kost.getNamaKost());
        tvPreviewAddress.setText(kost.getFullLocation());
        tvPreviewPrice.setText(FormatUtil.formatRupiah(kost.getHarga()) + " / bln");

        if (distanceKm >= 0) {
            tvPreviewDistance.setVisibility(View.VISIBLE);
            if (distanceKm < 1.0) {
                tvPreviewDistance.setText(String.format(Locale.getDefault(), "%d m", Math.round(distanceKm * 1000)));
            } else {
                tvPreviewDistance.setText(String.format(Locale.getDefault(), "%.1f km", distanceKm));
            }
        } else {
            tvPreviewDistance.setVisibility(View.GONE);
        }

        if (kost.getTipeKost() != null) {
            tvPreviewTipe.setText(kost.getTipeKost().name());
            if ("PUTRA".equalsIgnoreCase(kost.getTipeKost().name())) {
                tvPreviewTipe.setBackgroundResource(R.drawable.bg_badge_putra);
                tvPreviewTipe.setTextColor(ContextCompat.getColor(this, R.color.badge_putra));
            } else if ("PUTRI".equalsIgnoreCase(kost.getTipeKost().name())) {
                tvPreviewTipe.setBackgroundResource(R.drawable.bg_badge_putri);
                tvPreviewTipe.setTextColor(ContextCompat.getColor(this, R.color.badge_putri));
            } else {
                tvPreviewTipe.setBackgroundResource(R.drawable.bg_badge_campur);
                tvPreviewTipe.setTextColor(ContextCompat.getColor(this, R.color.badge_campur));
            }
        }

        String thumb = (kost.getFotoUtama() != null && !kost.getFotoUtama().isEmpty()) ? kost.getFotoUtama() : null;
        Glide.with(this)
                .load(thumb)
                .placeholder(R.drawable.bg_thumb_placeholder)
                .error(R.drawable.bg_thumb_placeholder)
                .centerCrop()
                .into(ivPreviewThumb);
    }

    private void hidePreviewCard() {
        if (cardPreview != null) cardPreview.setVisibility(View.GONE);
        selectedPreviewKost = null;
    }

    @Override
    public void onKostSelected(MapKostSheetAdapter.MapKostItem item) {
        if (item.kost.getLatitude() != 0.0 && item.kost.getLongitude() != 0.0) {
            GeoPoint point = new GeoPoint(item.kost.getLatitude(), item.kost.getLongitude());
            mapController.animateTo(point);
            showPreviewCard(item.kost, item.distanceKm);
            if (sheetBehavior != null) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        } else {
            Intent intent = new Intent(this, DetailKostActivity.class);
            intent.putExtra("kost_id", item.kost.getIdKost());
            startActivity(intent);
        }
    }

    private void showDetailedFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_map_filter, null);
        dialog.setContentView(view);

        TextView chipTipeAll = view.findViewById(R.id.chip_dialog_tipe_all);
        TextView chipTipePutra = view.findViewById(R.id.chip_dialog_tipe_putra);
        TextView chipTipePutri = view.findViewById(R.id.chip_dialog_tipe_putri);
        TextView chipTipeCampur = view.findViewById(R.id.chip_dialog_tipe_campur);

        TextView chipPriceAll = view.findViewById(R.id.chip_dialog_price_all);
        TextView chipPrice500k = view.findViewById(R.id.chip_dialog_price_500k);
        TextView chipPrice1m = view.findViewById(R.id.chip_dialog_price_1m);
        TextView chipPrice2m = view.findViewById(R.id.chip_dialog_price_2m);
        TextView chipPriceAbove = view.findViewById(R.id.chip_dialog_price_above);

        TextView chipDistAll = view.findViewById(R.id.chip_dialog_dist_all);
        TextView chipDist2km = view.findViewById(R.id.chip_dialog_dist_2km);
        TextView chipDist5km = view.findViewById(R.id.chip_dialog_dist_5km);
        TextView chipDist10km = view.findViewById(R.id.chip_dialog_dist_10km);

        TextView chipFacWifi = view.findViewById(R.id.chip_dialog_fac_wifi);
        TextView chipFacAc = view.findViewById(R.id.chip_dialog_fac_ac);
        TextView chipFacKm = view.findViewById(R.id.chip_dialog_fac_km);
        TextView chipFacParkir = view.findViewById(R.id.chip_dialog_fac_parkir);

        final String[] tempTipe = {filterTipe};
        final double[] tempMinPrice = {filterMinPrice};
        final double[] tempMaxPrice = {filterMaxPrice};
        final double[] tempMaxDist = {filterMaxDistanceKm};
        final List<String> tempFacs = new ArrayList<>(filterFacilities);

        int activeBg = R.drawable.bg_chip_filter_active;
        int inactiveBg = R.drawable.bg_chip_filter_inactive;
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        Runnable refreshDialogUI = () -> {
            chipTipeAll.setBackgroundResource("ALL".equalsIgnoreCase(tempTipe[0]) ? activeBg : inactiveBg);
            chipTipeAll.setTextColor("ALL".equalsIgnoreCase(tempTipe[0]) ? activeColor : inactiveColor);
            chipTipePutra.setBackgroundResource("PUTRA".equalsIgnoreCase(tempTipe[0]) ? activeBg : inactiveBg);
            chipTipePutra.setTextColor("PUTRA".equalsIgnoreCase(tempTipe[0]) ? activeColor : inactiveColor);
            chipTipePutri.setBackgroundResource("PUTRI".equalsIgnoreCase(tempTipe[0]) ? activeBg : inactiveBg);
            chipTipePutri.setTextColor("PUTRI".equalsIgnoreCase(tempTipe[0]) ? activeColor : inactiveColor);
            chipTipeCampur.setBackgroundResource("CAMPUR".equalsIgnoreCase(tempTipe[0]) ? activeBg : inactiveBg);
            chipTipeCampur.setTextColor("CAMPUR".equalsIgnoreCase(tempTipe[0]) ? activeColor : inactiveColor);

            chipPriceAll.setBackgroundResource(tempMaxPrice[0] == 0 && tempMinPrice[0] == 0 ? activeBg : inactiveBg);
            chipPriceAll.setTextColor(tempMaxPrice[0] == 0 && tempMinPrice[0] == 0 ? activeColor : inactiveColor);
            chipPrice500k.setBackgroundResource(tempMaxPrice[0] == 500000 ? activeBg : inactiveBg);
            chipPrice500k.setTextColor(tempMaxPrice[0] == 500000 ? activeColor : inactiveColor);
            chipPrice1m.setBackgroundResource(tempMinPrice[0] == 500000 && tempMaxPrice[0] == 1000000 ? activeBg : inactiveBg);
            chipPrice1m.setTextColor(tempMinPrice[0] == 500000 && tempMaxPrice[0] == 1000000 ? activeColor : inactiveColor);
            chipPrice2m.setBackgroundResource(tempMinPrice[0] == 1000000 && tempMaxPrice[0] == 2000000 ? activeBg : inactiveBg);
            chipPrice2m.setTextColor(tempMinPrice[0] == 1000000 && tempMaxPrice[0] == 2000000 ? activeColor : inactiveColor);
            chipPriceAbove.setBackgroundResource(tempMinPrice[0] == 2000000 ? activeBg : inactiveBg);
            chipPriceAbove.setTextColor(tempMinPrice[0] == 2000000 ? activeColor : inactiveColor);

            chipDistAll.setBackgroundResource(tempMaxDist[0] == 0 ? activeBg : inactiveBg);
            chipDistAll.setTextColor(tempMaxDist[0] == 0 ? activeColor : inactiveColor);
            chipDist2km.setBackgroundResource(tempMaxDist[0] == 2.0 ? activeBg : inactiveBg);
            chipDist2km.setTextColor(tempMaxDist[0] == 2.0 ? activeColor : inactiveColor);
            chipDist5km.setBackgroundResource(tempMaxDist[0] == 5.0 ? activeBg : inactiveBg);
            chipDist5km.setTextColor(tempMaxDist[0] == 5.0 ? activeColor : inactiveColor);
            chipDist10km.setBackgroundResource(tempMaxDist[0] == 10.0 ? activeBg : inactiveBg);
            chipDist10km.setTextColor(tempMaxDist[0] == 10.0 ? activeColor : inactiveColor);

            chipFacWifi.setBackgroundResource(tempFacs.contains("WiFi Cepat") ? activeBg : inactiveBg);
            chipFacWifi.setTextColor(tempFacs.contains("WiFi Cepat") ? activeColor : inactiveColor);
            chipFacAc.setBackgroundResource(tempFacs.contains("AC Dingin") ? activeBg : inactiveBg);
            chipFacAc.setTextColor(tempFacs.contains("AC Dingin") ? activeColor : inactiveColor);
            chipFacKm.setBackgroundResource(tempFacs.contains("Kamar Mandi Dalam") ? activeBg : inactiveBg);
            chipFacKm.setTextColor(tempFacs.contains("Kamar Mandi Dalam") ? activeColor : inactiveColor);
            chipFacParkir.setBackgroundResource(tempFacs.contains("Parkir Motor") ? activeBg : inactiveBg);
            chipFacParkir.setTextColor(tempFacs.contains("Parkir Motor") ? activeColor : inactiveColor);
        };

        refreshDialogUI.run();

        chipTipeAll.setOnClickListener(v -> { tempTipe[0] = "ALL"; refreshDialogUI.run(); });
        chipTipePutra.setOnClickListener(v -> { tempTipe[0] = "PUTRA"; refreshDialogUI.run(); });
        chipTipePutri.setOnClickListener(v -> { tempTipe[0] = "PUTRI"; refreshDialogUI.run(); });
        chipTipeCampur.setOnClickListener(v -> { tempTipe[0] = "CAMPUR"; refreshDialogUI.run(); });

        chipPriceAll.setOnClickListener(v -> { tempMinPrice[0] = 0; tempMaxPrice[0] = 0; refreshDialogUI.run(); });
        chipPrice500k.setOnClickListener(v -> { tempMinPrice[0] = 0; tempMaxPrice[0] = 500000; refreshDialogUI.run(); });
        chipPrice1m.setOnClickListener(v -> { tempMinPrice[0] = 500000; tempMaxPrice[0] = 1000000; refreshDialogUI.run(); });
        chipPrice2m.setOnClickListener(v -> { tempMinPrice[0] = 1000000; tempMaxPrice[0] = 2000000; refreshDialogUI.run(); });
        chipPriceAbove.setOnClickListener(v -> { tempMinPrice[0] = 2000000; tempMaxPrice[0] = 0; refreshDialogUI.run(); });

        chipDistAll.setOnClickListener(v -> { tempMaxDist[0] = 0; refreshDialogUI.run(); });
        chipDist2km.setOnClickListener(v -> { tempMaxDist[0] = 2.0; refreshDialogUI.run(); });
        chipDist5km.setOnClickListener(v -> { tempMaxDist[0] = 5.0; refreshDialogUI.run(); });
        chipDist10km.setOnClickListener(v -> { tempMaxDist[0] = 10.0; refreshDialogUI.run(); });

        chipFacWifi.setOnClickListener(v -> { toggleFacility(tempFacs, "WiFi Cepat"); refreshDialogUI.run(); });
        chipFacAc.setOnClickListener(v -> { toggleFacility(tempFacs, "AC Dingin"); refreshDialogUI.run(); });
        chipFacKm.setOnClickListener(v -> { toggleFacility(tempFacs, "Kamar Mandi Dalam"); refreshDialogUI.run(); });
        chipFacParkir.setOnClickListener(v -> { toggleFacility(tempFacs, "Parkir Motor"); refreshDialogUI.run(); });

        View btnReset = view.findViewById(R.id.btn_filter_reset);
        btnReset.setOnClickListener(v -> {
            tempTipe[0] = "ALL";
            tempMinPrice[0] = 0;
            tempMaxPrice[0] = 0;
            tempMaxDist[0] = 0;
            tempFacs.clear();
            refreshDialogUI.run();
        });

        View btnApply = view.findViewById(R.id.btn_dialog_apply_filter);
        btnApply.setOnClickListener(v -> {
            filterTipe = tempTipe[0];
            filterMinPrice = tempMinPrice[0];
            filterMaxPrice = tempMaxPrice[0];
            filterMaxDistanceKm = tempMaxDist[0];
            filterFacilities.clear();
            filterFacilities.addAll(tempFacs);
            dialog.dismiss();
            updateQuickChipStyles();
            applyAllFilters();
        });

        dialog.show();
    }

    private void toggleFacility(List<String> list, String fac) {
        if (list.contains(fac)) {
            list.remove(fac);
        } else {
            list.add(fac);
        }
    }

    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
    }
}
