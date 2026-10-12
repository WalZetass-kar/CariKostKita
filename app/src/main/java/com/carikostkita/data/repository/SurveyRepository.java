package com.carikostkita.data.repository;

import android.content.Context;
import com.carikostkita.data.model.SurveyRequest;
import com.carikostkita.data.remote.FastApiSurveyService;
import com.carikostkita.data.remote.SupabaseClient;
import com.carikostkita.data.remote.dto.SurveyCreateRequest;
import com.carikostkita.data.remote.dto.SurveyDto;
import com.carikostkita.data.remote.dto.SurveyListResponse;
import com.carikostkita.data.remote.dto.SurveyResponse;
import com.carikostkita.data.remote.dto.SurveyStatusUpdateRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

/** Survey operations use FastAPI; identities and allowed transitions are server-derived. */
public class SurveyRepository extends BaseRepository {

    private final FastApiSurveyService service;

    public SurveyRepository(Context context) {
        super(context);
        service = SupabaseClient.getInstance().createSurveyService();
    }

    private String bearer() throws IOException {
        String token = session.getAccessToken();
        if (token == null || token.isEmpty()) throw new IOException("Sesi kamu sudah berakhir. Silakan masuk lagi.");
        if (session.isTokenExpired()) token = SupabaseClient.getInstance().refreshTokenBlocking(token);
        if (token == null || token.isEmpty()) throw new IOException("Sesi kamu sudah berakhir. Silakan masuk lagi.");
        return "Bearer " + token;
    }

    private static long numericId(String value, String label) throws IOException {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException("non-positive ID");
            return id;
        } catch (Exception e) {
            throw new IOException(label + " tidak valid.", e);
        }
    }

    public void request(String kostId, String jadwalIso, String catatan, DataCallback<SurveyRequest> callback) {
        async("requestSurvey", callback, () -> {
            Response<SurveyResponse> response = service.create(
                    bearer(), new SurveyCreateRequest(numericId(kostId, "ID kost"), jadwalIso, catatan)
            ).execute();
            SurveyResponse body = check("requestSurvey", response);
            if (body == null || body.data == null) return null;
            new AnalyticsRepository(context).log(AnalyticsRepository.SURVEY_REQUEST, kostId);
            return map(body.data);
        });
    }

    /** Scope is a view filter; the server always applies verified participant ID and database RLS. */
    public void list(boolean asSeeker, DataCallback<List<SurveyRequest>> callback) {
        async("listSurveys", callback, () -> {
            Response<SurveyListResponse> response = service.list(
                    bearer(), asSeeker ? "seeker" : "owner", null, 1, 100
            ).execute();
            SurveyListResponse body = check("listSurveys", response);
            List<SurveyRequest> result = new ArrayList<>();
            if (body != null && body.data != null) {
                for (SurveyDto survey : body.data) result.add(map(survey));
            }
            return result;
        });
    }

    /** Latest survey by the authenticated seeker for a kost. */
    public void findMineForKost(String kostId, DataCallback<SurveyRequest> callback) {
        async("findSurvey", callback, () -> {
            Response<SurveyListResponse> response = service.list(
                    bearer(), "seeker", numericId(kostId, "ID kost"), 1, 20
            ).execute();
            SurveyListResponse body = check("findSurvey", response);
            return body == null || body.data == null || body.data.isEmpty() ? null : map(body.data.get(0));
        });
    }

    public void updateStatus(String id, String status, String alasan, DataCallback<Boolean> callback) {
        async("updateSurvey", callback, () -> {
            Response<SurveyResponse> response = service.updateStatus(
                    bearer(), numericId(id, "ID survei"), new SurveyStatusUpdateRequest(status, alasan)
            ).execute();
            SurveyResponse body = check("updateSurvey", response);
            return body != null && body.data != null;
        });
    }

    private static SurveyRequest map(SurveyDto dto) {
        SurveyRequest survey = new SurveyRequest();
        survey.id = dto.id;
        survey.kostId = dto.kostId;
        survey.namaKost = dto.namaKost;
        survey.pencariId = dto.pencariId;
        survey.ownerId = dto.ownerId;
        survey.jadwal = dto.jadwal;
        survey.catatan = dto.catatan;
        survey.status = dto.status;
        survey.alasan = dto.alasan;
        survey.createdAt = dto.createdAt;
        return survey;
    }
}
