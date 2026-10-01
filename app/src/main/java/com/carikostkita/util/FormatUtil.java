package com.carikostkita.util;

import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtil {

    public static String formatRupiah(double amount) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat format = NumberFormat.getCurrencyInstance(localeID);
        format.setMaximumFractionDigits(0);
        return format.format(amount);
    }

    public static String formatPhoneForWhatsApp(String rawPhone) {
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            return "";
        }
        // Bersihkan seluruh karakter selain angka
        String clean = rawPhone.replaceAll("[^0-9]", "");
        if (clean.startsWith("0")) {
            clean = "62" + clean.substring(1);
        } else if (clean.startsWith("+62")) {
            clean = clean.substring(1);
        } else if (!clean.startsWith("62")) {
            clean = "62" + clean;
        }
        return clean;
    }

    public static boolean isValidEmail(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }
}
