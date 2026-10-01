package com.carikostkita.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.local.dao.ActivityLogDAO;
import com.carikostkita.data.local.dao.FasilitasDAO;
import com.carikostkita.data.local.dao.FavoritDAO;
import com.carikostkita.data.local.dao.KostDAO;
import com.carikostkita.data.local.dao.WilayahDAO;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.FotoKost;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.model.KostFilterCriteria;
import com.carikostkita.data.model.KostVerificationStatus;
import com.carikostkita.data.model.StatusKost;
import com.carikostkita.data.model.Wilayah;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KostRepository {
    private final KostDAO kostDAO;
    private final FavoritDAO favoritDAO;
    private final FasilitasDAO fasilitasDAO;
    private final WilayahDAO wilayahDAO;
    private final ActivityLogDAO activityLogDAO;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public static class AdminStats {
        public int totalKost;
        public int totalTersedia;
        public int totalPenuh;
        public int totalPending;
        public int totalFasilitas;

        public AdminStats(int totalKost, int totalTersedia, int totalPenuh, int totalPending, int totalFasilitas) {
            this.totalKost = totalKost;
            this.totalTersedia = totalTersedia;
            this.totalPenuh = totalPenuh;
            this.totalPending = totalPending;
            this.totalFasilitas = totalFasilitas;
        }
    }

    public static class PemilikStats {
        public int totalKost;
        public int totalAktif;
        public int totalPending;
        public int totalRevisi;
        public int totalTersedia;
        public int totalTerisi;

        public PemilikStats(int totalKost, int totalAktif, int totalPending, int totalRevisi, int totalTersedia, int totalTerisi) {
            this.totalKost = totalKost;
            this.totalAktif = totalAktif;
            this.totalPending = totalPending;
            this.totalRevisi = totalRevisi;
            this.totalTersedia = totalTersedia;
            this.totalTerisi = totalTerisi;
        }
    }

    public KostRepository(Context context) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        this.kostDAO = new KostDAO(dbHelper);
        this.favoritDAO = new FavoritDAO(dbHelper, kostDAO);
        this.fasilitasDAO = new FasilitasDAO(dbHelper);
        this.wilayahDAO = new WilayahDAO(dbHelper);
        this.activityLogDAO = new ActivityLogDAO(dbHelper);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getAllActiveKost(int currentUserId, DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                List<Kost> list = kostDAO.findAllActive();
                if (currentUserId > 0) {
                    for (Kost k : list) {
                        k.setFavorite(favoritDAO.isFavorite(currentUserId, k.getIdKost()));
                    }
                }
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal memuat katalog kost: " + e.getMessage());
            }
        });
    }

    public void searchKost(String keyword, int currentUserId, DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                List<Kost> list = kostDAO.search(keyword);
                if (currentUserId > 0) {
                    for (Kost k : list) {
                        k.setFavorite(favoritDAO.isFavorite(currentUserId, k.getIdKost()));
                    }
                }
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal melakukan pencarian: " + e.getMessage());
            }
        });
    }

    public void filterKost(KostFilterCriteria criteria, int currentUserId, DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                List<Kost> list = kostDAO.filter(criteria);
                if (currentUserId > 0) {
                    for (Kost k : list) {
                        k.setFavorite(favoritDAO.isFavorite(currentUserId, k.getIdKost()));
                    }
                }
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal memfilter data kost: " + e.getMessage());
            }
        });
    }

    public void getKostDetail(int idKost, int currentUserId, DataCallback<Kost> callback) {
        executor.execute(() -> {
            try {
                Kost kost = kostDAO.findById(idKost);
                if (kost == null) {
                    postError(callback, "Detail kost tidak ditemukan.");
                    return;
                }
                if (currentUserId > 0) {
                    kost.setFavorite(favoritDAO.isFavorite(currentUserId, idKost));
                }
                mainHandler.post(() -> callback.onSuccess(kost));
            } catch (Exception e) {
                postError(callback, "Gagal memuat detail kost: " + e.getMessage());
            }
        });
    }

    public void toggleFavorite(int userId, int idKost, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (userId <= 0) {
                    postError(callback, "Silakan login terlebih dahulu untuk menyimpan favorit.");
                    return;
                }
                boolean isNowFav = favoritDAO.toggleFavorite(userId, idKost);
                mainHandler.post(() -> callback.onSuccess(isNowFav));
            } catch (Exception e) {
                postError(callback, "Gagal mengubah status favorit: " + e.getMessage());
            }
        });
    }

    public void getFavorites(int userId, DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                List<Kost> list = favoritDAO.findFavoritesByUserId(userId);
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal memuat daftar favorit: " + e.getMessage());
            }
        });
    }

    public void getAllKostForAdmin(DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                List<Kost> list = kostDAO.findAllForAdmin();
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal memuat list admin: " + e.getMessage());
            }
        });
    }

    public void getKostByPemilik(int idPemilik, DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                List<Kost> list = kostDAO.findByPemilik(idPemilik);
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal memuat kost milik Anda: " + e.getMessage());
            }
        });
    }

    public void saveKost(Kost kost, List<Integer> fasilitasIds, DataCallback<Long> callback) {
        saveKostWithFotos(kost, fasilitasIds, null, callback);
    }

    public void saveKostWithFotos(Kost kost, List<Integer> fasilitasIds, List<FotoKost> fotos, DataCallback<Long> callback) {
        executor.execute(() -> {
            try {
                long id = kostDAO.insert(kost, fasilitasIds);
                if (id == -1) {
                    postError(callback, "Gagal menyimpan data kost.");
                } else {
                    if (fotos != null && !fotos.isEmpty()) {
                        kostDAO.replaceKostFotos((int) id, fotos);
                    }
                    mainHandler.post(() -> callback.onSuccess(id));
                }
            } catch (Exception e) {
                postError(callback, "Kesalahan simpan kost: " + e.getMessage());
            }
        });
    }

    public void updateKost(Kost kost, List<Integer> fasilitasIds, DataCallback<Boolean> callback) {
        updateKostWithFotos(kost, fasilitasIds, null, callback);
    }

    public void updateKostWithFotos(Kost kost, List<Integer> fasilitasIds, List<FotoKost> fotos, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean ok = kostDAO.update(kost, fasilitasIds);
                if (ok && fotos != null) {
                    kostDAO.replaceKostFotos(kost.getIdKost(), fotos);
                }
                mainHandler.post(() -> callback.onSuccess(ok));
            } catch (Exception e) {
                postError(callback, "Kesalahan update kost: " + e.getMessage());
            }
        });
    }

    public void updateStatus(int idKost, StatusKost status, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean ok = kostDAO.updateStatus(idKost, status);
                mainHandler.post(() -> callback.onSuccess(ok));
            } catch (Exception e) {
                postError(callback, "Gagal mengubah status kost: " + e.getMessage());
            }
        });
    }

    public void getAllFasilitas(DataCallback<List<Fasilitas>> callback) {
        executor.execute(() -> {
            try {
                List<Fasilitas> list = fasilitasDAO.findAll();
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal memuat fasilitas: " + e.getMessage());
            }
        });
    }

    public void getAllWilayah(DataCallback<List<Wilayah>> callback) {
        executor.execute(() -> {
            try {
                List<Wilayah> list = wilayahDAO.findAll();
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal memuat wilayah: " + e.getMessage());
            }
        });
    }

    public void getPendingKosts(DataCallback<List<Kost>> callback) {
        executor.execute(() -> {
            try {
                List<Kost> list = kostDAO.findPendingKosts();
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                postError(callback, "Gagal memuat antrean verifikasi kost: " + e.getMessage());
            }
        });
    }

    public void updateKostVerification(int idKost, KostVerificationStatus status, String catatanRevisi, int adminId, String adminName, DataCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean ok = kostDAO.updateVerificationStatus(idKost, status, catatanRevisi);
                if (ok) {
                    activityLogDAO.logAction(
                            adminId,
                            adminName != null ? adminName : "Admin",
                            "VERIFIKASI_KOST",
                            "Kost #" + idKost + " diubah status verifikasi menjadi " + status.getDisplayName() + (catatanRevisi != null && !catatanRevisi.isEmpty() ? " (" + catatanRevisi + ")" : ""),
                            "KOST",
                            idKost
                    );
                }
                mainHandler.post(() -> callback.onSuccess(ok));
            } catch (Exception e) {
                postError(callback, "Gagal memperbarui verifikasi kost: " + e.getMessage());
            }
        });
    }

    public void updateVerificationStatus(int idKost, KostVerificationStatus status, String catatanRevisi, DataCallback<Boolean> callback) {
        updateKostVerification(idKost, status, catatanRevisi, 0, "Admin", callback);
    }

    public void getAdminStats(DataCallback<AdminStats> callback) {
        executor.execute(() -> {
            try {
                int total = kostDAO.countTotal();
                int tersedia = kostDAO.countByStatus(StatusKost.TERSEDIA);
                int penuh = kostDAO.countByStatus(StatusKost.PENUH);
                int pending = kostDAO.countPendingVerification();
                int totalFasilitas = fasilitasDAO.findAll().size();
                AdminStats stats = new AdminStats(total, tersedia, penuh, pending, totalFasilitas);
                mainHandler.post(() -> callback.onSuccess(stats));
            } catch (Exception e) {
                postError(callback, "Gagal memuat statistik admin: " + e.getMessage());
            }
        });
    }

    public void getPemilikStats(int idPemilik, DataCallback<PemilikStats> callback) {
        executor.execute(() -> {
            try {
                List<Kost> myKosts = kostDAO.findByPemilik(idPemilik);
                int total = myKosts.size();
                int aktif = 0;
                int pending = 0;
                int revisi = 0;
                int tersedia = 0;
                int terisi = 0;
                for (Kost k : myKosts) {
                    if (k.getVerificationStatus() == KostVerificationStatus.APPROVED) {
                        if (k.getStatus() == StatusKost.TERSEDIA) aktif++;
                    } else if (k.getVerificationStatus() == KostVerificationStatus.PENDING) {
                        pending++;
                    } else if (k.getVerificationStatus() == KostVerificationStatus.REVISION_REQUIRED) {
                        revisi++;
                    }
                    tersedia += k.getKamarTersedia();
                    terisi += k.getKamarTerisi();
                }
                PemilikStats stats = new PemilikStats(total, aktif, pending, revisi, tersedia, terisi);
                mainHandler.post(() -> callback.onSuccess(stats));
            } catch (Exception e) {
                postError(callback, "Gagal memuat statistik pemilik: " + e.getMessage());
            }
        });
    }

    private <T> void postError(DataCallback<T> callback, String msg) {
        mainHandler.post(() -> callback.onError(msg));
    }
}
