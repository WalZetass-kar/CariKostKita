package com.carikostkita.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.carikostkita.R;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import com.carikostkita.util.UserAvatarHelper;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class EditProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private UserRepository userRepository;

    private ImageView btnBack;
    private ImageView ivAvatar;
    private View btnChangeAvatarBadge;
    private MaterialButton btnChooseAvatar;
    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etBio;
    private MaterialButton btnSave;

    private String selectedAvatarKey = "avatar_male";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);
        userRepository = new UserRepository(this);

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        initViews();
        loadCurrentData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivAvatar = findViewById(R.id.iv_avatar);
        btnChangeAvatarBadge = findViewById(R.id.btn_change_avatar_badge);
        btnChooseAvatar = findViewById(R.id.btn_choose_avatar);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etBio = findViewById(R.id.et_bio);
        btnSave = findViewById(R.id.btn_save);
    }

    private void loadCurrentData() {
        etName.setText(sessionManager.getUserName());
        etEmail.setText(sessionManager.getUserEmail());
        etPhone.setText(sessionManager.getUserPhone());
        etBio.setText(sessionManager.getUserBio());

        selectedAvatarKey = sessionManager.getUserAvatar();
        applyAvatar(selectedAvatarKey);
    }

    private Uri selectedGalleryImageUri = null;
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedGalleryImageUri = uri;
                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(ivAvatar);
                }
            }
    );

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finishWithAnimation());

        View.OnClickListener galleryPickerListener = v -> {
            // HANYA dari galeri perangkat
            galleryLauncher.launch("image/*");
        };
        ivAvatar.setOnClickListener(galleryPickerListener);
        btnChangeAvatarBadge.setOnClickListener(galleryPickerListener);
        btnChooseAvatar.setOnClickListener(galleryPickerListener);

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void applyAvatar(String avatarKey) {
        UserAvatarHelper.loadAvatar(ivAvatar, avatarKey);
    }

    private void saveProfile() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String bio = etBio.getText() != null ? etBio.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etName.setError("Nama lengkap tidak boleh kosong");
            etName.requestFocus();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Menyimpan...");
        int userId = sessionManager.getUserId();

        if (selectedGalleryImageUri != null) {
            // Upload foto dari galeri terlebih dahulu ke Supabase Storage
            userRepository.uploadAvatar(selectedGalleryImageUri, new DataCallback<String>() {
                @Override
                public void onSuccess(String publicUrl) {
                    selectedAvatarKey = publicUrl;
                    executeSaveProfile(userId, name, phone, bio, publicUrl);
                }

                @Override
                public void onError(String message) {
                    btnSave.setEnabled(true);
                    btnSave.setText("Simpan Perubahan");
                    AppDialogHelper.showErrorDialog(EditProfileActivity.this, "Gagal Mengunggah Foto", message);
                }
            });
        } else {
            executeSaveProfile(userId, name, phone, bio, selectedAvatarKey);
        }
    }

    private void executeSaveProfile(int userId, String name, String phone, String bio, String avatarUrl) {
        userRepository.updateProfile(userId, name, phone, bio, avatarUrl, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                sessionManager.updateProfile(name, phone, bio, avatarUrl);
                Toast.makeText(EditProfileActivity.this, "Foto & data profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finishWithAnimation();
            }

            @Override
            public void onError(String message) {
                btnSave.setEnabled(true);
                btnSave.setText("Simpan Perubahan");
                AppDialogHelper.showErrorDialog(EditProfileActivity.this, "Gagal Menyimpan", message);
            }
        });
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
