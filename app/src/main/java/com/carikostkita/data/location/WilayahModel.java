package com.carikostkita.data.location;

import com.google.gson.annotations.SerializedName;

/**
 * Model representasi entitas wilayah administrasi Indonesia
 * (Provinsi, Kabupaten/Kota, Kecamatan, dan Kelurahan/Desa).
 */
public class WilayahModel {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("province_id")
    private String provinceId;

    @SerializedName("regency_id")
    private String regencyId;

    @SerializedName("district_id")
    private String districtId;

    public WilayahModel() {
    }

    public WilayahModel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(String provinceId) {
        this.provinceId = provinceId;
    }

    public String getRegencyId() {
        return regencyId;
    }

    public void setRegencyId(String regencyId) {
        this.regencyId = regencyId;
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    /**
     * Konversi teks ALL CAPS resmi (contoh: "KABUPATEN BOGOR", "BUKIT RAYA")
     * menjadi Title Case yang rapi dan mudah dibaca (contoh: "Kabupaten Bogor", "Bukit Raya").
     */
    public String getFormattedName() {
        if (name == null || name.trim().isEmpty()) return "";
        return toTitleCase(name);
    }

    public static String toTitleCase(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        String[] words = input.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;
            // Singkatan khusus
            if (word.equalsIgnoreCase("dki") || word.equalsIgnoreCase("di")) {
                sb.append(word.toUpperCase());
            } else if (word.equalsIgnoreCase("dan") || word.equalsIgnoreCase("ke")) {
                sb.append(i == 0 ? Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase() : word.toLowerCase());
            } else {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedName();
    }
}
