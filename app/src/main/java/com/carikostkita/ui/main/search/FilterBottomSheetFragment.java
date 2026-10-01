package com.carikostkita.ui.main.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.carikostkita.R;
import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.model.TipeKost;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class FilterBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnFilterAppliedListener {
        void onFilterApplied(KostFilterCriteria criteria);
    }

    private OnFilterAppliedListener listener;
    private RadioGroup rgTipe;
    private RadioGroup rgHarga;
    private CheckBox cbWifi;
    private CheckBox cbAc;
    private CheckBox cbKmDalam;
    private CheckBox cbParkir;

    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rgTipe = view.findViewById(R.id.rg_filter_tipe);
        rgHarga = view.findViewById(R.id.rg_filter_harga);
        cbWifi = view.findViewById(R.id.cb_fasilitas_wifi);
        cbAc = view.findViewById(R.id.cb_fasilitas_ac);
        cbKmDalam = view.findViewById(R.id.cb_fasilitas_km_dalam);
        cbParkir = view.findViewById(R.id.cb_fasilitas_parkir);
        MaterialButton btnApply = view.findViewById(R.id.btn_filter_apply);
        View tvReset = view.findViewById(R.id.tv_filter_reset);

        tvReset.setOnClickListener(v -> resetFilters());

        btnApply.setOnClickListener(v -> {
            KostFilterCriteria criteria = buildCriteria();
            if (listener != null) {
                listener.onFilterApplied(criteria);
            }
            dismiss();
        });
    }

    private void resetFilters() {
        rgTipe.check(R.id.rb_tipe_semua);
        rgHarga.check(R.id.rb_harga_semua);
        cbWifi.setChecked(false);
        cbAc.setChecked(false);
        cbKmDalam.setChecked(false);
        cbParkir.setChecked(false);
    }

    private KostFilterCriteria buildCriteria() {
        KostFilterCriteria criteria = new KostFilterCriteria();

        // Tipe
        int selectedTipeId = rgTipe.getCheckedRadioButtonId();
        if (selectedTipeId == R.id.rb_tipe_putri) {
            criteria.setTipeKost(TipeKost.PUTRI);
        } else if (selectedTipeId == R.id.rb_tipe_putra) {
            criteria.setTipeKost(TipeKost.PUTRA);
        } else if (selectedTipeId == R.id.rb_tipe_campur) {
            criteria.setTipeKost(TipeKost.CAMPUR);
        }

        // Harga
        int selectedHargaId = rgHarga.getCheckedRadioButtonId();
        if (selectedHargaId == R.id.rb_harga_dibawah_700) {
            criteria.setMaxHarga(700000.0);
        } else if (selectedHargaId == R.id.rb_harga_700_1jt) {
            criteria.setMinHarga(700000.0);
            criteria.setMaxHarga(1000000.0);
        } else if (selectedHargaId == R.id.rb_harga_diatas_1jt) {
            criteria.setMinHarga(1000000.0);
        }

        // Fasilitas IDs (berdasarkan database_seed: 1=WiFi, 2=Parkir Motor, 3=Parkir Mobil, 4=AC, 5=KM Dalam)
        List<Integer> reqIds = new ArrayList<>();
        if (cbWifi.isChecked()) reqIds.add(1);
        if (cbParkir.isChecked()) reqIds.add(2);
        if (cbAc.isChecked()) reqIds.add(4);
        if (cbKmDalam.isChecked()) reqIds.add(5);

        if (!reqIds.isEmpty()) {
            criteria.setFasilitasIds(reqIds);
        }

        return criteria;
    }
}
