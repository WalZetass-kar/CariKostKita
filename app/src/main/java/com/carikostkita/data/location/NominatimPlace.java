package com.carikostkita.data.location;

import com.google.gson.annotations.SerializedName;

public class NominatimPlace {

    @SerializedName("place_id")
    private long placeId;

    @SerializedName("lat")
    private String lat;

    @SerializedName("lon")
    private String lon;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("name")
    private String name;

    @SerializedName("address")
    private NominatimAddress address;

    public long getPlaceId() {
        return placeId;
    }

    public double getLatitude() {
        try {
            return lat != null ? Double.parseDouble(lat) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public double getLongitude() {
        try {
            return lon != null ? Double.parseDouble(lon) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public String getDisplayName() {
        return displayName != null ? displayName : "";
    }

    public String getName() {
        return name != null ? name : "";
    }

    public NominatimAddress getAddress() {
        return address;
    }
}
