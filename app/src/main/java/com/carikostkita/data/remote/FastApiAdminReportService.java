package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.ReportModerationRequest;
import com.carikostkita.data.remote.dto.AdminReportListResponse;
import com.carikostkita.data.remote.dto.PendingReportCountResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Staff-only report decisions; the API derives moderator identity from the JWT. */
public interface FastApiAdminReportService {
    @GET("api/v1/admin/reports/pending-count")
    Call<PendingReportCountResponse> readPendingCount(
            @Header("Authorization") String bearerToken
    );

    @GET("api/v1/admin/reports")
    Call<AdminReportListResponse> listReports(
            @Header("Authorization") String bearerToken,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    @PATCH("api/v1/admin/reports/{reportId}")
    Call<ResponseBody> reviewReport(
            @Header("Authorization") String bearerToken,
            @Path("reportId") String reportId,
            @Body ReportModerationRequest body
    );
}
