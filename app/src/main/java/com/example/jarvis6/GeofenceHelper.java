package com.example.jarvis6;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

// TODO check if this will be used in further versions -> deletable
public class GeofenceHelper {
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    public GeofenceHelper(Context context) {
        geofencingClient = LocationServices.getGeofencingClient(context);
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    @SuppressLint("MissingPermission")
    public void addGeofence(double latitude, double longitude, float radius) {
        Geofence geofence = new Geofence.Builder()
                .setRequestId("GEOFENCE_ID")
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent);
    }
}
