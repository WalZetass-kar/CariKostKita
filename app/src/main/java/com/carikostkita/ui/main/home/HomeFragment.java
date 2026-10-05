package com.carikostkita.ui.main.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.KostAdapter;
import com.carikostkita.ui.adapter.KostCarouselAdapter;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.SessionManager;
import com.carikostkita.util.SkeletonHelper;
import com.carikostkita.util.UserAvatarHelper;
import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.carikostkita.data.location.UserLocationManager;
import com.carikostkita.ui.location.LocationPickerBottomSheet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment implements KostAdapter.OnKostClickListener, KostCarouselAdapter.OnCarouselKostClickListener {

    private SwipeRefreshLayout swipeRefresh;
    private ImageView ivAvatar;
    private TextView tvGreeting;
    private View btnNotification;
    private View mockSearch;
    private View btnFilterTrigger;
    private TextView tvLocationLabel;
    private TextView tvDiscoveryAreaTitle;
    private TextView tvSectionNewTitle;
    private TextView tvArea1Title, tvArea1Sub;
    private TextView tvArea2Title, tvArea2Sub;
    private TextView tvArea3Title, tvArea3Sub;
    private TextView tvArea4Title, tvArea4Sub;
    private String area1 = "Bukit Raya";
    private String area2 = "Tangkerang";
    private String area3 = "Marpoyan";
    private String area4 = "Simpang Tiga";
    private UserLocationManager userLocationManager;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private TextView chipSemua, chipPutra, chipPutri, chipCampur;
    private View bannerKamarKosong;
    private TextView tvBannerText;

    private View layoutSkeleton;
    private View layoutHomeContent;
    private View layoutEmpty;
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
        ivAvatar = view.findViewById(R.id.iv_home_avatar);
        tvGreeting = view.findViewById(R.id.tv_home_greeting);
        btnNotification = view.findViewById(R.id.btn_home_notification);
        mockSearch = view.findViewById(R.id.layout_mock_search);
        btnFilterTrigger = view.findViewById(R.id.btn_home_filter_trigger);
        tvLocationLabel = view.findViewById(R.id.tv_home_location_label);
        tvDiscoveryAreaTitle = view.findViewById(R.id.tv_home_discovery_area_title);

        View layoutLocationChip = view.findViewById(R.id.layout_home_location_chip);
        if (layoutLocationChip != null) {
            layoutLocationChip.setOnClickListener(v -> showLocationPicker());
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
        layoutSectionTerjangkau = view.findViewById(R.id.layout_section_terjangkau);
        layoutSectionNew = view.findViewById(R.id.layout_section_new);
        tvSectionNewTitle = view.findViewById(R.id.tv_home_section_new_title);

        rvCarousel = view.findViewById(R.id.rv_home_carousel);
        rvKost = view.findViewById(R.id.rv_home_kost);

        View btnSeeAllTerjangkau = view.findViewById(R.id.btn_see_all_terjangkau);
        View btnSeeAllNew = view.findViewById(R.id.btn_see_all_new);

        // Discovery Area Cards Bindings
        View cardBukitRaya = view.findViewById(R.id.card_area_bukit_raya);
        View cardTangkerang = view.findViewById(R.id.card_area_tangkerang);
        View cardMarpoyan = view.findViewById(R.id.card_area_marpoyan);
        View cardSimpangTiga = view.findViewById(R.id.card_area_simpang_tiga);

        tvArea1Title = view.findViewById(R.id.tv_area_1_title);
        tvArea1Sub = view.findViewById(R.id.tv_area_1_sub);
        tvArea2Title = view.findViewById(R.id.tv_area_2_title);
        tvArea2Sub = view.findViewById(R.id.tv_area_2_sub);
        tvArea3Title = view.findViewById(R.id.tv_area_3_title);
        tvArea3Sub = view.findViewById(R.id.tv_area_3_sub);
        tvArea4Title = view.findViewById(R.id.tv_area_4_title);
        tvArea4Sub = view.findViewById(R.id.tv_area_4_sub);

        if (cardBukitRaya != null) cardBukitRaya.setOnClickListener(v -> openMapWithQuery(area1));
        if (cardTangkerang != null) cardTangkerang.setOnClickListener(v -> openMapWithQuery(area2));
        if (cardMarpoyan != null) cardMarpoyan.setOnClickListener(v -> openMapWithQuery(area3));
        if (cardSimpangTiga != null) cardSimpangTiga.setOnClickListener(v -> openMapWithQuery(area4));


        // Empty State Action Buttons
        View btnEmptyExpand = view.findViewById(R.id.btn_home_empty_expand);
        View btnEmptyMap = view.findViewById(R.id.btn_home_empty_map);
        if (btnEmptyExpand != null) btnEmptyExpand.setOnClickListener(v -> openMapWithQuery(""));
        if (btnEmptyMap != null) btnEmptyMap.setOnClickListener(v -> openMapWithQuery(""));

        // Header Greeting & Avatar
        refreshUserProfile();
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToProfile();
                }
            });
        }

        // Notification Button -> Navigate to Chat tab
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToChat();
                }
            });
            com.carikostkita.util.TouchFeedbackUtil.attachPress(btnNotification);
        }

        // Search bar & Filter Trigger -> Navigate to Map Search
        View.OnClickListener goToSearch = v -> {
            startActivity(new Intent(requireContext(), com.carikostkita.ui.map.MapSearchActivity.class));
        };
        if (mockSearch != null) mockSearch.setOnClickListener(goToSearch);
        if (btnFilterTrigger != null) {
            btnFilterTrigger.setOnClickListener(goToSearch);
            com.carikostkita.util.TouchFeedbackUtil.attachPress(btnFilterTrigger);
        }

        // Carousel 220dp cards setup
        carouselAdapter = new KostCarouselAdapter(this);
        rvCarousel.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCarousel.setAdapter(carouselAdapter);

        // Vertical List setup
        kostAdapter = new KostAdapter(this);
        rvKost.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvKost.setAdapter(kostAdapter);

        // Category Chips Click Listeners
        chipSemua.setOnClickListener(v -> filterByCategory("SEMUA"));
        chipPutra.setOnClickListener(v -> filterByCategory("PUTRA"));
        chipPutri.setOnClickListener(v -> filterByCategory("PUTRI"));
        chipCampur.setOnClickListener(v -> filterByCategory("CAMPUR"));

        // Informational Banner Click -> Filter Available Rooms
        if (bannerKamarKosong != null) {
            bannerKamarKosong.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToSearch();
                }
            });
        }

        if (btnSeeAllTerjangkau != null) btnSeeAllTerjangkau.setOnClickListener(goToSearch);
        if (btnSeeAllNew != null) btnSeeAllNew.setOnClickListener(goToSearch);

        swipeRefresh.setOnRefreshListener(this::loadData);

        loadData();

        // Location Detection & UX Flow (No forced permission popup on Home)
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
    }

    private void refreshUserProfile() {
        if (!isAdded()) return;
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            if (tvGreeting != null) tvGreeting.setText(sessionManager.getUserName());
            if (ivAvatar != null) UserAvatarHelper.loadAvatar(ivAvatar, sessionManager.getUserAvatar());
        } else {
            if (tvGreeting != null) tvGreeting.setText("Pencari Kost");
            if (ivAvatar != null) UserAvatarHelper.loadAvatar(ivAvatar, null);
        }
    }

    private void loadData() {
        long startTime = SkeletonHelper.markStart();

        if (!swipeRefresh.isRefreshing()) {
            if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.VISIBLE);
            if (layoutHomeContent != null) layoutHomeContent.setVisibility(View.GONE);
        }
        layoutEmpty.setVisibility(View.GONE);

        int userId = sessionManager.getUserId();
        kostRepository.getAllActiveKost(userId, new DataCallback<List<Kost>>() {
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

    private void onDataLoaded(List<Kost> allKost) {
        if (!isAdded()) return;

        if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
        if (layoutHomeContent != null) layoutHomeContent.setVisibility(View.VISIBLE);
        swipeRefresh.setRefreshing(false);

        masterKostList = (allKost != null) ? allKost : new ArrayList<>();

        if (masterKostList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            if (layoutSectionTerjangkau != null) layoutSectionTerjangkau.setVisibility(View.GONE);
            if (layoutSectionNew != null) layoutSectionNew.setVisibility(View.GONE);
            rvKost.setVisibility(View.GONE);
            chipSemua.setText("Semua");
            chipPutra.setText("Putra");
            chipPutri.setText("Putri");
            chipCampur.setText("Campur");
            return;
        }

        layoutEmpty.setVisibility(View.GONE);
        rvKost.setVisibility(View.VISIBLE);
        if (layoutSectionTerjangkau != null) layoutSectionTerjangkau.setVisibility(View.VISIBLE);
        if (layoutSectionNew != null) layoutSectionNew.setVisibility(View.VISIBLE);

        // 1. Calculate dynamic category counts
        int countPutri = 0, countPutra = 0, countCampur = 0, availableRooms = 0;
        for (Kost k : masterKostList) {
            if (k.getTipeKost() == TipeKost.PUTRI) countPutri++;
            else if (k.getTipeKost() == TipeKost.PUTRA) countPutra++;
            else if (k.getTipeKost() == TipeKost.CAMPUR) countCampur++;

            if (k.getStatus() == StatusKost.TERSEDIA) {
                availableRooms += Math.max(1, k.getKamarTersedia());
            }
        }

        chipSemua.setText("Semua " + masterKostList.size());
        chipPutra.setText("Putra " + countPutra);
        chipPutri.setText("Putri " + countPutri);
        chipCampur.setText("Campur " + countCampur);

        // Update Info Banner text
        if (tvBannerText != null) {
            if (availableRooms > 0) {
                tvBannerText.setText(availableRooms + " kamar kosong baru minggu ini");
            } else {
                tvBannerText.setText("Kamar kosong baru tersedia minggu ini");
            }
        }

        // 2. Section "Harga Terjangkau": Sorted by price ascending
        List<Kost> affordableList = new ArrayList<>(masterKostList);
        Collections.sort(affordableList, Comparator.comparingDouble(Kost::getHarga));
        int carouselLimit = Math.min(6, affordableList.size());
        carouselAdapter.submitList(affordableList.subList(0, carouselLimit));

        // 3. Section "Baru Ditambahkan": Filtered by active category, newest first
        applyCategoryFilter();
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

    private void applyCategoryFilter() {
        List<Kost> filtered = new ArrayList<>();
        for (Kost k : masterKostList) {
            if ("SEMUA".equals(selectedCategory)) {
                filtered.add(k);
            } else if ("PUTRA".equals(selectedCategory) && k.getTipeKost() == TipeKost.PUTRA) {
                filtered.add(k);
            } else if ("PUTRI".equals(selectedCategory) && k.getTipeKost() == TipeKost.PUTRI) {
                filtered.add(k);
            } else if ("CAMPUR".equals(selectedCategory) && k.getTipeKost() == TipeKost.CAMPUR) {
                filtered.add(k);
            }
        }
        Collections.sort(filtered, (a, b) -> {
            if (b.getCreatedAt() != null && a.getCreatedAt() != null) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
            return b.getId().compareTo(a.getId());
        });
        kostAdapter.submitList(filtered);
    }

    private void onErrorLoaded(String msg) {
        if (!isAdded()) return;
        if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
        if (layoutHomeContent != null) layoutHomeContent.setVisibility(View.VISIBLE);
        swipeRefresh.setRefreshing(false);
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onKostClick(Kost kost) {
        Intent intent = new Intent(requireContext(), DetailKostActivity.class);
        intent.putExtra("kost_id", kost.getIdKost());
        startActivity(intent);
    }

    @Override
    public void onFavoriteToggle(Kost kost, int position) {
        handleFavoriteToggle(kost);
    }

    private void handleFavoriteToggle(Kost kost) {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "Silakan login terlebih dahulu untuk menyimpan favorit", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void openMapWithQuery(String query) {
        Intent intent = new Intent(requireContext(), com.carikostkita.ui.map.MapSearchActivity.class);
        if (query != null && !query.isEmpty()) {
            intent.putExtra("search_query", query);
        }
        startActivity(intent);
    }

    private void openMapWithType(String tipe) {
        Intent intent = new Intent(requireContext(), com.carikostkita.ui.map.MapSearchActivity.class);
        if (tipe != null && !tipe.isEmpty()) {
            intent.putExtra("filter_tipe", tipe);
        }
        startActivity(intent);
    }

    private void showLocationPicker() {
        LocationPickerBottomSheet.show(requireContext(), (city, district, display) -> {
            updateLocationUI();
            updateDiscoveryArea(city);
        });
    }

    private void detectDeviceLocation() {
        if (tvLocationLabel != null) {
            tvLocationLabel.setText("Mendeteksi...");
        }
        userLocationManager.detectCurrentLocation(new UserLocationManager.LocationCallback() {
            @Override
            public void onLocationDetected(String city, String district, double lat, double lng, String display) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    updateLocationUI();
                    updateDiscoveryArea(city);
                });
            }

            @Override
            public void onLocationFailed(String fallbackMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    updateLocationUI();
                });
            }
        });
    }

    private void updateLocationUI() {
        if (!isAdded()) return;
        String loc = userLocationManager.getActiveLocationDisplay();
        if (tvLocationLabel != null) {
            if (loc != null && !loc.trim().isEmpty() && !loc.equalsIgnoreCase("Pilih Lokasi")) {
                tvLocationLabel.setText(loc);
            } else {
                tvLocationLabel.setText("Pilih Lokasi");
            }
        }
        if (tvSectionNewTitle != null) {
            if (loc != null && !loc.trim().isEmpty() && !loc.equalsIgnoreCase("Pilih Lokasi")) {
                tvSectionNewTitle.setText("Kost di sekitar " + loc);
            } else {
                tvSectionNewTitle.setText("Kost di sekitar Anda");
            }
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

        List<String> kecList = com.carikostkita.data.location.IndonesiaLocationData.getKecamatanList(activeCity);
        if (kecList != null && !kecList.isEmpty()) {
            if (kecList.size() > 0) area1 = kecList.get(0);
            if (kecList.size() > 1) area2 = kecList.get(1);
            if (kecList.size() > 2) area3 = kecList.get(2);
            if (kecList.size() > 3) area4 = kecList.get(3);
        } else {
            area1 = activeCity;
            area2 = "Pusat Kota";
            area3 = "Area Kampus";
            area4 = "Kecamatan Sekitar";
        }

        if (tvArea1Title != null) tvArea1Title.setText(area1);
        if (tvArea1Sub != null) tvArea1Sub.setText("Kecamatan");
        if (tvArea2Title != null) tvArea2Title.setText(area2);
        if (tvArea2Sub != null) tvArea2Sub.setText("Strategis");
        if (tvArea3Title != null) tvArea3Title.setText(area3);
        if (tvArea3Sub != null) tvArea3Sub.setText("Area Kost");
        if (tvArea4Title != null) tvArea4Title.setText(area4);
        if (tvArea4Sub != null) tvArea4Sub.setText("Akses Mudah");
    }
}
