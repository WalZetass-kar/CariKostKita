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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.KostAdapter;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.util.SessionManager;
import java.util.List;

public class SearchFragment extends Fragment implements KostAdapter.OnKostClickListener {

    private EditText etSearch;
    private ImageButton btnClear;
    private View btnFilter;
    private RecyclerView rvResults;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmpty;
    private KostAdapter kostAdapter;
    private KostRepository kostRepository;
    private SessionManager sessionManager;
    private KostFilterCriteria currentCriteria;

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

        etSearch = view.findViewById(R.id.et_search_query);
        btnClear = view.findViewById(R.id.btn_search_clear);
        btnFilter = view.findViewById(R.id.btn_open_filter);
        rvResults = view.findViewById(R.id.rv_search_results);
        pbLoading = view.findViewById(R.id.pb_search_loading);
        layoutEmpty = view.findViewById(R.id.layout_search_empty);
        View btnReset = view.findViewById(R.id.btn_reset_search);

        kostAdapter = new KostAdapter(this);
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvResults.setAdapter(kostAdapter);

        // Search text watcher
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

        // Filter bottom sheet trigger
        btnFilter.setOnClickListener(v -> {
            FilterBottomSheetFragment bottomSheet = new FilterBottomSheetFragment();
            bottomSheet.setOnFilterAppliedListener(criteria -> {
                // Preserve current text query
                String query = etSearch.getText().toString().trim();
                criteria.setKeyword(query.isEmpty() ? null : query);
                currentCriteria = criteria;
                performFilterOrSearch();
            });
            bottomSheet.show(getChildFragmentManager(), "FilterBottomSheet");
        });

        btnReset.setOnClickListener(v -> {
            etSearch.setText("");
            currentCriteria = new KostFilterCriteria();
            performFilterOrSearch();
        });

        performFilterOrSearch();
    }

    private void performFilterOrSearch() {
        pbLoading.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        int userId = sessionManager.getUserId();
        kostRepository.filterKost(currentCriteria, userId, new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                pbLoading.setVisibility(View.GONE);
                kostAdapter.submitList(data);
                if (data == null || data.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvResults.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvResults.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                pbLoading.setVisibility(View.GONE);
                if (getContext() != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
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
                kost.setFavorite(isFavorite);
                kostAdapter.notifyItemChanged(position);
                String msg = isFavorite ? "Ditambahkan ke Favorit" : "Dihapus dari Favorit";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
