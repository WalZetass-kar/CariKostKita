package com.carikostkita.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.carikostkita.data.model.Kost;

public class IntentHelper {

    public static void openWhatsApp(Context context, String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Toast.makeText(context, "Nomor WhatsApp pemilik tidak tersedia", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedPhone = FormatUtil.formatPhoneForWhatsApp(phoneNumber);
        try {
            String url = "https://wa.me/" + formattedPhone + "?text=" + Uri.encode(message != null ? message : "");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Tidak dapat membuka WhatsApp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void openGoogleMaps(Context context, double latitude, double longitude, String label) {
        if (latitude == 0 && longitude == 0) {
            Toast.makeText(context, "Koordinat lokasi belum diatur", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String encodedLabel = Uri.encode(label != null ? label : "Lokasi Kost");
            Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(" + encodedLabel + ")");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                // Fallback web browser
                Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude);
                context.startActivity(new Intent(Intent.ACTION_VIEW, webUri));
            }
        } catch (Exception e) {
            Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude);
            context.startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    public static void shareKost(Context context, Kost kost) {
        if (kost == null) return;
        String text = "Cari kost di Pekanbaru: *" + kost.getNamaKost() + "*\n" +
                "Tipe: " + kost.getTipeKost().getDisplayName() + "\n" +
                "Harga: " + kost.getFormattedHarga() + "\n" +
                "Alamat: " + kost.getFullLocation() + "\n\n" +
                "Ditemukan di aplikasi CariKostKita!";

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Bagikan Informasi Kost");
        context.startActivity(shareIntent);
    }
}
