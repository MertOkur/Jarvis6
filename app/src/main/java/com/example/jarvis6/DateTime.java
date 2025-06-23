package com.example.jarvis6;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class DateTime {

    public static String getCurrentDateTime() {
        var now = LocalDateTime.now();
        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return now.format(formatter);
    }
}
