package com.carikostkita.ui.main.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.KostAdapter;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.ui.map.MapSearchActivity;
import com.carikostkita.util.AuthPrompt;
import com.carikostkita.util.ErrorMessages;
import com.carikostkita.util.SessionManager;
import com.carikostkita.util.SkeletonHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

public class SearchFragment extends Fragment implements KostAdapter.OnKostClickListener {

    private static final String ARG_CATEGORY = "initial_category";
    private static final long SEARCH_DEBOUNCE_MS = 350;

    private EditText etSearch;
    private ImageButton btnClear;
    private View btnFilter;
    private TextView tvFilterBadgeCount;
    private RecyclerView rvResults;
    private ProgressBar pbLoading;
    private View layoutSkeleton;
    private View skeletonFull;
    private boolean hasLoadedOnce = false;
    private LinearLayout layoutEmpty;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private TextView btnEmptyAction;
    private TextView tvResultsCount;
    private View btnSortSelector;
    private TextView tvCurrentSort;

    private TextView chipAll, chipPutri, chipPutra, chipCampur, chipUnder1Jt, chipAvailable;

    private KostAdapter kostAdapter;
    private KostRepository kostRepository;
    private SessionManager sessionManager;
    private KostFilterCriteria currentCriteria;

    private String activeQuickChip = "ALL";
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final Runnable debouncedSearch = () -> performFilterOrSearch(false);
    /** Menandai hasil request lama agar tidak menimpa hasil pencarian yang lebih baru. */
    private int requestSerial = 0;
    private boolean hasResults = false;
    private String pendingQuickAction;
    private boolean pendingFocus;

    public static SearchFragment newInstance(String category) {
        SearchFragment fragment = new SearchFragment();
        if (category != null) {
            Bundle args = new Bundle();
            args.putString(ARG_CATEGORY, category);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        kostRepository = new KostRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentCriteria = new KostFilterCriteria();

        View band = view.findViewById(R.id.header_search_band);
        if (band != null) band.setClipToOutline(true);

        initViews(view);
        setupRecyclerView();
        setupSearchInput();
        setupQuickChips();
        setupSortSelector();

        if (getArguments() != null && getArguments().getString(ARG_CATEGORY) != null) {
            applyQuickChip(getArguments().getString(ARG_CATEGORY));
        } else if (pendingQuickAction != null || pendingFocus) {
            applyQuickAction(pendingQuickAction, pendingFocus);
        } else {
            performFilterOrSearch(true);
        }
    }

    @Override
    public void onDestroyView() {
        debounceHandler.removeCallbacks(debouncedSearch);
        super.onDestroyView();
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search_query);
        btnClear = view.findViewById(R.id.btn_search_clear);
        btnFilter = view.findViewById(R.id.btn_open_filter);
        tvFilterBadgeCount = view.findViewById(R.id.tv_filter_badge_count);
        rvResults = view.findViewById(R.id.rv_search_results);
        pbLoading = view.findViewById(R.id.pb_search_loading);
        layoutSkeleton = view.findViewById(R.id.layout_search_skeleton);
        skeletonFull = view.findViewById(R.id.skeleton_search_full);
        layoutEmpty = view.findViewById(R.id.layout_search_empty);
        ivEmptyIcon = view.findViewById(R.id.iv_search_empty_icon);
        tvEmptyTitle = view.findViewById(R.id.tv_search_empty_title);
        tvEmptyDesc = view.findViewById(R.id.tv_search_empty_desc);
        btnEmptyAction = view.findViewById(R.id.btn_reset_search);
        tvResultsCount = view.findViewById(R.id.tv_search_results_count);
        btnSortSelector = view.findViewById(R.id.btn_sort_selector);
        tvCurrentSort = view.findViewById(R.id.tv_current_sort);

        chipAll = view.findViewById(R.id.chip_search_all);
        chipPutri = view.findViewById(R.id.chip_search_putri);
        chipPutra = view.findViewById(R.id.chip_search_putra);
        chipCampur = view.findViewById(R.id.chip_search_campur);
        chipUnder1Jt = view.findViewById(R.id.chip_search_under_1jt);
        chipAvailable = view.findViewById(R.id.chip_search_available);

        btnFilter.setOnClickListener(v -> {
            FilterBottomSheetFragment bottomSheet = new FilterBottomSheetFragment();
            KostFilterCriteria forSheet = currentCriteria.copy();
            String q0 = etSearch.getText().toString().trim();
            forSheet.setKeyword(q0.isEmpty() ? null : q0);
            bottomSheet.setInitialCriteria(forSheet);
            bottomSheet.setOnFilterAppliedListener(criteria -> {
                String query = etSearch.getText().toString().trim();
                criteria.setKeyword(query.isEmpty() ? null : query);
                currentCriteria = criteria;
                syncSortLabel();
                // Chip cepat mengikuti tipe dari filter lengkap agar tidak saling bertentangan
                activeQuickChip = criteria.getTipeKost() != null ? criteria.getTipeKost().name()
                        : (criteria.getStatus() == StatusKost.TERSEDIA ? "AVAILABLE" : "ALL");
                updateQuickChipsVisual();
                updateFilterBadge();
                performFilterOrSearch(true);
            });
            bottomSheet.show(getChildFragmentManager(), "FilterBottomSheet");
        });

        View btnOpenMap = view.findViewById(R.id.btn_search_open_map);
        if (btnOpenMap != null) {
            btnOpenMap.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MapSearchActivity.class);
                String q = etSearch.getText().toString().trim();
                if (!q.isEmpty()) intent.putExtra("search_query", q);
                if (currentCriteria.getTipeKost() != null) intent.putExtra("filter_tipe", currentCriteria.getTipeKost().name());
                startActivity(intent);
            });
        }
    }

    private void setupRecyclerView() {
        kostAdapter = new KostAdapter(this);
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(kostAdapter);
        double lat = sessionManager.getUserSelectedLat();
        double lng = sessionManager.getUserSelectedLng();
        kostAdapter.setDistanceOrigin(lat, lng);
    }

    private void setupSearchInput() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                currentCriteria.setKeyword(s.toString().trim());
                // Tunggu pengguna selesai mengetik sebelum memanggil server
                debounceHandler.removeCallbacks(debouncedSearch);
                debounceHandler.postDelayed(debouncedSearch, SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            currentCriteria.setKeyword(null);
            debounceHandler.removeCallbacks(debouncedSearch);
            performFilterOrSearch(false);
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                debounceHandler.removeCallbacks(debouncedSearch);
                performFilterOrSearch(false);
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    private void setupQuickChips() {
        chipAll.setOnClickListener(v -> applyQuickChip("ALL"));
        chipPutri.setOnClickListener(v -> applyQuickChip("PUTRI"));
        chipPutra.setOnClickListener(v -> applyQuickChip("PUTRA"));
        chipCampur.setOnClickListener(v -> applyQuickChip("CAMPUR"));
        chipUnder1Jt.setOnClickListener(v -> applyQuickChip("UNDER_1JT"));
        chipAvailable.setOnClickListener(v -> applyQuickChip("AVAILABLE"));
        View chipDestination = requireView().findViewById(R.id.chip_search_destination);
        if (chipDestination != null) chipDestination.setOnClickListener(v -> askDestination());
    }

    public void applyCategoryFilter(TipeKost tipe) {
        applyQuickChip(tipe == null ? "ALL" : tipe.name());
    }

    /**
     * Dipanggil dari Beranda: "AVAILABLE", "SORT_CHEAPEST", tipe kost, atau null.
     * focusInput membuka keyboard di kolom pencarian.
     */
    public void applyQuickAction(String action, boolean focusInput) {
        if (etSearch == null) {
            pendingQuickAction = action;
            pendingFocus = focusInput;
            return;
        }
        pendingQuickAction = null;
        pendingFocus = false;
        if ("SORT_CHEAPEST".equals(action)) {
            currentCriteria.setSortBy("TERMURAH");
            tvCurrentSort.setText("Urutkan: Termurah");
            applyQuickChip("ALL");
        } else if (action != null) {
            applyQuickChip(action);
        } else {
            performFilterOrSearch(!hasResults);
        }
        if (focusInput) {
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void applyQuickChip(String type) {
        activeQuickChip = type;
        updateQuickChipsVisual();

        currentCriteria.setTipeKost(null);
        currentCriteria.setMaxHarga(null);
        currentCriteria.setMinHarga(null);
        currentCriteria.setStatus(null);

        switch (type) {
            case "PUTRI":
                currentCriteria.setTipeKost(TipeKost.PUTRI);
                break;
            case "PUTRA":
                currentCriteria.setTipeKost(TipeKost.PUTRA);
                break;
            case "CAMPUR":
                currentCriteria.setTipeKost(TipeKost.CAMPUR);
                break;
            case "UNDER_1JT":
                currentCriteria.setMaxHarga(1000000.0);
                break;
            case "AVAILABLE":
                currentCriteria.setStatus(StatusKost.TERSEDIA);
                break;
            case "ALL":
            default:
                break;
        }

        updateFilterBadge();
        performFilterOrSearch(true);
    }

    private void updateQuickChipsVisual() {
        setChipStyle(chipAll, "ALL".equals(activeQuickChip));
        setChipStyle(chipPutri, "PUTRI".equals(activeQuickChip));
        setChipStyle(chipPutra, "PUTRA".equals(activeQuickChip));
        setChipStyle(chipCampur, "CAMPUR".equals(activeQuickChip));
        setChipStyle(chipUnder1Jt, "UNDER_1JT".equals(activeQuickChip));
        setChipStyle(chipAvailable, "AVAILABLE".equals(activeQuickChip));
    }

    private void setChipStyle(TextView chip, boolean isSelected) {
        if (!isAdded() || chip == null) return;
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_badge_kategori_selected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary));
        } else {
            chip.setBackgroundResource(R.drawable.bg_badge_kategori_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private int countActiveFilters() {
        int activeCount = 0;
        if (currentCriteria.getTipeKost() != null) activeCount++;
        if ((currentCriteria.getMinHarga() != null && currentCriteria.getMinHarga() > 0)
                || (currentCriteria.getMaxHarga() != null && currentCriteria.getMaxHarga() > 0)) activeCount++;
        if (currentCriteria.getFasilitasIds() != null && !currentCriteria.getFasilitasIds().isEmpty()) activeCount++;
        if (currentCriteria.getStatus() != null) activeCount++;
        if (currentCriteria.getMaxDistanceKm() != null) activeCount++;
        if (currentCriteria.getKecamatan() != null) activeCount++;
        return activeCount;
    }

    /** Cari kost terdekat dari tempat tujuan (kampus, kantor) lewat geocoding OpenStreetMap. */
    private void askDestination() {
        com.carikostkita.util.AppDialogHelper.showInput(requireContext(), "Dekat dengan…",
                "Tulis nama kampus, kantor, atau tempat yang kamu tuju setiap hari.",
                "Contoh: Universitas Riau", currentCriteria.getOriginLabel(), "Cari",
                place -> {
                    if (place.trim().length() < 3) return;
                    com.carikostkita.data.location.NominatimGeocodingService.getInstance().searchPlaces(place.trim(),
                            new com.carikostkita.data.location.NominatimGeocodingService.SearchPlacesCallback() {
                                @Override
                                public void onSuccess(List<com.carikostkita.data.location.NominatimPlace> places) {
                                    if (!isAdded()) return;
                                    if (places == null || places.isEmpty()) {
                                        Toast.makeText(requireContext(), "Tempat tidak ditemukan. Coba nama yang lebih lengkap.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    String[] names = new String[Math.min(5, places.size())];
                                    for (int i = 0; i < names.length; i++) names[i] = places.get(i).getDisplayName();
                                    new MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("Pilih lokasi tujuan")
                                            .setItems(names, (d, which) -> {
                                                com.carikostkita.data.location.NominatimPlace p = places.get(which);
                                                String label = place.trim();
                                                currentCriteria.setOrigin(p.getLatitude(), p.getLongitude(), label);
                                                currentCriteria.setSortBy("TERDEKAT");
                                                tvCurrentSort.setText("Urutkan: Terdekat dari " + label);
                                                kostAdapter.setDistanceOrigin(p.getLatitude(), p.getLongitude());
                                                updateFilterBadge();
                                                performFilterOrSearch(true);
                                            })
                                            .show();
                                }

                                @Override
                                public void onError(String message) {
                                    if (isAdded()) Toast.makeText(requireContext(), "Pencarian tempat gagal. Periksa koneksi.", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    // ===== Riwayat pencarian (disimpan di perangkat, maks 8) =====
    private static final String PREF_HISTORY = "search_history";

    private List<String> loadHistory() {
        String raw = requireContext().getSharedPreferences("search_prefs", android.content.Context.MODE_PRIVATE).getString(PREF_HISTORY, "");
        List<String> list = new java.util.ArrayList<>();
        for (String s : raw.split("\n")) if (!s.trim().isEmpty()) list.add(s.trim());
        return list;
    }

    private void saveHistory(String keyword) {
        if (keyword == null || keyword.trim().length() < 3) return;
        List<String> list = loadHistory();
        list.remove(keyword.trim());
        list.add(0, keyword.trim());
        while (list.size() > 8) list.remove(list.size() - 1);
        requireContext().getSharedPreferences("search_prefs", android.content.Context.MODE_PRIVATE).edit()
                .putString(PREF_HISTORY, android.text.TextUtils.join("\n", list)).apply();
    }

    private void renderHistory() {
        View root = getView();
        if (root == null) return;
        com.google.android.material.chip.ChipGroup group = root.findViewById(R.id.cg_search_history);
        View container = root.findViewById(R.id.hsv_search_history);
        if (group == null || container == null) return;
        group.removeAllViews();
        boolean show = etSearch.getText().toString().trim().isEmpty();
        if (show) for (String h : loadHistory()) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
            chip.setText(h);
            chip.setChipIconResource(R.drawable.ic_clock);
            chip.setChipIconTint(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_muted)));
            chip.setOnClickListener(v -> {
                etSearch.setText(h);
                etSearch.setSelection(h.length());
            });
            group.addView(chip);
        }
        container.setVisibility(group.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void syncSortLabel() {
        String sort = currentCriteria.getSortBy();
        String label = "TERMURAH".equals(sort) ? "Termurah" : "TERMAHAL".equals(sort) ? "Termahal"
                : "TERDEKAT".equals(sort) ? "Terdekat" : "Terbaru";
        if (tvCurrentSort != null) tvCurrentSort.setText("Urutkan: " + label);
    }

    /** Chip ringkasan filter aktif; ketuk ikon X untuk menghapus satu filter. */
    private void renderActiveFilterChips() {
        View root = getView();
        if (root == null) return;
        com.google.android.material.chip.ChipGroup group = root.findViewById(R.id.cg_search_active_filters);
        View container = root.findViewById(R.id.hsv_search_active_filters);
        if (group == null || container == null) return;
        group.removeAllViews();

        if (currentCriteria.getMinHarga() != null || currentCriteria.getMaxHarga() != null) {
            String min = currentCriteria.getMinHarga() != null ? com.carikostkita.util.FormatUtil.formatRupiah(currentCriteria.getMinHarga()) : "Rp 0";
            String max = currentCriteria.getMaxHarga() != null ? com.carikostkita.util.FormatUtil.formatRupiah(currentCriteria.getMaxHarga()) : "∞";
            addActiveChip(group, min + " – " + max, () -> { currentCriteria.setMinHarga(null); currentCriteria.setMaxHarga(null); });
        }
        if (currentCriteria.getMaxDistanceKm() != null) {
            addActiveChip(group, "≤ " + currentCriteria.getMaxDistanceKm().intValue() + " km", () -> currentCriteria.setMaxDistanceKm(null));
        }
        if (currentCriteria.getOriginLabel() != null) {
            addActiveChip(group, "Dekat: " + currentCriteria.getOriginLabel(), () -> {
                currentCriteria.setOrigin(null, null, null);
                currentCriteria.setSortBy("TERBARU");
                tvCurrentSort.setText("Urutkan: Terbaru");
                kostAdapter.setDistanceOrigin(sessionManager.getUserSelectedLat(), sessionManager.getUserSelectedLng());
            });
        }
        if (currentCriteria.getKecamatan() != null) {
            addActiveChip(group, currentCriteria.getKecamatan(), () -> currentCriteria.setKecamatan(null));
        }
        if (currentCriteria.getFasilitasIds() != null) {
            for (Integer id : new java.util.ArrayList<>(currentCriteria.getFasilitasIds())) {
                String name = com.carikostkita.data.model.Fasilitas.nameForId(id);
                if (name == null) continue;
                addActiveChip(group, name, () -> currentCriteria.getFasilitasIds().remove(id));
            }
        }
        container.setVisibility(group.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void addActiveChip(com.google.android.material.chip.ChipGroup group, String label, Runnable onRemove) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
        chip.setText(label);
        chip.setCloseIconVisible(true);
        chip.setCloseIconContentDescription("Hapus filter " + label);
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_soft)));
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        chip.setCloseIconTint(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
        chip.setOnCloseIconClickListener(v -> {
            onRemove.run();
            updateFilterBadge();
            performFilterOrSearch(true);
        });
        group.addView(chip);
    }

    private void updateFilterBadge() {
        renderActiveFilterChips();
        if (tvFilterBadgeCount == null) return;
        int activeCount = countActiveFilters();
        if (activeCount > 0) {
            tvFilterBadgeCount.setText(String.valueOf(activeCount));
            tvFilterBadgeCount.setVisibility(View.VISIBLE);
        } else {
            tvFilterBadgeCount.setVisibility(View.GONE);
        }
    }

    private void setupSortSelector() {
        btnSortSelector.setOnClickListener(v -> {
            boolean hasLocation = sessionManager.getUserSelectedLat() != 0 && sessionManager.getUserSelectedLng() != 0;
            String[] options = hasLocation
                    ? new String[]{"Terbaru", "Harga Termurah", "Harga Termahal", "Terdekat"}
                    : new String[]{"Terbaru", "Harga Termurah", "Harga Termahal"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Urutkan Berdasarkan")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            currentCriteria.setSortBy("TERBARU");
                            tvCurrentSort.setText("Urutkan: Terbaru");
                        } else if (which == 1) {
                            currentCriteria.setSortBy("TERMURAH");
                            tvCurrentSort.setText("Urutkan: Termurah");
                        } else if (which == 2) {
                            currentCriteria.setSortBy("TERMAHAL");
                            tvCurrentSort.setText("Urutkan: Termahal");
                        } else {
                            currentCriteria.setSortBy("TERDEKAT");
                            tvCurrentSort.setText("Urutkan: Terdekat");
                        }
                        performFilterOrSearch(true);
                    })
                    .show();
        });
    }

    private void resetAllFilters() {
        etSearch.setText("");
        debounceHandler.removeCallbacks(debouncedSearch);
        currentCriteria = new KostFilterCriteria();
        activeQuickChip = "ALL";
        updateQuickChipsVisual();
        updateFilterBadge();
        tvCurrentSort.setText("Urutkan: Terbaru");
        performFilterOrSearch(true);
    }

    /**
     * @param showSkeleton true untuk perubahan filter besar; saat mengetik hasil lama
     *                     tetap tampil dengan indikator kecil agar layar tidak berkedip.
     */
    private void performFilterOrSearch(boolean showSkeleton) {
        if (!isAdded()) return;
        final int serial = ++requestSerial;
        long startTime = SkeletonHelper.markStart();

        layoutEmpty.setVisibility(View.GONE);
        if (!hasLoadedOnce && skeletonFull != null) {
            // Pemuatan pertama: skeleton satu halaman penuh
            skeletonFull.setVisibility(View.VISIBLE);
        } else if (showSkeleton || !hasResults) {
            if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.VISIBLE);
            rvResults.setVisibility(View.GONE);
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
        } else if (pbLoading != null) {
            pbLoading.setVisibility(View.VISIBLE);
        }

        kostRepository.filterKost(currentCriteria, sessionManager.getUserUid(), new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                SkeletonHelper.complete(startTime, () -> {
                    if (!isAdded() || serial != requestSerial) return;
                    hasLoadedOnce = true;
                    if (skeletonFull != null) skeletonFull.setVisibility(View.GONE);
                    if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    kostAdapter.submitList(data);

                    int count = (data != null) ? data.size() : 0;
                    hasResults = count > 0;
                    String kw = currentCriteria.getKeyword();
                    if (kw != null && kw.trim().length() >= 3) {
                        if (count > 0) saveHistory(kw);
                        new com.carikostkita.data.repository.AnalyticsRepository(requireContext())
                                .log(com.carikostkita.data.repository.AnalyticsRepository.SEARCH, null);
                    }
                    renderHistory();
                    tvResultsCount.setText(count == 0 ? "Tidak ada hasil" : count + " kost ditemukan");

                    if (count == 0) {
                        showNoResults();
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvResults.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String message) {
                SkeletonHelper.complete(startTime, () -> {
                    if (!isAdded() || serial != requestSerial) return;
                    hasLoadedOnce = true;
                    if (skeletonFull != null) skeletonFull.setVisibility(View.GONE);
                    if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    if (hasResults) {
                        rvResults.setVisibility(View.VISIBLE);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        showError(message);
                    }
                });
            }
        });
    }

    private void showNoResults() {
        rvResults.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(R.drawable.ic_nav_search);
        String keyword = etSearch.getText().toString().trim();
        int filters = countActiveFilters();
        if (tvEmptyTitle != null) {
            tvEmptyTitle.setText(keyword.isEmpty() ? "Belum ada kost yang cocok" : "\"" + keyword + "\" tidak ditemukan");
        }
        if (tvEmptyDesc != null) {
            tvEmptyDesc.setText(filters > 0
                    ? "Ada " + filters + " filter aktif. Coba hapus beberapa filter atau cari dengan kata lain."
                    : "Coba nama jalan, kelurahan, atau kecamatan. Kamu juga bisa mencari lewat peta.");
        }
        if (btnEmptyAction != null) {
            boolean canReset = filters > 0 || !keyword.isEmpty();
            btnEmptyAction.setText(canReset ? "Hapus Semua Filter" : "Cari di Peta");
            btnEmptyAction.setOnClickListener(v -> {
                if (canReset) resetAllFilters();
                else startActivity(new Intent(requireContext(), MapSearchActivity.class));
            });
        }
    }

    private void showError(String message) {
        rvResults.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        boolean offline = ErrorMessages.isOffline(message);
        if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(offline ? R.drawable.ic_error_circle : R.drawable.ic_warning);
        if (tvEmptyTitle != null) tvEmptyTitle.setText(offline ? "Kamu sedang offline" : "Gagal memuat hasil");
        if (tvEmptyDesc != null) tvEmptyDesc.setText(message);
        if (btnEmptyAction != null) {
            btnEmptyAction.setText("Coba Lagi");
            btnEmptyAction.setOnClickListener(v -> performFilterOrSearch(true));
        }
        tvResultsCount.setText("");
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
                if (!isAdded()) return;
                kost.setFavorite(isFavorite);
                kostAdapter.notifyItemChanged(position);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
