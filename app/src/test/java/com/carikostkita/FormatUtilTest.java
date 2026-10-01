package com.carikostkita;

import com.carikostkita.util.FormatUtil;
import org.junit.Test;
import static org.junit.Assert.*;

public class FormatUtilTest {

    @Test
    public void testFormatRupiah() {
        String result = FormatUtil.formatRupiah(750000.0);
        assertTrue(result.contains("750.000") || result.contains("750,000"));
    }

    @Test
    public void testFormatPhoneForWhatsApp() {
        assertEquals("6281234567890", FormatUtil.formatPhoneForWhatsApp("081234567890"));
        assertEquals("6281234567890", FormatUtil.formatPhoneForWhatsApp("+6281234567890"));
        assertEquals("6281234567890", FormatUtil.formatPhoneForWhatsApp("6281234567890"));
        assertEquals("6281234567890", FormatUtil.formatPhoneForWhatsApp("0812-3456-7890"));
    }

    @Test
    public void testEmptyPhone() {
        assertEquals("", FormatUtil.formatPhoneForWhatsApp(null));
        assertEquals("", FormatUtil.formatPhoneForWhatsApp("   "));
    }
}
