package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.ReportCreateRequest;
import com.carikostkita.data.remote.dto.ReportResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/** User-scoped report submission; the server derives reporter identity. */
public interface FastApiReportService {
    @POST("api/v1/reports")
    Call<ReportResponse> createReport(
            @Header("Authorization") String bearerToken,
            @Body ReportCreateRequest body
    );
}
