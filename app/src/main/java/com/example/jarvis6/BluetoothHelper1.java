package com.example.jarvis6;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class BluetoothHelper1 {

   private static final String TAG = "BluetoothHelper"; // Für Logcat
   private final Context context;
   private final BluetoothAdapter bluetoothAdapter;
   private final AudioManager audioManager;

   public BluetoothHelper1(Context context, AudioManager audioManager) {
      this.context = context;
      this.audioManager = audioManager;
      this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
   }

   // Prüft, ob Bluetooth generell aktiviert ist und ob ein Headset verbunden ist.
   // ACHTUNG: Benötigt BLUETOOTH_CONNECT Berechtigung auf API 31+
   public boolean isBluetoothHeadsetConnected() {
      if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
         Log.d(TAG, "Bluetooth ist nicht verfügbar oder deaktiviert.");
         return false;
      }

      // Berechtigungsprüfung für BLUETOOTH_CONNECT (API 31+)
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
         if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "BLUETOOTH_CONNECT Berechtigung fehlt.");
            // Normalerweise solltest du hier die Berechtigung anfordern,
            // oder den Benutzer informieren. Für diese Methode geben wir einfach false zurück.
            return false;
         }
      }

      // Überprüfe den Verbindungsstatus des Headset-Profils
      return bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED;
   }


   // Startet die Bluetooth SCO Verbindung.
   // Diese Methode sollte aufgerufen werden, wenn man Audio über das Headset senden/empfangen will.
   // Die tatsächliche Modus- und SCO-On-Einstellung sollte im BroadcastReceiver
   // beim Erreichen des CONNECTED-Status erfolgen.
   public void startBluetoothScoConnection() {
      if (!isBluetoothHeadsetConnected()) {
         Log.w(TAG, "Kein Bluetooth-Headset verbunden oder Bluetooth ist deaktiviert.");
         return;
      }

      Log.d(TAG, "Versuche Bluetooth SCO zu starten...");
      // WICHTIG: setMode und setBluetoothScoOn NICHT HIER aufrufen,
      // sondern im BroadcastReceiver, wenn SCO_AUDIO_STATE_CONNECTED erreicht wird!
      audioManager.startBluetoothSco();
   }

   // Beendet die Bluetooth SCO Verbindung.
   public void stopBluetoothScoConnection() {
      Log.d(TAG, "Versuche Bluetooth SCO zu stoppen...");
      audioManager.stopBluetoothSco();
      // Nach dem Stoppen kann es sinnvoll sein, den Audiomodus zurückzusetzen
      // Dies hängt aber vom Gesamtkontext deiner App ab.
      // audioManager.setMode(AudioManager.MODE_NORMAL);
      audioManager.setBluetoothScoOn(false); // Stellen Sie sicher, dass dies auch zurückgesetzt wird
   }

   // Diese Methode sollte vom SCO Broadcast Receiver aufgerufen werden,
   // wenn der SCO-Zustand CONNECTED ist.
   public void onScoConnected() {
      Log.d(TAG, "SCO Audio State: CONNECTED. Konfiguriere AudioManager.");
      audioManager.stopBluetoothSco();
      audioManager.setMode(AudioManager.MODE_NORMAL); // Setze Modus temporär auf IDLE
      audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION); // Richtigen Modus setzen
      audioManager.setBluetoothScoOn(false);
      audioManager.setSpeakerphoneOn(true); // Sehr wichtig für Headsets!
   }

   // Diese Methode sollte vom SCO Broadcast Receiver aufgerufen werden,
   // wenn der SCO-Zustand DISCONNECTED ist.
   public void onScoDisconnected() {
      if(!audioManager.isBluetoothScoOn()) {
         audioManager.startBluetoothSco();
         audioManager.setBluetoothScoOn(true);
         audioManager.setSpeakerphoneOn(false);
      }
   }
}