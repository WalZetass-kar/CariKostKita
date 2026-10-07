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

    private androidx.activity.result.ActivityResultLauncher<String> docPicker;
    private String pendingDocType;
    private View pendingDocRow;
    private final java.util.Set<String> uploadedDocs = new java.util.HashSet<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        docPicker = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
            if (uri == null || pendingDocType == null || pendingDocRow == null) return;
            String type = pendingDocType;
            View row = pendingDocRow;
            android.widget.TextView status = row.findViewWithTag("status");
            if (status != null) status.setText("Mengunggah…");
            new com.carikostkita.data.repository.VerificationRepository(requireContext()).upload(uri, type, new DataCallback<String>() {
                @Override
                public void onSuccess(String path) {
                    if (!isAdded()) return;
                    uploadedDocs.add(type);
                    if (status != null) {
                        status.setText("Terunggah ✓");
                        status.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_tersedia));
                    }
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;
                    if (status != null) {
                        status.setText("Gagal: " + message);
                        status.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_penuh));
                    }
                }
            });
        });
    }

    private void bindDocRow(View dialogView, int rowId, int statusId, String docType) {
        View row = dialogView.findViewById(rowId);
        View status = dialogView.findViewById(statusId);
        if (row == null) return;
        if (status != null) status.setTag("status");
        row.setOnClickListener(v -> {
            pendingDocType = docType;
            pendingDocRow = row;
            docPicker.launch("image/*");
        });
    }

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

        com.carikostkita.util.PageHeader.bind(view, "Profil Saya", "Akun, status peran, dan preferensi");
        initViews(view);
        notificationsEnabled = sessionManager.isChatNotificationEnabled();
        populateUserData();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            populateUserData(); // Tampilkan langsung data lokal dari sessionManager tanpa delay
            long startTime = com.carikostkita.util.SkeletonHelper.markStart();
            userRepository.getUserById(sessionManager.getUserUid(), new DataCallback<User>() {
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

        // Baris menu seragam; notifikasi memakai toggle sungguhan yang tersimpan
        com.carikostkita.util.SettingRowBinder.bind(itemMyFavorites, R.drawable.ic_nav_favorite,
                "Kost Favorit", "Lihat kost yang kamu simpan", null);
        com.carikostkita.util.SettingRowBinder.bind(requireView().findViewById(R.id.item_my_surveys), R.drawable.ic_clock,
                "Jadwal Survei", "Kunjungan kost yang kamu ajukan",
                v -> startActivity(new Intent(requireContext(), com.carikostkita.ui.survey.SurveyListActivity.class)));
        com.carikostkita.util.SettingRowBinder.bind(itemChangePassword, R.drawable.ic_lock,
                "Ganti Kata Sandi", "Jaga akunmu tetap aman", null);
        com.carikostkita.util.SettingRowBinder.bindToggle(itemNotificationSettings, R.drawable.ic_bell,
                "Notifikasi Pesan", "Kabari saya saat pemilik membalas chat",
                sessionManager.isChatNotificationEnabled(), (button, checked) -> {
                    notificationsEnabled = checked;
                    sessionManager.setChatNotificationEnabled(checked);
                });
        com.carikostkita.util.SettingRowBinder.bind(requireView().findViewById(R.id.item_delete_account), R.drawable.ic_delete,
                "Hapus Akun", "Hapus akun dan semua datamu secara permanen",
                v -> com.carikostkita.util.AccountDeletion.confirm(requireActivity()));
        View itemAbout = requireView().findViewById(R.id.item_about_app);
        com.carikostkita.util.SettingRowBinder.bind(itemAbout, R.drawable.ic_info,
                "Tentang CariKostKita", "Versi " + com.carikostkita.BuildConfig.VERSION_NAME + ", syarat & privasi",
                v -> com.carikostkita.util.AppInfoSheets.showAbout(requireContext()));

        btnLogout.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                com.carikostkita.util.AuthPrompt.openLogin(requireContext());
                return;
            }
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

    private void renderNotificationStatus() {
        if (tvNotificationStatus == null) return;
        tvNotificationStatus.setText(notificationsEnabled ? "Aktif" : "Senyap");
        tvNotificationStatus.setTextColor(ContextCompat.getColor(requireContext(),
                notificationsEnabled ? R.color.status_tersedia : R.color.text_muted));
    }

    private void setGroupVisibility(View row, int visibility) {
        if (row == null || !(row.getParent() instanceof View)) return;
        View card = (View) row.getParent().getParent();
        if (card == null || !(card.getParent() instanceof ViewGroup)) return;
        ViewGroup container = (ViewGroup) card.getParent();
        card.setVisibility(visibility);
        int index = container.indexOfChild(card);
        if (index > 0) container.getChildAt(index - 1).setVisibility(visibility);
    }

    /** Tamu melihat ajakan masuk, bukan menu akun yang tidak bisa dipakai. */
    private void applyGuestMode(boolean isGuest) {
        int accountOnly = isGuest ? View.GONE : View.VISIBLE;
        btnEditProfile.setVisibility(accountOnly);
        btnEditAvatar.setVisibility(accountOnly);
        // Sembunyikan seluruh grup (judul + kartu) yang hanya berguna untuk akun
        setGroupVisibility(itemChangePassword, accountOnly);
        setGroupVisibility(itemNotificationSettings, accountOnly);
        ivAvatar.setClickable(!isGuest);

        if (isGuest) {
            btnLogout.setText("Masuk / Daftar");
            btnLogout.setIconResource(R.drawable.ic_key);
            btnLogout.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_primary));
            btnLogout.setIconTint(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.on_primary)));
            btnLogout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
            btnLogout.setStrokeWidth(0);
        } else {
            btnLogout.setText("Keluar dari Akun");
            btnLogout.setIconResource(R.drawable.ic_logout);
            int danger = ContextCompat.getColor(requireContext(), R.color.status_penuh);
            btnLogout.setTextColor(danger);
            btnLogout.setIconTint(android.content.res.ColorStateList.valueOf(danger));
            btnLogout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            btnLogout.setStrokeColor(android.content.res.ColorStateList.valueOf(danger));
            btnLogout.setStrokeWidth((int) (1.2f * getResources().getDisplayMetrics().density));
        }
    }

    private void populateUserData() {
        renderNotificationStatus();
        if (!sessionManager.isLoggedIn()) {
            applyGuestMode(true);
            tvName.setText("Halo, Tamu");
            tvEmail.setText("Masuk untuk menyimpan favorit dan chat dengan pemilik kost");
            tvPhone.setVisibility(View.GONE);
            tvRole.setText("Belum masuk");
            tvBio.setVisibility(View.GONE);
            cardOwnerVerification.setVisibility(View.GONE);
            applyAvatar(null);
            return;
        }
        applyGuestMode(false);
        tvPhone.setVisibility(View.VISIBLE);

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

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(d -> {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet == null) return;
            sheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            sheet.getLayoutParams().height = (int) (getResources().getDisplayMetrics().heightPixels * 0.92f);
            com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet);
            behavior.setSkipCollapsed(true);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        });
        uploadedDocs.clear();
        bindDocRow(dialogView, R.id.doc_ktp, R.id.doc_ktp_status, com.carikostkita.data.repository.VerificationRepository.DOC_KTP);
        bindDocRow(dialogView, R.id.doc_selfie, R.id.doc_selfie_status, com.carikostkita.data.repository.VerificationRepository.DOC_SELFIE);
        bindDocRow(dialogView, R.id.doc_kepemilikan, R.id.doc_kepemilikan_status, com.carikostkita.data.repository.VerificationRepository.DOC_KEPEMILIKAN);
        View band = dialogView.findViewById(R.id.pengajuan_band);
        if (band != null) band.setClipToOutline(true);
        com.carikostkita.util.SettingRowBinder.bindValue(dialogView.findViewById(R.id.pengajuan_benefit_1),
                R.drawable.ic_sparkle, "Pasang kost gratis", "Tanpa biaya iklan dan tanpa komisi.");
        com.carikostkita.util.SettingRowBinder.bindValue(dialogView.findViewById(R.id.pengajuan_benefit_2),
                R.drawable.ic_nav_chat, "Chat langsung dengan calon penyewa", "Pertanyaan masuk ke tab Pesan kamu.");
        com.carikostkita.util.SettingRowBinder.bindValue(dialogView.findViewById(R.id.pengajuan_benefit_3),
                R.drawable.ic_verified, "Badge pemilik terverifikasi", "Tampil di halaman kost, menambah kepercayaan.");

        btnBatal.setOnClickListener(v -> dialog.dismiss());

        btnKirim.setOnClickListener(v -> {
            String namaKost = etNama.getText() != null ? etNama.getText().toString().trim() : "";
            String alamatKost = etAlamat.getText() != null ? etAlamat.getText().toString().trim() : "";
            String wa = etWa.getText() != null ? etWa.getText().toString().trim() : "";
            String catatan = etCatatan.getText() != null ? etCatatan.getText().toString().trim() : "";

            String waDigits = wa.replaceAll("[^0-9]", "");
            if (waDigits.startsWith("62")) waDigits = waDigits.substring(2);
            if (waDigits.startsWith("0")) waDigits = waDigits.substring(1);
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
            if (!waDigits.startsWith("8") || waDigits.length() < 8 || waDigits.length() > 12) {
                etWa.setError("Masukkan nomor aktif tanpa 0 di depan, contoh 81234567890");
                etWa.requestFocus();
                return;
            }

            // Normalisasi nomor whatsapp
            if (!uploadedDocs.contains(com.carikostkita.data.repository.VerificationRepository.DOC_KTP)
                    || !uploadedDocs.contains(com.carikostkita.data.repository.VerificationRepository.DOC_SELFIE)) {
                AppDialogHelper.showInfo(requireContext(), "Lengkapi Dokumen",
                        "Unggah foto KTP dan selfie sambil memegang KTP. Ini mencegah listing palsu dan melindungi pencari kost.");
                return;
            }
            String fullWa = "+62" + waDigits;

            StringBuilder noteBuilder = new StringBuilder();
            noteBuilder.append("Nama Kost: ").append(namaKost)
                    .append(" | Alamat: ").append(alamatKost)
                    .append(" | WA: ").append(fullWa);
            if (!catatan.isEmpty()) {
                noteBuilder.append(" | Catatan: ").append(catatan);
            }

            pbLoading.setVisibility(View.VISIBLE);
            btnKirim.setText("");
            btnKirim.setEnabled(false);
            btnBatal.setEnabled(false);

            String userId = sessionManager.getUserUid();
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
                            "Pengajuan untuk \"" + namaKost + "\" sudah kami terima. Status pengajuan bisa kamu pantau di halaman ini.",
                            null);
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;
                    pbLoading.setVisibility(View.GONE);
                    btnKirim.setText("Kirim Pengajuan");
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
