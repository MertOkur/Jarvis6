package com.example.jarvis6;

// The datatype for messages to chatGPT
public record Message(String date, GPSLocation gpsLocation, String message) {

    public String getMessage() {
        return "Aktuelles Datum und Uhrzeit: " + date + ", die GPS-Koordinaten sind: " + gpsLocation.lat() + ", " + gpsLocation.lon()
                + ", die Nachricht ist: " + message;
    }

}
