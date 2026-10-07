package com.carikostkita.ui.main.favorite;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.adapter.KostAdapter;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.ui.main.MainActivity;
import com.carikostkita.util.AuthPrompt;
import com.carikostkita.util.ErrorMessages;
import com.carikostkita.util.PageHeader;
import com.carikostkita.util.SessionManager;
import com.carikostkita.util.SkeletonHelper;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment implements KostAdapter.OnKostClickListener {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvFavorites;
    private ProgressBar pbLoading;
    private View layoutSkeleton;
    private View layoutEmpty;
    private View ivEmptyIllustration;
    private View frameEmptyIcon;
    private View layoutSuggestions;
    private com.carikostkita.ui.adapter.KostCarouselAdapter suggestionAdapter;
    private boolean suggestionsLoaded = false;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private TextView btnEmptyAction;
    private KostAdapter kostAdapter;
    private KostRepository kostRepository;
    private SessionManager sessionManager;
    private final List<Kost> currentList = new ArrayList<>();
    private boolean hasLoadedOnce = false;

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

        PageHeader.bind(view, "Kost Favorit", "Kost yang kamu simpan untuk dibandingkan");

        swipeRefresh = view.findViewById(R.id.swipe_refresh_fav);
        swipeRefresh.setColorSchemeResources(R.color.primary);
        rvFavorites = view.findViewById(R.id.rv_favorite_kost);
        pbLoading = view.findViewById(R.id.pb_fav_loading);
        layoutSkeleton = view.findViewById(R.id.skeleton_favorite);
        layoutEmpty = view.findViewById(R.id.layout_fav_empty);
        ivEmptyIcon = view.findViewById(R.id.iv_fav_empty_icon);
        tvEmptyTitle = view.findViewById(R.id.tv_fav_empty_title);
        tvEmptyDesc = view.findViewById(R.id.tv_fav_empty_desc);
        btnEmptyAction = view.findViewById(R.id.btn_fav_explore);
        ivEmptyIllustration = view.findViewById(R.id.iv_fav_empty_illustration);
        frameEmptyIcon = view.findViewById(R.id.frame_fav_empty_icon);
        layoutSuggestions = view.findViewById(R.id.layout_fav_suggestions);
        setupSuggestions(view);

        kostAdapter = new KostAdapter(this);
        rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavorites.setAdapter(kostAdapter);

        swipeRefresh.setOnRefreshListener(() -> loadFavorites(false));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites(!hasLoadedOnce);
    }

    private void loadFavorites(boolean showSkeleton) {
        if (!sessionManager.isLoggedIn()) {
            swipeRefresh.setRefreshing(false);
            if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
            showGuestState();
            return;
        }

        long startTime = SkeletonHelper.markStart();
        if (showSkeleton && !swipeRefresh.isRefreshing()) {
            if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
        }

        kostRepository.getFavorites(sessionManager.getUserUid(), new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                SkeletonHelper.complete(startTime, () -> {
                    if (!isAdded()) return;
                    hasLoadedOnce = true;
                    if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    currentList.clear();
                    if (data != null) currentList.addAll(data);
                    renderList();
                });
            }

            @Override
            public void onError(String message) {
                SkeletonHelper.complete(startTime, () -> {
                    if (!isAdded()) return;
                    if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    if (currentList.isEmpty()) {
                        showErrorState(message);
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void renderList() {
        kostAdapter.submitList(new ArrayList<>(currentList));
        View compare = getView() != null ? getView().findViewById(R.id.btn_fav_compare) : null;
        if (compare != null) {
            compare.setVisibility(currentList.size() >= 2 ? View.VISIBLE : View.GONE);
            compare.setOnClickListener(v -> pickToCompare());
        }
        if (currentList.isEmpty()) {
            showEmptyState();
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.VISIBLE);
        }
    }

    /** Pilih 2–3 kost favorit lalu buka tabel perbandingan. */
    private void pickToCompare() {
        String[] names = new String[currentList.size()];
        boolean[] checked = new boolean[currentList.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = currentList.get(i).getNamaKost() + " • " + currentList.get(i).getFormattedHarga();
            checked[i] = i < 2;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pilih 2–3 kost")
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Bandingkan", (d, w) -> {
                    ArrayList<String> ids = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) if (checked[i]) ids.add(currentList.get(i).getIdKost());
                    if (ids.size() < 2 || ids.size() > 3) {
                        Toast.makeText(requireContext(), "Pilih 2 sampai 3 kost", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startActivity(new Intent(requireContext(), com.carikostkita.ui.compare.CompareKostActivity.class)
                            .putStringArrayListExtra(com.carikostkita.ui.compare.CompareKostActivity.EXTRA_IDS, ids));
                })
                .show();
    }

    /** Ilustrasi untuk kondisi kosong/tamu, ikon kecil untuk kondisi error. */
    private void useIllustration(boolean illustration) {
        if (ivEmptyIllustration != null) ivEmptyIllustration.setVisibility(illustration ? View.VISIBLE : View.GONE);
        if (frameEmptyIcon != null) frameEmptyIcon.setVisibility(illustration ? View.GONE : View.VISIBLE);
        View tips = getView() != null ? getView().findViewById(R.id.card_fav_tips) : null;
        if (tips != null) tips.setVisibility(illustration ? View.VISIBLE : View.GONE);
        if (illustration) loadSuggestions();
        else if (layoutSuggestions != null) layoutSuggestions.setVisibility(View.GONE);
    }

    private void setupSuggestions(View root) {
        RecyclerView rv = root.findViewById(R.id.rv_fav_suggestions);
        if (rv == null) return;
        suggestionAdapter = new com.carikostkita.ui.adapter.KostCarouselAdapter(new com.carikostkita.ui.adapter.KostCarouselAdapter.OnCarouselKostClickListener() {
            @Override
            public void onKostClick(Kost kost) {
                FavoriteFragment.this.onKostClick(kost);
            }

            @Override
            public void onFavoriteToggle(Kost kost, int position) {
                if (!AuthPrompt.require(requireContext(), "Masuk untuk menyimpan kost favorit.")) return;
                kostRepository.toggleFavorite(sessionManager.getUserUid(), kost.getIdKost(), new DataCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isFavorite) {
                        if (!isAdded()) return;
                        suggestionsLoaded = false;
                        loadFavorites(false);
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(suggestionAdapter);
    }

    /** "Mungkin kamu suka": kost terdekat (bila lokasi ada) atau termurah, maksimal 6. */
    private void loadSuggestions() {
        if (suggestionAdapter == null || suggestionsLoaded) return;
        suggestionsLoaded = true;
        kostRepository.getAllActiveKost(sessionManager.getUserUid(), new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                if (!isAdded() || data == null || data.isEmpty()) return;
                List<Kost> list = new ArrayList<>();
                for (Kost k : data) if (!k.isFavorite() && k.isAvailable()) list.add(k);
                double lat = sessionManager.getUserSelectedLat();
                double lng = sessionManager.getUserSelectedLng();
                if (lat != 0 && lng != 0) {
                    java.util.Collections.sort(list, (a, b) -> Double.compare(
                            a.hasCoordinates() ? com.carikostkita.util.GeoUtil.distanceKm(lat, lng, a.getLatitude(), a.getLongitude()) : Double.MAX_VALUE,
                            b.hasCoordinates() ? com.carikostkita.util.GeoUtil.distanceKm(lat, lng, b.getLatitude(), b.getLongitude()) : Double.MAX_VALUE));
                } else {
                    java.util.Collections.sort(list, (a, b) -> Double.compare(a.getHarga(), b.getHarga()));
                }
                if (list.isEmpty()) return;
                suggestionAdapter.submitList(new ArrayList<>(list.subList(0, Math.min(6, list.size()))));
                if (layoutSuggestions != null) layoutSuggestions.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                suggestionsLoaded = false;
            }
        });
    }

    private void showEmptyState() {
        rvFavorites.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        useIllustration(true);
        if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.empty_favorite_title);
        if (tvEmptyDesc != null) tvEmptyDesc.setText(R.string.empty_favorite_desc);
        if (btnEmptyAction != null) {
            btnEmptyAction.setText("Jelajahi Kost");
            btnEmptyAction.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToSearch();
                }
            });
        }
    }

    private void showGuestState() {
        rvFavorites.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        useIllustration(true);
        if (tvEmptyTitle != null) tvEmptyTitle.setText("Simpan Kost Incaranmu");
        if (tvEmptyDesc != null) tvEmptyDesc.setText("Masuk untuk menyimpan kost favorit. Daftarnya tetap ada walau kamu ganti HP.");
        if (btnEmptyAction != null) {
            btnEmptyAction.setText("Masuk / Daftar");
            btnEmptyAction.setOnClickListener(v -> AuthPrompt.openLogin(requireContext()));
        }
    }

    private void showErrorState(String message) {
        rvFavorites.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        useIllustration(false);
        boolean offline = ErrorMessages.isOffline(message);
        if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(offline ? R.drawable.ic_error_circle : R.drawable.ic_warning);
        if (tvEmptyTitle != null) tvEmptyTitle.setText(offline ? "Kamu Sedang Offline" : "Gagal Memuat Favorit");
        if (tvEmptyDesc != null) tvEmptyDesc.setText(message);
        if (btnEmptyAction != null) {
            btnEmptyAction.setText("Coba Lagi");
            btnEmptyAction.setOnClickListener(v -> loadFavorites(true));
        }
    }

    @Override
    public void onKostClick(Kost kost) {
        Intent intent = new Intent(requireContext(), DetailKostActivity.class);
        intent.putExtra("kost_id", kost.getIdKost());
        startActivity(intent);
    }

    /** Hapus langsung dari daftar, dengan opsi Batalkan selama Snackbar tampil. */
    @Override
    public void onFavoriteToggle(Kost kost, int position) {
        int index = currentList.indexOf(kost);
        if (index < 0) return;
        currentList.remove(index);
        renderList();

        kostRepository.toggleFavorite(sessionManager.getUserUid(), kost.getIdKost(), new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isFavorite) {
                if (!isAdded() || getView() == null) return;
                Snackbar.make(getView(), "\"" + kost.getNamaKost() + "\" dihapus dari favorit", Snackbar.LENGTH_LONG)
                        .setAction("Batalkan", v -> restoreFavorite(kost, index))
                        .setAnchorView(requireActivity().findViewById(R.id.modern_bottom_nav))
                        .show();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                currentList.add(Math.min(index, currentList.size()), kost);
                renderList();
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void restoreFavorite(Kost kost, int index) {
        kostRepository.toggleFavorite(sessionManager.getUserUid(), kost.getIdKost(), new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isFavorite) {
                if (!isAdded()) return;
                kost.setFavorite(true);
                currentList.add(Math.min(index, currentList.size()), kost);
                renderList();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
