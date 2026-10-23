package com.carikostkita.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseStorageService;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Bukti verifikasi PROPERTI pemilik di bucket PRIVAT verification-docs.
 * Tidak ada KTP/selfie identitas: yang difoto adalah tampak depan properti,
 * kode verifikasi di lokasi, dan kamar/area umum.
 *
 * Nama kolom lama dipertahankan agar rilis ini tidak memerlukan migrasi DB:
 *   ktp_path         -> foto tampak depan properti
 *   selfie_path      -> foto kode verifikasi di lokasi
 *   kepemilikan_path -> foto kamar/area umum
 */
public class VerificationRepository extends BaseRepository {
    public static final String BUCKET = "verification-docs";
    // Nilai string = nama kolom lama di tabel users; lihat komentar kelas.
    public static final String DOC_FACADE = "ktp";
    public static final String DOC_CODE = "selfie";
    public static final String DOC_ROOM = "kepemilikan";

    private final SupabaseStorageService storage;

    public VerificationRepository(Context context) {
        super(context);
        storage = SupabaseClient.getInstance().createService(SupabaseStorageService.class);
    }

    /** Unggah satu bukti properti (dikompres) dan simpan path-nya di profil. Hasil: path di bucket. */
    public void upload(Uri uri, String docType, DataCallback<String> callback) {
        async("uploadVerification", callback, () -> {
            byte[] bytes;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) throw new HttpFailure("Foto tidak bisa dibaca. Pilih foto lain.");
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 2;
                Bitmap bmp = BitmapFactory.decodeStream(in, null, o);
                if (bmp == null) throw new HttpFailure("Format foto tidak didukung. Gunakan JPG atau PNG.");
                int edge = Math.max(bmp.getWidth(), bmp.getHeight());
                if (edge > 1600) {
                    float s = 1600f / edge;
                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth() * s), Math.round(bmp.getHeight() * s), true);
                    if (scaled != bmp) bmp.recycle();
                    bmp = scaled;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
                bmp.recycle();
                bytes = out.toByteArray();
            }
            String path = session.getUserUid() + "/" + storagePrefix(docType) + "_" + System.currentTimeMillis() + ".jpg";
            check("uploadVerification", storage.uploadFileBinary(BUCKET, path, "image/jpeg",
                    RequestBody.create(bytes, MediaType.parse("image/jpeg"))).execute());

            Map<String, String> f = new HashMap<>();
            f.put("id", "eq." + session.getUserUid());
            JsonObject body = new JsonObject();
            body.addProperty(docType + "_path", path);
            check("saveVerificationPath", db.patch("users", f, body).execute());
            return path;
        });
    }

    /** Prefix nama file baru yang netral; nama kolom database tetap memakai nama lama. */
    private static String storagePrefix(String docType) {
        if (DOC_FACADE.equals(docType)) return "properti";
        if (DOC_CODE.equals(docType)) return "kode";
        if (DOC_ROOM.equals(docType)) return "kamar";
        return "bukti";
    }

    /** URL bertanda tangan berlaku 10 menit, untuk ditinjau staf. */
    public void signedUrl(String path, DataCallback<String> callback) {
        async("signedUrl", callback, () -> {
            Map<String, Object> body = new HashMap<>();
            body.put("expiresIn", 600);
            JsonObject res = check("signedUrl", storage.createSignedUrl(BUCKET, path, body).execute());
            String signed = res != null ? str(res, "signedURL") : null;
            if (signed == null) throw new HttpFailure("Dokumen tidak ditemukan.");
            return com.carikostkita.BuildConfig.SUPABASE_URL + "/storage/v1" + signed;
        });
    }

    /** Path bukti properti milik user tertentu (untuk staf). Hasil: [fasad, kode, kamar]. */
    public void docPaths(String userId, DataCallback<String[]> callback) {
        async("docPaths", callback, () -> {
            Map<String, String> f = new HashMap<>();
            f.put("id", "eq." + userId);
            java.util.List<JsonObject> rows = check("docPaths", db.select("users", f, "ktp_path,selfie_path,kepemilikan_path", null).execute());
            JsonObject o = rows != null && !rows.isEmpty() ? rows.get(0) : null;
            return new String[]{str(o, "ktp_path"), str(o, "selfie_path"), str(o, "kepemilikan_path")};
        });
    }
}
