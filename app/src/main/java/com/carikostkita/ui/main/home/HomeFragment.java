package com.carikostkita.ui.main.home;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.carikostkita.R;
import com.carikostkita.data.location.IndonesiaLocationData;
import com.carikostkita.data.location.UserLocationManager;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.KostAdapter;
import com.carikostkita.ui.adapter.KostCarouselAdapter;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.ui.location.LocationPickerBottomSheet;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.ui.map.MapSearchActivity;
import com.carikostkita.util.AuthPrompt;
import com.carikostkita.util.ErrorMessages;
import com.carikostkita.util.GeoUtil;
import com.carikostkita.util.SessionManager;
import com.carikostkita.util.SkeletonHelper;
import com.carikostkita.util.TouchFeedbackUtil;
import com.carikostkita.util.UserAvatarHelper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment implements KostAdapter.OnKostClickListener, KostCarouselAdapter.OnCarouselKostClickListener {

    private SwipeRefreshLayout swipeRefresh;
    private ImageView ivAvatar;
    private TextView tvSalam;
    private TextView tvGreeting;
    private TextView tvLocationLabel;
    private TextView tvSearchHint;
    private TextView tvDiscoveryAreaTitle;
    private TextView tvSectionNewTitle;
    private final TextView[] areaTitles = new TextView[4];
    private final TextView[] areaSubs = new TextView[4];
    private final String[] areas = new String[4];
    private UserLocationManager userLocationManager;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private TextView chipSemua, chipPutra, chipPutri, chipCampur;
    private View bannerKamarKosong;
    private TextView tvBannerText;
    private View badgeNotification;

    private View layoutSkeleton;
    private View layoutHomeContent;
    private View layoutEmpty;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private TextView btnEmptyPrimary;
    private View layoutSectionTerjangkau;
    private View layoutSectionNew;

    private RecyclerView rvCarousel;
    private RecyclerView rvKost;

    private KostAdapter kostAdapter;
    private KostCarouselAdapter carouselAdapter;
    private KostRepository kostRepository;
    private SessionManager sessionManager;

    private List<Kost> masterKostList = new ArrayList<>();
    private String selectedCategory = "SEMUA";
    private boolean hasLoadedOnce = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (fineGranted || coarseGranted) {
                        detectDeviceLocation();
                    } else {
                        updateLocationUI();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        kostRepository = new KostRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        userLocationManager = UserLocationManager.getInstance(requireContext());

        swipeRefresh = view.findViewById(R.id.swipe_refresh_home);
        swipeRefresh.setColorSchemeResources(R.color.primary);
        ivAvatar = view.findViewById(R.id.iv_home_avatar);
        tvSalam = view.findViewById(R.id.tv_home_salam);
        tvGreeting = view.findViewById(R.id.tv_home_greeting);
        tvLocationLabel = view.findViewById(R.id.tv_home_location_label);
        tvSearchHint = view.findViewById(R.id.tv_home_search_hint);
        tvDiscoveryAreaTitle = view.findViewById(R.id.tv_home_discovery_area_title);
        badgeNotification = view.findViewById(R.id.badge_home_notification);

        // Siluet & ornamen header mengikuti sudut melengkung background
        View header = view.findViewById(R.id.header_home_banner);
        if (header != null) header.setClipToOutline(true);

        View layoutLocationChip = view.findViewById(R.id.layout_home_location_chip);
        if (layoutLocationChip != null) {
            layoutLocationChip.setOnClickListener(v -> showLocationPicker());
            TouchFeedbackUtil.attachPress(layoutLocationChip);
        }

        chipSemua = view.findViewById(R.id.chip_home_semua);
        chipPutra = view.findViewById(R.id.chip_home_putra);
        chipPutri = view.findViewById(R.id.chip_home_putri);
        chipCampur = view.findViewById(R.id.chip_home_campur);

        bannerKamarKosong = view.findViewById(R.id.banner_kamar_kosong);
        tvBannerText = view.findViewById(R.id.tv_banner_text);

        layoutSkeleton = view.findViewById(R.id.skeleton_home);
        layoutHomeContent = view.findViewById(R.id.layout_home_content);
        layoutEmpty = view.findViewById(R.id.layout_home_empty);
        ivEmptyIcon = view.findViewById(R.id.iv_home_empty_icon);
        tvEmptyTitle = view.findViewById(R.id.tv_home_empty_title);
        tvEmptyDesc = view.findViewById(R.id.tv_home_empty_desc);
        btnEmptyPrimary = view.findViewById(R.id.btn_home_empty_expand);
        layoutSectionTerjangkau = view.findViewById(R.id.layout_section_terjangkau);
        layoutSectionNew = view.findViewById(R.id.layout_section_new);
        tvSectionNewTitle = view.findViewById(R.id.tv_home_section_new_title);

        rvCarousel = view.findViewById(R.id.rv_home_carousel);
        rvKost = view.findViewById(R.id.rv_home_kost);

        int[] titleIds = {R.id.tv_area_1_title, R.id.tv_area_2_title, R.id.tv_area_3_title, R.id.tv_area_4_title};
        int[] subIds = {R.id.tv_area_1_sub, R.id.tv_area_2_sub, R.id.tv_area_3_sub, R.id.tv_area_4_sub};
        int[] cardIds = {R.id.card_area_bukit_raya, R.id.card_area_tangkerang, R.id.card_area_marpoyan, R.id.card_area_simpang_tiga};
        for (int i = 0; i < 4; i++) {
            areaTitles[i] = view.findViewById(titleIds[i]);
            areaSubs[i] = view.findViewById(subIds[i]);
            final int idx = i;
            View card = view.findViewById(cardIds[i]);
            if (card != null) card.setOnClickListener(v -> openMapWithQuery(areas[idx]));
        }

        View btnEmptyMap = view.findViewById(R.id.btn_home_empty_map);
        if (btnEmptyMap != null) btnEmptyMap.setOnClickListener(v -> openMapWithQuery(""));

        refreshUserProfile();
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToProfile();
                }
            });
        }

        View btnNotification = view.findViewById(R.id.btn_home_notification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                if (!AuthPrompt.require(requireContext(), "Masuk untuk melihat pesan dari pemilik kost.")) return;
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToChat();
                }
            });
            TouchFeedbackUtil.attachPress(btnNotification);
        }

        // Search bar Beranda membuka tab Cari: satu tempat pencarian untuk seluruh aplikasi
        View mockSearch = view.findViewById(R.id.layout_mock_search);
        if (mockSearch != null) {
            mockSearch.setOnClickListener(v -> openSearchTab(null));
            TouchFeedbackUtil.attachPress(mockSearch);
        }
        View btnMap = view.findViewById(R.id.btn_home_filter_trigger);
        if (btnMap != null) {
            btnMap.setOnClickListener(v -> openMapWithQuery(""));
            TouchFeedbackUtil.attachPress(btnMap);
        }

        setupTrustCards(view);

        carouselAdapter = new KostCarouselAdapter(this);
        rvCarousel.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCarousel.setAdapter(carouselAdapter);

        kostAdapter = new KostAdapter(this);
        rvKost.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvKost.setNestedScrollingEnabled(false);
        rvKost.setAdapter(kostAdapter);

        chipSemua.setOnClickListener(v -> filterByCategory("SEMUA"));
        chipPutra.setOnClickListener(v -> filterByCategory("PUTRA"));
        chipPutri.setOnClickListener(v -> filterByCategory("PUTRI"));
        chipCampur.setOnClickListener(v -> filterByCategory("CAMPUR"));

        if (bannerKamarKosong != null) {
            bannerKamarKosong.setOnClickListener(v -> openSearchTab("AVAILABLE"));
        }

        View btnSeeAllTerjangkau = view.findViewById(R.id.btn_see_all_terjangkau);
        View btnSeeAllNew = view.findViewById(R.id.btn_see_all_new);
        if (btnSeeAllTerjangkau != null) btnSeeAllTerjangkau.setOnClickListener(v -> openSearchTab("SORT_CHEAPEST"));
        if (btnSeeAllNew != null) btnSeeAllNew.setOnClickListener(v -> openSearchTab(null));

        swipeRefresh.setOnRefreshListener(() -> loadData(false));

        loadData(true);

        // Lokasi tidak dipaksa: izin hanya diminta saat pengguna memilihnya
        updateLocationUI();
        if (sessionManager.hasLocationSaved()) {
            updateDiscoveryArea(sessionManager.getUserSelectedCity());
        } else if (userLocationManager.hasLocationPermission()) {
            detectDeviceLocation();
        } else {
            updateDiscoveryArea(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUserProfile();
        // Sinkronkan status favorit yang mungkin berubah di tab/halaman lain tanpa skeleton
        if (hasLoadedOnce) loadData(false);
    }

    private void bindTrustCard(View card, int icon, String title, String desc, View.OnClickListener onClick) {
        if (card == null) return;
        ImageView iv = card.findViewById(R.id.iv_trust_icon);
        TextView tvTitle = card.findViewById(R.id.tv_trust_title);
        TextView tvDesc = card.findViewById(R.id.tv_trust_desc);
        if (iv != null) iv.setImageResource(icon);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvDesc != null) tvDesc.setText(desc);
        card.setContentDescription(title + ". " + desc);
        card.setOnClickListener(onClick);
        TouchFeedbackUtil.attachPress(card);
    }

    private void setupTrustCards(View root) {
        bindTrustCard(root.findViewById(R.id.trust_card_verified), R.drawable.ic_verified,
                "Terverifikasi", "Dicek tim sebelum tampil",
                v -> com.carikostkita.util.AppInfoSheets.showAbout(requireContext()));
        bindTrustCard(root.findViewById(R.id.trust_card_chat), R.drawable.ic_nav_chat,
                "Chat langsung", "Tanpa perantara & komisi", v -> {
                    if (!AuthPrompt.require(requireContext(), "Masuk untuk chat langsung dengan pemilik kost.")) return;
                    if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateToChat();
                });
        bindTrustCard(root.findViewById(R.id.trust_card_map), R.drawable.ic_map,
                "Lewat peta", "Lihat jarak dari lokasimu", v -> openMapWithQuery(""));
    }

    private void refreshUserProfile() {
        if (!isAdded()) return;
        if (tvSalam != null) tvSalam.setText(greetingForNow());
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            if (tvGreeting != null) tvGreeting.setText(firstName(sessionManager.getUserName()));
            if (ivAvatar != null) UserAvatarHelper.loadAvatar(ivAvatar, sessionManager.getUserAvatar());
        } else {
            if (tvGreeting != null) tvGreeting.setText("Pencari Kost");
            if (ivAvatar != null) UserAvatarHelper.loadAvatar(ivAvatar, null);
        }
    }

    private static String greetingForNow() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 11) return "Selamat pagi,";
        if (hour < 15) return "Selamat siang,";
        if (hour < 19) return "Selamat sore,";
        return "Selamat malam,";
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "Pencari Kost";
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    private void loadData(boolean showSkeleton) {
        long startTime = SkeletonHelper.markStart();

        if (showSkeleton && !swipeRefresh.isRefreshing()) {
            if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.VISIBLE);
            if (layoutHomeContent != null) layoutHomeContent.setVisibility(View.GONE);
        }

        kostRepository.getAllActiveKost(sessionManager.getUserUid(), new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> allKost) {
                SkeletonHelper.complete(startTime, () -> onDataLoaded(allKost));
            }

            @Override
            public void onError(String message) {
                SkeletonHelper.complete(startTime, () -> onErrorLoaded(message));
            }
        });
    }

    private void showContent() {
        if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
        if (layoutHomeContent != null) layoutHomeContent.setVisibility(View.VISIBLE);
        swipeRefresh.setRefreshing(false);
    }

    private void onDataLoaded(List<Kost> allKost) {
        if (!isAdded()) return;
        hasLoadedOnce = true;
        showContent();

        masterKostList = (allKost != null) ? allKost : new ArrayList<>();

        if (masterKostList.isEmpty()) {
            showEmptyState(false, null);
            return;
        }

        layoutEmpty.setVisibility(View.GONE);
        rvKost.setVisibility(View.VISIBLE);
        if (bannerKamarKosong != null) bannerKamarKosong.setVisibility(View.VISIBLE);
        if (layoutSectionTerjangkau != null) layoutSectionTerjangkau.setVisibility(View.VISIBLE);
        if (layoutSectionNew != null) layoutSectionNew.setVisibility(View.VISIBLE);

        int countPutri = 0, countPutra = 0, countCampur = 0, availableKost = 0;
        for (Kost k : masterKostList) {
            if (k.getTipeKost() == TipeKost.PUTRI) countPutri++;
            else if (k.getTipeKost() == TipeKost.PUTRA) countPutra++;
            else if (k.getTipeKost() == TipeKost.CAMPUR) countCampur++;
            if (k.isAvailable()) availableKost++;
        }

        chipSemua.setText("Semua " + masterKostList.size());
        chipPutra.setText("Putra " + countPutra);
        chipPutri.setText("Putri " + countPutri);
        chipCampur.setText("Campur " + countCampur);

        if (tvBannerText != null) {
            tvBannerText.setText(availableKost > 0
                    ? availableKost + " kost masih punya kamar kosong"
                    : "Semua kost sedang penuh. Simpan favorit untuk memantau.");
        }

        List<Kost> affordableList = new ArrayList<>(masterKostList);
        Collections.sort(affordableList, Comparator.comparingDouble(Kost::getHarga));
        carouselAdapter.submitList(new ArrayList<>(affordableList.subList(0, Math.min(6, affordableList.size()))));

        applyCategoryFilter();
    }

    /** Empty state & error state memakai kartu yang sama dengan isi yang berbeda. */
    private void showEmptyState(boolean isError, String errorMessage) {
        layoutEmpty.setVisibility(View.VISIBLE);
        rvKost.setVisibility(View.GONE);
        if (bannerKamarKosong != null) bannerKamarKosong.setVisibility(View.GONE);
        if (layoutSectionTerjangkau != null) layoutSectionTerjangkau.setVisibility(View.GONE);
        if (layoutSectionNew != null) layoutSectionNew.setVisibility(View.GONE);
        chipSemua.setText("Semua");
        chipPutra.setText("Putra");
        chipPutri.setText("Putri");
        chipCampur.setText("Campur");

        if (isError) {
            if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(ErrorMessages.isOffline(errorMessage) ? R.drawable.ic_error_circle : R.drawable.ic_warning);
            if (tvEmptyTitle != null) tvEmptyTitle.setText(ErrorMessages.isOffline(errorMessage) ? "Kamu Sedang Offline" : "Gagal Memuat Kost");
            if (tvEmptyDesc != null) tvEmptyDesc.setText(errorMessage);
            if (btnEmptyPrimary != null) {
                btnEmptyPrimary.setText("Coba Lagi");
                btnEmptyPrimary.setOnClickListener(v -> loadData(true));
            }
        } else {
            String city = userLocationManager.getActiveCity();
            if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(R.drawable.il_empty_kost);
            if (tvEmptyTitle != null) tvEmptyTitle.setText("Belum Ada Kost Tersedia");
            if (tvEmptyDesc != null) {
                tvEmptyDesc.setText("Belum ada kost terverifikasi" + (city != null && !city.isEmpty() ? " di " + city : "")
                        + ". Kost baru tampil di sini setelah lolos verifikasi tim CariKostKita.");
            }
            if (btnEmptyPrimary != null) {
                btnEmptyPrimary.setText("Ganti Lokasi");
                btnEmptyPrimary.setOnClickListener(v -> showLocationPicker());
            }
        }
    }

    private void filterByCategory(String category) {
        selectedCategory = category;
        updateChipsVisual();
        applyCategoryFilter();
    }

    private void updateChipsVisual() {
        setChipStyle(chipSemua, "SEMUA".equals(selectedCategory));
        setChipStyle(chipPutra, "PUTRA".equals(selectedCategory));
        setChipStyle(chipPutri, "PUTRI".equals(selectedCategory));
        setChipStyle(chipCampur, "CAMPUR".equals(selectedCategory));
    }

    private void setChipStyle(TextView chip, boolean isActive) {
        if (!isAdded() || chip == null) return;
        if (isActive) {
            chip.setBackgroundResource(R.drawable.bg_badge_kategori_selected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_badge_kategori_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    /**
     * Daftar utama: urut jarak terdekat bila lokasi pengguna diketahui,
     * selain itu urut dari yang terbaru. Judul section mengikuti urutan sebenarnya.
     */
    private void applyCategoryFilter() {
        List<Kost> filtered = new ArrayList<>();
        for (Kost k : masterKostList) {
            if ("SEMUA".equals(selectedCategory)
                    || (k.getTipeKost() != null && k.getTipeKost().name().equals(selectedCategory))) {
                filtered.add(k);
            }
        }

        double lat = sessionManager.getUserSelectedLat();
        double lng = sessionManager.getUserSelectedLng();
        boolean hasLocation = lat != 0 && lng != 0;
        if (hasLocation) {
            Collections.sort(filtered, (a, b) -> {
                double da = a.hasCoordinates() ? GeoUtil.distanceKm(lat, lng, a.getLatitude(), a.getLongitude()) : Double.MAX_VALUE;
                double db = b.hasCoordinates() ? GeoUtil.distanceKm(lat, lng, b.getLatitude(), b.getLongitude()) : Double.MAX_VALUE;
                return Double.compare(da, db);
            });
            kostAdapter.setDistanceOrigin(lat, lng);
        } else {
            Collections.sort(filtered, (a, b) -> {
                if (b.getCreatedAt() != null && a.getCreatedAt() != null) {
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                }
                return 0;
            });
            kostAdapter.setDistanceOrigin(0, 0);
        }
        carouselAdapter.notifyDataSetChanged();
        kostAdapter.submitList(filtered);
        updateSectionTitle(hasLocation);
    }

    private void updateSectionTitle(boolean sortedByDistance) {
        if (tvSectionNewTitle == null) return;
        String loc = userLocationManager.getActiveLocationDisplay();
        boolean hasLabel = loc != null && !loc.trim().isEmpty() && !loc.equalsIgnoreCase("Pilih Lokasi");
        if (sortedByDistance) {
            tvSectionNewTitle.setText(hasLabel ? "Terdekat dari " + shortLocation(loc) : "Terdekat dari lokasimu");
        } else {
            tvSectionNewTitle.setText("Baru ditambahkan");
        }
    }

    private static String shortLocation(String display) {
        int comma = display.indexOf(',');
        return comma > 0 ? display.substring(0, comma).trim() : display.trim();
    }

    private void onErrorLoaded(String msg) {
        if (!isAdded()) return;
        showContent();
        if (masterKostList.isEmpty()) {
            showEmptyState(true, msg);
        } else {
            // Data lama tetap tampil; cukup beri tahu bahwa pembaruan gagal
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onKostClick(Kost kost) {
        Intent intent = new Intent(requireContext(), DetailKostActivity.class);
        intent.putExtra("kost_id", kost.getIdKost());
        startActivity(intent);
    }

    @Override
    public void onFavoriteToggle(Kost kost, int position) {
        if (!AuthPrompt.require(requireContext(), "Masuk untuk menyimpan kost favorit dan membukanya lagi kapan saja.")) return;

        kostRepository.toggleFavorite(sessionManager.getUserUid(), kost.getIdKost(), new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isFavorite) {
                kost.setFavorite(isFavorite);
                kostAdapter.notifyDataSetChanged();
                carouselAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openSearchTab(String quickAction) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToSearch(quickAction, quickAction == null);
        }
    }

    private void openMapWithQuery(String query) {
        Intent intent = new Intent(requireContext(), MapSearchActivity.class);
        if (query != null && !query.isEmpty()) {
            intent.putExtra("search_query", query);
        }
        startActivity(intent);
    }

    private void showLocationPicker() {
        LocationPickerBottomSheet.show(requireContext(), (city, district, display) -> {
            updateLocationUI();
            updateDiscoveryArea(city);
            applyCategoryFilter();
        });
    }

    private void detectDeviceLocation() {
        if (tvLocationLabel != null) {
            tvLocationLabel.setText("Mendeteksi lokasi…");
        }
        userLocationManager.detectCurrentLocation(new UserLocationManager.LocationCallback() {
            @Override
            public void onLocationDetected(String city, String district, double lat, double lng, String display) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    updateLocationUI();
                    updateDiscoveryArea(city);
                    applyCategoryFilter();
                });
            }

            @Override
            public void onLocationFailed(String fallbackMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> updateLocationUI());
            }
        });
    }

    private void updateLocationUI() {
        if (!isAdded()) return;
        String loc = userLocationManager.getActiveLocationDisplay();
        boolean hasLabel = loc != null && !loc.trim().isEmpty() && !loc.equalsIgnoreCase("Pilih Lokasi");
        if (tvLocationLabel != null) tvLocationLabel.setText(hasLabel ? loc : "Pilih lokasi");
        if (tvSearchHint != null) {
            String city = userLocationManager.getActiveCity();
            tvSearchHint.setText(city != null && !city.isEmpty()
                    ? "Cari kost di " + city + "…"
                    : "Cari nama kost, jalan, kelurahan…");
        }
    }

    private void updateDiscoveryArea(String city) {
        if (!isAdded()) return;
        String activeCity = (city != null && !city.trim().isEmpty()) ? city.trim() : userLocationManager.getActiveCity();
        if (activeCity == null || activeCity.trim().isEmpty()) {
            activeCity = "Pekanbaru";
        }

        if (tvDiscoveryAreaTitle != null) {
            tvDiscoveryAreaTitle.setText("Jelajahi Area " + activeCity);
        }

        List<String> kecList = IndonesiaLocationData.getKecamatanList(activeCity);
        for (int i = 0; i < 4; i++) {
            if (kecList != null && kecList.size() > i) {
                areas[i] = kecList.get(i);
            } else {
                areas[i] = i == 0 ? activeCity : null;
            }
            View card = areaTitles[i] != null ? (View) areaTitles[i].getParent().getParent().getParent() : null;
            if (areas[i] == null) {
                if (card != null) card.setVisibility(View.GONE);
                continue;
            }
            if (card != null) card.setVisibility(View.VISIBLE);
            if (areaTitles[i] != null) areaTitles[i].setText(areas[i]);
            if (areaSubs[i] != null) areaSubs[i].setText(i == 0 && (kecList == null || kecList.isEmpty()) ? "Lihat di peta" : "Kecamatan • lihat peta");
        }
    }
}
