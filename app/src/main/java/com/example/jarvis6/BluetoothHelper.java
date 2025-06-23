package com.example.jarvis6;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

// TODO remove later -> since this is the older model
class BluetoothHelper {
    Context context;

    public BluetoothHelper(Context context) {
        this.context = context;
    }

    public boolean isBluetoothHeadsetConnected() {
        var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled()
                    && (bluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothAdapter.STATE_CONNECTED);

        } else
            return false;
    }

    public void startBluetoothConnection(AudioManager audioManager) {
        if (isBluetoothHeadsetConnected()) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        }
    }

    public void startBluetoothConnectionInCommunication(AudioManager audioManager) {
        if(isBluetoothHeadsetConnected()) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.startBluetoothSco();
            audioManager.setBluetoothScoOn(true);
        }
    }

    public void endBluetoothConnection(AudioManager audioManager) {
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setBluetoothScoOn(false);
        audioManager.stopBluetoothSco();
    }
}
