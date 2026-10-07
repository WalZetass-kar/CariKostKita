package com.carikostkita.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeoUtilTest {

    @Test
    public void distance_samePointIsZero() {
        assertEquals(0.0, GeoUtil.distanceKm(0.5071, 101.4478, 0.5071, 101.4478), 1e-9);
    }

    @Test
    public void distance_oneDegreeLatitudeIsAbout111Km() {
        double d = GeoUtil.distanceKm(0, 101, 1, 101);
        assertTrue("got " + d, d > 110.5 && d < 111.8);
    }

    @Test
    public void format_switchesUnitAtOneKilometer() {
        assertEquals("450 m", GeoUtil.formatDistance(0.45));
        assertEquals("2,3 km", GeoUtil.formatDistance(2.3));
    }
}
