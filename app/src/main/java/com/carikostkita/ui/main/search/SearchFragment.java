package com.carikostkita.ui.main.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.carikostkita.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

public class SearchFragment extends Fragment implements KostAdapter.OnKostClickListener {

    private EditText etSearch;
    private ImageButton btnClear;
    private View btnFilter;
    private RecyclerView rvResults;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmpty;
    private TextView tvResultsCount;
    private View btnSortSelector;
    private TextView tvCurrentSort;

    private TextView chipAll, chipPutri, chipPutra, chipCampur, chipUnder1Jt, chipAvailable;

    private KostAdapter kostAdapter;
    private KostRepository kostRepository;
    private SessionManager sessionManager;
    private KostFilterCriteria currentCriteria;

    private String activeQuickChip = "ALL";

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

        initViews(view);
        setupRecyclerView();
        setupSearchInput();
        setupQuickChips();
        setupSortSelector();

        performFilterOrSearch();
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search_query);
        btnClear = view.findViewById(R.id.btn_search_clear);
        btnFilter = view.findViewById(R.id.btn_open_filter);
        rvResults = view.findViewById(R.id.rv_search_results);
        pbLoading = view.findViewById(R.id.pb_search_loading);
        layoutEmpty = view.findViewById(R.id.layout_search_empty);
        tvResultsCount = view.findViewById(R.id.tv_search_results_count);
        btnSortSelector = view.findViewById(R.id.btn_sort_selector);
        tvCurrentSort = view.findViewById(R.id.tv_current_sort);

        chipAll = view.findViewById(R.id.chip_search_all);
        chipPutri = view.findViewById(R.id.chip_search_putri);
        chipPutra = view.findViewById(R.id.chip_search_putra);
        chipCampur = view.findViewById(R.id.chip_search_campur);
        chipUnder1Jt = view.findViewById(R.id.chip_search_under_1jt);
        chipAvailable = view.findViewById(R.id.chip_search_available);

        View btnReset = view.findViewById(R.id.btn_reset_search);
        btnReset.setOnClickListener(v -> resetAllFilters());

        btnFilter.setOnClickListener(v -> {
            FilterBottomSheetFragment bottomSheet = new FilterBottomSheetFragment();
            bottomSheet.setOnFilterAppliedListener(criteria -> {
                String query = etSearch.getText().toString().trim();
                criteria.setKeyword(query.isEmpty() ? null : query);
                criteria.setSortBy(currentCriteria.getSortBy());
                currentCriteria = criteria;
                performFilterOrSearch();
            });
            bottomSheet.show(getChildFragmentManager(), "FilterBottomSheet");
        });
    }

    private void setupRecyclerView() {
        kostAdapter = new KostAdapter(this);
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(kostAdapter);
    }

    private void setupSearchInput() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                currentCriteria.setKeyword(s.toString().trim());
                performFilterOrSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            currentCriteria.setKeyword(null);
            performFilterOrSearch();
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performFilterOrSearch();
                return true;
            }
            return false;
        });
    }

    private void setupQuickChips() {
        chipAll.setOnClickListener(v -> applyQuickChip("ALL"));
        chipPutri.setOnClickListener(v -> applyQuickChip("PUTRI"));
        chipPutra.setOnClickListener(v -> applyQuickChip("PUTRA"));
        chipCampur.setOnClickListener(v -> applyQuickChip("CAMPUR"));
        chipUnder1Jt.setOnClickListener(v -> applyQuickChip("UNDER_1JT"));
        chipAvailable.setOnClickListener(v -> applyQuickChip("AVAILABLE"));
    }

    private void applyQuickChip(String type) {
        activeQuickChip = type;
        updateQuickChipsVisual();

        // Reset quick criteria overrides
        currentCriteria.setTipeKost(null);
        currentCriteria.setMaxHarga(null);
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

        performFilterOrSearch();
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
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_badge_kategori_selected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary));
        } else {
            chip.setBackgroundResource(R.drawable.bg_badge_kategori_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void setupSortSelector() {
        btnSortSelector.setOnClickListener(v -> {
            String[] options = {"Terbaru", "Harga Termurah", "Harga Termahal"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Urutkan Berdasarkan")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            currentCriteria.setSortBy("TERBARU");
                            tvCurrentSort.setText("Urutkan: Terbaru");
                        } else if (which == 1) {
                            currentCriteria.setSortBy("TERMURAH");
                            tvCurrentSort.setText("Urutkan: Termurah");
                        } else {
                            currentCriteria.setSortBy("TERMAHAL");
                            tvCurrentSort.setText("Urutkan: Termahal");
                        }
                        performFilterOrSearch();
                    })
                    .show();
        });
    }

    private void resetAllFilters() {
        etSearch.setText("");
        currentCriteria = new KostFilterCriteria();
        activeQuickChip = "ALL";
        updateQuickChipsVisual();
        tvCurrentSort.setText("Urutkan: Terbaru");
        performFilterOrSearch();
    }

    private void performFilterOrSearch() {
        pbLoading.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        int userId = sessionManager.getUserId();
        kostRepository.filterKost(currentCriteria, userId, new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                if (!isAdded()) return;
                pbLoading.setVisibility(View.GONE);
                kostAdapter.submitList(data);

                int count = (data != null) ? data.size() : 0;
                tvResultsCount.setText("Ditemukan " + count + " properti kost");

                if (count == 0) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvResults.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvResults.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onKostClick(Kost kost) {
        Intent intent = new Intent(requireContext(), DetailKostActivity.class);
        intent.putExtra("kost_id", kost.getIdKost());
        startActivity(intent);
    }

    @Override
    public void onFavoriteToggle(Kost kost, int position) {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        kostRepository.toggleFavorite(sessionManager.getUserId(), kost.getIdKost(), new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isFavorite) {
                if (!isAdded()) return;
                kost.setFavorite(isFavorite);
                kostAdapter.notifyItemChanged(position);
                String msg = isFavorite ? "Ditambahkan ke Favorit" : "Dihapus dari Favorit";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
