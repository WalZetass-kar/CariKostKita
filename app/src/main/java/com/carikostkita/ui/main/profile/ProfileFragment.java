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
import com.bumptech.glide.Glide;
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
import com.carikostkita.util.UserAvatarHelper;
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

    private View itemMyFavorites;
    private View itemChangePassword;
    private View itemNotificationSettings;
    private TextView tvNotificationStatus;
    private MaterialButton btnLogout;

    private boolean notificationsEnabled = true;
    private View layoutSkeleton;
    private View layoutContent;

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
            populateUserData(); // Tampilkan langsung data lokal dari sessionManager tanpa delay
            long startTime = com.carikostkita.util.SkeletonHelper.markStart();
            userRepository.getUserById(sessionManager.getUserId(), new DataCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    com.carikostkita.util.SkeletonHelper.complete(startTime, () -> {
                        if (!isAdded()) return;
                        if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                        if (layoutContent != null) layoutContent.setVisibility(View.VISIBLE);
                        sessionManager.updateVerificationStatus(user.getVerificationStatus(), user.getRole(), user.getCatatanRevisi());
                        String serverAvatar = user.getAvatarUrl();
                        // Jangan overwrite avatar lokal jika server mengembalikan default atau kosong
                        if (serverAvatar != null && !serverAvatar.trim().isEmpty() && !serverAvatar.equals("avatar_male")) {
                            sessionManager.updateProfile(user.getNama(), user.getNoHp(), user.getBio(), serverAvatar);
                        } else {
                            sessionManager.updateProfile(user.getNama(), user.getNoHp(), user.getBio(), sessionManager.getUserAvatar());
                        }
                        populateUserData();
                    });
                }

                @Override
                public void onError(String message) {
                    com.carikostkita.util.SkeletonHelper.complete(startTime, () -> {
                        if (!isAdded()) return;
                        if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                        if (layoutContent != null) layoutContent.setVisibility(View.VISIBLE);
                    });
                }
            });
        }
    }

    private void initViews(View view) {
        layoutSkeleton = view.findViewById(R.id.skeleton_profile);
        layoutContent = view.findViewById(R.id.layout_profile_content);
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

        itemMyFavorites = view.findViewById(R.id.item_my_favorites);
        itemChangePassword = view.findViewById(R.id.item_change_password);
        itemNotificationSettings = view.findViewById(R.id.item_notification_settings);
        tvNotificationStatus = view.findViewById(R.id.tv_notification_status);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupListeners() {
        View.OnClickListener openEditProfileListener = v -> {
            Intent intent = new Intent(requireContext(), com.carikostkita.ui.profile.EditProfileActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        };
        btnEditAvatar.setOnClickListener(openEditProfileListener);
        ivAvatar.setOnClickListener(openEditProfileListener);
        btnEditProfile.setOnClickListener(openEditProfileListener);

        if (itemMyFavorites != null) {
            itemMyFavorites.setOnClickListener(v -> {
                if (getActivity() instanceof com.carikostkita.ui.main.MainActivity) {
                    ((com.carikostkita.ui.main.MainActivity) getActivity()).navigateToFavorites();
                }
            });
        }

        itemChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.carikostkita.ui.profile.ChangePasswordActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

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

        // Role Badge: Netral (Bukan Hijau)
        tvRole.setText("Pencari Kost");
        tvRole.setBackgroundResource(R.drawable.bg_badge_role_neutral);
        tvRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.role_neutral_text));

        // Pengajuan Pemilik Kost
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
            tvVerificationDesc.setText("Ajukan akun Anda untuk diverifikasi sebagai pemilik kost agar dapat memasang dan mengelola properti di CariKostKita.");
            btnApplyOwner.setText("Ajukan Verifikasi Pemilik Kost");
            btnApplyOwner.setEnabled(true);
            btnApplyOwner.setOnClickListener(v -> showOwnerApplicationDialog());
        }
    }

    private void showOwnerApplicationDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pengajuan_pemilik, null);

        com.google.android.material.textfield.TextInputEditText etNama = dialogView.findViewById(R.id.et_pengajuan_nama_kost);
        com.google.android.material.textfield.TextInputEditText etAlamat = dialogView.findViewById(R.id.et_pengajuan_alamat);
        com.google.android.material.textfield.TextInputEditText etWa = dialogView.findViewById(R.id.et_pengajuan_whatsapp);
        com.google.android.material.textfield.TextInputEditText etCatatan = dialogView.findViewById(R.id.et_pengajuan_catatan);
        android.widget.ProgressBar pbLoading = dialogView.findViewById(R.id.pb_pengajuan_loading);
        com.google.android.material.button.MaterialButton btnBatal = dialogView.findViewById(R.id.btn_pengajuan_batal);
        com.google.android.material.button.MaterialButton btnKirim = dialogView.findViewById(R.id.btn_pengajuan_kirim);

        // Pre-fill phone if available in session
        String userPhone = sessionManager.getUserPhone();
        if (userPhone != null && !userPhone.isEmpty()) {
            if (userPhone.startsWith("+62")) {
                etWa.setText(userPhone.substring(3).trim());
            } else if (userPhone.startsWith("62")) {
                etWa.setText(userPhone.substring(2).trim());
            } else if (userPhone.startsWith("0")) {
                etWa.setText(userPhone.substring(1).trim());
            } else {
                etWa.setText(userPhone.trim());
            }
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnBatal.setOnClickListener(v -> dialog.dismiss());

        btnKirim.setOnClickListener(v -> {
            String namaKost = etNama.getText() != null ? etNama.getText().toString().trim() : "";
            String alamatKost = etAlamat.getText() != null ? etAlamat.getText().toString().trim() : "";
            String wa = etWa.getText() != null ? etWa.getText().toString().trim() : "";
            String catatan = etCatatan.getText() != null ? etCatatan.getText().toString().trim() : "";

            if (namaKost.isEmpty()) {
                etNama.setError("Nama properti kost wajib diisi");
                etNama.requestFocus();
                return;
            }
            if (alamatKost.isEmpty()) {
                etAlamat.setError("Alamat properti kost wajib diisi");
                etAlamat.requestFocus();
                return;
            }
            if (wa.isEmpty()) {
                etWa.setError("Nomor WhatsApp wajib diisi");
                etWa.requestFocus();
                return;
            }

            // Normalisasi nomor whatsapp
            String fullWa = wa.startsWith("0") ? "+62" + wa.substring(1) : (wa.startsWith("+62") ? wa : "+62" + wa);

            StringBuilder noteBuilder = new StringBuilder();
            noteBuilder.append("Nama Kost: ").append(namaKost)
                    .append(" | Alamat: ").append(alamatKost)
                    .append(" | WA: ").append(fullWa);
            if (!catatan.isEmpty()) {
                noteBuilder.append(" | Catatan: ").append(catatan);
            }

            pbLoading.setVisibility(View.VISIBLE);
            btnKirim.setEnabled(false);
            btnBatal.setEnabled(false);

            int userId = sessionManager.getUserId();
            userRepository.submitOwnerVerification(userId, noteBuilder.toString(), new DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean ok) {
                    if (!isAdded()) return;
                    pbLoading.setVisibility(View.GONE);
                    dialog.dismiss();

                    // Simpan nomor whatsapp ke session jika belum ada
                    if (sessionManager.getUserPhone().isEmpty()) {
                        sessionManager.setUserPhone(fullWa);
                    }

                    sessionManager.updateVerificationStatus(VerificationStatus.PENDING, null, "");
                    new com.carikostkita.data.repository.ActivityLogRepository(requireContext())
                            .log(userId, sessionManager.getUserName(), "PENGAJUAN_PEMILIK",
                                    "Pengguna mengajukan verifikasi pemilik untuk kost: " + namaKost, "USER", userId);
                    populateUserData();
                    AppDialogHelper.showSuccessDialog(requireContext(),
                            "Pengajuan Terkirim!",
                            "Pengajuan verifikasi pemilik untuk \"" + namaKost + "\" telah dikirimkan ke Developer/Admin. Kami akan segera memproses akun Anda.",
                            null);
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;
                    pbLoading.setVisibility(View.GONE);
                    btnKirim.setEnabled(true);
                    btnBatal.setEnabled(true);
                    AppDialogHelper.showErrorDialog(requireContext(), "Gagal Mengajukan", message);
                }
            });
        });

        dialog.show();
    }

    private void applyAvatar(String avatarKey) {
        UserAvatarHelper.loadAvatar(ivAvatar, avatarKey);
    }

}
