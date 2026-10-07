package com.carikostkita.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.carikostkita.BuildConfig;
import com.carikostkita.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.Calendar;

/**
 * Lembar informasi aplikasi: Tentang, Syarat Layanan, dan Kebijakan Privasi.
 * Teks syarat & privasi adalah ringkasan; versi lengkap perlu ditinjau secara hukum sebelum rilis.
 */
public final class AppInfoSheets {

    private AppInfoSheets() {}

    public static void showAbout(Context context) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.sheet_about_app, null);
        dialog.setContentView(view);

        View band = view.findViewById(R.id.about_band);
        if (band != null) band.setClipToOutline(true);
        TextView version = view.findViewById(R.id.tv_about_version);
        if (version != null) version.setText("Versi " + BuildConfig.VERSION_NAME);
        TextView copyright = view.findViewById(R.id.tv_about_copyright);
        if (copyright != null) {
            copyright.setText("© " + Calendar.getInstance().get(Calendar.YEAR) + " CariKostKita");
        }

        SettingRowBinder.bindValue(view.findViewById(R.id.about_value_1), R.drawable.ic_verified,
                "Listing dicek tim", "Kost baru tampil setelah lolos verifikasi.");
        SettingRowBinder.bindValue(view.findViewById(R.id.about_value_2), R.drawable.ic_nav_chat,
                "Langsung ke pemilik", "Chat tanpa perantara dan tanpa komisi.");
        SettingRowBinder.bindValue(view.findViewById(R.id.about_value_3), R.drawable.ic_sparkle,
                "Gratis untuk pencari", "Cari, simpan, dan bandingkan tanpa biaya.");

        SettingRowBinder.bind(view.findViewById(R.id.about_link_terms), R.drawable.ic_info,
                "Syarat Layanan", null, v -> showTerms(context));
        SettingRowBinder.bind(view.findViewById(R.id.about_link_privacy), R.drawable.ic_lock,
                "Kebijakan Privasi", null, v -> showPrivacy(context));
        SettingRowBinder.bind(view.findViewById(R.id.about_link_feedback), R.drawable.ic_email,
                "Kirim Masukan", "Ceritakan kendala atau ide kamu", v -> sendFeedback(context));
        dialog.show();
    }

    public static void showTerms(Context context) {
        if (openUrl(context, context.getString(R.string.terms_url))) return;
        showDocument(context, "Syarat Layanan",
                "1. CariKostKita adalah platform yang mempertemukan pencari kost dan pemilik kost. Kami tidak menjadi pihak dalam perjanjian sewa.\n\n"
                        + "2. Pemilik bertanggung jawab atas kebenaran informasi kost (harga, foto, fasilitas, lokasi). Listing diperiksa tim sebelum tampil, namun pencari tetap disarankan survei langsung.\n\n"
                        + "3. Dilarang memasang listing palsu, meminta pembayaran di luar kesepakatan yang jelas, atau mengirim pesan yang melecehkan. Pelanggaran dapat membuat akun dinonaktifkan.\n\n"
                        + "4. Jangan transfer uang muka sebelum melihat kost dan bertemu pemilik. Laporkan listing mencurigakan lewat tombol Laporkan di halaman kost.\n\n"
                        + "5. Ketentuan dapat diperbarui. Perubahan penting akan diberitahukan di aplikasi.");
    }

    public static void showPrivacy(Context context) {
        if (openUrl(context, context.getString(R.string.privacy_policy_url))) return;
        showDocument(context, "Kebijakan Privasi",
                "Data yang kami simpan: nama, email, nomor WhatsApp (opsional), foto profil, kost favorit, isi chat, dan lokasi yang kamu pilih.\n\n"
                        + "Kegunaan: menampilkan kost terdekat, menghubungkan kamu dengan pemilik, dan menjaga keamanan platform (moderasi laporan).\n\n"
                        + "Yang bisa melihat: nomor WhatsApp pemilik tampil di halaman kost. Data pencari hanya terlihat oleh pemilik yang kamu ajak chat dan tim moderasi.\n\n"
                        + "Lokasi: hanya dipakai di perangkat untuk menghitung jarak dan tidak dibagikan ke pengguna lain.\n\n"
                        + "Hak kamu: meminta salinan atau penghapusan data dengan menghubungi " + context.getString(R.string.support_email) + ".");
    }

    /** Buka versi lengkap di browser bila URL publik sudah diisi di strings.xml. */
    private static boolean openUrl(Context context, String url) {
        if (url == null || url.trim().isEmpty()) return false;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void showDocument(Context context, String title, String body) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(body)
                .setPositiveButton("Mengerti", null)
                .show();
    }

    public static void sendFeedback(Context context) {
        Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + context.getString(R.string.support_email)));
        email.putExtra(Intent.EXTRA_SUBJECT, "Masukan CariKostKita v" + BuildConfig.VERSION_NAME);
        try {
            context.startActivity(email);
        } catch (Exception e) {
            Toast.makeText(context, "Tidak ada aplikasi email di perangkat ini", Toast.LENGTH_SHORT).show();
        }
    }
}
