package com.carikostkita.ui.main.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.carikostkita.R;
import com.carikostkita.data.model.User;
import com.carikostkita.data.model.VerificationStatus;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.UserRepository;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.admin.PemilikMainActivity;
import com.carikostkita.ui.auth.LoginActivity;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
    private TextView tvBio;
    private TextView tvRole;
    private MaterialButton btnEditProfile;

    private MaterialCardView cardOwnerVerification;
    private ImageView ivVerificationIcon;
    private TextView tvVerificationTitle;
    private TextView tvVerificationDesc;
    private MaterialButton btnApplyOwner;

    private MaterialCardView cardAdminShortcut;
    private TextView tvOwnerShortcutTitle;
    private TextView tvOwnerShortcutDesc;
    private MaterialButton btnGotoAdmin;

    private View itemChangePassword;
    private View itemNotificationSettings;
    private TextView tvNotificationStatus;
    private MaterialButton btnLogout;

    private boolean notificationsEnabled = true;

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

        initViews(view);
        populateUserData();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            userRepository.getUserById(sessionManager.getUserId(), new DataCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    sessionManager.updateVerificationStatus(user.getVerificationStatus(), user.getRole(), user.getCatatanRevisi());
                    populateUserData();
                }

                @Override
                public void onError(String message) {}
            });
        }
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.iv_profile_avatar);
        btnEditAvatar = view.findViewById(R.id.btn_edit_avatar);
        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvPhone = view.findViewById(R.id.tv_profile_phone);
        tvBio = view.findViewById(R.id.tv_profile_bio);
        tvRole = view.findViewById(R.id.tv_profile_role);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        cardOwnerVerification = view.findViewById(R.id.card_owner_verification);
        ivVerificationIcon = view.findViewById(R.id.iv_verification_icon);
        tvVerificationTitle = view.findViewById(R.id.tv_verification_title);
        tvVerificationDesc = view.findViewById(R.id.tv_verification_desc);
        btnApplyOwner = view.findViewById(R.id.btn_apply_owner);

        cardAdminShortcut = view.findViewById(R.id.card_admin_shortcut);
        tvOwnerShortcutTitle = view.findViewById(R.id.tv_owner_shortcut_title);
        tvOwnerShortcutDesc = view.findViewById(R.id.tv_owner_shortcut_desc);
        btnGotoAdmin = view.findViewById(R.id.btn_goto_admin);

        itemChangePassword = view.findViewById(R.id.item_change_password);
        itemNotificationSettings = view.findViewById(R.id.item_notification_settings);
        tvNotificationStatus = view.findViewById(R.id.tv_notification_status);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupListeners() {
        btnEditAvatar.setOnClickListener(v -> showAvatarSelectionDialog());
        ivAvatar.setOnClickListener(v -> showAvatarSelectionDialog());
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        itemChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        itemNotificationSettings.setOnClickListener(v -> {
            notificationsEnabled = !notificationsEnabled;
            tvNotificationStatus.setText(notificationsEnabled ? "Aktif" : "Senyap");
            tvNotificationStatus.setTextColor(ContextCompat.getColor(requireContext(),
                    notificationsEnabled ? R.color.status_tersedia : R.color.text_muted));
            Toast.makeText(requireContext(),
                    notificationsEnabled ? "Notifikasi aplikasi diaktifkan" : "Notifikasi disenyapkan",
                    Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            AppDialogHelper.showLogoutDialog(requireContext(), () -> {
                sessionManager.logout();
                Toast.makeText(requireContext(), "Anda telah keluar dari akun", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        });
    }

    private void populateUserData() {
        if (!sessionManager.isLoggedIn()) {
            tvName.setText("Tamu");
            tvEmail.setText("Belum masuk akun");
            tvPhone.setText("No. WhatsApp: -");
            tvRole.setText("GUEST");
            tvBio.setVisibility(View.GONE);
            cardAdminShortcut.setVisibility(View.GONE);
            cardOwnerVerification.setVisibility(View.GONE);
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

        String bio = sessionManager.getUserBio();
        if (bio != null && !bio.trim().isEmpty()) {
            tvBio.setText(bio.trim());
            tvBio.setVisibility(View.VISIBLE);
        } else {
            tvBio.setVisibility(View.GONE);
        }

        // Apply avatar
        applyAvatar(sessionManager.getUserAvatar());

        // Role & Verification RBAC separation
        if (sessionManager.isDeveloper()) {
            // ADMIN / DEVELOPER
            tvRole.setText("DEVELOPER / ADMIN");
            tvRole.setBackgroundResource(R.drawable.bg_badge_putra);
            tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_putra));

            cardOwnerVerification.setVisibility(View.GONE);
            cardAdminShortcut.setVisibility(View.VISIBLE);
            tvOwnerShortcutTitle.setText("Developer Control Center");
            tvOwnerShortcutDesc.setText("Pusat verifikasi pengajuan pemilik kost, manajemen properti, dan data sistem.");
            btnGotoAdmin.setText("Buka Control Center Admin");
            btnGotoAdmin.setOnClickListener(v -> startActivity(new Intent(requireContext(), AdminMainActivity.class)));

        } else if (sessionManager.isPemilikKost()) {
            // PEMILIK KOST TERVERIFIKASI
            tvRole.setText("PEMILIK TERVERIFIKASI");
            tvRole.setBackgroundResource(R.drawable.bg_badge_campur);
            tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.badge_campur));

            cardOwnerVerification.setVisibility(View.GONE);
            cardAdminShortcut.setVisibility(View.VISIBLE);
            tvOwnerShortcutTitle.setText("Dashboard Pemilik Kost");
            tvOwnerShortcutDesc.setText("Kelola daftar kost Anda, multiple foto galeri, ketersediaan kamar, dan kontak sewa.");
            btnGotoAdmin.setText("Buka Dashboard Pemilik");
            btnGotoAdmin.setOnClickListener(v -> startActivity(new Intent(requireContext(), PemilikMainActivity.class)));

        } else {
            // PENCARI KOST
            tvRole.setText("PENCARI KOST");
            tvRole.setBackgroundResource(R.drawable.bg_badge_tersedia);
            tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_tersedia));

            cardAdminShortcut.setVisibility(View.GONE);
            cardOwnerVerification.setVisibility(View.VISIBLE);

            VerificationStatus status = sessionManager.getVerificationStatus();
            if (status == VerificationStatus.PENDING) {
                ivVerificationIcon.setImageResource(R.drawable.ic_warning);
                ivVerificationIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
                tvVerificationTitle.setText("Menunggu Verifikasi Admin");
                tvVerificationDesc.setText("Pengajuan akun Anda sebagai pemilik kost sedang ditinjau developer/admin. Anda tetap dapat menggunakan akun untuk mencari & menyimpan kost.");
                btnApplyOwner.setText("Status: Sedang Ditinjau");
                btnApplyOwner.setEnabled(false);
            } else if (status == VerificationStatus.REVISION_REQUIRED) {
                ivVerificationIcon.setImageResource(R.drawable.ic_warning);
                ivVerificationIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.badge_campur));
                tvVerificationTitle.setText("Pengajuan Perlu Perbaikan");
                String revNote = sessionManager.getCatatanRevisi();
                String desc = (revNote != null && !revNote.trim().isEmpty())
                        ? "Catatan Developer/Admin:\n\"" + revNote.trim() + "\"\n\nSilakan perbaiki data lalu ajukan kembali."
                        : "Pengajuan Anda memerlukan perbaikan informasi. Silakan periksa kembali data properti kost Anda.";
                tvVerificationDesc.setText(desc);
                btnApplyOwner.setText("Perbaiki & Ajukan Ulang");
                btnApplyOwner.setEnabled(true);
                btnApplyOwner.setOnClickListener(v -> showOwnerApplicationDialog());
            } else if (status == VerificationStatus.REJECTED) {
                ivVerificationIcon.setImageResource(R.drawable.ic_warning);
                ivVerificationIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.status_penuh));
                tvVerificationTitle.setText("Pengajuan Pemilik Ditolak");
                tvVerificationDesc.setText("Pengajuan akun Anda sebagai pemilik kost belum dapat disetujui. Anda dapat mengajukan ulang data properti kost Anda.");
                btnApplyOwner.setText("Ajukan Verifikasi Ulang");
                btnApplyOwner.setEnabled(true);
                btnApplyOwner.setOnClickListener(v -> showOwnerApplicationDialog());
            } else { // NONE
                ivVerificationIcon.setImageResource(R.drawable.ic_info);
                ivVerificationIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
                tvVerificationTitle.setText("Ingin Mengiklankan Kost?");
                tvVerificationDesc.setText("Ajukan akun Anda untuk diverifikasi sebagai pemilik kost agar dapat memasang dan mengelola properti di Bukit Raya.");
                btnApplyOwner.setText("Ajukan Verifikasi Pemilik Kost");
                btnApplyOwner.setEnabled(true);
                btnApplyOwner.setOnClickListener(v -> showOwnerApplicationDialog());
            }
        }
    }

    private void showOwnerApplicationDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        TextView tvInfo = new TextView(requireContext());
        tvInfo.setText("Untuk menjaga keamanan pencari kost mahasiswa, akun pemilik wajib diverifikasi oleh developer/admin.");
        tvInfo.setTextSize(13);
        tvInfo.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        layout.addView(tvInfo);

        TextInputLayout tilNote = new TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilNote.setHint("Nama Kost & Alamat / Catatan Pengajuan");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 20;
        tilNote.setLayoutParams(lp);

        TextInputEditText etNote = new TextInputEditText(requireContext());
        etNote.setLines(3);
        tilNote.addView(etNote);
        layout.addView(tilNote);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pengajuan Pemilik Kost")
                .setView(layout)
                .setPositiveButton("Kirim Pengajuan", (dialog, which) -> {
                    String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
                    if (note.isEmpty()) {
                        note = "Pengajuan verifikasi dari pengguna: " + sessionManager.getUserName();
                    }

                    int userId = sessionManager.getUserId();
                    userRepository.submitOwnerVerification(userId, note, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean ok) {
                            sessionManager.updateVerificationStatus(VerificationStatus.PENDING, null, "");
                            new com.carikostkita.data.repository.ActivityLogRepository(requireContext())
                                    .log(userId, sessionManager.getUserName(), "PENGAJUAN_PEMILIK",
                                            "Pengguna mengajukan verifikasi akun pemilik kost", "USER", userId);
                            populateUserData();
                            AppDialogHelper.showSuccessDialog(requireContext(),
                                    "Pengajuan Terkirim",
                                    "Pengajuan pemilik kost berhasil dikirim ke Developer/Admin. Silakan tunggu proses verifikasi.",
                                    null);
                        }

                        @Override
                        public void onError(String message) {
                            AppDialogHelper.showErrorDialog(requireContext(), "Gagal Mengajukan", message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
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
                    userRepository.updateProfile(userId, sessionManager.getUserName(), sessionManager.getUserPhone(), sessionManager.getUserBio(), finalKey, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            sessionManager.updateProfile(sessionManager.getUserName(), sessionManager.getUserPhone(), sessionManager.getUserBio(), finalKey);
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
        tilPhone.setHint("Nomor WhatsApp");
        LinearLayout.LayoutParams lpPhone = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpPhone.topMargin = 20;
        tilPhone.setLayoutParams(lpPhone);
        TextInputEditText etPhone = new TextInputEditText(requireContext());
        etPhone.setText(sessionManager.getUserPhone());
        tilPhone.addView(etPhone);
        layout.addView(tilPhone);

        TextInputLayout tilBio = new TextInputLayout(requireContext(), null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilBio.setHint("Bio Singkat (misal: Pemilik Kost Melati / Mahasiswa UIR)");
        LinearLayout.LayoutParams lpBio = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBio.topMargin = 20;
        tilBio.setLayoutParams(lpBio);
        TextInputEditText etBio = new TextInputEditText(requireContext());
        etBio.setText(sessionManager.getUserBio());
        tilBio.addView(etBio);
        layout.addView(tilBio);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ubah Profil & Kontak")
                .setView(layout)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String newPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                    String newBio = etBio.getText() != null ? etBio.getText().toString().trim() : "";

                    if (newName.isEmpty()) {
                        Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int userId = sessionManager.getUserId();
                    String avatar = sessionManager.getUserAvatar();
                    userRepository.updateProfile(userId, newName, newPhone, newBio, avatar, new DataCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            sessionManager.updateProfile(newName, newPhone, newBio, avatar);
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
        lp.topMargin = 20;
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
                            AppDialogHelper.showSuccessDialog(requireContext(), "Kata Sandi Diperbarui", "Kata sandi akun Anda berhasil diganti.", null);
                        }

                        @Override
                        public void onError(String message) {
                            AppDialogHelper.showErrorDialog(requireContext(), "Gagal Mengganti Kata Sandi", message);
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
