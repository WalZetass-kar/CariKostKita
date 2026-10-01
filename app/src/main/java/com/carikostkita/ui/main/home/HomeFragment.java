package com.carikostkita.ui.main.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.KostAdapter;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.SessionManager;
import com.google.android.material.chip.ChipGroup;
import java.util.List;

public class HomeFragment extends Fragment implements KostAdapter.OnKostClickListener {

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvGreeting;
    private ChipGroup chipGroup;
    private RecyclerView rvKost;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmpty;
    private KostAdapter kostAdapter;
    private KostRepository kostRepository;
    private SessionManager sessionManager;
    private TipeKost selectedTipe = null;

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

        swipeRefresh = view.findViewById(R.id.swipe_refresh_home);
        tvGreeting = view.findViewById(R.id.tv_home_greeting);
        View mockSearch = view.findViewById(R.id.layout_mock_search);
        chipGroup = view.findViewById(R.id.chip_group_home);
        rvKost = view.findViewById(R.id.rv_home_kost);
        pbLoading = view.findViewById(R.id.pb_home_loading);
        layoutEmpty = view.findViewById(R.id.layout_home_empty);

        // Greeting
        if (sessionManager.isLoggedIn()) {
            tvGreeting.setText("Halo, " + sessionManager.getUserName() + "!");
        } else {
            tvGreeting.setText("Halo, Pencari Kost!");
        }

        // Mock search navigates to Search tab
        mockSearch.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToSearch();
            }
        });

        // Setup RecyclerView
        kostAdapter = new KostAdapter(this);
        rvKost.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvKost.setAdapter(kostAdapter);

        // Chip selection
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedTipe = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_putri) {
                    selectedTipe = TipeKost.PUTRI;
                } else if (id == R.id.chip_putra) {
                    selectedTipe = TipeKost.PUTRA;
                } else if (id == R.id.chip_campur) {
                    selectedTipe = TipeKost.CAMPUR;
                } else {
                    selectedTipe = null;
                }
            }
            loadData();
        });

        // Quick Category Cards
        View cardPutri = view.findViewById(R.id.card_quick_putri);
        View cardPutra = view.findViewById(R.id.card_quick_putra);
        View cardCampur = view.findViewById(R.id.card_quick_campur);

        if (cardPutri != null) {
            cardPutri.setOnClickListener(v -> chipGroup.check(R.id.chip_putri));
        }
        if (cardPutra != null) {
            cardPutra.setOnClickListener(v -> chipGroup.check(R.id.chip_putra));
        }
        if (cardCampur != null) {
            cardCampur.setOnClickListener(v -> chipGroup.check(R.id.chip_campur));
        }

        swipeRefresh.setOnRefreshListener(this::loadData);

        loadData();
    }

    private void loadData() {
        if (!swipeRefresh.isRefreshing()) {
            pbLoading.setVisibility(View.VISIBLE);
        }
        layoutEmpty.setVisibility(View.GONE);

        int userId = sessionManager.getUserId();

        if (selectedTipe == null) {
            kostRepository.getAllActiveKost(userId, new DataCallback<List<Kost>>() {
                @Override
                public void onSuccess(List<Kost> data) {
                    onDataLoaded(data);
                }

                @Override
                public void onError(String message) {
                    onErrorLoaded(message);
                }
            });
        } else {
            KostFilterCriteria criteria = new KostFilterCriteria();
            criteria.setTipeKost(selectedTipe);
            kostRepository.filterKost(criteria, userId, new DataCallback<List<Kost>>() {
                @Override
                public void onSuccess(List<Kost> data) {
                    onDataLoaded(data);
                }

                @Override
                public void onError(String message) {
                    onErrorLoaded(message);
                }
            });
        }
    }

    private void onDataLoaded(List<Kost> data) {
        pbLoading.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        kostAdapter.submitList(data);
        if (data == null || data.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvKost.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvKost.setVisibility(View.VISIBLE);
        }
    }

    private void onErrorLoaded(String msg) {
        pbLoading.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
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
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "Silakan login terlebih dahulu untuk menyimpan favorit", Toast.LENGTH_SHORT).show();
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
