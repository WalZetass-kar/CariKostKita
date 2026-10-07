package com.carikostkita.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import org.junit.Test;

public class ErrorMessagesTest {

    @Test
    public void codes_mapToFriendlyMessages() {
        assertEquals(ErrorMessages.SESSION_EXPIRED, ErrorMessages.fromCode("t", 401, ""));
        assertEquals(ErrorMessages.FORBIDDEN, ErrorMessages.fromCode("t", 403, ""));
        assertEquals(ErrorMessages.FORBIDDEN, ErrorMessages.fromCode("t", 400, "{\"code\":\"42501\"}"));
        assertEquals(ErrorMessages.SERVER, ErrorMessages.fromCode("t", 503, ""));
    }

    @Test
    public void technicalDetailsNeverReachTheUser() {
        String msg = ErrorMessages.fromCode("t", 400, "relation kosts violates row-level security policy");
        assertTrue(!msg.contains("row-level") && !msg.contains("kosts"));
    }

    @Test
    public void networkFailureIsReportedAsOffline() {
        String msg = ErrorMessages.fromException("t", new UnknownHostException("x.supabase.co"));
        assertEquals(ErrorMessages.OFFLINE, msg);
        assertTrue(ErrorMessages.isOffline(msg));
    }
}
