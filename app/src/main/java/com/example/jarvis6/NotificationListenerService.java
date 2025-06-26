package com.example.jarvis6;

import android.content.Intent;
import android.service.notification.StatusBarNotification;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class NotificationListenerService extends android.service.notification.NotificationListenerService {
    private static final String TAG = "NotificationListenerService";

    /*@Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        var packageName = sbn.getPackageName();
        var extras = sbn.getNotification().extras;

        var title = extras.getString("android.title");
        var text = extras.getString("android.text");

        System.out.printf("Benachrichtigung von: %s\ntitel: %s, text: %s", packageName, title, text);

        if (packageName.equalsIgnoreCase("com.whatsapp") || packageName.equalsIgnoreCase("com.google.android.gm")) {
            var intent = new Intent("com.example.jarvis6.NOTIFICATION");
            intent.putExtra("package", packageName);
            intent.putExtra("title", title);
            intent.putExtra("text", text);
            sendBroadcast(intent);
        }
    } */

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        var activeNotifications = getActiveNotifications();
        var notificationList = new ArrayList<Notification>();
        for (var sbn : activeNotifications) {
            var pkgName = sbn.getPackageName(); 
            var extras = sbn.getNotification().extras;
            var titleChar = (extras.getCharSequence("android.title"));
            var textChar = extras.getCharSequence("android.text");
            var title = titleChar != null ? titleChar.toString() : "";
            var text = textChar != null ? textChar.toString() : "";


           //  System.out.printf("Benachrichtigung von: %s\n titel: %s, text: %s", pkgName, title, text);
            if (pkgName.equalsIgnoreCase("com.whatsapp") || pkgName.equalsIgnoreCase("com.google.android.gm")) {
                var intent = new Intent("com.example.jarvis6.NOTIFICATION");
                intent.putExtra("package", pkgName);
                intent.putExtra("title", title);
                intent.putExtra("text", text);
                notificationList.add(new Notification(pkgName, title, text));
                // sendBroadcast(intent);
            }
        }

        saveNotifications(notificationList);
    }

    private void saveNotifications(ArrayList<Notification> notificationList) {
        var gson = new Gson();
        var directory = this.getFilesDir();
        var file = new File(directory, "notifications.jsonl");
        try (var writer = new BufferedWriter(new FileWriter(file))) {
            for (var notification : notificationList) {
                var jo = gson.toJson(notification);
                writer.write(jo);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("following error occurred while writing notifications: " + e);
        }
    }
}
