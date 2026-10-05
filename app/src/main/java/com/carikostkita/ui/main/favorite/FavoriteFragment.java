package com.carikostkita.ui.main.favorite;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.carikostkita.R;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.KostAdapter;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.SessionManager;
import java.util.List;

public class FavoriteFragment extends Fragment implements KostAdapter.OnKostClickListener {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvFavorites;
    private ProgressBar pbLoading;
    private View layoutSkeleton;
    private LinearLayout layoutEmpty;
    private KostAdapter kostAdapter;
    private KostRepository kostRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        kostRepository = new KostRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        swipeRefresh = view.findViewById(R.id.swipe_refresh_fav);
        rvFavorites = view.findViewById(R.id.rv_favorite_kost);
        pbLoading = view.findViewById(R.id.pb_fav_loading);
        layoutSkeleton = view.findViewById(R.id.skeleton_favorite);
        layoutEmpty = view.findViewById(R.id.layout_fav_empty);
        View btnExplore = view.findViewById(R.id.btn_fav_explore);

        kostAdapter = new KostAdapter(this);
        rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavorites.setAdapter(kostAdapter);

        swipeRefresh.setOnRefreshListener(this::loadFavorites);

        btnExplore.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToSearch();
            }
        });

        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        String uid = sessionManager.getUserUid();
        if (uid == null || uid.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            return;
        }

        long startTime = com.carikostkita.util.SkeletonHelper.markStart();
        if (!swipeRefresh.isRefreshing()) {
            if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.VISIBLE);
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.GONE);
        }
        layoutEmpty.setVisibility(View.GONE);

        kostRepository.getFavorites(uid, new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                com.carikostkita.util.SkeletonHelper.complete(startTime, () -> {
                    if (!isAdded()) return;
                    if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    kostAdapter.submitList(data);
                    if (data == null || data.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvFavorites.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvFavorites.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String message) {
                com.carikostkita.util.SkeletonHelper.complete(startTime, () -> {
                    if (!isAdded()) return;
                    if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
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
        kostRepository.toggleFavorite(sessionManager.getUserUid(), kost.getIdKost(), new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isFavorite) {
                loadFavorites();
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
