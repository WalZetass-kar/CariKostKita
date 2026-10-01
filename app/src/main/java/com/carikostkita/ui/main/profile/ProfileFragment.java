package com.carikostkita.ui.main.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.carikostkita.R;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.auth.LoginActivity;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ProfileFragment extends Fragment {

    private SessionManager sessionManager;
    private UserRepository userRepository;

    private ImageView ivAvatar;
    private FrameLayout btnEditAvatar;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvRole;
    private MaterialButton btnEditProfile;
    private CardView cardAdminShortcut;
    private TextView tvOwnerShortcutTitle;
    private TextView tvOwnerShortcutDesc;
    private Button btnGotoAdmin;
    private View itemChangePassword;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        userRepository = new UserRepository(requireContext());

        ivAvatar = view.findViewById(R.id.iv_profile_avatar);
        btnEditAvatar = view.findViewById(R.id.btn_edit_avatar);
        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvPhone = view.findViewById(R.id.tv_profile_phone);
        tvRole = view.findViewById(R.id.tv_profile_role);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        cardAdminShortcut = view.findViewById(R.id.card_admin_shortcut);
        tvOwnerShortcutTitle = view.findViewById(R.id.tv_owner_shortcut_title);
        tvOwnerShortcutDesc = view.findViewById(R.id.tv_owner_shortcut_desc);
        btnGotoAdmin = view.findViewById(R.id.btn_goto_admin);
        itemChangePassword = view.findViewById(R.id.item_change_password);
        btnLogout = view.findViewById(R.id.btn_logout);

        populateUserData();

        btnEditAvatar.setOnClickListener(v -> showAvatarSelectionDialog());
        ivAvatar.setOnClickListener(v -> showAvatarSelectionDialog());
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        itemChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Konfirmasi Keluar")
                    .setMessage("Apakah Anda yakin ingin keluar dari akun Anda?")
                    .setPositiveButton("Ya, Keluar", (dialog, which) -> {
                        sessionManager.logout();
                        Toast.makeText(requireContext(), "Anda telah keluar dari akun", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }

    private void populateUserData() {
        if (!sessionManager.isLoggedIn()) {
            tvName.setText("Tamu");
            tvEmail.setText("Belum masuk akun");
            tvPhone.setText("Nomor WhatsApp: -");
            tvRole.setText("GUEST");
            cardAdminShortcut.setVisibility(View.GONE);
            btnEditProfile.setVisibility(View.GONE);
            return;
        }

        tvName.setText(sessionManager.getUserName());
        tvEmail.setText(sessionManager.getUserEmail());

        String phone = sessionManager.getUserPhone();
        if (phone != null && !phone.isEmpty()) {
            tvPhone.setText("No. WhatsApp: " + phone);
        } else {
            tvPhone.setText("No. WhatsApp: Belum diatur");
        }

        // Set Avatar
        applyAvatar(sessionManager.getUserAvatar());

        // Role handling
        if (sessionManager.isDeveloper()) {
            tvRole.setText("DEVELOPER / ADMIN");
            tvRole.setBackgroundResource(R.drawable.bg_badge_putra);
            tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_putra));

            cardAdminShortcut.setVisibility(View.VISIBLE);
            tvOwnerShortcutTitle.setText("Panel Developer & Sistem");
            tvOwnerShortcutDesc.setText("Kontrol seluruh data properti kost, wilayah, fasilitas, dan preferensi sistem.");
            btnGotoAdmin.setText("Buka Dashboard Developer");
            btnGotoAdmin.setOnClickListener(v -> startActivity(new Intent(requireContext(), AdminMainActivity.class)));
        } else if (sessionManager.isPemilikKost()) {
            tvRole.setText("PEMILIK KOST");
            tvRole.setBackgroundResource(R.drawable.bg_badge_campur);
            tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_campur));

            cardAdminShortcut.setVisibility(View.VISIBLE);
            tvOwnerShortcutTitle.setText("Panel Pemilik Kost");
            tvOwnerShortcutDesc.setText("Kelola daftar properti kost, status kamar kosong/penuh, dan kontak sewa.");
            btnGotoAdmin.setText("Buka Dashboard Pemilik Kost");
            btnGotoAdmin.setOnClickListener(v -> startActivity(new Intent(requireContext(), AdminMainActivity.class)));
        } else {
            tvRole.setText("PENCARI KOST");
            tvRole.setBackgroundResource(R.drawable.bg_badge_tersedia);
            tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_tersedia));
            cardAdminShortcut.setVisibility(View.GONE);
        }
    }

    private void applyAvatar(String avatarKey) {
        if ("avatar_female".equals(avatarKey)) {
            ivAvatar.setImageResource(R.drawable.ic_avatar_female);
        } else if ("avatar_owner".equals(avatarKey)) {
            ivAvatar.setImageResource(R.drawable.ic_avatar_owner);
        } else if ("avatar_launcher".equals(avatarKey)) {
            ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_avatar_male);
        }
    }

    private void showAvatarSelectionDialog() {
        String[] options = {"Avatar Pria / Mahasiswa", "Avatar Wanita / Mahasiswi", "Avatar Pemilik Properti", "Logo Default"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pilih Foto Profil")
                .setItems(options, (dialog, which) -> {
                    String selectedKey = "avatar_male";
                    if (which == 1) selectedKey = "avatar_female";
                    else if (which == 2) selectedKey = "avatar_owner";
                    else if (which == 3) selectedKey = "avatar_launcher";

                    final String finalKey = selectedKey;
                    int userId = sessionManager.getUserId();
                    userRepository.updateProfile(userId, sessionManager.getUserName(), sessionManager.getUserPhone(), finalKey, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            sessionManager.updateProfile(sessionManager.getUserName(), sessionManager.getUserPhone(), finalKey);
                            applyAvatar(finalKey);
                            Toast.makeText(requireContext(), "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void showEditProfileDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        TextInputLayout tilName = new TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilName.setHint("Nama Lengkap");
        TextInputEditText etName = new TextInputEditText(requireContext());
        etName.setText(sessionManager.getUserName());
        tilName.addView(etName);
        layout.addView(tilName);

        TextInputLayout tilPhone = new TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilPhone.setHint("Nomor WhatsApp (contoh: 081234567890)");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 24;
        tilPhone.setLayoutParams(lp);
        TextInputEditText etPhone = new TextInputEditText(requireContext());
        etPhone.setText(sessionManager.getUserPhone());
        tilPhone.addView(etPhone);
        layout.addView(tilPhone);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ubah Profil & Kontak")
                .setView(layout)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String newPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

                    if (newName.isEmpty()) {
                        Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int userId = sessionManager.getUserId();
                    String avatar = sessionManager.getUserAvatar();
                    userRepository.updateProfile(userId, newName, newPhone, avatar, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            sessionManager.updateProfile(newName, newPhone, avatar);
                            populateUserData();
                            Toast.makeText(requireContext(), "Profil berhasil disimpan", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        TextInputLayout tilOld = new TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilOld.setHint("Kata Sandi Lama");
        tilOld.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        TextInputEditText etOld = new TextInputEditText(requireContext());
        etOld.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilOld.addView(etOld);
        layout.addView(tilOld);

        TextInputLayout tilNew = new TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilNew.setHint("Kata Sandi Baru (min 6 karakter)");
        tilNew.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 24;
        tilNew.setLayoutParams(lp);
        TextInputEditText etNew = new TextInputEditText(requireContext());
        etNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilNew.addView(etNew);
        layout.addView(tilNew);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ganti Kata Sandi")
                .setView(layout)
                .setPositiveButton("Perbarui", (dialog, which) -> {
                    String oldPass = etOld.getText() != null ? etOld.getText().toString().trim() : "";
                    String newPass = etNew.getText() != null ? etNew.getText().toString().trim() : "";

                    if (oldPass.isEmpty() || newPass.isEmpty()) {
                        Toast.makeText(requireContext(), "Harap isi kata sandi lama dan baru", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(requireContext(), "Kata sandi baru minimal 6 karakter", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int userId = sessionManager.getUserId();
                    userRepository.changePassword(userId, oldPass, newPass, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            Toast.makeText(requireContext(), "Kata sandi berhasil diperbarui", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
