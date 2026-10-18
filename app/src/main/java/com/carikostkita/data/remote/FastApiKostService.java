package com.carikostkita.data.remote;

import com.carikostkita.data.remote.dto.KostDetailResponse;
import com.carikostkita.data.remote.dto.KostListResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Public catalog endpoints backed by FastAPI /api/v1/kost. */
public interface FastApiKostService {
    @GET("api/v1/kost")
    Call<KostListResponse> listPublicKosts(
            @Query("q") String query,
            @Query("provinsi") String provinsi,
            @Query("kota") String kota,
            @Query("kecamatan") String kecamatan,
            @Query("tipe_kost") String tipeKost,
            @Query("harga_min") Double hargaMin,
            @Query("harga_max") Double hargaMax,
            @Query("fasilitas") String fasilitas,
            @Query("hanya_tersedia") Boolean hanyaTersedia,
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    @GET("api/v1/kost/{kostId}")
    Call<KostDetailResponse> readPublicKost(@Path("kostId") String kostId);
}
