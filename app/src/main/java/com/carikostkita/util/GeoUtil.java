package com.carikostkita.util;

import java.util.Locale;

/** Perhitungan & format jarak antar koordinat. */
public final class GeoUtil {

    private GeoUtil() {}

    /** Jarak garis lurus (Haversine) dalam kilometer. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** "450 m" atau "2,3 km". */
    public static String formatDistance(double km) {
        if (km < 1.0) return Math.round(km * 1000) + " m";
        return String.format(new Locale("in", "ID"), "%.1f km", km);
    }
}
