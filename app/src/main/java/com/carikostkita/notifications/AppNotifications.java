package com.carikostkita.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.carikostkita.R;
import com.carikostkita.ui.splash.SplashActivity;

/** Kanal & pengiriman notifikasi lokal. */
public final class AppNotifications {
    public static final String CHANNEL_CHAT = "chat";
    public static final String CHANNEL_STATUS = "status";

    private AppNotifications() {}

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel chat = new NotificationChannel(CHANNEL_CHAT, "Pesan", NotificationManager.IMPORTANCE_HIGH);
        chat.setDescription("Balasan baru dari pemilik atau calon penyewa");
        NotificationChannel status = new NotificationChannel(CHANNEL_STATUS, "Status & Survei", NotificationManager.IMPORTANCE_DEFAULT);
        status.setDescription("Verifikasi akun/kost, jadwal survei, dan kamar kosong di kost favorit");
        nm.createNotificationChannel(chat);
        nm.createNotificationChannel(status);
    }

    public static boolean canPost(Context context) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    @SuppressWarnings("MissingPermission")
    public static void post(Context context, int id, String channel, String title, String text) {
        if (!canPost(context)) return;
        Intent open = new Intent(context, SplashActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_bell)
                .setColor(ContextCompat.getColor(context, R.color.primary))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(CHANNEL_CHAT.equals(channel) ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(id, b.build());
    }

    /** Minta izin notifikasi sekali (Android 13+) lalu periksa status terbaru di latar. */
    public static void onAppOpened(android.app.Activity activity) {
        NotificationJobService.schedule(activity);
        NotificationJobService.runNow(activity);
        if (Build.VERSION.SDK_INT < 33) return;
        android.content.SharedPreferences p = activity.getSharedPreferences("notif_state", Context.MODE_PRIVATE);
        if (p.getBoolean("asked_permission", false)) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        p.edit().putBoolean("asked_permission", true).apply();
        androidx.core.app.ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7301);
    }
}
