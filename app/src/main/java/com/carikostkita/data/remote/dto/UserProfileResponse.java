package com.carikostkita.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Response wrapper for GET/PATCH /api/v1/me/profile. */
public class UserProfileResponse {
    @SerializedName("data")
    public UserDto data;
}
