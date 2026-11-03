package com.carikostkita.ui.roommate;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.carikostkita.R;
import com.carikostkita.data.model.RoommatePost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.RoommateRepository;
import com.carikostkita.util.PageHeader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddRoommateActivity extends AppCompatActivity {

    private TextInputEditText etName, etCampus, etFaculty, etBudget, etArea, etRoomSpec, etDesc, etWa;
    private RadioGroup rgGender;
    private RadioButton rbPutra, rbPutri;
    private MaterialButton btnSubmit;
    private RoommateRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_roommate);

        repository = new RoommateRepository(this);

        PageHeader.bind(findViewById(android.R.id.content), "Pasang Iklan",
                "Temukan teman sekamar yang cocok di Pekanbaru");

        findViewById(R.id.btn_add_roommate_back).setOnClickListener(v -> finish());

        etName = findViewById(R.id.et_roommate_name);
        etCampus = findViewById(R.id.et_roommate_campus);
        etFaculty = findViewById(R.id.et_roommate_faculty);
        rgGender = findViewById(R.id.rg_roommate_gender);
        rbPutra = findViewById(R.id.rb_gender_putra);
        rbPutri = findViewById(R.id.rb_gender_putri);
        etBudget = findViewById(R.id.et_roommate_budget);
        etArea = findViewById(R.id.et_roommate_area);
        etRoomSpec = findViewById(R.id.et_roommate_room_spec);
        etDesc = findViewById(R.id.et_roommate_desc);
        etWa = findViewById(R.id.et_roommate_wa);
        btnSubmit = findViewById(R.id.btn_submit_roommate);

        btnSubmit.setOnClickListener(v -> submitPost());
    }

    private void submitPost() {
        String name = getFieldText(etName);
        String campus = getFieldText(etCampus);
        String faculty = getFieldText(etFaculty);
        String gender = rbPutri.isChecked() ? "PUTRI" : "PUTRA";
        String budgetStr = getFieldText(etBudget);
        String area = getFieldText(etArea);
        String roomSpec = getFieldText(etRoomSpec);
        String desc = getFieldText(etDesc);
        String wa = getFieldText(etWa);

        if (name.length() < 2) {
            etName.setError("Masukkan nama kamu.");
            etName.requestFocus();
            return;
        }

        if (campus.isEmpty()) {
            etCampus.setError("Masukkan nama kampus (misal UNRI Panam).");
            etCampus.requestFocus();
            return;
        }

        int budget = 0;
        try {
            budget = Integer.parseInt(budgetStr);
        } catch (Exception ignored) {}

        if (budget < 100000) {
            etBudget.setError("Budget minimal Rp 100.000.");
            etBudget.requestFocus();
            return;
        }

        if (area.length() < 3) {
            etArea.setError("Masukkan target area kost.");
            etArea.requestFocus();
            return;
        }

        if (desc.length() < 10) {
            etDesc.setError("Tuliskan kriteria teman sekamar minimal 10 karakter.");
            etDesc.requestFocus();
            return;
        }

        if (wa.length() < 8) {
            etWa.setError("Masukkan nomor WhatsApp yang aktif.");
            etWa.requestFocus();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Menerbitkan...");

        RoommatePost newPost = new RoommatePost(name, campus, faculty, gender, budget, area, roomSpec, desc, wa);

        repository.createRoommatePost(newPost, new DataCallback<RoommatePost>() {
            @Override
            public void onSuccess(RoommatePost data) {
                Toast.makeText(AddRoommateActivity.this, "Iklan teman sekamar berhasil diterbitkan!", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String message) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Terbitkan Iklan Sekarang");
                Toast.makeText(AddRoommateActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFieldText(TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }
}
