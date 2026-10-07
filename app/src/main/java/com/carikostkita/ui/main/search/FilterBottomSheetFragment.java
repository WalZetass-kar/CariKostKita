package com.carikostkita.ui.main.search;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.carikostkita.R;
import com.carikostkita.data.location.IndonesiaLocationData;
import com.carikostkita.data.location.UserLocationManager;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.TipeKost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.RangeSlider;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter lengkap tab Cari: urutan, tipe, rentang harga, ketersediaan, jarak, area, dan fasilitas.
 * Jumlah hasil dihitung ulang (dengan jeda) setiap kali pilihan berubah.
 */
public class FilterBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnFilterAppliedListener {
        void onFilterApplied(KostFilterCriteria criteria);
    }

    private static final float PRICE_MAX = 5_000_000f;
    private static final String[] SORT_KEYS = {"TERBARU", "TERMURAH", "TERMAHAL", "TERDEKAT"};
    private static final String[] SORT_LABELS = {"Terbaru", "Termurah", "Termahal", "Terdekat"};
    private static final double[] DISTANCES = {0, 1, 3, 5, 10};
    private static final String[] DISTANCE_LABELS = {"Semua", "≤ 1 km", "≤ 3 km", "≤ 5 km", "≤ 10 km"};

    private OnFilterAppliedListener listener;
    private KostFilterCriteria initial = new KostFilterCriteria();

    private ChipGroup cgSort, cgTipe, cgDistance, cgFasilitas;
    private RangeSlider rsPrice;
    private TextView tvPriceLabel;
    private MaterialSwitch swAvailable;
    private AutoCompleteTextView actArea;
    private MaterialButton btnApply;
    private boolean hasLocation;

    private KostRepository kostRepository;
    private final Handler countHandler = new Handler(Looper.getMainLooper());
    private final Runnable countRunnable = this::refreshResultCount;
    private int countSerial = 0;

    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    /** Kriteria aktif saat ini, agar pilihan tidak hilang ketika sheet dibuka ulang. */
    public void setInitialCriteria(KostFilterCriteria criteria) {
        if (criteria != null) this.initial = criteria.copy();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            View sheet = ((BottomSheetDialog) dialog).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.getLayoutParams().height = (int) (getResources().getDisplayMetrics().heightPixels * 0.9f);
                sheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        kostRepository = new KostRepository(requireContext());
        SessionManager session = new SessionManager(requireContext());
        hasLocation = (session.getUserSelectedLat() != 0 && session.getUserSelectedLng() != 0) || initial.getOriginLat() != null;

        cgSort = view.findViewById(R.id.cg_filter_sort);
        cgTipe = view.findViewById(R.id.cg_filter_tipe);
        cgDistance = view.findViewById(R.id.cg_filter_distance);
        cgFasilitas = view.findViewById(R.id.cg_filter_fasilitas);
        rsPrice = view.findViewById(R.id.rs_filter_price);
        tvPriceLabel = view.findViewById(R.id.tv_filter_price_label);
        swAvailable = view.findViewById(R.id.sw_filter_available);
        actArea = view.findViewById(R.id.act_filter_area);
        btnApply = view.findViewById(R.id.btn_filter_apply);
        TextView tvDistanceHint = view.findViewById(R.id.tv_filter_distance_hint);

        for (int i = 0; i < SORT_KEYS.length; i++) {
            Chip chip = addChip(cgSort, SORT_LABELS[i], i);
            if ("TERDEKAT".equals(SORT_KEYS[i]) && !hasLocation) chip.setEnabled(false);
        }
        addChip(cgTipe, "Semua", -1);
        for (TipeKost t : new TipeKost[]{TipeKost.PUTRA, TipeKost.PUTRI, TipeKost.CAMPUR}) {
            String name = t.name().charAt(0) + t.name().substring(1).toLowerCase();
            addChip(cgTipe, name, t.ordinal());
        }
        for (int i = 0; i < DISTANCES.length; i++) {
            Chip chip = addChip(cgDistance, DISTANCE_LABELS[i], i);
            chip.setEnabled(hasLocation || i == 0);
        }
        tvDistanceHint.setText(hasLocation
                ? "Dihitung dari lokasi yang kamu pilih di Beranda"
                : "Pilih lokasi di Beranda untuk memakai filter jarak");
        for (Fasilitas f : Fasilitas.getMaster()) {
            addChip(cgFasilitas, f.getNamaFasilitas(), f.getIdFasilitas());
        }

        String city = UserLocationManager.getInstance(requireContext()).getActiveCity();
        List<String> kecamatan = IndonesiaLocationData.getKecamatanList(city != null && !city.isEmpty() ? city : "Pekanbaru");
        actArea.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line,
                kecamatan != null ? kecamatan : new ArrayList<>()));

        rsPrice.setLabelFormatter(FormatUtil::formatRupiah);
        rsPrice.addOnChangeListener((slider, value, fromUser) -> {
            updatePriceLabel();
            scheduleCount();
        });

        applyInitial();

        cgSort.setOnCheckedStateChangeListener((g, ids) -> scheduleCount());
        cgTipe.setOnCheckedStateChangeListener((g, ids) -> scheduleCount());
        cgDistance.setOnCheckedStateChangeListener((g, ids) -> scheduleCount());
        cgFasilitas.setOnCheckedStateChangeListener((g, ids) -> scheduleCount());
        swAvailable.setOnCheckedChangeListener((b, c) -> scheduleCount());
        actArea.setOnItemClickListener((p, v, pos, id) -> scheduleCount());
        actArea.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable e) {
                if (e.length() == 0) scheduleCount();
            }
        });

        view.findViewById(R.id.tv_filter_reset).setOnClickListener(v -> {
            initial = new KostFilterCriteria();
            initial.setKeyword(buildCriteria().getKeyword());
            applyInitial();
            scheduleCount();
        });

        btnApply.setOnClickListener(v -> {
            if (listener != null) listener.onFilterApplied(buildCriteria());
            dismiss();
        });

        scheduleCount();
    }

    @Override
    public void onDestroyView() {
        countHandler.removeCallbacks(countRunnable);
        super.onDestroyView();
    }

    private Chip addChip(ChipGroup group, String label, int tag) {
        Chip chip = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
        chip.setText(label);
        chip.setTag(tag);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(true);
        chip.setChipBackgroundColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_background_selector));
        chip.setChipStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_stroke_selector));
        chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
        chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector));
        chip.setCheckedIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
        chip.setId(View.generateViewId());
        group.addView(chip);
        return chip;
    }

    private void checkTag(ChipGroup group, int tag) {
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip c = (Chip) group.getChildAt(i);
            c.setChecked(c.getTag() != null && (int) c.getTag() == tag);
        }
    }

    private int checkedTag(ChipGroup group, int fallback) {
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip c = (Chip) group.getChildAt(i);
            if (c.isChecked()) return (int) c.getTag();
        }
        return fallback;
    }

    private void applyInitial() {
        int sortIndex = 0;
        for (int i = 0; i < SORT_KEYS.length; i++) {
            if (SORT_KEYS[i].equalsIgnoreCase(initial.getSortBy())) sortIndex = i;
        }
        if (sortIndex == 3 && !hasLocation) sortIndex = 0;
        checkTag(cgSort, sortIndex);
        checkTag(cgTipe, initial.getTipeKost() != null ? initial.getTipeKost().ordinal() : -1);

        int distanceIndex = 0;
        if (initial.getMaxDistanceKm() != null && hasLocation) {
            for (int i = 0; i < DISTANCES.length; i++) {
                if (DISTANCES[i] == initial.getMaxDistanceKm()) distanceIndex = i;
            }
        }
        checkTag(cgDistance, distanceIndex);

        List<Integer> fas = initial.getFasilitasIds() != null ? initial.getFasilitasIds() : new ArrayList<>();
        for (int i = 0; i < cgFasilitas.getChildCount(); i++) {
            Chip c = (Chip) cgFasilitas.getChildAt(i);
            c.setChecked(fas.contains((Integer) c.getTag()));
        }

        float min = initial.getMinHarga() != null ? (float) Math.min(initial.getMinHarga(), PRICE_MAX) : 0f;
        float max = initial.getMaxHarga() != null ? (float) Math.min(initial.getMaxHarga(), PRICE_MAX) : PRICE_MAX;
        min = Math.round(min / 100000f) * 100000f;
        max = Math.round(max / 100000f) * 100000f;
        rsPrice.setValues(min, Math.max(min, max));
        updatePriceLabel();

        swAvailable.setChecked(initial.getStatus() == StatusKost.TERSEDIA);
        actArea.setText(initial.getKecamatan() != null ? initial.getKecamatan() : "", false);
    }

    private void updatePriceLabel() {
        List<Float> v = rsPrice.getValues();
        float min = v.get(0), max = v.get(1);
        String right = max >= PRICE_MAX ? "Rp 5 jt+" : FormatUtil.formatRupiah(max);
        tvPriceLabel.setText(min <= 0 && max >= PRICE_MAX ? "Semua harga" : FormatUtil.formatRupiah(min) + " – " + right);
    }

    private KostFilterCriteria buildCriteria() {
        KostFilterCriteria c = new KostFilterCriteria();
        c.setKeyword(initial.getKeyword());
        c.setOrigin(initial.getOriginLat(), initial.getOriginLng(), initial.getOriginLabel());

        c.setSortBy(SORT_KEYS[checkedTag(cgSort, 0)]);

        int tipe = checkedTag(cgTipe, -1);
        if (tipe >= 0) c.setTipeKost(TipeKost.values()[tipe]);

        List<Float> v = rsPrice.getValues();
        if (v.get(0) > 0) c.setMinHarga((double) v.get(0));
        if (v.get(1) < PRICE_MAX) c.setMaxHarga((double) v.get(1));

        if (swAvailable.isChecked()) c.setStatus(StatusKost.TERSEDIA);

        double distance = DISTANCES[checkedTag(cgDistance, 0)];
        if (distance > 0) c.setMaxDistanceKm(distance);

        String area = actArea.getText() != null ? actArea.getText().toString().trim() : "";
        if (!area.isEmpty()) c.setKecamatan(area);

        List<Integer> fas = new ArrayList<>();
        for (int i = 0; i < cgFasilitas.getChildCount(); i++) {
            Chip chip = (Chip) cgFasilitas.getChildAt(i);
            if (chip.isChecked()) fas.add((Integer) chip.getTag());
        }
        if (!fas.isEmpty()) c.setFasilitasIds(fas);
        return c;
    }

    private void scheduleCount() {
        if (btnApply == null) return;
        btnApply.setText("Menghitung…");
        countHandler.removeCallbacks(countRunnable);
        countHandler.postDelayed(countRunnable, 400);
    }

    /** Hitung jumlah hasil agar pengguna tahu efek filter sebelum menerapkannya. */
    private void refreshResultCount() {
        final int serial = ++countSerial;
        kostRepository.filterKost(buildCriteria(), (String) null, new DataCallback<List<Kost>>() {
            @Override
            public void onSuccess(List<Kost> data) {
                if (!isAdded() || serial != countSerial) return;
                int n = data != null ? data.size() : 0;
                btnApply.setText(n == 0 ? "Tidak ada kost yang cocok" : "Tampilkan " + n + " kost");
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || serial != countSerial) return;
                btnApply.setText("Tampilkan hasil");
            }
        });
    }
}
