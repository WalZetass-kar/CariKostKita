package com.carikostkita.util;

import android.content.Context;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Alasan moderasi WAJIB: pilih templat (konsisten antar moderator) atau tulis sendiri.
 * Alasan dikirim ke pengguna dan dicatat di log aktivitas.
 */
public final class ModerationReasons {

    public static final String[] KOST_REJECT = {
            "Foto tidak menunjukkan kamar atau bangunan kost",
            "Alamat atau titik peta tidak sesuai",
            "Harga atau fasilitas tidak masuk akal / menyesatkan",
            "Listing duplikat dari kost yang sudah ada",
            "Terindikasi penipuan atau bukan pemilik sah"
    };
    public static final String[] KOST_REVISION = {
            "Lengkapi foto kamar, kamar mandi, dan tampak depan",
            "Perbaiki titik peta agar tepat di lokasi kost",
            "Isi jumlah kamar dan kamar kosong",
            "Lengkapi rincian biaya (listrik, air, deposit)",
            "Deskripsi terlalu singkat, tambahkan aturan kost"
    };
    public static final String[] OWNER_REJECT = {
            "Foto KTP tidak terbaca atau tidak sesuai nama akun",
            "Selfie dengan KTP tidak sesuai",
            "Bukti kepemilikan / kuasa kelola tidak ada",
            "Data properti tidak dapat diverifikasi"
    };
    public static final String[] OWNER_REVISION = {
            "Unggah ulang foto KTP yang jelas",
            "Unggah selfie sambil memegang KTP",
            "Lampirkan bukti kepemilikan atau surat kuasa",
            "Lengkapi nomor WhatsApp aktif dan alamat kost"
    };
    public static final String[] USER_SUSPEND = {
            "Penipuan atau meminta transfer mencurigakan",
            "Pelecehan / kata-kata kasar kepada pengguna lain",
            "Spam atau akun palsu",
            "Melanggar Syarat Layanan berulang kali"
    };

    private ModerationReasons() {}

    public interface OnReason {
        void onReason(String reason);
    }

    public static void pick(Context context, String title, String[] templates, String positive, OnReason onReason) {
        String[] items = new String[templates.length + 1];
        System.arraycopy(templates, 0, items, 0, templates.length);
        items[templates.length] = "Tulis alasan sendiri…";
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setItems(items, (d, which) -> {
                    String prefill = which < templates.length ? templates[which] : "";
                    AppDialogHelper.showInput(context, title,
                            "Alasan ini dikirim ke pengguna dan tercatat di log moderasi. Sesuaikan bila perlu.",
                            "Tulis alasan yang jelas", prefill, positive, text -> {
                                if (text.trim().length() < 10) {
                                    Toast.makeText(context, "Alasan wajib diisi (minimal 10 karakter)", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                onReason.onReason(text.trim());
                            });
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
