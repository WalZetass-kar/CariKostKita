package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.local.dao.UserDAO;
import com.carikostkita.data.model.Role;
import com.carikostkita.data.model.User;
import org.mindrot.jbcrypt.BCrypt;
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
                    // Fallback jika password hash format lama
                    passwordMatches = plainPassword.equals(user.getPassword());
                }

                if (!passwordMatches) {
                    postError(callback, "Password yang Anda masukkan salah.");
                    return;
                }

                User finalUser = user;
                mainHandler.post(() -> callback.onSuccess(finalUser));
            } catch (Exception e) {
                postError(callback, "Terjadi kesalahan saat login: " + e.getMessage());
            }
        });
    }

    public void register(String nama, String email, String plainPassword, DataCallback<User> callback) {
        register(nama, email, plainPassword, "", Role.USER, callback);
    }

    public void register(String nama, String email, String plainPassword, String noHp, Role role, DataCallback<User> callback) {
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
                newUser.setRole(role != null ? role : Role.USER);

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

    public void updateProfile(int idUser, String nama, String noHp, String avatarUrl, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (nama == null || nama.trim().isEmpty()) {
                    postError(callback, "Nama tidak boleh kosong");
                    return;
                }
                boolean ok = userDAO.updateProfile(idUser, nama.trim(), noHp != null ? noHp.trim() : "", avatarUrl);
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

    private <T> void postError(DataCallback<T> callback, String msg) {
        mainHandler.post(() -> callback.onError(msg));
    }
}
