package com.carikostkita.data.location;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit interface untuk API Publik Wilayah Indonesia (Standar Resmi Kemendagri).
 * Menyediakan akses lengkap ke:
 * - 38 Provinsi
 * - 514 Kabupaten / Kota
 * - 7.277 Kecamatan
 * - 83.700+ Kelurahan & Desa
 */
public interface WilayahApiService {

    @GET("provinces.json")
    Call<List<WilayahModel>> getProvinces();

    @GET("regencies/{provinceId}.json")
    Call<List<WilayahModel>> getRegencies(@Path("provinceId") String provinceId);

    @GET("districts/{regencyId}.json")
    Call<List<WilayahModel>> getDistricts(@Path("regencyId") String regencyId);

    @GET("villages/{districtId}.json")
    Call<List<WilayahModel>> getVillages(@Path("districtId") String districtId);
}
