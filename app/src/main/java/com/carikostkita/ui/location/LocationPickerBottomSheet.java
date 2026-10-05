package com.carikostkita.ui.location;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.carikostkita.R;
import com.carikostkita.data.location.IndonesiaLocationData;
import com.carikostkita.data.location.UserLocationManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class LocationPickerBottomSheet {

    public interface OnLocationSelectedListener {
        void onLocationSelected(String city, String district, String display);
    }

    public static void show(@NonNull Context context, OnLocationSelectedListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_location_picker, null);
        dialog.setContentView(view);

        MaterialButton btnUseGps = view.findViewById(R.id.btn_picker_use_gps);
        ProgressBar pbGps = view.findViewById(R.id.pb_picker_gps_loading);
        AutoCompleteTextView actProvinsi = view.findViewById(R.id.act_picker_provinsi);
        AutoCompleteTextView actKota = view.findViewById(R.id.act_picker_kota);
        AutoCompleteTextView actKecamatan = view.findViewById(R.id.act_picker_kecamatan);
        MaterialButton btnApply = view.findViewById(R.id.btn_picker_apply);

        UserLocationManager locationManager = UserLocationManager.getInstance(context);

        // 1. Setup Dropdown Provinsi
        List<String> provinsiList = IndonesiaLocationData.getProvinsiList();
        ArrayAdapter<String> provAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, provinsiList);
        actProvinsi.setAdapter(provAdapter);

        // 2. Cascade Provinsi -> Kota
        actProvinsi.setOnItemClickListener((parent, v, position, id) -> {
            String selectedProv = provAdapter.getItem(position);
            actKota.setText("", false);
            actKecamatan.setText("", false);
            actKecamatan.setEnabled(false);

            if (selectedProv != null) {
                List<String> kotaList = IndonesiaLocationData.getKotaList(selectedProv);
                ArrayAdapter<String> kotaAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, kotaList);
                actKota.setAdapter(kotaAdapter);
                actKota.setEnabled(true);
            }
        });

        // 3. Cascade Kota -> Kecamatan
        actKota.setOnItemClickListener((parent, v, position, id) -> {
            String selectedKota = (String) parent.getItemAtPosition(position);
            actKecamatan.setText("", false);

            if (selectedKota != null) {
                List<String> kecList = IndonesiaLocationData.getKecamatanList(selectedKota);
                ArrayAdapter<String> kecAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, kecList);
                actKecamatan.setAdapter(kecAdapter);
                actKecamatan.setEnabled(true);
            }
        });

        // 4. GPS Button
        btnUseGps.setOnClickListener(v -> {
            if (!locationManager.hasLocationPermission()) {
                Toast.makeText(context, "Izin lokasi diperlukan untuk deteksi GPS otomatis", Toast.LENGTH_SHORT).show();
                return;
            }
            pbGps.setVisibility(View.VISIBLE);
            btnUseGps.setEnabled(false);

            locationManager.detectCurrentLocation(new UserLocationManager.LocationCallback() {
                @Override
                public void onLocationDetected(String city, String district, double lat, double lng, String display) {
                    pbGps.setVisibility(View.GONE);
                    btnUseGps.setEnabled(true);
                    dialog.dismiss();
                    if (listener != null) {
                        listener.onLocationSelected(city, district, display);
                    }
                }

                @Override
                public void onLocationFailed(String error) {
                    pbGps.setVisibility(View.GONE);
                    btnUseGps.setEnabled(true);
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // 5. Apply Manual Button
        btnApply.setOnClickListener(v -> {
            String kota = actKota.getText() != null ? actKota.getText().toString().trim() : "";
            String kec = actKecamatan.getText() != null ? actKecamatan.getText().toString().trim() : "";
            String prov = actProvinsi.getText() != null ? actProvinsi.getText().toString().trim() : "";

            if (kota.isEmpty() && prov.isEmpty()) {
                Toast.makeText(context, "Silakan pilih minimal Provinsi dan Kota", Toast.LENGTH_SHORT).show();
                return;
            }

            String activeCity = !kota.isEmpty() ? kota : prov;
            locationManager.setManualLocation(activeCity, kec, 0, 0);

            String display = locationManager.getActiveLocationDisplay();
            dialog.dismiss();
            if (listener != null) {
                listener.onLocationSelected(activeCity, kec, display);
            }
        });

        dialog.show();
    }
}
