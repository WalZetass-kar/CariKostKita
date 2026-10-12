package com.carikostkita.data.repository;

import android.content.Context;
import com.carikostkita.data.remote.FastApiBlockService;
import com.carikostkita.data.remote.FastApiSafetyService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.dto.BlockStatusResponse;
import com.carikostkita.data.remote.dto.UserReportCreateRequest;
import com.carikostkita.data.remote.dto.UserReportResponse;
import java.io.IOException;
import java.util.HashMap;
import retrofit2.Response;

/** Blokir & laporkan pengguna, serta penghapusan akun sendiri. */
public class SafetyRepository extends BaseRepository {

    private final FastApiBlockService blockService;
    private final FastApiSafetyService safetyService;

    public static final String[] USER_REPORT_REASONS = {
            "Penipuan / minta transfer di luar kesepakatan",
            "Pelecehan atau kata-kata kasar",
            "Spam atau promosi",
            "Mengaku sebagai orang lain",
            "Lainnya"
    };

    public SafetyRepository(Context context) {
        super(context);
        this.blockService = SupabaseClient.getInstance().createBlockService();
        this.safetyService = SupabaseClient.getInstance().createSafetyService();
    }

    private String blockBearer() throws IOException {
        String token = session.getAccessToken();
        if (token == null || token.isEmpty()) throw new IOException("Sesi kamu sudah berakhir. Silakan masuk lagi.");
        if (session.isTokenExpired()) token = SupabaseClient.getInstance().refreshTokenBlocking(token);
        if (token == null || token.isEmpty()) throw new IOException("Sesi kamu sudah berakhir. Silakan masuk lagi.");
        return "Bearer " + token;
    }

    private static long appUserId(String value) throws IOException {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException("non-positive ID");
            return id;
        } catch (Exception e) {
            throw new IOException("ID pengguna tidak valid.", e);
        }
    }

    /** True bila salah satu pihak memblokir pihak lain. */
    public void isBlockedWith(String otherId, DataCallback<Boolean> callback) {
        async("isBlocked", callback, () -> {
            Response<BlockStatusResponse> response = blockService
                    .readStatus(blockBearer(), appUserId(otherId)).execute();
            BlockStatusResponse body = check("isBlocked", response);
            return body != null && (Boolean.TRUE.equals(body.blockedByMe) || Boolean.TRUE.equals(body.blockedMe));
        });
    }

    /** True bila SAYA yang memblokir (hanya saya yang bisa membuka blokir). */
    public void isBlockedByMe(String otherId, DataCallback<Boolean> callback) {
        async("isBlockedByMe", callback, () -> {
            Response<BlockStatusResponse> response = blockService
                    .readStatus(blockBearer(), appUserId(otherId)).execute();
            BlockStatusResponse body = check("isBlockedByMe", response);
            return body != null && Boolean.TRUE.equals(body.blockedByMe);
        });
    }

    public void block(String otherId, DataCallback<Boolean> callback) {
        async("block", callback, () -> {
            Response<BlockStatusResponse> response = blockService
                    .block(blockBearer(), appUserId(otherId)).execute();
            BlockStatusResponse body = check("block", response);
            return body != null && Boolean.TRUE.equals(body.blocked);
        });
    }

    public void unblock(String otherId, DataCallback<Boolean> callback) {
        async("unblock", callback, () -> {
            Response<BlockStatusResponse> response = blockService
                    .unblock(blockBearer(), appUserId(otherId)).execute();
            BlockStatusResponse body = check("unblock", response);
            return body != null && Boolean.FALSE.equals(body.blocked);
        });
    }

    public void reportUser(String reportedId, String chatId, String reason, String detail, DataCallback<Boolean> callback) {
        async("reportUser", callback, () -> {
            if (chatId == null) throw new IOException("Percakapan laporan tidak valid.");
            long verifiedChatId = appUserId(chatId);
            Response<UserReportResponse> response = safetyService.reportUser(
                    blockBearer(), verifiedChatId,
                    new UserReportCreateRequest(appUserId(reportedId), reason, detail)
            ).execute();
            UserReportResponse body = check("reportUser", response);
            return body != null && body.submitted;
        });
    }

    /** Hapus akun & semua data milik akun (fungsi SQL delete_my_account). */
    public void deleteMyAccount(DataCallback<Boolean> callback) {
        async("deleteMyAccount", callback, () -> {
            check("deleteMyAccount", db.rpc("delete_my_account", new HashMap<>()).execute());
            session.logout();
            return true;
        });
    }
}
