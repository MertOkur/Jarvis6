package com.example.jarvis6;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;

class NotificationHelper {

   private static final String CHANNEL_ID = "smart_assistant_channel_id";
   private static final String CHANNEL_NAME = "Smart Assistant Channel";
   private static final int NOTIFICATION_ID = 1;

   private final Context context;

   public NotificationHelper(Context context) {
      this.context = context;
      createNotificationChannel();
   }

   private void createNotificationChannel() {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
         var channel = new android.app.NotificationChannel(CHANNEL_ID, CHANNEL_NAME, android.app.NotificationManager.IMPORTANCE_DEFAULT);
         channel.setDescription("Benachrichtigungen vom Smart Assitant");
         channel.enableLights(true);
         channel.setLightColor(Color.BLUE);
         channel.enableVibration(true);

         var manager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
         if (manager != null) {
            manager.createNotificationChannel(channel);
         }
      }
   }

   public void showNotification(String title, String content, Class<?> activityToOpen) {
      var intent = new Intent(context, activityToOpen);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

      var pendingIntent = PendingIntent.getActivity(
              context,
              0,
              intent,
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
      );

      var builder = new NotificationCompat.Builder(context, CHANNEL_ID)
              .setSmallIcon(android.R.drawable.ic_dialog_info)
              .setContentTitle(title)
              .setContentText(content)
              .setPriority(NotificationCompat.PRIORITY_DEFAULT)
              .setContentIntent(pendingIntent)
              .setAutoCancel(true);

      var notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

      if (notificationManager != null) {
         notificationManager.notify(NOTIFICATION_ID, builder.build());
      }
   }
}
