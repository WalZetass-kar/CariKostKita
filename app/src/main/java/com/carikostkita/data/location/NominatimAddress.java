package com.carikostkita.data.location;

import com.google.gson.annotations.SerializedName;

public class NominatimAddress {

    @SerializedName("road")
    private String road;

    @SerializedName("house_number")
    private String houseNumber;

    @SerializedName("suburb")
    private String suburb;

    @SerializedName("village")
    private String village;

    @SerializedName("neighbourhood")
    private String neighbourhood;

    @SerializedName("quarter")
    private String quarter;

    @SerializedName("city_district")
    private String cityDistrict;

    @SerializedName("subdistrict")
    private String subdistrict;

    @SerializedName("municipality")
    private String municipality;

    @SerializedName("city")
    private String city;

    @SerializedName("town")
    private String town;

    @SerializedName("county")
    private String county;

    @SerializedName("state")
    private String state;

    @SerializedName("postcode")
    private String postcode;

    @SerializedName("country")
    private String country;

    public String getRoad() {
        return road;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public String getStreetName() {
        if (road != null && !road.trim().isEmpty()) {
            if (houseNumber != null && !houseNumber.trim().isEmpty()) {
                return road.trim() + " No. " + houseNumber.trim();
            }
            return road.trim();
        }
        return "";
    }

    public String getKelurahan() {
        if (suburb != null && !suburb.trim().isEmpty()) return suburb.trim();
        if (village != null && !village.trim().isEmpty()) return village.trim();
        if (neighbourhood != null && !neighbourhood.trim().isEmpty()) return neighbourhood.trim();
        if (quarter != null && !quarter.trim().isEmpty()) return quarter.trim();
        return "";
    }

    public String getKecamatan() {
        if (cityDistrict != null && !cityDistrict.trim().isEmpty()) return cityDistrict.trim();
        if (subdistrict != null && !subdistrict.trim().isEmpty()) return subdistrict.trim();
        if (municipality != null && !municipality.trim().isEmpty()) return municipality.trim();
        return "";
    }

    public String getKota() {
        if (city != null && !city.trim().isEmpty()) return city.trim();
        if (town != null && !town.trim().isEmpty()) return town.trim();
        if (county != null && !county.trim().isEmpty()) return county.trim();
        return "";
    }

    public String getProvinsi() {
        return state != null ? state.trim() : "";
    }

    public String getPostcode() {
        return postcode != null ? postcode.trim() : "";
    }

    public String getCountry() {
        return country != null ? country.trim() : "";
    }
}
