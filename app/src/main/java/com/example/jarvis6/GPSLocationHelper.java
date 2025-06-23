package com.example.jarvis6;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import static android.content.Context.LOCATION_SERVICE;

class GPSLocationHelper {
    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private GPSCallback currentPreciseLocationCallback;

    public interface GPSCallback {
        void onLocationReceived(GPSLocation location);

        void onLocationError(String errorMessage);
    }

    public GPSLocationHelper(Context context) {
        this.context = context.getApplicationContext(); // vermeidet memory-leak
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void getCurrentLocation(GPSCallback callback) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null)
                        callback.onLocationReceived(new GPSLocation(location.getLatitude(), location.getLongitude()));
                    else
                        callback.onLocationError("Location not available");
                }).addOnFailureListener(e -> callback.onLocationError(e.getMessage()));
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void requestPreciseCurrentLocation(GPSCallback callback) {
        this.currentPreciseLocationCallback = callback;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onLocationError("Missing permission for (ACCESS FINE LOCATION)!");
            return;
        }

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGPSEnabled) {
            callback.onLocationError("GPS is not enabled!");
            return;
        }

        if (locationListener == null) {
            locationListener = new LocationsListener();
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener, Looper.getMainLooper());

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (currentPreciseLocationCallback != null) {
                    currentPreciseLocationCallback.onLocationError("No location found!");
                    stopPreciseLocationUpdates();
                }
            }, 20000); // Timeout after 5 seconds
        } catch (SecurityException e) {
            callback.onLocationError(e.getMessage());
            stopPreciseLocationUpdates();
        }
    }

    private class LocationsListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (currentPreciseLocationCallback != null) {
                currentPreciseLocationCallback.onLocationReceived(new GPSLocation(location.getLatitude(), location.getLongitude()));
                stopPreciseLocationUpdates();
            }
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            LocationListener.super.onProviderEnabled(provider);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            // LocationListener.super.onProviderDisabled(provider);
            if (currentPreciseLocationCallback != null) {
                currentPreciseLocationCallback.onLocationError("GPS is not enabled!");
                stopPreciseLocationUpdates();
            }
        }
    }

    public void stopPreciseLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationListener = null;
            currentPreciseLocationCallback = null; // Reset the callback
        }
    }
}


