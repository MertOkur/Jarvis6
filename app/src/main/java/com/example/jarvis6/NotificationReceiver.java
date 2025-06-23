package com.example.jarvis6;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "jarvis_channel_id";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Notification-Kanal erstellen (nur für Android 8+)
        createNotificationChannel(context);

        // Intent zur Activity, die Sprachsteuerung startet
        var speechIntent = new Intent(context, SpeechRecForNotification.class);
        speechIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(speechIntent);

        // Notification bauen
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Jarvis")
                .setContentText("Sag 'Jarvis', ich höre zu...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Notification anzeigen
        var notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            System.err.println("Permission not granted");
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    // Notification Channel nur bei Android 8+ erforderlich
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var name = "Jarvis Channel";
            var description = "Channel für Sprachbenachrichtigung";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            var channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            var notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
    }
}