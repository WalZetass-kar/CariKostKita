package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.local.dao.UserDAO;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import com.carikostkita.data.model.VerificationStatus;
import org.mindrot.jbcrypt.BCrypt;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {
    private final UserDAO userDAO;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public UserRepository(Context context) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        this.userDAO = new UserDAO(dbHelper);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void login(String email, String plainPassword, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (email == null || email.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
                    postError(callback, "Email dan password wajib diisi");
                    return;
                }

                User user = userDAO.findByEmail(email.trim());
                if (user == null) {
                    postError(callback, "Akun tidak ditemukan. Silakan periksa kembali email Anda.");
                    return;
                }

                boolean passwordMatches = false;
                try {
                    passwordMatches = BCrypt.checkpw(plainPassword, user.getPassword());
                } catch (Exception e) {
                    passwordMatches = plainPassword.equals(user.getPassword());
                }

                if (!passwordMatches) {
                    postError(callback, "Password yang Anda masukkan salah.");
                    return;
                }

                if (!user.isActive()) {
                    postError(callback, "Akun Anda dinonaktifkan (disuspend) oleh sistem karena pelanggaran aturan.");
                    return;
                }

                User finalUser = user;
                mainHandler.post(() -> callback.onSuccess(finalUser));
            } catch (Exception e) {
                postError(callback, "Terjadi kesalahan saat login: " + e.getMessage());
            }
        });
    }

    public void loginWithGoogle(String email, String nama, String avatarUrl, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (email == null || !email.contains("@")) {
                    postError(callback, "Akun Google tidak valid");
                    return;
                }
                User user = userDAO.findOrCreateGoogleUser(email, nama, avatarUrl);
                mainHandler.post(() -> callback.onSuccess(user));
            } catch (Exception e) {
                postError(callback, "Gagal login dengan Google: " + e.getMessage());
            }
        });
    }

    public void register(String nama, String email, String plainPassword, DataCallback<User> callback) {
        register(nama, email, plainPassword, "", Role.USER, false, "", callback);
    }

    public void register(String nama, String email, String plainPassword, String noHp, Role role, DataCallback<User> callback) {
        register(nama, email, plainPassword, noHp, role, false, "", callback);
    }

    public void register(String nama, String email, String plainPassword, String noHp, Role role, boolean requestOwner, String ownerNote, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (nama == null || nama.trim().isEmpty()) {
                    postError(callback, "Nama lengkap tidak boleh kosong");
                    return;
                }
                if (email == null || !email.contains("@")) {
                    postError(callback, "Format email tidak valid");
                    return;
                }
                if (plainPassword == null || plainPassword.length() < 6) {
                    postError(callback, "Password minimal 6 karakter");
                    return;
                }

                User existing = userDAO.findByEmail(email.trim());
                if (existing != null) {
                    postError(callback, "Email sudah terdaftar. Silakan gunakan email lain.");
                    return;
                }

                String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
                User newUser = new User();
                newUser.setNama(nama.trim());
                newUser.setEmail(email.trim().toLowerCase());
                newUser.setPassword(hashedPassword);
                newUser.setNoHp(noHp != null ? noHp.trim() : "");
                
                // Security rule: Normal registration is ALWAYS USER.
                // If requested owner, set verificationStatus to PENDING.
                newUser.setRole(Role.USER);
                if (requestOwner || role == Role.PEMILIK_KOST) {
                    newUser.setVerificationStatus(VerificationStatus.PENDING);
                    newUser.setPengajuanCatatan(ownerNote != null && !ownerNote.trim().isEmpty() ? ownerNote.trim() : "Pengajuan saat pendaftaran");
                } else {
                    newUser.setVerificationStatus(VerificationStatus.NONE);
                }

                long id = userDAO.insert(newUser);
                if (id == -1) {
                    postError(callback, "Gagal mendaftarkan akun ke database");
                    return;
                }

                newUser.setIdUser((int) id);
                mainHandler.post(() -> callback.onSuccess(newUser));
            } catch (Exception e) {
                postError(callback, "Terjadi kesalahan registrasi: " + e.getMessage());
            }
        });
    }

    public void getUserById(int idUser, DataCallback<User> callback) {
        executor.execute(() -> {
            try {
                User user = userDAO.findById(idUser);
                if (user != null) {
                    mainHandler.post(() -> callback.onSuccess(user));
                } else {
                    postError(callback, "Data pengguna tidak ditemukan");
                }
            } catch (Exception e) {
                postError(callback, "Gagal mengambil data user: " + e.getMessage());
            }
        });
    }

    public void updateProfile(int idUser, String nama, String noHp, String bio, String avatarUrl, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (nama == null || nama.trim().isEmpty()) {
                    postError(callback, "Nama tidak boleh kosong");
                    return;
                }
                boolean ok = userDAO.updateProfile(idUser, nama.trim(), noHp != null ? noHp.trim() : "", bio, avatarUrl);
                if (ok) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal memperbarui profil pengguna");
                }
            } catch (Exception e) {
                postError(callback, "Kesalahan update profil: " + e.getMessage());
            }
        });
    }

    public void submitOwnerVerification(int idUser, String catatan, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean ok = userDAO.submitOwnerVerification(idUser, catatan);
                if (ok) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal mengajukan verifikasi pemilik kost");
                }
            } catch (Exception e) {
                postError(callback, "Error pengajuan: " + e.getMessage());
            }
        });
    }

    public void approveOwnerVerification(int idUser, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean ok = userDAO.approveOwnerVerification(idUser);
                if (ok) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal menyetujui pengajuan");
                }
            } catch (Exception e) {
                postError(callback, "Error approval: " + e.getMessage());
            }
        });
    }

    public void rejectOwnerVerification(int idUser, String alasan, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean ok = userDAO.rejectOwnerVerification(idUser, alasan);
                if (ok) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal menolak pengajuan");
                }
            } catch (Exception e) {
                postError(callback, "Error rejection: " + e.getMessage());
            }
        });
    }

    public void getPendingVerifications(DataCallback<List<User>> callback) {
        executor.execute(() -> {
            try {
                List<User> list = userDAO.getPendingVerifications();
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal memuat daftar pengajuan: " + e.getMessage());
            }
        });
    }

    public void changePassword(int idUser, String oldPassword, String newPassword, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                User user = userDAO.findById(idUser);
                if (user == null) {
                    postError(callback, "Akun tidak ditemukan");
                    return;
                }

                boolean match = false;
                try {
                    match = BCrypt.checkpw(oldPassword, user.getPassword());
                } catch (Exception e) {
                    match = oldPassword.equals(user.getPassword());
                }

                if (!match) {
                    postError(callback, "Kata sandi lama tidak sesuai");
                    return;
                }

                if (newPassword == null || newPassword.length() < 6) {
                    postError(callback, "Kata sandi baru minimal 6 karakter");
                    return;
                }

                String newHashed = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
                boolean ok = userDAO.updatePassword(idUser, newHashed);
                if (ok) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal memperbarui kata sandi");
                }
            } catch (Exception e) {
                postError(callback, "Kesalahan ganti kata sandi: " + e.getMessage());
            }
        });
    }

    public void requireRevisionOwnerVerification(int idUser, String catatanRevisi, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean ok = userDAO.requireRevisionOwnerVerification(idUser, catatanRevisi);
                if (ok) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal meminta revisi");
                }
            } catch (Exception e) {
                postError(callback, "Error revision: " + e.getMessage());
            }
        });
    }

    public void setUserActiveStatus(int idUser, boolean isActive, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean ok = userDAO.setUserActiveStatus(idUser, isActive);
                if (ok) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    postError(callback, "Gagal mengubah status aktif user");
                }
            } catch (Exception e) {
                postError(callback, "Error set status: " + e.getMessage());
            }
        });
    }

    public void getAllUsers(DataCallback<List<User>> callback) {
        executor.execute(() -> {
            try {
                List<User> list = userDAO.findAllUsers();
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal memuat pengguna: " + e.getMessage());
            }
        });
    }

    public static class UserStats {
        public final int totalUsers;
        public final int totalOwners;
        public final int totalPencari;
        public final int totalPemilik;
        public final int pendingVerifikasi;

        public UserStats(int totalUsers, int totalOwners, int pendingVerifikasi) {
            this.totalUsers = totalUsers;
            this.totalOwners = totalOwners;
            this.totalPemilik = totalOwners;
            this.totalPencari = Math.max(0, totalUsers - totalOwners);
            this.pendingVerifikasi = pendingVerifikasi;
        }

        public UserStats(int totalUsers, int totalOwners) {
            this(totalUsers, totalOwners, 0);
        }
    }

    public void getUserStats(DataCallback<UserStats> callback) {
        executor.execute(() -> {
            try {
                int total = userDAO.getTotalUserCount();
                int owners = userDAO.getTotalOwnerCount();
                int pending = userDAO.getPendingVerifications().size();
                UserStats stats = new UserStats(total, owners, pending);
                mainHandler.post(() -> callback.onSuccess(stats));
            } catch (Exception e) {
                postError(callback, "Gagal memuat statistik user: " + e.getMessage());
            }
        });
    }

    private <T> void postError(DataCallback<T> callback, String msg) {
        mainHandler.post(() -> callback.onError(msg));
    }
}
