package com.carikostkita.ui.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Test;

public class VerificationAdapterTest {

    @Test
    public void hoursSince_readsSupabaseUtcTimestamps() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        String thirtyHoursAgo = f.format(new Date(System.currentTimeMillis() - 30L * 3600000L)) + ".123456+00:00";
        long h = VerificationAdapter.hoursSince(thirtyHoursAgo);
        assertTrue("got " + h, h == 29 || h == 30);
    }

    @Test
    public void hoursSince_invalidIsMinusOne() {
        assertEquals(-1, VerificationAdapter.hoursSince("kemarin"));
        assertEquals(-1, VerificationAdapter.hoursSince(null));
    }
}
