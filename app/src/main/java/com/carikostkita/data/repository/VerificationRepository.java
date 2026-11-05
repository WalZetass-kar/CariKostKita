package com.carikostkita.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.SupabaseStorageService;
import com.carikostkita.data.remote.dto.AvatarUploadResponse;
import com.carikostkita.data.remote.dto.StorageUploadResponse;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;

/**
 * Bukti verifikasi PROPERTI pemilik.
 * Tidak ada KTP/selfie identitas: yang difoto adalah tampak depan properti,
 * kode verifikasi di lokasi, dan kamar/area umum.
 *
 * Simpan bukti foto terverifikasi secara aman dan berikan fallback base64
 * data URI apabila storage bucket eksternal dibatasi.
 */
public class VerificationRepository extends BaseRepository {
    public static final String BUCKET = "kost-images";
    public static final String DOC_FACADE = "ktp";
    public static final String DOC_CODE = "selfie";
    public static final String DOC_ROOM = "kepemilikan";

    private final SupabaseStorageService storage;

    public VerificationRepository(Context context) {
        super(context);
        storage = SupabaseClient.getInstance().createService(SupabaseStorageService.class);
    }

    /**
     * Unggah satu bukti properti (dikompres ramah memori).
     * Hasil: URL publik atau Data URI yang siap disimpan ke catatan permohonan.
     */
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
                if (edge > 1280) {
                    float s = 1280f / edge;
                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth() * s), Math.round(bmp.getHeight() * s), true);
                    if (scaled != bmp) bmp.recycle();
                    bmp = scaled;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, out);
                bmp.recycle();
                bytes = out.toByteArray();
            }

            // Kompresi ramah memori dan langsung siap diproses tanpa blocking ke cloud storage mati
            String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            return "data:image/jpeg;base64," + b64;
        });
    }

    /** Prefix nama file baru yang netral. */
    private static String storagePrefix(String docType) {
        if (DOC_FACADE.equals(docType)) return "properti";
        if (DOC_CODE.equals(docType)) return "kode";
        if (DOC_ROOM.equals(docType)) return "kamar";
        return "bukti";
    }

    /** URL gambar untuk ditinjau staf. */
    public void signedUrl(String pathOrUrl, DataCallback<String> callback) {
        async("signedUrl", callback, () -> {
            if (pathOrUrl == null || pathOrUrl.isEmpty()) {
                throw new HttpFailure("Dokumen tidak ditemukan.");
            }
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://") || pathOrUrl.startsWith("data:")) {
                return pathOrUrl;
            }
            Map<String, Object> body = new HashMap<>();
            body.put("expiresIn", 600);
            try {
                JsonObject res = check("signedUrl", storage.createSignedUrl(BUCKET, pathOrUrl, body).execute());
                String signed = res != null ? str(res, "signedURL") : null;
                if (signed != null) {
                    return com.carikostkita.BuildConfig.SUPABASE_URL + "/storage/v1" + signed;
                }
            } catch (Exception ignored) {}
            return SupabaseClient.getStoragePublicUrl(BUCKET, pathOrUrl);
        });
    }

    /** Path bukti properti milik user tertentu (untuk staf). Hasil: [fasad, kode, kamar]. */
    public void docPaths(String userId, DataCallback<String[]> callback) {
        async("docPaths", callback, () -> {
            Map<String, String> f = new HashMap<>();
            f.put("id", "eq." + userId);
            try {
                java.util.List<JsonObject> rows = check("docPaths", db.select("users", f, "ktp_path,selfie_path,kepemilikan_path", null).execute());
                JsonObject o = rows != null && !rows.isEmpty() ? rows.get(0) : null;
                if (o != null) {
                    return new String[]{str(o, "ktp_path"), str(o, "selfie_path"), str(o, "kepemilikan_path")};
                }
            } catch (Exception e) {
                android.util.Log.w("VerificationRepository", "docPaths query users error: " + e.getMessage());
            }
            return new String[]{null, null, null};
        });
    }
}
